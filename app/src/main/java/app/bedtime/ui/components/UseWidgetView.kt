package app.bedtime.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.bedtime.apps.AppCatalog
import app.bedtime.data.HomeStyle
import app.bedtime.data.HourState
import app.bedtime.data.UseLook
import app.bedtime.data.UseToday
import app.bedtime.data.UseWidget
import app.bedtime.data.UseWords
import app.bedtime.ui.homestyle.palette

/**
 * The distraction tracker in the look chosen for this screen: the hour dots (with a small line of
 * figures under them, if any are chosen), a line, a budget bar, or today's total and the apps that took
 * the most. A tap opens today in detail; a long press, the widget's own sheet.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UseWidgetView(
    widget: UseWidget,
    states: List<HourState>?,
    today: UseToday?,
    style: HomeStyle,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val p = style.palette()
    val calm = style.hourCalmColor?.let { Color(it) } ?: p.text.copy(alpha = 0.55f)
    val distracted = style.hourDistractedColor?.let { Color(it) } ?: p.accent
    val tappable = if (onClick != null || onLongClick != null) {
        Modifier.clip(RoundedCornerShape(10.dp)).combinedClickable(onClickLabel = "today on the phone", onLongClick = onLongClick, onClick = onClick ?: {})
    } else {
        Modifier
    }
    val small = MaterialTheme.typography.labelMedium
    val unlocks = today?.unlocks?.takeIf { widget.unlocks }?.let(UseWords::unlocks)
    when (if (today == null) UseLook.DOTS else widget.look) {
        UseLook.DOTS -> Column(modifier) {
            if (states != null) HourDots(states, p.text, p.accent, calmColor = style.hourCalmColor, distractedColor = style.hourDistractedColor, onClick = onClick, onLongClick = onLongClick)
            today?.let { UseWords.underDots(widget, it) }?.let { Text(it, style = small, color = p.faint, modifier = Modifier.padding(top = 2.dp)) }
        }
        UseLook.LINE -> Text(UseWords.line(widget, today!!), style = MaterialTheme.typography.bodyLarge, color = p.muted, modifier = modifier.fillMaxWidth().then(tappable).padding(vertical = 6.dp))
        UseLook.BUDGET -> Column(modifier.fillMaxWidth().then(tappable).padding(vertical = 6.dp)) {
            val budget = UseWords.budget(widget, today!!)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(budget.line, style = MaterialTheme.typography.bodyLarge, color = p.muted, modifier = Modifier.weight(1f))
                Text(budget.note, style = small, color = if (budget.over) distracted else p.faint)
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)).background(p.text.copy(alpha = 0.12f))) {
                Box(Modifier.fillMaxWidth(budget.filled).height(5.dp).clip(RoundedCornerShape(50)).background(if (budget.over) distracted else calm))
            }
            unlocks?.let { Text(it, style = small, color = p.faint, modifier = Modifier.padding(top = 8.dp)) }
        }
        UseLook.APPS -> Column(modifier.fillMaxWidth().then(tappable).padding(vertical = 6.dp)) {
            val context = LocalContext.current
            val top = UseWords.top(today!!)
            val labels = remember(top.map { it.first }) { top.associate { (pkg, _) -> pkg to AppCatalog.label(context, pkg) } }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(UseWords.short(today.total), style = MaterialTheme.typography.headlineSmall, color = p.text)
                Spacer(Modifier.width(10.dp))
                Text("on the phone today", style = MaterialTheme.typography.bodyMedium, color = p.faint, modifier = Modifier.padding(bottom = 3.dp).weight(1f))
                unlocks?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = p.muted, modifier = Modifier.padding(bottom = 3.dp)) }
            }
            if (top.isNotEmpty()) Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val most = top.firstOrNull()?.second?.coerceAtLeast(1) ?: 1
                top.forEach { (pkg, seconds) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AppIcon(pkg, labels[pkg] ?: pkg, 22.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Row {
                                VerbatimText(labels[pkg] ?: pkg, style = MaterialTheme.typography.bodyMedium, color = p.text, modifier = Modifier.weight(1f))
                                Text(UseWords.short(seconds), style = MaterialTheme.typography.bodyMedium, color = p.muted)
                            }
                            Spacer(Modifier.height(4.dp))
                            Box(Modifier.fillMaxWidth(seconds.toFloat() / most).height(3.dp).clip(RoundedCornerShape(50)).background(distracted.copy(alpha = 0.7f)))
                        }
                    }
                }
            }
        }
        UseLook.APPS_LINE -> {
            val context = LocalContext.current
            val top = UseWords.top(today!!).map { it.first }
            val labels = remember(top) { top.associateWith { AppCatalog.label(context, it) } }
            Text(
                UseWords.appsLine(widget, today) { labels[it] ?: it },
                style = MaterialTheme.typography.bodyLarge,
                color = p.muted,
                modifier = modifier.fillMaxWidth().then(tappable).padding(vertical = 6.dp),
            )
        }
    }
}

/** The tracker for [SessionHeader]'s slot, or null when it has nothing to show: no dots counted, and no other look. */
fun useWidgetSlot(
    widget: UseWidget,
    states: List<HourState>?,
    today: UseToday?,
    style: HomeStyle,
    onClick: (() -> Unit)?,
    onLongClick: (() -> Unit)?,
): (@Composable () -> Unit)? {
    if (states == null && (widget.look == UseLook.DOTS || today == null)) return null
    return { UseWidgetView(widget, states, today, style, Modifier.padding(top = 12.dp), onClick, onLongClick) }
}
