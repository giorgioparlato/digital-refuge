package app.bedtime.data

import kotlinx.serialization.Serializable
import java.util.UUID
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

/** Done or not each day, or a count with an aim for the day (reps, pages, glasses). */
@Serializable
enum class HabitKind { YES_NO, COUNT }

/**
 * A habit: how often it's meant to happen is [times] in every [days] days, so
 * every day is 1/1, every three days 1/3, three times a week 3/7, ten times a month 10/30.
 */
@Serializable
data class Habit(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val kind: HabitKind = HabitKind.YES_NO,
    val times: Int = 1,
    val days: Int = 1,
    /** A count's aim for the day, and what it counts. */
    val target: Int = 1,
    val unit: String = "",
    /** Its colour, ARGB, in the widgets and here; null takes the accent. */
    val color: Long? = null,
    /** Whether the habits widget shows it on the home screen, and on the lock screen (null: as on home). */
    val inWidget: Boolean = true,
    val inLockWidget: Boolean? = null,
    /** A set: the steps it's made of (a workout's exercises, say), each ticked apart; done when all are. Yes/no habits only. */
    val steps: List<HabitStep> = emptyList(),
) {
    /** A yes/no habit made of steps, whose day's mark holds which of them are ticked, a bit each. */
    val isSet: Boolean get() = kind == HabitKind.YES_NO && steps.isNotEmpty()

    /** Every step's bit, together: the mark of a day with all of them ticked. */
    val allSteps: Int get() = steps.fold(0) { all, step -> all or (1 shl step.bit) }

    fun shownOn(screen: Screen): Boolean = if (screen == Screen.HOME) inWidget else inLockWidget ?: inWidget

    fun withShownOn(screen: Screen, shown: Boolean): Habit = if (screen == Screen.HOME) copy(inWidget = shown) else copy(inLockWidget = shown)
}

/** One step of a set, and its own bit in the day's mark; a bit stays with its step when others go. */
@Serializable
data class HabitStep(val bit: Int, val name: String)

/** What was marked for a habit on a day (epoch day): 1 for done, or the count. */
@Serializable
data class HabitMark(val habit: String, val day: Long, val value: Int)

/**
 * How a day reads: done; part of a count; not done but already covered by how often it's meant to
 * happen (three times this week, say); or open.
 */
enum class HabitDay { DONE, PARTIAL, MET, OPEN }

/** The ways of saying how often. */
enum class HabitFrequency(val label: String, val days: Int) {
    DAILY("Every day", 1),
    EVERY("Every few days", 0),
    WEEK("Times a week", 7),
    MONTH("Times a month", 30),
    ;

    companion object {
        fun of(habit: Habit): HabitFrequency = when {
            habit.times == 1 && habit.days == 1 -> DAILY
            habit.times == 1 -> EVERY
            habit.days == 7 -> WEEK
            else -> MONTH
        }
    }
}

object Habits {
    /** Each habit's marks by day. */
    fun byDay(marks: List<HabitMark>): Map<String, Map<Long, Int>> =
        marks.groupBy { it.habit }.mapValues { (_, list) -> list.associate { it.day to it.value } }

    fun done(habit: Habit, value: Int): Boolean = when {
        habit.isSet -> value and habit.allSteps == habit.allSteps
        habit.kind == HabitKind.YES_NO -> value > 0
        else -> value >= habit.target
    }

    /** How a set's day reads in its steps: how many of them are ticked. */
    fun stepsTicked(habit: Habit, value: Int): Int = habit.steps.count { value and (1 shl it.bit) != 0 }

    /** [value] with [step] ticked or unticked. */
    fun stepTapped(value: Int, step: HabitStep): Int = value xor (1 shl step.bit)

    /** [habit] with a step called [name] at the end, on the next bit free (31 at most). */
    fun withStep(habit: Habit, name: String): Habit {
        if (habit.steps.size >= 31) return habit
        val used = habit.steps.map { it.bit }.toSet()
        val bit = (habit.steps.maxOfOrNull { it.bit } ?: -1).plus(1).takeIf { it <= 30 } ?: (0..30).first { it !in used }
        return habit.copy(steps = habit.steps + HabitStep(bit, name))
    }

    /**
     * Whether today asks for [habit]: whatever happens today, unless the days before have already
     * covered how often it's meant to happen (three times this week done, say). What keeps the run going.
     */
    fun due(habit: Habit, marks: Map<Long, Int>, today: Long): Boolean = day(habit, marks - today, today) != HabitDay.MET

