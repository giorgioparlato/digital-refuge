package app.bedtime.ui.stats

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import app.bedtime.data.HeatDay
import app.bedtime.data.UseHeat

import androidx.compose.runtime.remember

import app.bedtime.ui.components.LowercaseStrings

import androidx.compose.material3.rememberDatePickerState

import androidx.compose.material3.TextButton

import androidx.compose.material3.SelectableDates

import androidx.compose.material3.ExperimentalMaterial3Api

import androidx.compose.material3.DatePickerDialog

import androidx.compose.material3.DatePicker

import androidx.compose.material3.DropdownMenuItem

import androidx.compose.material3.DropdownMenu

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bedtime.data.UseComparison
import app.bedtime.data.UseDay
import app.bedtime.data.UseMonths
import app.bedtime.data.UseWords
import app.bedtime.data.UsePeriod
import app.bedtime.data.UsePeriods
import app.bedtime.data.UsageMonth
import app.bedtime.data.Versus
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.OptionRow
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.SegmentedChoice
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.UseDays
import app.bedtime.ui.components.VerbatimText
import app.bedtime.ui.formatMinutes
import app.bedtime.ui.theme.Obsidian
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private fun time(seconds: Long) = formatMinutes(seconds / 60)

/** More than before: told in a warm colour, as less is told in the accent. */
private val More = Color(0xFFD9762B)

/**
 * Phone use one period at a time: a day, a week or a month, stepped back through with the arrows. Three
 * figures, each against what you chose to compare with (the period before, your usual, a year back);
 * the period in bars, with the compared one as faint outlines behind; and the apps, with how each moved.
 * Without usage access it's refuge's own count, and it says what's missing.
 */
