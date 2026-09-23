package com.usefocus.app.ui.rule

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.usefocus.app.data.model.BlockRule
import com.usefocus.app.data.repository.BlockRuleRepository
import com.usefocus.app.domain.DomainNormalizer
import com.usefocus.app.domain.ServiceCatalog
import com.usefocus.app.vpn.ProtectionController
import com.usefocus.app.vpn.ProtectionPhase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class RuleCompletion {
    SAVED,
    DELETED,
}

enum class TargetTab {
    APP,
    WEBSITE,
}

data class RuleState(
    val domain: String = "",
    val serviceId: String? = null,
    val packageName: String? = null,
    val appDisplayName: String? = null,
    val customSelected: Boolean = false,
    val start: Int = 22 * 60,
    val end: Int = 7 * 60,
    val days: Int = 127,
    val enabled: Boolean = true,
    val protectionPhase: ProtectionPhase = ProtectionPhase.OFF,
    val loading: Boolean = false,
    val saving: Boolean = false,
    val completion: RuleCompletion? = null,
    val domainError: String? = null,
    val targetError: String? = null,
    val timeError: String? = null,
    val daysError: String? = null,
    val error: String? = null,
) {
    val canSave: Boolean
        get() =
            !loading &&
                !saving &&
                (serviceId != null ||
                    packageName != null ||
                    (customSelected && runCatching { DomainNormalizer.normalize(domain) }.isSuccess)) &&
                start != end &&
                days != 0

    val targetTab: TargetTab
        get() = if (customSelected) TargetTab.WEBSITE else TargetTab.APP
}

/** Restores an unfinished edit without replacing explicit cleared values with database values. */
internal fun RuleState.restore(handle: SavedStateHandle, rule: BlockRule?): RuleState =
    copy(
        domain = handle["domain"] ?: rule?.domain.orEmpty(),
        // A saved null means the user explicitly cleared the app target.
        serviceId = if (handle.contains("serviceId")) handle["serviceId"] else rule?.serviceId,
        packageName = if (handle.contains("packageName")) handle["packageName"] else rule?.packageName,
        appDisplayName =
            if (handle.contains("appDisplayName")) handle["appDisplayName"] else rule?.appDisplayName,
        customSelected =
            handle["customSelected"] ?: (rule != null && rule.serviceId == null && rule.packageName == null),
        start = handle["start"] ?: rule?.startMinute ?: 1320,
        end = handle["end"] ?: rule?.endMinute ?: 420,
        days = handle["days"] ?: rule?.daysMask ?: 127,
        enabled = handle["enabled"] ?: rule?.enabled ?: true,
        loading = false,
    )

