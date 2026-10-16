package app.bedtime.unlock

import android.content.Context
import app.bedtime.data.Repository
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
        val schedule = occurrence.schedule
        val now = System.currentTimeMillis()
        repo.updateRuntime { runtime ->
            when {
                minutes != null -> runtime.copy(
                    overrides = runtime.overrides + (schedule.id to ScheduleOverride(pausedUntil = now + minutes * 60_000L)),
                )
                schedule.isBlock -> runtime.copy(activeRuns = runtime.activeRuns - schedule.id)
                else -> runtime.copy(
                    overrides = runtime.overrides + (schedule.id to ScheduleOverride(endedOccurrenceStart = occurrence.start)),
                )
            }
        }
        repo.recordUnlock(occurrence, endedEarly = minutes == null, at = now)
    }
}
