package com.safescreen.app.system

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.safescreen.app.core.AppUsage
import com.safescreen.app.service.GuardAccessibilityService
import java.util.concurrent.TimeUnit

/** The special accesses SafeScreen asks for, each granted by the family in system settings. */
enum class Access { OVERLAY, ACCESSIBILITY, NOTIFICATION_LISTENER, USAGE, NOTIFICATIONS }

object Permissions {

    fun isGranted(context: Context, access: Access): Boolean = when (access) {
        Access.OVERLAY -> Settings.canDrawOverlays(context)
        Access.ACCESSIBILITY -> accessibilityEnabled(context)
        Access.NOTIFICATION_LISTENER ->
            context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)
        Access.USAGE -> usageGranted(context)
        Access.NOTIFICATIONS -> Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    /** The settings page where the family grants [access]; null for NOTIFICATIONS (runtime dialog). */
    fun settingsIntent(context: Context, access: Access): Intent? = when (access) {
        Access.OVERLAY -> Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        )
        Access.ACCESSIBILITY -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        Access.NOTIFICATION_LISTENER -> Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
        Access.USAGE -> Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        Access.NOTIFICATIONS -> null
    }

    private fun accessibilityEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val ours = ComponentName(context, GuardAccessibilityService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == ours }
    }

    // Each of these calls is deprecated on some API level; both answer this check correctly.
    @Suppress("DEPRECATION")
    private fun usageGranted(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }
}

/** Minutes per app over the last days, for the weekly summary. */
object UsageReader {

    fun lastDays(context: Context, days: Int): List<AppUsage> {
        if (!Permissions.isGranted(context, Access.USAGE)) return emptyList()
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val end = System.currentTimeMillis()
        val start = end - TimeUnit.DAYS.toMillis(days.toLong())
        return manager.queryAndAggregateUsageStats(start, end)
            .filterKeys { it != context.packageName }
            .mapNotNull { (pkg, stats) ->
                val minutes = TimeUnit.MILLISECONDS.toMinutes(stats.totalTimeInForeground).toInt()
                if (minutes < 1) return@mapNotNull null
                AppUsage(InstalledApps.label(context, pkg) ?: return@mapNotNull null, minutes)
            }
    }
}

data class InstalledApp(val label: String, val packageName: String)

object InstalledApps {

    /** Apps with a launcher icon, i.e. the ones a child actually opens. */
    fun launchable(context: Context): List<InstalledApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .map { InstalledApp(it.loadLabel(pm).toString(), it.activityInfo.packageName) }
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /** Label of a launchable app; null for system components without a launcher icon. */
    fun label(context: Context, packageName: String): String? {
        val pm = context.packageManager
        if (pm.getLaunchIntentForPackage(packageName) == null) return null
        return runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrNull()
    }
}
