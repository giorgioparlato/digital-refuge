package app.bedtime.ui.minimal

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import app.bedtime.ui.components.WeatherIcons
import app.bedtime.data.WeatherView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bedtime.data.HomeStyle
import app.bedtime.data.Quote
import app.bedtime.data.credit
import app.bedtime.ui.theme.AppFonts
import androidx.compose.ui.text.font.FontStyle
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.styledTime
import app.bedtime.ui.greeting
import app.bedtime.ui.homestyle.palette
import app.bedtime.ui.timeFormatter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Greeting, clock, date and the session pill: the top of the minimal home and the lock screen. */
@Composable
internal fun SessionHeader(
    now: LocalDateTime,
    scheduleName: String,
    until: String,
    style: HomeStyle,
    calendar: String? = null,
    /** Opens the week ahead from the calendar line; null where a tap means something else (the lock screen). */
    onCalendar: (() -> Unit)? = null,
    /** The sky and temperature beside the clock, and its words, today's range or the next days under them. */
    weather: WeatherView? = null,
    /** Opens the weather app, from a tap on the weather. */
    onWeather: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val p = style.palette()
    val clock = style.clockSize.scale
    Column {
        if (style.showGreeting) {
            Text(greeting(now.hour), style = MaterialTheme.typography.titleMedium, color = p.accent)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                styledTime(timeFormatter(context).format(now), suffixSize = (30 * clock).sp),
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = (76 * clock).sp,
                    lineHeight = (84 * clock).sp,
                    fontFeatureSettings = "tnum",
                ),
                color = p.text,
                maxLines = 1,
            )
            if (weather != null) {
                Spacer(Modifier.weight(1f))
                WeatherBeside(weather, style, onWeather)
            }
        }
        val detail = weather != null && (weather.condition != null || weather.range != null || weather.days.isNotEmpty())
        if (style.showDate || detail) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (style.showDate) DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()).format(now) else "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Normal,
                    color = p.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (weather != null && detail) WeatherDetail(weather, style, onWeather)
            }
        }
        if (weather != null && weather.week.isNotEmpty()) WeatherWeek(weather, style, now, onWeather)
        if (calendar != null) {
            Text(
                calendar,
                style = MaterialTheme.typography.bodyMedium,
                color = p.muted,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .then(if (onCalendar != null) Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClickLabel = "see the week ahead", onClick = onCalendar) else Modifier),
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .background(p.chip)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(BedtimeIcons.Moon, contentDescription = null, tint = p.accent, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text("$scheduleName · until $until", style = MaterialTheme.typography.labelLarge, color = p.muted)
        }
    }
}

/** A quote in the home style's quieter colours, with its author underneath. */
@Composable
internal fun QuoteBlock(quote: Quote, style: HomeStyle, modifier: Modifier = Modifier) {
    val p = style.palette()
    val scale = style.textSize.scale
    // Serifs read a size smaller than Inter at the same number, so they get a point more.
    val serif = AppFonts.quoteStyle(style.quoteFont) == FontStyle.Italic
    Column(modifier) {
        Text(
            "“${quote.text}”",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontSize = ((if (serif) 18 else 17) * scale).sp,
                lineHeight = ((if (serif) 27 else 25) * scale).sp,
                fontFamily = AppFonts.family(LocalContext.current, style.quoteFont),
                fontStyle = AppFonts.quoteStyle(style.quoteFont),
            ),
            color = p.muted,
        )
        quote.credit()?.let { credit ->
            Spacer(Modifier.height(8.dp))
            Text("— $credit", style = MaterialTheme.typography.labelMedium, color = p.faint)
        }
    }
}

/** The sky and temperature beside the clock, no taller than it; a tap opens the weather app. */
@Composable
private fun WeatherBeside(weather: WeatherView, style: HomeStyle, onWeather: (() -> Unit)?) {
    val p = style.palette()
    val clock = style.clockSize.scale
    Column(
        Modifier
            .padding(start = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(if (onWeather != null) Modifier.clickable(onClickLabel = "open the weather", onClick = onWeather) else Modifier)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        weather.icon?.let { Icon(WeatherIcons.of(it), contentDescription = null, tint = p.text, modifier = Modifier.size((32 * clock).coerceAtLeast(20f).dp)) }
        Text(weather.temperature, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Normal, color = p.text, maxLines = 1)
    }
}

/**
 * The rest of the weather, at the end of the date line: the sky's words, today's range, or the next
 * two days as small skies with their highs.
 */
@Composable
private fun WeatherDetail(weather: WeatherView, style: HomeStyle, onWeather: (() -> Unit)?) {
    val p = style.palette()
    Row(
        Modifier
            .padding(start = 12.dp)
            .clip(RoundedCornerShape(10.dp))
            .then(if (onWeather != null) Modifier.clickable(onClickLabel = "open the weather", onClick = onWeather) else Modifier)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        (weather.condition ?: weather.range)?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = p.faint, maxLines = 1) }
        weather.days.forEach { day ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                day.icon?.let { Icon(WeatherIcons.of(it), contentDescription = null, tint = p.muted, modifier = Modifier.padding(end = 3.dp).size(16.dp)) }
                Text(day.high?.let { "$it°" }.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = p.faint)
            }
        }
    }
}

/** Five days on a line of their own under the date: each day's name, its sky, and its high over its low. */
@Composable
private fun WeatherWeek(weather: WeatherView, style: HomeStyle, now: LocalDateTime, onWeather: (() -> Unit)?) {
    val p = style.palette()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .then(if (onWeather != null) Modifier.clickable(onClickLabel = "open the weather", onClick = onWeather) else Modifier)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        weather.week.forEach { day ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (day.offset == 0) "Today" else DateTimeFormatter.ofPattern("EEE", Locale.getDefault()).format(now.plusDays(day.offset.toLong())),
                    style = MaterialTheme.typography.labelMedium,
                    color = p.faint,
                )
                Box(Modifier.padding(vertical = 5.dp).size(22.dp), contentAlignment = Alignment.Center) {
                    day.icon?.let { Icon(WeatherIcons.of(it), contentDescription = null, tint = p.text, modifier = Modifier.size(22.dp)) }
                }
                Text(day.high?.let { "$it°" }.orEmpty(), style = MaterialTheme.typography.labelLarge, color = p.text)
                Text(day.low?.let { "$it°" }.orEmpty(), style = MaterialTheme.typography.labelMedium, color = p.faint)
            }
        }
    }
}
