package app.bedtime.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.AppSettings
import app.bedtime.data.Repository
import app.bedtime.data.Schedule
import app.bedtime.data.SessionLog
import app.bedtime.engine.ActiveState
import app.bedtime.engine.Engine
import app.bedtime.engine.Stats
import app.bedtime.ui.apps.AppPickerScreen
import app.bedtime.ui.apps.PickerLock
import app.bedtime.ui.apps.PickerMode
import app.bedtime.ui.formatTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Until when the always-available list stays shut, or null if it can be changed now.
 *
 * Everything on this list is exempt from *every* session, so adding to it mid-session is a way out of
 * all of them at once — and a permanent one. A running session therefore closes it, and, exactly as a
 * schedule's own settings do, an early unlock keeps it closed for as long as that session had left.
 */
internal fun alwaysAvailableLockedUntil(
    schedules: List<Schedule>,
    history: List<SessionLog>,
    active: ActiveState?,
    now: Long,
): Long? {
    val running = active?.active.orEmpty().maxOfOrNull { it.end }
    val afterUnlock = schedules
        .filterNot { it.editAfterUnlock }
        .mapNotNull { Stats.lockedUntil(history, it.id, now) }
        .maxOrNull()
    return listOfNotNull(running, afterUnlock).maxOrNull()
}

/** Settings → Emergency → Always-available apps. */
@Composable
fun AlwaysAvailableScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings: AppSettings? by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val schedules by repo.schedules.collectAsStateWithLifecycle(initialValue = emptyList())
    val history by repo.history.collectAsStateWithLifecycle(initialValue = emptyList())
    val active by Engine.state(context).collectAsStateWithLifecycle()
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    val current = settings ?: return

    val lockedUntil = alwaysAvailableLockedUntil(schedules, history, active, now)
    val lock = lockedUntil?.let {
        val running = active?.isActive == true
        PickerLock(
            title = if (running) "A session is running" else "You left a session early",
            body = if (running) {
                "Apps here are never blocked, so adding one now would be a way out of the session you are in — " +
                    "and of every session after it. The list opens again at ${formatTime(context, it)}."
            } else {
                "The list stays shut until ${formatTime(context, it)}, when that session would have ended, " +
                    "so unlocking early isn't a way to make the next one easier."
            },
            button = "Locked until ${formatTime(context, it)}",
        )
    }

    AppPickerScreen(
        title = "Always available",
        initial = current.alwaysAvailable,
        mode = PickerMode.ALWAYS,
        lock = lock,
        onDone = { selected ->
            scope.launch {
                repo.updateSettings { it.copy(alwaysAvailable = selected) }
                onBack()
            }
        },
        onBack = onBack,
    )
}
