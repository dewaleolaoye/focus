package com.usefocus.app.data.repository

import com.usefocus.app.data.local.BlockRuleDao
import com.usefocus.app.data.model.BlockRule
import com.usefocus.app.domain.DomainNormalizer
import com.usefocus.app.domain.ServiceCatalog

class BlockRuleRepository(private val dao: BlockRuleDao) {
    val rules = dao.observeRules()

    suspend fun find(id: Long) = dao.find(id)

    suspend fun save(rule: BlockRule) {
        require(
            rule.startMinute in 0..1439 &&
                rule.endMinute in 0..1439 &&
                rule.startMinute != rule.endMinute
        ) {
            "Choose different start and end times."
        }
        require(rule.daysMask in 1..127) { "Select at least one day." }
        val profile = ServiceCatalog.find(rule.serviceId)
        require(rule.serviceId == null || profile != null) { "Choose a supported service." }
        require(profile != null || !rule.packageName.isNullOrBlank() || rule.domain.isNotBlank()) {
            "Specify an app or website to block."
        }
        val normalizedDomain = when {
            profile != null -> DomainNormalizer.normalize(profile.primaryDomain)
            !rule.packageName.isNullOrBlank() -> ""
            else -> DomainNormalizer.normalize(rule.domain)
        }
        val normalized =
            rule.copy(
                domain = normalizedDomain,
                serviceId = profile?.id,
                packageName = rule.packageName?.trim()?.ifBlank { null },
                appDisplayName = rule.appDisplayName?.trim()?.ifBlank { null },
            )
        if (rule.id == 0L) dao.insert(normalized)
        else
            check(dao.update(normalized) == 1) {
                "This rule was deleted. Return home and add it again."
            }
    }

    suspend fun setEnabled(id: Long, enabled: Boolean) = dao.setEnabled(id, enabled)

    suspend fun delete(id: Long) = dao.delete(id)
}
