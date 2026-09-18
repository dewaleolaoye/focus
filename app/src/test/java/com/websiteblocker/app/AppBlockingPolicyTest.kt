package com.websiteblocker.app

import com.websiteblocker.app.data.model.BlockRule
import com.websiteblocker.app.domain.AppBlockingPolicy
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
                listOf(rule("instagram"), rule("x"), rule("facebook")),
                now,
            )
        assertTrue("com.instagram.android" in packages)
        assertTrue("com.instagram.lite" in packages)
        assertTrue("com.twitter.android" in packages)
        assertTrue("com.facebook.katana" in packages)
        assertTrue("com.facebook.lite" in packages)
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
}
