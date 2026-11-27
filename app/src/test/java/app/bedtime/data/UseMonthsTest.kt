package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

class UseMonthsTest {
    private fun day(date: String, all: Long, distracting: Long) =
        UseDay(LocalDate.parse(date).toEpochDay(), all = mapOf(10 to all), distracting = mapOf(10 to distracting))

    @Test
    fun daysAddUpIntoTheirMonths() {
        val kept = listOf(UsageMonth("2026-09", total = 1_000, distracting = 400, days = 10))
        val own = UseMonths.own(kept, listOf(day("2026-09-30", 100, 50), day("2026-10-01", 200, 20), day("2026-10-02", 0, 0)))
        assertEquals(UsageMonth("2026-09", 1_100, 450, 11, daily = mapOf(30 to 100L), dailyDistracting = mapOf(30 to 50L)), own["2026-09"])
        // A day without any use doesn't count towards the average.
        assertEquals(UsageMonth("2026-10", 200, 20, 1, daily = mapOf(1 to 200L), dailyDistracting = mapOf(1 to 20L)), own["2026-10"])
    }

    @Test
    fun androidsMonthsWinWhereItHasThem() {
        val own = mapOf("2026-09" to UsageMonth("2026-09", 1), "2026-10" to UsageMonth("2026-10", 2))
        val android = mapOf("2026-10" to UsageMonth("2026-10", 99))
        assertEquals(listOf(1L, 99L), UseMonths.merge(android, own).values.sortedBy { it.month }.map { it.total })
    }

    @Test
    fun theLastSixMonthsFillTheGaps() {
        val months = UseMonths.last(mapOf("2026-10" to UsageMonth("2026-10", 5)), YearMonth.of(2026, 10))
        assertEquals(listOf("2026-05", "2026-06", "2026-07", "2026-08", "2026-09", "2026-10"), months.map { it.month })
        assertEquals(listOf(0L, 0L, 0L, 0L, 0L, 5L), months.map { it.total })
    }

    @Test
    fun androidsBucketsCountInTheMonthOfTheirMiddle() {
        val zone = ZoneOffset.UTC
        fun at(date: String) = LocalDate.parse(date).atStartOfDay(zone).toInstant().toEpochMilli()
        val buckets = listOf(
            UseMonths.Bucket("ig", at("2026-09-01"), at("2026-09-30"), 600),
            UseMonths.Bucket("mail", at("2026-09-01"), at("2026-09-30"), 400),
            UseMonths.Bucket("launcher", at("2026-09-01"), at("2026-09-30"), 9_000),
            UseMonths.Bucket("ig", at("2026-10-01"), at("2026-10-27"), 300),
        )
        val months = UseMonths.fromBuckets(buckets, zone, excluded = setOf("launcher"), distracting = setOf("ig"), today = LocalDate.parse("2026-10-28"))
        assertEquals(UsageMonth("2026-09", 1_000, 600, 30), months["2026-09"])
        assertEquals(UsageMonth("2026-10", 300, 300, 28), months["2026-10"])
    }

    @Test
    fun theChartGrowsFromSixToTwelveMonthsAsTheyAreKept() {
        val now = YearMonth.parse("2026-11")
        fun from(first: String) = mapOf(first to UsageMonth(first, 100, 10, 30), "2026-11" to UsageMonth("2026-11", 100, 10, 27))
        // Nothing yet, or three months: six, as before.
        assertEquals(6, UseMonths.span(emptyMap(), now))
        assertEquals(6, UseMonths.span(from("2026-09"), now))
        // Since february: ten.
        assertEquals(10, UseMonths.span(from("2026-02"), now))
        // Two years back: still twelve.
        assertEquals(12, UseMonths.span(from("2024-11"), now))
        // An empty month kept doesn't count, nor one after the month looked at.
        assertEquals(6, UseMonths.span(mapOf("2025-01" to UsageMonth("2025-01")), now))
        assertEquals(6, UseMonths.span(from("2026-02"), YearMonth.parse("2026-05")))
    }

    @Test
    fun theAverageCountsOnlyMonthsWithUse() {
        val months = listOf(UsageMonth("2026-09", 3_000, 0, 30), UsageMonth("2026-10"), UsageMonth("2026-11", 1_000, 0, 10))
        assertEquals(100L, UseMonths.average(months))
        assertEquals(null, UseMonths.average(listOf(UsageMonth("2026-10"))))
    }
}
