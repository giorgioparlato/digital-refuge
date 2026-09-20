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
    fun textLengthUsesTheGivenFactor() {
        assertEquals(400, Escalation.textLength(200, 1, factor = 2f))
        assertEquals(600, Escalation.textLength(200, 2, factor = 2f)) // 800, capped at 3x = 600
    }

    @Test
    fun waitGrowsAndIsCappedAtEightTimes() {
        assertEquals(30, Escalation.waitSeconds(30, 0, factor = 2f))
        assertEquals(60, Escalation.waitSeconds(30, 1, factor = 2f))
        assertEquals(240, Escalation.waitSeconds(30, 3, factor = 2f)) // 30x8 = 240 (the cap)
        assertEquals(240, Escalation.waitSeconds(30, 5, factor = 2f)) // still capped
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
