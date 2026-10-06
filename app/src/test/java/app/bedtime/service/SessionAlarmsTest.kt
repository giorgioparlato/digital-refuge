package app.bedtime.service

import app.bedtime.data.RuntimeState
import app.bedtime.data.Schedule
import app.bedtime.data.ScheduleKind
import app.bedtime.data.ScheduleOverride
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneOffset

class SessionAlarmsTest {
    private val zone = ZoneOffset.UTC
    private val monday = LocalDate.of(2026, 9, 14)

    private val night = Schedule(
        id = "night",
        days = setOf(DayOfWeek.MONDAY.value, DayOfWeek.TUESDAY.value),
        startMinute = 22 * 60,
        endMinute = 7 * 60,
    )

    private fun at(day: LocalDate, hour: Int, minute: Int = 0) =
        day.atTime(hour, minute).toInstant(ZoneOffset.UTC).toEpochMilli()

    private fun wake(now: Long, schedules: List<Schedule> = listOf(night), runtime: RuntimeState = RuntimeState()) =
        SessionAlarms.nextWake(now, zone, schedules, runtime)

    @Test
    fun withoutAnythingDueThereIsNothingToWakeFor() {
        assertNull(wake(at(monday, 12), schedules = emptyList()))
    }

    @Test
    fun theNextSessionStartIsTheWakeUp() {
        assertEquals(at(monday, 22), wake(at(monday, 12)))
    }

    @Test
    fun aBlockNeverStartsByItselfSoItIsNotWaitedFor() {
        val block = night.copy(id = "block", kind = ScheduleKind.BLOCK)
        assertNull(wake(at(monday, 12), schedules = listOf(block)))
    }

    @Test
    fun aPauseEndingBeforeTheNextStartWinsIt() {
        // Mid-session at 23:00 with ten minutes granted: the block comes back long before tomorrow.
        val paused = RuntimeState(overrides = mapOf("night" to ScheduleOverride(pausedUntil = at(monday, 23, 10))))
        assertEquals(at(monday, 23, 10), wake(at(monday, 23), runtime = paused))
    }

    @Test
    fun aPauseAlreadyOverIsNotWaitedFor() {
        val stale = RuntimeState(overrides = mapOf("night" to ScheduleOverride(pausedUntil = at(monday, 9))))
        assertEquals(at(monday, 22), wake(at(monday, 12), runtime = stale))
    }

    @Test
    fun theEarliestOfSeveralPausesIsTheOneWaitedFor() {
        val paused = RuntimeState(
            overrides = mapOf(
                "night" to ScheduleOverride(pausedUntil = at(monday, 23, 30)),
                "other" to ScheduleOverride(pausedUntil = at(monday, 23, 5)),
            ),
        )
        assertEquals(at(monday, 23, 5), wake(at(monday, 23), runtime = paused))
    }

    @Test
    fun aPauseIsWaitedForEvenWithNoScheduleLeftToStart() {
        // A block paused mid-run: nothing starts by itself, but the pause still has to end.
        val block = night.copy(kind = ScheduleKind.BLOCK)
        val paused = RuntimeState(overrides = mapOf("night" to ScheduleOverride(pausedUntil = at(monday, 23, 10))))
        assertEquals(at(monday, 23, 10), wake(at(monday, 23), schedules = listOf(block), runtime = paused))
    }
}
