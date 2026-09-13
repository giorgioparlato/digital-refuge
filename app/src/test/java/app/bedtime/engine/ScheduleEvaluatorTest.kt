package app.bedtime.engine

import app.bedtime.data.DndMode
import app.bedtime.data.Run
import app.bedtime.data.RuntimeState
import app.bedtime.data.Schedule
import app.bedtime.data.ScheduleKind
import app.bedtime.data.ScheduleOverride
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

class ScheduleEvaluatorTest {
    private val zone = ZoneOffset.UTC
    private val monday = LocalDate.of(2026, 9, 14)
    private val tuesday = monday.plusDays(1)

    private val night = Schedule(
        id = "night",
        days = setOf(DayOfWeek.MONDAY.value),
        startMinute = 22 * 60,
        endMinute = 7 * 60,
        blockedApps = setOf("com.social"),
        greyscale = true,
    )

    private fun at(day: LocalDate, hour: Int, minute: Int = 0) =
        day.atTime(hour, minute).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun eval(now: Long, vararg schedules: Schedule, runtime: RuntimeState = RuntimeState()) =
        ScheduleEvaluator.evaluate(now, zone, schedules.toList(), runtime)

    @Test
    fun fixtureDateIsMonday() {
        assertEquals(DayOfWeek.MONDAY, monday.dayOfWeek)
    }

    @Test
    fun overnightSessionSpansMidnight() {
        assertFalse(eval(at(monday, 21, 59), night).isActive)
        assertTrue(eval(at(monday, 22), night).isActive)
        assertTrue(eval(at(tuesday, 6, 59), night).isActive)
        assertFalse(eval(at(tuesday, 7), night).isActive)
        // The session starts on Mondays only, so Tuesday night is free.
        assertFalse(eval(at(tuesday, 23), night).isActive)
    }

    @Test
    fun sameDayWindowRespectsDays() {
        val work = Schedule(id = "work", days = (1..5).toSet(), startMinute = 9 * 60, endMinute = 17 * 60)
        assertTrue(eval(at(monday, 10), work).isActive)
        assertFalse(eval(at(monday, 17), work).isActive)
        assertFalse(eval(at(monday.plusDays(5), 10), work).isActive) // Saturday
    }

    @Test
    fun equalStartAndEndMeansFullDay() {
        val allDay = Schedule(id = "all", days = setOf(1), startMinute = 0, endMinute = 0)
        assertTrue(eval(at(monday, 0), allDay).isActive)
        assertTrue(eval(at(monday, 23, 59), allDay).isActive)
        assertFalse(eval(at(tuesday, 0), allDay).isActive)
    }

    @Test
    fun disabledScheduleIsIgnored() {
        val state = eval(at(monday, 23), night.copy(enabled = false))
        assertFalse(state.isActive)
        assertNull(state.nextBoundary)
    }

    @Test
    fun nextBoundaryIsUpcomingStartOrEnd() {
        assertEquals(at(monday, 22), eval(at(monday, 12), night).nextBoundary)
        assertEquals(at(tuesday, 7), eval(at(monday, 23), night).nextBoundary)
    }

    @Test
    fun endSessionOverrideSuppressesOnlyThatOccurrence() {
        val everyNight = night.copy(days = (1..7).toSet())
        val runtime = RuntimeState(overrides = mapOf("night" to ScheduleOverride(endedOccurrenceStart = at(monday, 22))))
        assertFalse(eval(at(monday, 23), everyNight, runtime = runtime).isActive)
        assertFalse(eval(at(tuesday, 6), everyNight, runtime = runtime).isActive)
        assertTrue(eval(at(tuesday, 22, 30), everyNight, runtime = runtime).isActive)
    }

    @Test
    fun pauseOverrideExpires() {
        val pausedUntil = at(monday, 23)
        val runtime = RuntimeState(overrides = mapOf("night" to ScheduleOverride(pausedUntil = pausedUntil)))
        val paused = eval(at(monday, 22, 30), night, runtime = runtime)
        assertFalse(paused.isActive)
        assertEquals(pausedUntil, paused.nextBoundary)
        assertTrue(eval(at(monday, 23), night, runtime = runtime).isActive)
    }

    @Test
    fun combinesOverlappingSchedules() {
        val a = night.copy(id = "a", blockedApps = setOf("x"), greyscale = false, minimalMode = true, allowedApps = setOf("p", "q"))
        val b = night.copy(id = "b", blockedApps = setOf("y"), greyscale = true, minimalMode = true, allowedApps = setOf("q", "r"))
        val state = eval(at(monday, 23), a, b)
        assertEquals(setOf("x", "y"), state.blocked)
        assertTrue(state.greyscale)
        assertEquals(setOf("q"), state.minimalAllowlist)
        assertEquals("a", state.blockerOf("x")?.schedule?.id)
        assertEquals("a", state.blockerOf("r")?.schedule?.id) // not allowed by a's minimal mode
        assertNull(state.blockerOf("q"))
    }

    @Test
    fun noMinimalModeMeansNoAllowlist() {
        assertNull(eval(at(monday, 23), night).minimalAllowlist)
    }

    @Test
    fun blockIsActiveOnlyWhileRunning() {
        val block = Schedule(id = "b", kind = ScheduleKind.BLOCK, durationMinutes = 60, blockedApps = setOf("x"))
        val runtime = RuntimeState(activeRuns = mapOf("b" to Run(at(monday, 10), at(monday, 11))))
        assertFalse(eval(at(monday, 9, 59), block, runtime = runtime).isActive)
        val running = eval(at(monday, 10, 30), block, runtime = runtime)
        assertTrue(running.isActive)
        assertEquals(setOf("x"), running.blocked)
        assertEquals(at(monday, 11), running.nextBoundary)
        assertFalse(eval(at(monday, 11), block, runtime = runtime).isActive)
        assertFalse(eval(at(monday, 10, 30), block).isActive) // never started
        assertNull(ScheduleEvaluator.nextStart(block, at(monday, 9), zone))
    }

    @Test
    fun blockAndScheduleCombine() {
        val block = Schedule(id = "b", kind = ScheduleKind.BLOCK, blockedApps = setOf("y"), greyscale = false)
        val runtime = RuntimeState(activeRuns = mapOf("b" to Run(at(monday, 22, 30), at(monday, 23, 30))))
        val state = eval(at(monday, 23), night, block, runtime = runtime)
        assertEquals(setOf("com.social", "y"), state.blocked)
        assertEquals(2, state.active.size)
    }

    @Test
    fun strongestDndAndHideAreCombined() {
        val a = night.copy(id = "a", dnd = DndMode.PRIORITY)
        val b = night.copy(id = "b", dnd = DndMode.SILENCE, hideNotifications = true)
        val state = eval(at(monday, 23), a, b)
        assertEquals(DndMode.SILENCE, state.dnd)
        assertTrue(state.hideNotifications)
        val idle = eval(at(monday, 12), a)
        assertEquals(DndMode.OFF, idle.dnd)
        assertFalse(idle.hideNotifications)
    }
}
