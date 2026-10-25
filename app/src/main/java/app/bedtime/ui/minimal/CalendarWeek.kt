package app.bedtime.ui.minimal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.bedtime.data.HomeStyle
import app.bedtime.ui.components.Text
import app.bedtime.ui.formatTime
import app.bedtime.ui.homestyle.palette

/** The week ahead from the phone's calendar, opened from the calendar line, in the home's own colours. */
@Composable
internal fun CalendarWeekDialog(days: List<CalendarDay>, style: HomeStyle, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) { CalendarWeekContent(days, style) }
}

@Composable
internal fun CalendarWeekContent(days: List<CalendarDay>, style: HomeStyle) {
    val p = style.palette()
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(p.background)
            .padding(24.dp),
    ) {
        Text("The week ahead", style = MaterialTheme.typography.titleMedium, color = p.text)
        if (days.isEmpty()) {
            Text("Nothing in the next seven days.", style = MaterialTheme.typography.bodyMedium, color = p.muted, modifier = Modifier.padding(top = 16.dp))
            return@Column
        }
        LazyColumn(Modifier.heightIn(max = 520.dp).padding(top = 8.dp)) {
            days.forEach { day ->
                item(key = day.label) {
                    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(day.label, style = MaterialTheme.typography.labelLarge, color = p.accent)
                        day.events.forEach { event ->
                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    if (event.allDay) "All day" else "${formatTime(context, event.begin)}–${formatTime(context, event.end)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = p.faint,
                                    modifier = Modifier.width(112.dp).padding(top = 2.dp),
                                )
                                Text(event.title.ifBlank { "Busy" }, style = MaterialTheme.typography.bodyMedium, color = p.text)
                            }
                        }
                    }
                }
            }
        }
    }
}
