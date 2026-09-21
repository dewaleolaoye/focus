package com.websiteblocker.app

import android.app.UiAutomation
import android.content.Intent
import android.net.VpnService
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.websiteblocker.app.accessibility.AppBlockingAccess
import com.websiteblocker.app.data.model.BlockRule
import com.websiteblocker.app.domain.ServiceCatalog
import com.websiteblocker.app.vpn.ProtectionPhase
import com.websiteblocker.app.vpn.WebsiteBlockVpnService
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.time.LocalTime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Opt-in on a test emulator: -Pandroid.testInstrumentationRunnerArguments.appBlockingIntegration=true. */
@RunWith(AndroidJUnit4::class)
class AppBlockingIntegrationTest {
    @Test
    fun nativeBlockingAndWebsiteFilteringWorkTogether() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("appBlockingIntegration") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        // Default UiAutomation suppresses other accessibility services, invalidating this test.
        val automation = instrumentation.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        fun shell(command: String): String =
            ParcelFileDescriptor.AutoCloseInputStream(automation.executeShellCommand(command))
                .bufferedReader().use { it.readText().trim() }
        val app = instrumentation.targetContext.applicationContext as BlockerApplication
        val originalDisclosure = app.disclosures.state.value
        val dao = app.database.rules()
        val originalRules = dao.observeRules().first()
        val originallyOn = app.protection.desired
        val hadVpnConsent = VpnService.prepare(app) == null
        val originalServices = shell("settings get secure enabled_accessibility_services")
        val originalAccessibility = shell("settings get secure accessibility_enabled")
        val component = "com.websiteblocker.app/com.websiteblocker.app.accessibility.AppBlockAccessibilityService"
        val services = (originalServices.takeUnless { it == "null" || it.isBlank() }
            ?.split(':').orEmpty() + component).distinct().joinToString(":")
        val inserted = mutableListOf<Long>()
        val main = instrumentation.startActivitySync(Intent(app, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val youtube = "com.google.android.youtube"
        val youtubeNotificationsGranted = app.packageManager.checkPermission(
            android.Manifest.permission.POST_NOTIFICATIONS, youtube,
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val launchYoutube = app.packageManager.getLaunchIntentForPackage(youtube)
        assertNotNull("Install the real YouTube app on the test emulator", launchYoutube)
        var stage = "initial app launch"
        fun checkpoint(name: String) {
            stage = name
            instrumentation.sendStatus(2, android.os.Bundle().apply { putString("stream", "Checkpoint: $name\n") })
        }
        suspend fun waitForPackage(expected: String, timeout: Long = 12_000) {
            val reached = withTimeoutOrNull(timeout) {
                while (automation.rootInActiveWindow?.packageName?.toString() != expected) delay(100)
                true
            }
            assertTrue("$stage: expected foreground $expected, got ${automation.rootInActiveWindow?.packageName}", reached == true)
        }
        // Shell resolution is not narrowed by the app's package-visibility manifest queries.
        val launcher = shell("cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME")
            .lineSequence().last { '/' in it }.substringBefore('/')
        // Launch as a user/shell action: app.startActivity from a background test process is
        // rejected by modern Android and would falsely look like successful native blocking.
        fun openYoutube() {
            // UiAutomation executes argv directly; quote characters would become part of the name.
            // Do not use -W: immediate blocking can dismiss the activity before Android
            // reports its launch as drawn. Foreground assertions below observe the result.
            val result = shell("am start -n ${launchYoutube!!.component!!.flattenToString()}")
            assertFalse("YouTube launch failed: $result", result.contains("Error"))
        }
        try {
            if (!originalDisclosure.vpnAccepted) app.disclosures.acceptVpn()
            if (!originalDisclosure.accessibilityAccepted) app.disclosures.acceptAccessibility()
            originalRules.forEach { dao.setEnabled(it.id, false) }
            shell("settings put secure enabled_accessibility_services $services")
            shell("settings put secure accessibility_enabled 1")
            withTimeout(12_000) { AppBlockingAccess.connection.first { it } }
            shell("appops set com.websiteblocker.app ACTIVATE_VPN allow")
            instrumentation.runOnMainSync { app.protection.start() }
            withTimeout(15_000) { app.protection.state.first { it.phase == ProtectionPhase.ON } }
            // Avoid YouTube's first-launch notification dialog obscuring the actual app window.
            if (android.os.Build.VERSION.SDK_INT >= 33 && !youtubeNotificationsGranted)
                shell("pm grant $youtube android.permission.POST_NOTIFICATIONS")
            openYoutube()
            waitForPackage(youtube)
            val minute = LocalTime.now().let { it.hour * 60 + it.minute }
            val template = BlockRule(domain = "example.org", startMinute = (minute + 1439) % 1440,
                endMinute = (minute + 10) % 1440, daysMask = 127)
            val ids = ServiceCatalog.popular.associate { service ->
                service.id to dao.insert(template.copy(domain = service.primaryDomain, serviceId = service.id))
                    .also { inserted += it }
            }
            inserted += dao.insert(template)
            checkpoint("rule added while YouTube is open")
            // Saving a rule must eject an already-open native app without waiting for another event.
            waitForPackage(launcher)
            checkpoint("repeated user launches")
            repeat(3) {
                openYoutube()
                delay(1_000)
                waitForPackage(launcher)
            }
            checkpoint("concurrent DNS for all six services and custom websites")
            ServiceCatalog.popular.forEach {
                assertEquals("${it.name} website must stay blocked alongside native blocking", 3, resolve(it.primaryDomain))
                assertEquals("${it.name} subdomains", 3, resolve("www.${it.primaryDomain}"))
            }
            assertEquals("Custom sites remain blocked with all six app rules active", 3, resolve("example.org"))
            assertEquals("Unrelated DNS still forwards", 0, resolve("example.com"))

            checkpoint("rule disabled")
            dao.setEnabled(ids.getValue("youtube"), false)
            delay(600)
            openYoutube()
            waitForPackage(youtube)
            assertEquals("Disabling restores website DNS too", 0, resolve("youtube.com"))
            checkpoint("rule re-enabled")
            dao.setEnabled(ids.getValue("youtube"), true)
            waitForPackage(launcher)

            checkpoint("Accessibility consent withdrawn and restored")
            instrumentation.runOnMainSync { app.disclosures.revokeAccessibility() }
            delay(600)
            openYoutube()
            waitForPackage(youtube)
            delay(1_000)
            waitForPackage(youtube)
            assertEquals("Withdrawing native consent preserves website filtering", 3, resolve("youtube.com"))
            instrumentation.runOnMainSync { app.disclosures.acceptAccessibility() }
            // Consent was withdrawn, so no foreground name was retained. A new user launch
            // supplies a fresh event after consent is restored.
            shell("am start -n com.websiteblocker.app/.MainActivity")
            openYoutube()
            waitForPackage(launcher)

            checkpoint("notification stop")
            // The notification delivers STOP directly; it must also disable native enforcement.
            instrumentation.runOnMainSync {
                app.startService(Intent(app, WebsiteBlockVpnService::class.java)
                    .setAction(WebsiteBlockVpnService.STOP))
            }
            delay(600)
            assertFalse("Notification stop must persist the master switch", app.protection.desired)
            openYoutube()
            waitForPackage(youtube)
            delay(1_000)
            waitForPackage(youtube)
            checkpoint("protection restart")
            shell("am start -n com.websiteblocker.app/.MainActivity")
            waitForPackage(app.packageName)
            instrumentation.runOnMainSync { app.protection.start() }
            withTimeout(15_000) { app.protection.state.first { it.phase == ProtectionPhase.ON } }
            openYoutube()
            delay(1_000)
            waitForPackage(launcher)

            checkpoint("rule deleted")
            dao.delete(ids.getValue("youtube"))
            delay(600)
            openYoutube()
            waitForPackage(youtube)
            assertEquals("Deleting restores website DNS", 0, resolve("youtube.com"))

            checkpoint("future schedule boundary while YouTube is open")
            // A quiet foreground app must be ejected at a future boundary without another event.
            val nextMinute = (LocalTime.now().let { it.hour * 60 + it.minute } + 1) % 1440
            inserted += dao.insert(template.copy(domain = "youtube.com", serviceId = "youtube",
                startMinute = nextMinute, endMinute = (nextMinute + 2) % 1440))
            waitForPackage(youtube)
            waitForPackage(launcher, 70_000)
            assertEquals("Schedule boundary applies to web and native enforcement", 3, resolve("youtube.com"))

        } finally {
            inserted.forEach { dao.delete(it) }
            originalRules.forEach { dao.setEnabled(it.id, it.enabled) }
            shell(if (originalServices == "null" || originalServices.isEmpty())
                "settings delete secure enabled_accessibility_services"
                else "settings put secure enabled_accessibility_services $originalServices")
            shell(if (originalAccessibility == "null") "settings delete secure accessibility_enabled"
                else "settings put secure accessibility_enabled $originalAccessibility")
            if (originallyOn && !app.protection.desired)
                shell("am start -n com.websiteblocker.app/.MainActivity")
            instrumentation.runOnMainSync {
                if (originallyOn) app.protection.start() else app.protection.stop()
                main.finish()
            }
            if (!originalDisclosure.vpnAccepted) {
                instrumentation.runOnMainSync { app.protection.stop() }
                app.disclosures.revokeVpn()
                app.getSharedPreferences("protection", 0).edit().putBoolean("enabled", originallyOn).apply()
            }
            if (!originalDisclosure.accessibilityAccepted) app.disclosures.revokeAccessibility()
            if (!hadVpnConsent) shell("appops set com.websiteblocker.app ACTIVATE_VPN default")
            if (android.os.Build.VERSION.SDK_INT >= 33 && !youtubeNotificationsGranted)
                shell("pm revoke $youtube android.permission.POST_NOTIFICATIONS")
        }
    }

    private fun resolve(host: String): Int {
        val labels = host.split('.').flatMap { listOf(it.length.toByte()) + it.toByteArray().toList() }
        val query = (listOf<Byte>(0x43, 0x21, 1, 0, 0, 1, 0, 0, 0, 0, 0, 0) + labels +
            listOf<Byte>(0, 0, 1, 0, 1)).toByteArray()
        return DatagramSocket().use { socket ->
            socket.soTimeout = 8_000
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
