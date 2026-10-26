package app.bedtime.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.bedtime.data.AppUse
import app.bedtime.data.HourRule
import app.bedtime.data.HourlyUse
import app.bedtime.data.PhoneUsage
import app.bedtime.data.UseDay
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.HourDetailContent
import app.bedtime.ui.components.HourDots
import app.bedtime.ui.components.OptionRow
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.SegmentedChoice
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.UseDays
import app.bedtime.ui.formatMinutes
import app.bedtime.ui.theme.Obsidian
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun time(seconds: Long) = formatMinutes(seconds / 60)

/**
 * Phone use, Digital Wellbeing's kind but calmer: today, day by day, apps, the hours you tend to use it,
 * and this week against last. Without usage access it's refuge's own count, and it says what's missing.
 */
@Composable
internal fun PhoneUseSections(
    use: UseDays,
    today: Long,
    nowHour: Int,
    rule: HourRule,
    labels: Map<String, String>,
    onAllowAccess: (() -> Unit)?,
) {
    val c = Obsidian.colors
    val days = PhoneUsage.range(use.days, today - 13, today)
    val todayUse = days.last()
    val week = days.takeLast(7)
    val before = days.dropLast(7)
    if (!use.fromAndroid && onAllowAccess != null) {
        SectionCard {
            OptionRow(
                Icons.Default.Info,
                "Allow usage access",
                description = "For every app's time, your unlocks, and the days from before refuge, from Android's own record. It stays on the phone. The hour dots work without it",
                onClick = onAllowAccess,
            ) { Chevron() }
        }
    }

    SectionCard(title = "Today") {
        val hours = todayUse.hours(use.distractingApps)
        HourDots(HourlyUse.states(hours, nowHour, rule), text = c.textNormal, accent = c.accent)
        val yesterday = days[days.size - 2].hours(use.distractingApps)
        HourDetailContent(
            HourlyUse.report(hours, yesterday, rule, nowHour),
            hours,
            rule,
            labels,
            background = Color.Transparent,
            text = c.textNormal,
            faint = c.textMuted,
            accent = c.accent,
            unlocks = todayUse.unlocks,
            title = null,
            padding = 0.dp,
        )
        val past = days.dropLast(1).takeLast(7)
        PhoneUsage.dailyAverage(past) { it.total }?.let { avg ->
            val distractingAvg = PhoneUsage.dailyAverage(past) { it.distractingTotal } ?: 0L
            Text(
                "Your daily average this past week: ${time(avg)} on the phone, ${time(distractingAvg)} in distracting apps.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textMuted,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }

    var picked by rememberSaveable { mutableIntStateOf(days.lastIndex) }
    SectionCard(title = "Day by day", subtitle = "The last two weeks: all of it, and the part in distracting apps. Tap a day for its apps") {
        UseBars(days, picked) { picked = it }
        val day = days[picked.coerceIn(days.indices)]
        Spacer(Modifier.height(12.dp))
        Text(
            DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault()).format(LocalDate.ofEpochDay(day.day)),
            style = MaterialTheme.typography.labelLarge,
            color = c.textNormal,
        )
        Text(
            listOfNotNull(
                "${time(day.total)} on the phone",
                "${time(day.distractingTotal)} distracting",
                day.unlocks?.let { "$it unlocks" },
            ).joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = c.textMuted,
        )
        AppRows(PhoneUsage.ranked(listOf(day), use.distractingApps).take(5), labels)
    }

    var weekly by rememberSaveable { mutableStateOf(false) }
    var everything by rememberSaveable { mutableStateOf(false) }
    SectionCard(title = "Apps", subtitle = if (use.fromAndroid) "Time and opens; distracting ones marked" else "Only your distracting apps are counted without usage access") {
        SegmentedChoice(options = listOf("Today", "This week"), selected = if (weekly) 1 else 0, onSelect = { weekly = it == 1 })
        val ranked = PhoneUsage.ranked(if (weekly) week else listOf(todayUse), use.distractingApps)
        AppRows(if (everything) ranked else ranked.take(8), labels)
        if (ranked.isEmpty()) Text("Nothing yet.", style = MaterialTheme.typography.bodyMedium, color = c.textFaint, modifier = Modifier.padding(top = 8.dp))
        if (ranked.size > 8) {
            Text(
                if (everything) "Fewer" else "All ${ranked.size}",
                style = MaterialTheme.typography.labelLarge,
                color = c.accentText,
                modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).clickable { everything = !everything }.padding(vertical = 6.dp),
            )
        }
    }

    SectionCard(title = "Hours of the day", subtitle = "When you're on the phone, on average over the last week") {
        HourBars(PhoneUsage.averageHours(week))
    }

    val trend = PhoneUsage.trend(week, before)
    if (before.any { it.total > 0 }) {
        SectionCard(title = "This week against last") {
            TrendRow("On the phone", trend.total.first, trend.total.second) { time(it / 7) + " a day" }
            TrendRow("In distracting apps", trend.distracting.first, trend.distracting.second) { time(it / 7) + " a day" }
            trend.unlocks?.let { (now, then) -> TrendRow("Unlocks", now.toLong(), then.toLong()) { "${it / 7} a day" } }
        }
    }
}

/** A column a day, as tall as the time on the phone, the distracting part in the accent from the foot. */
@Composable
private fun UseBars(days: List<UseDay>, picked: Int, onPick: (Int) -> Unit) {
    val c = Obsidian.colors
    val most = days.maxOf { it.total }.coerceAtLeast(1)
    Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
        days.forEachIndexed { i, day ->
            Column(
                Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(4.dp)).clickable(onClickLabel = "this day") { onPick(i) },
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                val share = day.total.toFloat() / most
                val bar = (96 * share).dp
                Box(Modifier.fillMaxWidth().height(bar).clip(RoundedCornerShape(3.dp)).background(c.textNormal.copy(alpha = if (i == picked) 0.35f else 0.16f))) {
                    val part = if (day.total > 0) day.distractingTotal.toFloat() / day.total else 0f
                    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(bar * part).background(c.accent.copy(alpha = if (i == picked) 1f else 0.7f)))
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    DateTimeFormatter.ofPattern("EEEEE", Locale.getDefault()).format(LocalDate.ofEpochDay(day.day)),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (i == picked) c.textNormal else c.textFaint,
                )
            }
        }
    }
}

