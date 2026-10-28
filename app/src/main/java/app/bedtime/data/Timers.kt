package app.bedtime.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

/**
 * Timers counting down, for the lock screen: those set from refuge's search, and the clock app's own,
 * read from its notification (a chronometer counting down to the end). A swipe hides one until it ends.
 */
object Timers {
    /** Two ends this close are the same timer, seen from refuge and from the clock app. */
    const val SAME_MS = 2_000L

    private val _clock = MutableStateFlow<List<Long>>(emptyList())

    /** The ends the clock app's notifications count down to, while notification access reads them. */
    val clock: StateFlow<List<Long>> = _clock

    internal fun publishClock(ends: List<Long>) {
        _clock.value = ends
    }

    private val _hidden = MutableStateFlow<Set<Long>>(emptySet())

    /** Ends swiped away on the lock screen. */
    val hidden: StateFlow<Set<Long>> = _hidden

    fun hide(end: Long) {
        _hidden.value = _hidden.value + end
    }

    /** A notification's countdown end: its time, when it shows a chronometer counting down; null otherwise, as for a paused timer. */
    fun countdownEnd(showsChronometer: Boolean, countsDown: Boolean, at: Long): Long? = at.takeIf { showsChronometer && countsDown && it > 0 }

    /** The timers still running at [now], soonest first, one for each, leaving out those [hidden]. */
    fun running(fromRefuge: List<Long>, fromClock: List<Long>, now: Long, hidden: Set<Long> = emptySet()): List<Long> =
        (fromClock + fromRefuge).filter { it > now }.sorted()
            .fold(emptyList<Long>()) { kept, end -> if (kept.isNotEmpty() && end - kept.last() <= SAME_MS) kept else kept + end }
            .filterNot { end -> hidden.any { abs(it - end) <= SAME_MS } }

    /** What's left, rounded up to the second: "0:45", "4:12", "1:02:05". */
    fun left(end: Long, now: Long): String {
        val seconds = ((end - now).coerceAtLeast(0) + 999) / 1000
        val h = seconds / 3600
        val m = seconds % 3600 / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }
}
