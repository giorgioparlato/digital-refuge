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
import app.bedtime.data.Module
import app.bedtime.data.Modules
import app.bedtime.data.Screen
import app.bedtime.data.Quote
import app.bedtime.data.credit
import app.bedtime.ui.theme.AppFonts
import androidx.compose.ui.text.font.FontStyle
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.ScheduleIcons
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.styledTime
import app.bedtime.ui.greeting
import app.bedtime.ui.homestyle.palette
import app.bedtime.ui.timeFormatter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * The top of the minimal home and the lock screen: your widgets in your order (greeting, clock, date,
 * weather, next event, and the hours and quote handed in), then the session pill.
 *
 * The weather's sky and temperature sit beside the clock while the clock is shown, and its words or
 * next days at the end of the date line when it comes straight after the date, as they always have.
 * Moved elsewhere, they make a line of their own.
 */
@Composable
internal fun SessionHeader(
    now: LocalDateTime,
    /** The session in the pill; null outside sessions, and there's no pill. */
    scheduleName: String?,
    until: String?,
    style: HomeStyle,
    calendar: String? = null,
    /** Opens the week ahead from the calendar line; null where a tap means something else (the lock screen). */
    onCalendar: (() -> Unit)? = null,
    /** The sky and temperature beside the clock, and its words, today's range or the next days under them. */
    weather: WeatherView? = null,
    /** Opens the weather app, from a tap on the weather. */
    onWeather: (() -> Unit)? = null,
    /** The widgets shown, in order. */
    modules: List<Module> = Modules.shown(style, Screen.HOME),
    /** The session's mark on its pill. */
    scheduleIcon: String? = null,
    /** Today by the hour and the quote, drawn by the screen itself, in their places. */
    hours: (@Composable () -> Unit)? = null,
    quote: (@Composable () -> Unit)? = null,
) {
    val context = LocalContext.current
    val p = style.palette()
    val clock = style.clockSize.scale
    val clockShown = Module.CLOCK in modules
    val hasNow = weather != null && weather.temperature.isNotEmpty()
    val detail = weather != null && (weather.condition != null || weather.range != null || weather.days.isNotEmpty())
    val weatherAt = modules.indexOf(Module.WEATHER)
    val detailOnDate = detail && weatherAt > 0 && modules[weatherAt - 1] == Module.DATE
    // The pill follows the last of the clock's neighbours (greeting, clock, date, weather, next event),
    // before the hours and the quote, as it always has.
    val pillAfter = modules.indexOfLast { it != Module.HOURS && it != Module.QUOTE }
    Column {
        if (pillAfter < 0) SessionPill(scheduleName, until, scheduleIcon, style)
        modules.forEachIndexed { at, module ->
            when (module) {
                Module.GREETING -> Text(greeting(now.hour), style = MaterialTheme.typography.titleMedium, color = p.accent)
                Module.CLOCK -> Row(verticalAlignment = Alignment.CenterVertically) {
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
                    if (weather != null && hasNow && Module.WEATHER in modules) {
                        Spacer(Modifier.weight(1f))
                        WeatherBeside(weather, style, onWeather)
                    }
                }
                Module.DATE -> Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()).format(now),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Normal,
                        color = p.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (weather != null && detailOnDate) WeatherDetail(weather, style, onWeather)
                }
                Module.WEATHER -> if (weather != null) {
                    val nowHere = hasNow && !clockShown
                    if (nowHere || (detail && !detailOnDate)) {
                        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (nowHere) WeatherInline(weather, style, onWeather)
                            if (detail && !detailOnDate) WeatherDetail(weather, style, onWeather, start = if (nowHere) 12.dp else 0.dp)
                        }
                    }
                    if (weather.week.isNotEmpty()) WeatherWeek(weather, style, now, onWeather)
                }
                Module.EVENT -> if (calendar != null) {
                    Text(
                        calendar,
                        style = MaterialTheme.typography.bodyMedium,
                        color = p.muted,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .then(if (onCalendar != null) Modifier.clip(RoundedCornerShape(6.dp)).clickable(onClickLabel = "see the week ahead", onClick = onCalendar) else Modifier),
                    )
                }
                Module.HOURS -> hours?.invoke()
                Module.QUOTE -> quote?.invoke()
            }
            if (at == pillAfter) SessionPill(scheduleName, until, scheduleIcon, style)
        }
    }
}

/** The session's name and end, with its mark; nothing outside sessions. */
@Composable
private fun SessionPill(scheduleName: String?, until: String?, scheduleIcon: String?, style: HomeStyle) {
    if (scheduleName == null || until == null) return
    val p = style.palette()
    Spacer(Modifier.height(16.dp))
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(p.chip)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(ScheduleIcons.of(scheduleIcon), contentDescription = null, tint = p.accent, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Text("$scheduleName · until $until", style = MaterialTheme.typography.labelLarge, color = p.muted)
    }
}

/** The sky and temperature on a line, for when the clock isn't there to sit beside. */
@Composable
private fun WeatherInline(weather: WeatherView, style: HomeStyle, onWeather: (() -> Unit)?) {
    val p = style.palette()
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .then(if (onWeather != null) Modifier.clickable(onClickLabel = "open the weather", onClick = onWeather) else Modifier)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        weather.icon?.let { Icon(WeatherIcons.of(it), contentDescription = null, tint = p.text, modifier = Modifier.padding(end = 6.dp).size(22.dp)) }
        Text(weather.temperature, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Normal, color = p.text, maxLines = 1)
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
private fun WeatherDetail(weather: WeatherView, style: HomeStyle, onWeather: (() -> Unit)?, start: androidx.compose.ui.unit.Dp = 12.dp) {
    val p = style.palette()
    Row(
        Modifier
            .padding(start = start)
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
