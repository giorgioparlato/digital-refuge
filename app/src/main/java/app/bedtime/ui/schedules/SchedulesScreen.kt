package app.bedtime.ui.schedules

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.DndMode
import app.bedtime.data.Repository
import app.bedtime.data.Schedule
import app.bedtime.data.SessionLog
import app.bedtime.engine.ActiveState
import app.bedtime.engine.Engine
import app.bedtime.engine.ScheduleEvaluator
import app.bedtime.engine.Stats
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.ui.components.SwipeToDelete
import app.bedtime.ui.components.Tag
import app.bedtime.ui.components.Text
import app.bedtime.ui.formatDays
import app.bedtime.ui.formatMinuteOfDay
import app.bedtime.ui.formatRelative
import app.bedtime.ui.formatTime
import app.bedtime.ui.pluralApps
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId

/** The schedules tab: the hours that look after themselves. */
@Composable
fun SchedulesScreen(onEdit: (String) -> Unit, onCreate: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val schedules: List<Schedule>? by repo.schedules.collectAsStateWithLifecycle(initialValue = null)
    val active by Engine.state(context).collectAsStateWithLifecycle()
    val history by repo.history.collectAsStateWithLifecycle(initialValue = emptyList())
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    val loaded = schedules ?: return
    SchedulesContent(
        schedules = loaded.filterNot { it.isBlock },
        active = active,
        history = history,
        now = now,
        onEdit = onEdit,
        onCreate = onCreate,
        onToggle = { schedule, on -> scope.launch { repo.upsert(schedule.copy(enabled = on)) } },
        onDelete = { schedule -> scope.launch { repo.delete(schedule.id) } },
    )
}

@Composable
internal fun SchedulesContent(
    schedules: List<Schedule>,
    active: ActiveState?,
    history: List<SessionLog> = emptyList(),
    now: Long,
    onEdit: (String) -> Unit,
    onCreate: () -> Unit,
    onToggle: (Schedule, Boolean) -> Unit,
    onDelete: (Schedule) -> Unit = {},
) {
    val c = Obsidian.colors
    Scaffold(
        containerColor = c.bgPrimary,
        floatingActionButton = {
            if (schedules.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    text = { Text("new schedule", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = onCreate,
                    shape = RoundedCornerShape(16.dp),
                    containerColor = c.accentFill,
                    contentColor = c.textOnAccent,
                )
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                Column(Modifier.padding(bottom = 6.dp)) {
                    Text("schedules", fontSize = 27.sp, fontWeight = FontWeight.Light, color = c.textNormal)
                    Spacer(Modifier.height(6.dp))
                    Text("hours that look after themselves", style = MaterialTheme.typography.bodyMedium, color = c.textFaint)
                }
            }
            if (schedules.isEmpty()) {
                item(key = "empty") {
                    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                        Text(
                            "No schedules yet. A schedule keeps the same hours every week — bedtime, a working morning, a quiet weekend.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = c.textMuted,
                        )
                        Spacer(Modifier.height(16.dp))
                        CtaButton("Create a schedule", onClick = onCreate, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            items(schedules.size, key = { schedules[it].id }) { index ->
                val schedule = schedules[index]
                val running = active?.occurrenceOf(schedule.id) != null
                // The same lock the editor honours: switching a schedule off, or deleting it, would
                // otherwise walk straight around the unlock steps.
                val lockedUntil = if (schedule.editAfterUnlock) null else Stats.lockedUntil(history, schedule.id, now)
                SwipeToDelete(onDelete = { onDelete(schedule) }, enabled = !running && lockedUntil == null) {
                    ScheduleCard(
                        schedule = schedule,
                        running = running,
                        lockedUntil = lockedUntil,
                        now = now,
                        onClick = { onEdit(schedule.id) },
                        onToggle = { onToggle(schedule, it) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleCard(
    schedule: Schedule,
    running: Boolean,
    lockedUntil: Long?,
    now: Long,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val c = Obsidian.colors
    val shape = RoundedCornerShape(18.dp)
    val next = remember(schedule, now) { ScheduleEvaluator.nextStart(schedule, now, ZoneId.systemDefault()) }
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (running) c.accent.copy(alpha = 0.10f) else c.bgSecondary)
            .then(if (running) Modifier.border(1.dp, c.accent.copy(alpha = 0.45f), shape) else Modifier)
            .clickable(onClickLabel = "Edit ${schedule.name}", onClick = onClick)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).alpha(if (schedule.enabled) 1f else 0.6f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(schedule.name, style = MaterialTheme.typography.titleMedium, color = c.textNormal)
                    if (running) {
                        Spacer(Modifier.width(8.dp))
                        Tag("on now")
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    "${formatMinuteOfDay(context, schedule.startMinute)} – ${formatMinuteOfDay(context, schedule.endMinute)} · " +
                        formatDays(schedule.days),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textMuted,
                )
            }
            Spacer(Modifier.width(12.dp))
            ObsidianToggle(schedule.enabled, onToggle, enabled = !running && lockedUntil == null)
        }
        val tags = buildList {
            if (schedule.minimalMode) add("minimal mode")
            if (schedule.blockedApps.isNotEmpty()) add("${pluralApps(schedule.blockedApps.size)} blocked")
            if (schedule.greyscale) add("greyscale")
            when (schedule.dnd) {
                DndMode.PRIORITY -> add("priority only")
                DndMode.SILENCE -> add("silent")
                DndMode.OFF -> Unit
            }
        }
        if (tags.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tags.forEach { Tag(it) }
            }
        }
        if (lockedUntil != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                "locked until ${formatTime(context, lockedUntil)}",
                style = MaterialTheme.typography.labelMedium,
                color = c.textFaint,
            )
        } else if (!running && schedule.enabled && next != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                "starts ${formatRelative(context, next)}",
                style = MaterialTheme.typography.labelMedium,
                color = c.accentText,
            )
        }
    }
}
