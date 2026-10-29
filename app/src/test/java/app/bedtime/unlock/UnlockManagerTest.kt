package app.bedtime.unlock

import app.bedtime.data.Run
import app.bedtime.data.RuntimeState
import app.bedtime.data.Schedule
import app.bedtime.data.ScheduleKind
import app.bedtime.engine.Occurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UnlockManagerTest {
    private val runtime = RuntimeState(activeRuns = mapOf("focus" to Run(0L, 3_600_000L)))

    @Test
    fun aBreakPausesForItsMinutes() {
        val after = UnlockManager.applied(runtime, Occurrence(Schedule(id = "bed"), 0L, 10L), minutes = 15, now = 1_000L)
        assertEquals(1_000L + 15 * 60_000L, after.overrides["bed"]?.pausedUntil)
    }

    @Test
    fun withoutOneABlockEndsAndAScheduleIsDoneUntilNextTime() {
        val block = Occurrence(Schedule(id = "focus", kind = ScheduleKind.BLOCK), 0L, 3_600_000L)
        assertNull(UnlockManager.applied(runtime, block, minutes = null, now = 1_000L).activeRuns["focus"])
        val bed = Occurrence(Schedule(id = "bed"), 500L, 9_000L)
        assertEquals(500L, UnlockManager.applied(runtime, bed, minutes = null, now = 1_000L).overrides["bed"]?.endedOccurrenceStart)
    }
}
