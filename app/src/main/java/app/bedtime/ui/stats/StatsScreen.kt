package app.bedtime.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.SectionLabel
import app.bedtime.ui.components.UseDays
import app.bedtime.ui.components.rememberUseDays
import app.bedtime.apps.AppCatalog
import app.bedtime.data.HourRule
import app.bedtime.data.HourlyUse
import app.bedtime.service.UsageReader
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.bedtime.data.AppLimit
import app.bedtime.data.Repository
import app.bedtime.data.SessionLog
import app.bedtime.data.UsageDay
import app.bedtime.engine.DayBar
import app.bedtime.engine.DayMark
import app.bedtime.engine.Limits
import app.bedtime.engine.Stats
import app.bedtime.engine.WeekStats
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.Text
import app.bedtime.ui.formatMinutes
import app.bedtime.ui.theme.Obsidian
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.ceil

/**
 * Your days: phone use first (today, day by day, apps, hours, this week against last), then time in
 * refuge and in limited apps.
 */
@Composable
fun StatsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val history by repo.history.collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val usage by repo.usage.collectAsStateWithLifecycle(initialValue = emptyList())
    val use = rememberUseDays(14, live = false)
    val packages = use.days.values.flatMap { it.apps.keys }.toSet()
    val labels by produceState(emptyMap<String, String>(), packages) {
        value = withContext(Dispatchers.IO) { packages.associateWith { AppCatalog.label(context, it) } }
    }
    StatsContent(
        history = history,
        limits = settings?.limits.orEmpty(),
        usage = usage,
        now = System.currentTimeMillis(),
        zone = ZoneId.systemDefault(),
        onBack = onBack,
        use = use,
        rule = settings?.let { HourlyUse.rule(it) } ?: HourRule(5),
        labels = labels,
        onAllowAccess = { runCatching { context.startActivity(UsageReader.accessIntent(context)) } },
    )
}

/** The one line home shows: the week's time in refuge and the current run, or null with nothing yet. */
fun homeSummary(week: WeekStats, streak: Int): String? {
    if (week.protectedMinutes == 0L && streak == 0) return null
    val run = when (streak) {
        0 -> null
        1 -> "1 kept"
        else -> "$streak kept in a row"
    }
    return listOfNotNull("${formatMinutes(week.protectedMinutes)} in refuge this week", run).joinToString(" · ")
}

