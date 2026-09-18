package com.websiteblocker.app.ui

import java.time.DayOfWeek
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

fun formatTime(minute: Int, is24Hour: Boolean): String =
    LocalTime.of(minute / 60, minute % 60)
        .format(
            DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm" else "h:mm a", Locale.getDefault())
        )

fun formatDays(mask: Int): String =
    if (mask == 127) "Every day"
    else
        DayOfWeek.entries
            .filter { mask and (1 shl (it.value - 1)) != 0 }
            .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
