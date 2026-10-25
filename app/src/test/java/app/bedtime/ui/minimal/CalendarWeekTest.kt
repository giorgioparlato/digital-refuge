package app.bedtime.ui.minimal

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

class CalendarWeekTest {
    private val zone = ZoneId.of("Europe/Stockholm")
    private val today = LocalDate.of(2026, 10, 25)

    private fun at(day: LocalDate, hour: Int) = day.atTime(hour, 0).atZone(zone).toInstant().toEpochMilli()
    private fun utcMidnight(day: LocalDate) = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    @Test
    fun groupsByDayAllDayFirstAndSkipsEmptyDays() {
        val tomorrow = today.plusDays(1)
        val events = listOf(
            CalendarEvent("dentist", at(tomorrow, 9), at(tomorrow, 10)),
            CalendarEvent("dinner", at(today, 19), at(today, 21)),
            CalendarEvent("trip", utcMidnight(tomorrow), utcMidnight(tomorrow.plusDays(2)), allDay = true),
            CalendarEvent("yoga", at(tomorrow, 7), at(tomorrow, 8)),
        )
        val week = calendarWeek(events, today, zone)
        assertEquals(listOf("Today", "Tomorrow"), week.take(2).map { it.label })
        assertEquals(listOf("dinner"), week[0].events.map { it.title })
        // All-day first, then by time.
        assertEquals(listOf("trip", "yoga", "dentist"), week[1].events.map { it.title })
        // The trip covers the day after too, and that day is labelled by its date.
        assertEquals(listOf("trip"), week[2].events.map { it.title })
        assertEquals(3, week.size)
    }

    @Test
    fun onlySevenDays() {
        val far = today.plusDays(7)
        assertEquals(emptyList<CalendarDay>(), calendarWeek(listOf(CalendarEvent("later", at(far, 9), at(far, 10))), today, zone))
    }
}
