package app.bedtime.data

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.service.notification.NotificationListenerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** How much of a notification the lock screen may show, as Android's own lock screen would. */
enum class NoteDetail { HIDE, APP_ONLY, FULL }

/**
 * A notification as the session lock screen shows it. With [NoteDetail.APP_ONLY], [title] and [text]
 * are the app's own public version, if it gave one.
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
