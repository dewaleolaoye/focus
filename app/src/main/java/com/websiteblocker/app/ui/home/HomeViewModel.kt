package com.websiteblocker.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.websiteblocker.app.data.model.BlockRule
import com.websiteblocker.app.data.repository.BlockRuleRepository
import com.websiteblocker.app.domain.ScheduleEvaluator
import com.websiteblocker.app.domain.ServiceCatalog
import com.websiteblocker.app.vpn.ProtectionController
import com.websiteblocker.app.vpn.ProtectionPhase
import com.websiteblocker.app.vpn.ProtectionState
import java.time.ZonedDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class RuleUiStatus {
    ACTIVE,
    SCHEDULED,
    DISABLED,
    PROTECTION_OFF,
    NEEDS_ATTENTION,
    STARTING,
}

enum class DashboardStatus {
    NO_SCHEDULES,
    ACTIVE,
    IDLE,
    OFF,
    NEEDS_ATTENTION,
    STARTING,
}

data class RuleRow(
    val rule: BlockRule,
    val status: RuleUiStatus,
    val activeUntil: ZonedDateTime? = null,
    val nextStart: ZonedDateTime? = null,
)

data class HomeState(
    val rows: List<RuleRow> = emptyList(),
    val protection: ProtectionState = ProtectionState(),
    val dashboardStatus: DashboardStatus = DashboardStatus.NO_SCHEDULES,
    val configuredTargetCount: Int = 0,
    val activeTargetCount: Int = 0,
    val currentBlockingEnd: ZonedDateTime? = null,
    val nextBlockingStart: ZonedDateTime? = null,
    val nextTargetNames: List<String> = emptyList(),
    val now: ZonedDateTime = ZonedDateTime.now(),
    val loading: Boolean = true,
    val error: String? = null,
)

object HomeStateFactory {
    fun create(
        rules: List<BlockRule>,
        protection: ProtectionState,
        now: ZonedDateTime,
        error: String? = null,
    ): HomeState {
        val groups = rules.groupBy(::targetKey)
        val activeKeys =
            groups.filterValues { targetRules ->
                targetRules.any { ScheduleEvaluator.isActive(it, now) }
            }.keys
        val enabledRules = rules.filter { it.enabled }
        val currentEnd =
            if (protection.phase == ProtectionPhase.ON)
                ScheduleEvaluator.continuousActiveEnd(enabledRules, now)
            else null
        val nextOccurrence = ScheduleEvaluator.nextOccurrence(enabledRules, now)
        val nextStart = nextOccurrence?.start
        val nextNames =
            if (nextStart == null) emptyList()
            else
                groups.values
                    .filter { targetRules ->
                        targetRules.mapNotNull { ScheduleEvaluator.nextOccurrence(it, now) }
                            .any { it.start == nextStart }
                    }
                    .map { ServiceCatalog.displayName(it.first()) }
                    .sorted()

        val dashboard =
            when {
                protection.phase == ProtectionPhase.NEEDS_REACTIVATION ->
                    DashboardStatus.NEEDS_ATTENTION
                protection.phase == ProtectionPhase.STARTING -> DashboardStatus.STARTING
                protection.phase == ProtectionPhase.OFF -> DashboardStatus.OFF
                rules.isEmpty() -> DashboardStatus.NO_SCHEDULES
                activeKeys.isNotEmpty() -> DashboardStatus.ACTIVE
                else -> DashboardStatus.IDLE
            }

        val rows =
            rules.map { rule ->
                val targetRules = groups.getValue(targetKey(rule))
                val active = ScheduleEvaluator.isActive(rule, now)
                val status =
                    when {
                        !rule.enabled -> RuleUiStatus.DISABLED
                        protection.phase == ProtectionPhase.NEEDS_REACTIVATION ->
                            RuleUiStatus.NEEDS_ATTENTION
                        protection.phase == ProtectionPhase.STARTING -> RuleUiStatus.STARTING
                        protection.phase == ProtectionPhase.OFF -> RuleUiStatus.PROTECTION_OFF
                        active -> RuleUiStatus.ACTIVE
                        else -> RuleUiStatus.SCHEDULED
                    }
                RuleRow(
                    rule = rule,
                    status = status,
                    activeUntil =
                        if (active && protection.phase == ProtectionPhase.ON)
                            ScheduleEvaluator.continuousActiveEnd(targetRules, now)
                        else null,
                    nextStart =
                        if (rule.enabled) ScheduleEvaluator.nextOccurrence(rule, now)?.start else null,
                )
            }

        return HomeState(
            rows = rows,
            protection = protection,
            dashboardStatus = dashboard,
            configuredTargetCount = groups.size,
            activeTargetCount = if (protection.phase == ProtectionPhase.ON) activeKeys.size else 0,
            currentBlockingEnd = currentEnd,
            nextBlockingStart = nextStart,
            nextTargetNames = nextNames,
            now = now,
            loading = false,
            error = error,
        )
    }

    private fun targetKey(rule: BlockRule): String = rule.serviceId ?: rule.domain
}

class HomeViewModel(
    private val repository: BlockRuleRepository,
    val protection: ProtectionController,
) : ViewModel() {
    private val error = MutableStateFlow<String?>(null)
    private val clock =
        flow {
            while (true) {
                emit(ZonedDateTime.now())
                delay(1_000)
            }
        }

    val state =
        combine(repository.rules, protection.state, clock, error, HomeStateFactory::create)
            .catch {
                emit(
                    HomeState(
                        loading = false,
                        error = "Could not load saved schedules. Close and reopen the app.",
                    )
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())

    fun setEnabled(rule: BlockRule, enabled: Boolean) = mutate {
        repository.setEnabled(rule.id, enabled)
    }

    fun delete(rule: BlockRule) = mutate { repository.delete(rule.id) }

    fun dismissError() {
        error.value = null
    }

    private fun mutate(action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                error.value = "Could not update this schedule. Please try again."
            }
        }
    }
}
