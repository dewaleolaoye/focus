package com.websiteblocker.app.vpn

import android.app.*
import android.content.Context
import android.content.Intent
import com.websiteblocker.app.MainActivity
import com.websiteblocker.app.R

class VpnNotificationManager(private val context: Context) {
    fun notification(): Notification {
        val manager = context.getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            CHANNEL,
            "App and website protection",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Ongoing status when blocking is active"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
        val open =
            PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val stop =
            PendingIntent.getService(
                context,
                1,
                Intent(context, WebsiteBlockVpnService::class.java)
                    .setAction(WebsiteBlockVpnService.STOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        return Notification.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_shield)
            .setContentTitle("Website protection is on")
            .setContentText("Scheduled website filtering is active. Manage app blocking in Focus.")
            .setContentIntent(open)
            .setOngoing(true)
            .apply {
                if (android.os.Build.VERSION.SDK_INT >= 31)
                    setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
            }
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(Notification.Action.Builder(null, "Turn off", stop).build())
            .build()
    }

    companion object {
        const val ID = 100
        const val CHANNEL = "website_protection"
    }
}
