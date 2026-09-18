package com.websiteblocker.app.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.websiteblocker.app.BlockerApplication
import com.websiteblocker.app.data.model.BlockRule
import com.websiteblocker.app.domain.ScheduleEvaluator
import com.websiteblocker.app.domain.ServiceCatalog
import java.time.ZonedDateTime
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest

/** Keeps a scheduled popular app out of the foreground; the DNS VPN blocks its web endpoints. */
class AppBlockAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile private var rules: List<BlockRule> = emptyList()
    private var lastBlockedPackage: String? = null
    private var lastBlockedAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        val app = application as BlockerApplication
        scope.launch { app.repository.rules.collectLatest { rules = it } }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val packageName = event.packageName?.toString() ?: return
        if (packageName == this.packageName) return
        val now = ZonedDateTime.now()
        val blockedRule =
            rules.firstOrNull { rule ->
                rule.enabled &&
                    ScheduleEvaluator.isActive(rule, now) &&
                    ServiceCatalog.matchesPackage(packageName, rule)
            } ?: return

        val elapsed = android.os.SystemClock.elapsedRealtime()
        if (packageName == lastBlockedPackage && elapsed - lastBlockedAt < 750) return
        lastBlockedPackage = packageName
        lastBlockedAt = elapsed
        performGlobalAction(GLOBAL_ACTION_HOME)
        val name = ServiceCatalog.displayName(blockedRule)
        Toast.makeText(this, "$name is blocked during quiet hours", Toast.LENGTH_SHORT).show()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
