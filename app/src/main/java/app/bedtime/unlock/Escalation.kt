package app.bedtime.unlock

import app.bedtime.data.SessionLog
import kotlin.math.pow
import kotlin.math.roundToInt

/** Escalating friction: every early unlock in a day makes the next unlock's wait and text harder. */
object Escalation {
    private const val DAY_MS = 24 * 60 * 60 * 1000L

    fun recentUnlocks(history: List<SessionLog>, scheduleId: String, now: Long): Int =
        history.filter { it.scheduleId == scheduleId }
            .sumOf { log -> log.unlockTimes.count { it > now - DAY_MS && it <= now } }

    /** `base × factorⁿ`, capped at [maxTimes] × the base. */
    fun scale(base: Int, recentUnlocks: Int, factor: Float, maxTimes: Float): Int =
        (base * factor.toDouble().pow(recentUnlocks)).coerceAtMost(base * maxTimes.toDouble()).roundToInt()

    /** Text length grows by [factor] per unlock, capped at three times the base. */
    fun textLength(base: Int, recentUnlocks: Int, factor: Float = 1.5f): Int =
        scale(base, recentUnlocks, factor, 3f)

    /** Wait length grows by [factor] per unlock, capped at eight times the base. */
    fun waitSeconds(base: Int, recentUnlocks: Int, factor: Float): Int =
        scale(base, recentUnlocks, factor, 8f)
}
