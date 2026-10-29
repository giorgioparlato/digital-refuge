package app.bedtime.ui.limits

import kotlinx.coroutines.launch

import androidx.compose.runtime.rememberCoroutineScope

import app.bedtime.ui.components.SegmentedChoice

import app.bedtime.ui.components.SectionCard

import app.bedtime.ui.components.SectionLabel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.ui.components.AddCircleButton
import app.bedtime.ui.components.CardRadius
import app.bedtime.data.AppLimit
import app.bedtime.data.Repository
import app.bedtime.data.UsageDay
import app.bedtime.engine.Limits
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.formatMinutes
import app.bedtime.ui.formatWaitSeconds
import app.bedtime.ui.pluralApps
import app.bedtime.ui.theme.Obsidian
import java.time.LocalDate

/** The limits tab: limits that hold every day. */
@Composable
fun LimitsScreen(onEdit: (String) -> Unit, onCreate: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val usage by repo.usage.collectAsStateWithLifecycle(initialValue = emptyList())
    val loaded = settings ?: return
    LimitsContent(
        limits = loaded.limits,
        usage = usage,
        today = LocalDate.now().toEpochDay(),
        onEdit = onEdit,
        onCreate = onCreate,
        runNudge = loaded.runNudgeMinutes,
        onRunNudge = { m -> scope.launch { repo.updateSettings { it.copy(runNudgeMinutes = m) } } },
    )
}

@Composable
internal fun LimitsContent(
    limits: List<AppLimit>,
    usage: List<UsageDay>,
    today: Long,
    onEdit: (String) -> Unit,
    onCreate: () -> Unit,
    /** Minutes in a row on the phone before a nudge, or null for none. */
    runNudge: Int? = null,
    onRunNudge: (Int?) -> Unit = {},
) {
    val c = Obsidian.colors
    val todayUsage = usage.lastOrNull { it.day == today } ?: UsageDay(today)
    Scaffold(
        containerColor = c.bgPrimary,
        topBar = { ObsidianTopBar("limits", subtitle = "every day, not only in a session") },
        floatingActionButton = {
            if (limits.isNotEmpty()) {
                AddCircleButton("new limit", onClick = onCreate)
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = PageGutter, end = PageGutter, top = 4.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
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
            // Not one app's limit but the whole phone's: how long in a row before a nudge.
            item(key = "nudge") {
                SectionLabel("the whole phone", Modifier.padding(top = 8.dp))
                SectionCard(title = "A nudge after time in a row", subtitle = "After so long on the phone without the screen going off, in any app, a full screen asks if it's still what you meant to do. Never during a call") {
                    val nudges = listOf<Int?>(null, 5, 10, 15, 20, 30, 45)
                    SegmentedChoice(
                        options = nudges.map { it?.toString() ?: "Off" },
                        selected = nudges.indexOf(runNudge).coerceAtLeast(0),
                        onSelect = { onRunNudge(nudges[it]) },
                    )
                    Text("Minutes", style = MaterialTheme.typography.labelMedium, color = c.textMuted, modifier = Modifier.padding(top = 8.dp))
                }
            }
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
        rules.dailyOpens?.let { add(opensADay(it) + if (!rules.shared && rules.apps.size > 1) " each" else "") }
    }.joinToString(" · ")
}

/**
 * A limit as a card that fills like a tide as the day's minutes go: what's left on the right, and
 * underneath the limit itself in a line. No border; the fill and the card's own tone carry it.
 */
@Composable
private fun LimitCard(limit: AppLimit, today: UsageDay, todayDay: Long, onClick: () -> Unit) {
    val c = Obsidian.colors
    val shape = RoundedCornerShape(CardRadius)
    val rules = limit.rules
    val budget = rules.dailyMinutes
    val lifted = limit.id in today.lifted
    val extra = Limits.extraMinutes(limit, today)
    val total = (budget ?: 0) + extra
    val used = Limits.usedSeconds(limit, today) / 60
    val pause = rules.pauseSeconds?.takeIf { it > 0 }?.let { "${formatWaitSeconds(it)} pause" }
    val tide = if (budget == null || lifted) 0f else (used.toFloat() / total.coerceAtLeast(1)).coerceIn(0f, 1f)
    val opensLeft = Limits.opensLeft(limit, rules.apps.firstOrNull().orEmpty(), today)?.takeIf { rules.shared || rules.apps.size == 1 }
    val left = when {
        budget == null && opensLeft != null -> if (opensLeft <= 0) "no opens left" else if (opensLeft == 1) "1 open left" else "$opensLeft opens left"
        budget == null -> pause.orEmpty()
        lifted -> "lifted today"
        used >= total -> "none left"
        else -> "${formatMinutes(total - used)} left"
    }
    val details = buildList {
        budget?.let { add("${formatMinutes(it.toLong())} a day" + if (!rules.shared && rules.apps.size > 1) " each" else "") }
        rules.dailyOpens?.let { add(opensADay(it) + if (!rules.shared && rules.apps.size > 1) " each" else "") }
        if (budget == null || rules.apps.size > 1) add(pluralApps(rules.apps.size))
        if (budget != null && pause != null) add(pause)
        if (extra > 0) add("$extra extra today")
        when {
            limit.deleteFromDay != null -> add("ends at midnight")
            limit.next != null && limit.nextFromDay > todayDay -> add("looser from tomorrow")
        }
    }.joinToString(" · ")
    Box(
        Modifier.fillMaxWidth().clip(shape).background(c.bgSecondary)
            .clickable(onClickLabel = "Edit ${limit.name}", onClick = onClick),
    ) {
        if (tide > 0f) {
            Box(Modifier.matchParentSize()) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(tide).background(c.accent.copy(alpha = 0.16f)))
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    limit.name.ifBlank { "unnamed limit" },
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = c.textNormal,
                    modifier = Modifier.weight(1f),
                )
                Text(left, style = MaterialTheme.typography.bodyMedium, color = c.textNormal)
            }
            Spacer(Modifier.height(2.dp))
            Text(details, fontSize = 13.sp, lineHeight = 18.sp, color = c.textSoft)
        }
    }
}
