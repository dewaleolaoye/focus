package com.usefocus.app.privacy

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DisclosureConsent(
    val vpnAccepted: Boolean,
    val accessibilityAccepted: Boolean,
    val notificationAccepted: Boolean = false,
)

/** Explicit, versioned, on-device consent. Never inferred from an Android permission grant. */
class DisclosureConsentStore(context: Context) {
    private val preferences = context.getSharedPreferences("disclosure-consent", Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(read())
    val state = mutableState.asStateFlow()

    fun acceptVpn() = accept("vpn")
    fun acceptAccessibility() = accept("accessibility")
    fun acceptNotification() = accept("notification")
    fun revokeVpn() = revoke("vpn")
    fun revokeAccessibility() = revoke("accessibility")
    fun revokeNotification() = revoke("notification")

    private fun accept(name: String) {
        preferences.edit {
            putInt("${name}_version", VERSION)
            putLong("${name}_accepted_at", System.currentTimeMillis())
        }
        mutableState.value = read()
    }

    private fun revoke(name: String) {
        preferences.edit { remove("${name}_version"); remove("${name}_accepted_at") }
        mutableState.value = read()
    }

    private fun read() = DisclosureConsent(
        vpnAccepted = preferences.getInt("vpn_version", 0) == VERSION,
        accessibilityAccepted = preferences.getInt("accessibility_version", 0) == VERSION,
        notificationAccepted = preferences.getInt("notification_version", 0) == VERSION,
    )

    private companion object { const val VERSION = 1 }
}
