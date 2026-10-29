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
}
