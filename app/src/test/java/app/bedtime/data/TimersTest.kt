package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimersTest {
    private val now = 1_000_000L

    @Test
    fun theSameTimerFromBothSourcesIsOne() {
        // Set from the search, and seen in the clock app's notification a moment later.
        assertEquals(listOf(now + 60_000), Timers.running(listOf(now + 60_000), listOf(now + 61_200), now))
    }

    @Test
    fun endedAndHiddenOnesAreLeftOut() {
        val ends = listOf(now - 1, now + 300_000, now + 60_000)
        assertEquals(listOf(now + 60_000, now + 300_000), Timers.running(ends, emptyList(), now))
        assertEquals(listOf(now + 300_000), Timers.running(ends, emptyList(), now, hidden = setOf(now + 60_500)))
    }

    @Test
    fun onlyACountdownIsATimer() {
        assertEquals(now + 5_000, Timers.countdownEnd(showsChronometer = true, countsDown = true, at = now + 5_000))
        assertNull(Timers.countdownEnd(showsChronometer = true, countsDown = false, at = now))
        assertNull(Timers.countdownEnd(showsChronometer = false, countsDown = false, at = now))
    }

    @Test
    fun whatIsLeftRoundsUp() {
        assertEquals("4:12", Timers.left(now + 251_200, now))
        assertEquals("0:01", Timers.left(now + 1, now))
        assertEquals("1:02:05", Timers.left(now + 3_725_000, now))
    }
}
