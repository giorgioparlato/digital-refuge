package app.bedtime.engine

import app.bedtime.data.EditWindow
import app.bedtime.data.Schedule

/**
 * When a schedule is open to being changed.
 *
 * Two things can close it: the schedule's own hours, because a session can't be edited while it
 * runs, and an optional [EditWindow] naming the only hours in which it may be touched at all. Put
 * together they could seal a schedule off entirely — a bedtime locked from eight that runs till
 * seven, editable only in the evening, can never be reached — so every day has to keep at least
 * [MIN_DAILY_EDITABLE_MINUTES] unbroken minutes in which you can get back in.
 */
object EditWindows {
    /** The shortest stretch that counts as a way back in, on every day of the week. */
    const val MIN_DAILY_EDITABLE_MINUTES = 10

    private const val DAY = 24 * 60
    private const val WEEK = 7 * DAY

    /** Whether [window] is open at [minuteOfDay]. Equal ends mean the whole day, never none of it. */
    fun isOpen(window: EditWindow, minuteOfDay: Int): Boolean {
        val start = window.startMinute
        val end = window.endMinute
        return when {
            start == end -> true
            start < end -> minuteOfDay in start until end
            else -> minuteOfDay >= start || minuteOfDay < end
        }
    }

    /** Whether [schedule]'s window — if it has one — is open at [minuteOfDay]. */
    fun isOpen(schedule: Schedule, minuteOfDay: Int): Boolean =
        schedule.editWindow?.let { isOpen(it, minuteOfDay) } ?: true

    /** The next minute of the day at which [window] opens again, for telling someone when to come back. */
    fun opensAt(window: EditWindow): Int = window.startMinute

    /** One session's length in minutes; equal start and end mean a full day. */
    private fun sessionLength(schedule: Schedule): Int {
        val diff = schedule.endMinute - schedule.startMinute
        return if (diff > 0) diff else diff + DAY
    }

    /** Every minute of the week, true where [schedule] is closed to changes. */
    private fun closedWeek(schedule: Schedule): BooleanArray {
        val closed = BooleanArray(WEEK)
        schedule.editWindow?.let { window ->
            for (minute in 0 until WEEK) {
                if (!isOpen(window, minute % DAY)) closed[minute] = true
            }
        }
        // A session in progress closes it too, and a session can run past midnight into the next day.
        val length = sessionLength(schedule)
        for (day in schedule.days) {
            val from = (day - 1) * DAY + schedule.startMinute
            for (offset in 0 until length) {
                closed[Math.floorMod(from + offset, WEEK)] = true
            }
        }
        return closed
    }

    /**
     * The shortest day's longest unbroken editable stretch, in minutes. Runs are counted within a
     * calendar day, so a window spanning midnight gives each of the two days only its own share —
     * five minutes either side of midnight is not ten minutes you can use.
     */
    fun shortestDailyOpening(schedule: Schedule): Int {
        val closed = closedWeek(schedule)
        var shortest = Int.MAX_VALUE
        for (day in 0 until 7) {
            var longest = 0
            var run = 0
            for (minute in day * DAY until (day + 1) * DAY) {
                if (closed[minute]) {
                    run = 0
                } else {
                    run++
                    if (run > longest) longest = run
                }
            }
            if (longest < shortest) shortest = longest
        }
        return shortest
    }

    /** Whether [schedule] leaves a way back in on every day of the week. */
    fun leavesAWayIn(schedule: Schedule): Boolean =
        shortestDailyOpening(schedule) >= MIN_DAILY_EDITABLE_MINUTES
}