/** 24 small columns, the average on the phone in each hour, the distracting part in the accent. */
@Composable
private fun HourBars(hours: List<Pair<Long, Long>>) {
    val c = Obsidian.colors
    val most = hours.maxOf { it.first }.coerceAtLeast(1)
    Row(Modifier.fillMaxWidth().height(80.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
        hours.forEach { (all, distracting) ->
            val bar = (80 * all.toFloat() / most).dp
            Box(Modifier.weight(1f).height(bar).clip(RoundedCornerShape(2.dp)).background(c.textNormal.copy(alpha = 0.16f))) {
                val part = if (all > 0) distracting.toFloat() / all else 0f
                Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(bar * part).background(c.accent))
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
        listOf("midnight", "6", "noon", "18").forEach { Text(it, style = MaterialTheme.typography.labelSmall, color = c.textFaint, modifier = Modifier.weight(1f)) }
    }
    val peak = hours.indices.maxByOrNull { hours[it].first }?.takeIf { hours[it].first >= 60 }
    if (peak != null) {
        Text(
            "Most around ${"%02d".format(peak)}:00, ${time(hours[peak].first)} on an average day.",
            style = MaterialTheme.typography.bodySmall,
            color = c.textMuted,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun AppRows(apps: List<AppUse>, labels: Map<String, String>) {
    val c = Obsidian.colors
    apps.forEach { app ->
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (app.distracting) c.accent else Color.Transparent))
            Spacer(Modifier.width(10.dp))
            Text(labels[app.pkg] ?: app.pkg, style = MaterialTheme.typography.bodyMedium, color = c.textNormal, modifier = Modifier.weight(1f))
            Text(
                time(app.seconds) + if (app.opens > 0) " · ${app.opens}×" else "",
                style = MaterialTheme.typography.bodySmall,
                color = c.textMuted,
            )
        }
    }
}

/** A figure this week and last, and which way it went. */
@Composable
private fun TrendRow(label: String, now: Long, then: Long, format: (Long) -> String) {
    val c = Obsidian.colors
    val change = if (then > 0) ((now - then) * 100 / then).toInt() else null
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.End) {
            Text(format(now), style = MaterialTheme.typography.bodyMedium, color = c.textNormal)
            Text(
                when {
                    change == null -> "nothing last week"
                    change == 0 -> "the same as last week"
                    change < 0 -> "${-change}% less than last week"
                    else -> "$change% more than last week"
                },
                style = MaterialTheme.typography.labelSmall,
                color = if (change != null && change > 0) c.accentText else c.textFaint,
            )
        }
    }
}
