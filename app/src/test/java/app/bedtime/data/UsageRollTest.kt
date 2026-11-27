package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UsageRollTest {
    @Test
    fun todayIsMadeOrChanged() {
        val (days, months) = Repository.rolled(emptyList(), { emptyList() }, 100L) { it.copy(seconds = mapOf("a" to 5L)) }
        assertEquals(listOf(UsageDay(100L, seconds = mapOf("a" to 5L))), days)
        assertNull(months)
        val (again, _) = Repository.rolled(days, { emptyList() }, 100L) { it.copy(seconds = it.seconds + ("b" to 1L)) }
        assertEquals(1, again.size)
        assertEquals(mapOf("a" to 5L, "b" to 1L), again.single().seconds)
    }

    @Test
    fun daysThatFallOutGoIntoTheirMonth() {
        val nine = (1L..63L).map { UsageDay(20_000L + it, hourlyAll = mapOf(9 to 600L)) }
        val (days, months) = Repository.rolled(nine, { emptyList() }, 20_064L) { it }
        assertEquals(63, days.size)
        assertEquals(20_002L, days.first().day)
        // The oldest day left the list, and its time is in its month.
        assertEquals(600L, months!!.single().total)
    }

    @Test
    fun androidsMonthsFromBeforeRefugeAreKept() {
        val today = java.time.LocalDate.parse("2026-11-28")
        val android = listOf("2026-05", "2026-06", "2026-09", "2026-10", "2026-11").associateWith { UsageMonth(it, 1_000, 100, 30) }
        // refuge's own days begin in september: may and june are kept, the rest it counts itself.
        val usage = listOf(UsageDay(java.time.LocalDate.parse("2026-09-20").toEpochDay()))
        val kept = Repository.earlier(emptyList(), usage, android, today)
        assertEquals(listOf("2026-05", "2026-06"), kept?.map { it.month })
        // Kept already: nothing new.
        assertNull(Repository.earlier(kept!!, usage, android, today))
        // Nothing of refuge's own yet: everything before this month.
        assertEquals(listOf("2026-05", "2026-06", "2026-09", "2026-10"), Repository.earlier(emptyList(), emptyList(), android, today)?.map { it.month })
    }
}
