package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class UseHeatTest {
    private val today = LocalDate.of(2026, 10, 31)

    private fun day(date: LocalDate, all: Long, distracting: Long) =
        UseDay(date.toEpochDay(), all = mapOf(9 to all), distracting = mapOf(9 to distracting))

    @Test
    fun aMonthIsReadFromItsDaysThenFromWhatItKeptOfThem() {
        val october = YearMonth.of(2026, 10)
        val days = mapOf(LocalDate.of(2026, 10, 30).let { it.toEpochDay() to day(it, 7_200, 1_800) })
        val kept = UsageMonth("2026-10", daily = mapOf(2 to 3_600L), dailyDistracting = mapOf(2 to 600L))
        val heat = UseHeat.month(october, days, kept, wholePhone = true, today)
        assertEquals(31, heat.size)
        assertEquals(7_200L, heat[29].seconds)
        assertEquals(3_600L, heat[1].seconds)
        assertNull(heat[0].seconds)
        assertEquals(600L, UseHeat.month(october, days, kept, wholePhone = false, today)[1].seconds)
        // Days still to come are nothing yet.
        assertNull(UseHeat.month(YearMonth.of(2026, 11), emptyMap(), UsageMonth("2026-11", daily = mapOf(5 to 60L)), true, today)[4].seconds)
    }

    @Test
    fun theScaleIsTheBudgetsNotTheMonths() {
        assertEquals(0.5f, UseHeat.level(1_800, 3_600))
        assertEquals(1.5f, UseHeat.level(5_400, 3_600))
        // Twice the budget is as red as it gets, however much more.
        assertEquals(UseHeat.REDDEST, UseHeat.level(36_000, 3_600))
    }

    @Test
    fun theMonthsMost() {
        val heat = listOf(HeatDay(today.minusDays(2), 100), HeatDay(today.minusDays(1), 900), HeatDay(today, null))
        assertEquals(today.minusDays(1), UseHeat.most(heat)!!.date)
        assertNull(UseHeat.most(listOf(HeatDay(today, null))))
    }

    @Test
    fun monthsKeepEachDaysFiguresAndAndroidsMonthsKeepThemToo() {
        val months = UseMonths.add(emptyList(), day(LocalDate.of(2026, 9, 14), 5_000, 1_000))
        assertEquals(mapOf(14 to 5_000L), months.single().daily)
        assertEquals(mapOf(14 to 1_000L), months.single().dailyDistracting)
        val merged = UseMonths.merge(mapOf("2026-09" to UsageMonth("2026-09", total = 99_000)), months.associateBy { it.month })
        assertEquals(99_000L, merged.getValue("2026-09").total)
        assertEquals(mapOf(14 to 5_000L), merged.getValue("2026-09").daily)
    }
}
