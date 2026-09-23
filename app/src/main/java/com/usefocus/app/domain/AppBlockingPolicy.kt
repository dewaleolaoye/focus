package com.usefocus.app.domain

import com.usefocus.app.data.model.BlockRule
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
            .flatMap { rule ->
                if (rule.packageName != null) sequenceOf(rule.packageName)
                else ServiceCatalog.find(rule.serviceId)?.androidPackages?.asSequence() ?: emptySequence()
            }
            .toSet()
}
