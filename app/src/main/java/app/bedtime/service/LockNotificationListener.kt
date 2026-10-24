package app.bedtime.service

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
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    override fun onNotificationRankingUpdate(rankingMap: RankingMap?) = refresh()

    override fun cancel(key: String) {
        runCatching { cancelNotification(key) }
    }

    override fun refresh() {
        val active = runCatching { activeNotifications }.getOrNull() ?: return
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
            // Private ones show what the app chose to make public, if anything.
            val shown = if (detail == NoteDetail.FULL) n else n.publicVersion
            LockNotification(
                key = sbn.key,
                packageName = sbn.packageName,
                app = appLabel(sbn.packageName),
                title = shown?.extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.takeIf { it.isNotBlank() },
                text = shown?.extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.takeIf { it.isNotBlank() },
                postedAt = sbn.postTime,
                detail = detail,
                contentIntent = n.contentIntent,
                autoCancel = n.flags and Notification.FLAG_AUTO_CANCEL != 0,
            )
        }
        LockNotifications.publish(list.sortedByDescending { it.postedAt })
    }

    /** Android's own lock-screen settings: not public, so null when this phone won't say. */
    private fun readSecure(name: String): Boolean? =
        runCatching { Settings.Secure.getInt(contentResolver, name, 1) != 0 }.getOrNull()

    private fun appLabel(pkg: String): String =
        runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)

    companion object {
        fun hasAccess(context: Context): Boolean =
            context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)

        fun component(context: Context) = ComponentName(context, LockNotificationListener::class.java)
    }
}
