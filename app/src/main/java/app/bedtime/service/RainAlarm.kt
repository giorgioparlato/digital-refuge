package app.bedtime.service

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.bedtime.R
import app.bedtime.apps.AppCatalog
import app.bedtime.data.HomeStyle
import app.bedtime.data.Problems
import app.bedtime.data.RainAlert
import app.bedtime.data.Repository
import app.bedtime.data.Weather
import app.bedtime.ui.formatMinuteOfDay
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * The morning's rain alert: a wake each day at the chosen time, and a quiet notification then if
 * rain is likely today. It needn't be to the minute, so the alarm is an ordinary one.
 */
object RainAlarm {
    private const val REQUEST = 7002
    private const val CHANNEL = "rain"
    private const val ID = 4

    /** The next time the alert is due after [now], at [minute] of the day. */
    internal fun next(now: ZonedDateTime, minute: Int): ZonedDateTime {
        val today = now.toLocalDate().atTime(LocalTime.of(minute / 60, minute % 60)).atZone(now.zone)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    /** Sets the next alert, or takes it away when the alert is off. */
    fun schedule(context: Context, style: HomeStyle) {
        val app = context.applicationContext
        val manager = app.getSystemService(AlarmManager::class.java) ?: return
        val intent = PendingIntent.getBroadcast(app, REQUEST, Intent(app, RainAlarmReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        if (!style.rainAlert) {
            manager.cancel(intent)
            return
        }
        val at = next(ZonedDateTime.now(), style.rainAlertMinute).toInstant().toEpochMilli()
        Problems.attempt("setting the rain alert") { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent) }
    }

    /** Sends today's alert if rain is likely, then sets tomorrow's. */
    suspend fun fire(context: Context) {
        val repo = Repository.get(context)
        val style = repo.settings.first().homeStyle
        schedule(context, style)
        if (!style.rainAlert) return
        val outlook = RainAlert.outlook(repo.weather.first(), System.currentTimeMillis(), ZoneId.systemDefault(), style.rainAlertFrom) ?: return
        post(context, RainAlert.message(outlook) { hour -> formatMinuteOfDay(context, hour * 60) })
    }

    @SuppressLint("MissingPermission") // Checked through areNotificationsEnabled().
    private fun post(context: Context, text: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val system = context.getSystemService(NotificationManager::class.java)
        if (system.getNotificationChannel(CHANNEL) == null) {
            system.createNotificationChannel(
                NotificationChannel(CHANNEL, "rain today", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "in the morning, when rain is likely today, if you asked for it"
                    setShowBadge(false)
                },
            )
        }
        // A tap opens Breezy Weather, where the forecast is.
        val tap = Weather.APPS.firstNotNullOfOrNull { context.packageManager.getLaunchIntentForPackage(it) }?.let {
            PendingIntent.getActivity(context, ID, it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK), PendingIntent.FLAG_IMMUTABLE)
        }
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle("rain today")
            .setContentText(text.lowercase())
            .setContentIntent(tap)
            .setAutoCancel(true)
            .setTimeoutAfter(endOfDay())
            .build()
        Problems.attempt("posting the rain alert") { manager.notify(ID, notification) }
    }

    /** Milliseconds until midnight: it's about today, so it goes then. */
    private fun endOfDay(): Long =
        LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli() - System.currentTimeMillis()
}

/** Fires the rain alert; set by [RainAlarm]. */
class RainAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        async { RainAlarm.fire(context.applicationContext) }
    }
}
