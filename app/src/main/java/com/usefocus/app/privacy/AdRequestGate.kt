package com.usefocus.app.privacy

/** Covers consent changes racing with asynchronous ad SDK initialization. */
internal class AdRequestGate {
    private var authorized = false
    private var checking = true
    private var initialized = false
    val mayInitialize get() = authorized && !checking
    val canRequestAds get() = mayInitialize && initialized
    fun beginConsentCheck() { checking = true; authorized = false }
    fun completeConsentCheck(allowed: Boolean) {
        authorized = allowed
        checking = false
    }
    fun completeInitialization() { initialized = true }
}
