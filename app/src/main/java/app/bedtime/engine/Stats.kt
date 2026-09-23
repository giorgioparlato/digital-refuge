package app.bedtime.engine

import app.bedtime.data.SessionLog

data class WeekStats(val protectedMinutes: Long, val kept: Int, val escapes: Int)

/** Pure summaries of the session history for the home screen. */
object Stats {
    /**
     * When a session you unlocked would have ended, if that moment is still ahead. Its settings stay
     * shut until then, so an unlock can't be used as a way in to soften the next one.
     *
     * Any unlock counts, not only one that ended the session: unlocking a block set to "take a break"
     * leaves the occurrence merely paused, which would otherwise be an unwatched window to edit it in.
     */
    fun lockedUntil(history: List<SessionLog>, scheduleId: String, now: Long): Long? =
        history.filter { it.scheduleId == scheduleId && it.unlockTimes.isNotEmpty() && it.end > now }
            .maxOfOrNull { it.end }

    private const val WEEK_MS = 7 * 24 * 60 * 60 * 1000L

    /** Sessions that started in the last 7 days. */
    fun week(history: List<SessionLog>, now: Long): WeekStats {
        val recent = history.filter { it.start in (now - WEEK_MS)..now }
        val protectedMs = recent.sumOf { ((it.endedEarlyAt ?: it.end).coerceAtMost(now) - it.start).coerceAtLeast(0L) }
        return WeekStats(
            protectedMinutes = protectedMs / 60_000,
            kept = recent.count { it.isFinished(now) && it.escapes == 0 },
            escapes = recent.sumOf { log -> (log.unlockTimes + log.pausedAt).count { it > now - WEEK_MS } },
        )
    }

    /** Most recent finished sessions in a row that ended without an early unlock or a pause. */
    fun streak(history: List<SessionLog>, now: Long): Int =
        history.filter { it.isFinished(now) }
            .sortedByDescending { it.start }
            .takeWhile { it.escapes == 0 }
            .size

    private fun SessionLog.isFinished(now: Long) = (endedEarlyAt ?: end) <= now
}
