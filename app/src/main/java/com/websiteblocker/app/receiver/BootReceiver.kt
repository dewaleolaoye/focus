package com.websiteblocker.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.websiteblocker.app.BlockerApplication

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (
            intent.action !in
                listOf(Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED)
        )
            return
        val protection = (context.applicationContext as BlockerApplication).protection
        if (protection.desired)
            protection
                .start() // Handles denied consent and foreground startup restrictions without
                         // crashing.
    }
}
