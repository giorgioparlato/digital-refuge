package app.bedtime.engine

import app.bedtime.data.SessionLog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class StatsTest {
    @Test
    fun anyUnlockKeepsTheBlockShutForWhatWasLeftOfIt() {
        val now = 10_000_000L
        val hour = 3_600_000L
        // Unlocked in a way that ended the session.
        val ended = SessionLog("a", "A", now - hour, now + hour, unlockTimes = listOf(now), endedEarlyAt = now)
        assertEquals(now + hour, Stats.lockedUntil(listOf(ended), "a", now))

        // Unlocked on a block set to "take a break": merely paused, so endedEarlyAt stays null.
        val paused = SessionLog("b", "B", now - hour, now + hour, unlockTimes = listOf(now))
        assertEquals(now + hour, Stats.lockedUntil(listOf(paused), "b", now))

        // Once the occurrence is past, it unlocks again.
        val over = SessionLog("c", "C", now - 2 * hour, now - hour, unlockTimes = listOf(now - 2 * hour))
        assertNull(Stats.lockedUntil(listOf(over), "c", now))

        // Never unlocked, never locked.
        val clean = SessionLog("d", "D", now - hour, now + hour)
        assertNull(Stats.lockedUntil(listOf(clean), "d", now))
    }

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
        assertEquals(
            WeekStats(protectedMinutes = 120 + 60 + 30, kept = 1, finished = 2, escapes = 1, unlocks = 1),
            Stats.week(history, now),
        )
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

class StatsBreaksTest {
    private val hour = 3_600_000L
    private val day = 24 * hour
    private val now = 100 * day

    @Test
    fun breaksAreCountedApartAndBreakTheStreak() {
        val history = listOf(
            SessionLog("s", "S", now - 3 * day, now - 3 * day + hour),
            SessionLog("s", "S", now - 2 * day, now - 2 * day + hour, breakTimes = listOf(now - 2 * day + 1)),
            SessionLog("s", "S", now - day, now - day + hour, pausedAt = listOf(now - day + 1), unlockTimes = listOf(now - day + 2)),
        )
        val week = Stats.week(history, now)
        assertEquals(1, week.breaks)
        assertEquals(1, week.switchedOff)
        assertEquals(1, week.unlocks)
        assertEquals(3, week.escapes)
        assertEquals(1, week.kept)
        assertEquals(3, week.finished)
        assertEquals(0, Stats.streak(history, now))
    }

    @Test
    fun bestStreakIsTheLongestRun() {
        fun kept(d: Int) = SessionLog("s", "S", now - d * day, now - d * day + hour)
        val history = listOf(kept(9), kept(8), kept(7), SessionLog("s", "S", now - 6 * day, now - 6 * day + hour, unlockTimes = listOf(1)), kept(2))
        assertEquals(3, Stats.bestStreak(history, now))
        assertEquals(1, Stats.streak(history, now))
    }

    @Test
    fun daysMarkKeptEscapedAndEmpty() {
        val zone = java.time.ZoneOffset.UTC
        val history = listOf(
            SessionLog("s", "S", now - day + hour, now - day + 2 * hour),
            SessionLog("s", "S", now - 2 * day + hour, now - 2 * day + 2 * hour, unlockTimes = listOf(1)),
        )
        val days = Stats.days(history, now + hour, zone, count = 4).map { it.second }
        assertEquals(listOf(DayMark.NONE, DayMark.ESCAPED, DayMark.KEPT, DayMark.NONE), days)
    }

    @Test
    fun bySchedulePerSchedule() {
        val history = listOf(
            SessionLog("bed", "Bedtime", now - 2 * day, now - 2 * day + hour),
            SessionLog("bed", "Bedtime", now - day, now - day + hour, unlockTimes = listOf(1)),
            SessionLog("focus", "Focus", now - day, now - day + hour),
        )
        val stats = Stats.bySchedule(history, now)
        assertEquals(ScheduleStats("bed", "Bedtime", 2, 1), stats.first())
        assertEquals(ScheduleStats("focus", "Focus", 1, 1), stats.last())
    }
}
