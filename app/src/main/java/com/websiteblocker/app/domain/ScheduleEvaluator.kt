package com.websiteblocker.app.domain

import com.websiteblocker.app.data.model.BlockRule
import java.time.LocalDate
import java.time.LocalTime
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
            .map { occurrence(rule, it, now) }
            .firstOrNull { !now.isBefore(it.start) && now.isBefore(it.end) }
    }

    fun nextOccurrence(rule: BlockRule, now: ZonedDateTime): ScheduleOccurrence? {
        if (!valid(rule)) return null
        return (0L..7L)
            .asSequence()
            .map { now.toLocalDate().plusDays(it) }
            .filter { selected(rule, it) }
            .map { occurrence(rule, it, now) }
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
                        .map { occurrence(rule, it, now) }
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

    private fun occurrence(
        rule: BlockRule,
        startDate: LocalDate,
        now: ZonedDateTime,
    ): ScheduleOccurrence {
        val start = startDate.atTime(rule.startMinute.toTime()).atZone(now.zone)
        val endDate =
            if (rule.startMinute < rule.endMinute) startDate else startDate.plusDays(1)
        val end = endDate.atTime(rule.endMinute.toTime()).atZone(now.zone)
        return ScheduleOccurrence(rule, start, end)
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
