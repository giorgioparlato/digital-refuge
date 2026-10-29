package app.bedtime.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import app.bedtime.MainActivity
import app.bedtime.Routes
import app.bedtime.R
import app.bedtime.data.DndMode
import app.bedtime.data.Repository
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
    private const val ACCENT = 0xFF58A87C.toInt()

    /** What the notification says and does; posted again only when this changes. */
    private data class Content(
        val title: String,
        val text: String,
        val kind: Int,
        /** Wall-clock time the countdown runs to, or 0 for none. */
        val countdownTo: Long,
        val undoId: String?,
    )

    /** The last [Content] posted, so the guard's once-a-second check costs nothing while nothing changes. */
    @Volatile private var posted: Content? = null

    @Volatile private var channelReady = false

    /**
     * Builds the notification for the phase: on, on a break, or off and asking to be switched back on.
     * [breakEndsAt] is the break's end on the wall clock (0 for none); the countdown runs by itself.
     */
    fun build(context: Context, state: ActiveState?, blockingOn: Boolean, breakEndsAt: Long, overlayMissing: Boolean = false): Notification {
        // Shown by the caller (startForeground), so the next post can't assume what's there.
        posted = null
        return build(context, content(state, blockingOn, breakEndsAt, overlayMissing, context))
    }

    private fun content(state: ActiveState?, blockingOn: Boolean, breakEndsAt: Long, overlayMissing: Boolean, context: Context): Content {
        val main = state?.active?.maxByOrNull { it.end }
        val name = main?.schedule?.name?.lowercase()
        // A block started moments ago can be taken back, for the mis-tap on a widget or the tile.
        val undoable = if (blockingOn) {
            state?.active?.firstOrNull { it.schedule.isBlock && Repository.canUndo(it.start, System.currentTimeMillis()) }
        } else {
            null
        }
        return when {
            overlayMissing && blockingOn ->
                Content("the full-screen reminder is off", "tap to allow display over other apps again", KIND_OVERLAY, 0L, null)
            blockingOn -> Content(
                if (main == null) "refuge is on" else "$name · until ${formatTime(context, main.end)}".lowercase(),
                summary(state),
                KIND_ON,
                main?.end ?: 0L,
                undoable?.schedule?.id,
            )
            breakEndsAt > 0 -> Content("on a break", "switch blocking back on when you're done", KIND_OFF, breakEndsAt, null)
            else -> Content(
                "blocking is off",
                if (name == null) "tap to switch it back on" else "$name is still running · tap to switch blocking back on",
                KIND_OFF,
                0L,
                null,
            )
        }
    }

    private fun build(context: Context, content: Content): Notification {
        ensureChannel(context)
        val intent = when (content.kind) {
            KIND_OVERLAY -> BlockingState.overlayIntent(context)
            KIND_ON -> MainActivity.intent(context, Routes.HOME)
            else -> BlockingState.accessibilityIntent()
        }
        val blockingOn = content.kind != KIND_OFF
        val countdown = content.countdownTo > 0
        val tap = PendingIntent.getActivity(
            context,
            if (blockingOn) 0 else 1,
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_tile)
            .setColor(ACCENT)
            .setContentTitle(content.title)
            .setContentText(content.text)
            .setContentIntent(tap)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(countdown)
            .apply {
                if (countdown) {
                    setWhen(content.countdownTo)
                    setUsesChronometer(true)
                    setChronometerCountDown(true)
                }
            }
            // Alarms always pass our own Do Not Disturb, so "hide notifications" doesn't hide this one.
            .setCategory(if (blockingOn) NotificationCompat.CATEGORY_ALARM else NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .apply { if (content.undoId != null) addAction(0, "undo", UndoStartReceiver.pending(context, content.undoId)) }
            .build()
    }

    private const val KIND_ON = 0
    private const val KIND_OFF = 1
    private const val KIND_OVERLAY = 2

    @SuppressLint("MissingPermission") // Checked through areNotificationsEnabled().
    fun post(context: Context, id: Int, state: ActiveState?, blockingOn: Boolean, breakEndsAt: Long, overlayMissing: Boolean = false) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val content = content(state, blockingOn, breakEndsAt, overlayMissing, context)
        if (content == posted) return
        runCatching { manager.notify(id, build(context, content)) }.onSuccess { posted = content }
    }

    fun cancel(context: Context) {
        posted = null
        NotificationManagerCompat.from(context).cancel(ID)
    }

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
        if (channelReady) return
        channelReady = true
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

/** The notification's "undo": takes back a block started a moment ago, if that moment hasn't passed. */
class UndoStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getStringExtra(EXTRA_ID) ?: return
        val app = context.applicationContext
        async {
            val undone = Repository.get(app).undoStart(id)
            val message = if (undone) "taken back, as if it never started" else "too late to undo · leave early instead"
            Handler(Looper.getMainLooper()).post { Toast.makeText(app, message, Toast.LENGTH_SHORT).show() }
        }
    }

    companion object {
        private const val EXTRA_ID = "schedule"

        fun pending(context: Context, scheduleId: String): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                2,
                Intent(context, UndoStartReceiver::class.java).putExtra(EXTRA_ID, scheduleId),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
    }
}
