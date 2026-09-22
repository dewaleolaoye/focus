package com.usefocus.app

import com.usefocus.app.data.model.BlockRule
import com.usefocus.app.domain.AppBlockingPolicy
import com.usefocus.app.domain.ServiceCatalog
import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test

class AppBlockingPolicyTest {
    private val now = ZonedDateTime.parse("2026-09-14T12:00:00Z")

    private fun rule(serviceId: String?, enabled: Boolean = true, start: Int = 0, end: Int = 1439) =
        BlockRule(
            domain = "example.com",
            serviceId = serviceId,
            startMinute = start,
            endMinute = end,
            daysMask = 1,
            enabled = enabled,
        )

    @Test
    fun activePopularRulesReturnEveryKnownPackageVariant() {
        val packages =
            AppBlockingPolicy.activePackages(
                listOf(rule("instagram"), rule("x"), rule("facebook"), rule("youtube")),
                now,
            )
        assertTrue("com.instagram.android" in packages)
        assertTrue("com.instagram.lite" in packages)
        assertTrue("com.twitter.android" in packages)
        assertTrue("com.facebook.katana" in packages)
        assertTrue("com.facebook.lite" in packages)
        assertTrue("com.google.android.youtube" in packages)
        assertTrue("com.google.android.youtube.go" in packages)
        assertTrue("com.google.android.youtube.tv" in packages)
    }

    @Test
    fun customDisabledAndInactiveRulesDoNotSelectPackages() {
        val packages =
            AppBlockingPolicy.activePackages(
                listOf(rule(null), rule("instagram", enabled = false), rule("x", start = 1, end = 2)),
                now,
            )
        assertTrue(packages.isEmpty())
    }

    @Test
    fun allSixAppsAndTheirVariantsAreBlockedOnlyDuringTheirSchedule() {
        assertEquals(setOf("instagram", "whatsapp", "facebook", "x", "tiktok", "youtube"),
            ServiceCatalog.popular.map { it.id }.toSet())
        for (service in ServiceCatalog.popular) {
            val scheduled = rule(service.id, start = 720, end = 780)
            for (pkg in service.androidPackages) {
                assertNull(AppBlockingPolicy.blockingRule(pkg, listOf(scheduled), now.minusSeconds(1), true))
                assertEquals(scheduled, AppBlockingPolicy.blockingRule(pkg, listOf(scheduled), now, true))
                assertNull(AppBlockingPolicy.blockingRule(pkg, listOf(scheduled), now.plusHours(1), true))
                assertNull(AppBlockingPolicy.blockingRule(pkg, listOf(scheduled), now, false))
                assertNull(AppBlockingPolicy.blockingRule(pkg, listOf(scheduled.copy(enabled = false)), now, true))
            }
        }
    }

    @Test
    fun homeBrowserUnknownPackagesAndCustomWebsitesAreNeverEjected() {
        val rules = ServiceCatalog.popular.map { rule(it.id) }
        for (pkg in listOf(null, "com.android.launcher3", "com.android.chrome", "com.usefocus.app",
            "com.twitter.android.fake", "com.example.unrelated")) {
            assertNull(AppBlockingPolicy.blockingRule(pkg, rules, now, true))
        }
        assertNull(AppBlockingPolicy.blockingRule("com.instagram.android",
            listOf(rule(null).copy(domain = "instagram.com")), now, true))
    }

    @Test
    fun overnightAndOverlappingSchedulesKeepBlockingUntilTheLastRuleEnds() {
        val overnight = rule("whatsapp", start = 1380, end = 60)
        val tuesday = ZonedDateTime.parse("2026-09-15T00:30:00Z")
        assertEquals(overnight, AppBlockingPolicy.blockingRule("com.whatsapp", listOf(overnight), tuesday, true))
        assertNull(AppBlockingPolicy.blockingRule("com.whatsapp", listOf(overnight), tuesday.plusMinutes(30), true))
        val overlap = rule("whatsapp", start = 0, end = 120).copy(daysMask = 2)
        assertEquals(overlap, AppBlockingPolicy.blockingRule("com.whatsapp", listOf(overnight, overlap), tuesday.plusMinutes(30), true))
        assertNull(AppBlockingPolicy.blockingRule("com.whatsapp", emptyList(), tuesday, true))
    }
}
