package app.bedtime.ui.components

import app.bedtime.data.HourRule
import app.bedtime.data.DayHours
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
fun HourDots(
    states: List<HourState>,
    text: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    /** Your colours for calm and distracted hours, where you've chosen them. */
    calmColor: Long? = null,
    distractedColor: Long? = null,
    onClick: (() -> Unit)? = null,
) {
    val calm = calmColor?.let { Color(it) } ?: text.copy(alpha = 0.55f)
    val distracted = distractedColor?.let { Color(it) } ?: accent
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
                HourState.DISTRACTED, HourState.NOW_DISTRACTED -> distracted
                HourState.CALM, HourState.NOW -> calm
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

/** Today's seconds by hour, in distracting apps and in any, refreshed as they're counted. */
@Composable
fun rememberTodayHours(): DayHours {
    val context = LocalContext.current
    val repo = remember { Repository.get(context.applicationContext) }
    val usage by repo.usage.collectAsStateWithLifecycle(initialValue = emptyList())
    val today = HourlyUse.today()
    val day = usage.lastOrNull { it.day == today }
    return DayHours(day?.hourly.orEmpty(), day?.hourlyAll.orEmpty())
}

/** The minutes behind the dots: each hour of today with any time in distracting apps. */
@Composable
fun HourDetailDialog(hours: DayHours, rule: HourRule, background: Color, text: Color, faint: Color, accent: Color, onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(background).padding(24.dp),
        ) {
            Text("Today by the hour", style = MaterialTheme.typography.titleMedium, color = text)
            Text(
                if (rule.totalMinutes == null) {
                    "Minutes in your distracting apps; over ${rule.appMinutes} counts as a distracted hour."
                } else {
                    "Minutes in your distracting apps, then in any app; over ${rule.appMinutes} or over ${rule.totalMinutes} counts as a distracted hour."
                },
                style = MaterialTheme.typography.bodySmall,
                color = faint,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            val used = (0 until 24).filter { (hours.apps[it] ?: 0L) >= 60 || (rule.totalMinutes != null && (hours.all[it] ?: 0L) >= 60) }
            if (used.isEmpty()) {
                Text("Nothing yet today.", style = MaterialTheme.typography.bodyMedium, color = faint)
            } else {
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    used.forEach { hour ->
                        item(key = hour) {
                            val inApps = ((hours.apps[hour] ?: 0L) / 60).toInt()
                            val inAll = ((hours.all[hour] ?: 0L) / 60).toInt()
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(formatMinuteOfDay(context, hour * 60), style = MaterialTheme.typography.bodyMedium, color = faint, modifier = Modifier.weight(1f))
                                Text(
                                    if (rule.totalMinutes == null) "$inApps min" else "$inApps min · $inAll in all",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (HourlyUse.distracted(hours, hour, rule)) accent else text,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
