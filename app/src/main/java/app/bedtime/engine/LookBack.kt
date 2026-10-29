package app.bedtime.engine

import app.bedtime.data.Habit
import app.bedtime.data.HabitDay
import app.bedtime.data.Habits
import app.bedtime.data.SessionLog
import app.bedtime.data.UseDay
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * A week looked back on, Monday to Sunday, against the one before: time in refuge and how sessions
 * held, time on the phone and in distracting apps, the app that took most, and habits kept.
 */
data class LookBack(
    val weekStart: LocalDate,
    val refugeMinutes: Long,
    val refugeMinutesBefore: Long,
    val finished: Int,
    val kept: Int,
    val escapes: Int,
    /** Seconds; null when nothing was counted that week. */
    val phone: Long?,
    val phoneBefore: Long?,
    val distracting: Long?,
    val distractingBefore: Long?,
    /** The distracting app with the most time, and its seconds. */
    val topApp: Pair<String, Long>?,
    /** Days kept across your habits, of the days there were to keep. */
    val habitDaysKept: Int,
    val habitDays: Int,
) {
    val weekEnd: LocalDate get() = weekStart.plusDays(6)
}

object LookBacks {
    /** From Sunday at this hour, the week ending today is the one looked back on. */
    const val SUNDAY_HOUR = 18

    /** The Monday of the week to look back on at [now]: this week from Sunday evening, otherwise last week. */
    fun weekStart(now: LocalDateTime): LocalDate {
        val monday = now.toLocalDate().with(DayOfWeek.MONDAY)
        val weekDone = now.dayOfWeek == DayOfWeek.SUNDAY && now.hour >= SUNDAY_HOUR
        return if (weekDone) monday else monday.minusWeeks(1)
    }

    fun of(
        start: LocalDate,
        history: List<SessionLog>,
        days: Map<Long, UseDay>,
        distractingApps: Set<String>,
        habits: List<Habit>,
        marks: Map<String, Map<Long, Int>>,
        zone: ZoneId,
    ): LookBack {
        fun millis(d: LocalDate) = d.atStartOfDay(zone).toInstant().toEpochMilli()
        fun sessions(from: LocalDate) = history.filter { it.start >= millis(from) && it.start < millis(from.plusDays(7)) }
        fun minutes(logs: List<SessionLog>) = logs.sumOf { ((it.endedEarlyAt ?: it.end) - it.start).coerceAtLeast(0L) } / 60_000
        fun week(from: LocalDate) = (0L..6L).mapNotNull { days[from.toEpochDay() + it] }
        fun sum(list: List<UseDay>, of: (UseDay) -> Long) = list.takeIf { w -> w.any { it.total > 0 } }?.sumOf(of)

        val now = sessions(start)
        val before = start.minusWeeks(1)
        val use = week(start)
        val useBefore = week(before)
        val endMs = millis(start.plusDays(7))
        val finished = now.filter { (it.endedEarlyAt ?: it.end) <= endMs }
        val top = use.flatMap { it.apps.entries }.filter { it.key in distractingApps }
            .groupBy({ it.key }, { it.value }).mapValues { it.value.sum() }.maxByOrNull { it.value }?.toPair()
        val habitDays = (0L..6L).map { start.toEpochDay() + it }
        val keptDays = habits.sumOf { habit ->
            habitDays.count { Habits.day(habit, marks[habit.id].orEmpty(), it).let { s -> s == HabitDay.DONE || s == HabitDay.MET } }
        }
        return LookBack(
            weekStart = start,
            refugeMinutes = minutes(now),
            refugeMinutesBefore = minutes(sessions(before)),
            finished = finished.size,
            kept = finished.count { it.escapes == 0 },
            escapes = now.sumOf { it.escapes },
            phone = sum(use) { it.total },
            phoneBefore = sum(useBefore) { it.total },
            distracting = sum(use) { it.distractingTotal },
            distractingBefore = sum(useBefore) { it.distractingTotal },
            topApp = top,
            habitDaysKept = keptDays,
            habitDays = habits.size * 7,
        )
    }
}
