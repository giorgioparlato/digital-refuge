package app.bedtime.ui.settings

import app.bedtime.data.Schedule
import app.bedtime.data.SessionLog
import app.bedtime.engine.ActiveState
import app.bedtime.engine.Occurrence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlwaysAvailableLockTest {
    private val hour = 3_600_000L
    private val now = 10_000_000L
    private val bedtime = Schedule(id = "a", name = "Bedtime")

    private fun running(schedule: Schedule, end: Long) =
        ActiveState(listOf(Occurrence(schedule, now - hour, end)), nextBoundary = end)

    @Test
    fun openWhenNothingIsRunning() {
        assertNull(alwaysAvailableLockedUntil(listOf(bedtime), emptyList(), ActiveState(emptyList(), null), now))
        assertNull(alwaysAvailableLockedUntil(listOf(bedtime), emptyList(), null, now))
    }

    @Test
    fun shutWhileASessionRuns() {
        val active = running(bedtime, now + hour)
        assertEquals(now + hour, alwaysAvailableLockedUntil(listOf(bedtime), emptyList(), active, now))
    }

    @Test
    fun theLongestRunningSessionWins() {
        val other = Schedule(id = "b", name = "Deep work")
        val active = ActiveState(
            listOf(Occurrence(bedtime, now - hour, now + hour), Occurrence(other, now - hour, now + 3 * hour)),
            nextBoundary = now + hour,
        )
        assertEquals(now + 3 * hour, alwaysAvailableLockedUntil(listOf(bedtime, other), emptyList(), active, now))
    }

    @Test
    fun anEarlyUnlockKeepsItShutForWhatWasLeft() {
        // Nothing is running any more, because the unlock ended it — the list must not open early.
        val history = listOf(SessionLog("a", "Bedtime", now - hour, now + hour, unlockTimes = listOf(now)))
        val state = ActiveState(emptyList(), null)
        assertEquals(now + hour, alwaysAvailableLockedUntil(listOf(bedtime), history, state, now))
    }

    @Test
    fun aScheduleThatAllowsEditingAfterUnlockDoesNotLockIt() {
        val lenient = bedtime.copy(editAfterUnlock = true)
        val history = listOf(SessionLog("a", "Bedtime", now - hour, now + hour, unlockTimes = listOf(now)))
        assertNull(alwaysAvailableLockedUntil(listOf(lenient), history, ActiveState(emptyList(), null), now))
    }

    @Test
    fun oneStrictScheduleIsEnoughToLockIt() {
        val lenient = bedtime.copy(editAfterUnlock = true)
        val strict = Schedule(id = "b", name = "Deep work", editAfterUnlock = false)
        val history = listOf(
            SessionLog("a", "Bedtime", now - hour, now + hour, unlockTimes = listOf(now)),
            SessionLog("b", "Deep work", now - hour, now + 2 * hour, unlockTimes = listOf(now)),
        )
        assertEquals(
            now + 2 * hour,
            alwaysAvailableLockedUntil(listOf(lenient, strict), history, ActiveState(emptyList(), null), now),
        )
    }

    @Test
    fun opensAgainOncePastSessionsAreOver() {
        val history = listOf(SessionLog("a", "Bedtime", now - 3 * hour, now - hour, unlockTimes = listOf(now - 2 * hour)))
        assertNull(alwaysAvailableLockedUntil(listOf(bedtime), history, ActiveState(emptyList(), null), now))
    }
}
