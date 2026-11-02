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
}
