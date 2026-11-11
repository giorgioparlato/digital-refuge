package app.bedtime.data

import java.time.LocalDate
import java.time.YearMonth

/** One day of a month's heatmap: the seconds counted, or the unlocks; null where nothing's known (or it hasn't come yet). */
data class HeatDay(val date: LocalDate, val amount: Long?)

/**
 * A month of phone use, a day at a time, set against the daily budget. The scale is the budget's,
 * never the month's own, so months can be set beside each other: within it is calm, just over it is
 * the distracted colour, and twice it or more is as red as it gets.
 */
object UseHeat {
    /** How far over the budget is as red as it gets: twice it. */
    const val REDDEST = 2f

    /**
     * Each day of [month]: from the days kept in full ([days]) where there are any, otherwise from the
     * month's own record of its days ([kept]). [wholePhone] counts every app, else only distracting ones.
     */
    fun month(month: YearMonth, days: Map<Long, UseDay>, kept: UsageMonth?, wholePhone: Boolean, today: LocalDate): List<HeatDay> =
        (1..month.lengthOfMonth()).map { d ->
            val date = month.atDay(d)
            val seconds = when {
                date.isAfter(today) -> null
                else -> days[date.toEpochDay()]?.let { if (wholePhone) it.total else it.distractingTotal }
                    ?: (if (wholePhone) kept?.daily else kept?.dailyDistracting)?.get(d)
            }
            HeatDay(date, seconds)
        }

    /** Each day of [month]'s unlocks, from the days kept; months kept only as totals don't know them. */
    fun unlocks(month: YearMonth, days: Map<Long, UseDay>, today: LocalDate): List<HeatDay> =
        (1..month.lengthOfMonth()).map { d ->
            val date = month.atDay(d)
            HeatDay(date, if (date.isAfter(today)) null else days[date.toEpochDay()]?.unlocks?.toLong())
        }

    /** Where [seconds] sits against [budget] seconds: up to 1 within it, then on to [REDDEST]. */
    fun level(seconds: Long, budget: Long): Float = (seconds.toFloat() / budget.coerceAtLeast(1)).coerceIn(0f, REDDEST)

    /** The day with the most, if any day is known. */
    fun most(heat: List<HeatDay>): HeatDay? = heat.filter { (it.amount ?: 0) > 0 }.maxByOrNull { it.amount ?: 0 }
}
