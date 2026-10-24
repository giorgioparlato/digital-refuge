package app.bedtime.data

import android.app.Notification
import android.app.NotificationManager
import android.service.notification.NotificationListenerService.Ranking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockNotificationsTest {
    private val none = Ranking.VISIBILITY_NO_OVERRIDE

    @Test
    fun onlyWhatAndroidsOwnShadeWouldShow() {
        val hidden = NotificationManager.Policy.SUPPRESSED_EFFECT_NOTIFICATION_LIST or NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK
        // Android 9 and later: a session's "hide notifications" takes it out of the shade, and so out of here.
        assertFalse(LockNotifications.getsThrough(hidden, matchesFilter = false, sdk = 34))
        assertTrue(LockNotifications.getsThrough(0, matchesFilter = false, sdk = 34))
        assertTrue(LockNotifications.getsThrough(NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK, matchesFilter = false, sdk = 34))
        // Android 8: held back by Do Not Disturb while anything is suppressed.
        assertFalse(LockNotifications.getsThrough(NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_ON, matchesFilter = false, sdk = 26))
        assertTrue(LockNotifications.getsThrough(NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_ON, matchesFilter = true, sdk = 26))
        assertTrue(LockNotifications.getsThrough(0, matchesFilter = false, sdk = 26))
    }

    @Test
    fun privacyFollowsAndroidsLockScreen() {
        val private = Notification.VISIBILITY_PRIVATE
        assertEquals(NoteDetail.FULL, LockNotifications.detail(private, none, showOnLock = true, allowPrivate = true))
        assertEquals(NoteDetail.APP_ONLY, LockNotifications.detail(private, none, showOnLock = true, allowPrivate = false))
        assertEquals(NoteDetail.FULL, LockNotifications.detail(Notification.VISIBILITY_PUBLIC, none, showOnLock = true, allowPrivate = false))
        assertEquals(NoteDetail.HIDE, LockNotifications.detail(Notification.VISIBILITY_SECRET, none, showOnLock = true, allowPrivate = true))
        // A channel can make it stricter, never looser.
        assertEquals(NoteDetail.HIDE, LockNotifications.detail(Notification.VISIBILITY_PUBLIC, Notification.VISIBILITY_SECRET, true, true))
        assertEquals(NoteDetail.APP_ONLY, LockNotifications.detail(Notification.VISIBILITY_PUBLIC, private, true, false))
        assertEquals(NoteDetail.APP_ONLY, LockNotifications.detail(private, Notification.VISIBILITY_PUBLIC, true, false))
        // No notifications on the lock screen at all.
        assertEquals(NoteDetail.HIDE, LockNotifications.detail(Notification.VISIBILITY_PUBLIC, none, showOnLock = false, allowPrivate = true))
        // Settings this phone won't share: shown, but privately.
        assertEquals(NoteDetail.APP_ONLY, LockNotifications.detail(private, none, showOnLock = null, allowPrivate = null))
    }

    @Test
    fun leavesOutOursOngoingSummariesAndMinimal() {
        val default = NotificationManager.IMPORTANCE_DEFAULT
        assertTrue(LockNotifications.worthShowing("com.chat", "app.bedtime", clearable = true, groupSummary = false, importance = default))
        assertFalse(LockNotifications.worthShowing("app.bedtime", "app.bedtime", true, false, default))
        assertFalse(LockNotifications.worthShowing("com.music", "app.bedtime", clearable = false, groupSummary = false, importance = default))
        assertFalse(LockNotifications.worthShowing("com.chat", "app.bedtime", clearable = true, groupSummary = true, importance = default))
        assertFalse(LockNotifications.worthShowing("com.chat", "app.bedtime", true, false, NotificationManager.IMPORTANCE_MIN))
    }
}
