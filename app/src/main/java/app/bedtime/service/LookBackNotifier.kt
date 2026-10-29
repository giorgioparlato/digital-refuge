package app.bedtime.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.bedtime.MainActivity
import app.bedtime.R
import app.bedtime.Routes
import app.bedtime.data.Habits
import app.bedtime.data.PhoneUsage
import app.bedtime.data.Repository
import app.bedtime.engine.LookBacks
import app.bedtime.ui.stats.LookBackLines
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The week looked back on, as a quiet notification on Sunday evening, once: the first time the phone
 * is unlocked after six. Only when asked for, from the card in insights. Counts from refuge's own
 * records, so it needs nothing more than the card does.
 */
object LookBackNotifier {
    private const val CHANNEL = "look_back"
    private const val ID = 3

    /** Posts this week's note if it's Sunday evening, it's wanted, and it hasn't been posted yet. */
    suspend fun maybePost(context: Context) {
        val now = LocalDateTime.now()
        if (now.dayOfWeek != DayOfWeek.SUNDAY || now.hour < LookBacks.SUNDAY_HOUR) return
        val repo = Repository.get(context)
        val settings = repo.settings.first()
        val start = LookBacks.weekStart(now)
        if (!settings.weeklyNote || settings.weeklyNotedWeek == start.toEpochDay()) return
        val week = LookBacks.of(
            start,
            repo.history.first(),
            PhoneUsage.fromRecords(repo.usage.first()),
            settings.distractingApps,
            settings.habits,
            Habits.byDay(repo.habitMarks.first()),
            ZoneId.systemDefault(),
        )
        repo.updateSettings { it.copy(weeklyNotedWeek = start.toEpochDay()) }
        post(context, LookBackLines.summary(week))
    }

    @SuppressLint("MissingPermission") // Checked through areNotificationsEnabled().
    private fun post(context: Context, text: String) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val system = context.getSystemService(NotificationManager::class.java)
        if (system.getNotificationChannel(CHANNEL) == null) {
            system.createNotificationChannel(
                NotificationChannel(CHANNEL, "your week", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "a look back on the week, on sunday evening, if you asked for it"
                    setShowBadge(false)
                },
            )
        }
        val tap = PendingIntent.getActivity(
            context,
            ID,
            MainActivity.intent(context, Routes.INSIGHTS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle("your week")
            .setContentText(text.lowercase())
            .setStyle(NotificationCompat.BigTextStyle().bigText(text.lowercase()))
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        runCatching { manager.notify(ID, notification) }
    }
}