    /**
     * The habits the widget shows: those chosen for [screen]; or, asked for what's due, every habit
     * today asks for, those done included only when [keepDone].
     */
    fun forWidget(habits: List<Habit>, marks: Map<String, Map<Long, Int>>, today: Long, screen: Screen, dueOnly: Boolean, keepDone: Boolean): List<Habit> =
        if (!dueOnly) {
            habits.filter { it.shownOn(screen) }
        } else {
            habits.filter { h ->
                val m = marks[h.id].orEmpty()
                due(h, m, today) && (keepDone || day(h, m, today) != HabitDay.DONE)
            }
        }

    fun day(habit: Habit, marks: Map<Long, Int>, day: Long): HabitDay {
        val value = marks[day] ?: 0
        return when {
            done(habit, value) -> HabitDay.DONE
            value > 0 -> HabitDay.PARTIAL
            habit.days > 1 && ((day - habit.days + 1)..day).count { done(habit, marks[it] ?: 0) } >= habit.times -> HabitDay.MET
            else -> HabitDay.OPEN
        }
    }

    private fun kept(state: HabitDay) = state == HabitDay.DONE || state == HabitDay.MET

    /** Days in a row kept, up to today; today still open doesn't break it yet. */
    fun streak(habit: Habit, marks: Map<Long, Int>, today: Long): Int {
        var day = if (kept(day(habit, marks, today))) today else today - 1
        var n = 0
        while (kept(day(habit, marks, day))) {
            n++
            day--
        }
        return n
    }

    /** The longest run of kept days since the first mark. */
    fun bestStreak(habit: Habit, marks: Map<Long, Int>, today: Long): Int {
        val first = marks.keys.minOrNull() ?: return 0
        var best = 0
        var run = 0
        for (d in first..today) {
            run = if (kept(day(habit, marks, d))) run + 1 else 0
            best = maxOf(best, run)
        }
        return best
    }

    /**
     * The habit's strength, 0 to 100: each day nudges it toward that day's share of what was meant
     * (a done day, a count's part of its aim, or the part of the times-per-period so far), slower for
     * habits meant less often. Missing a day only dents a strong habit.
     */
    fun strength(habit: Habit, marks: Map<Long, Int>, today: Long): Int {
        val first = marks.keys.minOrNull() ?: return 0
        val frequency = habit.times.toDouble() / habit.days
        val keep = 0.5.pow(sqrt(frequency) / 13.0)
        var score = 0.0
        for (d in first..today) {
            val share = when {
                habit.kind == HabitKind.COUNT -> min(1.0, (marks[d] ?: 0).toDouble() / habit.target.coerceAtLeast(1))
                habit.days > 1 -> min(1.0, ((d - habit.days + 1)..d).count { done(habit, marks[it] ?: 0) }.toDouble() / habit.times)
                else -> if (done(habit, marks[d] ?: 0)) 1.0 else 0.0
            }
            score = score * keep + share * (1 - keep)
        }
        return (score * 100).toInt()
    }

    /** What a tap on today does: a yes/no is ticked or unticked; a count goes up by one. */
    fun tapped(habit: Habit, value: Int): Int = when {
        // A set at once: all its steps ticked, or none once they all are.
        habit.isSet -> if (done(habit, value)) 0 else habit.allSteps
        habit.kind == HabitKind.YES_NO -> if (value > 0) 0 else 1
        else -> value + 1
    }

    /** "Every day", "Every 3 days", "3 times a week", "10 times a month", or "10 reps a day". */
    fun describe(habit: Habit): String {
        if (habit.kind == HabitKind.COUNT) return "${habit.target}${if (habit.unit.isBlank()) "" else " ${habit.unit}"} a day"
        val how = when (HabitFrequency.of(habit)) {
            HabitFrequency.DAILY -> "Every day"
            HabitFrequency.EVERY -> "Every ${habit.days} days"
            HabitFrequency.WEEK -> if (habit.times == 1) "Once a week" else "${habit.times} times a week"
            HabitFrequency.MONTH -> if (habit.times == 1) "Once a month" else "${habit.times} times a month"
        }
        return if (habit.isSet) "$how · ${habit.steps.size} ${if (habit.steps.size == 1) "step" else "steps"}" else how
    }

    /** [habit] said to happen [n] at a time in [frequency]: n days apart for "every few days", n times otherwise. */
    fun withFrequency(habit: Habit, frequency: HabitFrequency, n: Int): Habit = when (frequency) {
        HabitFrequency.DAILY -> habit.copy(times = 1, days = 1)
        HabitFrequency.EVERY -> habit.copy(times = 1, days = n.coerceIn(2, 30))
        HabitFrequency.WEEK -> habit.copy(times = n.coerceIn(1, 6), days = 7)
        HabitFrequency.MONTH -> habit.copy(times = n.coerceIn(1, 29), days = 30)
    }
}
