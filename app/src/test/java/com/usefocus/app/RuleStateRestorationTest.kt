package com.usefocus.app

import androidx.lifecycle.SavedStateHandle
import com.usefocus.app.data.model.BlockRule
import com.usefocus.app.ui.rule.RuleState
import com.usefocus.app.ui.rule.restore
import org.junit.Assert.*
import org.junit.Test

class RuleStateRestorationTest {
    private val existing = BlockRule(
        id = 1, domain = "instagram.com", startMinute = 540, endMinute = 1020,
        daysMask = 31, serviceId = "instagram",
    )

    @Test
    fun customWebsiteEditSurvivesRecreation() {
        val restored = RuleState().restore(SavedStateHandle(mapOf(
            "serviceId" to null, "customSelected" to true, "domain" to "example.com",
        )), existing)
        assertNull(restored.serviceId)
        assertEquals("example.com", restored.domain)
        assertTrue(restored.customSelected)
        assertTrue(restored.canSave)
    }

    @Test
    fun clearedTargetStaysClearedAfterRecreation() {
        val restored = RuleState().restore(SavedStateHandle(mapOf(
            "serviceId" to null, "customSelected" to false, "domain" to "",
        )), existing)
        assertNull(restored.serviceId)
        assertFalse(restored.customSelected)
        assertFalse(restored.canSave)
    }

    @Test
    fun openingExistingRuleWithoutAnEditUsesStoredTarget() {
        val restored = RuleState().restore(SavedStateHandle(), existing)
        assertEquals("instagram", restored.serviceId)
        assertEquals("instagram.com", restored.domain)
        assertEquals(540, restored.start)
        assertFalse(restored.customSelected)
        assertTrue(restored.canSave)
    }

    @Test
    fun replacementAppAndOtherEditsSurviveRecreation() {
        val restored = RuleState().restore(SavedStateHandle(mapOf(
            "serviceId" to "youtube", "domain" to "youtube.com", "customSelected" to false,
            "enabled" to false, "days" to 64, "start" to 600,
        )), existing)
        assertEquals("youtube", restored.serviceId)
        assertEquals("youtube.com", restored.domain)
        assertFalse(restored.enabled)
        assertEquals(64, restored.days)
        assertEquals(600, restored.start)
    }
}
