package app.bedtime.ui.homestyle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.bedtime.Routes
import app.bedtime.data.ClockSize
import app.bedtime.data.Habit
import app.bedtime.data.HomeStyle
import app.bedtime.data.Module
import app.bedtime.data.Note
import app.bedtime.data.NoteWidget
import app.bedtime.data.Notes
import app.bedtime.data.QuoteRefresh
import app.bedtime.data.Screen
import app.bedtime.data.TempUnit
import app.bedtime.data.TextSize
import app.bedtime.data.UseLook
import app.bedtime.data.UseWidget
import app.bedtime.data.UseWords
import app.bedtime.data.WeatherStyle
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.ui.components.QuickChip
import app.bedtime.ui.components.SegmentedChoice
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.VerbatimText
import app.bedtime.ui.theme.Obsidian

/**
 * One thing a widget lets you change. Each widget's list is made once, by [WidgetOptions.of], and
 * shown the same, in the same order, on the Widgets page and when you hold the widget on a screen.
 */
sealed interface WidgetOption {
    val label: String

    /** One of a few. */
    class Choice<T>(override val label: String, val options: List<Pair<String, T>>, val selected: T?, val pick: (T) -> Unit) : WidgetOption

    /** On or off. */
    class Switch(
        override val label: String,
        val description: String? = null,
        val on: Boolean,
        val enabled: Boolean = true,
        val set: (Boolean) -> Unit,
    ) : WidgetOption

    /** A colour from a set, or [automatic] (null) where there is one. */
    class Colour(
        override val label: String,
        val presets: List<Pair<String, Long>>,
        val selected: Long?,
        val automatic: String?,
        val pick: (Long?) -> Unit,
    ) : WidgetOption

    /** One of a list, each on its own line, by id: the note it shows. */
    class Pick(override val label: String, val items: List<Pair<String, String>>, val selected: String?, val empty: String, val pick: (String) -> Unit) : WidgetOption

    /** Several of a list, each ticked or not, by id: the habits it shows. */
    class Ticks(override val label: String, val items: List<Triple<String, String, Boolean>>, val set: (id: String, on: Boolean) -> Unit) : WidgetOption

    /** A way to one of digital refuge's pages ([Routes]). */
    class Link(override val label: String, val route: String) : WidgetOption

    /** A line saying where it comes from or how it behaves; on the Widgets page, where there's room. */
    class Hint(override val label: String) : WidgetOption
}

