package app.bedtime.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.bedtime.data.Habit
import app.bedtime.data.HabitDay
import app.bedtime.data.HabitYear
import app.bedtime.data.Habits
import app.bedtime.ui.components.LineIcons
import app.bedtime.ui.components.Text
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** [HabitYearContent] over the whole screen; Back closes it. */
@Composable
fun HabitYearDialog(
    habit: Habit,
    marks: Map<Long, Int>,
    today: Long,
    background: Color,
    card: Color,
    text: Color,
    faint: Color,
    accent: Color,
    onSet: (day: Long, value: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        HabitYearContent(habit, marks, today, background, card, text, faint, accent, onSet, onDismiss)
    }
}

/**
 * A habit's year: how much of its goal was reached, every day as a dot in twelve small months, the
 * share month by month, and its runs. A month tapped opens it, to tick its days.
 */
@Composable
fun HabitYearContent(
    habit: Habit,
    marks: Map<Long, Int>,
    today: Long,
    background: Color,
    card: Color,
    text: Color,
    faint: Color,
    accent: Color,
    onSet: (day: Long, value: Int) -> Unit,
    onBack: () -> Unit,
) {
    val tint = habit.tint(accent)
    val now = LocalDate.ofEpochDay(today)
    // From the first day marked, or today for a habit not started yet.
    val first = marks.filterValues { it > 0 }.keys.minOrNull()?.coerceAtMost(today) ?: today
    var year by remember(habit.id) { mutableIntStateOf(now.year) }
    val start = maxOf(LocalDate.of(year, 1, 1).toEpochDay(), first)
    val end = minOf(LocalDate.of(year, 12, 31).toEpochDay(), today)
    val goal = HabitYear.goal(habit, marks, start, end, today)
    var month by remember { mutableStateOf<YearMonth?>(null) }
    month?.let { m ->
        app.bedtime.ui.minimal.HomeSheetDialog({ month = null }) {
            HabitDetailContent(
                habit, marks, today, card, text, faint, accent,
                onSet = onSet,
                monthsBack = ChronoUnit.MONTHS.between(m, YearMonth.from(now)).toInt(),
            )
        }
    }
    Column(
        Modifier
            .fillMaxSize()
            .background(background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                LineIcons.ArrowBack,
                contentDescription = "back",
                tint = faint,
                modifier = Modifier.clip(CircleShape).clickable(onClick = onBack).padding(8.dp).size(22.dp),
            )
            Column(Modifier.padding(start = 8.dp).weight(1f)) {
                Text(habit.name, style = MaterialTheme.typography.titleLarge, color = text)
                Text(Habits.describe(habit), style = MaterialTheme.typography.bodySmall, color = faint)
            }
        }
        // The year shown, and the others with something in them.
        Row(verticalAlignment = Alignment.CenterVertically) {
            val earliest = LocalDate.ofEpochDay(first).year
            Arrow(LineIcons.KeyboardArrowLeft, "the year before", faint, enabled = year > earliest) { year-- }
            Text("$year", style = MaterialTheme.typography.titleMedium, color = text, modifier = Modifier.padding(horizontal = 12.dp))
            Arrow(LineIcons.KeyboardArrowRight, "the year after", faint, enabled = year < now.year) { year++ }
        }
        // How much of the goal: the share, and what it's made of.
        Column {
            if (goal.of == 0) {
                Text("Nothing to count yet this year.", style = MaterialTheme.typography.bodyMedium, color = faint)
            } else {
                Text("${goal.percent}%", style = MaterialTheme.typography.displaySmall, color = tint)
                Text(
                    "${goal.done} of ${goal.of}${if (goal.unit.isBlank()) "" else " ${goal.unit}"} · " +
                        if (start > LocalDate.of(year, 1, 1).toEpochDay()) "since you started" else "of the goal this year",
                    style = MaterialTheme.typography.bodyMedium,
                    color = faint,
                )
            }
        }
        // Twelve small months, three to a row.
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            (1..12).chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    row.forEach { m ->
                        val ym = YearMonth.of(year, m)
                        Box(Modifier.weight(1f)) {
                            MiniMonth(habit, marks, ym, today, first, text, faint, tint) { if (ym.atDay(1).toEpochDay() <= today) month = ym }
                        }
                    }
                }
            }
        }
        Legend(text, faint, tint)
        // Each month's share of the goal.
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Month by month", style = MaterialTheme.typography.labelLarge, color = text)
            MonthShares(HabitYear.months(habit, marks, year, first, today), text, faint, tint)
        }
        // The strength through the year: building, holding or fading.
        val yearStart = LocalDate.of(year, 1, 1).toEpochDay()
        if (marks.isNotEmpty() && end >= start) {
            val daily = Habits.strengths(habit, marks, start, end)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Strength", style = MaterialTheme.typography.labelLarge, color = text)
                Text(
                    "${daily.first()}% → ${daily.last()}%" + if (end == today) " now" else " at the year's end",
                    style = MaterialTheme.typography.bodyMedium,
                    color = faint,
                )
                StrengthLine(daily, offset = (start - yearStart).toInt(), days = LocalDate.of(year, 12, 31).dayOfYear, text, faint, tint)
            }
        }
        // Runs of days kept: now, the best, and the longest ones.
        val runs = HabitYear.runs(habit, marks, first, today)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Runs", style = MaterialTheme.typography.labelLarge, color = text)
            Row {
                RunFigure("${Habits.streak(habit, marks, today)}", "days in a row now", text, faint, Modifier.weight(1f))
                RunFigure("${runs.firstOrNull()?.days ?: 0}", "the best run", text, faint, Modifier.weight(1f))
            }
            runs.take(3).forEach { run ->
                val from = LocalDate.ofEpochDay(run.start)
                val to = from.plusDays(run.days - 1L)
                Text(
                    "${run.days} ${if (run.days == 1) "day" else "days"} · ${spanWords(from, to)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = faint,
                )
            }
            if (runs.isEmpty()) Text("No days kept in a row yet.", style = MaterialTheme.typography.bodyMedium, color = faint)
        }
        Spacer(Modifier.height(24.dp))
    }
}

