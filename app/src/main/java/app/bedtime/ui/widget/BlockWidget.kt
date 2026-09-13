package app.bedtime.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import app.bedtime.MainActivity
import app.bedtime.R
import app.bedtime.Routes
import app.bedtime.data.AppSettings
import app.bedtime.data.Repository
import app.bedtime.data.Schedule
import app.bedtime.engine.ActiveState
import app.bedtime.engine.ScheduleEvaluator
import app.bedtime.ui.formatMinutes
import app.bedtime.ui.formatTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.ZoneId

/**
 * Home-screen widget: one tap starts its block. While the block runs it shows the end time, and a tap
 * opens the unlock steps (stopping early always goes through them, as with the Quick Settings tile).
 */
class BlockWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        async { updateAll(context) }
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val gone = appWidgetIds.map(Int::toString).toSet()
        async { Repository.get(context).updateSettings { it.copy(widgetBlocks = it.widgetBlocks - gone) } }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action != ACTION_START) return
        val blockId = intent.getStringExtra(EXTRA_BLOCK) ?: return
        async {
            val repo = Repository.get(context)
            val block = repo.schedules.first().firstOrNull { it.id == blockId && it.isBlock }
            if (block != null) repo.startBlock(block, block.durationMinutes)
            updateAll(context)
        }
    }

    /** Runs [work] off the main thread while keeping the broadcast alive. */
    private fun async(work: suspend () -> Unit) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                work()
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        private const val ACTION_START = "app.bedtime.widget.START"
        private const val EXTRA_BLOCK = "block"

        /** Redraws every block widget to match the current blocks and sessions. */
        suspend fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val ids = manager.getAppWidgetIds(ComponentName(context, BlockWidget::class.java))
            if (ids.isEmpty()) return
            val repo = Repository.get(context)
            val schedules = repo.schedules.first()
            val settings = repo.settings.first()
            val state = ScheduleEvaluator.evaluate(System.currentTimeMillis(), ZoneId.systemDefault(), schedules, repo.runtime.first())
            ids.forEach { id -> manager.updateAppWidget(id, views(context, id, schedules, settings, state)) }
        }

        private fun blockFor(widgetId: Int, schedules: List<Schedule>, settings: AppSettings): Schedule? {
            val blocks = schedules.filter { it.isBlock }
            return blocks.firstOrNull { it.id == settings.widgetBlocks[widgetId.toString()] }
                ?: blocks.firstOrNull { it.id == settings.tileBlockId }
                ?: blocks.firstOrNull()
        }

        private fun views(context: Context, widgetId: Int, schedules: List<Schedule>, settings: AppSettings, state: ActiveState): RemoteViews {
            val block = blockFor(widgetId, schedules, settings)
            val running = block?.let { b -> state.active.firstOrNull { it.schedule.id == b.id } }
            val title: String
            val subtitle: String
            val click: PendingIntent
            when {
                block == null -> {
                    title = context.getString(R.string.app_name)
                    subtitle = "add a block"
                    click = open(context, widgetId, Routes.CREATE)
                }
                running != null -> {
                    title = block.name
                    subtitle = "on · until ${formatTime(context, running.end)}"
                    click = open(context, widgetId, Routes.unlock(block.id))
                }
                else -> {
                    title = block.name
                    subtitle = "${formatMinutes(block.durationMinutes.toLong())} · tap to start"
                    click = start(context, widgetId, block.id)
                }
            }
            return RemoteViews(context.packageName, R.layout.widget_block).apply {
                setTextViewText(R.id.widget_title, title.lowercase())
                setTextViewText(R.id.widget_subtitle, subtitle.lowercase())
                setInt(
                    R.id.widget_root,
                    "setBackgroundResource",
                    if (running != null) R.drawable.widget_background_on else R.drawable.widget_background,
                )
                setOnClickPendingIntent(R.id.widget_root, click)
            }
        }

        private fun open(context: Context, widgetId: Int, route: String): PendingIntent =
            PendingIntent.getActivity(
                context,
                widgetId,
                MainActivity.intent(context, route),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        private fun start(context: Context, widgetId: Int, blockId: String): PendingIntent =
            PendingIntent.getBroadcast(
                context,
                widgetId,
                Intent(context, BlockWidget::class.java).setAction(ACTION_START).putExtra(EXTRA_BLOCK, blockId),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
    }
}
