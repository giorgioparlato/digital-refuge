package app.bedtime.ui.stats

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.bedtime.engine.LookBack
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.VerbatimText
import app.bedtime.ui.formatMinutes
import app.bedtime.ui.theme.Obsidian
import java.time.format.DateTimeFormatter

/**
 * The week looked back on, in a few plain lines against the week before. It shows only while it's
 * fresh, and the × puts it away until the next one.
 */
@Composable
internal fun LookBackCard(week: LookBack, appLabel: (String) -> String, onPutAway: () -> Unit) {
    val c = Obsidian.colors
    Box {
        SectionCard(title = "Your week", subtitle = weekLabel(week)) {
            LookBackLines.of(week, appLabel).forEach { (what, how) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                    Text(what, style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.weight(1f).padding(end = 12.dp))
                    VerbatimText(how, style = MaterialTheme.typography.bodyMedium, color = c.textNormal)
                }
            }
        }
        Box(
            Modifier.align(Alignment.TopEnd).padding(6.dp).size(40.dp).clip(CircleShape).clickable(onClickLabel = "put it away until next week", onClick = onPutAway),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Clear, contentDescription = "put it away", tint = c.textFaint, modifier = Modifier.size(18.dp))
        }
    }
}

/** "20 – 26 october". */
internal fun weekLabel(week: LookBack): String {
    val day = DateTimeFormatter.ofPattern("d")
    val dayMonth = DateTimeFormatter.ofPattern("d MMMM")
    return if (week.weekStart.month == week.weekEnd.month) {
        "${week.weekStart.format(day)} – ${week.weekEnd.format(dayMonth)}"
    } else {
        "${week.weekStart.format(dayMonth)} – ${week.weekEnd.format(dayMonth)}"
    }.lowercase()
}

/** The look-back as lines of what and how much; shared by the card and the Sunday note. */
internal object LookBackLines {
    fun of(week: LookBack, appLabel: (String) -> String): List<Pair<String, String>> = buildList {
        add("In refuge" to formatMinutes(week.refugeMinutes) + versus(week.refugeMinutes, week.refugeMinutesBefore))
        if (week.finished > 0 || week.escapes > 0) {
            add(
                "Sessions" to "${week.kept} of ${week.finished} kept" +
                    if (week.escapes > 0) " · ${week.escapes} ${if (week.escapes == 1) "way" else "ways"} out" else "",
            )
        }
        week.phone?.let { add("On the phone" to formatMinutes(it / 60) + versus(it, week.phoneBefore)) }
        week.distracting?.takeIf { it > 0 }?.let { add("In distracting apps" to formatMinutes(it / 60) + versus(it, week.distractingBefore)) }
        week.topApp?.let { (pkg, seconds) -> add("Most of that" to "${appLabel(pkg)}, ${formatMinutes(seconds / 60)}") }
        if (week.habitDays > 0) add("Habits" to "${week.habitDaysKept} of ${week.habitDays} days kept")
    }

    /** One line for the notification: the time in refuge, how sessions held, the phone against before. */
    fun summary(week: LookBack): String = buildList {
        add("${formatMinutes(week.refugeMinutes)} in refuge")
        if (week.finished > 0) add("${week.kept} of ${week.finished} kept")
        week.phone?.let { add("phone ${formatMinutes(it / 60)}" + versus(it, week.phoneBefore)) }
    }.joinToString(" · ")

    /** ", 12% less" against the week before; nothing when there's no week before to say it against. */
    private fun versus(now: Long, before: Long?): String {
        if (before == null || before <= 0) return ""
        val change = ((now - before) * 100 / before).toInt()
        return when {
            change == 0 -> ", as before"
            change > 0 -> ", $change% more"
            else -> ", ${-change}% less"
        }
    }
}
