package com.websiteblocker.app

import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.websiteblocker.app.data.model.BlockRule
import com.websiteblocker.app.vpn.ProtectionPhase
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.time.LocalTime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Opt-in: run on a disposable emulator with
 * -Pandroid.testInstrumentationRunnerArguments.vpnIntegration=true.
 */
@RunWith(AndroidJUnit4::class)
class VpnDnsIntegrationTest {
    @Test
    fun realTunBlocksSubdomainsAndForwardsAllowedDns() = runBlocking {
        org.junit.Assume.assumeTrue(
            InstrumentationRegistry.getArguments().getString("vpnIntegration") == "true"
        )
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val app = instrumentation.targetContext.applicationContext as BlockerApplication
        val dao = app.database.rules()
        check(dao.observeRules().first().isEmpty()) {
            "Use a fresh test emulator; this test must not change user rules."
        }
        android.os.ParcelFileDescriptor.AutoCloseInputStream(
                instrumentation.uiAutomation.executeShellCommand(
                    "appops set com.websiteblocker.app ACTIVATE_VPN allow"
                )
            )
            .use { it.readBytes() }
        val activity =
            instrumentation.startActivitySync(
                Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        val now = LocalTime.now().let { it.hour * 60 + it.minute }
        val rule =
            BlockRule(
                domain = "x.com",
                startMinute = (now + 1439) % 1440,
                endMinute = (now + 10) % 1440,
                daysMask = 127,
            )
        val id = dao.insert(rule)
        try {
            instrumentation.runOnMainSync { app.protection.start() }
            withTimeout(15000) { app.protection.state.first { it.phase == ProtectionPhase.ON } }
            val connectivity = app.getSystemService(android.net.ConnectivityManager::class.java)
            withTimeout(15000) {
                while (
                    connectivity
                        .getNetworkCapabilities(connectivity.activeNetwork)
                        ?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_VPN) != true
                ) delay(100)
            }
            assertEquals("Blocked root must return NXDOMAIN", 3, resolve("x.com"))
            assertEquals("Blocked subdomain must return NXDOMAIN", 3, resolve("mobile.x.com"))
            assertEquals(
                "Unrelated site must resolve through protected upstream",
                0,
                resolve("example.com"),
            )
            dao.setEnabled(id, false)
            delay(500)
            assertEquals("Disabling must restore DNS", 0, resolve("x.com"))
            dao.setEnabled(id, true)
            delay(500)
            assertEquals(3, resolve("x.com"))
            dao.delete(id)
            delay(500)
            assertEquals("Deleting must restore DNS", 0, resolve("x.com"))
        } finally {
            dao.delete(id)
            instrumentation.runOnMainSync {
                app.protection.stop()
                activity.finish()
            }
        }
    }

    private fun resolve(host: String): Int {
        val labels =
            host.split('.').flatMap { listOf(it.length.toByte()) + it.toByteArray().toList() }
        val query =
            (listOf<Byte>(0x43, 0x21, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0) +
                    labels +
                    listOf<Byte>(0, 0, 1, 0, 1))
                .toByteArray()
        return DatagramSocket().use { socket ->
            socket.soTimeout = 8000
            socket.connect(InetAddress.getByAddress(byteArrayOf(10, 111, 0, 2)), 53)
            socket.send(DatagramPacket(query, query.size))
            val answer = DatagramPacket(ByteArray(4096), 4096)
            socket.receive(answer)
            assertTrue(answer.length >= 12)
            assertEquals(0x43, answer.data[0].toInt())
            assertEquals(0x21, answer.data[1].toInt())
            answer.data[3].toInt() and 15
        }
    }
}
