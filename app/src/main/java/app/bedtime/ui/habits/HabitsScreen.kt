package app.bedtime.ui.habits

import androidx.compose.ui.text.font.FontWeight
import app.bedtime.ui.components.LineIcons
import app.bedtime.data.Screen
import androidx.compose.material.icons.filled.Lock
import app.bedtime.ui.components.ObsidianToggle
import androidx.compose.material.icons.filled.Home
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.filled.CheckCircle
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
import app.bedtime.data.HabitStep
import androidx.compose.material.icons.filled.Close
import app.bedtime.data.Habits
import app.bedtime.data.HourlyUse
import app.bedtime.data.Repository
import app.bedtime.ui.components.DragList
import app.bedtime.ui.components.NumberStepper
import app.bedtime.ui.components.ObsidianTextField
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.AddCircleButton
import app.bedtime.ui.components.OptionRow
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.QuickChip
import app.bedtime.ui.components.SegmentedChoice
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.launch

/** Your habits: tick today, open one for its month and strength, add and change them. */
@Composable
fun HabitsScreen(onBack: (() -> Unit)?) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val (habits, marks) = rememberHabits()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    HabitsContent(
        habitsTab = settings?.habitsTab == true,
        onHabitsTab = { on -> scope.launch { repo.updateSettings { it.copy(habitsTab = on) } } },
        weekShown = settings?.habitsWeekShown != false,
        onWeekShown = { on -> scope.launch { repo.updateSettings { it.copy(habitsWeekShown = on) } } },
        view = settings?.habitsView ?: app.bedtime.data.HabitsView.GRID,
        onView = { v -> scope.launch { repo.updateSettings { it.copy(habitsView = v) } } },
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
    onBack: (() -> Unit)?,
    onMark: (Habit, Long, Int) -> Unit = { _, _, _ -> },
    onSave: (Habit) -> Unit = {},
    onMove: (List<Habit>) -> Unit = {},
    onDelete: (Habit) -> Unit = {},
    /** Habits as a tab of their own at refuge's foot, or not. */
    habitsTab: Boolean = false,
    onHabitsTab: ((Boolean) -> Unit)? = null,
    weekShown: Boolean = true,
    onWeekShown: (Boolean) -> Unit = {},
    view: app.bedtime.data.HabitsView = app.bedtime.data.HabitsView.GRID,
    onView: (app.bedtime.data.HabitsView) -> Unit = {},
) {
    val c = Obsidian.colors
    var editing by remember { mutableStateOf<Habit?>(null) }
    Scaffold(
        containerColor = c.bgPrimary,
        topBar = { ObsidianTopBar("Habits", onBack = onBack, subtitle = "Kept on this phone") },
        floatingActionButton = { AddCircleButton("add a habit", onClick = { editing = Habit() }) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HabitsBody(habits, marks, today, editing, { editing = it }, onMark, onSave, onMove, onDelete, weekShown, onWeekShown, view, onView)
            // At the foot, once they're set up: a tab of their own, as notes can have.
            onHabitsTab?.let { set ->
                app.bedtime.ui.components.SectionCard {
                    app.bedtime.ui.components.OptionRow(LineIcons.CheckCircle, "A habits tab", description = "Your habits at refuge's foot, beside focus and insights") {
                        app.bedtime.ui.components.ObsidianToggle(habitsTab, set)
                    }
                }
            }
            Spacer(Modifier.height(96.dp))
        }
    }
}

/**
 * Your habits, as on their page and in insights: today's cards, held and dragged to reorder, and the
 * week told plainly. [editing] is the habit being added or changed, if any; the round + button that
 * starts a new one belongs to the page around this.
 */
