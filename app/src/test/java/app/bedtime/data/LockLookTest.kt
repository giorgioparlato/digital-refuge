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
    fun resettingTheLookKeepsEverythingElse() {
        val set = home.copy(
            accent = 0xFF123456,
            lockScreen = false,
            lockGestures = mapOf(Swipe.UP to GESTURE_CAMERA),
            homeGestures = mapOf(HomeGesture.DOUBLE_TAP to HomeActions.LOCK),
            lockNotificationApps = setOf("a.b"),
            quickReplies = listOf("on my way"),
            homeModules = listOf(ModuleChoice(Module.QUOTE, true)),
            customFontName = "mine.ttf",
        )
        val reset = set.edit(Screen.HOME) { it.withDefaultLook(Screen.HOME) }
        assertEquals(HomeStyle().background, reset.background)
        assertEquals(HomeStyle().accent, reset.accent)
        // The clock's size belongs to its widget, not to the look.
        assertEquals(ClockSize.LARGE, reset.clockSize)
        assertEquals(false, reset.lockScreen)
        assertEquals(set.lockGestures, reset.lockGestures)
        assertEquals(set.homeGestures, reset.homeGestures)
        assertEquals(set.lockNotificationApps, reset.lockNotificationApps)
        assertEquals(set.quickReplies, reset.quickReplies)
        assertEquals(set.homeModules, reset.homeModules)
        assertEquals("mine.ttf", reset.customFontName)
        // The lock screen keeps the look it had.
        assertEquals(0xFF123456, reset.on(Screen.LOCK).accent)
    }

    @Test
    fun resettingTheLockScreensLookLeavesHomeAlone() {
        val set = home.edit(Screen.LOCK) { it.copy(accent = 0xFF654321, textSize = TextSize.LARGE) }
        val reset = set.edit(Screen.LOCK) { it.withDefaultLook(Screen.LOCK) }
        assertEquals(HomeStyle().accent, reset.on(Screen.LOCK).accent)
        assertEquals(TextSize.MEDIUM, reset.on(Screen.LOCK).textSize)
        assertEquals(0xFF111111, reset.background)
        assertEquals(ClockSize.LARGE, reset.clockSize)
    }

    @Test
    fun onHomeAChangeIsJustMade() {
        assertEquals(ClockSize.SMALL, home.edit(Screen.HOME) { it.copy(clockSize = ClockSize.SMALL) }.clockSize)
    }
}
