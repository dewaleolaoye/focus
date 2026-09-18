package com.websiteblocker.app.vpn

import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ProtectionPhase {
    OFF,
    STARTING,
    ON,
    NEEDS_REACTIVATION,
}

data class ProtectionState(
    val phase: ProtectionPhase = ProtectionPhase.OFF,
    val message: String = "Protection is off",
)

class ProtectionController(private val context: Context) {
    private val preferences = context.getSharedPreferences("protection", Context.MODE_PRIVATE)
    @Volatile private var silentRecoveryAttempted = false
    val desired
        get() = preferences.getBoolean("enabled", false)

    private val mutableState =
        MutableStateFlow(
            if (desired)
                ProtectionState(
                    ProtectionPhase.NEEDS_REACTIVATION,
                    "Protection requires reactivation",
                )
            else ProtectionState()
        )
    val state = mutableState.asStateFlow()

    /**
     * Restores a previously enabled tunnel without presenting UI when Android still retains VPN
     * consent. One automatic attempt is made per interruption; a failed attempt deliberately falls
     * back to the visible repair action instead of creating a restart loop.
     */
    @Synchronized
    fun recoverIfPossible() {
        if (
            !desired ||
                silentRecoveryAttempted ||
                state.value.phase in listOf(ProtectionPhase.ON, ProtectionPhase.STARTING)
        )
            return
        val consentRequired =
            try {
                VpnService.prepare(context) != null
            } catch (_: RuntimeException) {
                return
            }
        if (consentRequired) return
        silentRecoveryAttempted = true
        start()
    }

    fun start() {
        if (state.value.phase in listOf(ProtectionPhase.ON, ProtectionPhase.STARTING)) return
        val consentRequired =
            try {
                VpnService.prepare(context) != null
            } catch (_: RuntimeException) {
                failed("VPN is unavailable or restricted by this device.")
                return
            }
        if (consentRequired) {
            failed("VPN permission is required. Enable blocking again.")
            return
        }
        preferences.edit { putBoolean("enabled", true) }
        mutableState.value = ProtectionState(ProtectionPhase.STARTING, "Starting protection…")
        try {
            context.startForegroundService(
                Intent(context, WebsiteBlockVpnService::class.java)
                    .setAction(WebsiteBlockVpnService.START)
            )
        } catch (_: RuntimeException) {
            failed("Android prevented startup. Open the app and enable blocking again.")
        }
    }

    fun stop() {
        preferences.edit { putBoolean("enabled", false) }
        mutableState.value = ProtectionState()
        val stopIntent =
            Intent(context, WebsiteBlockVpnService::class.java)
                .setAction(WebsiteBlockVpnService.STOP)
        try {
            // A VPN service may remain system-bound after stopService(). Send an explicit command
            // so it closes the tunnel and cancels enforcement before removing the service.
            context.startService(stopIntent)
        } catch (_: RuntimeException) {
            context.stopService(Intent(context, WebsiteBlockVpnService::class.java))
        }
    }

    fun active() {
        silentRecoveryAttempted = false
        mutableState.value = ProtectionState(ProtectionPhase.ON, "VPN protection is on")
    }

    fun failed(message: String) {
        mutableState.value = ProtectionState(ProtectionPhase.NEEDS_REACTIVATION, message)
    }

    fun stopped() {
        mutableState.value =
            if (desired)
                ProtectionState(
                    ProtectionPhase.NEEDS_REACTIVATION,
                    "Protection stopped. Enable it again to resume.",
                )
            else ProtectionState()
    }

    fun consentDenied() {
        failed("VPN permission was not granted. Your websites are not blocked.")
    }
}
