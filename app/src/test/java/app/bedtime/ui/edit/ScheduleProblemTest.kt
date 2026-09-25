package app.bedtime.ui.edit

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
        assertNotNull(scheduleProblem(everyDay.copy(startMinute = 22 * 60, endMinute = 22 * 60 - 4), ""))
    }

    @Test
    fun acceptsTheSmallestGapThatWorks() {
        assertNull(scheduleProblem(everyDay.copy(startMinute = 22 * 60, endMinute = 22 * 60 - 5), ""))
    }

    @Test
    fun aDayOffIsGapEnough() {
        // Six days leaves a whole uncovered day, so a full-day session can't trap anyone.
        val sixDays = everyDay.copy(days = (1..6).toSet(), startMinute = 22 * 60, endMinute = 22 * 60)
        assertNull(scheduleProblem(sixDays, ""))
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
