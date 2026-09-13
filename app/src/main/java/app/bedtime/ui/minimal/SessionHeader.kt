package app.bedtime.ui.minimal

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
internal fun SessionHeader(now: LocalDateTime, scheduleName: String, until: String, style: HomeStyle) {
    val context = LocalContext.current
    val p = style.palette()
    val scale = style.textSize.scale
    Column {
        if (style.showGreeting) {
            Text(greeting(now.hour), style = MaterialTheme.typography.titleMedium, color = p.accent)
        }
        Text(
            styledTime(timeFormatter(context).format(now), suffixSize = (30 * scale).sp),
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = (76 * scale).sp,
                lineHeight = (84 * scale).sp,
                fontFeatureSettings = "tnum",
            ),
            color = p.text,
            maxLines = 1,
        )
        if (style.showDate) {
            Text(
                DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault()).format(now),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Normal,
                color = p.muted,
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
