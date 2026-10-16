package app.bedtime.unlock

import android.content.Context
import app.bedtime.data.Repository
import app.bedtime.data.ScheduleOverride
import app.bedtime.data.UnlockMode
import app.bedtime.engine.Occurrence

object UnlockManager {
    /** Applies the schedule's unlock action once every challenge is passed, and logs the unlock. */
    suspend fun complete(context: Context, occurrence: Occurrence) {
        val repo = Repository.get(context)
        val schedule = occurrence.schedule
        val now = System.currentTimeMillis()
        val endsSession = schedule.unlockAction.mode == UnlockMode.END_SESSION
        repo.updateRuntime { runtime ->
            when {
                endsSession && schedule.isBlock -> runtime.copy(activeRuns = runtime.activeRuns - schedule.id)
                endsSession -> runtime.copy(
                    overrides = runtime.overrides + (schedule.id to ScheduleOverride(endedOccurrenceStart = occurrence.start)),
                )
                else -> runtime.copy(
                    overrides = runtime.overrides +
                        (schedule.id to ScheduleOverride(pausedUntil = now + schedule.unlockAction.pauseMinutes * 60_000L)),
                )
            }
        }
        repo.recordUnlock(occurrence, endedEarly = endsSession, at = now)
    }
}
