package app.bedtime.ui.minimal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.HomeStyle
import app.bedtime.data.Repository
import app.bedtime.data.Weather
import app.bedtime.ui.components.Text
import app.bedtime.ui.formatMinuteOfDay
import app.bedtime.ui.homestyle.palette

/**
 * Today's chance of rain, hour by hour, from a tap on the weather: a sheet from the bottom, like the
 * calendar's, with a bar for each hour and the weather app a tap away ([onOpenApp]), when there is one.
 */
@Composable
internal fun RainDialog(style: HomeStyle, onDismiss: () -> Unit, onOpenApp: (() -> Unit)?) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val weather by repo.weather.collectAsStateWithLifecycle(initialValue = null)
    val hours = weather?.let { Weather.rainByHour(it.rainHours, System.currentTimeMillis(), java.time.ZoneId.systemDefault(), Weather.fahrenheit(style.weatherUnit)) }.orEmpty()
    HomeSheetDialog(onDismiss) { RainContent(hours, style, onOpenApp) }
}

/** The sheet's contents: each hour's time, its temperature when known, and its chance of rain. */
@Composable
internal fun RainContent(hours: List<app.bedtime.data.HourRain>, style: HomeStyle, onOpenApp: (() -> Unit)?) {
    val context = LocalContext.current
    val p = style.palette()
    // A column for the temperatures only if Breezy sent any, so older weather doesn't leave a gap.
    val temps = hours.any { it.degrees != null }
    HomeSheet(style) {
        Text("Rain today", style = MaterialTheme.typography.labelMedium, color = p.faint, modifier = Modifier.padding(bottom = 10.dp))
        if (hours.isEmpty()) {
            Text("No hour-by-hour rain from your weather app yet.", style = MaterialTheme.typography.bodyMedium, color = p.muted)
        } else {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                hours.forEach { (hour, chance, degrees) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(formatMinuteOfDay(context, hour * 60), style = MaterialTheme.typography.bodyMedium, color = p.faint, modifier = Modifier.width(64.dp))
                        if (temps) Text(degrees?.let { "$it°" } ?: "", style = MaterialTheme.typography.bodyMedium, color = p.text, modifier = Modifier.width(40.dp))
                        Box(Modifier.weight(1f).height(10.dp).clip(RoundedCornerShape(50)).background(p.text.copy(alpha = 0.08f))) {
                            Box(Modifier.fillMaxWidth(chance / 100f).fillMaxHeight().clip(RoundedCornerShape(50)).background(p.accent.copy(alpha = 0.35f + 0.65f * chance / 100f)))
                        }
                        Text("$chance%", style = MaterialTheme.typography.bodyMedium, color = if (chance >= 50) p.text else p.faint, modifier = Modifier.padding(start = 12.dp).width(44.dp))
                    }
                }
            }
        }
        if (onOpenApp != null) {
            Text(
                "Open the weather app",
                style = MaterialTheme.typography.labelLarge,
                color = p.accent,
                modifier = Modifier.padding(top = 16.dp).clip(RoundedCornerShape(50)).border(1.dp, p.accent.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .clickable(onClick = onOpenApp).padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
}
