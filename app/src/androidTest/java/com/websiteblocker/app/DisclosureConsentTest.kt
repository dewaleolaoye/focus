package com.websiteblocker.app

import android.content.Context
import android.content.ContextWrapper
import androidx.test.platform.app.InstrumentationRegistry
import com.websiteblocker.app.privacy.DisclosureConsentStore
import org.junit.Assert.*
import org.junit.Test

class DisclosureConsentTest {
    @Test fun consentIsSeparateExplicitPersistentAndWithdrawable() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val context = object : ContextWrapper(base) {
            override fun getSharedPreferences(name: String, mode: Int) =
                base.getSharedPreferences("test-disclosures", Context.MODE_PRIVATE)
        }
        val preferences = context.getSharedPreferences("", 0)
        preferences.edit().clear().commit()
        try {
            val store = DisclosureConsentStore(context)
            assertFalse(store.state.value.vpnAccepted)
            assertFalse(store.state.value.accessibilityAccepted)
            store.acceptVpn()
            assertTrue(DisclosureConsentStore(context).state.value.vpnAccepted)
            assertFalse(store.state.value.accessibilityAccepted)
            store.acceptAccessibility()
            assertTrue(store.state.value.accessibilityAccepted)
            store.revokeAccessibility()
            assertFalse(store.state.value.accessibilityAccepted)
            assertTrue(store.state.value.vpnAccepted)
            store.revokeVpn()
            assertFalse(DisclosureConsentStore(context).state.value.vpnAccepted)
            preferences.edit().putInt("vpn_version", -1).commit()
            assertFalse(DisclosureConsentStore(context).state.value.vpnAccepted)
        } finally { preferences.edit().clear().commit() }
    }
    @Test fun ageChoiceDefaultsToUnknownAndPersistsOnlyTheChosenGroup() {
        val base = InstrumentationRegistry.getInstrumentation().targetContext
        val context = object : ContextWrapper(base) {
            override fun getSharedPreferences(name: String, mode: Int) =
                base.getSharedPreferences("test-audience", Context.MODE_PRIVATE)
        }
        val preferences = context.getSharedPreferences("", 0)
        preferences.edit().clear().commit()
        try {
            val store = com.websiteblocker.app.privacy.AudienceStore(context)
            assertNull(store.state.value)
            for (group in com.websiteblocker.app.privacy.AgeGroup.entries) {
                store.setAgeGroup(group)
                assertEquals(group, com.websiteblocker.app.privacy.AudienceStore(context).state.value)
                assertEquals(setOf("age_group"), preferences.all.keys)
            }
        } finally { preferences.edit().clear().commit() }
    }
}
