package app.bedtime.ui.blocked

import org.junit.Assert.assertEquals
import org.junit.Test

class CoverProgressTest {
    @Test
    fun aSessionsProgressRunsFromItsStartToItsEnd() {
        assertEquals(0f, sessionProgress(1_000, 2_000, now = 500), 0f)
        assertEquals(0.25f, sessionProgress(1_000, 2_000, now = 1_250), 0.001f)
        assertEquals(1f, sessionProgress(1_000, 2_000, now = 9_000), 0f)
        // No length to it: nothing to show.
        assertEquals(0f, sessionProgress(2_000, 2_000, now = 2_000), 0f)
    }

    @Test
    fun aLimitsUseSaysItsUnitOnce() {
        assertEquals("15 of 45 min today", usedOf(15, 45))
        assertEquals("50 min of 1 h 30 min today", usedOf(50, 90))
    }
}
