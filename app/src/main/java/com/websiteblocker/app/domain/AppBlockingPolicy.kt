package com.websiteblocker.app.domain

import com.websiteblocker.app.data.model.BlockRule
import java.time.ZonedDateTime

object AppBlockingPolicy {
    fun activePackages(rules: List<BlockRule>, now: ZonedDateTime): Set<String> =
        rules
            .asSequence()
            .filter { it.enabled && ScheduleEvaluator.isActive(it, now) }
            .mapNotNull { ServiceCatalog.find(it.serviceId) }
            .flatMap { it.androidPackages.asSequence() }
            .toSet()
}
