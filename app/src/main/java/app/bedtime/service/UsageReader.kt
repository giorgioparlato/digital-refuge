package app.bedtime.service

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import app.bedtime.data.UseEvent

/**
 * Android's own record of which app was in front and when, the one Digital Wellbeing reads. Only with
 * usage access, which you grant in Android's settings; refuge's own count stands in without it.
 */
object UsageReader {
    fun hasAccess(context: Context): Boolean = runCatching {
        val ops = context.getSystemService(AppOpsManager::class.java)
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            ops.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName)
        }
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    /** Android's usage access page, at digital refuge where the phone allows it. */
    fun accessIntent(context: Context): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.fromParts("package", context.packageName, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Apps whose time isn't phone use: digital refuge itself and the home screens. */
    fun excluded(context: Context): Set<String> {
        val homes = runCatching {
            context.packageManager.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY)
                .map { it.activityInfo.packageName }
        }.getOrDefault(emptyList())
        return (homes + context.packageName).toSet()
    }

    /** The events from [from] to [to], as far back as Android keeps them; empty without access. */
    fun events(context: Context, from: Long, to: Long): List<UseEvent> {
        if (!hasAccess(context)) return emptyList()
        val manager = context.getSystemService(UsageStatsManager::class.java) ?: return emptyList()
        val out = mutableListOf<UseEvent>()
        runCatching {
            val events = manager.queryEvents(from, to)
            val e = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(e)
                val kind = when (e.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> UseEvent.Kind.RESUMED
                    UsageEvents.Event.ACTIVITY_PAUSED -> UseEvent.Kind.PAUSED
                    UsageEvents.Event.SCREEN_NON_INTERACTIVE -> UseEvent.Kind.SCREEN_OFF
                    UsageEvents.Event.KEYGUARD_HIDDEN -> UseEvent.Kind.UNLOCK
                    else -> null
                } ?: continue
                out.add(UseEvent(e.timeStamp, kind, e.packageName.orEmpty()))
            }
        }
        return out
    }
}
