package app.bedtime.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import app.bedtime.data.Repository
import app.bedtime.engine.ScheduleEvaluator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZoneId

/**
 * Wakes the app when the next schedule is due to begin.
 *
 * Without this, [SessionGuardService] could only ever be started by [BlockerService] — which means
 * that with blocking switched off before a session, nothing was running to notice the session start,
 * and the takeover never appeared. An alarm is the one thing that still fires either way.
 *
 * It has to be an *exact* alarm: exact alarms are exempt from the ban on starting a foreground
 * service from the background, and an inexact one is not (and could also arrive minutes late).
 */
object SessionAlarms {
    private const val REQUEST = 7001

    private fun pending(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            REQUEST,
            Intent(context, SessionAlarmReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun canBeExact(manager: AlarmManager): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()

    /** Sets a wake-up for the next schedule's start, replacing any earlier one. */
    suspend fun schedule(context: Context) {
        val app = context.applicationContext
        val manager = app.getSystemService(AlarmManager::class.java) ?: return
        val schedules = runCatching { Repository.get(app).schedules.first() }.getOrNull() ?: return
        val now = System.currentTimeMillis()
        val zone = ZoneId.systemDefault()
        val next = schedules.filterNot { it.isBlock }
            .mapNotNull { ScheduleEvaluator.nextStart(it, now, zone) }
            .minOrNull()
        val intent = pending(app)
        if (next == null) {
            runCatching { manager.cancel(intent) }
            return
        }
        // A second past the boundary, so the engine already counts the session as started.
        val at = next + 1_000
        runCatching {
            if (canBeExact(manager)) {
                manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            } else {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent)
            }
        }
    }
}

/** Runs [work] off the main thread while keeping the broadcast alive. */
private fun BroadcastReceiver.async(work: suspend () -> Unit) {
    val result = goAsync()
    CoroutineScope(Dispatchers.Default).launch {
        try {
            work()
        } finally {
            result.finish()
        }
    }
}

/** The session is starting: bring the guard up, whether or not blocking is on, then set the next alarm. */
class SessionAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Started before anything suspends, so it still counts as part of handling the exact alarm.
        SessionGuardService.start(context)
        async { SessionAlarms.schedule(context) }
    }
}

/** Alarms don't survive a restart, so they're set again as soon as the phone comes back. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Only the two we registered for; anything else isn't ours to act on.
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        SessionGuardService.start(context) // Stops itself again if nothing is running.
        async { SessionAlarms.schedule(context) }
    }
}
