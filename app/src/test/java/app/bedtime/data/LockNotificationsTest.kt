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

    private fun note(key: String, app: String = "Messages", who: String? = null, count: Int = 1, missedCall: Boolean = false, conversation: String? = null) =
        LockNotification(key, "pkg.$app", app, null, null, 0L, NoteDetail.FULL, who = who, count = count, missedCall = missedCall, conversation = conversation)

    @Test
    fun summaryReadsCalmly() {
        assertEquals("", LockNotifications.summary(emptyList()))
        assertEquals(
            "a missed call · 3 from anna · 1 from mail",
            LockNotifications.summary(listOf(note("a", who = "anna", count = 3), note("m", app = "mail"), note("c", app = "Phone", missedCall = true))),
        )
        assertEquals("2 missed calls", LockNotifications.summary(listOf(note("c1", missedCall = true), note("c2", missedCall = true))))
        // Two notifications from the same person add up.
        assertEquals("3 from anna", LockNotifications.summary(listOf(note("a1", who = "anna", count = 2), note("a2", who = "anna"))))
        assertEquals(
            "3 from anna · 2 from bo · 1 from cy · and more",
            LockNotifications.summary(listOf(note("a", who = "anna", count = 3), note("b", who = "bo", count = 2), note("c", who = "cy"), note("d", who = "di"))),
        )
    }

    @Test
    fun repliesAlwaysAskForTheUnlockButtonsOnlyWhenTheirAppSays() {
        assertTrue(LockNotifications.needsUnlock(authenticationRequired = false, isReply = true))
        assertTrue(LockNotifications.needsUnlock(authenticationRequired = true, isReply = false))
        assertFalse(LockNotifications.needsUnlock(authenticationRequired = false, isReply = false))
    }

    @Test
    fun quietCoversTheConversationOrTheApp() {
        val chat = note("a", app = "Signal", conversation = "anna")
        val mail = note("m", app = "Mail")
        assertEquals("pkg.Signal/anna", LockNotifications.quietKey(chat))
        assertEquals("pkg.Mail", LockNotifications.quietKey(mail))
        val quiet = mapOf("pkg.Signal/anna" to 2_000L, "pkg.Mail" to 500L)
        assertTrue(LockNotifications.isQuiet(chat, quiet, now = 1_000L))
        assertFalse(LockNotifications.isQuiet(note("b", app = "Signal", conversation = "bo"), quiet, now = 1_000L))
        // Over once the session has ended.
        assertFalse(LockNotifications.isQuiet(mail, quiet, now = 1_000L))
        // Quieting a whole app covers its conversations too.
        assertTrue(LockNotifications.isQuiet(chat, mapOf("pkg.Signal" to 2_000L), now = 1_000L))
    }

    @Test
    fun laterLastsUntilTheSessionEndsAndAtLeastAMinute() {
        assertEquals(3_600_000L, LockNotifications.snoozeFor(now = 0L, until = 3_600_000L))
        assertEquals(60_000L, LockNotifications.snoozeFor(now = 10_000L, until = 20_000L))
    }

    private val close = Circle("close", "Close", people = setOf("anna"), groups = setOf("Family"))
    private val known = Circle("known", "Known", people = setOf("Bo"))

    @Test
    fun fromYourAppsOrFromYourCircles() {
        val fromAnna = LockNotification("k", "com.whatsapp", "WhatsApp", "Anna", null, 0L, NoteDetail.FULL, people = listOf("Anna"))
        val fromCy = fromAnna.copy(key = "c", people = listOf("Cy"))
        // Not a chosen app, but someone in a circle: shows.
        assertTrue(LockNotifications.wanted(fromAnna, LockNotifications.Chosen(setOf("com.signal"), emptySet(), listOf(close))))
        assertFalse(LockNotifications.wanted(fromCy, LockNotifications.Chosen(setOf("com.signal"), emptySet(), listOf(close))))
        // A chosen app shows whoever it's from; a session's own people count too.
        assertTrue(LockNotifications.wanted(fromCy, LockNotifications.Chosen(setOf("com.whatsapp"), emptySet(), emptyList())))
        assertTrue(LockNotifications.wanted(fromCy, LockNotifications.Chosen(emptySet(), setOf("cy"), emptyList())))
    }

    @Test
    fun aGroupBelongsByItsNameNotByWhoWritesInIt() {
        val family = LockNotification("f", "com.whatsapp", "WhatsApp", "Family", null, 0L, NoteDetail.FULL, people = listOf("Bo"), group = "family")
        val football = family.copy(key = "g", people = listOf("Anna"), group = "Football")
        assertEquals(close, LockNotifications.circleOf(family, listOf(known, close)))
        // Anna is close, but her football group isn't.
        assertEquals(null, LockNotifications.circleOf(football, listOf(close)))
    }

    @Test
    fun circlesComeFirstInTheirOrder() {
        val fromBo = LockNotification("b", "x", "X", null, null, 300L, NoteDetail.FULL, people = listOf("Bo"))
        val fromAnna = fromBo.copy(key = "a", postedAt = 100L, people = listOf("Anna"))
        val other = fromBo.copy(key = "o", postedAt = 900L, people = emptyList())
        assertEquals(listOf("a", "b", "o"), LockNotifications.sorted(listOf(other, fromBo, fromAnna), listOf(close, known)).map { it.key })
    }

    @Test
    fun theFirstCircleStartsWithThePeopleChosenBefore() {
        assertEquals(setOf("Anna"), Circles.of(HomeStyle(lockNotificationPeople = setOf("Anna"))).first().people)
        assertEquals(listOf(known), Circles.of(HomeStyle(circles = listOf(known))))
    }

    @Test
    fun aSessionCanChooseItsOwnOrNone() {
        val circles = listOf(close, known)
        val own = Schedule(name = "Work", lockNotes = LockNotes.OWN, lockNoteApps = setOf("slack"), lockNoteCircles = setOf("known"))
        val none = Schedule(name = "Bed", lockNotes = LockNotes.NONE)
        val usual = LockNotifications.Chosen(setOf("signal"), emptySet(), circles)
        assertEquals(usual, LockNotifications.chosen(setOf("signal"), circles, emptyList()))
        assertEquals(usual, LockNotifications.chosen(setOf("signal"), circles, listOf(Schedule())))
        assertEquals(LockNotifications.Chosen(setOf("slack"), emptySet(), listOf(known)), LockNotifications.chosen(setOf("signal"), circles, listOf(Schedule(), own)))
        // The strictest wins.
        assertEquals(null, LockNotifications.chosen(setOf("signal"), circles, listOf(own, none)))
    }

    @Test
    fun ourRainAlertAndWeekShowButNotTheSession() {
        val default = NotificationManager.IMPORTANCE_DEFAULT
        assertTrue(LockNotifications.worthShowing("app.bedtime", "app.bedtime", true, false, default, channel = "rain"))
        assertTrue(LockNotifications.worthShowing("app.bedtime", "app.bedtime", true, false, default, channel = "look_back"))
        assertFalse(LockNotifications.worthShowing("app.bedtime", "app.bedtime", true, false, default, channel = "session_running"))
        // And whatever apps are chosen.
        val rain = LockNotification("r", "app.bedtime", "digital refuge", "rain today", null, 0L, NoteDetail.FULL, ours = true)
        assertTrue(LockNotifications.wanted(rain, LockNotifications.Chosen(emptySet(), emptySet(), emptyList())))
    }

    @Test
    fun directionsAreTold() {
        assertTrue(LockNotifications.isNavigation("navigation", ongoing = true, packageName = "any.nav.app"))
        assertTrue(LockNotifications.isNavigation(null, ongoing = true, packageName = "com.google.android.apps.maps"))
        // Maps' other notifications (a review asked for, a place saved) aren't directions.
        assertFalse(LockNotifications.isNavigation(null, ongoing = false, packageName = "com.google.android.apps.maps"))
        assertFalse(LockNotifications.isNavigation("msg", ongoing = true, packageName = "com.whatsapp"))
    }
}
