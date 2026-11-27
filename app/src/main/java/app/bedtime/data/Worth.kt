package app.bedtime.data

import kotlinx.serialization.Serializable

/**
 * One answer to "was it worth it?": the [app] you'd just left (or null for the phone as a whole, from
 * the nudge), how long the stretch was, and whether you thought it was worth it.
 */
@Serializable
data class WorthAnswer(val day: Long, val app: String? = null, val seconds: Long = 0L, val worth: Boolean, val at: Long = 0L)

/** "Was it worth it?" after a stretch in a distracting app: when it's asked, and what the answers add up to. */
object Worth {
    /** Never more than this many times a day, whichever apps. */
    const val MAX_A_DAY = 3

    /** Not within this long of another check-in ("how do you feel now?" or this). */
    const val AGAIN_MS = 15 * 60_000L

    /** Answers kept, about a year of a few a day. */
    const val KEPT = 1500

    /** Fewer answers than this about an app say nothing about it yet. */
    const val ENOUGH = 3

    /**
     * Whether to ask after [seconds] in [app]: asked for at all ([minutes]), long enough, not yet today
     * for this app ([askedToday]), not more than [MAX_A_DAY] today ([askedCount]), and not within
     * [AGAIN_MS] of the last check-in ([lastCheckIn]).
     */
    fun due(minutes: Int?, seconds: Long, app: String, askedToday: Set<String>, askedCount: Int, lastCheckIn: Long, now: Long): Boolean =
        minutes != null && minutes > 0 &&
            seconds >= minutes * 60L &&
            app !in askedToday &&
            askedCount < MAX_A_DAY &&
            now - lastCheckIn >= AGAIN_MS

    /** [answers] with [answer] added, the oldest going once there are more than [KEPT]. */
    fun added(answers: List<WorthAnswer>, answer: WorthAnswer): List<WorthAnswer> = (answers + answer).takeLast(KEPT)

    /**
     * What [answers] about one app say, once there are [ENOUGH]: "mostly worth it", "mostly not worth it"
     * or "half worth it", and how many. Null before.
     */
    fun words(answers: List<WorthAnswer>): String? {
        if (answers.size < ENOUGH) return null
        val yes = answers.count { it.worth }
        val verdict = when {
            yes * 3 >= answers.size * 2 -> "mostly worth it"
            yes * 3 <= answers.size -> "mostly not worth it"
            else -> "half worth it"
        }
        return "$verdict · ${answers.size} answers"
    }
}
