package com.usefocus.app.vpn

import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.*
import android.os.Build
import android.os.ParcelFileDescriptor
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import com.usefocus.app.BlockerApplication
import com.usefocus.app.data.model.BlockRule
import com.usefocus.app.domain.ScheduleEvaluator
import com.usefocus.app.domain.ServiceCatalog
import java.io.FileInputStream
import java.io.FileOutputStream
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
            app.protection.recordUserStop()
            job?.cancel()
            job = null
            runCatching { tunnel?.close() }
            tunnel = null
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        if (!app.disclosures.state.value.vpnAccepted) {
            app.protection.failed("Review and accept website protection's DNS disclosure in Focus.")
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
            val descriptor = establishTunnel()
            tunnel = descriptor
            app.protection.active()
            val worker = launch { processPackets(descriptor) }
            try {
                while (isActive) {
                    withTimeoutOrNull(1_000) { changed.receive() }
                }
            } finally {
                worker.cancel()
                runCatching { descriptor.close() }
                if (tunnel === descriptor) tunnel = null
                worker.join()
            }
        }
    }

    private fun establishTunnel(): ParcelFileDescriptor {
        val builder = Builder().setSession("Focus").setMtu(8192).setBlocking(true)
        builder
            .addAddress("10.111.0.1", 32)
            .addDnsServer("10.111.0.2")
            .addRoute("10.111.0.2", 32)
            .allowFamily(OsConstants.AF_INET6)
        return builder.establish() ?: error("VPN permission revoked")
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
        // Connections are explicitly bound to a non-VPN network; the tunnel captures only its
        // local DNS address. DNS payloads go to Cloudflare over verified HTTPS, never UDP/53.
        for (network in networks.take(2)) {
            val answer = DnsOverHttps { url -> network.openConnection(url, java.net.Proxy.NO_PROXY) }
                .resolve(query)
            if (answer != null) return answer
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
        const val START = "com.usefocus.app.START"
        const val STOP = "com.usefocus.app.STOP"
    }
}
