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
import app.bedtime.data.DndMode
import app.bedtime.engine.ActiveState
import app.bedtime.ui.formatTime

/** A quiet, ongoing notification (a lotus in the status bar) while any session runs. */
object SessionNotifier {
    private const val CHANNEL = "session"
    private const val ID = 1
    private const val ACCENT = 0xFF2EA873.toInt()

    /** Posts, updates or removes the notification to match [state]. */
    @SuppressLint("MissingPermission") // Checked through areNotificationsEnabled().
    fun update(context: Context, state: ActiveState?) {
        val manager = NotificationManagerCompat.from(context)
        val main = state?.active?.maxByOrNull { it.end }
        if (state == null || main == null) {
            manager.cancel(ID)
            return
        }
        if (!manager.areNotificationsEnabled()) return
        ensureChannel(context)

        val others = state.active.size - 1
        val title = "${main.schedule.name} · until ${formatTime(context, main.end)}".lowercase()
        val text = buildList {
            if (others > 0) add("+$others more")
            if (state.minimalAllowlist != null) add("minimal home")
            if (state.blocked.isNotEmpty()) add("${state.blocked.size} apps blocked")
            if (state.greyscale) add("greyscale")
            if (state.dnd != DndMode.OFF) add("do not disturb")
        }.ifEmpty { listOf("refuge is on") }.joinToString(" · ")
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_tile)
            .setColor(ACCENT)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(true)
            .setWhen(main.end)
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            // Alarms always pass our own Do Not Disturb, so "hide notifications" doesn't hide this one.
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        runCatching { manager.notify(ID, notification) }
    }

    fun cancel(context: Context) = NotificationManagerCompat.from(context).cancel(ID)

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(CHANNEL) != null) return
        val channel = NotificationChannel(CHANNEL, "session running", NotificationManager.IMPORTANCE_LOW).apply {
            description = "a lotus in the status bar while a schedule or block is on"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }
}
