package app.bedtime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.HourState
import app.bedtime.data.HourlyUse
import app.bedtime.data.Repository
import app.bedtime.ui.formatMinuteOfDay

/**
 * Today as 24 dots, one an hour: calm ones soft, distracted ones in [accent], this one ringed, those
 * still to come faint. A tap opens the minutes behind them.
 */
@Composable
fun HourDots(states: List<HourState>, text: Color, accent: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClickLabel = "today by the hour", onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        states.forEach { state ->
            val fill = when (state) {
                HourState.DISTRACTED, HourState.NOW_DISTRACTED -> accent
                HourState.CALM, HourState.NOW -> text.copy(alpha = 0.55f)
                HourState.AHEAD -> text.copy(alpha = 0.16f)
            }
            val now = state == HourState.NOW || state == HourState.NOW_DISTRACTED
            Box(Modifier.size(11.dp), contentAlignment = Alignment.Center) {
                if (now) Box(Modifier.size(11.dp).border(1.5.dp, text, CircleShape))
                Box(Modifier.size(if (now) 5.dp else 7.dp).clip(CircleShape).background(fill))
            }
        }
    }
}

/** Today's hours (0–23) and seconds in distracting apps, refreshed as they're counted. */
@Composable
fun rememberTodayHours(): Map<Int, Long> {
    val context = LocalContext.current
    val repo = remember { Repository.get(context.applicationContext) }
    val usage by repo.usage.collectAsStateWithLifecycle(initialValue = emptyList())
    val today = HourlyUse.today()
    return usage.lastOrNull { it.day == today }?.hourly.orEmpty()
}

/** The minutes behind the dots: each hour of today with any time in distracting apps. */
@Composable
fun HourDetailDialog(hourly: Map<Int, Long>, thresholdMinutes: Int, background: Color, text: Color, faint: Color, accent: Color, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(background).padding(24.dp),
        ) {
            Text("Today by the hour", style = MaterialTheme.typography.titleMedium, color = text)
            Text(
                "Minutes in your distracting apps; over $thresholdMinutes counts as a distracted hour.",
                style = MaterialTheme.typography.bodySmall,
                color = faint,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            val used = hourly.filterValues { it >= 60 }.toSortedMap()
            if (used.isEmpty()) {
                Text("Nothing yet today.", style = MaterialTheme.typography.bodyMedium, color = faint)
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    used.forEach { (hour, seconds) ->
                        item(key = hour) {
                            val minutes = (seconds / 60).toInt()
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(formatMinuteOfDay(context, hour * 60), style = MaterialTheme.typography.bodyMedium, color = faint, modifier = Modifier.weight(1f))
                                Text("$minutes min", style = MaterialTheme.typography.bodyMedium, color = if (minutes > thresholdMinutes) accent else text)
                            }
                        }
                    }
                }
            }
        }
    }
}