@Composable
internal fun StatsContent(
    history: List<SessionLog>,
    limits: List<AppLimit>,
    usage: List<UsageDay>,
    now: Long,
    zone: ZoneId,
    onBack: () -> Unit,
    /** Phone use, and what makes an hour a distracted one; no phone sections without it. */
    use: UseDays? = null,
    rule: HourRule = HourRule(5),
    labels: Map<String, String> = emptyMap(),
    onAllowAccess: (() -> Unit)? = null,
) {
    val c = Obsidian.colors
    val week = Stats.week(history, now)
    val streak = Stats.streak(history, now)
    val best = Stats.bestStreak(history, now)
    val bars = Stats.daily(history, now, zone)
    val bySchedule = Stats.bySchedule(history, now)
    val today = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate().toEpochDay()
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Your days", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            if (use != null) {
                val nowHour = java.time.Instant.ofEpochMilli(now).atZone(zone).hour
                PhoneUseSections(use, today, nowHour, rule, labels, onAllowAccess)
                SectionLabel("Time in refuge")
            }
            SectionCard {
                Text(formatMinutes(week.protectedMinutes), fontSize = 32.sp, fontWeight = FontWeight.Light, color = c.textNormal)
                Text("in refuge in the last seven days", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth()) {
                    Figure("$streak", "kept in a row", Modifier.weight(1f))
                    Figure("$best", "best run", Modifier.weight(1f))
                    Figure("${week.kept} of ${week.finished}", "kept this week", Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    if (week.escapes == 0) {
                        "no ways out taken this week"
                    } else {
                        listOf(
                            week.unlocks to ("early unlock" to "early unlocks"),
                            week.breaks to ("emergency break" to "emergency breaks"),
                            week.switchedOff to ("time blocking was switched off" to "times blocking was switched off"),
                        ).filter { it.first > 0 }.joinToString(" · ") { (n, words) -> "$n ${if (n == 1) words.first else words.second}" }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textMuted,
                )
            }
            SectionCard(title = "Two weeks", subtitle = "Time in refuge each day, by the day a session began. Tap a day for its figures.") {
                DayBars(bars)
            }
            if (bySchedule.isNotEmpty()) {
                SectionCard(title = "Each schedule and block", subtitle = "The last four weeks") {
                    bySchedule.forEach { s ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(s.name, style = MaterialTheme.typography.bodyMedium, color = c.textNormal, modifier = Modifier.weight(1f))
                            Text("${s.kept} of ${s.sessions} kept", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                        }
                    }
                }
            }
            val withTime = limits.map { it to Limits.week(it, usage, today) }.filter { it.second.first > 0 || it.second.second > 0 }
            if (withTime.isNotEmpty()) {
                SectionCard(title = "Time in limited apps", subtitle = "The last seven days") {
                    withTime.forEach { (limit, figures) ->
                        val (seconds, extras) = figures
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                limit.name.ifBlank { "unnamed limit" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.textNormal,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                formatMinutes(seconds / 60) + if (extras > 0) " · more time $extras×" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textMuted,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun Figure(value: String, label: String, modifier: Modifier) {
    val c = Obsidian.colors
    Column(modifier) {
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.Light, color = c.textNormal)
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.textFaint)
    }
}

/** Kept and escaped, validated against the card for colour-blind separation, chroma and contrast. */
@Composable
private fun barColours(): Pair<Color, Color> = Obsidian.colors.accent to Color(0xFFD9762B)

private val DAY = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())

/**
 * A column per day, as tall as the time in refuge, green when every session held and orange when a
 * way out was taken. Tapping a day picks it out and spells it out underneath.
 */
@Composable
private fun DayBars(bars: List<DayBar>) {
    val c = Obsidian.colors
    val (kept, escaped) = barColours()
    val measurer = rememberTextMeasurer()
    var selected by rememberSaveable { mutableIntStateOf(bars.lastIndex) }
    val maxMinutes = bars.maxOfOrNull { it.minutes } ?: 0L
    // A clean top for the scale: whole hours, even ones past four.
    val topHours = ceil(maxMinutes / 60.0).toInt().coerceAtLeast(1).let { if (it > 4 && it % 2 == 1) it + 1 else it }
    val axisStyle = TextStyle(fontSize = 10.sp, color = c.textFaint)
    val labelStyle = TextStyle(fontSize = 11.sp, color = c.textFaint)
    val pickedStyle = labelStyle.copy(color = c.textNormal, fontWeight = FontWeight.SemiBold)
    val gridColor = c.border.copy(alpha = 0.7f)
    val chosen = bars.getOrNull(selected)
    val summary = bars.joinToString("; ") { "${DAY.format(it.date)}: ${formatMinutes(it.minutes)}, ${markWords(it.mark)}" }

    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendKey(kept, "kept")
        LegendKey(escaped, "a way out taken")
    }
    Spacer(Modifier.height(14.dp))
    Canvas(
        Modifier.fillMaxWidth().height(170.dp)
            .semantics { contentDescription = "time in refuge each day. $summary" }
            .pointerInput(bars) {
                detectTapGestures { offset ->
                    val axis = 34.dp.toPx()
                    val slot = (size.width - axis) / bars.size
                    selected = ((offset.x - axis) / slot).toInt().coerceIn(0, bars.lastIndex)
                }
            },
    ) {
        val axis = 34.dp.toPx()
        val labelsHeight = 20.dp.toPx()
        val plotTop = 8.dp.toPx()
        val plotBottom = size.height - labelsHeight
        val plotHeight = plotBottom - plotTop
        val slot = (size.width - axis) / bars.size
        val barWidth = minOf(slot - 2.dp.toPx(), 24.dp.toPx()) * 0.72f
        val radius = 4.dp.toPx()

        // Recessive scale: a hairline at nothing, half way and the top, each named on the left.
        listOf(0, topHours / 2, topHours).distinct().forEach { hours ->
            val y = plotBottom - plotHeight * hours / topHours
            drawLine(gridColor, Offset(axis, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            val text = measurer.measure(if (hours == 0) "0" else "$hours h", axisStyle)
            drawText(text, topLeft = Offset(axis - text.size.width - 6.dp.toPx(), y - text.size.height / 2f))
        }

        bars.forEachIndexed { i, bar ->
            val centre = axis + slot * i + slot / 2
            if (bar.minutes > 0) {
                val height = (plotHeight * bar.minutes / (topHours * 60f)).coerceAtLeast(3.dp.toPx())
                val colour = if (bar.mark == DayMark.ESCAPED) escaped else kept
                // Rounded at the data end, square at the baseline.
                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = centre - barWidth / 2,
                            top = plotBottom - height,
                            right = centre + barWidth / 2,
                            bottom = plotBottom,
                            topLeftCornerRadius = CornerRadius(radius),
                            topRightCornerRadius = CornerRadius(radius),
                            bottomLeftCornerRadius = CornerRadius.Zero,
                            bottomRightCornerRadius = CornerRadius.Zero,
                        ),
                    )
                }
                drawPath(path, colour)
            }
            if (i == selected) {
                // The picked day is marked under its column, whether or not it had a bar.
                drawRect(c.textNormal, Offset(centre - barWidth / 2, plotBottom + 2.dp.toPx()), Size(barWidth, 2.dp.toPx()))
            }
            val initial = DateTimeFormatter.ofPattern("EEEEE", Locale.getDefault()).format(bar.date).lowercase()
            val label = measurer.measure(initial, if (i == selected) pickedStyle else labelStyle)
            drawText(label, topLeft = Offset(centre - label.size.width / 2f, plotBottom + 6.dp.toPx()))
        }
    }
    Spacer(Modifier.height(10.dp))
    if (chosen != null) {
        Text(
            buildString {
                append(if (selected == bars.lastIndex) "today" else DAY.format(chosen.date).lowercase())
                if (chosen.mark == DayMark.NONE) {
                    append(" · nothing on")
                } else {
                    append(" · ${formatMinutes(chosen.minutes)} · ${markWords(chosen.mark)}")
                }
            },
            style = MaterialTheme.typography.bodyMedium,
            color = c.textNormal,
        )
    }
}

private fun markWords(mark: DayMark): String = when (mark) {
    DayMark.KEPT -> "kept"
    DayMark.ESCAPED -> "a way out taken"
    DayMark.NONE -> "nothing on"
}

@Composable
private fun LegendKey(colour: Color, label: String) {
    val c = Obsidian.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(width = 10.dp, height = 10.dp).clip(RoundedCornerShape(2.dp)).background(colour))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.textMuted)
    }
}
