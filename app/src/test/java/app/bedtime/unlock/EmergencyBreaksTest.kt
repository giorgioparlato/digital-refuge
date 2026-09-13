package app.bedtime.unlock

import app.bedtime.data.EmergencyEntry
import app.bedtime.data.SessionLog
import org.junit.Assert.assertEquals
import org.junit.Test

class EmergencyBreaksTest {
    private val day = 86_400_000L
    private val now = 100 * day

    @Test
    fun countsOnlyBreaksFromTheLastSevenDays() {
        val history = listOf(
            SessionLog("a", "A", 0, 1, emergencies = listOf(EmergencyEntry(now - day, "call"), EmergencyEntry(now - 8 * day, "old"))),
            SessionLog("b", "B", 0, 1, emergencies = listOf(EmergencyEntry(now - 2 * day, "ride"))),
        )
        assertEquals(2, EmergencyBreaks.usedThisWeek(history, now))
        assertEquals(EmergencyBreaks.PER_WEEK - 2, EmergencyBreaks.remaining(history, now))
    }

    @Test
    fun remainingNeverGoesBelowZero() {
        val many = (1..5).map { EmergencyEntry(now - it * 3_600_000L, "again") }
        assertEquals(0, EmergencyBreaks.remaining(listOf(SessionLog("a", "A", 0, 1, emergencies = many)), now))
    }
}
