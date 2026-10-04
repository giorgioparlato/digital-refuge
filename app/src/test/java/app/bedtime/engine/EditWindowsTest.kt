package app.bedtime.engine

import app.bedtime.data.EditWindow
import app.bedtime.data.Schedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditWindowsTest {
    private fun at(hour: Int, minute: Int = 0) = hour * 60 + minute
    private val bedtime = Schedule(days = (1..7).toSet(), startMinute = at(21), endMinute = at(7))

    @Test
    fun aWindowInsideOneDayIsOpenOnlyThen() {
        val window = EditWindow(at(9), at(17))
        assertFalse(EditWindows.isOpen(window, at(8, 59)))
        assertTrue(EditWindows.isOpen(window, at(9)))
        assertTrue(EditWindows.isOpen(window, at(16, 59)))
        assertFalse(EditWindows.isOpen(window, at(17)))
    }

    @Test
    fun aWindowCanSpanMidnight() {
        val window = EditWindow(at(22), at(2))
        assertTrue(EditWindows.isOpen(window, at(23)))
        assertTrue(EditWindows.isOpen(window, at(1)))
        assertFalse(EditWindows.isOpen(window, at(12)))
    }

    @Test
    fun equalEndsMeanTheWholeDay() {
        val window = EditWindow(at(9), at(9))
        assertTrue(EditWindows.isOpen(window, at(3)))
        assertTrue(EditWindows.isOpen(window, at(15)))
    }

    @Test
    fun noWindowLeavesTheDayWideOpenAroundTheSession() {
        // 21:00-07:00 every day leaves 14 hours.
        assertEquals(14 * 60, EditWindows.shortestDailyOpening(bedtime))
        assertTrue(EditWindows.leavesAWayIn(bedtime))
    }

    @Test
    fun theUsersExampleIsAllowed() {
        // Bedtime starts at 21:00; editable only 08:00-20:00, so the evening can't talk you out of it.
        val guarded = bedtime.copy(editWindow = EditWindow(at(8), at(20)))
        assertEquals(12 * 60, EditWindows.shortestDailyOpening(guarded))
        assertTrue(EditWindows.leavesAWayIn(guarded))
    }

    @Test
    fun aWindowSwallowedByTheSessionLeavesNoWayIn() {
        // Editable only 22:00-23:00, but the session runs 21:00-07:00 right through it.
        val sealed = bedtime.copy(editWindow = EditWindow(at(22), at(23)))
        assertEquals(0, EditWindows.shortestDailyOpening(sealed))
        assertFalse(EditWindows.leavesAWayIn(sealed))
    }

    @Test
    fun aWindowOverlappingTheSessionKeepsOnlyWhatIsLeft() {
        // Editable 06:00-07:10, and the session holds until 07:00: ten minutes survive.
        val tight = bedtime.copy(editWindow = EditWindow(at(6), at(7, 10)))
        assertEquals(10, EditWindows.shortestDailyOpening(tight))
        assertTrue(EditWindows.leavesAWayIn(tight))
    }

    @Test
    fun nineMinutesIsNotEnough() {
        val tooTight = bedtime.copy(editWindow = EditWindow(at(6), at(7, 9)))
        assertEquals(9, EditWindows.shortestDailyOpening(tooTight))
        assertFalse(EditWindows.leavesAWayIn(tooTight))
    }

    @Test
    fun aSevenDayRoundTheClockScheduleStillLeavesNothing() {
        val sealed = Schedule(days = (1..7).toSet(), startMinute = at(22), endMinute = at(22))
        assertEquals(0, EditWindows.shortestDailyOpening(sealed))
        assertFalse(EditWindows.leavesAWayIn(sealed))
    }

    @Test
    fun aDayOffIsStillJudgedOnItsWorstDay() {
        // Six days of a full-day session: the uncovered day is wide open, but the others are shut.
        val sixDays = Schedule(days = (1..6).toSet(), startMinute = at(22), endMinute = at(22))
        assertEquals(0, EditWindows.shortestDailyOpening(sixDays))
        assertFalse(EditWindows.leavesAWayIn(sixDays))
    }

    @Test
    fun aWindowSplitByMidnightCountsEachDaySeparately() {
        // 23:55-00:05 is ten minutes, but only five fall on either side of midnight.
        val split = Schedule(days = emptySet(), editWindow = EditWindow(at(23, 55), at(0, 5)))
        assertEquals(5, EditWindows.shortestDailyOpening(split))
        assertFalse(EditWindows.leavesAWayIn(split))
    }
}
