package app.bedtime.unlock

import app.bedtime.data.SessionLog

/** Emergency breaks: a short, challenge-free pause, limited per week so it stays an emergency tool. */
object EmergencyBreaks {
    const val PER_WEEK = 3
    const val MINUTES = 15
    private const val WEEK_MS = 7 * 24 * 60 * 60 * 1000L

    fun usedThisWeek(history: List<SessionLog>, now: Long): Int =
        history.sumOf { log -> log.emergencies.count { it.at in (now - WEEK_MS)..now } }

    fun remaining(history: List<SessionLog>, now: Long): Int = (PER_WEEK - usedThisWeek(history, now)).coerceAtLeast(0)
}
