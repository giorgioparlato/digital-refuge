package app.bedtime.ui.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.MainActivity
import app.bedtime.Routes
import app.bedtime.data.Repository
import app.bedtime.data.Schedule
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.Text
import app.bedtime.ui.formatMinutes
import app.bedtime.ui.theme.BedtimeTheme
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.launch

/** Opens when a block widget is placed (or reconfigured) to pick which block it starts. */
class BlockWidgetConfigActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val widgetId = intent?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            ?: AppWidgetManager.INVALID_APPWIDGET_ID
        // Backing out without a choice doesn't place the widget.
        setResult(RESULT_CANCELED, result(widgetId))
        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }
        enableEdgeToEdge()
        val repo = Repository.get(this)
        val activity = this
        setContent {
            BedtimeTheme {
                val scope = rememberCoroutineScope()
                val schedules: List<Schedule>? by repo.schedules.collectAsStateWithLifecycle(initialValue = null)
                val blocks = schedules?.filter { it.isBlock } ?: return@BedtimeTheme
                WidgetBlockPickerContent(
                    blocks = blocks,
                    onPick = { block ->
                        scope.launch {
                            repo.updateSettings { it.copy(widgetBlocks = it.widgetBlocks + (widgetId.toString() to block.id)) }
                            BlockWidget.updateAll(activity)
                            setResult(RESULT_OK, result(widgetId))
                            finish()
                        }
                    },
                    onCreate = {
                        setResult(RESULT_OK, result(widgetId))
                        startActivity(MainActivity.intent(activity, Routes.CREATE))
                        finish()
                    },
                    onBack = ::finish,
                )
            }
        }
    }

    private fun result(widgetId: Int) = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
}

@Composable
internal fun WidgetBlockPickerContent(blocks: List<Schedule>, onPick: (Schedule) -> Unit, onCreate: () -> Unit, onBack: () -> Unit) {
    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Widget", onBack = onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                if (blocks.isEmpty()) {
                    "You don't have any focus blocks yet. Create one, and this widget will start it with a tap."
                } else {
                    "Which block should this widget start? While it runs, the widget shows when it ends."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            blocks.forEach { block ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(c.bgSecondary)
                        .clickable(onClickLabel = "use ${block.name}") { onPick(block) }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(BedtimeIcons.Target, contentDescription = null, tint = c.accent, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(block.name, style = MaterialTheme.typography.titleMedium, color = c.textNormal)
                        Text(formatMinutes(block.durationMinutes.toLong()), style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                    }
                }
            }
            if (blocks.isEmpty()) CtaButton("Create a block", onClick = onCreate, modifier = Modifier.fillMaxWidth())
        }
    }
}
