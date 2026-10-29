package app.bedtime.engine

import app.bedtime.data.Habit
import app.bedtime.data.SessionLog
import app.bedtime.data.UseDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

class LookBackTest {
    private val zone = ZoneOffset.UTC
    private val monday = LocalDate.of(2026, 10, 19)
    private fun at(day: LocalDate, hour: Int) = day.atTime(hour, 0).toInstant(zone).toEpochMilli()

    @Test
    fun sundayEveningLooksAtTheWeekEndingNow() {
        assertEquals(monday, LookBacks.weekStart(LocalDateTime.of(2026, 10, 25, 18, 0)))
        // Before six on Sunday, and on any other day, it's the week before.
        assertEquals(monday.minusWeeks(1), LookBacks.weekStart(LocalDateTime.of(2026, 10, 25, 17, 59)))
        assertEquals(monday, LookBacks.weekStart(LocalDateTime.of(2026, 10, 28, 9, 0)))
    }

    @Test
    fun theWeekAgainstTheOneBefore() {
        val history = listOf(
            SessionLog("s", "bedtime", at(monday, 22), at(monday.plusDays(1), 7)),
            SessionLog("s", "bedtime", at(monday.plusDays(2), 22), at(monday.plusDays(3), 7), unlockTimes = listOf(at(monday.plusDays(2), 23))),
            // The week before: 1 hour.
            SessionLog("b", "focus", at(monday.minusDays(3), 9), at(monday.minusDays(3), 10)),
        )
        val days = mapOf(
            monday.toEpochDay() to UseDay(monday.toEpochDay(), all = mapOf(9 to 3600L), distracting = mapOf(9 to 1800L), apps = mapOf("insta" to 1800L)),
            monday.minusDays(1).toEpochDay() to UseDay(monday.minusDays(1).toEpochDay(), all = mapOf(9 to 7200L)),
        )
        val habit = Habit(id = "h", name = "walk")
        val marks = mapOf("h" to mapOf(monday.toEpochDay() to 1, monday.plusDays(1).toEpochDay() to 1))
        val week = LookBacks.of(monday, history, days, setOf("insta"), listOf(habit), marks, zone)
        assertEquals(9 * 60L + 9 * 60L, week.refugeMinutes)
        assertEquals(60L, week.refugeMinutesBefore)
        assertEquals(2, week.finished)
        assertEquals(1, week.kept)
        assertEquals(1, week.escapes)
        assertEquals(3600L, week.phone)
        assertEquals(7200L, week.phoneBefore)
        assertEquals("insta" to 1800L, week.topApp)
        assertEquals(2, week.habitDaysKept)
        assertEquals(7, week.habitDays)
    }

    @Test
    fun noPhoneFiguresWithoutAnyCounted() {
        val week = LookBacks.of(monday, emptyList(), emptyMap(), emptySet(), emptyList(), emptyMap(), zone)
        assertNull(week.phone)
        assertNull(week.topApp)
        assertEquals(0, week.habitDays)
    }
}