@Composable
internal fun HabitsBody(
    habits: List<Habit>,
    marks: Map<String, Map<Long, Int>>,
    today: Long,
    editing: Habit?,
    onEditing: (Habit?) -> Unit,
    onMark: (Habit, Long, Int) -> Unit = { _, _, _ -> },
    onSave: (Habit) -> Unit = {},
    onMove: (List<Habit>) -> Unit = {},
    onDelete: (Habit) -> Unit = {},
    weekShown: Boolean = true,
    onWeekShown: (Boolean) -> Unit = {},
    view: app.bedtime.data.HabitsView = app.bedtime.data.HabitsView.GRID,
    onView: (app.bedtime.data.HabitsView) -> Unit = {},
) {
    val c = Obsidian.colors
    var open by remember { mutableStateOf<String?>(null) }
    editing?.let { habit ->
        HabitEditDialog(
            habit = habit,
            isNew = habits.none { it.id == habit.id },
            onDismiss = { onEditing(null) },
            onSave = { onSave(it); onEditing(null) },
            onDelete = { onDelete(habit); onEditing(null); open = null },
        )
    }
    habits.firstOrNull { it.id == open }?.let { habit ->
        Dialog(onDismissRequest = { open = null }) {
            HabitDetailContent(
                habit, marks[habit.id].orEmpty(), today, c.bgSecondary, c.textNormal, c.textMuted, c.accent,
                onSet = { day, value -> onMark(habit, day, value) },
                onEdit = { onEditing(habit) },
            )
        }
    }
    if (habits.isEmpty()) {
        Text(
            "Habits you want to keep, ticked off each day: daily, a few times a week, or a count like pages or reps. Add one with the + below; your home and lock screen can show them too.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.textMuted,
        )
        return
    }
    val grid = view == app.bedtime.data.HabitsView.GRID
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("Today", style = MaterialTheme.typography.titleLarge, color = c.textNormal, modifier = Modifier.weight(1f))
        // The other look, a tap away: the week as a grid, or a compact row each.
        Box(
            Modifier.size(44.dp).clip(CircleShape)
                .clickable(onClickLabel = if (grid) "show as rows" else "show as a week grid") {
                    onView(if (grid) app.bedtime.data.HabitsView.ROWS else app.bedtime.data.HabitsView.GRID)
                },
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (grid) LineIcons.Menu else app.bedtime.ui.components.BedtimeIcons.Grid, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(20.dp))
        }
    }
    if (habits.size > 1) Text("Hold one and drag it to change the order", style = MaterialTheme.typography.bodySmall, color = c.textFaint)
    val tick = { habit: Habit -> onMark(habit, today, Habits.tapped(habit, marks[habit.id]?.get(today) ?: 0)) }
    val step = { habit: Habit, s: HabitStep -> onMark(habit, today, Habits.stepTapped(marks[habit.id]?.get(today) ?: 0, s)) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(CardRadius)).background(c.bgSecondary).padding(vertical = 6.dp)) {
        if (grid) WeekHeader(today)
        DragList(habits, key = { it.id }, onMoved = onMove, wholeRow = true) { habit ->
            Box(Modifier.weight(1f)) {
                if (grid) {
                    HabitGridRow(
                        habit, marks[habit.id].orEmpty(), today,
                        onDay = { day ->
                            // Counts and sets open, to change the right number or step; the rest tick in place.
                            if (habit.isSet || habit.kind == HabitKind.COUNT) {
                                open = habit.id
                            } else {
                                onMark(habit, day, Habits.tapped(habit, marks[habit.id]?.get(day) ?: 0))
                            }
                        },
                        onOpen = { open = habit.id },
                    )
                } else {
                    HabitRow(habit, marks[habit.id].orEmpty(), today, onTick = { tick(habit) }, onStep = { step(habit, it) }, onOpen = { open = habit.id })
                }
            }
        }
    }
    // How the week went, under the habits: ticking comes first.
    WeekSummary(habits, marks, today, weekShown, onWeekShown)
}

/** The grid's days along the top: the last seven, today brightest. */
@Composable
private fun WeekHeader(today: Long) {
    val c = Obsidian.colors
    Row(Modifier.fillMaxWidth().padding(start = 14.dp, end = 10.dp, top = 6.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("the last seven days", style = MaterialTheme.typography.labelSmall, color = c.textMuted, modifier = Modifier.weight(1f))
        ((today - 6)..today).forEach { day ->
            Text(
                DateTimeFormatter.ofPattern("EEEEE", Locale.getDefault()).format(LocalDate.ofEpochDay(day)),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (day == today) FontWeight.SemiBold else FontWeight.Normal,
                color = if (day == today) c.textNormal else c.textMuted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.width(34.dp),
            )
        }
    }
}

/**
 * A habit as one row of the week grid: its colour and name (a tap opens it), then a square a day,
 * filled when done, half when part done, a ring when already covered; today's is outlined.
 */
@Composable
private fun HabitGridRow(habit: Habit, marks: Map<Long, Int>, today: Long, onDay: (Long) -> Unit, onOpen: () -> Unit) {
    val c = Obsidian.colors
    val tint = habit.tint(c.accent)
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    Row(Modifier.fillMaxWidth().height(44.dp).padding(start = 14.dp, end = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).clip(RoundedCornerShape(8.dp)).clickable(onClickLabel = "open ${habit.name}", onClick = onOpen).padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(tint))
            Spacer(Modifier.width(8.dp))
            Text(habit.name, style = MaterialTheme.typography.bodyMedium, color = c.textNormal, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
        }
        ((today - 6)..today).forEach { day ->
            val state = Habits.day(habit, marks, day)
            Box(Modifier.width(34.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(
                            when (state) {
                                HabitDay.DONE -> tint
                                HabitDay.PARTIAL -> tint.copy(alpha = 0.4f)
                                else -> c.interactive
                            },
                        )
                        .then(if (state == HabitDay.MET) Modifier.border(1.5.dp, tint, RoundedCornerShape(9.dp)) else Modifier)
                        .then(if (day == today && state != HabitDay.DONE) Modifier.border(1.5.dp, c.textNormal, RoundedCornerShape(9.dp)) else Modifier)
                        .clickable(onClickLabel = "${habit.name}, ${if (day == today) "today" else "${today - day} days ago"}") {
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onDay(day)
                        },
                )
            }
        }
    }
}

