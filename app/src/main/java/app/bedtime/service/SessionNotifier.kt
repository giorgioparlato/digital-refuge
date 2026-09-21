package app.bedtime.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.bedtime.MainActivity
import app.bedtime.Routes
import app.bedtime.R
import app.bedtime.data.DndMode
import app.bedtime.engine.ActiveState
import app.bedtime.ui.formatTime

/**
 * The one ongoing notification a session shows: a lotus in the status bar while everything is
 * running, and a reminder to switch blocking back on while it isn't. Posted by [SessionGuardService],
 * which keeps running even when the accessibility service has been switched off.
 */
object SessionNotifier {
    /** Normal importance: Android hides "silent" (low-importance) notifications from the status bar. */
    private const val CHANNEL = "session_running"
    private const val OLD_CHANNEL = "session"
    private const val ID = 1
    private const val ACCENT = 0xFF2EA873.toInt()

    /** Builds the notification for the phase: on, on a break, or off and asking to be switched back on. */
    fun build(context: Context, state: ActiveState?, blockingOn: Boolean, breakMs: Long, overlayMissing: Boolean = false): Notification {
        ensureChannel(context)
        val main = state?.active?.maxByOrNull { it.end }
        val name = main?.schedule?.name?.lowercase()

        val title: String
        val text: String
        val intent: Intent
        when {
            overlayMissing && blockingOn -> {
                title = "the full-screen reminder is off"
                text = "tap to allow display over other apps again"
                intent = BlockingState.overlayIntent(context)
            }
            blockingOn -> {
                title = if (main == null) "refuge is on" else "$name · until ${formatTime(context, main.end)}".lowercase()
                text = summary(state)
                intent = MainActivity.intent(context, Routes.HOME)
            }
            breakMs > 0 -> {
                title = "on a break · ${(breakMs / 1000).coerceAtLeast(1)}s left"
                text = "switch blocking back on when you're done"
                intent = BlockingState.accessibilityIntent()
            }
            else -> {
                title = "blocking is off"
                text = if (name == null) "tap to switch it back on" else "$name is still running · tap to switch blocking back on"
                intent = BlockingState.accessibilityIntent()
            }
        }

        val tap = PendingIntent.getActivity(
            context,
            if (blockingOn) 0 else 1,
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_tile)
            .setColor(ACCENT)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(tap)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(blockingOn && main != null)
            .apply {
                if (blockingOn && main != null) {
                    setWhen(main.end)
                    setUsesChronometer(true)
                    setChronometerCountDown(true)
                }
            }
            // Alarms always pass our own Do Not Disturb, so "hide notifications" doesn't hide this one.
            .setCategory(if (blockingOn) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    @SuppressLint("MissingPermission") // Checked through areNotificationsEnabled().
    fun post(context: Context, id: Int, state: ActiveState?, blockingOn: Boolean, breakMs: Long, overlayMissing: Boolean = false) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        runCatching { manager.notify(id, build(context, state, blockingOn, breakMs, overlayMissing)) }
    }

    fun cancel(context: Context) = NotificationManagerCompat.from(context).cancel(ID)

    /** What the session is doing right now, for the notification's second line. */
    private fun summary(state: ActiveState?): String {
        if (state == null || !state.isActive) return "no session running"
        val others = state.active.size - 1
        return buildList {
            if (others > 0) add("+$others more")
            if (state.minimalAllowlist != null) add("minimal home")
            if (state.blocked.isNotEmpty()) add("${state.blocked.size} apps blocked")
            if (state.greyscale) add("greyscale")
            if (state.dnd != DndMode.OFF) add("do not disturb")
        }.ifEmpty { listOf("refuge is on") }.joinToString(" · ")
    }

    private fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(OLD_CHANNEL)
        if (manager.getNotificationChannel(CHANNEL) != null) return
        // Never makes a sound or vibrates; only the icon, the countdown and the reminder.
        val channel = NotificationChannel(CHANNEL, "session running", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "a lotus in the status bar while a schedule or block is on, and a reminder if blocking goes off"
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            // Only honoured with Do Not Disturb access, which sessions that hide notifications have anyway.
            setBypassDnd(true)
        }
        manager.createNotificationChannel(channel)
    }
}
