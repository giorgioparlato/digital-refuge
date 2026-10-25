package app.bedtime.ui.components

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import app.bedtime.ui.formatMinutes
import app.bedtime.data.DayReport
import app.bedtime.apps.AppCatalog
import app.bedtime.data.HourRule
import app.bedtime.data.DayHours
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.HourState
import app.bedtime.data.HourlyUse
import app.bedtime.data.Repository
import app.bedtime.ui.formatMinuteOfDay

/**
 * Today as 24 dots, one an hour: calm ones soft, distracted ones in [accent], this one ringed, those
 * still to come faint. A tap opens the minutes behind them.
 */
@Composable
fun HourDots(
    states: List<HourState>,
    text: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    /** Your colours for calm and distracted hours, where you've chosen them. */
    calmColor: Long? = null,
    distractedColor: Long? = null,
    onClick: (() -> Unit)? = null,
) {
    val calm = calmColor?.let { Color(it) } ?: text.copy(alpha = 0.55f)
    val distracted = distractedColor?.let { Color(it) } ?: accent
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(onClickLabel = "today by the hour", onClick = onClick) else Modifier)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        states.forEach { state ->
            val fill = when (state) {
                HourState.DISTRACTED, HourState.NOW_DISTRACTED -> distracted
                HourState.CALM, HourState.NOW -> calm
                HourState.AHEAD -> text.copy(alpha = 0.16f)
            }
            val now = state == HourState.NOW || state == HourState.NOW_DISTRACTED
            Box(Modifier.size(11.dp), contentAlignment = Alignment.Center) {
                if (now) Box(Modifier.size(11.dp).border(1.5.dp, text, CircleShape))
                Box(Modifier.size(if (now) 5.dp else 7.dp).clip(CircleShape).background(fill))
            }
        }
    }
}

/** Today's seconds by hour, in distracting apps and in any, refreshed as they're counted. */
@Composable
fun rememberTodayHours(): DayHours = rememberDayHours(HourlyUse.today()) ?: DayHours()

/** [day]'s seconds by hour and by distracting app, or null if nothing was counted that day. */
@Composable
fun rememberDayHours(day: Long): DayHours? {
    val context = LocalContext.current
    val repo = remember { Repository.get(context.applicationContext) }
    val usage by repo.usage.collectAsStateWithLifecycle(initialValue = emptyList())
    return usage.lastOrNull { it.day == day }?.let { DayHours(it.hourly, it.hourlyAll, it.distractingSeconds) }
}

/**
 * What's behind the dots: today's time in distracting apps and on the phone at all, against yesterday;
 * the distracted hours so far and the heaviest; the distracting apps used most; and each hour.
 */
@Composable
fun HourDetailDialog(hours: DayHours, rule: HourRule, background: Color, text: Color, faint: Color, accent: Color, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val yesterday = rememberDayHours(HourlyUse.today() - 1)
    val nowHour = remember { java.time.LocalTime.now().hour }
    val report = HourlyUse.report(hours, yesterday, rule, nowHour)
    val labels = remember(report.topApps) { report.topApps.associate { (pkg, _) -> pkg to AppCatalog.label(context, pkg) } }
    Dialog(onDismissRequest = onDismiss) {
        HourDetailContent(report, hours, rule, labels, background, text, faint, accent)
    }
}

@Composable
fun HourDetailContent(
    report: DayReport,
    hours: DayHours,
    rule: HourRule,
    labels: Map<String, String>,
    background: Color,
    text: Color,
    faint: Color,
    accent: Color,
) {
    val context = LocalContext.current
    fun time(seconds: Long) = formatMinutes(seconds / 60)
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(background).padding(24.dp)) {
        Text("Today so far", style = MaterialTheme.typography.titleMedium, color = text)
        Spacer(Modifier.height(14.dp))
        Figure("In your distracting apps", time(report.inApps), report.inAppsVsYesterday?.let { HourlyUse.versus(it, ::formatMinutes) }, text, faint, accent)
        Figure("On the phone at all", time(report.overall), report.overallVsYesterday?.let { HourlyUse.versus(it, ::formatMinutes) }, text, faint, accent)
        Figure(
            "Distracted hours",
            "${report.distracted} of ${report.hoursSoFar}",
            if (rule.totalMinutes == null) "over ${rule.appMinutes} min in distracting apps" else "over ${rule.appMinutes} min in distracting apps, or ${rule.totalMinutes} at all",
            text,
            faint,
            accent,
        )
        report.peakHour?.let { hour ->
            Figure("The heaviest hour", formatMinuteOfDay(context, hour * 60), "${time(report.peakSeconds)} in it", text, faint, accent)
        }
        if (report.topApps.isNotEmpty()) {
            Text("Used most", style = MaterialTheme.typography.labelLarge, color = faint, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
            report.topApps.forEach { (pkg, seconds) ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                    Text(labels[pkg] ?: pkg, style = MaterialTheme.typography.bodyMedium, color = text, modifier = Modifier.weight(1f))
                    Text(time(seconds), style = MaterialTheme.typography.bodyMedium, color = faint)
                }
            }
        }
        val used = (0 until 24).filter { (hours.apps[it] ?: 0L) >= 60 || (hours.all[it] ?: 0L) >= 60 }
        Text("By the hour", style = MaterialTheme.typography.labelLarge, color = faint, modifier = Modifier.padding(top = 14.dp, bottom = 6.dp))
        if (used.isEmpty()) {
            Text("Nothing yet today.", style = MaterialTheme.typography.bodyMedium, color = faint)
        } else {
            val most = used.maxOf { maxOf(hours.apps[it] ?: 0L, hours.all[it] ?: 0L) }.coerceAtLeast(1)
            LazyColumn(Modifier.heightIn(max = 300.dp)) {
                used.forEach { hour ->
                    item(key = hour) {
                        val inApps = hours.apps[hour] ?: 0L
                        val inAll = hours.all[hour] ?: 0L
                        val lit = HourlyUse.distracted(hours, hour, rule)
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(formatMinuteOfDay(context, hour * 60), style = MaterialTheme.typography.bodySmall, color = faint, modifier = Modifier.width(64.dp))
                            // The whole hour on the phone, faint; the part in distracting apps over it.
                            Box(Modifier.weight(1f).height(8.dp)) {
                                Box(Modifier.fillMaxWidth(inAll.toFloat() / most).height(8.dp).clip(RoundedCornerShape(4.dp)).background(text.copy(alpha = 0.14f)))
                                Box(Modifier.fillMaxWidth(inApps.toFloat() / most).height(8.dp).clip(RoundedCornerShape(4.dp)).background(if (lit) accent else text.copy(alpha = 0.5f)))
                            }
                            // A fixed width, so every bar is drawn to the same scale.
                            Text(
                                "${inApps / 60} · ${inAll / 60} min",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (lit) accent else text,
                                textAlign = TextAlign.End,
                                modifier = Modifier.width(84.dp),
                            )
                        }
                    }
                }
            }
            Text("Minutes in distracting apps · on the phone at all", style = MaterialTheme.typography.labelSmall, color = faint, modifier = Modifier.padding(top = 6.dp))
        }
    }
}

@Composable
private fun Figure(label: String, value: String, note: String?, text: Color, faint: Color, accent: Color) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = faint)
            if (note != null) Text(note, style = MaterialTheme.typography.labelSmall, color = if (note.contains("more than")) accent else faint)
        }
        Text(value, style = MaterialTheme.typography.titleMedium, color = text)
    }
}
