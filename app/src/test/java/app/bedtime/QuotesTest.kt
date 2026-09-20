package app.bedtime

import app.bedtime.data.QuoteRefresh
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

    @Test
    fun quoteFollowsTheChosenRhythm() {
        // Anchored to the epoch so every period boundary is exact, whatever today happens to be.
        val epoch = LocalDate.ofEpochDay(0).atStartOfDay()

        // Daily behaves exactly as before: a new one at local midnight.
        val day = LocalDate.of(2026, 9, 20)
        assertEquals(Quotes.forDay(day), Quotes.forPeriod(day.atTime(12, 30), QuoteRefresh.DAILY))

        // Hourly turns over every hour.
        assertNotEquals(Quotes.forPeriod(epoch, QuoteRefresh.HOURLY), Quotes.forPeriod(epoch.plusHours(1), QuoteRefresh.HOURLY))

        // Six-hourly holds through the stretch, then turns over.
        assertEquals(Quotes.forPeriod(epoch, QuoteRefresh.SIX_HOURS), Quotes.forPeriod(epoch.plusHours(5), QuoteRefresh.SIX_HOURS))
        assertNotEquals(Quotes.forPeriod(epoch, QuoteRefresh.SIX_HOURS), Quotes.forPeriod(epoch.plusHours(6), QuoteRefresh.SIX_HOURS))

        // Weekly holds for a whole week.
        assertEquals(Quotes.forPeriod(epoch, QuoteRefresh.WEEKLY), Quotes.forPeriod(epoch.plusDays(6), QuoteRefresh.WEEKLY))
        assertNotEquals(Quotes.forPeriod(epoch, QuoteRefresh.WEEKLY), Quotes.forPeriod(epoch.plusDays(7), QuoteRefresh.WEEKLY))
    }
}
