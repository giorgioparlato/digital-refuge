package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class UsePeriodsTest {
    private val wed = LocalDate.of(2026, 10, 28)

    private fun day(date: LocalDate, all: Map<Int, Long>, distracting: Map<Int, Long> = emptyMap(), unlocks: Int? = null, apps: Map<String, Long> = emptyMap()) =
        date.toEpochDay() to UseDay(date.toEpochDay(), all, distracting, apps, unlocks = unlocks, unlockHours = if (unlocks != null) mapOf(9 to unlocks) else emptyMap())

    @Test
    fun periodsStartOnMondayAndTheFirst() {
        assertEquals(LocalDate.of(2026, 10, 26), UsePeriods.start(UsePeriod.WEEK, wed))
        assertEquals(LocalDate.of(2026, 10, 1), UsePeriods.start(UsePeriod.MONTH, wed))
        assertEquals(31, UsePeriods.dates(UsePeriod.MONTH, LocalDate.of(2026, 10, 1)).size)
    }

    @Test
    fun aDaysUsualIsTheSameWeekday() {
        assertEquals(listOf(21, 14, 7, 30).map { if (it == 30) LocalDate.of(2026, 9, 30) else LocalDate.of(2026, 10, it) }, UsePeriods.against(UsePeriod.DAY, wed, Versus.USUAL))
        assertEquals(listOf(LocalDate.of(2026, 10, 27)), UsePeriods.against(UsePeriod.DAY, wed, Versus.LAST))
        assertEquals(LocalDate.of(2025, 10, 1), UsePeriods.against(UsePeriod.MONTH, LocalDate.of(2026, 10, 1), Versus.YEAR).single())
    }

    @Test
    fun todayMeetsYesterdayUpToTheSameHour() {
        val days = mapOf(
            day(wed, mapOf(9 to 600L)),
            day(wed.minusDays(1), mapOf(9 to 300L, 20 to 3_000L)),
        )
        val versus = UsePeriods.compare(days, UsePeriod.DAY, wed, Versus.LAST, today = wed, nowHour = 10)!!
        // So far: only yesterday's hours up to ten count; the evening is in the outline, not the figure.
        assertEquals(300L, versus.total)
        assertEquals(3_000L, versus.slots[20])
    }

    @Test
    fun aWeekInProgressMeetsTheSameDaysOfTheOthers() {
        val monday = LocalDate.of(2026, 10, 26)
        val days = mapOf(
            day(monday, mapOf(9 to 100L)),
            day(monday.minusWeeks(1), mapOf(9 to 200L)),
            day(monday.minusWeeks(1).plusDays(5), mapOf(9 to 9_000L)),
        )
        val versus = UsePeriods.compare(days, UsePeriod.WEEK, monday, Versus.LAST, today = wed, nowHour = 12)!!
        assertEquals(200L, versus.total)
    }

    @Test
    fun theUsualAveragesOnlyPeriodsWithUse() {
        val monday = LocalDate.of(2026, 10, 26)
        val days = mapOf(day(monday.minusWeeks(1), mapOf(9 to 400L)), day(monday.minusWeeks(3), mapOf(9 to 200L)))
        assertEquals(300L, UsePeriods.compare(days, UsePeriod.WEEK, monday.minusWeeks(0), Versus.USUAL, today = monday.plusDays(6), nowHour = 23)!!.total)
        assertNull(UsePeriods.compare(emptyMap(), UsePeriod.WEEK, monday, Versus.LAST, today = wed, nowHour = 12))
    }

    @Test
    fun unlocksOnlyWhenEveryDayKnewThem() {
        val monday = LocalDate.of(2026, 10, 26)
        val known = mapOf(day(monday, mapOf(9 to 60L), unlocks = 5), day(monday.plusDays(1), mapOf(9 to 60L), unlocks = 7))
        assertEquals(12, UsePeriods.totals(known, UsePeriods.dates(UsePeriod.WEEK, monday), slotsByHour = false).unlocks)
        val partly = known + day(monday.plusDays(2), mapOf(9 to 60L))
        assertNull(UsePeriods.totals(partly, UsePeriods.dates(UsePeriod.WEEK, monday), slotsByHour = false).unlocks)
    }

    @Test
    fun monthsBeyondTheDaysKeptComeFromTheirTotals() {
        val months = mapOf("2026-09" to UsageMonth("2026-09", total = 30 * 3_600L, distracting = 30 * 600L, days = 30))
        val versus = UsePeriods.compareMonths(months, LocalDate.of(2026, 10, 1), Versus.LAST, elapsed = 10)!!
        assertEquals(10 * 3_600L, versus.total)
        assertEquals(10 * 600L, versus.distracting)
    }

    @Test
    fun changeInPercent() {
        assertEquals(-25, UsePeriods.change(75, 100))
        assertNull(UsePeriods.change(75, 0))
    }
}
