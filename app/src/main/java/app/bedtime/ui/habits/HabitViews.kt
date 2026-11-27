package app.bedtime.ui.habits

import androidx.compose.foundation.layout.heightIn
import app.bedtime.ui.components.LineIcons
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.Habit
import app.bedtime.data.HabitDay
import app.bedtime.data.HabitKind
import app.bedtime.data.HabitStep
import androidx.compose.material.icons.filled.Check
import app.bedtime.data.Habits
import app.bedtime.data.Repository
import app.bedtime.ui.components.Text
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Your habits, and each one's marks by day, kept current. */
@Composable
fun rememberHabits(): Pair<List<Habit>, Map<String, Map<Long, Int>>> {
    val context = LocalContext.current
    val repo = remember { Repository.get(context.applicationContext) }
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val marks by repo.habitMarks.collectAsStateWithLifecycle(initialValue = emptyList())
    return settings?.habits.orEmpty() to Habits.byDay(marks)
}

/**
 * The habits widget: a line a habit, its name and its last seven days as dots, today's ringed. A tap
 * on today ticks it (or counts one more); holding the line opens it.
 */
@Composable
fun HabitsWidget(
    habits: List<Habit>,
    marks: Map<String, Map<Long, Int>>,
    today: Long,
    text: Color,
    faint: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    onTap: (Habit) -> Unit,
    onOpen: (Habit) -> Unit,
    /** A long press on a line: the widget's own sheet; without one, it opens the habit as a tap does. */
    onHold: (() -> Unit)? = null,
) {
    // With any count among them, every line keeps room for its figure, so all the days line up.
    val counts = habits.any { it.kind == HabitKind.COUNT }
    Column(modifier.fillMaxWidth()) {
        habits.forEach { habit -> HabitLine(habit, marks[habit.id].orEmpty(), today, text, faint, habit.tint(accent), { onTap(habit) }, { onOpen(habit) }, onHold, counts) }
    }
}

/**
 * The habits widget small: a dot for each habit, today's (filled when done, part-filled for a count
 * on its way), and how many of them are done. A tap opens all your habits; held, the widget's sheet.
 */
@OptIn(ExperimentalFoundationApi::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun HabitsDots(
    habits: List<Habit>,
    marks: Map<String, Map<Long, Int>>,
    today: Long,
    text: Color,
    faint: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    onOpenAll: () -> Unit,
    onHold: (() -> Unit)? = null,
    /** "habits · 2 of 5" after the dots; off, the dots alone. */
    label: Boolean = true,
) {
    val states = habits.map { Habits.day(it, marks[it.id].orEmpty(), today) }
    val done = states.count { it == HabitDay.DONE || it == HabitDay.MET }
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClickLabel = "open all your habits", onLongClick = onHold, onClick = onOpenAll)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Wrapping onto a second row where there are more than fit, as beside the clock.
        FlowRow(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            habits.zip(states).forEach { (habit, state) -> HabitDot(state, ringed = false, text = text, accent = habit.tint(accent), size = 14) }
        }
        if (label) Text("habits · $done of ${habits.size}", style = MaterialTheme.typography.bodyMedium, color = faint, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitLine(habit: Habit, marks: Map<Long, Int>, today: Long, text: Color, faint: Color, accent: Color, onTap: () -> Unit, onOpen: () -> Unit, onHold: (() -> Unit)?, roomForCount: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClickLabel = "open ${habit.name}", onLongClick = onHold ?: onOpen, onClick = onOpen)
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(habit.name, style = MaterialTheme.typography.bodyMedium, color = faint, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        ((today - 6)..today).forEach { day ->
            val isToday = day == today
            Box(
                Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .then(if (isToday) Modifier.clickable(onClickLabel = "mark ${habit.name} today", onClick = onTap) else Modifier),
                contentAlignment = Alignment.Center,
            ) { HabitDot(Habits.day(habit, marks, day), isToday, text, accent, 11) }
        }
        if (habit.kind == HabitKind.COUNT) {
            Text("${marks[today] ?: 0}/${habit.target}", style = MaterialTheme.typography.labelSmall, color = faint, maxLines = 1, modifier = Modifier.padding(start = 4.dp).width(34.dp))
        } else if (roomForCount) {
            Spacer(Modifier.padding(start = 4.dp).width(34.dp))
        }
    }
}

/** A set's steps, each a row with its tick, for today's [value]; a tap ticks or unticks one. */
@Composable
fun HabitStepRows(habit: Habit, value: Int, text: Color, muted: Color, faint: Color, accent: Color, modifier: Modifier = Modifier, onStep: (HabitStep) -> Unit) {
    Column(modifier) {
        habit.steps.forEach { step ->
            val ticked = value and (1 shl step.bit) != 0
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClickLabel = if (ticked) "untick ${step.name}" else "tick ${step.name}") { onStep(step) }
                    .padding(vertical = 8.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(22.dp).clip(CircleShape).background(if (ticked) accent else Color.Transparent).border(1.5.dp, if (ticked) accent else faint, CircleShape),
                    contentAlignment = Alignment.Center,
                ) { if (ticked) Icon(LineIcons.Check, contentDescription = null, tint = background(accent), modifier = Modifier.size(14.dp)) }
                Spacer(Modifier.width(16.dp))
                Text(step.name, style = MaterialTheme.typography.bodyMedium, color = if (ticked) muted else text)
            }
        }
    }
}

