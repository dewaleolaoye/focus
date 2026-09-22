package com.usefocus.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.usefocus.app.BlockerApplication
import com.usefocus.app.data.model.BlockRule
import com.usefocus.app.domain.AppBlockingPolicy
import com.usefocus.app.domain.ServiceCatalog
import java.time.ZonedDateTime
import kotlinx.coroutines.*

/** Foreground enforcement in both distributions, independent of the website DNS tunnel. */
class AppBlockAccessibilityService : AccessibilityService() {
    // Events, rule updates and the schedule clock share the main thread. No window content is read.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var monitoring: Job? = null
    private var rules: List<BlockRule> = emptyList()
    private var foregroundPackage: String? = null
    private var lastActionAt: Long? = null
    private var lastToastAt: Long? = null
    private val app get() = application as BlockerApplication

    override fun onServiceConnected() {
        super.onServiceConnected()
        AppBlockingAccess.connected.value = true
        monitoring?.cancel()
        monitoring = scope.launch {
            launch {
                app.repository.rules.collect {
                    rules = it
                    enforce()
                }
            }
            launch { app.protection.state.collect { enforce() } }
            launch {
                app.disclosures.state.collect {
                    if (!it.accessibilityAccepted) foregroundPackage = null
                    enforce()
                }
            }
            // A schedule must also start while an app is already open and produces no new events.
            // Re-evaluate wall time (including timezone changes) without keeping a wake lock.
            while (isActive) {
                enforce()
                delay(500)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!app.disclosures.state.value.accessibilityAccepted) {
            foregroundPackage = null
            return
        }
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val target = event.packageName?.toString() ?: return
        if (foregroundPackage != target) lastActionAt = null
        // Track allowed apps and Home too, so a later schedule cannot eject an unrelated app.
        foregroundPackage = target
        enforce()
    }

    private fun enforce() {
        val rule = AppBlockingPolicy.blockingRule(
            foregroundPackage, rules, ZonedDateTime.now(), app.protection.desired && app.disclosures.state.value.accessibilityAccepted,
        ) ?: return
        val elapsed = SystemClock.elapsedRealtime()
        if (lastActionAt?.let { elapsed - it < 500 } == true) return
        lastActionAt = elapsed
        // Retry on the clock if Android temporarily rejects Home. Never consume the event forever.
        if (!performGlobalAction(GLOBAL_ACTION_HOME)) return
        // Home may not emit another window-state event immediately (launcher transitions,
        // animations). Do not keep acting on the package we just dismissed. A subsequent
        // launch supplies a fresh event; rejected Home actions still retry on the clock.
        foregroundPackage = null
        if (lastToastAt?.let { elapsed - it < 2_000 } != true) {
            lastToastAt = elapsed
            Toast.makeText(
                this, "${ServiceCatalog.displayName(rule)} is blocked during quiet hours",
                Toast.LENGTH_SHORT,
            ).show()
        }
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        AppBlockingAccess.connected.value = false
        monitoring?.cancel()
        foregroundPackage = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        AppBlockingAccess.connected.value = false
        scope.cancel()
        super.onDestroy()
    }
}
