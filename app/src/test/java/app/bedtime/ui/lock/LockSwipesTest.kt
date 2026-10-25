package app.bedtime.ui.lock

import app.bedtime.data.Swipe
import org.junit.Assert.assertEquals
import org.junit.Test

class LockSwipesTest {
    private val w = 1000f
    private val h = 2000f
    private val min = 100f

    private fun read(x: Float, y: Float, dx: Float, dy: Float) = readLockSwipe(x, y, w, h, dx, dy, min)

    @Test
    fun theFourSwipesFromTheTop() {
        assertEquals(LockSwipe.From(Swipe.UP), read(500f, 400f, 20f, -400f))
        assertEquals(LockSwipe.From(Swipe.DOWN), read(500f, 400f, -30f, 400f))
        assertEquals(LockSwipe.From(Swipe.LEFT), read(500f, 600f, -400f, 50f))
        assertEquals(LockSwipe.From(Swipe.RIGHT), read(500f, 300f, 400f, 0f))
    }

    @Test
    fun theMiddleIsLeftToTheNotifications() {
        assertEquals(LockSwipe.None, read(500f, 1000f, 400f, 0f))
        assertEquals(LockSwipe.None, read(500f, 1200f, 0f, -400f))
    }

    @Test
    fun anySwipeFromTheBottomJustUnlocks() {
        assertEquals(LockSwipe.Unlock, read(500f, 1900f, 0f, -600f))
        assertEquals(LockSwipe.Unlock, read(500f, 1900f, 400f, 0f))
        assertEquals(LockSwipe.Unlock, read(500f, 1600f, -400f, 0f))
    }

    @Test
    fun theSidesUnlockAndTheTopPullsTheShade() {
        assertEquals(LockSwipe.Unlock, read(30f, 1000f, 400f, 0f))
        // A pull down from the very top is Android's notification shade, as on any lock screen.
        assertEquals(LockSwipe.Shade, read(500f, 50f, 0f, 400f))
        // Sideways along the top edge still just unlocks.
        assertEquals(LockSwipe.Unlock, read(500f, 50f, 400f, 30f))
    }

    @Test
    fun aShortMoveIsNothing() {
        assertEquals(LockSwipe.None, read(500f, 400f, 40f, -60f))
    }
}
