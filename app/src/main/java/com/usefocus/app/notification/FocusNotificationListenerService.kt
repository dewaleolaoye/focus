package com.usefocus.app.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.usefocus.app.BlockerApplication
import com.usefocus.app.data.model.BlockRule
import com.usefocus.app.domain.AppBlockingPolicy
import com.usefocus.app.vpn.ProtectionPhase
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Optional notification listener that dismisses notifications from blocked apps
 * during their scheduled quiet hours. Reads only the posting package name and notification key;
 * does not inspect notification text, title or contents.
 */
class FocusNotificationListenerService : NotificationListenerService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observation: Job? = null
    private var rules: List<BlockRule> = emptyList()
    private val app get() = application as? BlockerApplication

    override fun onListenerConnected() {
        super.onListenerConnected()
        NotificationAccess.connected.value = true
        observation?.cancel()
        val currentApp = app ?: return
        observation = scope.launch {
            launch {
                currentApp.repository.rules.collect { rules = it }
            }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        NotificationAccess.connected.value = false
        observation?.cancel()
        observation = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        val currentApp = app ?: return
        if (!currentApp.disclosures.state.value.notificationAccepted) return

        val targetPkg = sbn.packageName ?: return
        if (targetPkg == packageName) return // Never dismiss Focus's own notifications

        val protectionEnabled = currentApp.protection.state.value.phase == ProtectionPhase.ON
        val activeRule = AppBlockingPolicy.blockingRule(targetPkg, rules, ZonedDateTime.now(), protectionEnabled)
        if (activeRule != null) {
            cancelNotification(sbn.key)
        }
    }
}
