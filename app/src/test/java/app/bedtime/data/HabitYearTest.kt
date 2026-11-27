package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class HabitYearTest {
    private fun day(m: Int, d: Int) = LocalDate.of(2026, m, d).toEpochDay()

    @Test
    fun aDailyHabitCountsItsDaysKeptAndTodayOnlyOnceDone() {
        val habit = Habit("read", "read")
        val marks = listOf(day(3, 1), day(3, 2), day(3, 4)).associateWith { 1 }
        // 1 to 5 March, today the 5th and still open: 3 of the 4 days before it.
        assertEquals(HabitGoal(3, 4, "days"), HabitYear.goal(habit, marks, day(3, 1), day(3, 5), today = day(3, 5)))
        assertEquals(75, HabitYear.goal(habit, marks, day(3, 1), day(3, 5), today = day(3, 5)).percent)
        // Done today, it counts.
        assertEquals(HabitGoal(4, 5, "days"), HabitYear.goal(habit, marks + (day(3, 5) to 1), day(3, 1), day(3, 5), today = day(3, 5)))
    }

    @Test
    fun timesAWeekCountsTheWeeksThatGotThere() {
        val habit = Habit("run", "run", times = 2, days = 7)
        // Week of Mon 2 March: twice. Week of the 9th: once. Week of the 16th, still going: once so far.
        val marks = listOf(day(3, 3), day(3, 5), day(3, 10), day(3, 17)).associateWith { 1 }
        assertEquals(HabitGoal(1, 2, "weeks"), HabitYear.goal(habit, marks, day(3, 2), day(3, 18), today = day(3, 18)))
        // Twice in the week going: counted already.
        assertEquals(HabitGoal(2, 3, "weeks"), HabitYear.goal(habit, marks + (day(3, 18) to 1), day(3, 2), day(3, 18), today = day(3, 18)))
    }

    @Test
    fun aCountAddsUpEachDayToItsAim() {
        val habit = Habit("pages", "pages", kind = HabitKind.COUNT, target = 10, unit = "pages")
        val marks = mapOf(day(3, 1) to 10, day(3, 2) to 25, day(3, 3) to 4)
        // Over the aim counts as the aim: 10 + 10 + 4 of 30.
        assertEquals(HabitGoal(24, 30, "pages"), HabitYear.goal(habit, marks, day(3, 1), day(3, 3), today = day(3, 4)))
    }

    @Test
    fun monthsBeforeTheStartOrToComeAreLeftOut() {
        val habit = Habit("read", "read")
        val marks = (day(2, 1)..day(2, 28)).associateWith { 1 }
        val months = HabitYear.months(habit, marks, 2026, start = day(2, 1), today = day(3, 10))
        assertNull(months[0])
        assertEquals(100, months[1])
        assertEquals(0, months[2])
        assertNull(months[3])
    }

    @Test
    fun runsLongestFirstAndTheOneGoingKeepsGoingToday() {
        val habit = Habit("read", "read")
        val marks = (listOf(day(3, 1), day(3, 2)) + (day(3, 5)..day(3, 9)) + listOf(day(3, 12), day(3, 13))).associateWith { 1 }
        val runs = HabitYear.runs(habit, marks, start = day(3, 1), today = day(3, 14))
        assertEquals(listOf(HabitRun(day(3, 5), 5), HabitRun(day(3, 12), 2), HabitRun(day(3, 1), 2)), runs)
    }

    @Test
    fun strengthDayByDayEndsWhereTodaysIs() {
        val habit = Habit("read", "read")
        val marks = (day(1, 10)..day(3, 1) step 2).associateWith { 1 }
        val daily = Habits.strengths(habit, marks, day(1, 1), day(3, 10))
        assertEquals((day(3, 10) - day(1, 1) + 1).toInt(), daily.size)
        // Nothing before the first mark; then what the single figure says for each day.
        assertEquals(0, daily.first())
        assertEquals(Habits.strength(habit, marks, day(2, 15)), daily[(day(2, 15) - day(1, 1)).toInt()])
        assertEquals(Habits.strength(habit, marks, day(3, 10)), daily.last())
    }
}
