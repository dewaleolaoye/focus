package com.websiteblocker.app.accessibility

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.view.accessibility.AccessibilityManager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppBlockingAccess {
    internal val connected = MutableStateFlow(false)
    val connection = connected.asStateFlow()
    fun isEnabled(context: Context): Boolean {
        val manager = context.getSystemService(AccessibilityManager::class.java)
        return manager
            .getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any {
                val service = it.resolveInfo.serviceInfo
                service.packageName == context.packageName &&
                    service.name ==
                        "com.websiteblocker.app.accessibility.AppBlockAccessibilityService"
            }
    }
}
