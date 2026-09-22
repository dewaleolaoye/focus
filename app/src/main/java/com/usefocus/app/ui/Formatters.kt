package com.usefocus.app.ui

import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

fun formatTime(minute: Int, is24Hour: Boolean): String =
    LocalTime.of(minute / 60, minute % 60)
        .format(
            DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", Locale.getDefault())
        )

fun formatDays(mask: Int): String =
    when (mask) {
        127 -> "Every day"
        31 -> "Weekdays"
        96 -> "Weekends"
        else ->
            DayOfWeek.entries
                .filter { mask and (1 shl (it.value - 1)) != 0 }
                .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
    }

fun formatDuration(from: ZonedDateTime, until: ZonedDateTime): String {
    val totalMinutes = ((Duration.between(from, until).seconds.coerceAtLeast(0) + 59) / 60)
    val days = totalMinutes / (24 * 60)
    val hours = totalMinutes % (24 * 60) / 60
    val minutes = totalMinutes % 60
    return buildList {
            if (days > 0) add("${days}d")
            if (hours > 0) add("${hours}h")
            if (minutes > 0 || isEmpty()) add("${minutes}m")
        }
        .take(2)
        .joinToString(" ")
}

fun formatUpcoming(start: ZonedDateTime, now: ZonedDateTime, is24Hour: Boolean): String {
    val time = formatTime(start.hour * 60 + start.minute, is24Hour)
    val days = java.time.temporal.ChronoUnit.DAYS.between(now.toLocalDate(), start.toLocalDate())
    return when (days) {
        0L -> "at $time"
        1L -> "tomorrow at $time"
        else ->
            "${start.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())} at $time"
    }
}
