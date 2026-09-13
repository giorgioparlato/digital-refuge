package app.bedtime.unlock

import app.bedtime.data.SessionLog
import kotlin.math.pow
import kotlin.math.roundToInt

/** Escalating friction: every early unlock in a day makes the next text challenge longer. */
object Escalation {
    private const val DAY_MS = 24 * 60 * 60 * 1000L

    fun recentUnlocks(history: List<SessionLog>, scheduleId: String, now: Long): Int =
        history.filter { it.scheduleId == scheduleId }
            .sumOf { log -> log.unlockTimes.count { it > now - DAY_MS && it <= now } }

    /** `base × 1.5ⁿ`, capped at three times the base length. */
    fun textLength(base: Int, recentUnlocks: Int): Int =
        (base * 1.5.pow(recentUnlocks)).coerceAtMost(base * 3.0).roundToInt()
}
