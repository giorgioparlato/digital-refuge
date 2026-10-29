package app.bedtime.unlock

import android.content.Context
import app.bedtime.data.Repository
import app.bedtime.data.RuntimeState
import app.bedtime.data.Schedule
import app.bedtime.data.ScheduleOverride
import app.bedtime.data.UnlockMode
import app.bedtime.engine.Occurrence

object UnlockManager {
    /** How long the schedule's own setting unlocks for: its set break, or null for the rest of the session. */
    fun setLength(schedule: Schedule): Int? = if (schedule.unlockAction.mode == UnlockMode.PAUSE) schedule.unlockAction.pauseMinutes else null

    /**
     * Applies the unlock once every challenge is passed, and logs it: a break of [minutes], or with
     * null the rest of the session.
     */
    suspend fun complete(context: Context, occurrence: Occurrence, minutes: Int?) {
        val repo = Repository.get(context)
        val now = System.currentTimeMillis()
        repo.updateRuntime { applied(it, occurrence, minutes, now) }
        repo.recordUnlock(occurrence, endedEarly = minutes == null, at = now)
    }

    /**
     * What an unlock does to the running state: a break of [minutes] pauses the schedule; without one,
     * a block's run ends, and a schedule's occurrence is ended until its next start.
     */
    fun applied(runtime: RuntimeState, occurrence: Occurrence, minutes: Int?, now: Long): RuntimeState {
        val schedule = occurrence.schedule
        return when {
            minutes != null -> runtime.copy(overrides = runtime.overrides + (schedule.id to ScheduleOverride(pausedUntil = now + minutes * 60_000L)))
            schedule.isBlock -> runtime.copy(activeRuns = runtime.activeRuns - schedule.id)
            else -> runtime.copy(overrides = runtime.overrides + (schedule.id to ScheduleOverride(endedOccurrenceStart = occurrence.start)))
        }
    }
}