class RuleViewModel(
    private val id: Long,
    private val repository: BlockRuleRepository,
    private val protection: ProtectionController,
    private val handle: SavedStateHandle,
) : ViewModel() {
    private val mutable =
        MutableStateFlow(
            RuleState(
                loading = id != 0L,
                protectionPhase = protection.state.value.phase,
            )
        )
    val state = mutable.asStateFlow()

    init {
        viewModelScope.launch {
            protection.state.collect { protectionState ->
                mutable.update { it.copy(protectionPhase = protectionState.phase) }
            }
        }
        viewModelScope.launch {
            try {
                val rule =
                    if (id != 0L) repository.find(id) ?: error("This schedule no longer exists.")
                    else null
                mutable.update {
                    it.restore(handle, rule)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutable.update {
                    it.copy(
                        loading = false,
                        error = "Could not load this schedule. Return home and try again.",
                    )
                }
            }
        }
    }

    fun domain(value: String) {
        handle["domain"] = value
        mutable.update { it.copy(domain = value, domainError = null, error = null) }
    }

    fun service(id: String) {
        val profile = ServiceCatalog.find(id) ?: return
        handle["serviceId"] = id
        handle["packageName"] = null
        handle["appDisplayName"] = null
        handle["customSelected"] = false
        handle["domain"] = profile.primaryDomain
        mutable.update {
            it.copy(
                domain = profile.primaryDomain,
                serviceId = id,
                packageName = null,
                appDisplayName = null,
                customSelected = false,
                domainError = null,
                targetError = null,
                error = null,
            )
        }
    }

    fun installedApp(packageName: String, label: String) {
        handle["serviceId"] = null
        handle["packageName"] = packageName
        handle["appDisplayName"] = label
        handle["customSelected"] = false
        handle["domain"] = ""
        mutable.update {
            it.copy(
                domain = "",
                serviceId = null,
                packageName = packageName,
                appDisplayName = label,
                customSelected = false,
                domainError = null,
                targetError = null,
                error = null,
            )
        }
    }

    fun selectTab(tab: TargetTab) {
        if (tab == TargetTab.WEBSITE) {
            val domain =
                if (mutable.value.serviceId != null || mutable.value.packageName != null) ""
                else mutable.value.domain
            handle["serviceId"] = null
            handle["packageName"] = null
            handle["appDisplayName"] = null
            handle["customSelected"] = true
            handle["domain"] = domain
            mutable.update {
                it.copy(
                    domain = domain,
                    serviceId = null,
                    packageName = null,
                    appDisplayName = null,
                    customSelected = true,
                    domainError = null,
                    targetError = null,
                    error = null,
                )
            }
        } else {
            handle["customSelected"] = false
            handle["domain"] = ""
            mutable.update {
                it.copy(
                    customSelected = false,
                    domain = "",
                    domainError = null,
                    targetError = null,
                    error = null,
                )
            }
        }
    }

    fun customWebsite() {
        selectTab(TargetTab.WEBSITE)
    }

    fun clearTarget() {
        handle["serviceId"] = null
        handle["packageName"] = null
        handle["appDisplayName"] = null
        handle["customSelected"] = false
        handle["domain"] = ""
        mutable.update {
            it.copy(
                domain = "",
                serviceId = null,
                packageName = null,
                appDisplayName = null,
                customSelected = false,
                domainError = null,
                targetError = null,
            )
        }
    }

    fun start(value: Int) {
        handle["start"] = value
        mutable.update { it.copy(start = value, timeError = null, error = null) }
    }

    fun end(value: Int) {
        handle["end"] = value
        mutable.update { it.copy(end = value, timeError = null, error = null) }
    }

    fun days(value: Int) {
        handle["days"] = value
        mutable.update { it.copy(days = value, daysError = null, error = null) }
    }

    fun enabled(value: Boolean) {
        handle["enabled"] = value
        mutable.update { it.copy(enabled = value, error = null) }
    }

    fun save() {
        val value = mutable.value
        if (value.loading || value.saving) return
        val profile = ServiceCatalog.find(value.serviceId)
        val targetError = when {
            value.targetTab == TargetTab.APP && profile == null && value.packageName == null ->
                "Choose a popular app or an installed app."
            value.targetTab == TargetTab.WEBSITE && value.domain.isBlank() ->
                "Enter a website domain to block."
            else -> null
        }
        val domain =
            if (profile != null) Result.success(profile.primaryDomain)
            else if (value.packageName != null) Result.success("")
            else if (value.customSelected) runCatching { DomainNormalizer.normalize(value.domain) }
            else Result.failure(IllegalArgumentException("Choose what you want to block."))
        val timeError =
            if (value.start == value.end) "Start and stop times must be different." else null
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
                        id = id,
                        domain = domain.getOrThrow(),
                        startMinute = value.start,
                        endMinute = value.end,
                        daysMask = value.days,
                        enabled = value.enabled,
                        serviceId = profile?.id,
                        packageName = value.packageName,
                        appDisplayName = value.appDisplayName,
                    )
                )
                mutable.update { it.copy(saving = false, completion = RuleCompletion.SAVED) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutable.update {
                    it.copy(
                        saving = false,
                        error = "Could not save this schedule. Check the details and try again.",
                    )
                }
            }
        }
    }

    fun delete() {
        if (id == 0L || mutable.value.saving) return
        mutable.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                repository.delete(id)
                mutable.update { it.copy(saving = false, completion = RuleCompletion.DELETED) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                mutable.update {
                    it.copy(saving = false, error = "Could not delete this schedule. Try again.")
                }
            }
        }
    }
}
