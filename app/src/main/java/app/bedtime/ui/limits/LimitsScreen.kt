package app.bedtime.ui.limits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.AppLimit
import app.bedtime.data.Repository
import app.bedtime.data.SessionLog
import app.bedtime.data.UsageDay
import app.bedtime.engine.DayMark
import app.bedtime.engine.Limits
import app.bedtime.engine.Stats
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.Text
import app.bedtime.ui.formatMinutes
import app.bedtime.ui.formatWaitSeconds
import app.bedtime.ui.pluralApps
import app.bedtime.ui.theme.Obsidian
import java.time.LocalDate
import java.time.ZoneId

/** The limits tab: limits that hold every day, then how things have been going. */
@Composable
fun LimitsScreen(onEdit: (String) -> Unit, onCreate: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val usage by repo.usage.collectAsStateWithLifecycle(initialValue = emptyList())
    val history by repo.history.collectAsStateWithLifecycle(initialValue = emptyList())
    val loaded = settings ?: return
    LimitsContent(
        limits = loaded.limits,
        usage = usage,
        history = history,
        now = System.currentTimeMillis(),
        today = LocalDate.now().toEpochDay(),
        onEdit = onEdit,
        onCreate = onCreate,
    )
}

@Composable
internal fun LimitsContent(
    limits: List<AppLimit>,
    usage: List<UsageDay>,
    history: List<SessionLog>,
    now: Long,
    today: Long,
    onEdit: (String) -> Unit,
    onCreate: () -> Unit,
) {
    val c = Obsidian.colors
    val todayUsage = usage.lastOrNull { it.day == today } ?: UsageDay(today)
    Scaffold(
        containerColor = c.bgPrimary,
        floatingActionButton = {
            if (limits.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    text = { Text("new limit", fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = onCreate,
                    shape = RoundedCornerShape(16.dp),
                    containerColor = c.accentFill,
                    contentColor = c.textOnAccent,
                )
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "header") {
                Column(Modifier.padding(bottom = 6.dp)) {
                    Text("limits", fontSize = 27.sp, fontWeight = FontWeight.Light, color = c.textNormal)
                    Spacer(Modifier.height(6.dp))
                    Text("every day, not only in a session", style = MaterialTheme.typography.bodyMedium, color = c.textFaint)
                }
            }
            if (limits.isEmpty()) {
                item(key = "empty") {
                    Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        Text(
                            "No limits yet. A limit can ask for a few breaths before an app opens, give it a daily " +
                                "budget, or both — for the apps that pull hardest.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = c.textMuted,
                        )
                        Spacer(Modifier.height(16.dp))
                        CtaButton("Create a limit", onClick = onCreate, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            items(limits.size, key = { limits[it].id }) { index ->
                LimitCard(limits[index], todayUsage, today, onClick = { onEdit(limits[index].id) })
            }
            item(key = "stats") { RefugeStats(history, limits, usage, now, today) }
        }
    }
}

/** What a limit does, in a line. */
internal fun limitSummary(limit: AppLimit): String {
    val rules = limit.rules
    return buildList {
        add(pluralApps(rules.apps.size))
        rules.pauseSeconds?.takeIf { it > 0 }?.let { add("${formatWaitSeconds(it)} pause") }
        rules.dailyMinutes?.let { add("${formatMinutes(it.toLong())} a day" + if (!rules.shared && rules.apps.size > 1) " each" else "") }
    }.joinToString(" · ")
}

@Composable
private fun LimitCard(limit: AppLimit, today: UsageDay, todayDay: Long, onClick: () -> Unit) {
    val c = Obsidian.colors
    val shape = RoundedCornerShape(18.dp)
    val rules = limit.rules
    Column(
        Modifier.fillMaxWidth().clip(shape).background(c.bgSecondary)
            .clickable(onClickLabel = "Edit ${limit.name}", onClick = onClick)
            .padding(18.dp),
    ) {
        Text(limit.name.ifBlank { "unnamed limit" }, style = MaterialTheme.typography.titleMedium, color = c.textNormal)
        Spacer(Modifier.height(3.dp))
        Text(limitSummary(limit), style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
        val budget = rules.dailyMinutes
        if (budget != null) {
            val usedMinutes = Limits.usedSeconds(limit, today) / 60
            val extra = (today.extras[limit.id]?.size ?: 0) * rules.extraMinutes
            val total = budget + extra
            val lifted = limit.id in today.lifted
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(50)).background(c.accent.copy(alpha = 0.15f))) {
                val share = if (lifted) 1f else (usedMinutes.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)
                Box(
                    Modifier.fillMaxWidth(share).height(4.dp).clip(RoundedCornerShape(50))
                        .background(if (share >= 1f) c.orange else c.accent),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    lifted -> "lifted for the rest of today"
                    extra > 0 -> "${formatMinutes(usedMinutes)} of ${formatMinutes(total.toLong())} today, with $extra extra"
                    else -> "${formatMinutes(usedMinutes)} of ${formatMinutes(budget.toLong())} today"
                },
                style = MaterialTheme.typography.labelMedium,
                color = c.textFaint,
            )
        }
        val pending = when {
            limit.deleteFromDay != null -> "goes at midnight"
            limit.next != null && limit.nextFromDay > todayDay -> "looser rules start tomorrow"
            else -> null
        }
        if (pending != null) {
            Spacer(Modifier.height(8.dp))
            Text(pending, style = MaterialTheme.typography.labelMedium, color = c.accentText)
        }
    }
}

/** How it's been going: the week's figures, a month of days, and each schedule and limit. */
@Composable
private fun RefugeStats(history: List<SessionLog>, limits: List<AppLimit>, usage: List<UsageDay>, now: Long, today: Long) {
    val c = Obsidian.colors
    val week = Stats.week(history, now)
    val streak = Stats.streak(history, now)
    val best = Stats.bestStreak(history, now)
    val days = Stats.days(history, now, ZoneId.systemDefault())
    val bySchedule = Stats.bySchedule(history, now)
    Column(Modifier.padding(top = 18.dp)) {
        Text("your refuge", fontSize = 22.sp, fontWeight = FontWeight.Light, color = c.textNormal)
        Spacer(Modifier.height(4.dp))
        Text("the last seven days, unless it says otherwise", style = MaterialTheme.typography.bodySmall, color = c.textFaint)
        Spacer(Modifier.height(14.dp))
        SectionCard {
            Text(
                formatMinutes(week.protectedMinutes),
                fontSize = 34.sp,
                fontWeight = FontWeight.Light,
                color = c.textNormal,
            )
            Text("in refuge this week", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
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
                color = if (week.escapes == 0) c.accentText else c.textMuted,
            )
        }
        Spacer(Modifier.height(12.dp))
        SectionCard(title = "four weeks", subtitle = "a dot a day: kept, a way out taken, or nothing on") {
            days.chunked(7).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { (_, mark) ->
                        Box(
                            Modifier.width(26.dp).aspectRatio(1f).clip(CircleShape).background(
                                when (mark) {
                                    DayMark.KEPT -> c.accent
                                    DayMark.ESCAPED -> c.orange
                                    DayMark.NONE -> c.border.copy(alpha = 0.5f)
                                },
                            ),
                        )
                    }
                }
            }
            if (bySchedule.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
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
            Spacer(Modifier.height(12.dp))
            SectionCard(title = "time in limited apps") {
                withTime.forEach { (limit, week) ->
                    val (seconds, extras) = week
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(limit.name.ifBlank { "unnamed limit" }, style = MaterialTheme.typography.bodyMedium, color = c.textNormal, modifier = Modifier.weight(1f))
                        Text(
                            formatMinutes(seconds / 60) + if (extras > 0) " · $extras× more time" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textMuted,
                        )
                    }
                }
            }
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
