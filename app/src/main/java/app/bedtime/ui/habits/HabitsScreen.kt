package app.bedtime.ui.habits

import java.util.Locale
import java.time.format.DateTimeFormatter
import java.time.LocalDate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.compose.material3.Icon
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import app.bedtime.ui.components.CardRadius
import app.bedtime.data.HabitDay
import app.bedtime.ui.homestyle.AccentPresets
import app.bedtime.ui.homestyle.ColourChoice
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.bedtime.data.Habit
import app.bedtime.data.HabitFrequency
import app.bedtime.data.HabitKind
import app.bedtime.data.Habits
import app.bedtime.data.HourlyUse
import app.bedtime.data.Repository
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.DragList
import app.bedtime.ui.components.NumberStepper
import app.bedtime.ui.components.ObsidianTextField
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.OptionRow
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.QuickChip
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.SegmentedChoice
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.launch

/** Your habits: tick today, open one for its month and strength, add and change them. */
@Composable
fun HabitsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val (habits, marks) = rememberHabits()
    HabitsContent(
        habits = habits,
        marks = marks,
        today = HourlyUse.today(),
        onBack = onBack,
        onMark = { habit, day, value -> scope.launch { repo.markHabit(habit.id, day, value) } },
        onSave = { habit ->
            scope.launch {
                repo.updateSettings { s ->
                    s.copy(habits = if (s.habits.any { it.id == habit.id }) s.habits.map { if (it.id == habit.id) habit else it } else s.habits + habit)
                }
            }
        },
        onMove = { moved -> scope.launch { repo.updateSettings { it.copy(habits = moved) } } },
        onDelete = { habit -> scope.launch { repo.deleteHabit(habit.id) } },
    )
}

