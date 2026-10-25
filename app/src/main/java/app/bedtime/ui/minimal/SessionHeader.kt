package app.bedtime.ui.minimal

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
    /** "12° · light rain" after the date, with its sky, and the next days if chosen. */
    weather: WeatherView? = null,
) {
    val context = LocalContext.current
    val p = style.palette()
    val clock = style.clockSize.scale
    Column {
        if (style.showGreeting) {
            Text(greeting(now.hour), style = MaterialTheme.typography.titleMedium, color = p.accent)
        }
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
        if (style.showDate || weather != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (style.showDate) {
                    Text(
                        DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()).format(now) + if (weather != null) " · " else "",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Normal,
                        color = p.muted,
                    )
                }
                if (weather != null) {
                    weather.icon?.let { Icon(WeatherIcons.of(it), contentDescription = null, tint = p.muted, modifier = Modifier.padding(end = 6.dp).size(18.dp)) }
                    Text(weather.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Normal, color = p.muted)
                }
            }
        }
        if (weather != null && weather.days.isNotEmpty()) {
            Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                weather.days.forEach { day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            when (day.offset) {
                                0 -> "Today"
                                1 -> "Tomorrow"
                                else -> DateTimeFormatter.ofPattern("EEE", Locale.getDefault()).format(now.plusDays(day.offset.toLong()))
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = p.faint,
                        )
                        Box(Modifier.padding(vertical = 4.dp).size(24.dp), contentAlignment = Alignment.Center) {
                            day.icon?.let { Icon(WeatherIcons.of(it), contentDescription = null, tint = p.muted, modifier = Modifier.size(24.dp)) }
                        }
                        Text(
                            listOfNotNull(day.high?.let { "$it°" }, day.low?.let { "$it°" }).joinToString(" · "),
                            style = MaterialTheme.typography.labelMedium,
                            color = p.muted,
                        )
                    }
                }
            }
        }
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
