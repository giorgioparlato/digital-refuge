package app.bedtime.unlock

import app.bedtime.data.SessionLog
import org.junit.Assert.assertEquals
import org.junit.Test

class EscalationTest {
    private val hour = 3_600_000L
    private val now = 1_000 * hour

    @Test
    fun growsByHalfPerUnlockAndCapsAtTriple() {
        assertEquals(200, Escalation.textLength(200, 0))
        assertEquals(300, Escalation.textLength(200, 1))
        assertEquals(450, Escalation.textLength(200, 2))
        assertEquals(600, Escalation.textLength(200, 3))
        assertEquals(600, Escalation.textLength(200, 6))
    }

    @Test
    fun countsOnlyThisScheduleWithinTheLastDay() {
        val history = listOf(
            SessionLog("a", "A", 0, 1, unlockTimes = listOf(now - 2 * hour, now - 30 * hour)),
            SessionLog("a", "A", 2, 3, unlockTimes = listOf(now - 5 * hour)),
            SessionLog("b", "B", 0, 1, unlockTimes = listOf(now - hour)),
        )
        assertEquals(2, Escalation.recentUnlocks(history, "a", now))
        assertEquals(1, Escalation.recentUnlocks(history, "b", now))
        assertEquals(0, Escalation.recentUnlocks(history, "c", now))
    }
}
