package app.bedtime.ui.habits

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
) {
    Column(modifier.fillMaxWidth()) {
        habits.forEach { habit -> HabitLine(habit, marks[habit.id].orEmpty(), today, text, faint, habit.tint(accent), { onTap(habit) }, { onOpen(habit) }) }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HabitLine(habit: Habit, marks: Map<Long, Int>, today: Long, text: Color, faint: Color, accent: Color, onTap: () -> Unit, onOpen: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(onClickLabel = "open ${habit.name}", onLongClick = onOpen, onClick = onOpen)
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
            Text("${marks[today] ?: 0}/${habit.target}", style = MaterialTheme.typography.labelSmall, color = faint, modifier = Modifier.padding(start = 4.dp).width(34.dp))
        }
    }
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
 * One habit up close: its strength and streaks, today's count to change, and a month of days to tick
 * or untick.
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
) {
    val accent = habit.tint(accent)
    var shown by remember { mutableIntStateOf(0) }
    val month = YearMonth.from(LocalDate.ofEpochDay(today)).minusMonths(shown.toLong())
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(background).padding(22.dp)) {
        Text(habit.name, style = MaterialTheme.typography.titleMedium, color = text)
        Text(Habits.describe(habit), style = MaterialTheme.typography.bodySmall, color = faint)
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth()) {
            Figure("${Habits.strength(habit, marks, today)}%", "strength", text, faint, Modifier.weight(1f))
            Figure("${Habits.streak(habit, marks, today)}", "days in a row", text, faint, Modifier.weight(1f))
            Figure("${Habits.bestStreak(habit, marks, today)}", "best run", text, faint, Modifier.weight(1f))
        }
        if (habit.kind == HabitKind.COUNT) {
            val value = marks[today] ?: 0
            Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Today", style = MaterialTheme.typography.bodyMedium, color = faint, modifier = Modifier.weight(1f))
                Step("−", text) { onSet(today, (value - 1).coerceAtLeast(0)) }
                Text("$value of ${habit.target}${if (habit.unit.isBlank()) "" else " ${habit.unit}"}", style = MaterialTheme.typography.bodyLarge, color = text, modifier = Modifier.padding(horizontal = 12.dp))
                Step("+", text) { onSet(today, value + 1) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "the month before", tint = faint, modifier = Modifier.clip(CircleShape).clickable { shown++ }.padding(4.dp))
            Text(
                DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()).format(month),
                style = MaterialTheme.typography.labelLarge,
                color = text,
                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
            )
            if (shown > 0) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "the month after", tint = faint, modifier = Modifier.clip(CircleShape).clickable { shown-- }.padding(4.dp))
            }
        }
        MonthGrid(habit, marks, month, today, text, faint, accent, onSet)
        if (habit.kind == HabitKind.YES_NO) {
            Text("Tap a day to tick or untick it.", style = MaterialTheme.typography.labelSmall, color = faint, modifier = Modifier.padding(top = 6.dp))
        }
        if (onEdit != null) {
            Text(
                "Change it",
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                modifier = Modifier.padding(top = 12.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onEdit).padding(vertical = 6.dp),
            )
        }
    }
}

/** The month's days in weeks, Monday first; days to come are faint and can't be marked. */
@Composable
private fun MonthGrid(habit: Habit, marks: Map<Long, Int>, month: YearMonth, today: Long, text: Color, faint: Color, accent: Color, onSet: (Long, Int) -> Unit) {
    val first = month.atDay(1)
    val lead = first.dayOfWeek.value - 1
    val cells = List(lead) { null } + (1..month.lengthOfMonth()).map { first.withDayOfMonth(it).toEpochDay() }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                Text(it, style = MaterialTheme.typography.labelSmall, color = faint, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    Box(Modifier.weight(1f).height(32.dp), contentAlignment = Alignment.Center) {
                        if (day != null) {
                            val ahead = day > today
                            val state = Habits.day(habit, marks, day)
                            Box(
                                Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            ahead -> Color.Transparent
                                            state == HabitDay.DONE -> accent
                                            state == HabitDay.PARTIAL -> accent.copy(alpha = 0.45f)
                                            else -> Color.Transparent
                                        },
                                    )
                                    .then(if (state == HabitDay.MET && !ahead) Modifier.border(1.5.dp, accent, CircleShape) else Modifier)
                                    .then(if (day == today) Modifier.border(1.5.dp, text, CircleShape) else Modifier)
                                    .clickable(enabled = !ahead && habit.kind == HabitKind.YES_NO) { onSet(day, Habits.tapped(habit, marks[day] ?: 0)) },
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
    Column(modifier) {
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