/**
 * A set from a widget, as a sheet: how many of its steps are done today, each step to tick, all of the
 * rest at once ([onAll]), and a way on to the habit itself. Ticking goes through [onStep], so the lock
 * screen can ask for its unlock first, as for any tick there.
 */
@Composable
fun HabitStepsCard(
    habit: Habit,
    value: Int,
    background: Color,
    text: Color,
    faint: Color,
    accent: Color,
    onStep: (HabitStep) -> Unit,
    onOpen: () -> Unit,
    onAll: (() -> Unit)? = null,
) {
    val accent = habit.tint(accent)
    val ticked = Habits.stepsTicked(habit, value)
    app.bedtime.ui.minimal.HomeSheet(background, faint) {
        Text(habit.name, style = MaterialTheme.typography.titleLarge, color = text)
        Text(Habits.describe(habit), style = MaterialTheme.typography.bodySmall, color = faint)
        // How far today: a segment a step, and the count.
        Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                habit.steps.forEach { step ->
                    val on = value and (1 shl step.bit) != 0
                    Box(Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50)).background(if (on) accent else text.copy(alpha = 0.12f)))
                }
            }
            Text("$ticked of ${habit.steps.size} today", style = MaterialTheme.typography.bodySmall, color = faint, modifier = Modifier.padding(start = 10.dp))
        }
        // The steps, on a card of their own, a line between each.
        Column(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(text.copy(alpha = 0.06f))) {
            habit.steps.forEachIndexed { i, step ->
                val on = value and (1 shl step.bit) != 0
                if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(text.copy(alpha = 0.06f)))
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 54.dp).clickable(onClickLabel = if (on) "untick ${step.name}" else "tick ${step.name}") { onStep(step) }.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(26.dp).clip(CircleShape).background(if (on) accent else Color.Transparent).border(1.5.dp, if (on) accent else faint, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { if (on) Icon(LineIcons.Check, contentDescription = null, tint = background(accent), modifier = Modifier.size(16.dp)) }
                    Spacer(Modifier.width(14.dp))
                    Text(
                        step.name,
                        style = MaterialTheme.typography.bodyLarge.copy(textDecoration = if (on) androidx.compose.ui.text.style.TextDecoration.LineThrough else null),
                        color = if (on) faint else text,
                    )
                }
            }
        }
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (onAll != null && ticked < habit.steps.size) {
                Box(
                    Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(50)).background(accent).clickable(onClick = onAll),
                    contentAlignment = Alignment.Center,
                ) { Text("Tick the rest", style = MaterialTheme.typography.labelLarge, color = background(accent)) }
            }
            SheetButton("Open the habit ›", text, Modifier.weight(1f), onOpen)
        }
    }
}

/** A pill at a sheet's foot, outlined in [colour]; [tinted] fills it faintly too. */
@Composable
private fun SheetButton(label: String, colour: Color, modifier: Modifier, onClick: () -> Unit, tinted: Boolean = false) {
    Box(
        modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(50)).background(if (tinted) colour.copy(alpha = 0.1f) else Color.Transparent)
            .border(1.dp, colour.copy(alpha = if (tinted) 0.5f else 0.25f), RoundedCornerShape(50)).clickable(onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = MaterialTheme.typography.labelLarge, color = colour) }
}

