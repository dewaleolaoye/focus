package com.websiteblocker.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.websiteblocker.app.data.model.BlockRule
import com.websiteblocker.app.data.repository.BlockRuleRepository
import com.websiteblocker.app.domain.ScheduleEvaluator
import com.websiteblocker.app.vpn.ProtectionController
import com.websiteblocker.app.vpn.ProtectionState
import java.time.ZonedDateTime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class RuleRow(val rule: BlockRule, val status: String)

data class HomeState(
    val rows: List<RuleRow> = emptyList(),
    val protection: ProtectionState = ProtectionState(),
    val loading: Boolean = true,
    val error: String? = null,
)

class HomeViewModel(
    private val repository: BlockRuleRepository,
    val protection: ProtectionController,
) : ViewModel() {
    private val error = MutableStateFlow<String?>(null)
    private val clock = flow {
        while (true) {
            emit(ZonedDateTime.now())
            delay(1000)
        }
    }
    val state =
        combine(repository.rules, protection.state, clock, error) { rules, status, now, failure ->
                HomeState(
                    rules.map {
                        RuleRow(
                            it,
                            if (!it.enabled) "Disabled"
                            else if (ScheduleEvaluator.isActive(it, now)) "Active schedule"
                            else "Scheduled",
                        )
                    },
                    status,
                    false,
                    failure,
                )
            }
            .catch {
                emit(
                    HomeState(
                        loading = false,
                        error = "Could not load saved rules. Close and reopen the app.",
                    )
                )
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeState())

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
                error.value = "Could not update this rule. Please try again."
            }
        }
    }
}
