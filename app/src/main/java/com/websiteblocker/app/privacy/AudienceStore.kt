package com.websiteblocker.app.privacy

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AgeGroup(val label: String) {
    UNDER_13("Under 13"), TEEN("13–17"), ADULT("18 or older"), UNSPECIFIED("Prefer not to say"),
}

class AudienceStore(context: Context) {
    private val preferences = context.getSharedPreferences("audience", Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(
        preferences.getString("age_group", null)?.let { saved -> AgeGroup.entries.find { it.name == saved } }
    )
    val state = mutableState.asStateFlow()
    fun setAgeGroup(group: AgeGroup) {
        preferences.edit { putString("age_group", group.name) }
        mutableState.value = group
    }
}
