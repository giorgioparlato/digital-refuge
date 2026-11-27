package app.bedtime.ui.components

import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ColumnScope
import app.bedtime.data.HourState
import app.bedtime.data.HourlyUse
import app.bedtime.ui.formatMinuteOfDay

/**
 * Today as 24 dots, one an hour: calm ones soft, distracted ones in [accent], this one ringed, those
 * still to come faint. A tap opens the minutes behind them.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
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
    /** A long press: the widget's own sheet. */
    onLongClick: (() -> Unit)? = null,
) {
    // In a greyscale session every colour turns to grey, so calm and distracted hours would look the
    // same; there, the calm ones fade further and the distracted ones are what stand out.
    val context = androidx.compose.ui.platform.LocalContext.current
    val greyscale = if (androidx.compose.ui.platform.LocalInspectionMode.current) {
        false
    } else {
        val session by app.bedtime.engine.Engine.state(context).collectAsStateWithLifecycle()
        session?.greyscale == true
    }
    val chosenCalm = calmColor?.let { Color(it) } ?: text.copy(alpha = 0.55f)
    val calm = if (greyscale) chosenCalm.copy(alpha = chosenCalm.alpha * 0.45f) else chosenCalm
    val distracted = distractedColor?.let { Color(it) } ?: accent
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(if (onClick != null || onLongClick != null) Modifier.combinedClickable(onClickLabel = "today by the hour", onLongClick = onLongClick, onClick = onClick ?: {}) else Modifier)
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

/**
 * Today's seconds by hour, in distracting apps and in any, kept current: Android's record with usage
 * access, refuge's own count without it, so the dots never need the permission.
 */
@Composable
fun rememberTodayHours(): DayHours = rememberDayHours(HourlyUse.today(), live = true) ?: DayHours()

/** [day]'s seconds by hour and by distracting app, or null if nothing was counted that day. */
@Composable
fun rememberDayHours(day: Long, live: Boolean = false): DayHours? {
    val use = rememberUseDays(2, live)
    return use.days[day]?.hours(use.distractingApps)
}

/** Today for the distraction tracker, kept current: the hours for the dots, and the figures its other looks show. */
@Composable
fun rememberUseToday(): Pair<DayHours, app.bedtime.data.UseToday> {
    val use = rememberUseDays(2, live = true)
    val day = use.days[HourlyUse.today()]
    val hours = day?.hours(use.distractingApps) ?: DayHours()
    return hours to app.bedtime.data.UseToday(hours.all.values.sum(), hours.apps.values.sum(), day?.unlocks, hours.byApp, day?.apps.orEmpty() + hours.byApp)
}


/**
 * What's behind the dots and the budget line: today's time in distracting apps and on the phone, the
 * day hour by hour (tap one for its minutes and unlocks), the apps used most (three, or six with
 * "more"), and the way to statistics.
 */
@Composable
fun HourDetailDialog(
    hours: DayHours,
    rule: HourRule,
    background: Color,
    text: Color,
    faint: Color,
    accent: Color,
    onDismiss: () -> Unit,
    onStats: (() -> Unit)? = null,
    /** The colour you chose for distracted hours on the dots; without one, the app's own distracting colour. */
    distracted: Color? = null,
) {
    val context = LocalContext.current
    val yesterday = rememberDayHours(HourlyUse.today() - 1)
    val nowHour = remember { java.time.LocalTime.now().hour }
    val report = HourlyUse.report(hours, yesterday, rule, nowHour)
    val today = rememberUseDays(2, live = true).days[HourlyUse.today()]
    val useToday = rememberUseToday().second
    val ranked = app.bedtime.data.UseWords.ranked(useToday).take(6)
    val labels = remember(ranked.map { it.first }) { ranked.associate { (pkg, _) -> pkg to AppCatalog.label(context, pkg) } }
    app.bedtime.ui.minimal.HomeSheetDialog(onDismiss) {
        HourDetailContent(
            report, hours, rule, labels, background, text, faint, accent,
            unlocks = today?.unlocks, unlockHours = today?.unlockHours.orEmpty(), onStats = onStats?.let { { onDismiss(); it() } },
            distracted = distracted ?: app.bedtime.ui.theme.Obsidian.colors.distracting,
            apps = ranked,
            distractingApps = useToday.apps.keys,
            nowHour = nowHour,
        )
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
    /** Today's unlocks, when known, and by hour; the title, or none where the page already says it's today. */
    unlocks: Int? = null,
    unlockHours: Map<Int, Int> = emptyMap(),
    title: String? = "Today so far",
    padding: androidx.compose.ui.unit.Dp = 24.dp,
    /** Opens the statistics, from a link at the foot. */
    onStats: (() -> Unit)? = null,
    /** Time in distracting apps and distracted hours: their figure, bars and minutes. */
    distracted: Color = accent,
    /** The apps used most today, most first (up to six), and which of them are distracting. */
    apps: List<Pair<String, Long>> = report.topApps,
    distractingApps: Set<String> = report.topApps.map { it.first }.toSet(),
    /** The hour it is: those after it haven't happened yet. */
    nowHour: Int = 23,
) {
    val context = LocalContext.current
    var picked by remember { mutableStateOf<Int?>(null) }
    var more by remember { mutableStateOf(false) }
    // With padding it's a sheet from the bottom, like the calendar's; without, it sits inside a card of its own.
    val body: @Composable ColumnScope.() -> Unit = {
        if (title != null) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = faint)
            Spacer(Modifier.height(10.dp))
        }
        // The two figures side by side: distracting time, and the phone at all with its unlocks.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FigureCard(
                UseWordsShort(report.inApps),
                listOfNotNull("distracting", report.inAppsVsYesterday?.let { HourlyUse.versus(it, ::formatMinutes) }).joinToString(" · "),
                distracted, faint, text, Modifier.weight(1f),
            )
            FigureCard(
                UseWordsShort(report.overall),
                listOfNotNull("on the phone", unlocks?.let { if (it == 1) "1 unlock" else "$it unlocks" }).joinToString(" · "),
                text, faint, text, Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(18.dp))
        // The day, an hour a column: all of it soft, the distracting part over it from the foot.
        val most = (0 until 24).maxOf { hours.all[it] ?: 0L }.coerceAtLeast(1)
        Row(Modifier.fillMaxWidth().height(64.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
            (0 until 24).forEach { h ->
                val all = hours.all[h] ?: 0L
                val inApps = hours.apps[h] ?: 0L
                val lit = HourlyUse.distracted(hours, h, rule)
                Box(
                    Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).clickable(enabled = h <= nowHour, onClickLabel = "this hour") { picked = if (picked == h) null else h },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(Modifier.fillMaxWidth().height(1.dp).background(text.copy(alpha = 0.12f)))
                    if (all > 0) {
                        val bar = (64f * all / most).dp.coerceAtLeast(2.dp)
                        Box(Modifier.fillMaxWidth().height(bar).clip(RoundedCornerShape(3.dp)).background(text.copy(alpha = if (picked == h) 0.3f else 0.16f)), contentAlignment = Alignment.BottomCenter) {
                            Box(Modifier.fillMaxWidth().height(bar * (inApps.toFloat() / all).coerceIn(0f, 1f)).background(if (lit) distracted else text.copy(alpha = 0.45f)))
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            listOf(0, 6, 12, 18).forEach { h ->
                Text(formatMinuteOfDay(context, h * 60), style = MaterialTheme.typography.labelSmall, color = faint, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(10.dp))
        val hour = picked
        if (hour != null) {
            val all = hours.all[hour] ?: 0L
            val inApps = hours.apps[hour] ?: 0L
            val n = unlockHours[hour] ?: 0
            Text(
                "${formatMinuteOfDay(context, hour * 60)}–${formatMinuteOfDay(context, (hour + 1) % 24 * 60)} · ${UseWordsShort(all)} on the phone, " +
                    "${UseWordsShort(inApps)} distracting" + if (n > 0) " · " + (if (n == 1) "1 unlock" else "$n unlocks") else "",
                style = MaterialTheme.typography.bodySmall,
                color = text,
            )
        } else {
            Text(
                "Tap an hour to see it · ${report.distracted} distracted ${if (report.distracted == 1) "hour" else "hours"} so far",
                style = MaterialTheme.typography.bodySmall,
                color = faint,
            )
        }
        Text(
            if (rule.totalMinutes == null) "An hour is distracted with over ${rule.appMinutes} min in distracting apps" else "An hour is distracted with over ${rule.appMinutes} min in distracting apps, or ${rule.totalMinutes} at all",
            style = MaterialTheme.typography.labelSmall,
            color = faint,
        )
        // The apps used most: three, or six with "more"; distracting ones in their colour.
        if (apps.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text("Used most", style = MaterialTheme.typography.labelLarge, color = faint)
            Spacer(Modifier.height(8.dp))
            val top = apps.first().second.coerceAtLeast(1)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                apps.take(if (more) 6 else 3).forEach { (pkg, seconds) ->
                    Column {
                        Row {
                            VerbatimText(labels[pkg] ?: pkg, style = MaterialTheme.typography.bodyMedium, color = text, modifier = Modifier.weight(1f))
                            Text(UseWordsShort(seconds), style = MaterialTheme.typography.bodyMedium, color = faint)
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(
                            Modifier.fillMaxWidth(seconds.toFloat() / top).height(5.dp).clip(RoundedCornerShape(50))
                                .background(if (pkg in distractingApps) distracted else text.copy(alpha = 0.25f)),
                        )
                    }
                }
            }
            if (apps.size > 3) {
                Text(
                    if (more) "Fewer" else "More",
                    style = MaterialTheme.typography.labelLarge,
                    color = faint,
                    modifier = Modifier.padding(top = 6.dp).clip(RoundedCornerShape(8.dp)).clickable { more = !more }.padding(vertical = 6.dp),
                )
            }
        }
        if (onStats != null) {
            Text(
                "Your statistics ›",
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                modifier = Modifier.padding(top = 16.dp).clip(RoundedCornerShape(50)).border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(50))
                    .clickable(onClick = onStats).padding(horizontal = 16.dp, vertical = 10.dp),
            )
        }
    }
    if (padding > 0.dp) app.bedtime.ui.minimal.HomeSheet(background, faint, body) else Column(Modifier.fillMaxWidth(), content = body)
}

private fun UseWordsShort(seconds: Long): String = app.bedtime.data.UseWords.short(seconds)

/** A figure on a card of its own: the time, big, and what it is under it. */
@Composable
private fun FigureCard(value: String, label: String, valueColour: Color, faint: Color, text: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(text.copy(alpha = 0.06f)).padding(horizontal = 14.dp, vertical = 12.dp)) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = valueColour)
        Text(label, style = MaterialTheme.typography.labelSmall, color = faint)
    }
}
