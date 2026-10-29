package app.bedtime.ui.minimal

import androidx.compose.material.icons.filled.DateRange
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
import app.bedtime.data.ClockSize
import app.bedtime.data.HomeStyle
import app.bedtime.data.Module
import app.bedtime.data.QuoteRefresh
import app.bedtime.data.UseLook
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
    /** A habit in the habits widget, or not. */
    val habitShown: (habitId: String, shown: Boolean) -> Unit = { _, _ -> },
)

/**
 * A long press on a widget: hide it from this screen, and whatever can be changed from here: the
 * weather's style, the clock's size, how often the quote changes, a way to your habits or your
 * distracting apps.
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
                style.weatherStyleFor(screen),
            ) { handlers.change(style.withWeatherStyle(screen, it)) }
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
            Module.HABITS -> {
                if (habits.isNotEmpty()) {
                    Text(if (screen == app.bedtime.data.Screen.HOME) "Show on the home screen" else "Show on the lock screen", style = MaterialTheme.typography.labelLarge, color = p.faint, modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp))
                    habits.forEach { habit ->
                        Row(
                            Modifier.fillMaxWidth().clickable { handlers.habitShown(habit.id, !habit.shownOn(screen)) }.padding(horizontal = 22.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            app.bedtime.ui.components.VerbatimText(habit.name, style = MaterialTheme.typography.bodyLarge, color = if (habit.shownOn(screen)) p.text else p.faint, modifier = Modifier.weight(1f))
                            if (habit.shownOn(screen)) Icon(Icons.Default.Check, contentDescription = "shown", tint = p.accent, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Action(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Open your habits: add, change or remove", p) { handlers.open(app.bedtime.Routes.HABITS); onDone() }
            }
            Module.HOURS -> {
                val use = style.useWidgetFor(screen)
                fun set(changed: app.bedtime.data.UseWidget) = handlers.change(style.withUseWidget(screen, changed))
                choices(
                    "Look",
                    listOf("Dots" to UseLook.DOTS, "Line" to UseLook.LINE, "Budget" to UseLook.BUDGET, "Apps" to UseLook.APPS, "Apps in a line" to UseLook.APPS_LINE),
                    use.look,
                ) { look -> set(if (look == UseLook.LINE && !use.total && !use.distracting) use.copy(look = look, total = true, distracting = true) else use.copy(look = look)) }
                Text("Show", style = MaterialTheme.typography.labelLarge, color = p.faint, modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp))
                FlowRow(Modifier.padding(horizontal = 22.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (use.look == UseLook.DOTS || use.look == UseLook.LINE) {
                        Chip("Time on the phone", use.total, p) { set(use.copy(total = !use.total)) }
                        Chip("Distracting time", use.distracting, p) { set(use.copy(distracting = !use.distracting)) }
                    }
                    Chip("Unlocks", use.unlocks, p) { set(use.copy(unlocks = !use.unlocks)) }
                }
                Spacer(Modifier.padding(top = 8.dp))
                if (use.look == UseLook.BUDGET) {
                    choices("The budget counts", listOf("Distracting apps" to false, "The whole phone" to true), use.budgetWholePhone) { set(use.copy(budgetWholePhone = it)) }
                    val minutes = if (use.budgetWholePhone) app.bedtime.data.UseWords.PHONE_BUDGETS else app.bedtime.data.UseWords.DISTRACTING_BUDGETS
                    choices(
                        "A day's allowance",
                        minutes.map { app.bedtime.data.UseWords.compact(it) to it },
                        if (use.budgetWholePhone) use.phoneBudgetMinutes else use.budgetMinutes,
                    ) { set(if (use.budgetWholePhone) use.copy(phoneBudgetMinutes = it) else use.copy(budgetMinutes = it)) }
                }
                Action(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Open your statistics", p) { handlers.open(app.bedtime.Routes.STATS); onDone() }
                Action(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Choose your distracting apps", p) { handlers.open(app.bedtime.Routes.DISTRACTION); onDone() }
            }
            Module.EVENT -> {
                Action(
                    Icons.Default.DateRange,
                    if (style.calendarIcon(screen)) "No calendar icon when nothing's coming" else "A calendar icon when nothing's coming",
                    p,
                ) { handlers.change(style.withCalendarIcon(screen, !style.calendarIcon(screen))); onDone() }
                Action(
                    Icons.Default.DateRange,
                    if (style.calendarTomorrow) "Only today's events" else "Tomorrow's first event, once today's are done",
                    p,
                ) { handlers.change(style.copy(calendarTomorrow = !style.calendarTomorrow)); onDone() }
            }
            Module.NOTE -> {
                val chosen = style.noteWidgetId ?: app.bedtime.data.Notes.TODO_ID
                if (notes.any { it.id == chosen }) {
                    Action(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Open this note in full", p) { handlers.open(app.bedtime.Routes.note(chosen)); onDone() }
                }
                choices("Lines", listOf(1, 2, 3, 6, 10).map { it.toString() to it }, style.noteLines) { handlers.change(style.copy(noteLines = it)) }
                choices("Text", listOf("Small" to app.bedtime.data.TextSize.SMALL, "Medium" to app.bedtime.data.TextSize.MEDIUM, "Large" to app.bedtime.data.TextSize.LARGE), style.noteTextSize) { handlers.change(style.copy(noteTextSize = it)) }
                val fits = style.noteFitsBesideClock(screen)
                Action(
                    Icons.Default.DateRange,
                    when {
                        style.noteBeside(screen) && fits -> "In the list, not beside the clock"
                        fits -> "Beside the clock"
                        else -> "Beside the clock: needs a small clock, and no weather there"
                    },
                    p,
                    enabled = fits || style.noteBeside(screen),
                ) { handlers.change(style.withNoteBeside(screen, !style.noteBeside(screen))); onDone() }
                Text("Show", style = MaterialTheme.typography.labelLarge, color = p.faint, modifier = Modifier.padding(horizontal = 22.dp, vertical = 6.dp))
                Column(Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState())) {
                    notes.sortedBy { it.id != app.bedtime.data.Notes.TODO_ID }.forEach { note ->
                        val name = app.bedtime.data.NoteWidget.title(note.text).ifBlank { "A note" }
                        Row(
                            Modifier.fillMaxWidth().clickable { handlers.change(style.copy(noteWidgetId = note.id)); onDone() }.padding(horizontal = 22.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            app.bedtime.ui.components.VerbatimText(name, style = MaterialTheme.typography.bodyLarge, color = if (note.id == chosen) p.accent else p.text, modifier = Modifier.weight(1f))
                            if (note.id == chosen) Icon(Icons.Default.Check, contentDescription = "shown now", tint = p.accent, modifier = Modifier.size(18.dp))
                        }
                    }
                    if (notes.isEmpty()) Text("No notes yet. Write one in refuge notes.", style = MaterialTheme.typography.bodyMedium, color = p.faint, modifier = Modifier.padding(horizontal = 22.dp))
                }
            }
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
private fun Action(icon: ImageVector, label: String, p: HomePalette, enabled: Boolean = true, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick).padding(horizontal = 22.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = p.faint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (enabled) p.text else p.faint)
    }
}