object WidgetOptions {
    /**
     * [module]'s options on [screen], in the one order every widget follows: what it shows, then its
     * size, then its colours, then what a tap or a hold does. [style] is as [screen] shows it, and each
     * change is made to it and handed to [change].
     */
    fun of(
        module: Module,
        style: HomeStyle,
        screen: Screen,
        change: (HomeStyle) -> Unit,
        notes: List<Note> = emptyList(),
        habits: List<Habit> = emptyList(),
        habitShown: (id: String, shown: Boolean) -> Unit = { _, _ -> },
        weatherHeard: String? = null,
    ): List<WidgetOption> = when (module) {
        Module.CLOCK -> listOf(
            WidgetOption.Choice(
                "Size",
                listOf("Very small" to ClockSize.VERY_SMALL, "Small" to ClockSize.SMALL, "Medium" to ClockSize.MEDIUM, "Large" to ClockSize.LARGE),
                style.clockSize,
            ) { change(style.copy(clockSize = it)) },
        )
        Module.WEATHER -> listOf(
            WidgetOption.Hint(
                "From Breezy Weather, a free app that does the fetching; this one never goes online. " +
                    (weatherHeard ?: "Nothing from it yet."),
            ),
            WidgetOption.Choice(
                "Show",
                listOf("Today" to WeatherStyle.TODAY, "3 days" to WeatherStyle.DAYS, "Now + 5" to WeatherStyle.WEEK, "5 days" to WeatherStyle.FIVE_DAYS),
                style.weatherStyleFor(screen),
            ) { change(style.withWeatherStyle(screen, it)) },
            WidgetOption.Choice("In", listOf("Automatic" to TempUnit.AUTO, "°C" to TempUnit.CELSIUS, "°F" to TempUnit.FAHRENHEIT), style.weatherUnit) {
                change(style.copy(weatherUnit = it))
            },
            WidgetOption.Hint(
                "Install Breezy Weather (it's on F-Droid). In its settings, under widgets, open “Send Gadgetbridge data” and tick digital refuge. " +
                    "It sends the weather each time it updates.",
            ),
        )
        Module.EVENT -> listOfNotNull(
            WidgetOption.Hint("Read from the calendar already on this phone. Nothing is fetched."),
            WidgetOption.Switch("Tomorrow's first event, once today's are done", "In the evening, what the morning starts with", style.calendarTomorrow) {
                change(style.copy(calendarTomorrow = it))
            },
            if (screen == Screen.LOCK) {
                WidgetOption.Switch("Event names", "Off, the lock screen only says when the next one is", style.calendarNamesOnLock) {
                    change(style.copy(calendarNamesOnLock = it))
                }
            } else {
                null
            },
            WidgetOption.Switch("A calendar icon when nothing's coming", "A tap on it, or on the event, shows the week ahead", style.calendarIcon(screen)) {
                change(style.withCalendarIcon(screen, it))
            },
        )
        Module.QUOTE -> listOf(
            WidgetOption.Choice(
                "A new one every",
                listOf("Hour" to QuoteRefresh.HOURLY, "6 hours" to QuoteRefresh.SIX_HOURS, "Day" to QuoteRefresh.DAILY, "Week" to QuoteRefresh.WEEKLY),
                style.quoteRefresh,
            ) { change(style.copy(quoteRefresh = it)) },
            WidgetOption.Link("Your quotes: add your own, pick favourites, hide some", Routes.QUOTES),
            WidgetOption.Hint("Tap it for another; hold it to favourite or hide it."),
        )
        Module.NOTE -> {
            val chosen = style.noteWidgetId ?: Notes.TODO_ID
            val fits = style.noteFitsBesideClock(screen)
            listOfNotNull(
                WidgetOption.Pick(
                    "Show",
                    notes.sortedBy { it.id != Notes.TODO_ID }.map { it.id to NoteWidget.title(it.text).ifBlank { "A note" } },
                    chosen,
                    empty = "No notes yet. Write one in refuge notes.",
                ) { change(style.copy(noteWidgetId = it)) },
                WidgetOption.Choice("Lines shown", listOf(1, 2, 3, 6, 10).map { it.toString() to it }, style.noteLines) { change(style.copy(noteLines = it)) },
                WidgetOption.Switch(
                    "Beside the clock",
                    if (fits) "Small, at the top right: its title and first lines" else "Only with a small or very small clock, and the weather not beside it (off, or set to 5 days)",
                    style.noteBeside(screen) && fits,
                    enabled = fits || style.noteBeside(screen),
                ) { change(style.withNoteBeside(screen, it)) },
                WidgetOption.Choice("Text size", listOf("Small" to TextSize.SMALL, "Medium" to TextSize.MEDIUM, "Large" to TextSize.LARGE), style.noteTextSize) {
                    change(style.copy(noteTextSize = it))
                },
                WidgetOption.Colour("Its background", NotePresets, style.noteBackground, automatic = "The screen's own") { change(style.copy(noteBackground = it)) },
                if (style.noteBackground != null) {
                    WidgetOption.Choice("Its text", listOf("To suit it" to null, "Dark" to true, "Light" to false), style.noteDarkText) {
                        change(style.copy(noteDarkText = it))
                    }
                } else {
                    null
                },
                if (notes.any { it.id == chosen }) WidgetOption.Link("Open this note in full", Routes.note(chosen)) else null,
                WidgetOption.Hint("Tap its lines to write right there."),
            )
        }
        Module.HOURS -> {
            val use = style.useWidgetFor(screen)
            fun set(changed: UseWidget) = change(style.withUseWidget(screen, changed))
            val figures = use.look == UseLook.DOTS || use.look == UseLook.LINE
            val budgets = if (use.budgetWholePhone) UseWords.PHONE_BUDGETS else UseWords.DISTRACTING_BUDGETS
            listOfNotNull(
                WidgetOption.Choice(
                    "Look",
                    listOf("Dots" to UseLook.DOTS, "Line" to UseLook.LINE, "Budget" to UseLook.BUDGET, "Apps" to UseLook.APPS, "Apps in a line" to UseLook.APPS_LINE),
                    use.look,
                ) { look ->
                    // A line with nothing chosen would only say the distracting time; start it with both.
                    set(if (look == UseLook.LINE && !use.total && !use.distracting) use.copy(look = look, total = true, distracting = true) else use.copy(look = look))
                },
                if (figures) WidgetOption.Switch("Time on the phone", "All of today, in any app", use.total) { set(use.copy(total = it)) } else null,
                if (figures) WidgetOption.Switch("Time in distracting apps", "Today so far", use.distracting) { set(use.copy(distracting = it)) } else null,
                WidgetOption.Switch("Unlocks", "How many times today; refuge counts them itself", use.unlocks) { set(use.copy(unlocks = it)) },
                if (use.look == UseLook.BUDGET) {
                    WidgetOption.Choice("The budget counts", listOf("Distracting apps" to false, "The whole phone" to true), use.budgetWholePhone) {
                        set(use.copy(budgetWholePhone = it))
                    }
                } else {
                    null
                },
                if (use.look == UseLook.BUDGET) {
                    WidgetOption.Choice("A day's allowance", budgets.map { UseWords.compact(it) to it }, if (use.budgetWholePhone) use.phoneBudgetMinutes else use.budgetMinutes) {
                        set(if (use.budgetWholePhone) use.copy(phoneBudgetMinutes = it) else use.copy(budgetMinutes = it))
                    }
                } else {
                    null
                },
                WidgetOption.Colour("Calm hours", TextPresets, style.hourCalmColor, automatic = "Automatic") { change(style.copy(hourCalmColor = it)) },
                WidgetOption.Colour("Distracted hours", AccentPresets + DistractedPresets, style.hourDistractedColor, automatic = "Accent") {
                    change(style.copy(hourDistractedColor = it))
                },
                WidgetOption.Link("Choose your distracting apps", Routes.DISTRACTION),
                WidgetOption.Link("Open your statistics", Routes.STATS),
                WidgetOption.Hint("Tap it for today's detail."),
            )
        }
        Module.HABITS -> listOf(
            WidgetOption.Ticks("Show", habits.map { Triple(it.id, it.name, it.shownOn(screen)) }, habitShown),
            WidgetOption.Link("Your habits: add, change or remove", Routes.HABITS),
        )
        Module.GREETING, Module.DATE, Module.NOW_PLAYING -> emptyList()
    }
}

