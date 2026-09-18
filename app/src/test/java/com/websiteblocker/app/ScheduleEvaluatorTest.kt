package com.websiteblocker.app

import com.websiteblocker.app.data.model.BlockRule
import com.websiteblocker.app.domain.ScheduleEvaluator
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class ScheduleEvaluatorTest {
    private val same =
        BlockRule(domain = "x.com", startMinute = 540, endMinute = 1020, daysMask = 1)
    private val night = same.copy(startMinute = 1320, endMinute = 420)

    private fun active(rule: BlockRule, date: String) =
        ScheduleEvaluator.isActive(rule, ZonedDateTime.parse(date))

    @Test
    fun sameDayActive() {
        assertTrue(active(same, "2026-09-14T12:00:00Z"))
    }

    @Test
    fun sameDayInactive() {
        assertFalse(active(same, "2026-09-14T18:00:00Z"))
    }

    @Test
    fun overnightBeforeMidnight() {
        assertTrue(active(night, "2026-09-14T23:00:00Z"))
    }

    @Test
    fun overnightAfterMidnight() {
        assertTrue(active(night, "2026-09-15T06:59:59Z"))
    }

    @Test
    fun previousDayRequired() {
        assertFalse(active(night, "2026-09-14T06:00:00Z"))
        assertFalse(active(night, "2026-09-16T06:00:00Z"))
    }

    @Test
    fun selectedWeekdays() {
        assertFalse(active(same, "2026-09-15T12:00:00Z"))
        assertTrue(active(same.copy(daysMask = 127), "2026-09-20T12:00:00Z"))
    }

    @Test
    fun disabled() {
        assertFalse(active(same.copy(enabled = false), "2026-09-14T12:00:00Z"))
    }

    @Test
    fun startInclusiveEndExclusive() {
        assertTrue(active(same, "2026-09-14T09:00:00Z"))
        assertFalse(active(same, "2026-09-14T08:59:59Z"))
        assertFalse(active(same, "2026-09-14T17:00:00Z"))
        assertTrue(active(night, "2026-09-14T22:00:00Z"))
        assertFalse(active(night, "2026-09-15T07:00:00Z"))
    }

    @Test
    fun sundayWrapsToMonday() {
        assertTrue(active(night.copy(daysMask = 64), "2026-09-14T02:00:00Z"))
    }

    @Test
    fun equalTimesAndNoDaysInactive() {
        assertFalse(active(same.copy(endMinute = 540), "2026-09-14T09:00:00Z"))
        assertFalse(active(same.copy(daysMask = 0), "2026-09-14T12:00:00Z"))
    }

    @Test
    fun localTimeZone() {
        val instant = Instant.parse("2026-09-14T08:30:00Z")
        assertFalse(ScheduleEvaluator.isActive(same, instant.atZone(ZoneId.of("UTC"))))
        assertTrue(ScheduleEvaluator.isActive(same, instant.atZone(ZoneId.of("Africa/Lagos"))))
    }

    @Test
    fun daylightSavingOverlap() {
        val rule = night.copy(daysMask = 64, startMinute = 60, endMinute = 150)
        listOf("2026-11-01T05:30:00Z", "2026-11-01T06:30:00Z").forEach {
            assertTrue(
                ScheduleEvaluator.isActive(
                    rule,
                    Instant.parse(it).atZone(ZoneId.of("America/New_York")),
                )
            )
        }
    }
}