/** A month as dots, Monday first: done filled, part done lighter, covered by how often ringed, open faint. */
@Composable
private fun MiniMonth(habit: Habit, marks: Map<Long, Int>, month: YearMonth, today: Long, first: Long, text: Color, faint: Color, tint: Color, onClick: () -> Unit) {
    val lead = month.atDay(1).dayOfWeek.value - 1
    val days = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it).toEpochDay() }
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClickLabel = "open this month", onClick = onClick).padding(4.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Text(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()).format(month), style = MaterialTheme.typography.labelMedium, color = text)
        days.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                (week + List(7 - week.size) { null }).forEach { day ->
                    Box(Modifier.size(8.dp).clip(CircleShape).then(dotLook(habit, marks, day, today, first, text, tint)))
                }
            }
        }
    }
}

private fun dotLook(habit: Habit, marks: Map<Long, Int>, day: Long?, today: Long, first: Long, text: Color, tint: Color): Modifier = when {
    day == null -> Modifier
    day > today -> Modifier.border(1.dp, text.copy(alpha = 0.08f), CircleShape)
    day < first -> Modifier.background(text.copy(alpha = 0.05f))
    else -> when (Habits.day(habit, marks, day)) {
        HabitDay.DONE -> Modifier.background(tint)
        HabitDay.PARTIAL -> Modifier.background(tint.copy(alpha = 0.4f))
        HabitDay.MET -> Modifier.border(1.5.dp, tint, CircleShape)
        HabitDay.OPEN -> Modifier.background(text.copy(alpha = 0.14f))
    }
}