/** The habit's own colour, or [accent] if it has none. */
fun Habit.tint(accent: Color): Color = color?.let { Color(it) } ?: accent

/** One day: filled when done, half when part of a count, a ring when already covered, faint when open. */
@Composable
internal fun HabitDot(state: HabitDay, ringed: Boolean, text: Color, accent: Color, size: Int) {
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        if (ringed) Box(Modifier.size(size.dp).border(1.5.dp, text, CircleShape))
        val inner = if (ringed) size - 5 else size - 3
        when (state) {
            HabitDay.DONE -> Box(Modifier.size(inner.dp).clip(CircleShape).background(accent))
            HabitDay.PARTIAL -> Box(Modifier.size(inner.dp).clip(CircleShape).background(accent.copy(alpha = 0.45f)))
            HabitDay.MET -> Box(Modifier.size(inner.dp).border(1.5.dp, accent, CircleShape))
            HabitDay.OPEN -> Box(Modifier.size(inner.dp).clip(CircleShape).background(text.copy(alpha = 0.16f)))
        }
    }
}

/**
 * One habit up close, as a sheet: its strength and runs, today's count to change, and a month of days
 * to tick or untick, in the year page's colours; the whole year and all your habits a tap away.
 */
@Composable
fun HabitDetailContent(
    habit: Habit,
    marks: Map<Long, Int>,
    today: Long,
    background: Color,
    text: Color,
    faint: Color,
    accent: Color,
    onSet: (day: Long, value: Int) -> Unit,
    onEdit: (() -> Unit)? = null,
    /** Opens all your habits, from a button at the foot. */
    onOpenAll: (() -> Unit)? = null,
    /** The whole year of it, on a page of its own. */
    onYear: (() -> Unit)? = null,
    /** How many months back it opens on: 0 for this one. */
    monthsBack: Int = 0,
) {
    val accent = habit.tint(accent)
    var shown by remember { mutableIntStateOf(monthsBack) }
    val month = YearMonth.from(LocalDate.ofEpochDay(today)).minusMonths(shown.toLong())
    // A count's day to change: today, or one picked on the month below.
    var picked by remember(habit.id) { androidx.compose.runtime.mutableLongStateOf(today) }
    app.bedtime.ui.minimal.HomeSheet(background, faint) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.titleLarge, color = text)
                Text(Habits.describe(habit), style = MaterialTheme.typography.bodySmall, color = faint)
            }
            if (onEdit != null) {
                Text(
                    "Change",
                    style = MaterialTheme.typography.labelMedium,
                    color = text.copy(alpha = 0.75f),
                    modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, text.copy(alpha = 0.2f), RoundedCornerShape(50)).clickable(onClick = onEdit)
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Figure("${Habits.strength(habit, marks, today)}%", "strength", text, faint, Modifier.weight(1f))
            Figure("${Habits.streak(habit, marks, today)}", "days in a row", text, faint, Modifier.weight(1f))
            Figure("${Habits.bestStreak(habit, marks, today)}", "best run", text, faint, Modifier.weight(1f))
        }
        if (habit.kind == HabitKind.COUNT) {
            val value = marks[picked] ?: 0
            Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (picked == today) "Today" else DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()).format(LocalDate.ofEpochDay(picked)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (picked == today) faint else accent,
                    modifier = Modifier.weight(1f),
                )
                Step("−", text) { onSet(picked, (value - 1).coerceAtLeast(0)) }
                Text("$value of ${habit.target}${if (habit.unit.isBlank()) "" else " ${habit.unit}"}", style = MaterialTheme.typography.bodyLarge, color = text, modifier = Modifier.padding(horizontal = 12.dp))
                Step("+", text) { onSet(picked, value + 1) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(LineIcons.KeyboardArrowLeft, contentDescription = "the month before", tint = faint, modifier = Modifier.clip(CircleShape).clickable { shown++ }.padding(6.dp))
            Text(
                DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()).format(month),
                style = MaterialTheme.typography.labelLarge,
                color = text,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.weight(1f),
            )
            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                if (shown > 0) Icon(LineIcons.KeyboardArrowRight, contentDescription = "the month after", tint = faint, modifier = Modifier.clip(CircleShape).clickable { shown-- }.padding(6.dp))
            }
        }
        MonthGrid(habit, marks, month, today, text, faint, accent, onSet, picked = picked.takeIf { habit.kind == HabitKind.COUNT }, onPick = { picked = it })
        HabitLegend(text, faint, accent, Modifier.padding(top = 10.dp))
        Text(
            if (habit.kind == HabitKind.YES_NO) "Tap a day to tick or untick it" else "Tap a day to change its count above",
            style = MaterialTheme.typography.labelSmall,
            color = faint,
            modifier = Modifier.padding(top = 4.dp),
        )
        if (onYear != null || onOpenAll != null) {
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (onYear != null) SheetButton("The whole year ›", accent, Modifier.weight(1f), onYear, tinted = true)
                if (onOpenAll != null) SheetButton("All your habits", text, Modifier.weight(1f), onOpenAll)
            }
        }
    }
}

