package com.websiteblocker.app.ui.rule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.websiteblocker.app.data.model.BlockRule
import com.websiteblocker.app.data.repository.BlockRuleRepository
import com.websiteblocker.app.domain.DomainNormalizer
import com.websiteblocker.app.domain.ServiceCatalog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class RuleState(
    val domain: String = "",
    val serviceId: String? = null,
    val customSelected: Boolean = false,
    val start: Int = 22 * 60,
    val end: Int = 7 * 60,
    val days: Int = 127,
    val enabled: Boolean = true,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val saved: Boolean = false,
    val domainError: String? = null,
    val targetError: String? = null,
    val timeError: String? = null,
    val daysError: String? = null,
    val error: String? = null,
)

class RuleViewModel(
    private val id: Long,
    private val repository: BlockRuleRepository,
    private val handle: SavedStateHandle,
) : ViewModel() {
    private val mutable = MutableStateFlow(RuleState(loading = id != 0L))
    val state = mutable.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val rule =
                    if (id != 0L) repository.find(id) ?: error("This rule no longer exists.")
                    else null
                mutable.value =
                    RuleState(
                        domain = handle["domain"] ?: rule?.domain.orEmpty(),
                        serviceId = handle["serviceId"] ?: rule?.serviceId,
                        customSelected =
                            handle["customSelected"] ?: (rule != null && rule.serviceId == null),
                        start = handle["start"] ?: rule?.startMinute ?: 1320,
                        end = handle["end"] ?: rule?.endMinute ?: 420,
                        days = handle["days"] ?: rule?.daysMask ?: 127,
                        enabled = handle["enabled"] ?: rule?.enabled ?: true,
                    )
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutable.value =
                    RuleState(error = "Could not load this rule. Return home and try again.")
            }
        }
    }

    fun domain(value: String) {
        handle["domain"] = value
        mutable.update { it.copy(domain = value, domainError = null) }
    }

    fun service(id: String) {
        val profile = ServiceCatalog.find(id) ?: return
        handle["serviceId"] = id
        handle["customSelected"] = false
        handle["domain"] = profile.primaryDomain
        mutable.update {
            it.copy(
                domain = profile.primaryDomain,
                serviceId = id,
                customSelected = false,
                domainError = null,
                targetError = null,
            )
        }
    }

    fun customWebsite() {
        val domain = if (mutable.value.serviceId != null) "" else mutable.value.domain
        handle["serviceId"] = null
        handle["customSelected"] = true
        handle["domain"] = domain
        mutable.update {
            it.copy(
                domain = domain,
                serviceId = null,
                customSelected = true,
                domainError = null,
                targetError = null,
            )
        }
    }

    fun start(value: Int) {
        handle["start"] = value
        mutable.update { it.copy(start = value, timeError = null) }
    }

    fun end(value: Int) {
        handle["end"] = value
        mutable.update { it.copy(end = value, timeError = null) }
    }

    fun days(value: Int) {
        handle["days"] = value
        mutable.update { it.copy(days = value, daysError = null) }
    }

    fun enabled(value: Boolean) {
        handle["enabled"] = value
        mutable.update { it.copy(enabled = value) }
    }

    fun save() {
        val value = mutable.value
        if (value.loading || value.saving) return
        val profile = ServiceCatalog.find(value.serviceId)
        val targetError =
            if (profile == null && !value.customSelected) "Choose a service or custom website."
            else null
        val domain =
            if (profile != null) Result.success(profile.primaryDomain)
            else if (value.customSelected) runCatching { DomainNormalizer.normalize(value.domain) }
            else Result.failure(IllegalArgumentException("Choose what you want to block."))
        val timeError =
            if (value.start == value.end) "Start and end times must be different." else null
        val daysError = if (value.days == 0) "Select at least one day." else null
        mutable.update {
            it.copy(
                domainError = domain.exceptionOrNull()?.message,
                targetError = targetError,
                timeError = timeError,
                daysError = daysError,
                error = null,
            )
        }
        if (domain.isFailure || targetError != null || timeError != null || daysError != null) return
        mutable.update { it.copy(saving = true) }
        viewModelScope.launch {
            try {
                repository.save(
                    BlockRule(
                        id,
                        domain.getOrThrow(),
                        value.start,
                        value.end,
                        value.days,
                        value.enabled,
                        profile?.id,
                    )
                )
                mutable.update { it.copy(saving = false, saved = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutable.update {
                    it.copy(saving = false, error = "Could not save the rule. Please try again.")
                }
            }
        }
    }
}