@Composable
internal fun HabitsContent(
    habits: List<Habit>,
    marks: Map<String, Map<Long, Int>>,
    today: Long,
    onBack: () -> Unit,
    onMark: (Habit, Long, Int) -> Unit = { _, _, _ -> },
    onSave: (Habit) -> Unit = {},
    onMove: (List<Habit>) -> Unit = {},
    onDelete: (Habit) -> Unit = {},
) {
    val c = Obsidian.colors
    var editing by remember { mutableStateOf<Habit?>(null) }
    var open by remember { mutableStateOf<String?>(null) }
    editing?.let { habit ->
        HabitEditDialog(
            habit = habit,
            isNew = habits.none { it.id == habit.id },
            onDismiss = { editing = null },
            onSave = { onSave(it); editing = null },
            onDelete = { onDelete(habit); editing = null; open = null },
        )
    }
    habits.firstOrNull { it.id == open }?.let { habit ->
        Dialog(onDismissRequest = { open = null }) {
            HabitDetailContent(
                habit, marks[habit.id].orEmpty(), today, c.bgSecondary, c.textNormal, c.textMuted, c.accent,
                onSet = { day, value -> onMark(habit, day, value) },
                onEdit = { editing = habit },
            )
        }
    }
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Habits", onBack = onBack, subtitle = "Kept on this phone") }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (habits.isEmpty()) {
                Text(
                    "Habits you want to keep, ticked off each day: daily, a few times a week, or a count like pages or reps. Add one to start; your home and lock screen can show them too.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textMuted,
                )
            } else {
                Text("Today", style = MaterialTheme.typography.titleLarge, color = c.textNormal)
            }
            habits.forEach { habit ->
                HabitCard(habit, marks[habit.id].orEmpty(), today, onTick = {
                    onMark(habit, today, Habits.tapped(habit, marks[habit.id]?.get(today) ?: 0))
                }, onOpen = { open = habit.id })
            }
            if (habits.isNotEmpty()) WeekStrip(habits, marks, today)
            SectionCard {
                OptionRow(Icons.Default.Add, "Add a habit", onClick = { editing = Habit() }, trailing = { Chevron() })
            }
            if (habits.size > 1) {
                SectionCard(title = "Order", subtitle = "As they show here and in the widgets") {
                    DragList(habits, key = { it.id }, onMoved = onMove) { habit ->
                        Column(Modifier.weight(1f).padding(horizontal = 8.dp).clip(RoundedCornerShape(8.dp)).clickable { editing = habit }) {
                            Text(habit.name, style = MaterialTheme.typography.bodyLarge, color = c.textNormal)
                            Text(Habits.describe(habit), style = MaterialTheme.typography.bodySmall, color = c.textFaint)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * A habit as a card: the circle to tick today (or count one more), its name and how often, its strength
 * and run, and the week as dots, in its colour. A tap anywhere else opens it.
 */
@Composable
private fun HabitCard(habit: Habit, marks: Map<Long, Int>, today: Long, onTick: () -> Unit, onOpen: () -> Unit) {
    val c = Obsidian.colors
    val tint = habit.tint(c.accent)
    val state = Habits.day(habit, marks, today)
    val kept = state == HabitDay.DONE || state == HabitDay.MET
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(CardRadius)).background(c.bgSecondary).clickable(onClickLabel = "open ${habit.name}", onClick = onOpen).padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (state == HabitDay.DONE) tint else if (state == HabitDay.PARTIAL) tint.copy(alpha = 0.35f) else Color.Transparent)
                    .border(2.dp, if (kept || state == HabitDay.PARTIAL) tint else c.textFaint, CircleShape)
                    .clickable(onClickLabel = if (habit.kind == HabitKind.COUNT) "one more" else "tick ${habit.name}", onClick = onTick),
                contentAlignment = Alignment.Center,
            ) {
                if (state == HabitDay.DONE) Icon(Icons.Default.Check, contentDescription = null, tint = c.bgPrimary, modifier = Modifier.size(20.dp))
                else if (habit.kind == HabitKind.COUNT) Text("+", style = MaterialTheme.typography.titleMedium, color = c.textMuted)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.bodyLarge, color = c.textNormal)
                Text(
                    if (habit.kind == HabitKind.COUNT) "${marks[today] ?: 0} of ${habit.target}${if (habit.unit.isBlank()) "" else " ${habit.unit}"} today" else Habits.describe(habit),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textFaint,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${Habits.strength(habit, marks, today)}%", style = MaterialTheme.typography.titleMedium, color = tint)
                Text("${Habits.streak(habit, marks, today)} in a row", style = MaterialTheme.typography.labelSmall, color = c.textFaint)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            ((today - 6)..today).forEach { day ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    HabitDot(Habits.day(habit, marks, day), day == today, c.textNormal, tint, 22)
                    Text(
                        DateTimeFormatter.ofPattern("EEEEE", Locale.getDefault()).format(LocalDate.ofEpochDay(day)),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (day == today) c.textNormal else c.textFaint,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }
    }
}

/** The week at a glance: a square a day, as full as the share of habits kept that day. */
@Composable
private fun WeekStrip(habits: List<Habit>, marks: Map<String, Map<Long, Int>>, today: Long) {
    val c = Obsidian.colors
    Text("This week", style = MaterialTheme.typography.labelLarge, color = c.textMuted, modifier = Modifier.padding(top = 4.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ((today - 6)..today).forEach { day ->
            val share = habits.count { h ->
                Habits.day(h, marks[h.id].orEmpty(), day).let { it == HabitDay.DONE || it == HabitDay.MET }
            }.toFloat() / habits.size
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(9.dp)).background(c.accent.copy(alpha = 0.1f + 0.85f * share)))
                Text(
                    DateTimeFormatter.ofPattern("EEEEE", Locale.getDefault()).format(LocalDate.ofEpochDay(day)),
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textFaint,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}

/** A habit's name, its colour, whether it's done-or-not or a count, and how often. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HabitEditDialog(habit: Habit, isNew: Boolean, onDismiss: () -> Unit, onSave: (Habit) -> Unit, onDelete: () -> Unit) {
    val c = Obsidian.colors
    var draft by remember { mutableStateOf(habit) }
    var frequency by remember { mutableStateOf(HabitFrequency.of(habit)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.bgSecondary,
        title = { Text(if (isNew) "A new habit" else "Change it") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ObsidianTextField(draft.name, { draft = draft.copy(name = it) }, placeholder = "Walk, read, stretch…")
                SegmentedChoice(
                    options = listOf("Yes or no", "A count"),
                    selected = draft.kind.ordinal,
                    onSelect = { draft = draft.copy(kind = HabitKind.entries[it]) },
                )
                if (draft.kind == HabitKind.YES_NO) {
                    Text("How often", style = MaterialTheme.typography.labelLarge, color = c.textMuted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        HabitFrequency.entries.forEach { f ->
                            QuickChip(f.label, selected = frequency == f) {
                                frequency = f
                                draft = Habits.withFrequency(draft, f, if (f == HabitFrequency.EVERY) 2 else if (f == HabitFrequency.WEEK) 3 else 10)
                            }
                        }
                    }
                    when (frequency) {
                        HabitFrequency.DAILY -> Unit
                        HabitFrequency.EVERY -> NumberStepper(draft.days, { draft = Habits.withFrequency(draft, frequency, it) }, 2..30, title = "days apart", format = { "every $it days" })
                        HabitFrequency.WEEK -> NumberStepper(draft.times, { draft = Habits.withFrequency(draft, frequency, it) }, 1..6, title = "times", format = { "$it a week" })
                        HabitFrequency.MONTH -> NumberStepper(draft.times, { draft = Habits.withFrequency(draft, frequency, it) }, 1..29, title = "times", format = { "$it a month" })
                    }
                } else {
                    Text("A day's aim", style = MaterialTheme.typography.labelLarge, color = c.textMuted)
                    NumberStepper(draft.target, { draft = draft.copy(target = it, times = 1, days = 1) }, 1..1000, title = "aim", presets = listOf(5, 8, 10, 20, 30, 50, 100))
                    ObsidianTextField(draft.unit, { draft = draft.copy(unit = it.take(20)) }, placeholder = "reps, pages, glasses…")
                }
                Text("Its colour", style = MaterialTheme.typography.labelLarge, color = c.textMuted)
                ColourChoice(HabitColours, draft.color, { draft = draft.copy(color = it) }, automatic = "Accent")
                if (!isNew) {
                    Text(
                        "Delete this habit",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.textFaint,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onDelete).padding(vertical = 6.dp),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(draft.copy(name = draft.name.trim())) }, enabled = draft.name.isNotBlank()) {
                Text("Save", color = if (draft.name.isNotBlank()) c.accentText else c.textFaint)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.textMuted) } },
    )
}

private val HabitColours: List<Pair<String, Long>> = AccentPresets + listOf("Ember" to 0xFFD9604AL, "Coral" to 0xFFE8875BL, "Gold" to 0xFFD4C04FL)
