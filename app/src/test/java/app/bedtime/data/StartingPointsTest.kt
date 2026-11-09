package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class StartingPointsTest {
    @Test
    fun strictnessFillsTheStepsInAndIsYourOwnOnceChanged() {
        val firm = Strictness.FIRM.applied(UnlockConfig())
        assertEquals(600, firm.waitDurationSeconds)
        assertEquals(TextSource.PASSAGES, firm.textSource)
        assertFalse(firm.escalate)
        assertEquals(Strictness.FIRM, Strictness.of(firm))
        assertEquals(Strictness.STRICT, Strictness.of(Strictness.STRICT.applied(UnlockConfig())))
        // A 10-minute wait set in minutes is the same wait.
        assertEquals(Strictness.FIRM, Strictness.of(firm.copy(waitSeconds = null, waitMinutes = 10)))
        assertNull(Strictness.of(firm.copy(textLength = 200)))
        // A switched-off step's settings don't matter.
        assertEquals(Strictness.GENTLE, Strictness.of(Strictness.GENTLE.applied(UnlockConfig()).copy(textLength = 999)))
    }

    @Test
    fun aHushSetsTheThreeAndIsYourOwnOnceChanged() {
        val quiet = Hush.QUIET.applied(Schedule())
        assertEquals(DndMode.PRIORITY to LockNotes.OWN, quiet.dnd to quiet.lockNotes)
        assertEquals(Hush.QUIET, Hush.of(quiet))
        assertNull(Hush.of(quiet.copy(hideNotifications = false)))
        assertEquals(Hush.USUAL, Hush.of(Schedule()))
    }

    @Test
    fun anOlderScheduleIsSettledToDoJustWhatItDid() {
        val old = Schedule(minimalMode = true, blockedApps = setOf("x"), allowedApps = setOf("x", "maps"), dnd = DndMode.OFF, hideNotifications = true).settled()
        assertEquals(true, old.allowOnly)
        assertEquals(setOf("maps"), old.allowedApps)
        assertEquals(DndMode.PRIORITY, old.dnd)
        // Settling twice changes nothing.
        assertEquals(old, old.settled())
        assertEquals(false, Schedule(blockedApps = setOf("x")).settled().allowOnly)
    }

    @Test
    fun aLimitStartsFromAPauseOrMinutesAndKeepsItsApps() {
        val rules = LimitRules(apps = setOf("insta"), dailyOpens = 5)
        val thirty = LimitChoice.THIRTY.applied(rules)
        assertEquals(setOf("insta"), thirty.apps)
        assertEquals(30, thirty.dailyMinutes)
        assertNull(thirty.dailyOpens)
        assertNull(thirty.pauseSeconds)
        assertEquals(LimitChoice.THIRTY, LimitChoice.of(thirty))
        assertEquals(LimitChoice.PAUSE, LimitChoice.of(LimitChoice.PAUSE.applied(rules)))
        // Changed after, it's your own.
        assertNull(LimitChoice.of(thirty.copy(dailyMinutes = 40)))
    }
}