@Composable
internal fun PhoneStats(
    use: UseDays,
    today: LocalDate,
    nowHour: Int,
    labels: Map<String, String>,
    onAllowAccess: (() -> Unit)?,
    /** Every month known, for the six-month bars and for comparing months beyond the days kept. */
    months: Map<String, UsageMonth> = emptyMap(),
    /** Apps under one of your limits, marked in the list. */
    limitedApps: Set<String> = emptySet(),
    /** What you last chose to compare with; a week or a month falls back to the one before for a day's choices. */
    chosenVersus: Versus = Versus.LAST,
    onVersus: (Versus) -> Unit = {},
    /** The month first: its heatmap, then any day a tap away. */
    initialPeriod: UsePeriod = UsePeriod.MONTH,
    /** The tracker's budget and colours, for the month's heatmap. */
    homeStyle: app.bedtime.data.HomeStyle = app.bedtime.data.HomeStyle(),
    /** Sets the day's allowance for the whole phone (true) or distracting apps. */
    onBudget: (Boolean, Int) -> Unit = { _, _ -> },
    /** Another page of the app: which apps are distracting. */
    onOpen: (String) -> Unit = {},
    /** The month heatmap's colours, and choosing them. */
) {
    val c = Obsidian.colors
    var period by rememberSaveable { mutableStateOf(initialPeriod) }
    var startDay by rememberSaveable { mutableLongStateOf(UsePeriods.start(initialPeriod, today).toEpochDay()) }
    val start = LocalDate.ofEpochDay(startDay)
    val currentStart = UsePeriods.start(period, today)
    fun go(to: UsePeriod, date: LocalDate) {
        period = to
        startDay = UsePeriods.start(to, date).toEpochDay()
    }
    // A day opened from a month: Back returns to that month rather than leaving.
    var fromMonth by rememberSaveable { mutableStateOf<Long?>(null) }
    BackHandler(enabled = fromMonth != null && period == UsePeriod.DAY) {
        fromMonth?.let { go(UsePeriod.MONTH, LocalDate.ofEpochDay(it)) }
        fromMonth = null
    }
    val openDay = { date: LocalDate ->
        if (period == UsePeriod.MONTH) fromMonth = start.toEpochDay()
        go(UsePeriod.DAY, date)
    }

    if (!use.fromAndroid && onAllowAccess != null) {
        SectionCard {
            OptionRow(
                Icons.Default.Info,
                "Allow usage access",
                description = "For every app's time and the days from before refuge, from Android's own record. It stays on the phone. The hour dots work without it",
                onClick = onAllowAccess,
            ) { Chevron() }
        }
    }

    SegmentedChoice(options = listOf("Day", "Week", "Month"), selected = period.ordinal, onSelect = {
        fromMonth = null
        go(UsePeriod.entries[it], today)
    })
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Step(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "the one before", enabled = true) { startDay = UsePeriods.shift(period, start, -1).toEpochDay() }
        Text(periodLabel(period, start, today), style = MaterialTheme.typography.titleMedium, color = c.textNormal, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        Step(Icons.AutoMirrored.Filled.KeyboardArrowRight, "the one after", enabled = start < currentStart) { startDay = UsePeriods.shift(period, start, 1).toEpochDay() }
    }

    // A day can be set against averages and a day you pick; a week or a month, against the one before when one of those is chosen.
    var pickedDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var picking by rememberSaveable { mutableStateOf(false) }
    var choosing by remember { mutableStateOf(false) }
    val picked = pickedDay?.let(LocalDate::ofEpochDay)
    // A day picked lives only as long as the page; coming back, it's the day before again.
    val versus = Versus.on(period, chosenVersus).let { if (it == Versus.PICKED && picked == null) Versus.LAST else it }
    if (picking) {
        DayPickerDialog(
            initial = picked ?: start.minusDays(7),
            latest = today.minusDays(1),
            onPick = { day ->
                pickedDay = day.toEpochDay()
                picking = false
                onVersus(Versus.PICKED)
            },
            onDismiss = { picking = false },
        )
    }

    val dates = UsePeriods.dates(period, start)
    val inProgress = today in dates
    val elapsed = if (inProgress) dates.indexOf(today) + 1 else dates.size
    val totals = UsePeriods.totals(use.days, dates, slotsByHour = period == UsePeriod.DAY)
    val fromDays = when {
        versus == Versus.YEAR_AVG -> UsePeriods.yearAverage(months, start.year)
        versus == Versus.PICKED && picked == null -> null
        else -> UsePeriods.compare(use.days, period, start, versus, today, nowHour, picked)
    }
    // Months beyond the days kept are compared from their totals; the outlines and apps still come from days, where kept.
    val comparison: UseComparison? = if (period == UsePeriod.MONTH && (fromDays == null || versus != Versus.LAST)) {
        UsePeriods.compareMonths(months, start, versus, elapsed)?.copy(unlocks = fromDays?.unlocks, slots = fromDays?.slots.orEmpty(), apps = fromDays?.apps.orEmpty()) ?: fromDays
    } else {
        fromDays
    }
    val against = versusWords(period, versus, start == currentStart, start, picked)

    if (period == UsePeriod.MONTH) {
        // The whole phone first; distracting apps a tap away.
        var wholePhone by rememberSaveable { mutableStateOf(true) }
        val heat = UseHeat.month(YearMonth.from(start), use.days, months[UseMonths.key(start.toEpochDay())], wholePhone, today)
        // A day tapped: its total and top apps at a glance, the month still behind; the whole day a tap further.
        var glance by rememberSaveable { mutableStateOf<Long?>(null) }
        glance?.let(LocalDate::ofEpochDay)?.let { date ->
            DayGlance(
                date,
                use.days[date.toEpochDay()],
                heat.firstOrNull { it.date == date }?.seconds,
                wholePhone,
                labels,
                use.distractingApps,
                limitedApps,
                today,
                onOpen = {
                    glance = null
                    openDay(date)
                },
                onDismiss = { glance = null },
            )
        }
        MonthHeat(
            heat,
            homeStyle,
            wholePhone = wholePhone,
            onWholePhone = { wholePhone = it },
            onDay = { glance = it.toEpochDay() },
            onBudget = onBudget,
            today = today,
        )
    }

    SectionCard {
        Row(Modifier.fillMaxWidth()) {
            Figure(UseWords.short(totals.total), "on the phone", totals.total, comparison?.total, Modifier.weight(1f))
            Figure(UseWords.short(totals.distracting), "distracting", totals.distracting, comparison?.distracting, Modifier.weight(1f))
            Figure(totals.unlocks?.toString() ?: "–", "unlocks", totals.unlocks?.toLong(), comparison?.unlocks?.toLong(), Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Box {
            Text(
                "against $against" + (if (comparison == null) " · nothing to compare with" else "") + (if (inProgress) ", so far" else "") + "  ▾",
                style = MaterialTheme.typography.labelMedium,
                color = c.accentText,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClickLabel = "choose what to compare with") { choosing = true }.padding(vertical = 6.dp),
            )
            DropdownMenu(expanded = choosing, onDismissRequest = { choosing = false }, containerColor = c.bgSecondary) {
                Versus.options(period).forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (option == Versus.PICKED) "a day you pick…" else versusWords(period, option, start == currentStart, start, null),
                                color = if (option == versus) c.accentText else c.textNormal,
                            )
                        },
                        onClick = {
                            choosing = false
                            if (option == Versus.PICKED) picking = true else onVersus(option)
                        },
                    )
                }
            }
        }
    }

    // A month's days are on the heatmap; under its figures, how it sits among the months before.
    if (period == UsePeriod.MONTH) {
        SectionCard(title = "The last six months", subtitle = "A day on average: all of it, and the part in distracting apps") {
            MonthBars(UseMonths.last(months, YearMonth.from(start)))
        }
    } else SectionCard(
        title = if (period == UsePeriod.DAY) "By the hour" else "By the day",
        subtitle = buildString {
            append("All of it, and the part in distracting apps")
            if (comparison?.slots?.isNotEmpty() == true) append("; outlined, $against")
            if (period == UsePeriod.DAY) append(". The dots are unlocks") else append(". Tap a day for its hours")
        },
    ) {
        val todayIndex = dates.indexOf(today).takeIf { it >= 0 && period != UsePeriod.DAY }
        Bars(
            slots = totals.slots,
            ghost = comparison?.slots,
            axis = { i ->
                when (period) {
                    UsePeriod.DAY -> if (i % 6 == 0) "$i" else null
                    UsePeriod.WEEK -> dates[i].dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
                    UsePeriod.MONTH -> null
                }
            },
            // A month's days are too narrow for their numbers: those of each week's first day go in a row of their own.
            axisEvery = if (period == UsePeriod.MONTH) 7 else null,
            picked = todayIndex,
            dots = period == UsePeriod.DAY,
            onPick = if (period == UsePeriod.DAY) null else { i -> openDay(dates[i]) },
        )
        if (period == UsePeriod.DAY) {
            // The day's worst hours, and those of what it's set against: the average day this year from the days kept.
            val here = UsePeriods.mostDistracted(totals.slots.map { it.second })
            val there = UsePeriods.mostDistracted(if (versus == Versus.YEAR_AVG) UsePeriods.yearHours(use.days, start) else comparison?.distracted.orEmpty())
            if (here.isNotEmpty() || there.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Text("Most distracted", style = MaterialTheme.typography.labelMedium, color = c.textMuted)
                if (here.isNotEmpty()) HoursLine(if (start == today) "today" else "this day", here)
                if (there.isNotEmpty()) HoursLine(against, there)
            }
        }
        if (period == UsePeriod.WEEK) {
            Spacer(Modifier.height(16.dp))
            Text("The last eight weeks", style = MaterialTheme.typography.labelMedium, color = c.textMuted)
            Spacer(Modifier.height(8.dp))
            val weeks = (7L downTo 0L).map { back -> UsePeriods.shift(UsePeriod.WEEK, start, -back) }
            Bars(
                slots = weeks.map { w -> UsePeriods.totals(use.days, UsePeriods.dates(UsePeriod.WEEK, w), slotsByHour = false).let { Triple(it.total, it.distracting, 0) } },
                ghost = null,
                axis = { i -> weeks[i].format(DateTimeFormatter.ofPattern("d/M")) },
                picked = weeks.lastIndex,
                dots = false,
                height = 56,
                onPick = { i -> startDay = weeks[i].toEpochDay() },
            )
        }
    }

    var everything by rememberSaveable { mutableStateOf(false) }
    val ranked = totals.apps.filterValues { it >= 60 }.toList().sortedByDescending { it.second }
    SectionCard(
        title = "Apps",
        subtitle = if (use.fromAndroid) "Time and opens, and the change against $against; distracting ones marked" else "Only your distracting apps are counted without usage access",
    ) {
        (if (everything) ranked else ranked.take(8)).forEach { (pkg, seconds) ->
            AppRow(
                label = labels[pkg] ?: pkg,
                seconds = seconds,
                opens = totals.opens[pkg] ?: 0,
                then = comparison?.let { it.apps[pkg] ?: 0L },
                distracting = pkg in use.distractingApps,
                limited = pkg in limitedApps,
            )
        }
        if (ranked.isEmpty()) Text("Nothing here.", style = MaterialTheme.typography.bodyMedium, color = c.textFaint, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(4.dp))
        LinkLine(
            if (use.distractingApps.isEmpty()) "No distracting apps chosen yet. Choose them" else "Which apps are distracting",
            strong = use.distractingApps.isEmpty(),
        ) { onOpen(app.bedtime.Routes.DISTRACTION) }
        if (ranked.size > 8) {
            Text(
                if (everything) "Fewer" else "All ${ranked.size}",
                style = MaterialTheme.typography.labelLarge,
                color = c.accentText,
                modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).clickable { everything = !everything }.padding(vertical = 6.dp),
            )
        }
    }
}

