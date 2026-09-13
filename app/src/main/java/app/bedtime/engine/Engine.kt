package app.bedtime.engine

import android.content.Context
import app.bedtime.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.ZoneId

/**
 * Process-wide source of truth for what is enforced right now. Re-evaluates whenever schedules or
 * runtime state change, at every schedule boundary, and whenever [refresh] is called (screen on,
 * clock change) because coroutine delays can run late while the device dozes.
 */
object Engine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val tick = MutableStateFlow(0)

    @Volatile
    private var shared: StateFlow<ActiveState?>? = null

    fun refresh() = tick.update { it + 1 }

    /** Null until the first evaluation, so screens never act on a placeholder "idle" state. */
    fun state(context: Context): StateFlow<ActiveState?> = shared ?: synchronized(this) {
        shared ?: build(Repository.get(context)).also { shared = it }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun build(repo: Repository): StateFlow<ActiveState?> =
        combine(repo.schedules, repo.runtime, tick) { schedules, runtime, _ -> schedules to runtime }
            .flatMapLatest { (schedules, runtime) ->
                flow {
                    while (true) {
                        val now = System.currentTimeMillis()
                        val result = ScheduleEvaluator.evaluate(now, ZoneId.systemDefault(), schedules, runtime)
                        emit(result)
                        val next = result.nextBoundary ?: awaitCancellation()
                        delay(next - now + 200)
                    }
                }
            }
            .stateIn(scope, SharingStarted.Eagerly, null)
}