/** A widget's options on the Widgets page, in settings' own look. Links open through [onOpen]. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WidgetOptionsPage(options: List<WidgetOption>, onOpen: (String) -> Unit) {
    val c = Obsidian.colors
    Column {
        options.forEach { option ->
            when (option) {
                is WidgetOption.Hint -> Text(option.label, style = MaterialTheme.typography.bodySmall, color = c.textFaint, modifier = Modifier.padding(bottom = 10.dp))
                is WidgetOption.Choice<*> -> {
                    OptionLabel(option.label)
                    @Suppress("UNCHECKED_CAST")
                    val choice = option as WidgetOption.Choice<Any?>
                    val index = choice.options.indexOfFirst { it.second == choice.selected }
                    if (choice.options.size <= 5 && choice.options.all { it.first.length <= 10 }) {
                        SegmentedChoice(choice.options.map { it.first }, index.coerceAtLeast(0), { choice.pick(choice.options[it].second) })
                    } else {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            choice.options.forEachIndexed { i, (name, value) -> QuickChip(name, i == index) { choice.pick(value) } }
                        }
                    }
                    Spacer12()
                }
                is WidgetOption.Switch -> Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text(option.label, style = MaterialTheme.typography.bodyLarge, color = if (option.enabled) c.textNormal else c.textFaint)
                        option.description?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.textFaint) }
                    }
                    ObsidianToggle(option.on, option.set, enabled = option.enabled)
                }
                is WidgetOption.Colour -> {
                    OptionLabel(option.label)
                    ColourChoice(option.presets, option.selected, option.pick, automatic = option.automatic)
                    Spacer12()
                }
                is WidgetOption.Pick -> {
                    OptionLabel(option.label)
                    if (option.items.isEmpty()) Text(option.empty, style = MaterialTheme.typography.bodyMedium, color = c.textFaint)
                    option.items.forEach { (id, name) ->
                        PickRow(name, id == option.selected) { option.pick(id) }
                    }
                    Spacer12()
                }
                is WidgetOption.Ticks -> {
                    OptionLabel(option.label)
                    if (option.items.isEmpty()) Text("No habits yet.", style = MaterialTheme.typography.bodyMedium, color = c.textFaint)
                    option.items.forEach { (id, name, on) -> PickRow(name, on) { option.set(id, !on) } }
                    Spacer12()
                }
                is WidgetOption.Link -> Row(
                    Modifier.fillMaxWidth().clickable { onOpen(option.route) }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(option.label, style = MaterialTheme.typography.bodyLarge, color = c.accentText, modifier = Modifier.weight(1f))
                    Chevron()
                }
            }
        }
    }
}

@Composable
private fun OptionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = Obsidian.colors.textMuted, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
}

@Composable
private fun Spacer12() = androidx.compose.foundation.layout.Spacer(Modifier.padding(top = 12.dp))

@Composable
private fun PickRow(name: String, chosen: Boolean, onClick: () -> Unit) {
    val c = Obsidian.colors
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        VerbatimText(name, style = MaterialTheme.typography.bodyLarge, color = if (chosen) c.accentText else c.textNormal, modifier = Modifier.weight(1f))
        if (chosen) Icon(Icons.Default.Check, contentDescription = "chosen", tint = c.accent, modifier = Modifier.size(18.dp))
    }
}