/** What the dots mean. */
@Composable
private fun Legend(text: Color, faint: Color, tint: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(
            Modifier.background(tint) to "done",
            Modifier.background(tint.copy(alpha = 0.4f)) to "part done",
            Modifier.border(1.5.dp, tint, CircleShape) to "covered",
            Modifier.background(text.copy(alpha = 0.14f)) to "missed",
        ).forEach { (look, label) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(CircleShape).then(look))
                Text(label, style = MaterialTheme.typography.labelSmall, color = faint, modifier = Modifier.padding(start = 5.dp))
            }
        }
    }
}

/** A column a month, as tall as its share of the goal, the share over it; nothing for months not counted. */
@Composable
private fun MonthShares(shares: List<Int?>, text: Color, faint: Color, tint: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
        shares.forEachIndexed { i, share ->
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(share?.let { "$it" } ?: "", style = MaterialTheme.typography.labelSmall, color = faint, maxLines = 1, softWrap = false)
                Box(Modifier.fillMaxWidth().height(64.dp), contentAlignment = Alignment.BottomCenter) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(text.copy(alpha = 0.12f)))
                    if (share != null && share > 0) {
                        Box(Modifier.fillMaxWidth().height((64f * share / 100).dp).clip(RoundedCornerShape(3.dp)).background(tint.copy(alpha = 0.75f)))
                    }
                }
                Text(
                    YearMonth.of(2000, i + 1).format(DateTimeFormatter.ofPattern("MMMMM", Locale.getDefault())),
                    style = MaterialTheme.typography.labelSmall,
                    color = faint,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * The strength day by day across the year, January to December along the foot and 0 to 100% up the
 * side: a line from the first day counted ([offset] days into the year) to the last, a dot at its end.
 */
@Composable
private fun StrengthLine(daily: List<Int>, offset: Int, days: Int, text: Color, faint: Color, tint: Color) {
    Column {
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(72.dp)) {
            val x = { i: Int -> size.width * (offset + i) / (days - 1).coerceAtLeast(1) }
            val y = { v: Int -> size.height * (1 - v / 100f) }
            // 0, 50 and 100%, faint.
            listOf(0, 50, 100).forEach { v ->
                drawLine(text.copy(alpha = 0.1f), androidx.compose.ui.geometry.Offset(0f, y(v)), androidx.compose.ui.geometry.Offset(size.width, y(v)), strokeWidth = 1.dp.toPx())
            }
            val path = androidx.compose.ui.graphics.Path()
            daily.forEachIndexed { i, v -> if (i == 0) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v)) }
            drawPath(path, tint, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            drawCircle(tint, radius = 4.dp.toPx(), center = androidx.compose.ui.geometry.Offset(x(daily.lastIndex), y(daily.last())))
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            (1..12).forEach { m ->
                Text(
                    YearMonth.of(2000, m).format(DateTimeFormatter.ofPattern("MMMMM", Locale.getDefault())),
                    style = MaterialTheme.typography.labelSmall,
                    color = faint,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun RunFigure(value: String, label: String, text: Color, faint: Color, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = text)
        Text(label, style = MaterialTheme.typography.labelSmall, color = faint)
    }
}

@Composable
private fun Arrow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, enabled: Boolean, onClick: () -> Unit) {
    Icon(
        icon,
        contentDescription = label,
        tint = if (enabled) tint else tint.copy(alpha = 0.25f),
        modifier = Modifier.clip(CircleShape).clickable(enabled = enabled, onClick = onClick).padding(6.dp).size(20.dp),
    )
}

/** "3–25 mar", "28 feb – 4 mar", or with the year when it isn't this one's. */
private fun spanWords(from: LocalDate, to: LocalDate): String {
    val day = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    val year = if (to.year != LocalDate.now().year) " ${to.year}" else ""
    return when {
        from == to -> day.format(from) + year
        from.month == to.month && from.year == to.year -> "${from.dayOfMonth}–${day.format(to)}$year"
        else -> "${day.format(from)} – ${day.format(to)}$year"
    }
}
