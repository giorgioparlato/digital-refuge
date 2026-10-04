package app.bedtime.ui.edit

import app.bedtime.data.EditWindow
import app.bedtime.data.Schedule
import app.bedtime.data.ScheduleKind
import app.bedtime.data.UnlockConfig
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleProblemTest {
    private val everyDay = Schedule(days = (1..7).toSet())

    @Test
    fun acceptsAnOrdinaryNight() {
        assertNull(scheduleProblem(everyDay.copy(startMinute = 22 * 60, endMinute = 7 * 60), ""))
    }

    @Test
    fun refusesAWholeWeekWithNoGap() {
        // Equal start and end means a full day, and seven of those tile the week end to end.
        assertNotNull(scheduleProblem(everyDay.copy(startMinute = 22 * 60, endMinute = 22 * 60), ""))
    }

    @Test
    fun refusesAGapTooShortToUse() {
        assertNotNull(scheduleProblem(everyDay.copy(startMinute = 22 * 60, endMinute = 22 * 60 - 9), ""))
    }

    @Test
    fun acceptsTheSmallestGapThatWorks() {
        assertNull(scheduleProblem(everyDay.copy(startMinute = 22 * 60, endMinute = 22 * 60 - 10), ""))
    }

    @Test
    fun aDayOffIsNotEnoughOnItsOwn() {
        // Six full days running back to back leave six days with no way in at all; the free seventh
        // day doesn't help on the Tuesday you want to change it.
        val sixDays = everyDay.copy(days = (1..6).toSet(), startMinute = 22 * 60, endMinute = 22 * 60)
        assertNotNull(scheduleProblem(sixDays, ""))
    }

    @Test
    fun anOrdinaryWeekdayScheduleIsFine() {
        val weekdays = everyDay.copy(days = (1..5).toSet(), startMinute = 9 * 60, endMinute = 17 * 60)
        assertNull(scheduleProblem(weekdays, ""))
    }

    @Test
    fun refusesAnEditWindowTheSessionSwallows() {
        // Only editable 22:00-23:00, but the session runs 21:00-07:00 straight through it.
        val sealed = everyDay.copy(
            startMinute = 21 * 60,
            endMinute = 7 * 60,
            editWindow = EditWindow(22 * 60, 23 * 60),
        )
        assertNotNull(scheduleProblem(sealed, ""))
    }

    @Test
    fun acceptsAnEditWindowClearOfTheSession() {
        // The example: bedtime at 21:00, and after 20:00 you can no longer talk yourself out of it.
        val guarded = everyDay.copy(
            startMinute = 21 * 60,
            endMinute = 7 * 60,
            editWindow = EditWindow(8 * 60, 20 * 60),
        )
        assertNull(scheduleProblem(guarded, ""))
    }

    @Test
    fun blocksAreNotSchedulesAndEndByThemselves() {
        val block = everyDay.copy(kind = ScheduleKind.BLOCK, startMinute = 22 * 60, endMinute = 22 * 60)
        assertNull(scheduleProblem(block, ""))
    }

    @Test
    fun stillAsksForTheDaysAndThePassword() {
        assertNotNull(scheduleProblem(everyDay.copy(days = emptySet()), ""))
        assertNotNull(scheduleProblem(everyDay.copy(unlock = UnlockConfig(passwordEnabled = true)), ""))
        assertNull(scheduleProblem(everyDay.copy(unlock = UnlockConfig(passwordEnabled = true)), "hunter2"))
    }
}
