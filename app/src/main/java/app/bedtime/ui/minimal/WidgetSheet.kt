package app.bedtime.ui.minimal

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.bedtime.data.ClockSize
import app.bedtime.data.HomeStyle
import app.bedtime.data.Module
import app.bedtime.data.QuoteRefresh
import app.bedtime.data.WeatherStyle
import app.bedtime.ui.components.Text
import app.bedtime.ui.homestyle.HomePalette
import app.bedtime.ui.homestyle.palette

/** What a long press on a widget can change from where it is. */
class WidgetHandlers(
    /** Takes it off this screen (or off the running session's own list for it). */
    val hide: () -> Unit = {},
    /** Saves a change to how it looks, made right here. */
    val change: (HomeStyle) -> Unit = {},
    /** Opens digital refuge on one of its pages ([app.bedtime.Routes]). */
    val open: (String) -> Unit = {},
)

/**
 * A long press on a widget: hide it from this screen, and whatever can be changed from here: the
 * weather's style, the clock's size, how often the quote changes, a way to your habits or your
 * distracting apps.
 */
@Composable
internal fun WidgetSheet(module: Module, style: HomeStyle, sessionOwned: Boolean, handlers: WidgetHandlers, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) { WidgetSheetContent(module, style, sessionOwned, handlers, onDismiss) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WidgetSheetContent(module: Module, style: HomeStyle, sessionOwned: Boolean, handlers: WidgetHandlers, onDone: () -> Unit = {}) {
    val p = style.palette()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(p.background)
            .border(1.dp, p.faint.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
            .padding(vertical = 18.dp),
    ) {
        Text(module.label, style = MaterialTheme.typography.titleMedium, color = p.text, modifier = Modifier.padding(horizontal = 22.dp).padding(bottom = 6.dp))
        @Composable
        fun <T> choices(label: String, options: List<Pair<String, T>>, selected: T, pick: (T) -> Unit) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = p.faint, modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp))
            FlowRow(Modifier.padding(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                options.forEach { (name, value) -> Chip(name, value == selected, p) { pick(value) } }
            }
            Spacer(Modifier.padding(top = 8.dp))
        }
        when (module) {
            Module.WEATHER -> choices(
                "Show",
                listOf("Today" to WeatherStyle.TODAY, "3 days" to WeatherStyle.DAYS, "Now + 5" to WeatherStyle.WEEK, "5 days" to WeatherStyle.FIVE_DAYS),
                style.weatherStyle,
            ) { handlers.change(style.copy(weatherStyle = it)) }
            Module.CLOCK -> choices(
                "Size",
                listOf("Very small" to ClockSize.VERY_SMALL, "Small" to ClockSize.SMALL, "Medium" to ClockSize.MEDIUM, "Large" to ClockSize.LARGE),
                style.clockSize,
            ) { handlers.change(style.copy(clockSize = it)) }
            Module.QUOTE -> choices(
                "A new one every",
                listOf("Hour" to QuoteRefresh.HOURLY, "6 hours" to QuoteRefresh.SIX_HOURS, "Day" to QuoteRefresh.DAILY, "Week" to QuoteRefresh.WEEKLY),
                style.quoteRefresh,
            ) { handlers.change(style.copy(quoteRefresh = it)) }
            Module.HABITS -> Action(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Add or remove habits", p) { handlers.open(app.bedtime.Routes.HABITS); onDone() }
            Module.HOURS -> Action(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Choose your distracting apps", p) { handlers.open(app.bedtime.Routes.DISTRACTION); onDone() }
            else -> Unit
        }
        Action(Icons.Default.Clear, "Hide from this screen", p) { handlers.hide(); onDone() }
        Text(
            if (sessionOwned) "This session has its own widgets; hiding takes it off its list. " else "Bring it back from Widgets in digital refuge's settings.",
            style = MaterialTheme.typography.bodySmall,
            color = p.faint,
            modifier = Modifier.padding(horizontal = 22.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, p: HomePalette, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) p.text else p.faint,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) p.accent.copy(alpha = 0.22f) else p.background)
            .border(1.dp, if (selected) p.accent else p.faint.copy(alpha = 0.4f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun Action(icon: ImageVector, label: String, p: HomePalette, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = p.faint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = p.text)
    }
}
