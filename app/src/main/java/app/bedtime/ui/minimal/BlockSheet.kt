package app.bedtime.ui.minimal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.bedtime.data.HomeStyle
import app.bedtime.data.Schedule
import app.bedtime.ui.components.ScheduleIcons
import app.bedtime.ui.components.Text
import app.bedtime.ui.formatMinutes
import app.bedtime.ui.homestyle.palette

/**
 * Your blocks, from a long press on the refuge mark or a gesture: a tap starts one for its usual length.
 * Opening this never starts anything by itself, so a stray gesture only shows the list.
 */
@Composable
internal fun BlockSheet(blocks: List<Schedule>, running: Set<String>, style: HomeStyle, onStart: (Schedule) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) { BlockSheetContent(blocks, running, style, onStart) }
}

@Composable
internal fun BlockSheetContent(blocks: List<Schedule>, running: Set<String>, style: HomeStyle, onStart: (Schedule) -> Unit) {
    val p = style.palette()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(p.background)
            .border(1.dp, p.faint.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
            .padding(vertical = 18.dp),
    ) {
        Text("Start a block", style = MaterialTheme.typography.titleMedium, color = p.text, modifier = Modifier.padding(horizontal = 22.dp).padding(bottom = 8.dp))
        if (blocks.isEmpty()) {
            Text("No blocks yet. Make one in digital refuge, under schedules.", style = MaterialTheme.typography.bodyMedium, color = p.faint, modifier = Modifier.padding(horizontal = 22.dp))
        }
        blocks.forEach { block ->
            val on = block.id in running
            Row(
                Modifier.fillMaxWidth().clickable(enabled = !on) { onStart(block) }.padding(horizontal = 22.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(ScheduleIcons.of(block.icon), contentDescription = null, tint = p.accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(16.dp))
                Text(block.name.ifBlank { "A block" }, style = MaterialTheme.typography.bodyLarge, color = if (on) p.faint else p.text, modifier = Modifier.weight(1f))
                Text(if (on) "on now" else formatMinutes(block.durationMinutes.toLong()), style = MaterialTheme.typography.bodyMedium, color = p.faint)
            }
        }
    }
}
