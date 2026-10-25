package app.bedtime.data

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.service.notification.NotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** How much of a notification the lock screen may show, as Android's own lock screen would. */
enum class NoteDetail { HIDE, APP_ONLY, FULL }

/** One line of a conversation, read in place: who wrote it (when the app says) and what. */
data class NoteLine(val sender: String?, val text: String)

/** One of the notification's own buttons. [index] is its place in the notification's actions. */
data class NoteAction(val title: String, val index: Int, val isReply: Boolean, val needsUnlock: Boolean)

/**
 * A notification as the session lock screen shows it. With [NoteDetail.APP_ONLY], [title] and [text]
 * are the app's own public version, if it gave one, and nothing more is read.
 */
data class LockNotification(
    val key: String,
    val packageName: String,
    val app: String,
    val title: String?,
    val text: String?,
    val postedAt: Long,
    val detail: NoteDetail,
    val contentIntent: PendingIntent? = null,
    val autoCancel: Boolean = false,
    /** The whole text, when longer than [text]. */
    val body: String? = null,
    /** A conversation's last few lines, oldest first. */
    val messages: List<NoteLine> = emptyList(),
    val actions: List<NoteAction> = emptyList(),
    /** The conversation it belongs to, if the app says, for quieting just that one. */
    val conversation: String? = null,
    /** Who it's from, for the summary: the conversation or its last writer. */
    val who: String? = null,
    /** How many messages it holds, for the summary. */
    val count: Int = 1,
    val missedCall: Boolean = false,
    /** The names it's about (sender, conversation, people it lists), only for choosing which show. */
    val people: List<String> = emptyList(),
)

/**
 * The notifications for the session lock screen, read by [app.bedtime.service.LockNotificationListener]
 * once notification access is granted. Kept in memory only, never saved.
 */
object LockNotifications {
    private val _current = MutableStateFlow(emptyList<LockNotification>())
    val current: StateFlow<List<LockNotification>> = _current

    /** Set by the listener while it's connected. */
    @Volatile
    internal var listener: Listener? = null

    interface Listener {
        fun refresh()
        fun cancel(key: String)
        fun snooze(key: String, millis: Long)
        fun action(key: String, index: Int): Notification.Action?
    }

    /** Swiped away, by key and post time: gone at once, and an update to the same one can still show. */
    private val dismissed = mutableSetOf<Pair<String, Long>>()

    internal fun publish(list: List<LockNotification>) {
        synchronized(dismissed) {
            dismissed.retainAll { gone -> list.any { it.key == gone.first } }
            _current.value = list.filterNot { (it.key to it.postedAt) in dismissed }
        }
    }

    /** Clears it as Android's own shade does, and keeps it off the lock screen even if that fails. */
    fun dismiss(note: LockNotification) {
        synchronized(dismissed) {
            dismissed += note.key to note.postedAt
            _current.value = _current.value.filterNot { it.key == note.key }
        }
        cancel(note.key)
    }

    fun refresh() = listener?.refresh()

    fun cancel(key: String) = listener?.cancel(key)

    /** Out of the way until [until] (the session's end), when Android hands it back. */
    fun later(note: LockNotification, now: Long, until: Long) {
        synchronized(dismissed) {
            dismissed += note.key to note.postedAt
            _current.value = _current.value.filterNot { it.key == note.key }
        }
        listener?.snooze(note.key, snoozeFor(now, until))
    }

    fun action(key: String, index: Int): Notification.Action? = listener?.action(key, index)

    /** At least a minute, so a session about to end doesn't bounce it straight back. */
    fun snoozeFor(now: Long, until: Long): Long = (until - now).coerceAtLeast(60_000L)

    /** Replies always ask for the unlock; other buttons only when their app says so, as on Android's lock screen. */
    fun needsUnlock(authenticationRequired: Boolean, isReply: Boolean): Boolean = authenticationRequired || isReply

    /** What "quiet until morning" quiets: just the conversation if there is one, otherwise the whole app. */
    fun quietKey(note: LockNotification): String = note.conversation?.let { "${note.packageName}/$it" } ?: note.packageName

    /** Whether [note] is quieted by any entry in [quiet] still running at [now]. */
    fun isQuiet(note: LockNotification, quiet: Map<String, Long>, now: Long): Boolean =
        listOf(note.packageName, quietKey(note)).any { (quiet[it] ?: 0L) > now }

    /**
     * One calm line for the whole list: "a missed call · 3 from anna · 1 from mail". Missed calls first,
     * then whoever wrote most. At most three parts.
     */
    fun summary(notes: List<LockNotification>): String {
        if (notes.isEmpty()) return ""
        val calls = notes.count { it.missedCall }
        val parts = buildList {
            if (calls > 0) add(if (calls == 1) "a missed call" else "$calls missed calls")
            notes.filterNot { it.missedCall }
                .groupBy { it.who ?: it.app }
                .map { (who, group) -> who to group.sumOf { it.count } }
                .sortedByDescending { it.second }
                .forEach { (who, n) -> add("$n from $who") }
        }
        return parts.take(3).joinToString(" · ") + if (parts.size > 3) " · and more" else ""
    }

    /** Whether [note] is one you asked for: from one of [apps], or from one of [people] in any app. */
    fun wanted(note: LockNotification, apps: Set<String>, people: Set<String>): Boolean =
        note.packageName in apps || note.people.any { name -> people.any { it.equals(name.trim(), ignoreCase = true) } }

    /** Worth a line at all: not ours, not ongoing (music, navigation), not a group's summary, not minimal. */
    fun worthShowing(packageName: String, ownPackage: String, clearable: Boolean, groupSummary: Boolean, importance: Int): Boolean =
        packageName != ownPackage && clearable && !groupSummary && importance > NotificationManager.IMPORTANCE_MIN

    /**
     * Whether Android's own shade would show it now: a session's "hide notifications" takes it out of
     * there, and so out of here. Before Android 9 that suppression couldn't empty the shade, so anything
     * Do Not Disturb holds back counts as hidden while any suppression is on.
     */
    fun getsThrough(suppressedEffects: Int, matchesFilter: Boolean, sdk: Int): Boolean =
        if (sdk >= 28) {
            suppressedEffects and NotificationManager.Policy.SUPPRESSED_EFFECT_NOTIFICATION_LIST == 0
        } else {
            matchesFilter || suppressedEffects == 0
        }

    /**
     * Android's lock-screen privacy, followed: none at all if the phone shows no notifications when
     * locked; secret ones never; private ones in full only with sensitive content allowed. A null
     * setting couldn't be read, and the private choice applies.
     */
    fun detail(visibility: Int, channelVisibility: Int, showOnLock: Boolean?, allowPrivate: Boolean?): NoteDetail {
        if (showOnLock == false) return NoteDetail.HIDE
        val effective = if (channelVisibility == NotificationListenerService.Ranking.VISIBILITY_NO_OVERRIDE) visibility else minOf(visibility, channelVisibility)
        return when {
            effective <= Notification.VISIBILITY_SECRET -> NoteDetail.HIDE
            effective >= Notification.VISIBILITY_PUBLIC -> NoteDetail.FULL
            allowPrivate == true -> NoteDetail.FULL
            else -> NoteDetail.APP_ONLY
        }
    }
}
