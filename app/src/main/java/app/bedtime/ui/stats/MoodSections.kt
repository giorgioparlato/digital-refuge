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
internal fun MoodStats(entries: List<JournalEntry>, moods: List<String>, today: LocalDate, onDay: (LocalDate) -> Unit) {
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
    SectionCard(title = "How often", subtitle = "Each mood this month, and when in the day it came up most") {
        val counts = Moods.counts(marks)
        if (counts.isEmpty()) {
            Text("No moods yet this month: tap how you feel when answering “why”, or in the microjournal.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
        }
        counts.forEach { (mood, count) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(colourOf(mood, moods)))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(mood, style = MaterialTheme.typography.bodyLarge, color = c.textNormal)
                    Moods.mostly(marks, mood, zone)?.let { Text("mostly $it", style = MaterialTheme.typography.bodySmall, color = c.textFaint) }
                }
                Text(if (count == 1) "once" else "$count times", style = MaterialTheme.typography.labelLarge, color = c.textMuted)
            }
        }
    }
}
