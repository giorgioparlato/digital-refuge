package app.bedtime.ui.schedules

import androidx.compose.foundation.layout.size
import app.bedtime.ui.components.ScheduleIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.bedtime.ui.components.CardRadius
import app.bedtime.data.DndMode
import app.bedtime.data.EditWindow
import app.bedtime.data.Schedule
import app.bedtime.data.SessionLog
import app.bedtime.engine.ActiveState
import app.bedtime.engine.EditWindows
import app.bedtime.engine.ScheduleEvaluator
import app.bedtime.engine.Stats
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
import java.time.ZoneId

/** The schedules tab: the hours that look after themselves. */
/** A schedule as a card that opens it, with its switch, and a swipe to delete it when that's allowed. */
@Composable
internal fun ScheduleItem(
    schedule: Schedule,
    active: ActiveState?,
    history: List<SessionLog>,
    now: Long,
    minuteOfDay: Int,
    onEdit: (String) -> Unit,
    onToggle: (Schedule, Boolean) -> Unit,
    onDelete: (Schedule) -> Unit,
) {
    val running = active?.occurrenceOf(schedule.id) != null
    // The same lock the editor honours: switching a schedule off, or deleting it, would
    // otherwise walk straight around the unlock steps.
    val lockedUntil = if (schedule.editAfterUnlock) null else Stats.lockedUntil(history, schedule.id, now)
    // And the hours it may be changed in, if it names any.
    val shut = schedule.editWindow?.takeIf { !EditWindows.isOpen(it, minuteOfDay) }
    SwipeToDelete(onDelete = { onDelete(schedule) }, enabled = !running && lockedUntil == null && shut == null) {
        ScheduleCard(
            schedule = schedule,
            running = running,
            lockedUntil = lockedUntil,
            shutWindow = shut,
            now = now,
            onClick = { onEdit(schedule.id) },
            onToggle = { onToggle(schedule, it) },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScheduleCard(
    schedule: Schedule,
    running: Boolean,
    lockedUntil: Long?,
    shutWindow: EditWindow?,
    now: Long,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val c = Obsidian.colors
    val shape = RoundedCornerShape(CardRadius)
    val next = remember(schedule, now) { ScheduleEvaluator.nextStart(schedule, now, ZoneId.systemDefault()) }
    Column(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (running) c.accent.copy(alpha = 0.10f) else c.bgSecondary)
            .border(1.dp, if (running) c.accent.copy(alpha = 0.45f) else c.border, shape)
            .clickable(onClickLabel = "Edit ${schedule.name}", onClick = onClick)
            .padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f).alpha(if (schedule.enabled) 1f else 0.6f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(ScheduleIcons.of(schedule.icon), contentDescription = null, tint = c.accent, modifier = Modifier.padding(end = 8.dp).size(18.dp))
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
            ObsidianToggle(schedule.enabled, onToggle, enabled = !running && lockedUntil == null && shutWindow == null)
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
        if (shutWindow != null) {
            Spacer(Modifier.height(10.dp))
            Text(
                "can be changed from ${formatMinuteOfDay(context, shutWindow.startMinute)}",
                style = MaterialTheme.typography.labelMedium,
                color = c.textFaint,
            )
        } else if (lockedUntil != null) {
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
