package app.bedtime.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RunClockTest {
    @Test
    fun aRunCountsFromTheUnlock() {
        val clock = RunClock()
        assertNull(clock.dueAt(15))
        clock.start(1_000)
        // A second unlock signal mid-run doesn't start it again.
        clock.start(5_000)
        assertEquals(1_000 + 15 * 60_000L, clock.dueAt(15))
        assertNull(clock.dueAt(null))
    }

    @Test
    fun theScreenGoingOffEndsItAndCarryingOnRestartsIt() {
        val clock = RunClock()
        clock.start(0)
        clock.restart(600_000)
        assertEquals(600_000 + 5 * 60_000L, clock.dueAt(5))
        clock.stop()
        assertNull(clock.dueAt(5))
    }
}
