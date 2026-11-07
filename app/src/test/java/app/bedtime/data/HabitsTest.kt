package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitsTest {
    private val today = 20_000L
    private val daily = Habit(id = "walk", name = "Walk")

    @Test
    fun aDailyHabitsRunsAndToday() {
        val marks = mapOf(today - 3 to 1, today - 2 to 1, today - 1 to 1)
        // Today still open doesn't break the run yet; ticked, it adds to it.
        assertEquals(3, Habits.streak(daily, marks, today))
        assertEquals(4, Habits.streak(daily, marks + (today to 1), today))
        assertEquals(0, Habits.streak(daily, mapOf(today - 2 to 1), today))
        assertEquals(3, Habits.bestStreak(daily, marks, today))
        assertEquals(HabitDay.OPEN, Habits.day(daily, marks, today))
        assertEquals(1, Habits.tapped(daily, 0))
        assertEquals(0, Habits.tapped(daily, 1))
    }

    @Test
    fun threeTimesAWeekCoversTheRest() {
        val gym = Habits.withFrequency(Habit(name = "Gym"), HabitFrequency.WEEK, 3)
        assertEquals(3 to 7, gym.times to gym.days)
        val marks = mapOf(today - 4 to 1, today - 2 to 1, today - 1 to 1)
        // Three in the last seven days: today is already covered, and so is the run.
        assertEquals(HabitDay.MET, Habits.day(gym, marks, today))
        assertEquals(HabitDay.OPEN, Habits.day(gym, marks, today - 3))
        assertEquals("3 times a week", Habits.describe(gym))
        assertEquals("Every 3 days", Habits.describe(Habits.withFrequency(daily, HabitFrequency.EVERY, 3)))
    }

    @Test
    fun aCountHasItsAim() {
        val pushups = Habit(name = "Push-ups", kind = HabitKind.COUNT, target = 20, unit = "reps")
        assertEquals(HabitDay.PARTIAL, Habits.day(pushups, mapOf(today to 12), today))
        assertEquals(HabitDay.DONE, Habits.day(pushups, mapOf(today to 20), today))
        assertEquals(13, Habits.tapped(pushups, 12))
        assertEquals("20 reps a day", Habits.describe(pushups))
    }

    @Test
    fun strengthGrowsSlowlyAndDentsGently() {
        val month = (today - 29..today).associateWith { 1 }
        val strong = Habits.strength(daily, month, today)
        assertTrue(strong in 70..90)
        // One day missed takes a little off, not all of it.
        val missed = Habits.strength(daily, month - today, today)
        assertTrue(missed in (strong - 10) until strong)
        assertEquals(0, Habits.strength(daily, emptyMap(), today))
    }

    @Test
    fun whatTodayNeedsLeavesOutWhatsCovered() {
        val gym = Habits.withFrequency(Habit(id = "gym", name = "Gym"), HabitFrequency.WEEK, 3)
        val read = Habit(id = "read", name = "Read", inWidget = false)
        val habits = listOf(daily, gym, read)
        // Gym's three are in already; a daily one is always asked for, chosen for the widget or not.
        val marks = mapOf("gym" to mapOf(today - 4 to 1, today - 2 to 1, today - 1 to 1), "walk" to mapOf(today to 1))
        assertEquals(listOf(daily, read), Habits.forWidget(habits, marks, today, Screen.HOME, dueOnly = true, keepDone = true))
        assertEquals(listOf(read), Habits.forWidget(habits, marks, today, Screen.HOME, dueOnly = true, keepDone = false))
        assertEquals(listOf(daily, gym), Habits.forWidget(habits, marks, today, Screen.HOME, dueOnly = false, keepDone = false))
        // Today's own tick doesn't make it covered: done today, it was still due.
        val twice = mapOf("gym" to mapOf(today - 2 to 1, today - 1 to 1, today to 1))
        assertTrue(Habits.due(gym, twice.getValue("gym"), today))
    }

    @Test
    fun aSetIsDoneWhenAllItsStepsAre() {
        var legs = Habits.withFrequency(Habit(name = "Leg workout"), HabitFrequency.WEEK, 3)
        legs = Habits.withStep(Habits.withStep(Habits.withStep(legs, "Squats"), "Lunges"), "Calf raises")
        assertEquals(listOf(0, 1, 2), legs.steps.map { it.bit })
        val squats = legs.steps[0]
        val lunges = legs.steps[1]
        val some = Habits.stepTapped(Habits.stepTapped(0, squats), lunges)
        assertEquals(2, Habits.stepsTicked(legs, some))
        assertEquals(HabitDay.PARTIAL, Habits.day(legs, mapOf(today to some), today))
        assertEquals(HabitDay.DONE, Habits.day(legs, mapOf(today to Habits.stepTapped(some, legs.steps[2])), today))
        // Unticked again; and from a widget, the whole set at once.
        assertEquals(1 shl 1, Habits.stepTapped(some, squats))
        assertEquals(legs.allSteps, Habits.tapped(legs, some))
        assertEquals(0, Habits.tapped(legs, legs.allSteps))
        // A step removed keeps the others' bits; a new one doesn't take the old one's.
        val fewer = legs.copy(steps = legs.steps.filterNot { it.bit == 1 })
        assertEquals(3, Habits.withStep(fewer, "Hip thrusts").steps.last().bit)
        assertEquals("3 times a week · 3 steps", Habits.describe(legs))
    }
}
