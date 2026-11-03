package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneOffset

class MoodsTest {
    private val zone = ZoneOffset.UTC
    private fun at(day: Int, hour: Int, month: Int = 10) = LocalDateTime.of(2026, month, day, hour, 0).toInstant(zone).toEpochMilli()

    @Test
    fun aMonthsMoodsCountedAndEachDaysMost() {
        val entries = listOf(
            JournalEntry(1, "", moods = listOf(MoodMark(at(30, 9), "tired"), MoodMark(at(30, 21), "bored"), MoodMark(at(30, 23), "tired"))),
            JournalEntry(2, "", moods = listOf(MoodMark(at(31, 8), "happy"), MoodMark(at(31, 20), "bored"), MoodMark(at(1, 9, month = 11), "angry"))),
        )
        val marks = Moods.inMonth(entries, YearMonth.of(2026, 10), zone)
        assertEquals(5, marks.size)
        assertEquals(listOf("bored" to 2, "tired" to 2, "happy" to 1), Moods.counts(marks))
        // A tie goes to the latest of them.
        assertEquals(mapOf(LocalDate.of(2026, 10, 30) to "tired", LocalDate.of(2026, 10, 31) to "bored"), Moods.byDay(marks, zone))
        assertEquals("evenings", Moods.mostly(marks, "bored", zone))
        // Once isn't a pattern.
        assertNull(Moods.mostly(marks, "happy", zone))
        assertEquals("nights", Moods.partOfDay(2))
    }

    private fun mark(day: Int, hour: Int, mood: String, from: String? = null, wentOn: Boolean? = null, after: Boolean = false) =
        MoodMark(at(day, hour), mood, from, wentOn, after)

    @Test
    fun aMoodsLinesAndItsWeek() {
        val entries = listOf(
            JournalEntry(LocalDate.of(2026, 10, 30).toEpochDay(), "- 09:00 · tired · slept badly\n- 10:00 · coffee"),
            JournalEntry(LocalDate.of(2026, 10, 31).toEpochDay(), "- 21:00 · unlocking · went on · tired, bored · what for? mail"),
        )
        assertEquals(
            listOf(LocalDate.of(2026, 10, 31) to "21:00 · unlocking · went on · tired, bored · what for? mail", LocalDate.of(2026, 10, 30) to "09:00 · tired · slept badly"),
            Moods.linesWith(entries, YearMonth.of(2026, 10), "tired"),
        )
        // 30 october 2026 is a friday.
        assertEquals(mapOf((java.time.DayOfWeek.FRIDAY to "mornings") to 2), Moods.grid(listOf(mark(30, 9, "tired"), mark(30, 10, "tired")), "tired", zone))
    }

    @Test
    fun whatCameAfterAndWhereTheyreTapped() {
        val marks = listOf(
            mark(1, 9, "bored", "instagram", wentOn = true), mark(2, 9, "bored", "instagram", wentOn = true),
            mark(3, 9, "bored", Moods.UNLOCKING, wentOn = false), mark(4, 9, "bored", Moods.UNLOCKING, wentOn = true),
            mark(5, 9, "happy"), mark(6, 9, "happy"), mark(7, 9, "tired", after = true),
        )
        assertEquals(Moods.Next(putDown = 1, of = 4, app = "instagram"), Moods.next(marks, "bored"))
        assertEquals(listOf("bored") to listOf("happy"), Moods.whereFrom(marks))
        assertEquals(listOf("tired" to 1), Moods.afterwards(marks))
    }

    @Test
    fun thePhoneOnAMoodsDays() {
        fun use(day: Int, distracting: Long, late: Long = 0) = LocalDate.of(2026, 10, day).toEpochDay().let { d ->
            d to UseDay(d, all = mapOf(12 to 600L, 2 to late), distracting = mapOf(12 to distracting))
        }
        val days = mapOf(use(1, 3_600), use(2, 3_000), use(3, 600), use(4, 1_200))
        val marks = listOf(mark(1, 9, "anxious"), mark(2, 9, "anxious"), mark(3, 9, "happy"))
        assertEquals(3_300L to 900L, Moods.useOnDays(marks, days, "anxious", zone))
        // One day isn't enough to compare.
        assertNull(Moods.useOnDays(marks, days, "happy", zone))
    }

    @Test
    fun morningsAfterLateNightsAndKeptHabits() {
        val days = (1..9).associate { d -> LocalDate.of(2026, 10, d).toEpochDay().let { it to UseDay(it, all = mapOf(2 to if (d in 2..4) 1_200L else 0L, 12 to 60L)) } }
        // Tired in the mornings after the three late nights, once otherwise.
        val marks = (2..4).map { mark(it, 8, "tired") } + listOf(mark(5, 8, "tired"), mark(6, 8, "happy"), mark(7, 8, "happy"), mark(8, 8, "happy"))
        val late = Moods.afterLateNights(marks, days, zone)
        assertEquals(listOf(Moods.Odds("tired", 3, 3, 1, 4)), late)
        val walk = Habit(id = "w", name = "walk")
        val walked = (6..8).associate { LocalDate.of(2026, 10, it).toEpochDay() to 1 }
        assertEquals(listOf(walk to Moods.Odds("happy", 3, 3, 0, 4)), Moods.withHabits(marks, listOf(walk), mapOf("w" to walked), zone))
    }
}
