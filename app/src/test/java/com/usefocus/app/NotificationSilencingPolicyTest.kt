package com.usefocus.app

import com.usefocus.app.data.model.BlockRule
import com.usefocus.app.domain.AppBlockingPolicy
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test

class NotificationSilencingPolicyTest {
    private val zone = ZoneId.of("UTC")
    private val now = ZonedDateTime.of(2026, 9, 22, 14, 0, 0, 0, zone) // Tuesday 14:00

    private val activeInstagramRule = BlockRule(
        id = 1,
        domain = "instagram.com",
        serviceId = "instagram",
        enabled = true,
        startMinute = 9 * 60,
        endMinute = 17 * 60,
        daysMask = 0b1111111,
    )

    private val inactiveRule = BlockRule(
        id = 2,
        domain = "youtube.com",
        serviceId = "youtube",
        enabled = true,
        startMinute = 18 * 60,
        endMinute = 20 * 60,
        daysMask = 0b1111111,
    )

    @Test
    fun silencesNotificationForActiveBlockedPackageWhenProtectionOn() {
        val rules = listOf(activeInstagramRule, inactiveRule)
        val rule = AppBlockingPolicy.blockingRule(
            packageName = "com.instagram.android",
            rules = rules,
            now = now,
            protectionEnabled = true,
        )
        assertNotNull(rule)
        assertEquals("instagram", rule?.serviceId)
    }

    @Test
    fun doesNotSilenceWhenScheduleIsInactive() {
        val rules = listOf(activeInstagramRule, inactiveRule)
        val rule = AppBlockingPolicy.blockingRule(
            packageName = "com.google.android.youtube",
            rules = rules,
            now = now,
            protectionEnabled = true,
        )
        assertNull(rule)
    }

    @Test
    fun doesNotSilenceUnrelatedApp() {
        val rules = listOf(activeInstagramRule)
        val rule = AppBlockingPolicy.blockingRule(
            packageName = "com.android.calculator2",
            rules = rules,
            now = now,
            protectionEnabled = true,
        )
        assertNull(rule)
    }

    @Test
    fun doesNotSilenceWhenProtectionIsOff() {
        val rules = listOf(activeInstagramRule)
        val rule = AppBlockingPolicy.blockingRule(
            packageName = "com.instagram.android",
            rules = rules,
            now = now,
            protectionEnabled = false,
        )
        assertNull(rule)
    }
}
