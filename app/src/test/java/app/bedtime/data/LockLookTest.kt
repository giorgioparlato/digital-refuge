package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LockLookTest {
    private val home = HomeStyle(background = 0xFF111111, clockSize = ClockSize.LARGE, noteLines = 6)

    @Test
    fun theLockScreenFollowsHomeUntilChanged() {
        assertEquals(home.background, home.on(Screen.LOCK).background)
        assertEquals(ClockSize.LARGE, home.on(Screen.LOCK).clockSize)
    }

    @Test
    fun aChangeOnTheLockScreenIsItsOwn() {
        val changed = home.edit(Screen.LOCK) { it.copy(clockSize = ClockSize.SMALL, background = 0xFF000000) }
        assertEquals(ClockSize.SMALL, changed.on(Screen.LOCK).clockSize)
        assertEquals(0xFF000000, changed.on(Screen.LOCK).background)
        // The home screen keeps its own.
        assertEquals(ClockSize.LARGE, changed.clockSize)
        assertEquals(0xFF111111, changed.background)
        // And what wasn't touched still follows home.
        assertNull(changed.lock.noteLines)
        assertEquals(10, changed.copy(noteLines = 10).on(Screen.LOCK).noteLines)
    }

    @Test
    fun whatIsntTheLockScreensOwnChangesForBoth() {
        val changed = home.edit(Screen.LOCK) { it.copy(showIcons = true) }
        assertEquals(true, changed.showIcons)
        assertEquals(LockLook(), changed.lock)
    }

    @Test
    fun onHomeAChangeIsJustMade() {
        assertEquals(ClockSize.SMALL, home.edit(Screen.HOME) { it.copy(clockSize = ClockSize.SMALL) }.clockSize)
    }
}
