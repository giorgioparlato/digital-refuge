package app.bedtime.engine

import app.bedtime.data.SessionLog

data class WeekStats(val protectedMinutes: Long, val kept: Int, val earlyUnlocks: Int)

/** Pure summaries of the session history for the home screen. */
object Stats {
    private const val WEEK_MS = 7 * 24 * 60 * 60 * 1000L

    /** Sessions that started in the last 7 days. */
    fun week(history: List<SessionLog>, now: Long): WeekStats {
        val recent = history.filter { it.start in (now - WEEK_MS)..now }
        val protectedMs = recent.sumOf { ((it.endedEarlyAt ?: it.end).coerceAtMost(now) - it.start).coerceAtLeast(0L) }
        return WeekStats(
            protectedMinutes = protectedMs / 60_000,
            kept = recent.count { it.isFinished(now) && it.unlocks == 0 },
            earlyUnlocks = recent.sumOf { log -> log.unlockTimes.count { it > now - WEEK_MS } },
        )
    }

    /** Most recent finished sessions in a row that ended without an early unlock. */
    fun streak(history: List<SessionLog>, now: Long): Int =
        history.filter { it.isFinished(now) }
            .sortedByDescending { it.start }
            .takeWhile { it.unlocks == 0 }
            .size

    private fun SessionLog.isFinished(now: Long) = (endedEarlyAt ?: end) <= now
}
