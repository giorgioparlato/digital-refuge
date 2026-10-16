package app.bedtime.ui.limits

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val usage by repo.usage.collectAsStateWithLifecycle(initialValue = emptyList())
    val loaded = settings ?: return
    LimitsContent(
        limits = loaded.limits,
        usage = usage,
        today = LocalDate.now().toEpochDay(),
        onEdit = onEdit,
        onCreate = onCreate,
    )
}

@Composable
internal fun LimitsContent(
    limits: List<AppLimit>,
    usage: List<UsageDay>,
    today: Long,
    onEdit: (String) -> Unit,
    onCreate: () -> Unit,
) {
    val c = Obsidian.colors
    val todayUsage = usage.lastOrNull { it.day == today } ?: UsageDay(today)
    Scaffold(
        containerColor = c.bgPrimary,
        topBar = { ObsidianTopBar("limits", subtitle = "every day, not only in a session") },
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
    val shape = RoundedCornerShape(CardRadius)
    val rules = limit.rules
    Column(
        Modifier.fillMaxWidth().clip(shape).background(c.bgSecondary).border(1.dp, c.border, shape)
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
