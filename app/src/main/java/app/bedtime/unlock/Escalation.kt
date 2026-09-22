package app.bedtime.unlock

import app.bedtime.data.SessionLog
import java.time.Instant
import java.time.ZoneId
import kotlin.math.pow
import kotlin.math.roundToInt

/** Escalating friction: every early unlock today makes the next unlock's wait and text harder. */
object Escalation {
    /**
     * Early unlocks of this schedule **since local midnight**. Counting a rolling 24 hours instead
     * would let last night's unlocks keep tonight's challenge long, and the day would never start clean.
     */
    fun recentUnlocks(
        history: List<SessionLog>,
        scheduleId: String,
        now: Long,
        zone: ZoneId = ZoneId.systemDefault(),
    ): Int {
        val midnight = Instant.ofEpochMilli(now).atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli()
        return history.filter { it.scheduleId == scheduleId }
            .sumOf { log -> log.unlockTimes.count { it in midnight..now } }
    }

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
