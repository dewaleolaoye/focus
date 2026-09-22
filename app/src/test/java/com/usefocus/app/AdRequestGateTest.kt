package com.usefocus.app

import com.usefocus.app.privacy.AdRequestGate
import org.junit.Assert.*
import org.junit.Test

class AdRequestGateTest {
    @Test fun neitherFirstLaunchNorAnErrorAuthorizesSdkInitialization() {
        val gate = AdRequestGate()
        assertFalse(gate.mayInitialize)
        assertFalse(gate.canRequestAds)
        gate.completeConsentCheck(false, isAdult = true)
        assertFalse(gate.mayInitialize)
        gate.completeInitialization()
        assertFalse(gate.canRequestAds)
    }
    @Test fun consentAllowsInitializationButAdsWaitForItsCompletion() {
        val gate = AdRequestGate()
        gate.completeConsentCheck(true, isAdult = true)
        assertTrue(gate.mayInitialize)
        assertFalse(gate.canRequestAds)
        gate.completeInitialization()
        assertTrue(gate.canRequestAds)
    }
    @Test fun changingConsentBlocksRequestsEvenIfOldInitializationCompletes() {
        val gate = AdRequestGate()
        gate.completeConsentCheck(true, isAdult = true)
        gate.beginConsentCheck()
        gate.completeInitialization()
        assertFalse(gate.canRequestAds)
        gate.completeConsentCheck(false, isAdult = true)
        assertFalse(gate.canRequestAds)
        gate.beginConsentCheck()
        gate.completeConsentCheck(true, isAdult = true)
        assertTrue(gate.canRequestAds)
    }
    @Test fun cachedAdultConsentCannotEnableAdsForTeenOrUnspecifiedAudience() {
        val gate = AdRequestGate()
        gate.completeConsentCheck(true, isAdult = true)
        gate.completeInitialization()
        assertTrue(gate.canRequestAds)
        gate.beginConsentCheck()
        gate.completeConsentCheck(true, isAdult = false)
        assertFalse(gate.mayInitialize)
        assertFalse(gate.canRequestAds)
    }
}