/** What a day's look means, as on the year page: done, part done, covered by how often, missed. */
@Composable
internal fun HabitLegend(text: Color, faint: Color, accent: Color, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(
            Modifier.background(accent) to "done",
            Modifier.background(accent.copy(alpha = 0.4f)) to "part done",
            Modifier.border(1.5.dp, accent, CircleShape) to "covered",
            Modifier.background(text.copy(alpha = 0.14f)) to "missed",
        ).forEach { (look, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).then(look))
                Text(label, style = MaterialTheme.typography.labelSmall, color = faint, modifier = Modifier.padding(start = 5.dp))
            }
        }
    }
}

/**
 * The month's days in weeks, Monday first: done filled in the habit's colour, part done lighter,
 * covered ringed, missed faintly shaded, today ringed apart; days to come faint and can't be marked.
 */
@Composable
private fun MonthGrid(
    habit: Habit,
    marks: Map<Long, Int>,
    month: YearMonth,
    today: Long,
    text: Color,
    faint: Color,
    accent: Color,
    onSet: (Long, Int) -> Unit,
    /** A count's day being changed, ringed in its colour; a tap on a day picks it. */
    picked: Long? = null,
    onPick: (Long) -> Unit = {},
) {
    val first = month.atDay(1)
    val lead = first.dayOfWeek.value - 1
    val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { first.withDayOfMonth(it).toEpochDay() }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = faint, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(Modifier.weight(1f).height(40.dp), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            val ahead = day > today
                            val state = Habits.day(habit, marks, day)
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .then(
                                        when (day) {
                                            picked -> Modifier.border(2.dp, accent, CircleShape)
                                            today -> Modifier.border(1.5.dp, text, CircleShape)
                                            else -> Modifier
                                        },
                                    )
                                    .padding(3.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            ahead -> Color.Transparent
                                            state == HabitDay.DONE -> accent
                                            state == HabitDay.PARTIAL -> accent.copy(alpha = 0.4f)
                                            state == HabitDay.MET -> Color.Transparent
                                            else -> text.copy(alpha = 0.08f)
                                        },
                                    )
                                    .then(if (state == HabitDay.MET && !ahead) Modifier.border(1.5.dp, accent, CircleShape) else Modifier)
                                    // A tick habit's day ticks; a count's is picked, to change it above.
                                    .clickable(enabled = !ahead) {
                                        if (habit.kind == HabitKind.COUNT) onPick(day) else onSet(day, Habits.tapped(habit, marks[day] ?: 0))
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${LocalDate.ofEpochDay(day).dayOfMonth}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = when {
                                        ahead -> faint.copy(alpha = 0.5f)
                                        state == HabitDay.DONE -> background(accent)
                                        else -> text
                                    },
                                )
                            }
                        }
                    }
                }
                repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/** Dark text on a light accent, light on a dark one. */
private fun background(accent: Color): Color =
    if (accent.red * 0.299f + accent.green * 0.587f + accent.blue * 0.114f > 0.6f) Color(0xFF202020) else Color.White

@Composable
private fun Figure(value: String, label: String, text: Color, faint: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(text.copy(alpha = 0.06f)).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = text)
        Text(label, style = MaterialTheme.typography.labelSmall, color = faint)
    }
}

@Composable
private fun Step(label: String, text: Color, onClick: () -> Unit) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).border(1.dp, text.copy(alpha = 0.3f), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(label, style = MaterialTheme.typography.titleMedium, color = text) }
}
