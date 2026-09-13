package app.bedtime

import app.bedtime.data.Quotes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class QuotesTest {
    @Test
    fun everyQuoteHasTextAndAuthor() {
        Quotes.all.forEach { assertTrue(it.toString(), it.text.isNotBlank() && it.author.isNotBlank()) }
    }

    @Test
    fun noDuplicates() {
        assertEquals(Quotes.all.size, Quotes.all.map { it.text }.toSet().size)
    }

    @Test
    fun neighbouringDaysHaveDifferentAuthors() {
        (Quotes.all + Quotes.all.first()).zipWithNext().forEach { (a, b) -> assertNotEquals("${a.text} / ${b.text}", a.author, b.author) }
    }

    @Test
    fun changesDailyCyclesAndStepsWithOffset() {
        val day = LocalDate.of(2026, 9, 14)
        assertNotEquals(Quotes.forDay(day), Quotes.forDay(day.plusDays(1)))
        assertEquals(Quotes.forDay(day), Quotes.forDay(day.plusDays(Quotes.all.size.toLong())))
        assertEquals(Quotes.forDay(day.plusDays(1)), Quotes.forDay(day, offset = 1))
    }
}
