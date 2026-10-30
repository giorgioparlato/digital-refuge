package app.bedtime.ui.minimal

import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.Icons
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import app.bedtime.ui.components.WeatherIcons
import app.bedtime.data.WeatherView
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
    habits: (@Composable () -> Unit)? = null,
    nowPlaying: (@Composable () -> Unit)? = null,
    note: (@Composable () -> Unit)? = null,
    /** The note, small, beside the clock instead of in the list (only offered with a small clock and no weather there). */
    noteBeside: (@Composable () -> Unit)? = null,
    /** A long press on a widget: its own sheet, to hide or change it. */
    onHold: ((Module) -> Unit)? = null,
    /** Which screen this is, for the settings each has its own of. */
    screen: Screen = Screen.HOME,
    /** A tap on the clock (on the lock screen, the unlock, as anywhere there). */
    onClockTap: (() -> Unit)? = null,
    /** A double tap on the clock: its own gesture, or the screen's double tap. */
    onClockDoubleTap: (() -> Unit)? = null,
) {
    val hold: (Module) -> (() -> Unit)? = { m -> onHold?.let { { it(m) } } }
    // Long-pressing a widget that has no tap of its own.
    val held: (Module) -> Modifier = { m ->
        if (onHold == null) Modifier else Modifier.pointerInput(m) { detectTapGestures(onLongPress = { onHold(m) }) }
    }
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
    val pillAfter = modules.indexOfLast { it in Modules.CLOCK_GROUP }
    // The note beside the clock takes a column of its own on the right, beside the greeting, clock and
    // date at the top, with room between.
    val besideHead = if (noteBeside != null && Module.NOTE in modules) {
        modules.takeWhile { it == Module.GREETING || it == Module.CLOCK || it == Module.DATE }.size
    } else {
        0
    }
    val one: @Composable (Int, Module) -> Unit = { at, module ->
            when (module) {
                Module.GREETING -> Text(greeting(now.hour), style = MaterialTheme.typography.titleMedium, color = p.accent, modifier = held(Module.GREETING))
                Module.CLOCK -> Row(
                    if (onClockTap == null && onClockDoubleTap == null) {
                        held(Module.CLOCK)
                    } else {
                        Modifier.pointerInput(onClockTap, onClockDoubleTap, onHold) {
                            detectTapGestures(
                                onTap = onClockTap?.let { { _ -> it() } },
                                onDoubleTap = onClockDoubleTap?.let { { _ -> it() } },
                                onLongPress = onHold?.let { { _ -> it(Module.CLOCK) } },
                            )
                        }
                    },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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
                        WeatherBeside(weather, style, onWeather, hold(Module.WEATHER))
                    }
                }
                Module.DATE -> Row(held(Module.DATE), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()).format(now),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Normal,
                        color = p.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (weather != null && detailOnDate) WeatherDetail(weather, style, onWeather, onWeatherHold = hold(Module.WEATHER))
                }
                Module.WEATHER -> if (weather != null) {
                    val nowHere = hasNow && !clockShown
                    if (nowHere || (detail && !detailOnDate)) {
                        Row(Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (nowHere) WeatherInline(weather, style, onWeather, hold(Module.WEATHER))
                            if (detail && !detailOnDate) WeatherDetail(weather, style, onWeather, start = if (nowHere) 12.dp else 0.dp, onWeatherHold = hold(Module.WEATHER))
                        }
                    }
                    if (weather.week.isNotEmpty()) WeatherWeek(weather, style, now, onWeather, hold(Module.WEATHER))
                }
                // Nothing coming today: a small calendar icon still opens the week ahead, unless you'd rather not.
                Module.EVENT -> if (calendar == null && style.calendarIcon(screen) && (onCalendar != null || onHold != null)) {
                    Icon(
                        Icons.Default.DateRange,
                        contentDescription = "the week ahead",
                        tint = p.faint,
                        modifier = Modifier.padding(top = 6.dp).size(28.dp).clip(RoundedCornerShape(8.dp))
                            .tapAndHold("see the week ahead", onCalendar, hold(Module.EVENT)).padding(4.dp),
                    )
                } else if (calendar != null) {
                    Text(
                        calendar,
                        style = MaterialTheme.typography.bodyMedium,
                        color = p.muted,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .then(if (onCalendar != null || onHold != null) Modifier.tapAndHold("see the week ahead", onCalendar, hold(Module.EVENT)) else Modifier),
                    )
                }
                Module.HOURS -> hours?.let { Column(held(Module.HOURS)) { it() } }
                Module.QUOTE -> quote?.let { Column(held(Module.QUOTE)) { it() } }
                Module.HABITS -> habits?.let { Column(held(Module.HABITS)) { it() } }
                Module.NOW_PLAYING -> nowPlaying?.let { Column(held(Module.NOW_PLAYING)) { it() } }
                Module.NOTE -> if (besideHead == 0) note?.let { Column(held(Module.NOTE)) { it() } }
            }
            if (at == pillAfter && at >= besideHead) SessionPill(scheduleName, until, scheduleIcon, style)
    }
    Column {
        if (pillAfter < 0) SessionPill(scheduleName, until, scheduleIcon, style)
        if (besideHead > 0 && noteBeside != null) {
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1.3f)) { modules.take(besideHead).forEachIndexed { at, m -> one(at, m) } }
                Box(Modifier.weight(1f).padding(start = 24.dp, top = 6.dp)) { noteBeside() }
            }
            if (pillAfter in 0 until besideHead) SessionPill(scheduleName, until, scheduleIcon, style)
        }
        modules.drop(besideHead).forEachIndexed { i, m -> one(besideHead + i, m) }
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
private fun WeatherInline(weather: WeatherView, style: HomeStyle, onWeather: (() -> Unit)?, onWeatherHold: (() -> Unit)? = null) {
    val p = style.palette()
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .tapAndHold("open the weather", onWeather, onWeatherHold)
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
private fun WeatherBeside(weather: WeatherView, style: HomeStyle, onWeather: (() -> Unit)?, onWeatherHold: (() -> Unit)? = null) {
    val p = style.palette()
    val clock = style.clockSize.scale
    Column(
        Modifier
            .padding(start = 10.dp)
            .clip(RoundedCornerShape(14.dp))
            .tapAndHold("open the weather", onWeather, onWeatherHold)
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
private fun WeatherDetail(weather: WeatherView, style: HomeStyle, onWeather: (() -> Unit)?, start: androidx.compose.ui.unit.Dp = 12.dp, onWeatherHold: (() -> Unit)? = null) {
    val p = style.palette()
    Row(
        Modifier
            .padding(start = start)
            .clip(RoundedCornerShape(10.dp))
            .tapAndHold("open the weather", onWeather, onWeatherHold)
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
private fun WeatherWeek(weather: WeatherView, style: HomeStyle, now: LocalDateTime, onWeather: (() -> Unit)?, onWeatherHold: (() -> Unit)? = null) {
    val p = style.palette()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .tapAndHold("open the weather", onWeather, onWeatherHold)
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

/** A tap does [onTap] and a long press [onHold], either of which may be missing. */
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.tapAndHold(label: String, onTap: (() -> Unit)?, onHold: (() -> Unit)?): Modifier =
    if (onTap == null && onHold == null) this else combinedClickable(onClickLabel = label, onLongClick = onHold, onClick = onTap ?: {})
