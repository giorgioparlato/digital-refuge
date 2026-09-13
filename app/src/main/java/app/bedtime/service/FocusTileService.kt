package app.bedtime.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.bedtime.MainActivity
import app.bedtime.R
import app.bedtime.Routes
import app.bedtime.data.AppSettings
import app.bedtime.data.Repository
import app.bedtime.data.Schedule
import app.bedtime.engine.ActiveState
import app.bedtime.engine.Engine
import app.bedtime.engine.ScheduleEvaluator
import app.bedtime.ui.formatMinutes
import app.bedtime.ui.formatTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZoneId

/**
 * Quick Settings tile: starts the chosen focus block, shows it while it runs, and opens the unlock
 * screen when tapped during a block (stopping early always goes through the unlock steps).
 */
class FocusTileService : TileService() {
    private val actions = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var listening: CoroutineScope? = null

    override fun onStartListening() {
        super.onStartListening()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { listening = it }
        val repo = Repository.get(this)
        scope.launch {
            combine(Engine.state(this@FocusTileService).filterNotNull(), repo.schedules, repo.settings) { state, schedules, settings ->
                Triple(state, schedules, settings)
            }.collect { (state, schedules, settings) -> render(state, schedules, settings) }
        }
    }

    override fun onStopListening() {
        listening?.cancel()
        listening = null
        super.onStopListening()
    }

    override fun onDestroy() {
        actions.cancel()
        super.onDestroy()
    }

    override fun onClick() {
        super.onClick()
        if (isLocked) unlockAndRun { handleClick() } else handleClick()
    }

    private fun handleClick() {
        actions.launch {
            val repo = Repository.get(this@FocusTileService)
            val schedules = repo.schedules.first()
            val now = System.currentTimeMillis()
            val state = ScheduleEvaluator.evaluate(now, ZoneId.systemDefault(), schedules, repo.runtime.first())
            val running = state.active.firstOrNull { it.schedule.isBlock }
            val block = chosenBlock(schedules, repo.settings.first())
            when {
                running != null -> open(Routes.unlock(running.schedule.id))
                block != null -> repo.startBlock(block, block.durationMinutes)
                else -> open(Routes.CREATE)
            }
        }
    }

    private fun render(state: ActiveState, schedules: List<Schedule>, settings: AppSettings) {
        val tile = qsTile ?: return
        val running = state.active.firstOrNull { it.schedule.isBlock }
        val block = chosenBlock(schedules, settings)
        val subtitle: String
        if (running != null) {
            tile.state = Tile.STATE_ACTIVE
            tile.label = running.schedule.name
            subtitle = "until ${formatTime(this, running.end)}"
        } else {
            tile.state = Tile.STATE_INACTIVE
            tile.label = block?.name ?: getString(R.string.tile_label)
            subtitle = block?.let { formatMinutes(it.durationMinutes.toLong()) } ?: "add a block"
        }
        // The app's text is all lowercase, the tile included.
        tile.label = tile.label?.toString()?.lowercase()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) tile.subtitle = subtitle.lowercase()
        tile.updateTile()
    }

    private fun chosenBlock(schedules: List<Schedule>, settings: AppSettings): Schedule? {
        val blocks = schedules.filter { it.isBlock }
        return blocks.firstOrNull { it.id == settings.tileBlockId } ?: blocks.firstOrNull()
    }

    // The Intent overload is only reached below API 34, where it's the only one available.
    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun open(route: String) {
        val intent = MainActivity.intent(this, route)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT),
            )
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }
}
