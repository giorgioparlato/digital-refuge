package app.bedtime.unlock

import android.content.Context
import app.bedtime.data.PendingWait
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
            val cleared = runtime.copy(pendingWaits = runtime.pendingWaits - schedule.id)
            when {
                endsSession && schedule.isBlock -> cleared.copy(activeRuns = cleared.activeRuns - schedule.id)
                endsSession -> cleared.copy(
                    overrides = cleared.overrides + (schedule.id to ScheduleOverride(endedOccurrenceStart = occurrence.start)),
                )
                else -> cleared.copy(
                    overrides = cleared.overrides +
                        (schedule.id to ScheduleOverride(pausedUntil = now + schedule.unlockAction.pauseMinutes * 60_000L)),
                )
            }
        }
        repo.recordUnlock(occurrence, endedEarly = endsSession, at = now)
    }

    /** Returns when the wait challenge for [occurrence] finishes, starting the timer if needed. */
    suspend fun startOrResumeWait(context: Context, occurrence: Occurrence): Long {
        val id = occurrence.schedule.id
        var readyAt = 0L
        Repository.get(context).updateRuntime { runtime ->
            val existing = runtime.pendingWaits[id]
            if (existing != null && existing.occurrenceStart == occurrence.start) {
                readyAt = existing.readyAt
                runtime
            } else {
                readyAt = System.currentTimeMillis() + occurrence.schedule.unlock.waitMinutes * 60_000L
                runtime.copy(pendingWaits = runtime.pendingWaits + (id to PendingWait(occurrence.start, readyAt)))
            }
        }
        return readyAt
    }
}
