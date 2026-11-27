package app.bedtime.ui.minimal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.HomeStyle
import app.bedtime.data.HourRain
import app.bedtime.data.Repository
import app.bedtime.data.Weather
import app.bedtime.data.WeatherNow
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.WeatherIcons
import app.bedtime.ui.formatMinuteOfDay
import app.bedtime.ui.formatTime
import app.bedtime.ui.homestyle.palette
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The weather from a tap on it: a sheet from the bottom, like the calendar's. The rain in a line, the
 * weather now, the next hours in a row to scroll through to tomorrow morning, the next days, and the
 * weather app a tap away ([onOpenApp]), when there is one.
 */
@Composable
internal fun RainDialog(style: HomeStyle, onDismiss: () -> Unit, onOpenApp: (() -> Unit)?) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val weather by repo.weather.collectAsStateWithLifecycle(initialValue = null)
    HomeSheetDialog(onDismiss) { RainContent(weather, System.currentTimeMillis(), style, onOpenApp) }
}

@Composable
internal fun RainContent(weather: WeatherNow?, now: Long, style: HomeStyle, onOpenApp: (() -> Unit)?) {
    val context = LocalContext.current
    val p = style.palette()
    val fahrenheit = Weather.fahrenheit(style.weatherUnit)
    val zone = java.time.ZoneId.systemDefault()
    val hours = weather?.let { Weather.rainByHour(it.rainHours, now, zone, fahrenheit) }.orEmpty()
    HomeSheet(style) {
        if (weather == null) {
            Text("No weather from your weather app yet.", style = MaterialTheme.typography.bodyMedium, color = p.muted)
        } else {
            // The rain in a line, first.
            if (hours.isNotEmpty() || weather.days.isNotEmpty()) {
                Text(
                    Weather.rainWords(hours, weather.days.firstOrNull()?.rainChance, time = { formatMinuteOfDay(context, it * 60) }),
                    style = MaterialTheme.typography.titleMedium,
                    color = p.text,
                )
                Spacer(Modifier.height(14.dp))
            }
            // Now: the sky, the temperature, its words, today's low and high, and the sun.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Weather.icon(weather.code, weather.condition)?.let { Icon(WeatherIcons.of(it), contentDescription = null, tint = p.text, modifier = Modifier.size(36.dp)) }
                Text(deg(Weather.degrees(weather.kelvin, fahrenheit)), style = MaterialTheme.typography.headlineLarge, color = p.text, modifier = Modifier.padding(start = 10.dp))
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    weather.condition?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = p.muted) }
                    if (weather.low != null && weather.high != null) {
                        Text("low ${deg(Weather.degrees(weather.low, fahrenheit))} · high ${deg(Weather.degrees(weather.high, fahrenheit))}", style = MaterialTheme.typography.bodySmall, color = p.faint)
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    weather.sunrise?.let { Text("sunrise ${formatTime(context, it)}", style = MaterialTheme.typography.labelSmall, color = p.faint) }
                    weather.sunset?.let { Text("sunset ${formatTime(context, it)}", style = MaterialTheme.typography.labelSmall, color = p.faint) }
                }
            }
            if (hours.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("The next hours", style = MaterialTheme.typography.labelMedium, color = p.faint)
                Spacer(Modifier.height(6.dp))
                HoursRow(hours, style)
            }
            if (weather.days.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text("The next days", style = MaterialTheme.typography.labelMedium, color = p.faint)
                Spacer(Modifier.height(8.dp))
                DaysList(weather, now, fahrenheit, style)
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

/** An hour a column, scrolling sideways through the next day: its hour, sky, temperature and chance of rain. */
@Composable
private fun HoursRow(hours: List<HourRain>, style: HomeStyle) {
    val p = style.palette()
    val context = LocalContext.current
    val h24 = android.text.format.DateFormat.is24HourFormat(context)
    Box {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            hours.forEach { hour ->
                Column(Modifier.width(46.dp).padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        if (h24) "%02d".format(hour.hour) else "${(hour.hour + 11) % 12 + 1}${if (hour.hour < 12) "am" else "pm"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = p.faint,
                    )
                    Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                        hour.icon?.let { Icon(WeatherIcons.of(it), contentDescription = null, tint = p.text, modifier = Modifier.size(20.dp)) }
                    }
                    Text(hour.degrees?.let(::deg) ?: "", style = MaterialTheme.typography.bodyMedium, color = p.text)
                    Text("${hour.chance}%", style = MaterialTheme.typography.labelSmall, color = if (hour.chance >= 50) p.accent else p.faint)
                }
            }
        }
        // A fade at the right edge, into the sheet's own shade: there's more to scroll to. It takes the
        // row's own size, so it never stretches the sheet.
        Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
            Box(Modifier.width(28.dp).fillMaxHeight().background(Brush.horizontalGradient(listOf(androidx.compose.ui.graphics.Color.Transparent, p.chip.compositeOver(p.background)))))
        }
    }
}

