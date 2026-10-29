package app.bedtime.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

/** The statistics' stretch of time: a day, a week (Monday to Sunday), or a month. */
enum class UsePeriod { DAY, WEEK, MONTH }

/** What a period is compared with: the one before, the usual (the four before), or a year back. */
enum class Versus { LAST, USUAL, YEAR }

/**
 * A stretch of phone use added up: time on the phone and in distracting apps, unlocks if every day knew
 * them, and the same by slot (an hour for a day, a day for a week or month), with each app's time and opens.
 */
data class UseTotals(
    val total: Long = 0,
    val distracting: Long = 0,
    val unlocks: Int? = null,
    /** By slot: all, distracting, unlocks. */
    val slots: List<Triple<Long, Long, Int>> = emptyList(),
    val apps: Map<String, Long> = emptyMap(),
    val opens: Map<String, Int> = emptyMap(),
    /** Days with any use at all. */
    val usedDays: Int = 0,
)

/** The period on show against its comparison: each figure then, the slots then for the faint outlines, and each app then. */
data class UseComparison(val total: Long, val distracting: Long, val unlocks: Int?, val slots: List<Long>, val apps: Map<String, Long>)

object UsePeriods {
    /** The first day of the [period] that holds [date]. */
    fun start(period: UsePeriod, date: LocalDate): LocalDate = when (period) {
        UsePeriod.DAY -> date
        UsePeriod.WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        UsePeriod.MONTH -> date.withDayOfMonth(1)
    }

    /** All the days of the [period] beginning [start]. */
    fun dates(period: UsePeriod, start: LocalDate): List<LocalDate> = when (period) {
        UsePeriod.DAY -> listOf(start)
        UsePeriod.WEEK -> (0L until 7L).map { start.plusDays(it) }
        UsePeriod.MONTH -> (0L until start.lengthOfMonth().toLong()).map { start.plusDays(it) }
    }

    /** The period [by] steps from the one beginning [start]. */
    fun shift(period: UsePeriod, start: LocalDate, by: Long): LocalDate = when (period) {
        UsePeriod.DAY -> start.plusDays(by)
        UsePeriod.WEEK -> start.plusWeeks(by)
        UsePeriod.MONTH -> start.plusMonths(by)
    }

    /**
     * The periods to compare the one beginning [start] with, as their first days. A day's usual is the
     * same weekday in the four weeks before; a year back keeps the weekday for days and weeks.
     */
    fun against(period: UsePeriod, start: LocalDate, versus: Versus): List<LocalDate> = when (versus) {
        Versus.LAST -> listOf(shift(period, start, -1))
        Versus.USUAL -> (1L..4L).map { if (period == UsePeriod.DAY) start.minusWeeks(it) else shift(period, start, -it) }
        Versus.YEAR -> listOf(if (period == UsePeriod.MONTH) start.minusYears(1) else start.minusWeeks(52))
    }

    /**
     * [dates] of [days] added up. [untilHour] keeps only the hours up to and including it (today, so far);
     * [slotsByHour] makes the slots the 24 hours rather than the days.
     */
    fun totals(days: Map<Long, UseDay>, dates: List<LocalDate>, slotsByHour: Boolean, untilHour: Int? = null): UseTotals {
        val list = dates.map { days[it.toEpochDay()] ?: UseDay(it.toEpochDay()) }
        fun UseDay.cut(map: Map<Int, Long>) = if (untilHour == null) map else map.filterKeys { it <= untilHour }
        fun UseDay.allSeconds() = cut(all).values.sum()
        fun UseDay.distractingSeconds() = cut(distracting).values.sum()
        val slots = if (slotsByHour) {
            (0 until 24).map { h -> Triple(list.sumOf { it.all[h] ?: 0L }, list.sumOf { it.distracting[h] ?: 0L }, list.sumOf { it.unlockHours[h] ?: 0 }) }
        } else {
            list.map { Triple(it.allSeconds(), it.distractingSeconds(), it.unlocks ?: 0) }
        }
        val used = list.filter { it.total > 0 }
        val apps = mutableMapOf<String, Long>()
        val opens = mutableMapOf<String, Int>()
        list.forEach { d ->
            d.apps.forEach { (pkg, s) -> apps[pkg] = (apps[pkg] ?: 0L) + s }
            d.opens.forEach { (pkg, n) -> opens[pkg] = (opens[pkg] ?: 0) + n }
        }
        // Unlocks only when every day with use knew them; a day before they were counted would read as none.
        val unlocks = if (used.isNotEmpty() && used.all { it.unlocks != null }) {
            if (untilHour == null) used.sumOf { it.unlocks ?: 0 } else used.sumOf { d -> d.unlockHours.filterKeys { it <= untilHour }.values.sum() }
        } else {
            null
        }
        return UseTotals(list.sumOf { it.allSeconds() }, list.sumOf { it.distractingSeconds() }, unlocks, slots, apps, opens, used.size)
    }

    /**
     * The period beginning [start] against [versus]: each compared period taken as far as this one has
     * gone by [today] (and by [nowHour], for today), so a week in progress meets the same days of the
     * others; then averaged over those with any use. The outlines are the compared periods whole. Null
     * with nothing to compare against.
     */
    fun compare(days: Map<Long, UseDay>, period: UsePeriod, start: LocalDate, versus: Versus, today: LocalDate, nowHour: Int): UseComparison? {
        val current = dates(period, start)
        val inProgress = today in current
        val elapsed = if (inProgress) current.indexOf(today) + 1 else current.size
        val untilHour = if (period == UsePeriod.DAY && inProgress) nowHour else null
        val byHour = period == UsePeriod.DAY
        val compared = against(period, start, versus).map { other ->
            val all = dates(period, other)
            totals(days, all.take(elapsed), byHour, untilHour) to totals(days, all, byHour)
        }.filter { (_, whole) -> whole.usedDays > 0 }
        if (compared.isEmpty()) return null
        val n = compared.size
        val parts = compared.map { it.first }
        val unlocks = parts.mapNotNull { it.unlocks }.takeIf { it.size == n }?.let { it.sum() / n }
        val slotCount = compared.maxOf { it.second.slots.size }
        val slots = (0 until slotCount).map { i -> compared.sumOf { it.second.slots.getOrNull(i)?.first ?: 0L } / n }
        val apps = parts.flatMap { it.apps.keys }.toSet().associateWith { pkg -> parts.sumOf { it.apps[pkg] ?: 0L } / n }
        return UseComparison(parts.sumOf { it.total } / n, parts.sumOf { it.distracting } / n, unlocks, slots, apps)
    }

    /**
     * Months against [versus] from the month totals alone, for when the days themselves are no longer
     * kept: each compared month's daily average over [elapsed] days. No unlocks, outlines or apps.
     */
    fun compareMonths(months: Map<String, UsageMonth>, start: LocalDate, versus: Versus, elapsed: Int): UseComparison? {
        val found = against(UsePeriod.MONTH, start, versus).mapNotNull { months["%04d-%02d".format(it.year, it.monthValue)] }.filter { it.days > 0 }
        if (found.isEmpty()) return null
        val n = found.size
        return UseComparison(found.sumOf { it.total / it.days * elapsed } / n, found.sumOf { it.distracting / it.days * elapsed } / n, null, emptyList(), emptyMap())
    }

    /** The change from [then] to [now] in percent, or null with nothing then. */
    fun change(now: Long, then: Long): Int? = if (then > 0) ((now - then) * 100 / then).toInt() else null
}
