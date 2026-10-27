package app.bedtime.ui.minimal

import java.util.Locale
import java.time.format.DateTimeFormatter
import app.bedtime.ui.components.VerbatimText
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

/**
 * The week ahead from the phone's calendar, opened from the calendar line, in the home's own colours.
 * A tap on an event shows its details, with a way into the calendar app.
 */
@Composable
internal fun CalendarWeekDialog(days: List<CalendarDay>, style: HomeStyle, onDismiss: () -> Unit, onOpenEvent: (CalendarEvent) -> Unit = {}) {
    Dialog(onDismissRequest = onDismiss) { CalendarWeekContent(days, style, onOpenEvent) }
}

@Composable
internal fun CalendarWeekContent(days: List<CalendarDay>, style: HomeStyle, onOpenEvent: (CalendarEvent) -> Unit = {}, initial: CalendarEvent? = null) {
    val p = style.palette()
    val context = LocalContext.current
    var open by remember { mutableStateOf(initial) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(p.background)
            .padding(24.dp),
    ) {
        open?.let { event ->
            EventDetail(event, style, onBack = { open = null }, onOpen = { onOpenEvent(event) })
            return@Column
        }
        Text("The week ahead", style = MaterialTheme.typography.titleMedium, color = p.text)
        if (days.isEmpty()) {
            Text("Nothing in the next seven days.", style = MaterialTheme.typography.bodyMedium, color = p.muted, modifier = Modifier.padding(top = 16.dp))
            return@Column
        }
        LazyColumn(Modifier.heightIn(max = 520.dp).padding(top = 8.dp)) {
            days.forEach { day ->
                item(key = day.label) {
                    Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(day.label, style = MaterialTheme.typography.labelLarge, color = p.accent)
                        day.events.forEach { event ->
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClickLabel = "details") { open = event }.padding(vertical = 4.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Text(
                                    if (event.allDay) "All day" else "${formatTime(context, event.begin)}–${formatTime(context, event.end)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = p.faint,
                                    modifier = Modifier.width(112.dp).padding(top = 2.dp),
                                )
                                VerbatimText(event.title.ifBlank { "Busy" }, style = MaterialTheme.typography.bodyMedium, color = p.text)
                            }
                        }
                    }
                }
            }
        }
    }
}

/** One event: what, when, where, its notes and calendar, and "open in calendar". */
@Composable
private fun EventDetail(event: CalendarEvent, style: HomeStyle, onBack: () -> Unit, onOpen: () -> Unit) {
    val p = style.palette()
    val context = LocalContext.current
    val zone = if (event.allDay) java.time.ZoneOffset.UTC else java.time.ZoneId.systemDefault()
    val day = java.time.Instant.ofEpochMilli(event.begin).atZone(zone)
    val whenText = DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault()).format(day) +
        if (event.allDay) ", all day" else ", ${formatTime(context, event.begin)}–${formatTime(context, event.end)}"
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "the week",
            tint = p.faint,
            modifier = Modifier.clip(CircleShape).clickable(onClick = onBack).padding(4.dp).size(20.dp),
        )
    }
    VerbatimText(event.title.ifBlank { "Busy" }, style = MaterialTheme.typography.titleLarge, color = p.text, modifier = Modifier.padding(top = 8.dp))
    Text(whenText, style = MaterialTheme.typography.bodyMedium, color = p.muted, modifier = Modifier.padding(top = 4.dp))
    if (event.location.isNotBlank()) {
        Row(Modifier.padding(top = 12.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = p.faint, modifier = Modifier.padding(end = 8.dp).size(18.dp))
            VerbatimText(event.location, style = MaterialTheme.typography.bodyMedium, color = p.text)
        }
    }
    if (event.description.isNotBlank()) {
        VerbatimText(
            event.description.trim(),
            style = MaterialTheme.typography.bodyMedium,
            color = p.muted,
            modifier = Modifier.padding(top = 12.dp).heightIn(max = 240.dp).verticalScroll(rememberScrollState()),
        )
    }
    if (event.calendar.isNotBlank()) {
        VerbatimText(event.calendar, style = MaterialTheme.typography.labelSmall, color = p.faint, modifier = Modifier.padding(top = 12.dp))
    }
    Text(
        "Open in calendar",
        style = MaterialTheme.typography.labelLarge,
        color = p.accent,
        modifier = Modifier.padding(top = 16.dp).clip(RoundedCornerShape(50)).border(1.dp, p.accent.copy(alpha = 0.5f), RoundedCornerShape(50)).clickable(onClick = onOpen)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}