/** The coming days: each with its sky, a bar from its low to its high against the others, and its chance of rain. */
@Composable
private fun DaysList(weather: WeatherNow, now: Long, fahrenheit: Boolean, style: HomeStyle) {
    val p = style.palette()
    val today = java.time.Instant.ofEpochMilli(now).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
    val lows = weather.days.mapNotNull { it.low?.let { k -> Weather.degrees(k, fahrenheit) } }
    val highs = weather.days.mapNotNull { it.high?.let { k -> Weather.degrees(k, fahrenheit) } }
    val coldest = lows.minOrNull() ?: 0
    val warmest = (highs.maxOrNull() ?: coldest + 1).coerceAtLeast(coldest + 1)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        weather.days.forEachIndexed { i, day ->
            val low = day.low?.let { Weather.degrees(it, fahrenheit) }
            val high = day.high?.let { Weather.degrees(it, fahrenheit) }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(dayName(today.plusDays(i + 1L)), style = MaterialTheme.typography.bodyMedium, color = p.muted, modifier = Modifier.width(44.dp))
                Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                    Weather.icon(day.code)?.let { Icon(WeatherIcons.of(it), contentDescription = null, tint = p.text, modifier = Modifier.size(20.dp)) }
                }
                Text(low?.let(::deg) ?: "", style = MaterialTheme.typography.bodyMedium, color = p.faint, textAlign = TextAlign.End, modifier = Modifier.width(40.dp))
                Box(Modifier.weight(1f).padding(horizontal = 8.dp).height(6.dp).clip(RoundedCornerShape(50)).background(p.text.copy(alpha = 0.08f))) {
                    if (low != null && high != null) {
                        val from = (low - coldest).toFloat() / (warmest - coldest)
                        val to = (high - coldest).toFloat() / (warmest - coldest)
                        Row(Modifier.fillMaxWidth()) {
                            if (from > 0f) Spacer(Modifier.weight(from))
                            Box(Modifier.weight((to - from).coerceAtLeast(0.02f)).height(6.dp).clip(RoundedCornerShape(50)).background(p.text.copy(alpha = 0.45f)))
                            if (to < 1f) Spacer(Modifier.weight(1f - to))
                        }
                    }
                }
                Text(high?.let(::deg) ?: "", style = MaterialTheme.typography.bodyMedium, color = p.text, modifier = Modifier.width(36.dp))
                Text(
                    day.rainChance?.let { "$it%" } ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if ((day.rainChance ?: 0) >= 50) p.accent else p.faint,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(36.dp),
                )
            }
        }
    }
}

private fun dayName(date: LocalDate): String = DateTimeFormatter.ofPattern("EEE", Locale.getDefault()).format(date)

/** "3°", or "−2°" with a true minus, which reads as one with its number where a hyphen leaves a gap. */
private fun deg(degrees: Int): String = if (degrees < 0) "\u2212${-degrees}°" else "$degrees°"
