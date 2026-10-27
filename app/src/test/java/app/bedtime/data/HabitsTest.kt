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
}
