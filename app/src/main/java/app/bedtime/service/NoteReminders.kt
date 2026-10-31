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
import app.bedtime.MainActivity
import app.bedtime.R
import app.bedtime.Routes
import app.bedtime.data.Note
import app.bedtime.data.NoteShelf
import app.bedtime.data.NoteWidget
import app.bedtime.data.Problems
import app.bedtime.data.Repository
import kotlinx.coroutines.flow.first

/**
 * "Remind me" on a note: one wake for the soonest reminder; then a notification for each note due,
 * its reminder cleared, and a wake for the next. It needn't be to the minute, so the alarm is an
 * ordinary one, as the rain alert's is.
 */
object NoteReminders {
    private const val REQUEST = 7003
    const val CHANNEL = "note_reminders"

    /** Sets the wake for the soonest reminder in [notes], or takes it away when there's none. */
    fun schedule(context: Context, notes: List<Note>) {
        val app = context.applicationContext
        val manager = app.getSystemService(AlarmManager::class.java) ?: return
        val intent = PendingIntent.getBroadcast(app, REQUEST, Intent(app, NoteReminderReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val at = NoteShelf.nextReminder(notes, System.currentTimeMillis())
        if (at == null) {
            manager.cancel(intent)
            return
        }
        Problems.attempt("setting a note's reminder") { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, intent) }
    }

    /** Tells of the notes due, clears their reminders, and sets the next. */
    suspend fun fire(context: Context) {
        val repo = Repository.get(context)
        val now = System.currentTimeMillis()
        // A little ahead too: the wake may come a moment early.
        NoteShelf.due(repo.notes.first(), now + 60_000L).forEach { note ->
            post(context, note)
            repo.remindNote(note.id, null)
        }
        schedule(context, repo.notes.first())
    }

    @SuppressLint("MissingPermission") // Checked through areNotificationsEnabled().
    private fun post(context: Context, note: Note) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        val system = context.getSystemService(NotificationManager::class.java)
        if (system.getNotificationChannel(CHANNEL) == null) {
            system.createNotificationChannel(
                NotificationChannel(CHANNEL, "note reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "a note you asked to be reminded of, at the time you chose"
                },
            )
        }
        val id = note.id.hashCode()
        // A tap opens the note.
        val tap = PendingIntent.getActivity(context, id, MainActivity.intent(context, Routes.note(note.id)), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val title = NoteWidget.title(note.text).ifBlank { "A note" }
        val rest = note.text.lines().drop(1).filter { it.isNotBlank() }.take(3).joinToString(" · ") { it.trim() }
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle(title)
            .setContentText(rest.ifEmpty { "you asked to be reminded" })
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
        Problems.attempt("posting a note's reminder") { manager.notify(id, notification) }
    }
}

/** Fires due reminders; set by [NoteReminders]. */
class NoteReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        async { NoteReminders.fire(context.applicationContext) }
    }
}
