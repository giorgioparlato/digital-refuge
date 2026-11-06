package app.bedtime.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.bedtime.data.MoodMark
import app.bedtime.data.SessionLog
import app.bedtime.data.UseDay
import app.bedtime.engine.SessionInsights
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.Text
import app.bedtime.ui.formatMinutes
import app.bedtime.ui.theme.Obsidian
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

/**
 * Sessions by what they did: what they give you (nights without the phone, phone time spared), how
 * often they hold week by week, when and why they break, what follows them, and blocks' focus. For
 * all of them, or one schedule or block picked. A card with nothing to say yet doesn't show.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SessionStats(
    history: List<SessionLog>,
    days: Map<Long, UseDay>,
    marks: List<MoodMark>,
    blocks: Set<String>,
    now: Long,
    zone: ZoneId,
    /** Android's app-by-app record of the last days, for the apps used in sessions; and how to name an app. */
    events: List<app.bedtime.data.UseEvent> = emptyList(),
    excluded: Set<String> = emptySet(),
    label: (String) -> String = { it },
    /** Another page: a schedule's own, to change what it lets through. */
    onOpen: (String) -> Unit = {},
) {
    val c = Obsidian.colors
    val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
    val since = today.minusWeeks(8)
    val recent = history.filter { it.end <= now && !Instant.ofEpochMilli(it.start).atZone(zone).toLocalDate().isBefore(since) }
    val bySchedule = recent.groupBy { it.scheduleId }
    val names = bySchedule.mapValues { (_, logs) -> logs.maxBy { it.start }.name.ifBlank { "a session" }.lowercase() }
    var chosen by rememberSaveable { mutableStateOf<String?>(null) }
    val pick = chosen?.takeIf { it in bySchedule }
    if (bySchedule.size > 1) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            (listOf<String?>(null) + bySchedule.keys).forEach { id ->
                val on = id == pick
                Text(
                    id?.let { names[it] } ?: "all",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) c.bgPrimary else c.textMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (on) c.accent else Color.Transparent)
                        .border(1.dp, if (on) c.accent else c.border, RoundedCornerShape(50))
                        .clickable { chosen = id }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }
    val picked = if (pick == null) recent else bySchedule[pick].orEmpty()
    val groups = if (pick == null) bySchedule else bySchedule.filterKeys { it == pick }
    val single = pick != null || bySchedule.size == 1

    // What it gives you: nights without the phone, and phone time spared.
    val nights = if (pick == null || SessionInsights.atNight(picked, zone)) SessionInsights.nights(days, today) else emptyList()
    val thisWeek = nights.filter { it.morning.isAfter(today.minusDays(7)) }
    val before = nights - thisWeek.toSet()
    val saved = groups.mapNotNull { (id, logs) -> SessionInsights.saved(logs, days, zone)?.let { Triple(names[id].orEmpty(), SessionInsights.atNight(logs, zone), it) } }
    if (thisWeek.isNotEmpty() || saved.isNotEmpty()) {
        SectionCard(title = "What it gives you", subtitle = "This week; and phone time in sessions against the same hours without") {
            if (thisWeek.isNotEmpty()) {
                val hours = thisWeek.map { it.hours }.average()
                val change = before.takeIf { it.isNotEmpty() }?.let { b -> ((hours - b.map { it.hours }.average()) * 60).toLong() }
                val last = thisWeek.map { if (it.lastHour < 12) it.lastHour + 24 else it.lastHour }.average().toInt() % 24
                val first = thisWeek.map { it.firstHour }.average().toInt()
                Line(
                    "Phone-free nights",
                    "~${formatMinutes((hours * 60).toLong())}" + (change?.takeIf { kotlin.math.abs(it) >= 15 }?.let { if (it > 0) " · ${formatMinutes(it)} more than before" else " · ${formatMinutes(-it)} less than before" } ?: ""),
                    "last used around ${"%02d".format(last)}:00, first again around ${"%02d".format(first)}:00",
                )
            }
            saved.forEach { (name, night, cmp) ->
                val spared = cmp.without - cmp.with
                Line(
                    if (single) "Phone time it spares" else name,
                    if (spared >= 60) "${formatMinutes(spared / 60)} ${if (night) "a night" else "each time"}" else "no less than without it",
                    "${formatMinutes(cmp.with / 60)} on the phone during it, ${formatMinutes(cmp.without / 60)} in the same hours without",
                )
            }
        }
    }

    // Kept, week by week.
    val weeks = SessionInsights.keptByWeek(picked, today, zone)
    if (weeks.any { it != null }) {
        SectionCard(title = "Kept, week by week", subtitle = "The share of sessions with no way out taken, over the last eight weeks") {
            val now7 = picked.filter { !Instant.ofEpochMilli(it.start).atZone(zone).toLocalDate().isBefore(today.minusDays(6)) }
            Row(verticalAlignment = Alignment.Bottom) {
                KeptBars(weeks, 44)
                Text(
                    if (now7.isEmpty()) "none this week" else "${now7.count { it.escapes == 0 }} of ${now7.size} kept this week",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textNormal,
                    modifier = Modifier.padding(start = 14.dp),
                )
            }
            if (!single) {
                groups.forEach { (id, logs) ->
                    val own = SessionInsights.keptByWeek(logs, today, zone)
                    Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(names[id].orEmpty(), style = MaterialTheme.typography.bodySmall, color = c.textMuted, modifier = Modifier.weight(1f))
                        KeptBars(own, 16)
                        Text(
                            "${logs.count { it.escapes == 0 } * 100 / logs.size}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = c.textNormal,
                            modifier = Modifier.width(44.dp).padding(start = 8.dp),
                        )
                    }
                }
            }
        }
    }

    // When you break.
    val breaks = SessionInsights.breaks(picked, zone)
    if (breaks.total >= 3) {
        SectionCard(title = "When you break", subtitle = "Ways out taken, by how far into a session; the last eight weeks") {
            val most = breaks.byMinutes.max().coerceAtLeast(1)
            listOf("first 15 min", "15 to 30 min", "30 to 60 min", "after an hour").zip(breaks.byMinutes).forEach { (label, n) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = MaterialTheme.typography.bodySmall, color = c.textMuted, modifier = Modifier.width(96.dp))
                    Box(Modifier.weight(1f)) {
                        if (n > 0) Box(Modifier.fillMaxWidth(n.toFloat() / most).height(10.dp).clip(RoundedCornerShape(50)).background(Color(0xFFD9762B)))
                    }
                    Text("$n", style = MaterialTheme.typography.labelMedium, color = c.textNormal, modifier = Modifier.width(28.dp).padding(start = 8.dp))
                }
            }
            val top = breaks.byDay.entries.sortedByDescending { it.value }.take(2).filter { it.value >= 2 }
            if (top.isNotEmpty()) {
                Text(
                    "most often on " + top.joinToString(" and ") { it.key.getDisplayName(TextStyle.FULL, Locale.getDefault()).lowercase() + "s" },
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textFaint,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
    }

    // Why you break.
    SessionInsights.why(picked, marks)?.let { why ->
        SectionCard(title = "Why you break", subtitle = "The moods “why?” heard inside sessions") {
            if (why.moods.isNotEmpty()) Text(why.moods.joinToString(" · ") { (mood, n) -> "$mood $n" }, style = MaterialTheme.typography.bodyLarge, color = c.textNormal)
            if (why.answers > 0) {
                Text("put the phone down ${why.putDown} of ${why.answers} times", style = MaterialTheme.typography.bodySmall, color = c.textMuted, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }

    // After it ends.
    val after = groups.mapNotNull { (id, logs) -> SessionInsights.rebound(logs, days, zone)?.let { names[id].orEmpty() to it } }
    if (after.isNotEmpty()) {
        SectionCard(title = "After it ends", subtitle = "The phone in the hour after, against that hour on days without") {
            after.forEach { (name, cmp) ->
                Line(
                    if (single) "The hour after" else name,
                    "${formatMinutes(cmp.with / 60)}, against ${formatMinutes(cmp.without / 60)} usually",
                    if (cmp.with > cmp.without + 300) "more than usual: some of it comes back after" else null,
                )
            }
        }
    }

    // The apps used in them: let through, or after a way out; worth a thought for blocking too.
    val apps = androidx.compose.runtime.remember(picked, events) { SessionInsights.apps(picked, events, zone, excluded) }
    if (apps.isNotEmpty()) {
        SectionCard(title = "Apps used in them", subtitle = "The last days, as far back as Android keeps them: let through, or after a way out") {
            var all by rememberSaveable { mutableStateOf(false) }
            (if (all) apps else apps.take(6)).forEach { (pkg, seconds) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label(pkg), style = MaterialTheme.typography.bodyMedium, color = c.textNormal, modifier = Modifier.weight(1f))
                    Text(formatMinutes(seconds / 60), style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                }
            }
            if (apps.size > 6) {
                Text(
                    if (all) "Fewer" else "All ${apps.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.accentText,
                    modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(8.dp)).clickable { all = !all }.padding(vertical = 6.dp),
                )
            }
            // One picked: straight to what it lets through.
            if (pick != null) {
                Text(
                    "Change what ${names[pick]} lets through ›",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.accentText,
                    modifier = Modifier.padding(top = 6.dp).clip(RoundedCornerShape(8.dp)).clickable { onOpen(app.bedtime.Routes.EDIT + pick) }.padding(vertical = 6.dp),
                )
            } else {
                Text("Pick a schedule above to change what it lets through.", style = MaterialTheme.typography.bodySmall, color = c.textFaint, modifier = Modifier.padding(top = 6.dp))
            }
        }
    }

    // Blocks.
    SessionInsights.blocks(picked.filter { it.scheduleId in blocks }, today, zone)?.let { b ->
        SectionCard(title = "Blocks", subtitle = "Focus, this week against last; over the last eight weeks, the longest and when they hold") {
            val change = b.thisWeek - b.lastWeek
            Line(
                "Focused this week",
                formatMinutes(b.thisWeek) + if (b.lastWeek > 0 && kotlin.math.abs(change) >= 10) (if (change > 0) " · ${formatMinutes(change)} more" else " · ${formatMinutes(-change)} less") else "",
                "the longest ${formatMinutes(b.longest)}",
            )
            b.best?.let { (part, kept, of) ->
                Text("they hold best in the $part: $kept of $of kept", style = MaterialTheme.typography.bodySmall, color = c.textMuted, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun Line(what: String, how: String, detail: String?) {
    val c = Obsidian.colors
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(what, style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.weight(1f).padding(end = 10.dp))
            Text(how, style = MaterialTheme.typography.bodyLarge, color = c.textNormal)
        }
        detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.textFaint) }
    }
}

/** Eight weeks of kept, a bar each, as tall as the share kept; a week without sessions, a dash. */
@Composable
private fun KeptBars(weeks: List<Float?>, height: Int) {
    val c = Obsidian.colors
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom) {
        weeks.forEach { share ->
            Box(
                Modifier
                    .width(if (height > 20) 10.dp else 6.dp)
                    .height(if (share == null) 2.dp else (height * share).coerceAtLeast(2f).dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (share == null) c.border else c.accent.copy(alpha = 0.4f + 0.6f * share)),
            )
        }
    }
}

/** The figures as they were, in one quiet line at the foot: time in refuge, kept in a row, the best run. */
@Composable
internal fun SessionTotals(protectedMinutes: Long, streak: Int, best: Int) {
    val c = Obsidian.colors
    Text(
        "in refuge this week ${formatMinutes(protectedMinutes)} · kept in a row $streak · best run $best",
        style = MaterialTheme.typography.bodySmall,
        color = c.textFaint,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
    )
}
