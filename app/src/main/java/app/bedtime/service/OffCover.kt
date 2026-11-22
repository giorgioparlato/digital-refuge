package app.bedtime.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.bedtime.data.Problems
import app.bedtime.data.Repository
import app.bedtime.engine.Engine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * The cover for blocking left switched off outside a session, if you asked for it
 * (`AppSettings.offCoverMinutes`). Switching blocking off sets an alarm that many minutes ahead;
 * if it's still off then, a full screen asks you to switch it back on, and "later" sets the alarm
 * again. Inside a session [SessionGuardService] already covers the screen, so the alarm only looks
 * again later. Switching blocking back on calls it all off.
 */
object OffCover {
    private const val REQUEST = 7311

    /** Called as blocking goes off: the alarm, if the cover's wanted. */
    fun blockingOff(context: Context) {
        val app = context.applicationContext
        scope.launch {
            val minutes = Repository.get(app).settings.first().offCoverMinutes ?: return@launch
            schedule(app, minutes)
        }
    }

    /** Called as blocking comes back on: no alarm, no cover. */
    fun blockingOn(context: Context) {
        val app = context.applicationContext
        cancel(app)
        if (!TakeoverOverlay.isForSession()) TakeoverOverlay.hide(app)
    }

    /** The setting changed: from now on, or not at all. */
    fun settingChanged(context: Context, minutes: Int?) {
        val app = context.applicationContext
        if (minutes == null || BlockingState.isOn(app)) {
            cancel(app)
            if (minutes == null && !TakeoverOverlay.isForSession()) TakeoverOverlay.hide(app)
        } else {
            schedule(app, minutes)
        }
    }

    fun schedule(context: Context, minutes: Int) {
        val manager = context.getSystemService(AlarmManager::class.java) ?: return
        val at = System.currentTimeMillis() + minutes.coerceAtLeast(1) * 60_000L
        Problems.attempt("setting the reminder for blocking left off") {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(context))
        }
    }

    private fun cancel(context: Context) {
        context.getSystemService(AlarmManager::class.java)?.cancel(pending(context))
    }

    private fun pending(context: Context): PendingIntent =
        PendingIntent.getBroadcast(context, REQUEST, Intent(context, OffCoverReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    /** The alarm's check: still off, no session, still wanted — then the cover. */
    internal suspend fun check(context: Context) {
        val minutes = Repository.get(context).settings.first().offCoverMinutes ?: return
        if (BlockingState.isOn(context)) return
        val session = withTimeoutOrNull(5_000) { Engine.state(context).filterNotNull().first() }
        if (session?.isActive == true || !TakeoverOverlay.canShow(context)) {
            // A session's own cover is up, or the cover can't be drawn: look again later.
            schedule(context, minutes)
            return
        }
        kotlinx.coroutines.withContext(Dispatchers.Main) { TakeoverOverlay.showOff(context, minutes) }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}

class OffCoverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val done = goAsync()
        val app = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                OffCover.check(app)
            } finally {
                done.finish()
            }
        }
    }
}
