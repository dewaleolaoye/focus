package com.websiteblocker.app.vpn

import android.app.*
import android.content.Context
import android.content.Intent
import com.websiteblocker.app.MainActivity
import com.websiteblocker.app.R

class VpnNotificationManager(private val context: Context) {
    fun notification(): Notification {
        context
            .getSystemService(NotificationManager::class.java)
            .createNotificationChannel(
                NotificationChannel(
                    CHANNEL,
                    "App and website protection",
                    NotificationManager.IMPORTANCE_LOW,
                )
            )
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
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("App & website protection is on")
            .setContentText("Active schedules block selected apps or website DNS on this device.")
            .setContentIntent(open)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .addAction(Notification.Action.Builder(null, "Turn off", stop).build())
            .build()
    }

    companion object {
        const val ID = 100
        const val CHANNEL = "website_protection"
    }
}
