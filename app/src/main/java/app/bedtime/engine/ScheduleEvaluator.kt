package app.bedtime.engine

import app.bedtime.data.DndMode
import app.bedtime.data.RuntimeState
import app.bedtime.data.Schedule
import app.bedtime.data.ScheduleKind
import app.bedtime.data.ScheduleOverride
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** One concrete session of a schedule or run of a block, as epoch-millis [start, end). */
data class Occurrence(val schedule: Schedule, val start: Long, val end: Long)

/** What should be enforced right now. */
data class ActiveState(
    val active: List<Occurrence>,
    /** Next instant at which the result of [ScheduleEvaluator.evaluate] can change. */
    val nextBoundary: Long?,
) {
    val isActive: Boolean get() = active.isNotEmpty()

    val blocked: Set<String> = active.flatMapTo(mutableSetOf()) { it.schedule.blockedApps }

    val greyscale: Boolean = active.any { it.schedule.greyscale }

    /** Strongest Do Not Disturb mode among active sessions. */
    val dnd: DndMode = active.maxOfOrNull { it.schedule.dnd } ?: DndMode.OFF

    val hideNotifications: Boolean = active.any { it.schedule.hideNotifications }

    /** Our own home screen stays in colour only if every session greying the screen allows it. */
    val keepHomeInColour: Boolean = active.filter { it.schedule.greyscale }.all { it.schedule.keepHomeInColour }

    /** Null when no active schedule uses minimal mode; otherwise only apps allowed by *every* such schedule. */
    val minimalAllowlist: Set<String>? = active
        .filter { it.schedule.minimalMode }
        .map { it.schedule.allowedApps }
        .reduceOrNull { a, b -> a intersect b }

    val minimalOccurrence: Occurrence? get() = active.firstOrNull { it.schedule.minimalMode }

    fun occurrenceOf(scheduleId: String): Occurrence? = active.firstOrNull { it.schedule.id == scheduleId }

    /** The active occurrence that forbids [pkg], preferring an explicit block over minimal mode. */
    fun blockerOf(pkg: String): Occurrence? =
        active.firstOrNull { pkg in it.schedule.blockedApps }
            ?: active.firstOrNull { it.schedule.minimalMode && pkg !in it.schedule.allowedApps }

    companion object {
        val Idle = ActiveState(emptyList(), null)
    }
}

object ScheduleEvaluator {

    fun evaluate(now: Long, zone: ZoneId, schedules: List<Schedule>, runtime: RuntimeState): ActiveState {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val active = mutableListOf<Occurrence>()
        val boundaries = mutableListOf<Long>()

        for (schedule in schedules) {
            val candidates = if (schedule.kind == ScheduleKind.BLOCK) {
                // A block has at most one occurrence: its current run, if started.
                listOfNotNull(runtime.activeRuns[schedule.id]?.let { Occurrence(schedule, it.start, it.end) })
            } else {
                if (!schedule.enabled) continue
                // Yesterday covers sessions that cross midnight; a week ahead finds the next start.
                occurrences(schedule, today.minusDays(1), 9, zone)
            }
            val override = runtime.overrides[schedule.id]
            override?.pausedUntil?.let { boundaries += it }
            for (occurrence in candidates) {
                boundaries += occurrence.start
                boundaries += occurrence.end
                if (now >= occurrence.start && now < occurrence.end && !isSuppressed(override, occurrence, now)) {
                    active += occurrence
                }
            }
        }
        return ActiveState(active, boundaries.filter { it > now }.minOrNull())
    }

    /** Next time [schedule] starts after [now], for display. Blocks never start by themselves. */
    fun nextStart(schedule: Schedule, now: Long, zone: ZoneId): Long? {
        if (!schedule.enabled || schedule.isBlock) return null
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        return occurrences(schedule, today, 8, zone).map { it.start }.firstOrNull { it > now }
    }

    fun occurrences(schedule: Schedule, from: LocalDate, count: Int, zone: ZoneId): List<Occurrence> =
        (0 until count).mapNotNull { i ->
            val day = from.plusDays(i.toLong())
            if (day.dayOfWeek.value !in schedule.days) return@mapNotNull null
            val endDay = if (schedule.endMinute <= schedule.startMinute) day.plusDays(1) else day
            Occurrence(schedule, epoch(day, schedule.startMinute, zone), epoch(endDay, schedule.endMinute, zone))
        }

    private fun epoch(day: LocalDate, minuteOfDay: Int, zone: ZoneId): Long =
        day.atTime(minuteOfDay / 60, minuteOfDay % 60).atZone(zone).toInstant().toEpochMilli()

    private fun isSuppressed(override: ScheduleOverride?, occurrence: Occurrence, now: Long): Boolean =
        override != null &&
            (override.endedOccurrenceStart == occurrence.start || (override.pausedUntil ?: 0L) > now)
}
