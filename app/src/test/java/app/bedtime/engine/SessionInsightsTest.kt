package app.bedtime.engine

import app.bedtime.data.MoodMark
import app.bedtime.data.SessionLog
import app.bedtime.data.UseDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

class SessionInsightsTest {
    private val zone = ZoneOffset.UTC
    private val monday = LocalDate.of(2026, 11, 2)
    private fun at(day: LocalDate, hour: Int, minute: Int = 0) = day.atTime(hour, minute).toInstant(zone).toEpochMilli()
    private fun use(day: LocalDate, all: Map<Int, Long>) = day.toEpochDay() to UseDay(day.toEpochDay(), all = all)
    private fun night(day: LocalDate, escapes: List<Long> = emptyList()) =
        SessionLog("bed", "bedtime", at(day, 22), at(day.plusDays(1), 7), unlockTimes = escapes)

    @Test
    fun phoneTimeSharedOutWithinAnHour() {
        val days = mapOf(use(monday, mapOf(22 to 1_200L, 23 to 600L)))
        assertEquals(600L + 600L, SessionInsights.phoneIn(days, at(monday, 22, 30), at(monday, 24 - 1, 59) + 60_000, zone))
        // A day not known: nothing can be said.
        assertNull(SessionInsights.phoneIn(days, at(monday, 23), at(monday.plusDays(1), 1), zone))
    }

    @Test
    fun whatASessionSparesAgainstTheSameHoursWithout() {
        // Two nights with bedtime, little phone; two without, an hour in those hours.
        val days = (0L..5L).associate { d ->
            val day = monday.plusDays(d)
            use(day, if (d < 2) mapOf(22 to 300L) else mapOf(22 to 3_600L, 12 to 60L))
        }
        val sessions = listOf(night(monday), night(monday.plusDays(1)))
        val saved = SessionInsights.saved(sessions, days, zone)!!
        assertEquals(300L, saved.with)
        assertEquals(3_600L, saved.without)
        assertTrue(SessionInsights.atNight(sessions, zone))
    }

    @Test
    fun whenAndWhyItBreaks() {
        val sessions = listOf(
            night(monday, listOf(at(monday, 22, 5), at(monday, 22, 10))),
            night(monday.plusDays(4), listOf(at(monday.plusDays(4), 23, 30))),
        )
        val breaks = SessionInsights.breaks(sessions, zone)
        assertEquals(listOf(2, 0, 0, 1), breaks.byMinutes)
        assertEquals(2, breaks.byDay[DayOfWeek.MONDAY])
        val marks = listOf(
            MoodMark(at(monday, 22, 5), "bored", "unlocking", wentOn = true),
            MoodMark(at(monday, 22, 10), "bored", "unlocking", wentOn = false),
            MoodMark(at(monday, 12), "happy"),
        )
        val why = SessionInsights.why(sessions, marks)!!
        assertEquals(listOf("bored" to 2), why.moods)
        assertEquals(1 to 2, why.putDown to why.answers)
    }

    @Test
    fun phoneFreeNightsByTheHour() {
        // Last used in the 23:00 hour, first again in the 7:00 one: about seven hours between.
        val days = mapOf(use(monday, mapOf(21 to 600L, 23 to 120L)), use(monday.plusDays(1), mapOf(2 to 30L, 7 to 300L)))
        val n = SessionInsights.nights(days, monday.plusDays(1), count = 1).single()
        assertEquals(23 to 7, n.lastHour to n.firstHour)
        assertEquals(7, n.hours)
    }

    @Test
    fun keptWeekByWeekAndBlocks() {
        val sessions = listOf(night(monday), night(monday.plusDays(1), listOf(at(monday.plusDays(1), 23))))
        val weeks = SessionInsights.keptByWeek(sessions, monday.plusDays(3), zone, weeks = 2)
        assertEquals(listOf(null, 0.5f), weeks)
        val block = { day: LocalDate, escapes: Int -> SessionLog("b", "deep work", at(day, 9), at(day, 10, 30), unlockTimes = List(escapes) { at(day, 9, 30) }) }
        val blocks = SessionInsights.blocks(listOf(block(monday, 0), block(monday.plusDays(1), 0), block(monday.plusDays(2), 1)), monday.plusDays(3), zone)!!
        assertEquals(270L, blocks.thisWeek)
        assertEquals(90L, blocks.longest)
        assertEquals(Triple("mornings", 2, 3), blocks.best)
        assertFalse(SessionInsights.atNight(listOf(block(monday, 0)), zone))
    }

    @Test
    fun theAppsUsedInsideSessions() {
        val E = app.bedtime.data.UseEvent.Kind.RESUMED
        val P = app.bedtime.data.UseEvent.Kind.PAUSED
        val events = listOf(
            app.bedtime.data.UseEvent(at(monday, 21), E, "insta"), app.bedtime.data.UseEvent(at(monday, 21, 30), P, "insta"),
            // Inside the 22:00 session: ten minutes of messages, then twenty of insta; and the home screen, not counted.
            app.bedtime.data.UseEvent(at(monday, 22, 30), E, "msg"), app.bedtime.data.UseEvent(at(monday, 22, 40), P, "msg"),
            app.bedtime.data.UseEvent(at(monday, 23), E, "insta"), app.bedtime.data.UseEvent(at(monday, 23, 20), P, "insta"),
            app.bedtime.data.UseEvent(at(monday, 23, 30), E, "home"), app.bedtime.data.UseEvent(at(monday, 23, 40), P, "home"),
        )
        assertEquals(listOf("insta" to 1_200L, "msg" to 600L), SessionInsights.apps(listOf(night(monday)), events, zone, setOf("home")))
    }
}
