package app.bedtime.ui.minimal

import app.bedtime.data.HomeGesture
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeDragTest {
    private fun read(dx: Float, dy: Float) = readHomeDrag(dx, dy, minDistance = 100f)

    @Test
    fun theLongerWayWins() {
        assertEquals(HomeDrag.AllApps, read(30f, -300f))
        assertEquals(HomeDrag.Gesture(HomeGesture.SWIPE_DOWN), read(-40f, 300f))
        assertEquals(HomeDrag.Gesture(HomeGesture.SWIPE_LEFT), read(-300f, 50f))
        assertEquals(HomeDrag.Gesture(HomeGesture.SWIPE_RIGHT), read(300f, -80f))
    }

    @Test
    fun aShortMoveIsNothing() {
        assertEquals(HomeDrag.None, read(60f, -80f))
    }
}