/**
 * A habit as a compact row: the circle to tick today (or count one more, or a set's steps to unfold),
 * its name with how it's going under it, and the week as small dots. A tap elsewhere opens it.
 */
@Composable
private fun HabitRow(habit: Habit, marks: Map<Long, Int>, today: Long, onTick: () -> Unit, onStep: (HabitStep) -> Unit, onOpen: () -> Unit) {
    val c = Obsidian.colors
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    val tint = habit.tint(c.accent)
    val state = Habits.day(habit, marks, today)
    val kept = state == HabitDay.DONE || state == HabitDay.MET
    var unfolded by remember(habit.id) { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clickable(onClickLabel = "open ${habit.name}", onClick = onOpen).padding(horizontal = 14.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (state == HabitDay.DONE) tint else if (state == HabitDay.PARTIAL) tint.copy(alpha = 0.35f) else Color.Transparent)
                    .border(2.dp, if (kept || state == HabitDay.PARTIAL) tint else c.textFaint, CircleShape)
                    .clickable(
                        onClickLabel = when {
                            habit.isSet -> if (unfolded) "fold its steps" else "show its steps"
                            habit.kind == HabitKind.COUNT -> "one more"
                            else -> "tick ${habit.name}"
                        },
                    ) {
                        if (habit.isSet) {
                            unfolded = !unfolded
                        } else {
                            haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            onTick()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (state == HabitDay.DONE) Icon(LineIcons.Check, contentDescription = null, tint = c.bgPrimary, modifier = Modifier.size(17.dp))
                else if (habit.isSet) Text("${Habits.stepsTicked(habit, marks[today] ?: 0)}/${habit.steps.size}", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                else if (habit.kind == HabitKind.COUNT) Text("+", style = MaterialTheme.typography.titleSmall, color = c.textMuted)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.bodyMedium, color = c.textNormal, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                Text(
                    (if (habit.kind == HabitKind.COUNT) "${marks[today] ?: 0} of ${habit.target}${if (habit.unit.isBlank()) "" else " ${habit.unit}"} today" else Habits.describe(habit)) +
                        " · ${Habits.streak(habit, marks, today)} in a row",
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textMuted,
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                ((today - 6)..today).forEach { day -> HabitDot(Habits.day(habit, marks, day), day == today, c.textNormal, tint, 11) }
            }
        }
        if (habit.isSet && unfolded) HabitStepRows(habit, marks[today] ?: 0, c.textNormal, c.textMuted, c.textFaint, tint, Modifier.padding(top = 8.dp, start = 44.dp), onStep)
    }
}

/**
 * The last seven days in plain figures: how many of the habits due were kept, then a column a day as
 * tall as its share, with its count under it ("3/4").
 */
@Composable
private fun WeekSummary(habits: List<Habit>, marks: Map<String, Map<Long, Int>>, today: Long, shown: Boolean, onShown: (Boolean) -> Unit) {
    val c = Obsidian.colors
    val days = ((today - 6)..today).map { day ->
        val kept = habits.count { h -> Habits.day(h, marks[h.id].orEmpty(), day).let { it == HabitDay.DONE || it == HabitDay.MET } }
        day to kept
    }
    val kept = days.sumOf { it.second }
    val possible = habits.size * days.size
    // Small, so today's habits come first: one line, and under it the week as short bars. A tap on the
    // line folds the bars away, and it's remembered.
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(CardRadius)).background(c.bgSecondary)) {
        Row(
            Modifier.fillMaxWidth().clickable(onClickLabel = if (shown) "hide the week" else "show the week") { onShown(!shown) }
                .padding(start = 16.dp, end = 10.dp, top = 12.dp, bottom = if (shown) 8.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("$kept of $possible", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = c.textNormal)
            Spacer(Modifier.width(6.dp))
            Text("kept in the last seven days", style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.weight(1f))
            Icon(if (shown) LineIcons.KeyboardArrowUp else LineIcons.KeyboardArrowDown, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(20.dp))
        }
        if (shown) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                days.forEach { (day, n) ->
                    val share = n.toFloat() / habits.size
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.fillMaxWidth().height(26.dp), contentAlignment = Alignment.BottomCenter) {
                            Box(Modifier.fillMaxWidth().height(26.dp).clip(RoundedCornerShape(3.dp)).background(c.textNormal.copy(alpha = 0.08f)))
                            Box(Modifier.fillMaxWidth().height((26 * share).dp).clip(RoundedCornerShape(3.dp)).background(c.accent.copy(alpha = if (day == today) 1f else 0.7f)))
                        }
                        Text(
                            DateTimeFormatter.ofPattern("EEEEE", Locale.getDefault()).format(LocalDate.ofEpochDay(day)),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (day == today) c.textNormal else c.textFaint,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
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
    // A step being typed, added with its + (or kept by saving).
    var step by remember { mutableStateOf("") }
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
                    Text("Steps", style = MaterialTheme.typography.labelLarge, color = c.textMuted)
                    Text(
                        "Optional: what it's made of, like a workout's exercises. Each is ticked on its own on the habits page, and it's done once they all are; the widgets show it as one habit.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textFaint,
                    )
                    draft.steps.forEach { step ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ObsidianTextField(
                                step.name,
                                { name -> draft = draft.copy(steps = draft.steps.map { if (it.bit == step.bit) it.copy(name = name.take(40)) else it }) },
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                LineIcons.Close,
                                contentDescription = "remove ${step.name}",
                                tint = c.textFaint,
                                modifier = Modifier.padding(start = 4.dp).clip(CircleShape).clickable { draft = draft.copy(steps = draft.steps.filterNot { it.bit == step.bit }) }.padding(8.dp),
                            )
                        }
                    }
                    val addStep = {
                        if (step.isNotBlank()) {
                            draft = Habits.withStep(draft, step.trim())
                            step = ""
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ObsidianTextField(
                            step,
                            { step = it.take(40) },
                            modifier = Modifier.weight(1f),
                            placeholder = "Add a step: squats, lunges…",
                            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { addStep() }),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                        )
                        Icon(
                            LineIcons.Add,
                            contentDescription = "add the step",
                            tint = if (step.isNotBlank()) c.accent else c.textFaint,
                            modifier = Modifier.padding(start = 4.dp).clip(CircleShape).clickable(enabled = step.isNotBlank()) { addStep() }.padding(8.dp),
                        )
                    }
                } else {
                    Text("A day's aim", style = MaterialTheme.typography.labelLarge, color = c.textMuted)
                    NumberStepper(draft.target, { draft = draft.copy(target = it, times = 1, days = 1) }, 1..1000, title = "aim", presets = listOf(5, 8, 10, 20, 30, 50, 100))
                    ObsidianTextField(draft.unit, { draft = draft.copy(unit = it.take(20)) }, placeholder = "reps, pages, glasses…")
                }
                OptionRow(LineIcons.Home, "In the home screen's habits widget") {
                    ObsidianToggle(draft.shownOn(Screen.HOME), { draft = draft.withShownOn(Screen.HOME, it) })
                }
                OptionRow(LineIcons.Lock, "In the lock screen's habits widget") {
                    ObsidianToggle(draft.shownOn(Screen.LOCK), { draft = draft.withShownOn(Screen.LOCK, it) })
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
            TextButton(onClick = { onSave((if (step.isNotBlank() && draft.kind == HabitKind.YES_NO) Habits.withStep(draft, step.trim()) else draft).let { d -> d.copy(name = d.name.trim(), steps = d.steps.filter { it.name.isNotBlank() }.map { it.copy(name = it.name.trim()) }) }) }, enabled = draft.name.isNotBlank()) {
                Text("Save", color = if (draft.name.isNotBlank()) c.accentText else c.textFaint)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.textMuted) } },
    )
}

private val HabitColours: List<Pair<String, Long>> = AccentPresets + listOf("Ember" to 0xFFD9604AL, "Coral" to 0xFFE8875BL, "Gold" to 0xFFD4C04FL)
