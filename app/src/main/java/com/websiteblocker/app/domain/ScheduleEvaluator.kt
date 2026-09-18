package com.websiteblocker.app.domain

import com.websiteblocker.app.data.model.BlockRule
import java.time.ZonedDateTime

object ScheduleEvaluator {
    fun isActive(rule: BlockRule, now: ZonedDateTime): Boolean {
        if (
            !rule.enabled ||
                rule.startMinute !in 0..1439 ||
                rule.endMinute !in 0..1439 ||
                rule.startMinute == rule.endMinute
        )
            return false
        val minute = now.hour * 60 + now.minute
        fun selected(day: Int) = rule.daysMask and (1 shl (day - 1)) != 0
        return if (rule.startMinute < rule.endMinute)
            selected(now.dayOfWeek.value) && minute >= rule.startMinute && minute < rule.endMinute
        else
            (minute >= rule.startMinute && selected(now.dayOfWeek.value)) ||
                (minute < rule.endMinute && selected(now.minusDays(1).dayOfWeek.value))
    }
}
