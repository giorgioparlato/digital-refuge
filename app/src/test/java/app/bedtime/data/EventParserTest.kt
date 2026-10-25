package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class EventParserTest {
    // A Sunday evening.
    private val now = LocalDateTime.of(2026, 10, 25, 21, 0)
    private val today = now.toLocalDate()

    private fun parse(text: String) = EventParser.parse(text, now)
    private fun at(date: LocalDate, h: Int, m: Int = 0) = date.atTime(h, m)

    @Test
    fun plainTextIsNoEvent() {
        assertNull(parse("how tall is mont blanc"))
        assertNull(parse("buy 2 apples"))
        assertNull(parse("buy 2-3 apples"))
        // Only words about when, nothing to call it.
        assertNull(parse("tomorrow"))
    }

    @Test
    fun daysByName() {
        assertEquals(today.plusDays(1), parse("dentist tomorrow")!!.start.toLocalDate())
        assertEquals(today.plusDays(2), parse("dentist the day after tomorrow")!!.start.toLocalDate())
        assertEquals(today, parse("call mum today")!!.start.toLocalDate())
        assertEquals(today.plusDays(3), parse("call mum in 3 days")!!.start.toLocalDate())
        // Next week means its Monday.
        assertEquals(LocalDate.of(2026, 10, 26), parse("plan the trip next week")!!.start.toLocalDate())
    }

    @Test
    fun weekdaysAndNextOnes() {
        // Friday is five days on; "next friday" is the one in the following week.
        assertEquals(LocalDate.of(2026, 10, 30), parse("dentist friday")!!.start.toLocalDate())
        assertEquals(LocalDate.of(2026, 10, 30), parse("dentist this fri")!!.start.toLocalDate())
        // "Next friday" is next week's: from a Sunday, the week starting tomorrow.
        assertEquals(LocalDate.of(2026, 10, 30), parse("dentist next friday")!!.start.toLocalDate())
        val wednesday = LocalDateTime.of(2026, 10, 28, 12, 0)
        assertEquals(LocalDate.of(2026, 10, 30), EventParser.parse("dentist friday", wednesday)!!.start.toLocalDate())
        assertEquals(LocalDate.of(2026, 11, 6), EventParser.parse("dentist next friday", wednesday)!!.start.toLocalDate())
        // A weekday that is today means today.
        assertEquals(today, parse("brunch sunday")!!.start.toLocalDate())
    }

    @Test
    fun dates() {
        assertEquals(LocalDate.of(2026, 10, 28), parse("trip 28/10")!!.start.toLocalDate())
        assertEquals(LocalDate.of(2026, 11, 3), parse("trip 3 nov")!!.start.toLocalDate())
        assertEquals(LocalDate.of(2026, 11, 3), parse("trip november 3rd")!!.start.toLocalDate())
        assertEquals(LocalDate.of(2027, 3, 1), parse("trip 2027-03-01")!!.start.toLocalDate())
        // Already gone this year: next year's.
        assertEquals(LocalDate.of(2027, 1, 5), parse("trip 5 jan")!!.start.toLocalDate())
    }

    @Test
    fun timesAsPeopleSayThem() {
        val friday = LocalDate.of(2026, 10, 30)
        assertEquals(at(friday, 9), parse("dentist friday 9")!!.start)
        assertEquals(at(friday, 15), parse("dentist friday 3")!!.start)
        assertEquals(at(friday, 21, 30), parse("dinner friday 21:30")!!.start)
        assertEquals(at(friday, 19), parse("dinner friday at 7pm")!!.start)
        assertEquals(at(friday, 12), parse("lunch friday noon")!!.start)
        assertEquals(at(friday, 9), parse("run friday morning")!!.start)
        assertEquals(at(today, 20), parse("call anna tonight")!!.start)
    }

    @Test
    fun lengthsAndRanges() {
        val friday = LocalDate.of(2026, 10, 30)
        parse("dentist friday 9")!!.let { assertEquals(at(friday, 10), it.end) }
        parse("dinner friday 7-9pm")!!.let {
            assertEquals(at(friday, 19), it.start)
            assertEquals(at(friday, 21), it.end)
        }
        parse("workshop friday from 9 to 11")!!.let {
            assertEquals(at(friday, 9), it.start)
            assertEquals(at(friday, 11), it.end)
        }
        parse("call friday 18:30 for 30 min")!!.let { assertEquals(at(friday, 19), it.end) }
        parse("gym friday 7 for 2h")!!.let { assertEquals(at(friday, 9), it.end) }
        parse("walk friday 10 for half an hour")!!.let { assertEquals(at(friday, 10, 30), it.end) }
    }

    @Test
    fun noTimeIsAllDay() {
        val e = parse("mum's birthday 3 nov")!!
        assertTrue(e.allDay)
        assertEquals(LocalDate.of(2026, 11, 3).atStartOfDay(), e.start)
        assertEquals(LocalDate.of(2026, 11, 4).atStartOfDay(), e.end)
    }

    @Test
    fun aTimeAlreadyGoneMeansTomorrowUnlessADayWasNamed() {
        assertEquals(at(today.plusDays(1), 9), parse("stretch at 9am")!!.start)
        assertEquals(at(today, 22), parse("stretch at 22:00")!!.start)
        // Named as today, it stays today even if past.
        assertEquals(at(today, 9), parse("stretch today 9am")!!.start)
    }

    @Test
    fun theTitleIsWhatsLeft() {
        assertEquals("dentist", parse("dentist next friday at 9")!!.title)
        assertEquals("Dinner with Sam", parse("Dinner with Sam tomorrow 7-9pm")!!.title)
        assertEquals("call mum", parse("call mum on 3 nov for 30 min")!!.title)
        assertEquals("pick up keys", parse("tomorrow 9 - pick up keys")!!.title)
    }
}
