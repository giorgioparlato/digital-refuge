package app.bedtime.engine

import app.bedtime.data.SessionLog
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class WeekStats(
    val protectedMinutes: Long,
    val kept: Int,
    /** Sessions that finished this week, kept or not. */
    val finished: Int = kept,
    /** Every way out, of whichever kind; the three below add up to it. */
    val escapes: Int,
    val unlocks: Int = 0,
    val breaks: Int = 0,
    /** Blocking switched off some other way than an emergency break (Settings, safe mode). */
    val switchedOff: Int = 0,
)

/** How a day went, for the strip of days: nothing ran, everything held, or something was escaped. */
enum class DayMark { NONE, KEPT, ESCAPED }

/** One day of the bars: minutes in refuge and how the day went. */
data class DayBar(val date: LocalDate, val minutes: Long, val mark: DayMark)

/** How one schedule or block has gone lately. */
data class ScheduleStats(val scheduleId: String, val name: String, val sessions: Int, val kept: Int)

/** Pure summaries of the session history. */
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

    private const val DAY_MS = 24 * 60 * 60 * 1000L
    private const val WEEK_MS = 7 * DAY_MS

    /** Sessions that started in the last 7 days. */
    fun week(history: List<SessionLog>, now: Long): WeekStats {
        val recent = history.filter { it.start in (now - WEEK_MS)..now }
        val protectedMs = recent.sumOf { ((it.endedEarlyAt ?: it.end).coerceAtMost(now) - it.start).coerceAtLeast(0L) }
        fun count(times: (SessionLog) -> List<Long>) = recent.sumOf { log -> times(log).count { it > now - WEEK_MS } }
        val unlocks = count { it.unlockTimes }
        val breaks = count { it.breakTimes }
        val switchedOff = count { it.pausedAt }
        return WeekStats(
            protectedMinutes = protectedMs / 60_000,
            kept = recent.count { it.isFinished(now) && it.escapes == 0 },
            finished = recent.count { it.isFinished(now) },
            escapes = unlocks + breaks + switchedOff,
            unlocks = unlocks,
            breaks = breaks,
            switchedOff = switchedOff,
        )
    }

    /** Most recent finished sessions in a row that ended without an early unlock, a break or a pause. */
    fun streak(history: List<SessionLog>, now: Long): Int =
        history.filter { it.isFinished(now) }
            .sortedByDescending { it.start }
            .takeWhile { it.escapes == 0 }
            .size

    /** The longest run of kept sessions in the whole history. */
    fun bestStreak(history: List<SessionLog>, now: Long): Int {
        var best = 0
        var run = 0
        history.filter { it.isFinished(now) }.sortedBy { it.start }.forEach {
            run = if (it.escapes == 0) run + 1 else 0
            best = maxOf(best, run)
        }
        return best
    }

    /**
     * The last [count] days, oldest first and ending today: minutes in refuge in the sessions that
     * started that day, and whether any of them was escaped. A session still running counts so far,
     * and as kept so far.
     */
    fun daily(history: List<SessionLog>, now: Long, zone: ZoneId, count: Int = 14): List<DayBar> {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val byDay = history.filter { it.start <= now }.groupBy { Instant.ofEpochMilli(it.start).atZone(zone).toLocalDate() }
        return (count - 1 downTo 0).map { back ->
            val day = today.minusDays(back.toLong())
            val logs = byDay[day].orEmpty()
            DayBar(
                date = day,
                minutes = logs.sumOf { ((it.endedEarlyAt ?: it.end).coerceAtMost(now) - it.start).coerceAtLeast(0L) } / 60_000,
                mark = when {
                    logs.isEmpty() -> DayMark.NONE
                    logs.any { it.escapes > 0 } -> DayMark.ESCAPED
                    else -> DayMark.KEPT
                },
            )
        }
    }

    /** Finished sessions of each schedule that started in the last [days] days, most used first. */
    fun bySchedule(history: List<SessionLog>, now: Long, days: Int = 28): List<ScheduleStats> =
        history.filter { it.start in (now - days * DAY_MS)..now && it.isFinished(now) }
            .groupBy { it.scheduleId }
            .map { (id, logs) -> ScheduleStats(id, logs.maxBy { it.start }.name, logs.size, logs.count { it.escapes == 0 }) }
            .sortedByDescending { it.sessions }

    private fun SessionLog.isFinished(now: Long) = (endedEarlyAt ?: end) <= now
}
