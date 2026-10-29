package app.bedtime.ui.minimal

import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.heightIn
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
import app.bedtime.data.HomeStyle
import app.bedtime.data.Module
import app.bedtime.ui.components.Text
import app.bedtime.ui.homestyle.HomePalette
import app.bedtime.ui.homestyle.palette
import app.bedtime.ui.homestyle.WidgetOption
import app.bedtime.ui.homestyle.WidgetOptions

/** What a long press on a widget can change from where it is. */
class WidgetHandlers(
    /** Takes it off this screen (or off the running session's own list for it). */
    val hide: () -> Unit = {},
    /** Saves a change to how it looks, made right here. */
    val change: (HomeStyle) -> Unit = {},
    /** Opens digital refuge on one of its pages ([app.bedtime.Routes]). */
    val open: (String) -> Unit = {},
    /** A habit in the habits widget, or not. */
    val habitShown: (habitId: String, shown: Boolean) -> Unit = { _, _ -> },
)

/**
 * A long press on a widget: its options, the same as on the Widgets page ([WidgetOptions]), and a way
 * to hide it from this screen.
 */
@Composable
internal fun WidgetSheet(
    module: Module,
    style: HomeStyle,
    sessionOwned: Boolean,
    handlers: WidgetHandlers,
    onDismiss: () -> Unit,
    notes: List<app.bedtime.data.Note> = emptyList(),
    screen: app.bedtime.data.Screen = app.bedtime.data.Screen.HOME,
    habits: List<app.bedtime.data.Habit> = emptyList(),
) {
    Dialog(onDismissRequest = onDismiss) { WidgetSheetContent(module, style, sessionOwned, handlers, onDismiss, notes, screen, habits) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WidgetSheetContent(
    module: Module,
    style: HomeStyle,
    sessionOwned: Boolean,
    handlers: WidgetHandlers,
    onDone: () -> Unit = {},
    /** Your refuge notes, for the note widget to show one of. */
    notes: List<app.bedtime.data.Note> = emptyList(),
    screen: app.bedtime.data.Screen = app.bedtime.data.Screen.HOME,
    /** Your habits, to choose which the widget shows. */
    habits: List<app.bedtime.data.Habit> = emptyList(),
) {
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
        // The same options, in the same order, as on the Widgets page; hints are left to that page.
        val options = WidgetOptions.of(module, style, screen, handlers.change, notes, habits, handlers.habitShown)
        Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
            options.forEach { option ->
                when (option) {
                    is WidgetOption.Hint -> Unit
                    is WidgetOption.Choice<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        val choice = option as WidgetOption.Choice<Any?>
                        Label(choice.label, p)
                        FlowRow(Modifier.padding(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            choice.options.forEach { (name, value) -> Chip(name, value == choice.selected, p) { choice.pick(value) } }
                        }
                        Spacer(Modifier.padding(top = 8.dp))
                    }
                    is WidgetOption.Switch -> Ticked(option.label, option.on, p, enabled = option.enabled) { option.set(!option.on) }
                    is WidgetOption.Colour -> {
                        Label(option.label, p)
                        val custom = option.selected != null && option.presets.none { it.second == option.selected }
                        FlowRow(Modifier.padding(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            option.automatic?.let { Chip(it, option.selected == null, p) { option.pick(null) } }
                            option.presets.forEach { (name, value) -> Chip(name, value == option.selected, p) { option.pick(value) } }
                            if (custom) Chip("Custom", true, p) {}
                        }
                        Spacer(Modifier.padding(top = 8.dp))
                    }
                    is WidgetOption.Pick -> {
                        Label(option.label, p)
                        if (option.items.isEmpty()) Text(option.empty, style = MaterialTheme.typography.bodyMedium, color = p.faint, modifier = Modifier.padding(horizontal = 22.dp))
                        option.items.forEach { (id, name) -> Ticked(name, id == option.selected, p, verbatim = true) { option.pick(id); onDone() } }
                    }
                    is WidgetOption.Ticks -> {
                        if (option.items.isNotEmpty()) Label(if (screen == app.bedtime.data.Screen.HOME) "Show on the home screen" else "Show on the lock screen", p)
                        option.items.forEach { (id, name, on) -> Ticked(name, on, p, verbatim = true) { option.set(id, !on) } }
                    }
                    is WidgetOption.Link -> Action(Icons.AutoMirrored.Filled.KeyboardArrowRight, option.label, p) { handlers.open(option.route); onDone() }
                }
            }
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
private fun Label(text: String, p: HomePalette) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = p.faint, modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp))
}

/** A line that's on or off, with a tick when it's on. */
@Composable
private fun Ticked(label: String, on: Boolean, p: HomePalette, enabled: Boolean = true, verbatim: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(horizontal = 22.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val colour = if (on && enabled) p.text else p.faint
        if (verbatim) {
            app.bedtime.ui.components.VerbatimText(label, style = MaterialTheme.typography.bodyLarge, color = colour, modifier = Modifier.weight(1f))
        } else {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = colour, modifier = Modifier.weight(1f))
        }
        if (on) Icon(Icons.Default.Check, contentDescription = "on", tint = p.accent, modifier = Modifier.size(18.dp))
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
private fun Action(icon: ImageVector, label: String, p: HomePalette, enabled: Boolean = true, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(horizontal = 22.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = p.faint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (enabled) p.text else p.faint)
    }
}
