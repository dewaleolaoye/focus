package com.usefocus.app.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.usefocus.app.data.model.InstalledApp
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InstalledAppRepository(private val context: Context) {

    suspend fun loadInstalledApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }

        val ownPackage = context.packageName
        val seen = mutableSetOf<String>()
        val apps = mutableListOf<InstalledApp>()

        for (resolveInfo in activities) {
            val pkg = resolveInfo.activityInfo?.packageName ?: continue
            if (pkg == ownPackage || !seen.add(pkg)) continue
            if (isIgnoredPackage(pkg)) continue
            val label = runCatching {
                resolveInfo.loadLabel(pm).toString().trim()
            }.getOrNull()
            apps.add(
                InstalledApp(
                    packageName = pkg,
                    label = if (!label.isNullOrBlank()) label else pkg,
                )
            )
        }

        apps.sortedBy { it.label.lowercase(Locale.ROOT) }
    }

    private fun isIgnoredPackage(pkg: String): Boolean {
        val lower = pkg.lowercase(Locale.ROOT)
        return lower.startsWith("com.android.stk") ||
            lower.contains(".stk") ||
            lower.startsWith("com.samsung.android.ar") ||
            lower.startsWith("com.samsung.android.app.reminder") ||
            lower.startsWith("com.samsung.android.beaconmanager") ||
            lower.contains(".overlay.") ||
            lower.contains(".wallpaper.") ||
            lower == "com.android.traceur" ||
            lower == "com.google.android.feedback" ||
            lower == "com.android.shell" ||
            lower == "com.android.keyguard"
    }
}
