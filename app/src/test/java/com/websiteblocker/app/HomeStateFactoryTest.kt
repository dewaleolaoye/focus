package com.websiteblocker.app

import com.websiteblocker.app.data.model.BlockRule
import com.websiteblocker.app.ui.home.DashboardStatus
import com.websiteblocker.app.ui.home.HomeStateFactory
import com.websiteblocker.app.ui.home.RuleUiStatus
import com.websiteblocker.app.vpn.ProtectionPhase
import com.websiteblocker.app.vpn.ProtectionState
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeStateFactoryTest {
    private val mondayNoon = ZonedDateTime.parse("2026-09-14T12:00:00Z")
    private val on = ProtectionState(ProtectionPhase.ON, "VPN protection is on")
    private val instagram =
        BlockRule(
            id = 1,
            domain = "instagram.com",
            startMinute = 9 * 60,
            endMinute = 17 * 60,
            daysMask = 1,
            serviceId = "instagram",
        )

    @Test
    fun emptyOperationalDashboardDoesNotClaimTargetsAreProtected() {
        val state = HomeStateFactory.create(emptyList(), on, mondayNoon)
        assertEquals(DashboardStatus.NO_SCHEDULES, state.dashboardStatus)
        assertEquals(0, state.configuredTargetCount)
        assertEquals(0, state.activeTargetCount)
    }

    @Test
    fun activeDashboardCountsTargetsAndProvidesTheRealEnd() {
        val duplicate = instagram.copy(id = 2, startMinute = 10 * 60, endMinute = 18 * 60)
        val state = HomeStateFactory.create(listOf(instagram, duplicate), on, mondayNoon)
        assertEquals(DashboardStatus.ACTIVE, state.dashboardStatus)
        assertEquals(1, state.configuredTargetCount)
        assertEquals(1, state.activeTargetCount)
        assertEquals(ZonedDateTime.parse("2026-09-14T18:00:00Z"), state.currentBlockingEnd)
        assertTrue(state.rows.all { it.status == RuleUiStatus.ACTIVE })
    }

    @Test
    fun idleDashboardProvidesNextStartAndTargets() {
        val future = instagram.copy(startMinute = 20 * 60, endMinute = 22 * 60)
        val state = HomeStateFactory.create(listOf(future), on, mondayNoon)
        assertEquals(DashboardStatus.IDLE, state.dashboardStatus)
        assertEquals(ZonedDateTime.parse("2026-09-14T20:00:00Z"), state.nextBlockingStart)
        assertEquals(listOf("Instagram"), state.nextTargetNames)
        assertEquals(RuleUiStatus.SCHEDULED, state.rows.single().status)
    }

    @Test
    fun protectionOffOverridesAnEnabledSchedule() {
        val state =
            HomeStateFactory.create(
                listOf(instagram),
                ProtectionState(),
                mondayNoon,
            )
        assertEquals(DashboardStatus.OFF, state.dashboardStatus)
        assertEquals(0, state.activeTargetCount)
        assertEquals(RuleUiStatus.PROTECTION_OFF, state.rows.single().status)
    }

    @Test
    fun enforcementFailureIsExposedAsNeedsAttention() {
        val state =
            HomeStateFactory.create(
                listOf(instagram),
                ProtectionState(ProtectionPhase.NEEDS_REACTIVATION, "VPN permission revoked"),
                mondayNoon,
            )
        assertEquals(DashboardStatus.NEEDS_ATTENTION, state.dashboardStatus)
        assertEquals(RuleUiStatus.NEEDS_ATTENTION, state.rows.single().status)
    }

    @Test
    fun disabledScheduleHasNoUpcomingOccurrence() {
        val state = HomeStateFactory.create(listOf(instagram.copy(enabled = false)), on, mondayNoon)
        assertEquals(DashboardStatus.IDLE, state.dashboardStatus)
        assertEquals(RuleUiStatus.DISABLED, state.rows.single().status)
        assertEquals(null, state.nextBlockingStart)
        assertNotNull(state.rows.single().rule)
    }
}
