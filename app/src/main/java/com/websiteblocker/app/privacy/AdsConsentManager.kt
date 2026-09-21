package com.websiteblocker.app.privacy

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AdsPrivacyState(
    val canShowAds: Boolean = false,
    val busy: Boolean = false,
    val privacyOptionsRequired: Boolean = false,
    val message: String? = null,
)

/** No ad SDK initialization or requests before UMP permits them. Errors fail closed. */
class AdsConsentManager(private val context: Context) {
    private val consent = UserMessagingPlatform.getConsentInformation(context)
    private val mutableState = MutableStateFlow(AdsPrivacyState())
    val state = mutableState.asStateFlow()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var initializing = false
    private var initialized = false
    private var requestGeneration = 0
    private val gate = AdRequestGate()

    private val adult get() = (context.applicationContext as com.websiteblocker.app.BlockerApplication)
        .audience.state.value == AgeGroup.ADULT

    fun pauseForAudienceChoice() {
        ++requestGeneration
        gate.beginConsentCheck()
        mutableState.value = AdsPrivacyState(message = "Ads are off until adult age eligibility and privacy choices are confirmed.")
    }

    fun request(activity: Activity) {
        if (!adult) { pauseForAudienceChoice(); return }
        if (mutableState.value.busy) return
        val generation = ++requestGeneration
        gate.beginConsentCheck()
        mutableState.value = mutableState.value.copy(canShowAds = false, busy = true, message = null)
        consent.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
            if (generation != requestGeneration) return@requestConsentInfoUpdate
            if (activity.isFinishing || activity.isDestroyed) {
                finish(false, "Open privacy choices again to finish setup.")
                return@requestConsentInfoUpdate
            }
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                if (generation == requestGeneration)
                    finish(error == null && consent.canRequestAds(),
                        if (error == null) null else "Ads are paused because privacy choices could not be loaded. You can retry here.")
            }
        }, {
            if (generation == requestGeneration)
                finish(false, "Ads are paused because privacy choices could not be checked. You can retry here.")
        })
    }

    fun showPrivacyOptions(activity: Activity) {
        if (!adult) { pauseForAudienceChoice(); return }
        if (mutableState.value.busy) return
        if (consent.privacyOptionsRequirementStatus != ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED) {
            request(activity)
            return
        }
        val generation = ++requestGeneration
        gate.beginConsentCheck()
        mutableState.value = mutableState.value.copy(canShowAds = false, busy = true, message = null)
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (generation != requestGeneration) return@showPrivacyOptionsForm
            finish(error == null && consent.canRequestAds(),
                if (error == null) null else "Ads are paused. Privacy choices could not be opened; please retry.")
        }
    }

    private fun finish(allowed: Boolean, message: String?) {
        gate.completeConsentCheck(allowed, isAdult = adult)
        mutableState.value = AdsPrivacyState(
            canShowAds = gate.canRequestAds,
            privacyOptionsRequired = consent.privacyOptionsRequirementStatus ==
                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED,
            message = message,
        )
        if (!gate.mayInitialize || initialized || initializing) return
        initializing = true
        scope.launch(Dispatchers.IO) {
            try {
                MobileAds.initialize(context) {
                scope.launch {
                    initialized = true
                    gate.completeInitialization()
                    initializing = false
                    // Consent may have changed while SDK initialization was in flight.
                    mutableState.value = mutableState.value.copy(
                        canShowAds = gate.canRequestAds && adult && consent.canRequestAds(),
                    )
                }
                }
            } catch (_: Exception) {
                scope.launch {
                    initializing = false
                    gate.completeConsentCheck(false, isAdult = adult)
                    mutableState.value = mutableState.value.copy(
                        canShowAds = false, busy = false,
                        message = "Ads are paused because initialization failed. You can retry privacy choices.",
                    )
                }
            }
        }
    }
}
