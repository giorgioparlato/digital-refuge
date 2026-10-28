package app.bedtime.ui.minimal

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalendarLineTest {
    private val now = 10_000L
    private val time: (Long) -> String = { "t$it" }

    @Test
    fun whatIsOnNowComesFirst() {
        val events = listOf(CalendarEvent("standup", now - 5, now + 100), CalendarEvent("dentist", now + 500, now + 900))
        assertEquals("now · standup until t${now + 100}", calendarLine(events, now, showNames = true, time))
        assertEquals("an event until t${now + 100}", calendarLine(events, now, showNames = false, time))
    }

    @Test
    fun otherwiseWhatIsNext() {
        val events = listOf(CalendarEvent("dentist", now + 500, now + 900))
        assertEquals("next · dentist at t${now + 500}", calendarLine(events, now, showNames = true, time))
        assertEquals("next event at t${now + 500}", calendarLine(events, now, showNames = false, time))
    }

    @Test
    fun anEmptyRestOfTheDayShowsNoLine() {
        assertNull(calendarLine(emptyList(), now, showNames = true, time))
        // An event that already ended isn't "next".
        assertNull(calendarLine(listOf(CalendarEvent("lunch", now - 900, now - 1)), now, true, time))
    }

    @Test
    fun anUntitledEventIsDescribedByItsTime() {
        assertEquals("next event at t${now + 5}", calendarLine(listOf(CalendarEvent("", now + 5, now + 9)), now, true, time))
    }

    @Test
    fun tomorrowsFirstEventOnceTodayIsDone() {
        val midnight = now + 1_000
        val events = listOf(CalendarEvent("dentist", now + 500, now + 900), CalendarEvent("standup", midnight + 300, midnight + 600))
        // Today's still to come, so it comes first.
        assertEquals("next · dentist at t${now + 500}", calendarLine(events, now, true, time, midnight))
        val evening = now + 950
        assertEquals("tomorrow · standup at t${midnight + 300}", calendarLine(events, evening, true, time, midnight))
        assertEquals("tomorrow, first event at t${midnight + 300}", calendarLine(events, evening, false, time, midnight))
    }
}
