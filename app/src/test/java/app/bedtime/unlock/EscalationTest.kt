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
    fun countsOnlyThisScheduleAndOnlyToday() {
        // Fixed at midday UTC so "today" is unambiguous whenever the test runs.
        val zone = java.time.ZoneId.of("UTC")
        val noon = java.time.LocalDate.of(2026, 9, 22).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
        val history = listOf(
            SessionLog("a", "A", 0, 1, unlockTimes = listOf(noon - 2 * hour, noon - 30 * hour)),
            SessionLog("a", "A", 2, 3, unlockTimes = listOf(noon - 5 * hour)),
            SessionLog("b", "B", 0, 1, unlockTimes = listOf(noon - hour)),
        )
        assertEquals(2, Escalation.recentUnlocks(history, "a", noon, zone))
        assertEquals(1, Escalation.recentUnlocks(history, "b", noon, zone))
        assertEquals(0, Escalation.recentUnlocks(history, "c", noon, zone))
    }

    @Test
    fun yesterdaysUnlocksDontCarryIntoToday() {
        val zone = java.time.ZoneId.of("UTC")
        val elevenPm = java.time.LocalDate.of(2026, 9, 21).atTime(23, 0).atZone(zone).toInstant().toEpochMilli()
        val tenPmNextDay = java.time.LocalDate.of(2026, 9, 22).atTime(22, 0).atZone(zone).toInstant().toEpochMilli()
        val history = listOf(SessionLog("a", "A", 0, 1, unlockTimes = listOf(elevenPm)))
        // Within 24 hours, but a different day: the challenge starts clean again.
        assertEquals(0, Escalation.recentUnlocks(history, "a", tenPmNextDay, zone))
        assertEquals(1, Escalation.recentUnlocks(history, "a", elevenPm, zone))
    }
}
