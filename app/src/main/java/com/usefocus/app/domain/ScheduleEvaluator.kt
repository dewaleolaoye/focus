package com.usefocus.app.domain

import com.usefocus.app.data.model.BlockRule
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.ZonedDateTime

data class ScheduleOccurrence(
    val rule: BlockRule,
    val start: ZonedDateTime,
    val end: ZonedDateTime,
)

/**
 * Authoritative wall-clock schedule calculations used by enforcement and UI state derivation.
 * Selected days are the local days on which blocking begins.
 */
object ScheduleEvaluator {
    fun isActive(rule: BlockRule, now: ZonedDateTime): Boolean =
        currentOccurrence(rule, now) != null

    fun currentOccurrence(rule: BlockRule, now: ZonedDateTime): ScheduleOccurrence? {
        if (!valid(rule)) return null
        return listOf(now.toLocalDate().minusDays(1), now.toLocalDate())
            .asSequence()
            .filter { selected(rule, it) }
            .flatMap { occurrences(rule, it, now).asSequence() }
            .firstOrNull { !now.isBefore(it.start) && now.isBefore(it.end) }
    }

    fun nextOccurrence(rule: BlockRule, now: ZonedDateTime): ScheduleOccurrence? {
        if (!valid(rule)) return null
        // An overnight rule can resume in the repeated hour after its start day.
        return (-1L..7L)
            .asSequence()
            .map { now.toLocalDate().plusDays(it) }
            .filter { selected(rule, it) }
            .flatMap { occurrences(rule, it, now).asSequence() }
            .firstOrNull { it.start.isAfter(now) }
    }

    fun nextOccurrence(rules: Collection<BlockRule>, now: ZonedDateTime): ScheduleOccurrence? =
        rules.asSequence().mapNotNull { nextOccurrence(it, now) }.minByOrNull { it.start }

    /**
     * Returns the end of the continuous active interval formed by all overlapping/touching rules.
     * Pass rules for one target to calculate when that target becomes available, or all rules for
     * the dashboard's current blocking period.
     */
    fun continuousActiveEnd(
        rules: Collection<BlockRule>,
        now: ZonedDateTime,
    ): ZonedDateTime? {
        val occurrences =
            rules.asSequence()
                .filter(::valid)
                .flatMap { rule ->
                    (-1L..7L).asSequence()
                        .map { now.toLocalDate().plusDays(it) }
                        .filter { selected(rule, it) }
                        .flatMap { occurrences(rule, it, now).asSequence() }
                }
                .filter { it.end.isAfter(now) }
                .sortedBy { it.start }
                .toList()
        var end =
            occurrences
                .filter { !now.isBefore(it.start) && now.isBefore(it.end) }
                .maxOfOrNull { it.end }
                ?: return null
        for (candidate in occurrences) {
            if (candidate.start.isAfter(end)) break
            if (candidate.end.isAfter(end)) end = candidate.end
        }
        return end
    }

    private fun occurrences(
        rule: BlockRule,
        startDate: LocalDate,
        now: ZonedDateTime,
    ): List<ScheduleOccurrence> {
        val localStart = startDate.atTime(rule.startMinute.toTime())
        val endDate =
            if (rule.startMinute < rule.endMinute) startDate else startDate.plusDays(1)
        val localEnd = endDate.atTime(rule.endMinute.toTime())
        val zoneRules = now.zone.rules
        // Intersect the local interval with each constant-offset timeline segment. This
        // repeats overlapping clock minutes and skips nonexistent minutes without shifting
        // a boundary forward by the size of a DST gap.
        var cursor = localStart.toInstant(ZoneOffset.MAX)
        val limit = localEnd.toInstant(ZoneOffset.MIN)
        val result = mutableListOf<ScheduleOccurrence>()
        while (cursor.isBefore(limit)) {
            val offset = zoneRules.getOffset(cursor)
            val segmentEnd = zoneRules.nextTransition(cursor)?.instant?.let { minOf(it, limit) }
                ?: limit
            val start = maxOf(cursor, localStart.toInstant(offset))
            val end = minOf(segmentEnd, localEnd.toInstant(offset))
            if (start.isBefore(end)) {
                val previous = result.lastOrNull()
                if (previous != null && previous.end.toInstant() == start) {
                    result[result.lastIndex] = previous.copy(end = end.atZone(now.zone))
                } else {
                    result += ScheduleOccurrence(rule, start.atZone(now.zone), end.atZone(now.zone))
                }
            }
            cursor = segmentEnd
        }
        return result
    }

    private fun selected(rule: BlockRule, date: LocalDate): Boolean =
        rule.daysMask and (1 shl (date.dayOfWeek.value - 1)) != 0

    private fun valid(rule: BlockRule): Boolean =
        rule.enabled &&
            rule.daysMask in 1..127 &&
            rule.startMinute in 0..1439 &&
            rule.endMinute in 0..1439 &&
            rule.startMinute != rule.endMinute

    private fun Int.toTime(): LocalTime = LocalTime.of(this / 60, this % 60)
}