/** Whose hours, and which: "today · 9–10 and 20–21". */
@Composable
private fun HoursLine(whose: String, hours: List<Int>) {
    val c = Obsidian.colors
    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text(whose, style = MaterialTheme.typography.bodyMedium, color = c.textFaint, modifier = Modifier.weight(1f).padding(end = 12.dp))
        Text(UsePeriods.hourWords(hours), style = MaterialTheme.typography.bodyMedium, color = c.textNormal)
    }
}

/**
 * The heatmap's one scale: green for no time, cream at your budget, and red at the month's worst day,
 * so the reddest is always the worst there was. Mixed in Oklab, so the steps between look even.
 */
internal object HeatColours {
    private val GREEN = Color(0xFF5FA87E)
    private val CREAM = Color(0xFFF2E6C9)
    private val RED = Color(0xFFD7263D)

    /** [seconds] on the month's scale: [budget] at cream, [most] (the month's worst) at red; red only past the budget. */
    fun at(seconds: Long, budget: Long, most: Long): Color {
        val within = budget.coerceAtLeast(1)
        return if (seconds <= within || most <= within) {
            mix(GREEN, CREAM, (seconds.toFloat() / within).coerceIn(0f, 1f))
        } else {
            mix(CREAM, RED, ((seconds - within).toFloat() / (most - within)).coerceIn(0f, 1f))
        }
    }

