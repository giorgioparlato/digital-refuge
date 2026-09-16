package app.bedtime.engine

import app.bedtime.data.SessionLog
import org.junit.Assert.assertEquals
import org.junit.Test

class StatsTest {
    private val hour = 3_600_000L
    private val day = 24 * hour
    private val now = 100 * day

    private fun log(
        start: Long,
        end: Long,
        unlocks: List<Long> = emptyList(),
        endedEarlyAt: Long? = null,
        pauses: List<Long> = emptyList(),
    ) = SessionLog("s", "S", start, end, unlocks, endedEarlyAt, pauses)

    @Test
    fun streakCountsKeptSessionsSinceTheLastUnlock() {
        val history = listOf(
            log(now - 4 * day, now - 4 * day + hour, unlocks = listOf(now - 4 * day + 1)),
            log(now - 3 * day, now - 3 * day + hour),
            log(now - 2 * day, now - 2 * day + hour),
            log(now - day, now - day + hour),
        )
        assertEquals(3, Stats.streak(history, now))
    }

    @Test
    fun runningSessionDoesNotCountYet() {
        val history = listOf(log(now - day, now - day + hour), log(now - hour, now + hour))
        assertEquals(1, Stats.streak(history, now))
    }

    @Test
    fun pauseUnlockBreaksTheStreak() {
        val history = listOf(log(now - 2 * day, now - 2 * day + hour), log(now - day, now - day + hour, unlocks = listOf(now - day + 1)))
        assertEquals(0, Stats.streak(history, now))
    }

    @Test
    fun weekTotals() {
        val history = listOf(
            log(now - 8 * day, now - 8 * day + hour), // older than a week
            log(now - 2 * day, now - 2 * day + 2 * hour), // kept: 120 min
            log(now - day, now - day + 2 * hour, listOf(now - day + hour), endedEarlyAt = now - day + hour), // 60 min, unlocked
            log(now - hour / 2, now + hour), // running: 30 min so far
        )
        assertEquals(WeekStats(protectedMinutes = 120 + 60 + 30, kept = 1, escapes = 1), Stats.week(history, now))
    }

    @Test
    fun switchingBlockingOffBreaksTheStreakToo() {
        val history = listOf(
            log(now - 2 * day, now - 2 * day + hour),
            log(now - day, now - day + hour, pauses = listOf(now - day + 1)),
        )
        assertEquals(0, Stats.streak(history, now))
    }

    @Test
    fun pausesCountAsEscapesForTheWeek() {
        val history = listOf(
            log(now - 2 * day, now - 2 * day + hour, pauses = listOf(now - 2 * day + 1)),
            log(now - day, now - day + hour, unlocks = listOf(now - day + 1)),
        )
        val week = Stats.week(history, now)
        assertEquals(2, week.escapes)
        assertEquals(0, week.kept)
    }
}
