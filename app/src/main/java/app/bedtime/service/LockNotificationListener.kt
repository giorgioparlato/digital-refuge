package app.bedtime.service

import java.util.concurrent.ConcurrentHashMap
import app.bedtime.data.NoteLine
import app.bedtime.data.NoteAction
import androidx.core.app.NotificationCompat
import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import app.bedtime.data.LockNotification
import app.bedtime.data.LockNotifications
import app.bedtime.data.Timers
import app.bedtime.data.NoteDetail

/**
 * Reads the notifications Android would show on its own lock screen, so the session lock screen can
 * show them too. It only reads: nothing is held back, changed or saved, and hiding notifications in a
 * session is still Do Not Disturb's job.
 */
class LockNotificationListener : NotificationListenerService(), LockNotifications.Listener {
    override fun onListenerConnected() {
        LockNotifications.listener = this
        refresh()
    }

    override fun onListenerDisconnected() {
        if (LockNotifications.listener === this) LockNotifications.listener = null
        LockNotifications.publish(emptyList())
        Timers.publishClock(emptyList())
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    override fun onNotificationRankingUpdate(rankingMap: RankingMap?) = refresh()

    override fun cancel(key: String) {
        runCatching { cancelNotification(key) }
    }

    override fun snooze(key: String, millis: Long) {
        runCatching { snoozeNotification(key, millis) }
    }

    override fun action(key: String, index: Int): Notification.Action? = held[key]?.actions?.getOrNull(index)

    /** The notifications on show, by key, so their buttons and replies can be used from the lock screen. */
    private val held = ConcurrentHashMap<String, Notification>()

    override fun refresh() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        // A clock app's running timer, read before anything is filtered: it's ongoing, so it never shows as a notification here.
        // refuge's own countdown, to a session's end, is no timer.
        Timers.publishClock(
            active.mapNotNull { sbn ->
                if (sbn.packageName == packageName) return@mapNotNull null
                val extras = sbn.notification.extras ?: return@mapNotNull null
                Timers.countdownEnd(extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER), extras.getBoolean(Notification.EXTRA_CHRONOMETER_COUNT_DOWN), sbn.notification.`when`)
            },
        )
        val ranking = currentRanking
        val showOnLock = readSecure("lock_screen_show_notifications")
        val allowPrivate = readSecure("lock_screen_allow_private_notifications")
        val list = active.mapNotNull { sbn ->
            val n = sbn.notification
            val rank = Ranking().takeIf { ranking?.getRanking(sbn.key, it) == true } ?: return@mapNotNull null
            if (!LockNotifications.worthShowing(sbn.packageName, packageName, sbn.isClearable, n.flags and Notification.FLAG_GROUP_SUMMARY != 0, rank.importance)) {
                return@mapNotNull null
            }
            if (!LockNotifications.getsThrough(rank.suppressedVisualEffects, rank.matchesInterruptionFilter(), Build.VERSION.SDK_INT)) {
                return@mapNotNull null
            }
            val detail = LockNotifications.detail(n.visibility, rank.channel?.lockscreenVisibility ?: Ranking.VISIBILITY_NO_OVERRIDE, showOnLock, allowPrivate)
            if (detail == NoteDetail.HIDE) return@mapNotNull null
            // Private ones show what the app chose to make public, if anything, and nothing more is read.
            val full = detail == NoteDetail.FULL
            val shown = if (full) n else n.publicVersion
            val style = if (full) NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n) else null
            val lines = style?.messages.orEmpty()
                .mapNotNull { m -> m.text?.toString()?.takeIf { it.isNotBlank() }?.let { NoteLine(m.person?.name?.toString(), it) } }
            val title = shown?.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.takeIf { it.isNotBlank() }
            held[sbn.key] = n
            LockNotification(
                key = sbn.key,
                packageName = sbn.packageName,
                app = appLabel(sbn.packageName),
                title = title,
                text = shown?.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.takeIf { it.isNotBlank() },
                postedAt = sbn.postTime,
                detail = detail,
                contentIntent = n.contentIntent,
                autoCancel = n.flags and Notification.FLAG_AUTO_CANCEL != 0,
                body = if (full) n.extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.takeIf { it.isNotBlank() } else null,
                messages = lines.takeLast(5),
                actions = if (full) buttons(n) else emptyList(),
                conversation = n.shortcutId,
                who = if (full && style != null) style.conversationTitle?.toString() ?: lines.lastOrNull()?.sender ?: title else null,
                count = lines.size.coerceAtLeast(1),
                missedCall = n.category == CATEGORY_MISSED_CALL,
                people = peopleIn(n),
            )
        }
        held.keys.retainAll(active.map { it.key }.toSet())
        LockNotifications.publish(list.sortedByDescending { it.postedAt })
    }

    /**
     * The names a notification is about, read only to choose which show: the people it lists, a
     * conversation's name and writers, its title, and a missed call's caller.
     */
    private fun peopleIn(n: Notification): List<String> {
        val extras = n.extras ?: return emptyList()
        val listed = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            @Suppress("DEPRECATION")
            extras.getParcelableArrayList<android.app.Person>(Notification.EXTRA_PEOPLE_LIST).orEmpty().mapNotNull { it.name?.toString() }
        } else {
            emptyList()
        }
        val style = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n)
        val writers = style?.messages.orEmpty().mapNotNull { it.person?.name?.toString() }
        val call = if (n.category == CATEGORY_MISSED_CALL) listOfNotNull(extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()) else emptyList()
        return (listed + writers + listOfNotNull(style?.conversationTitle?.toString(), extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()) + call)
            .map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    }

    /** The reply, if any, and up to three other buttons; contextual ones (Android's own suggestions) left out. */
    private fun buttons(n: Notification): List<NoteAction> {
        val all = n.actions.orEmpty().mapIndexedNotNull { i, a ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && a.isContextual) return@mapIndexedNotNull null
            val title = a.title?.toString()?.takeIf { it.isNotBlank() } ?: return@mapIndexedNotNull null
            val reply = a.remoteInputs?.any { it.allowFreeFormInput } == true
            val auth = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && a.isAuthenticationRequired
            NoteAction(title, i, reply, LockNotifications.needsUnlock(auth, reply))
        }
        return all.filter { it.isReply }.take(1) + all.filterNot { it.isReply }.take(3)
    }

    /** Android's own lock-screen settings: not public, so null when this phone won't say. */
    private fun readSecure(name: String): Boolean? =
        runCatching { Settings.Secure.getInt(contentResolver, name, 1) != 0 }.getOrNull()

    private fun appLabel(pkg: String): String =
        runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)

    companion object {
        /** `Notification.CATEGORY_MISSED_CALL`, only named from Android 12. */
        private const val CATEGORY_MISSED_CALL = "missed_call"

        fun hasAccess(context: Context): Boolean =
            context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

        fun component(context: Context) = ComponentName(context, LockNotificationListener::class.java)
    }
}