    private fun mix(from: Color, to: Color, t: Float): Color {
        val x = oklab(from)
        val y = oklab(to)
        val m = FloatArray(3) { x[it] + (y[it] - x[it]) * t }
        return fromOklab(m)
    }

    private fun lin(c: Float) = if (c <= 0.04045f) c / 12.92f else Math.pow(((c + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    private fun gamma(c: Float) = (if (c <= 0.0031308f) 12.92f * c else 1.055f * Math.pow(c.toDouble(), 1 / 2.4).toFloat() - 0.055f).coerceIn(0f, 1f)

    private fun oklab(c: Color): FloatArray {
        val r = lin(c.red); val g = lin(c.green); val b = lin(c.blue)
        val l = Math.cbrt((0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b))
        val m = Math.cbrt((0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b))
        val s = Math.cbrt((0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b))
        return floatArrayOf(
            (0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s).toFloat(),
            (1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s).toFloat(),
            (0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s).toFloat(),
        )
    }

    private fun fromOklab(lab: FloatArray): Color {
        val l = lab[0] + 0.3963377774f * lab[1] + 0.2158037573f * lab[2]
        val m = lab[0] - 0.1055613458f * lab[1] - 0.0638541728f * lab[2]
        val s = lab[0] - 0.0894841775f * lab[1] - 1.2914855480f * lab[2]
        val l3 = l * l * l; val m3 = m * m * m; val s3 = s * s * s
        return Color(
            gamma(4.0767416621f * l3 - 3.3077115913f * m3 + 0.2309699292f * s3),
            gamma(-1.2684380046f * l3 + 2.6097574011f * m3 - 0.3413193965f * s3),
            gamma(-0.0041960863f * l3 - 0.7034186147f * m3 + 1.7076147010f * s3),
        )
    }
}

/**
 * The month a day at a time, each coloured by its time against the tracker's daily budget: green
 * within it, cream at it, deepening to red at the month's worst day. A tap shows that day at a
 * glance. Under it, the scale in times, the month's most, and the budget it's set against.
 */
@Composable
private fun MonthHeat(
    heat: List<HeatDay>,
    style: app.bedtime.data.HomeStyle,
    wholePhone: Boolean,
    onWholePhone: (Boolean) -> Unit,
    onDay: (LocalDate) -> Unit,
    onBudget: (Boolean, Int) -> Unit,
    today: LocalDate,
) {
    val c = Obsidian.colors
    // Your budget where you have one, on the home screen's tracker or the lock screen's.
    val widget = listOfNotNull(style.useWidget, style.lockUseWidget).firstOrNull { it.chosen(wholePhone) } ?: style.useWidget
    // Each against its own budget: the phone's, or the distracting apps'.
    val budget = widget.allowance(wholePhone) * 60L
    var settingBudget by remember { mutableStateOf(false) }
    if (settingBudget) {
        BudgetDialog(wholePhone, widget.allowance(wholePhone), onDismiss = { settingBudget = false }) { minutes ->
            settingBudget = false
            onBudget(wholePhone, minutes)
        }
    }
    // The worst day of the month is the reddest; within your budget, nothing is red.
    val top = UseHeat.most(heat)?.seconds ?: 0L
    fun colour(seconds: Long): Color = HeatColours.at(seconds, budget, top)
    SectionCard(title = "Each day", subtitle = "Against a day's budget, the same in every month. Tap a day for its time and top apps") {
        SegmentedChoice(options = listOf("On the phone", "Distracting"), selected = if (wholePhone) 0 else 1, onSelect = { onWholePhone(it == 0) })
        Spacer(Modifier.height(8.dp))
        // The budget it's set against, and a way to choose it; until you have, a nudge to.
        val what = if (wholePhone) "on the phone" else "in distracting apps"
        LinkLine(
            if (widget.chosen(wholePhone)) "Budget: ${formatMinutes(budget / 60)} a day $what"
            else "No budget chosen yet, so ${formatMinutes(budget / 60)} a day $what is suggested. Choose yours",
            strong = !widget.chosen(wholePhone),
        ) { settingBudget = true }
        Spacer(Modifier.height(8.dp))
        // Monday first, as the weeks are.
        Row(Modifier.fillMaxWidth()) {
            java.time.DayOfWeek.entries.forEach { d ->
                Text(d.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = c.textFaint, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(6.dp))
        val lead = heat.first().date.dayOfWeek.value - 1
        val cells: List<HeatDay?> = List(lead) { null } + heat
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (0 until 7).forEach { i ->
                    val day = week.getOrNull(i)
                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                        if (day != null) {
                            val seconds = day.seconds
                            val shape = RoundedCornerShape(8.dp)
                            // Today stands out, outlined in the accent, and always opens.
                            val isToday = day.date == today
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(shape)
                                    .background(if (seconds != null && seconds > 0) colour(seconds) else Color.Transparent)
                                    .border(if (isToday) 2.dp else 1.dp, if (isToday) c.accent else c.border.copy(alpha = if (seconds == null) 0.4f else 0.8f), shape)
                                    .then(if (seconds != null || isToday) Modifier.clickable(onClickLabel = "this day at a glance") { onDay(day.date) } else Modifier),
                                contentAlignment = Alignment.Center,
                            ) {
                                // Dark numbers on the light end of the scale, light ones on the rest.
                                val filled = seconds != null && seconds > 0
                                Text(
                                    "${day.date.dayOfMonth}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isToday) androidx.compose.ui.text.font.FontWeight.Bold else null,
                                    color = when {
                                        filled && colour(seconds ?: 0).luminance() > 0.45f -> Color(0xFF2A2A2A)
                                        filled -> Color.White
                                        seconds == null && !isToday -> c.textFaint
                                        else -> c.textNormal
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        // The scale itself, in times: nothing, your budget where it falls, and the month's most.
        val end = maxOf(top, budget)
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(androidx.compose.ui.graphics.Brush.horizontalGradient((0..24).map { colour(end * it / 24) })))
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            val at = (budget.toFloat() / end).coerceIn(0.05f, 0.95f)
            Text("0", style = MaterialTheme.typography.labelSmall, color = c.textFaint)
            Spacer(Modifier.weight(at))
            Text("${UseWords.compact((budget / 60).toInt())} budget", style = MaterialTheme.typography.labelSmall, color = c.textFaint)
            Spacer(Modifier.weight(1f - at))
            if (top > budget) Text(UseWords.compact((top / 60).toInt()), style = MaterialTheme.typography.labelSmall, color = c.textFaint)
        }
        Spacer(Modifier.height(10.dp))
        // Today, so far: a way into its page.
        heat.firstOrNull { it.date == today }?.let { day ->
            LinkLine("Today so far: ${UseWords.short(day.seconds ?: 0)} of ${formatMinutes(budget / 60)}", strong = true) { onDay(today) }
        }
        val most = UseHeat.most(heat)
        Text(
            most?.let { "Most: ${UseWords.short(it.seconds ?: 0)}, " + it.date.format(DateTimeFormatter.ofPattern("EEEE d", Locale.getDefault())) }
                ?: "Nothing kept for these days yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.textMuted,
        )
    }
}

/** A line that leads somewhere, with "›": in the accent when it asks for something still to do ([strong]). */
@Composable
private fun LinkLine(text: String, strong: Boolean, onClick: () -> Unit) {
    val c = Obsidian.colors
    Text(
        "$text ›",
        style = MaterialTheme.typography.bodySmall,
        color = if (strong) c.accentText else c.textMuted,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
    )
}

/**
 * A day from the month at a glance: its time on the phone or in distracting apps ([wholePhone]), and
 * the five apps that took most of it; then the whole day, if you want it. Days older than those kept
 * have only their total.
 */
@Composable
private fun DayGlance(
    date: LocalDate,
    day: UseDay?,
    total: Long?,
    wholePhone: Boolean,
    labels: Map<String, String>,
    distractingApps: Set<String>,
    limitedApps: Set<String>,
    today: LocalDate,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = Obsidian.colors
    val top = day?.apps.orEmpty().filter { (pkg, seconds) -> seconds >= 60 && (wholePhone || pkg in distractingApps) }
        .toList().sortedByDescending { it.second }.take(5)
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.bgSecondary,
        title = { Text(periodLabel(UsePeriod.DAY, date, today)) },
        text = {
            Column {
                val seconds = day?.let { if (wholePhone) it.total else it.distractingTotal } ?: total ?: 0L
                Text(
                    UseWords.short(seconds) + if (wholePhone) " on the phone" else " in distracting apps",
                    style = MaterialTheme.typography.titleMedium,
                    color = c.textNormal,
                )
                if (wholePhone && day != null && day.distractingTotal > 0) {
                    Text("${UseWords.short(day.distractingTotal)} of it distracting", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                }
                Spacer(Modifier.height(12.dp))
                when {
                    day == null -> Text("Only the day's total is kept this far back.", style = MaterialTheme.typography.bodySmall, color = c.textFaint)
                    top.isEmpty() -> Text("No app took a minute.", style = MaterialTheme.typography.bodySmall, color = c.textFaint)
                    else -> top.forEach { (pkg, s) ->
                        AppRow(labels[pkg] ?: pkg, s, day.opens[pkg] ?: 0, then = null, distracting = pkg in distractingApps, limited = pkg in limitedApps)
                    }
                }
            }
        },
        confirmButton = { if (day != null) TextButton(onClick = onOpen) { Text("The whole day ›", color = c.accentText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close", color = c.textMuted) } },
    )
}

/** A day's budget, for the whole phone ([wholePhone]) or distracting apps: a few usual amounts, or any other. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun BudgetDialog(wholePhone: Boolean, current: Int, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    val c = Obsidian.colors
    var minutes by remember { mutableStateOf(current) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.bgSecondary,
        title = { Text(if (wholePhone) "A day on the phone" else "A day in distracting apps") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (if (wholePhone) UseWords.PHONE_BUDGETS else UseWords.DISTRACTING_BUDGETS).forEach { m ->
                        val on = m == minutes
                        Text(
                            UseWords.compact(m),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (on) c.bgPrimary else c.textMuted,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (on) c.accent else Color.Transparent)
                                .border(1.dp, if (on) c.accent else c.border, RoundedCornerShape(50))
                                .clickable { minutes = m }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                }
                app.bedtime.ui.components.NumberStepper(minutes, { minutes = it }, UseWords.BUDGET_RANGE, step = 5, suffix = " min")
            }
        },
        confirmButton = { TextButton(onClick = { onSave(minutes) }) { Text("Save", color = c.accentText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.textMuted) } },
    )
}

/** "today", "yesterday", "wednesday 21 october"; "this week", "last week", "19 – 25 october"; "october", "october 2025". */
internal fun periodLabel(period: UsePeriod, start: LocalDate, today: LocalDate): String = when (period) {
    UsePeriod.DAY -> when (start) {
        today -> "today"
        today.minusDays(1) -> "yesterday"
        else -> start.format(DateTimeFormatter.ofPattern(if (start.year == today.year) "EEEE d MMMM" else "EEEE d MMMM yyyy", Locale.getDefault()))
    }
    UsePeriod.WEEK -> {
        val thisWeek = UsePeriods.start(UsePeriod.WEEK, today)
        when (start) {
            thisWeek -> "this week"
            thisWeek.minusWeeks(1) -> "last week"
            else -> {
                val end = start.plusDays(6)
                val from = if (start.month == end.month) start.dayOfMonth.toString() else start.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))
                "$from – " + end.format(DateTimeFormatter.ofPattern("d MMMM", Locale.getDefault()))
            }
        }
    }
    UsePeriod.MONTH -> start.format(DateTimeFormatter.ofPattern(if (start.year == today.year) "MMMM" else "MMMM yyyy", Locale.getDefault()))
}

/** What the period is set against, in words: "yesterday", "your usual wednesday", "the month before", "a year back". */
internal fun versusWords(period: UsePeriod, versus: Versus, current: Boolean, start: LocalDate, picked: LocalDate? = null): String = when (versus) {
    Versus.LAST -> when (period) {
        UsePeriod.DAY -> if (current) "yesterday" else "the day before"
        UsePeriod.WEEK -> if (current) "last week" else "the week before"
        UsePeriod.MONTH -> if (current) "last month" else "the month before"
    }
    Versus.USUAL -> when (period) {
        UsePeriod.DAY -> "your usual " + start.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
        UsePeriod.WEEK -> "your usual week"
        UsePeriod.MONTH -> "your usual month"
    }
    Versus.YEAR -> "a year back"
    Versus.WEEK_AVG -> "your 7-day average"
    Versus.MONTH_AVG -> "your 30-day average"
    Versus.YEAR_AVG -> "your average day this year"
    Versus.PICKED -> picked?.format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())) ?: "a day you pick"
}

/** Android's own calendar, for a day to compare with: any day up to [latest]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayPickerDialog(initial: LocalDate, latest: LocalDate, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val c = Obsidian.colors
    val utc = java.time.ZoneOffset.UTC
    val last = latest.atStartOfDay(utc).toInstant().toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.coerceAtMost(latest).atStartOfDay(utc).toInstant().toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis <= last
        },
    )
    LowercaseStrings {
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = { state.selectedDateMillis?.let { onPick(java.time.Instant.ofEpochMilli(it).atZone(utc).toLocalDate()) } }) {
                    Text("Compare", color = c.accentText)
                }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.textMuted) } },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun Step(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    val c = Obsidian.colors
    Box(
        Modifier.size(44.dp).clip(CircleShape).clickable(enabled = enabled, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (enabled) c.textNormal else c.textFaint.copy(alpha = 0.4f))
    }
}

/** A figure, what it stands for, and how it moved: less in the accent, more in a warm colour. */
@Composable
private fun Figure(value: String, label: String, now: Long?, then: Long?, modifier: Modifier) {
    val c = Obsidian.colors
    Column(modifier) {
        Text(value, fontSize = 22.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Light, color = c.textNormal)
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.textFaint)
        val change = if (now != null && then != null) UsePeriods.change(now, then) else null
        Text(
            when {
                change == null -> " "
                change == 0 -> "= the same"
                change < 0 -> "↓ ${-change}%"
                else -> "↑ $change%"
            },
            style = MaterialTheme.typography.labelMedium,
            color = when {
                change == null || change == 0 -> c.textFaint
                change < 0 -> c.accentText
                else -> More
            },
        )
    }
}

/**
 * Columns, one a slot: all of it in the soft tone, the distracting part in the accent from the foot, the
 * compared period as a faint outline behind, and with [dots] a dot above for unlocks, darker for more.
 */
@Composable
private fun Bars(
    slots: List<Triple<Long, Long, Int>>,
    ghost: List<Long>?,
    axis: (Int) -> String?,
    picked: Int?,
    dots: Boolean,
    height: Int = 110,
    onPick: ((Int) -> Unit)? = null,
    /** Labels only every so many slots, in a row under the bars, each as wide as its slots: for narrow ones. */
    axisEvery: Int? = null,
) {
    val c = Obsidian.colors
    val most = maxOf(slots.maxOfOrNull { it.first } ?: 0L, ghost?.maxOrNull() ?: 0L).coerceAtLeast(1)
    val mostUnlocks = slots.maxOfOrNull { it.third }?.coerceAtLeast(1) ?: 1
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(if (slots.size > 10) 2.dp else 6.dp), verticalAlignment = Alignment.Bottom) {
        slots.forEachIndexed { i, (all, distracting, unlocks) ->
            Column(
                Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).then(if (onPick != null) Modifier.clickable { onPick(i) } else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (dots) {
                    Box(Modifier.size(4.dp).clip(CircleShape).background(if (unlocks > 0) c.textNormal.copy(alpha = 0.3f + 0.7f * unlocks / mostUnlocks) else Color.Transparent))
                    Spacer(Modifier.height(4.dp))
                }
                Box(Modifier.fillMaxWidth().height(height.dp), contentAlignment = Alignment.BottomCenter) {
                    ghost?.getOrNull(i)?.takeIf { it > 0 }?.let { g ->
                        Box(Modifier.fillMaxWidth().height((height * g.toFloat() / most).dp).border(1.dp, c.textFaint.copy(alpha = 0.7f), RoundedCornerShape(3.dp)))
                    }
                    val bar = (height * all.toFloat() / most).dp
                    Box(Modifier.fillMaxWidth().height(bar).clip(RoundedCornerShape(3.dp)).background(c.textNormal.copy(alpha = if (i == picked) 0.35f else 0.18f))) {
                        val part = if (all > 0) distracting.toFloat() / all else 0f
                        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(bar * part).background(c.accent.copy(alpha = if (i == picked) 1f else 0.8f)))
                    }
                }
                if (axisEvery == null) {
                    Spacer(Modifier.height(4.dp))
                    Text(axis(i) ?: "", style = MaterialTheme.typography.labelSmall, color = if (i == picked) c.textNormal else c.textFaint, maxLines = 1)
                }
            }
        }
    }
    if (axisEvery != null) {
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            slots.indices.chunked(axisEvery).forEach { group ->
                Text("${group.first() + 1}", style = MaterialTheme.typography.labelSmall, color = c.textFaint, maxLines = 1, modifier = Modifier.weight(group.size.toFloat()))
            }
        }
    }
}

/** A column a month, as tall as its daily average on the phone, the distracting part in the accent. */
@Composable
private fun MonthBars(months: List<UsageMonth>) {
    val c = Obsidian.colors
    val most = months.maxOf { it.perDay ?: 0L }.coerceAtLeast(1)
    Row(Modifier.fillMaxWidth().height(90.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
        months.forEachIndexed { i, month ->
            val current = i == months.lastIndex
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Bottom, horizontalAlignment = Alignment.CenterHorizontally) {
                val all = month.perDay ?: 0L
                val bar = (64 * all.toFloat() / most).dp
                Box(Modifier.fillMaxWidth().height(bar).clip(RoundedCornerShape(3.dp)).background(c.textNormal.copy(alpha = if (current) 0.3f else 0.16f))) {
                    val part = if (all > 0) (month.distractingPerDay ?: 0L).toFloat() / all else 0f
                    Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(bar * part).background(c.accent.copy(alpha = if (current) 1f else 0.7f)))
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    YearMonth.parse(month.month).month.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (current) c.textNormal else c.textFaint,
                )
            }
        }
    }
}

/** An app over the period: its time and opens, and how it moved against the comparison ("+20 min", "new"). */
@Composable
private fun AppRow(label: String, seconds: Long, opens: Int, then: Long?, distracting: Boolean, limited: Boolean) {
    val c = Obsidian.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (distracting) c.accent else Color.Transparent))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            VerbatimText(label, style = MaterialTheme.typography.bodyMedium, color = c.textNormal)
            if (limited) Text("limited", style = MaterialTheme.typography.labelSmall, color = c.textFaint)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(time(seconds) + if (opens > 0) " · ${opens}×" else "", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            if (then != null) {
                val diff = (seconds - then) / 60
                Text(
                    when {
                        then < 60 -> "new"
                        diff == 0L -> "the same"
                        diff > 0 -> "+${formatMinutes(diff)}"
                        else -> "−${formatMinutes(-diff)}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when {
                        then < 60 || diff > 0 -> More
                        diff < 0 -> c.accentText
                        else -> c.textFaint
                    },
                )
            }
        }
    }
}
