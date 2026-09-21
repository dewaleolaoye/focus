package com.websiteblocker.app.domain

import com.websiteblocker.app.data.model.BlockRule
import java.time.ZonedDateTime

object AppBlockingPolicy {
    fun blockingRule(
        packageName: String?,
        rules: List<BlockRule>,
        now: ZonedDateTime,
        protectionEnabled: Boolean,
    ): BlockRule? {
        if (!protectionEnabled || packageName == null) return null
        return rules.firstOrNull {
            ServiceCatalog.matchesPackage(packageName, it) && ScheduleEvaluator.isActive(it, now)
        }
    }

    fun activePackages(rules: List<BlockRule>, now: ZonedDateTime): Set<String> =
        rules
            .asSequence()
            .filter { it.enabled && ScheduleEvaluator.isActive(it, now) }
            .mapNotNull { ServiceCatalog.find(it.serviceId) }
            .flatMap { it.androidPackages.asSequence() }
            .toSet()
}
