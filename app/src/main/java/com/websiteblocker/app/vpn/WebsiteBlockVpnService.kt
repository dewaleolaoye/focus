package com.websiteblocker.app.vpn

import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.*
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import com.websiteblocker.app.BlockerApplication
import com.websiteblocker.app.data.model.BlockRule
import com.websiteblocker.app.domain.ScheduleEvaluator
import com.websiteblocker.app.domain.ServiceCatalog
import com.websiteblocker.app.domain.AppBlockingPolicy
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.time.ZonedDateTime
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.collectLatest

/** DNS-only split tunnel. Browsing data never passes through this application. */
class WebsiteBlockVpnService : VpnService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val app
        get() = application as BlockerApplication

    private var tunnel: ParcelFileDescriptor? = null
    private var job: Job? = null
    @Volatile private var rules: List<BlockRule> = emptyList()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == STOP) {
            job?.cancel()
            job = null
            runCatching { tunnel?.close() }
            tunnel = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        if (!app.protection.desired || prepare(this) != null) {
            app.protection.failed("VPN permission is required. Open Focus to reactivate.")
            stopSelf()
            return START_NOT_STICKY
        }
        if (job?.isActive == true) {
            // The system can keep a VPN service bound while its started state changes. If the
            // tunnel is already running, report the real enforcement state instead of leaving
            // the UI indefinitely in STARTING.
            app.protection.active()
            return START_STICKY
        }
        try {
            val notification = VpnNotificationManager(this).notification()
            if (Build.VERSION.SDK_INT >= 34)
                startForeground(
                    VpnNotificationManager.ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED,
                )
            else startForeground(VpnNotificationManager.ID, notification)
        } catch (_: RuntimeException) {
            app.protection.failed(
                "Android prevented VPN startup. Open the app and enable protection again."
            )
            stopSelf()
            return START_NOT_STICKY
        }
        job = scope.launch {
            try {
                rules = app.repository.rules.first()
                runTunnelLoop()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                app.protection.failed(
                    "Protection stopped. Check VPN permission, then enable it again."
                )
                withContext(Dispatchers.Main) { stopSelf() }
            } finally {
                runCatching { tunnel?.close() }
                tunnel = null
            }
        }
        return START_STICKY
    }

    private suspend fun runTunnelLoop() = coroutineScope {
        val changed = Channel<Unit>(Channel.CONFLATED)
        launch {
            app.repository.rules.collectLatest {
                rules = it
                changed.trySend(Unit)
            }
        }
        while (isActive) {
            val requestedPackages = AppBlockingPolicy.activePackages(rules, ZonedDateTime.now())
            val installedPackages = requestedPackages.filterTo(linkedSetOf(), ::isInstalled)
            val descriptor = establishTunnel(installedPackages)
            tunnel = descriptor
            app.protection.active()
            val worker =
                launch {
                    if (installedPackages.isEmpty()) processPackets(descriptor)
                    else discardPackets(descriptor)
                }
            try {
                while (isActive) {
                    withTimeoutOrNull(1_000) { changed.receive() }
                    val next =
                        AppBlockingPolicy.activePackages(rules, ZonedDateTime.now())
                            .filterTo(linkedSetOf(), ::isInstalled)
                    if (next != installedPackages) break
                }
            } finally {
                worker.cancel()
                runCatching { descriptor.close() }
                if (tunnel === descriptor) tunnel = null
                worker.join()
            }
        }
    }

    private fun isInstalled(packageName: String): Boolean =
        try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: Exception) {
            false
        }

    private fun establishTunnel(blockedPackages: Set<String>): ParcelFileDescriptor {
        val builder = Builder().setSession("Focus").setMtu(8192).setBlocking(true)
        if (blockedPackages.isEmpty()) {
            builder
                .addAddress("10.111.0.1", 32)
                .addDnsServer("10.111.0.2")
                .addRoute("10.111.0.2", 32)
                .allowFamily(OsConstants.AF_INET6)
        } else {
            builder
                .addAddress("10.111.0.1", 32)
                .addAddress("fd6f:7a65:626c::1", 128)
                .addRoute("0.0.0.0", 0)
                .addRoute("::", 0)
            blockedPackages.forEach(builder::addAllowedApplication)
        }
        return builder.establish() ?: error("VPN permission revoked")
    }

    /** Package-scoped full tunnel: reading and discarding blocks every protocol and endpoint. */
    private suspend fun discardPackets(descriptor: ParcelFileDescriptor) =
        withContext(Dispatchers.IO) {
            val input = FileInputStream(descriptor.fileDescriptor)
            val buffer = ByteArray(8192)
            while (isActive) {
                val count = input.read(buffer)
                if (count <= 0) error("Tunnel closed")
            }
        }

    private suspend fun processPackets(descriptor: ParcelFileDescriptor) = coroutineScope {
        val queue = Channel<DnsPacketHandler.Query>(32)
        val input = FileInputStream(descriptor.fileDescriptor)
        val output = FileOutputStream(descriptor.fileDescriptor)
        val outputLock = Any()
        repeat(8) {
            launch {
                for (query in queue) {
                    val blocked = rules.any {
                        ServiceCatalog.matches(query.hostname, it) &&
                            ScheduleEvaluator.isActive(it, ZonedDateTime.now())
                    }
                    var answer =
                        if (blocked) DnsPacketHandler.failure(query)
                        else forward(query) ?: DnsPacketHandler.failure(query, 2)
                    // Recheck after network IO so an edit or schedule boundary cannot release a
                    // now-blocked answer.
                    if (
                        rules.any {
                            ServiceCatalog.matches(query.hostname, it) &&
                                ScheduleEvaluator.isActive(it, ZonedDateTime.now())
                        }
                    )
                        answer = DnsPacketHandler.failure(query)
                    val packet = DnsPacketHandler.wrap(query, answer)
                    synchronized(outputLock) { output.write(packet) }
                }
            }
        }
        val poll =
            StructPollfd().apply {
                fd = descriptor.fileDescriptor
                events = OsConstants.POLLIN.toShort()
            }
        val buffer = ByteArray(8192)
        try {
            while (isActive) {
                if (Os.poll(arrayOf(poll), 500) == 0) continue
                if (
                    poll.revents.toInt() and
                        (OsConstants.POLLERR or OsConstants.POLLHUP or OsConstants.POLLNVAL) != 0
                )
                    error("Tunnel closed")
                val count = input.read(buffer)
                if (count <= 0) error("Tunnel closed")
                val query = DnsPacketHandler.parse(buffer.copyOf(count)) ?: continue
                if (queue.trySend(query).isFailure)
                    synchronized(outputLock) {
                        output.write(
                            DnsPacketHandler.wrap(query, DnsPacketHandler.failure(query, 2))
                        )
                    }
            }
        } finally {
            queue.close()
        }
    }

    @Suppress("DEPRECATION")
    private fun forward(query: DnsPacketHandler.Query): ByteArray? {
        val manager = getSystemService(ConnectivityManager::class.java)
        // Never select our own VPN as the underlying network. Resolve afresh on every query to
        // follow Wi-Fi/cellular changes.
        val networks =
            manager.allNetworks
                .filter { network ->
                    manager.getNetworkCapabilities(network)?.let {
                        it.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                            it.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
                    } == true
                }
                .sortedByDescending { network ->
                    val capabilities = manager.getNetworkCapabilities(network)
                    (if (network == manager.activeNetwork) 8 else 0) +
                        (if (
                            capabilities?.hasCapability(
                                NetworkCapabilities.NET_CAPABILITY_VALIDATED
                            ) == true
                        )
                            4
                        else 0) +
                        (if (
                            Build.VERSION.SDK_INT >= 28 &&
                                capabilities?.hasCapability(
                                    NetworkCapabilities.NET_CAPABILITY_FOREGROUND
                                ) == true
                        )
                            2
                        else 0) +
                        (if (
                            capabilities?.hasCapability(
                                NetworkCapabilities.NET_CAPABILITY_NOT_METERED
                            ) == true
                        )
                            1
                        else 0)
                }
        // A validated background cellular network may still reject this UID's sockets.
        // Prefer the usable foreground network, then try another reported network on failure.
        for (network in networks.take(2)) {
            val configured =
                manager.getLinkProperties(network)?.dnsServers.orEmpty().filter {
                    !it.isAnyLocalAddress &&
                        !it.isLoopbackAddress &&
                        !it.isMulticastAddress &&
                        it.hostAddress != "10.111.0.2"
                }
            val servers = configured.ifEmpty {
                listOf(InetAddress.getByAddress(byteArrayOf(1, 1, 1, 1)))
            }
            for (server in servers.take(2)) {
                try {
                    DatagramSocket().use { socket ->
                        if (!protect(socket)) return null
                        network.bindSocket(socket)
                        socket.connect(server, 53)
                        socket.soTimeout = 2000
                        socket.send(DatagramPacket(query.dns, query.dns.size))
                        val response = DatagramPacket(ByteArray(4096), 4096)
                        socket.receive(response)
                        val bytes = response.data.copyOf(response.length)
                        if (DnsPacketHandler.validResponse(query, bytes)) return bytes
                    }
                } catch (_: Exception) {
                    /* No traffic logging. Try the next reported resolver/network. */
                }
            }
        }
        return null
    }

    override fun onRevoke() {
        app.protection.failed(
            "VPN permission was revoked or another VPN took over. Reactivate protection here."
        )
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        runCatching { tunnel?.close() }
        tunnel = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        if (app.protection.state.value.phase != ProtectionPhase.NEEDS_REACTIVATION)
            app.protection.stopped()
        super.onDestroy()
    }

    companion object {
        const val START = "com.websiteblocker.app.START"
        const val STOP = "com.websiteblocker.app.STOP"
    }
}
