package app.bedtime.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.bedtime.data.JournalEntry
import app.bedtime.data.Moods
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/** A colour for each mood, in the order of your moods; one no longer among them is grey. */
private val MoodColours = listOf(
    Color(0xFFE0B243), Color(0xFFE47B8A), Color(0xFF4DB2D1), Color(0xFFA99BEA),
    Color(0xFF86BC9F), Color(0xFFD9762B), Color(0xFF6C8EBF), Color(0xFFC9A0DC),
)

private fun colourOf(mood: String, moods: List<String>): Color = MoodColours.getOrNull(moods.indexOf(mood).takeIf { it >= 0 } ?: -1) ?: Color(0xFF8A8A8A)

/**
 * Moods over a month: each day coloured by the one tapped most, a tap on a day opening its
 * microjournal entry; then how often each came up, and when in the day.
 */
@Composable
internal fun MoodStats(
    entries: List<JournalEntry>,
    moods: List<String>,
    today: LocalDate,
    onDay: (LocalDate) -> Unit,
    onMoods: () -> Unit = {},
    /** Phone use by day, habits and their marks, for the patterns: each set beside the moods. */
    days: Map<Long, app.bedtime.data.UseDay> = emptyMap(),
    habits: List<app.bedtime.data.Habit> = emptyList(),
    habitMarks: Map<String, Map<Long, Int>> = emptyMap(),
) {
    val c = Obsidian.colors
    val zone = ZoneId.systemDefault()
    var monthDay by rememberSaveable { mutableLongStateOf(today.withDayOfMonth(1).toEpochDay()) }
    val month = YearMonth.from(LocalDate.ofEpochDay(monthDay))
    val marks = Moods.inMonth(entries, month, zone)
    val byDay = Moods.byDay(marks, zone)
    val written = entries.map { it.day }.toSet()
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).clickable { monthDay = month.minusMonths(1).atDay(1).toEpochDay() }, contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "the month before", tint = c.textMuted)
        }
        Text(
            month.atDay(1).format(DateTimeFormatter.ofPattern(if (month.year == today.year) "MMMM" else "MMMM yyyy", Locale.getDefault())),
            style = MaterialTheme.typography.titleMedium,
            color = c.textNormal,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f),
        )
        val latest = month >= YearMonth.from(today)
        Box(Modifier.size(40.dp).clip(CircleShape).clickable(enabled = !latest) { monthDay = month.plusMonths(1).atDay(1).toEpochDay() }, contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "the month after", tint = if (latest) c.textFaint.copy(alpha = 0.4f) else c.textMuted)
        }
    }
    SectionCard(title = "Each day", subtitle = "Coloured by the mood tapped most. Tap a day for its entry") {
        Row(Modifier.fillMaxWidth()) {
            DayOfWeek.entries.forEach { d ->
                Text(d.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = c.textFaint, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(6.dp))
        val lead = month.atDay(1).dayOfWeek.value - 1
        val cells: List<LocalDate?> = List(lead) { null } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0 until 7).forEach { i ->
                    val day = week.getOrNull(i)
                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                        if (day != null) {
                            val mood = byDay[day]
                            val isToday = day == today
                            val shape = RoundedCornerShape(8.dp)
                            val opens = day.toEpochDay() in written
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(shape)
                                    .background(mood?.let { colourOf(it, moods).copy(alpha = 0.75f) } ?: Color.Transparent)
                                    .border(if (isToday) 2.dp else 1.dp, if (isToday) c.accent else c.border.copy(alpha = if (day.isAfter(today)) 0.3f else 0.7f), shape)
                                    .then(if (opens) Modifier.clickable(onClickLabel = "open this day's entry") { onDay(day) } else Modifier),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${day.dayOfMonth}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isToday) FontWeight.Bold else null,
                                    color = if (mood == null && !isToday) c.textFaint else c.textNormal,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    var chosen by rememberSaveable { mutableStateOf<String?>(null) }
    SectionCard(title = "How often", subtitle = "Each mood this month, and when in the day it came up most. Tap one for its lines") {
        val counts = Moods.counts(marks)
        if (counts.isEmpty()) {
            Text("No moods yet this month: tap how you feel when answering “why”, or in the microjournal.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
        }
        counts.forEach { (mood, count) ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .background(if (mood == chosen) c.bgSecondary else Color.Transparent)
                    .clickable(onClickLabel = "this mood's lines") { chosen = if (mood == chosen) null else mood }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(colourOf(mood, moods)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(mood, style = MaterialTheme.typography.bodyLarge, color = c.textNormal)
                    Moods.mostly(marks, mood, zone)?.let { Text("mostly $it", style = MaterialTheme.typography.bodySmall, color = c.textFaint) }
                }
                Text(if (count == 1) "once" else "$count times", style = MaterialTheme.typography.labelLarge, color = c.textMuted)
            }
        }
        Text(
            "Your moods ›",
            style = MaterialTheme.typography.labelLarge,
            color = c.accentText,
            modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onMoods).padding(vertical = 6.dp),
        )
    }
    // Sixty days or so back, for what needs more than a month to say anything.
    val zone8 = ZoneId.systemDefault()
    val recent = Moods.between(entries, today.minusWeeks(8), today, zone8)
    chosen?.let { mood -> MoodDetail(mood, entries, month, recent, days, zone8) { chosen = null } }
    MoodPatterns(recent, days, habits, habitMarks, zone8)

}

/** One mood: when in the week it comes, what "why?" saw come after it, the phone on its days, and its lines this month. */
@Composable
private fun MoodDetail(
    mood: String,
    entries: List<JournalEntry>,
    month: YearMonth,
    recent: List<app.bedtime.data.MoodMark>,
    days: Map<Long, app.bedtime.data.UseDay>,
    zone: ZoneId,
    onClose: () -> Unit,
) {
    val c = Obsidian.colors
    SectionCard(title = mood, subtitle = "When in the week, over the last eight weeks; then its lines this month") {
        // Weekdays across, the parts of the day down: the more, the stronger.
        val grid = Moods.grid(recent, mood, zone)
        val most = grid.values.maxOrNull() ?: 0
        val parts = listOf("mornings", "afternoons", "evenings", "nights")
        Row(Modifier.fillMaxWidth()) {
            Spacer(Modifier.width(76.dp))
            DayOfWeek.entries.forEach { d ->
                Text(d.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = c.textFaint, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        parts.forEach { part ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(part, style = MaterialTheme.typography.labelSmall, color = c.textFaint, modifier = Modifier.width(76.dp))
                DayOfWeek.entries.forEach { d ->
                    val n = grid[d to part] ?: 0
                    Box(Modifier.weight(1f).padding(2.dp).height(18.dp).clip(RoundedCornerShape(4.dp)).background(if (n == 0) c.bgSecondary else c.accent.copy(alpha = 0.25f + 0.75f * n / most)))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Moods.next(recent, mood)?.let { next ->
            PatternLine(
                "When “why?” asked",
                "put it down ${next.putDown} of ${next.of} times" + (next.app?.let { " · mostly opened $it" } ?: ""),
            )
        }
        Moods.useOnDays(recent, days, mood, zone)?.let { (with, without) ->
            PatternLine("In distracting apps", "${app.bedtime.data.UseWords.short(with)} on its days, against ${app.bedtime.data.UseWords.short(without)} on others")
        }
        val lines = Moods.linesWith(entries, month, mood)
        if (lines.isEmpty()) Text("No lines with it this month.", style = MaterialTheme.typography.bodySmall, color = c.textFaint, modifier = Modifier.padding(top = 6.dp))
        var all by rememberSaveable(mood) { mutableStateOf(false) }
        (if (all) lines else lines.take(12)).forEach { (day, line) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(day.format(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())), style = MaterialTheme.typography.labelSmall, color = c.textFaint, modifier = Modifier.width(52.dp).padding(top = 2.dp))
                app.bedtime.ui.components.VerbatimText(line, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            }
        }
        if (lines.size > 12) {
            Text(
                if (all) "Fewer" else "All ${lines.size}",
                style = MaterialTheme.typography.labelLarge,
                color = c.accentText,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { all = !all }.padding(vertical = 6.dp),
            )
        }
        Text("Close", style = MaterialTheme.typography.labelLarge, color = c.textMuted, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClose).padding(vertical = 6.dp))
    }
}

/**
 * The moods beside the rest of the last eight weeks: where they're tapped, mornings after a late night
 * on the phone, the days you kept a habit, and how a stretch on the phone left you. Each only once
 * there's enough to say it; and only "on days you…", never why.
 */
@Composable
private fun MoodPatterns(
    recent: List<app.bedtime.data.MoodMark>,
    days: Map<Long, app.bedtime.data.UseDay>,
    habits: List<app.bedtime.data.Habit>,
    habitMarks: Map<String, Map<Long, Int>>,
    zone: ZoneId,
) {
    val c = Obsidian.colors
    val (atUnlock, own) = Moods.whereFrom(recent)
    val nights = Moods.afterLateNights(recent, days, zone).take(2)
    val kept = Moods.withHabits(recent, habits, habitMarks, zone).take(3)
    val after = Moods.afterwards(recent).take(3)
    SectionCard(title = "Patterns", subtitle = "The last eight weeks, once there's enough to say something") {
        if (atUnlock.isEmpty() && own.isEmpty() && nights.isEmpty() && kept.isEmpty() && after.isEmpty()) {
            Text("Nothing yet: these show after a few weeks of moods.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
        }
        if (atUnlock.isNotEmpty()) PatternLine("When “why?” asks after an unlock", atUnlock.joinToString(", "))
        if (own.isNotEmpty()) PatternLine("In what you write yourself", own.joinToString(", "))
        nights.forEach { o -> PatternLine("Mornings after a late night on the phone", "${o.mood} ${o.with} of ${o.days}, against ${o.otherWith} of ${o.others} after the others") }
        kept.forEach { (habit, o) -> PatternLine("Days you kept “${habit.name}”", "${o.mood} ${o.with} of ${o.days}, against ${o.otherWith} of ${o.others} on the others") }
        if (after.isNotEmpty()) PatternLine("After a stretch on the phone", after.joinToString(", ") { (mood, n) -> if (n == 1) mood else "$mood ($n)" })
    }
}

@Composable
private fun PatternLine(what: String, how: String) {
    val c = Obsidian.colors
    Column(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(what, style = MaterialTheme.typography.bodySmall, color = c.textFaint)
        Text(how, style = MaterialTheme.typography.bodyMedium, color = c.textNormal)
    }
}
