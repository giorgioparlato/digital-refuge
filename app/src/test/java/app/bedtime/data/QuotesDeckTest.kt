package app.bedtime.data

import app.bedtime.ui.quotes.addMine
import app.bedtime.ui.quotes.editMine
import app.bedtime.ui.quotes.removeMine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

class QuotesDeckTest {
    private val day = LocalDate.of(2026, 10, 23)
    private val mine = listOf(Quote("Mine one"), Quote("Mine two", "Me"), Quote("Mine three", "Me", "Notebook"))

    @Test
    fun withNothingChosenTheRotationIsUnchanged() {
        // The scramble every version before this one used.
        val before = Quotes.all.indices.shuffled(Random(20260922L)).map { Quotes.all[it] }
        assertEquals(before, Quotes.deck())
        assertEquals(before, Quotes.deck(QuotePrefs(source = QuoteSource.ALL, hidden = setOf("not a quote"))))
    }

    @Test
    fun hiddenQuotesNeverShow() {
        val hidden = Quotes.all.take(5).map { it.text }.toSet()
        val deck = Quotes.deck(QuotePrefs(hidden = hidden, mine = mine))
        assertTrue(deck.none { it.text in hidden })
        assertEquals(Quotes.all.size - 5 + mine.size, deck.size)
    }

    @Test
    fun favouritesComeRoundThreeTimesEvenlySpaced() {
        val favourites = Quotes.all.slice(listOf(3, 30, 60)).map { it.text }.toSet()
        val deck = Quotes.deck(QuotePrefs(favourites = favourites))
        assertEquals(Quotes.all.size + 2 * favourites.size, deck.size)
        deck.groupingBy { it.text }.eachCount().forEach { (text, n) ->
            assertEquals(text, if (text in favourites) Quotes.FAVOURITE_TIMES else 1, n)
        }
        val third = deck.size / 3.0
        favourites.forEach { text ->
            val at = deck.indices.filter { deck[it].text == text }
            val gaps = (at.zipWithNext { a, b -> b - a } + (at.first() + deck.size - at.last()))
            assertTrue("$text at $at", gaps.all { it > third - 4 && it < third + 4 })
        }
    }

    @Test
    fun onlyYoursAndOnlyFavourites() {
        assertEquals(mine.toSet(), Quotes.deck(QuotePrefs(QuoteSource.MINE, mine = mine)).toSet())
        val favourites = setOf(Quotes.all[0].text, mine[1].text)
        val deck = Quotes.deck(QuotePrefs(QuoteSource.FAVOURITES, mine = mine, favourites = favourites))
        // Each once: under "only favourites" they're all favourites.
        assertEquals(favourites, deck.map { it.text }.toSet())
        assertEquals(2, deck.size)
    }

    @Test
    fun anEmptyChoiceFallsBackRatherThanGoingBlank() {
        // Only yours, with none of your own: the app's.
        assertEquals(Quotes.all.toSet(), Quotes.deck(QuotePrefs(QuoteSource.MINE)).toSet())
        // Only favourites, with none: the app's and yours.
        assertEquals((Quotes.all + mine).toSet(), Quotes.deck(QuotePrefs(QuoteSource.FAVOURITES, mine = mine)).toSet())
        // Everything hidden: the app's after all.
        val everything = (Quotes.all + mine).map { it.text }.toSet()
        assertEquals(Quotes.all.toSet(), Quotes.deck(QuotePrefs(mine = mine, hidden = everything)).toSet())
    }

    @Test
    fun recentGoesBackThroughThePastPeriods() {
        val now = day.atTime(9, 0)
        val recent = Quotes.recent(now, QuoteRefresh.DAILY)
        assertEquals(10, recent.size)
        assertEquals(Quotes.forDay(day), recent[0])
        assertEquals(Quotes.forDay(day.minusDays(1)), recent[1])
        assertEquals(Quotes.forDay(day.minusDays(9)), recent[9])
    }

    @Test
    fun pastingManyReadsTheCredits() {
        val parsed = Quotes.parseMany(
            """
            “Be here now.” — Ram Dass, Be Here Now
            Slow is smooth – Anon
            A well-known saying - Someone
            Nothing credited

            "Quoted" -- not a dash
            """.trimIndent(),
        )
        assertEquals(Quote("Be here now.", "Ram Dass", "Be Here Now"), parsed[0])
        assertEquals(Quote("Slow is smooth", "Anon"), parsed[1])
        // A hyphen inside a word is part of the quote; only a spaced one starts the credit.
        assertEquals(Quote("A well-known saying", "Someone"), parsed[2])
        assertEquals(Quote("Nothing credited"), parsed[3])
        assertEquals(5, parsed.size)
    }

    @Test
    fun creditsLeaveOutWhatIsMissing() {
        assertNull(Quote("x").credit())
        assertEquals("Me", Quote("x", "Me").credit())
        assertEquals("Me, Notebook", Quote("x", "Me", "Notebook").credit())
        assertEquals("Notebook", Quote("x", "", "Notebook").credit())
    }

    @Test
    fun editingOneOfYoursKeepsItsMarks() {
        val prefs = QuotePrefs(mine = mine, favourites = setOf("Mine two")).editMine("Mine two", Quote("Mine 2", "Me"))
        assertEquals(listOf("Mine one", "Mine 2", "Mine three"), prefs.mine.map { it.text })
        assertEquals(setOf("Mine 2"), prefs.favourites)
        val removed = prefs.removeMine("Mine 2")
        assertFalse(removed.mine.any { it.text == "Mine 2" })
        assertTrue(removed.favourites.isEmpty())
        // Adding skips what's already there, the app's own included.
        assertEquals(3, removed.addMine(listOf(Quote("Mine one"), Quote(Quotes.all[0].text), Quote("New"))).mine.size)
    }

    @Test
    fun typingUsesTheSameChoices() {
        val hidden = Quotes.all[0].text
        val favourite = Quotes.all[1].text
        val pool = Passages.forPrefs(QuotePrefs(mine = mine, hidden = setOf(hidden), favourites = setOf(favourite)))
        assertFalse(hidden in pool)
        assertTrue(mine.all { it.text in pool })
        assertEquals(Quotes.FAVOURITE_TIMES, pool.count { it == favourite })
        assertEquals(Passages.ALL, Passages.forPrefs(QuotePrefs()))
    }
}
