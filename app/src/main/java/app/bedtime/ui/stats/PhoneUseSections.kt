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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bedtime.data.UseComparison
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
    initialPeriod: UsePeriod = UsePeriod.DAY,
    /** The tracker's budget and colours, for the month's heatmap. */
    homeStyle: app.bedtime.data.HomeStyle = app.bedtime.data.HomeStyle(),
    /** Sets the day's allowance for the whole phone (true) or distracting apps. */
    onBudget: (Boolean, Int) -> Unit = { _, _ -> },
    /** Another page of the app: which apps are distracting. */
    onOpen: (String) -> Unit = {},
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
        MonthHeat(
            UseHeat.month(YearMonth.from(start), use.days, months[UseMonths.key(start.toEpochDay())], wholePhone, today),
            homeStyle,
            wholePhone = wholePhone,
            onWholePhone = { wholePhone = it },
            onDay = openDay,
            onBudget = onBudget,
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

    SectionCard(
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
        if (period == UsePeriod.MONTH) {
            Spacer(Modifier.height(16.dp))
            Text("The last six months, a day on average", style = MaterialTheme.typography.labelMedium, color = c.textMuted)
            Spacer(Modifier.height(8.dp))
            MonthBars(UseMonths.last(months, YearMonth.from(start)))
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

/** Within the budget: calm, the tracker's calm colour where it's set. */
private val Within = Color(0xFF86BC9F)

/** Over it, from the tracker's distracted colour (rose unless set) towards red, at twice the budget. */
private val Rose = Color(0xFFE47B8A)
private val Reddest = Color(0xFFD7263D)

/**
 * The month a day at a time, each coloured by its time against the tracker's daily budget, on the
 * budget's scale so months compare: calm within it, then the distracted colour deepening to red at
 * twice it. A tap opens that day. Under it, the month's most, and the budget it's set against.
 */
@Composable
private fun MonthHeat(
    heat: List<HeatDay>,
    style: app.bedtime.data.HomeStyle,
    wholePhone: Boolean,
    onWholePhone: (Boolean) -> Unit,
    onDay: (LocalDate) -> Unit,
    onBudget: (Boolean, Int) -> Unit,
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
    val calm = style.hourCalmColor?.let { Color(it) } ?: Within
    val over = style.hourDistractedColor?.let { Color(it) } ?: Rose
    fun colour(seconds: Long): Color {
        val level = UseHeat.level(seconds, budget)
        return if (level <= 1f) calm.copy(alpha = 0.3f + 0.4f * level) else androidx.compose.ui.graphics.lerp(over, Reddest, (level - 1f) / (UseHeat.REDDEST - 1f))
    }
    SectionCard(title = "Each day", subtitle = "Against a day's budget, the same in every month. Tap a day for it") {
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
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clip(shape)
                                    .background(if (seconds != null && seconds > 0) colour(seconds) else Color.Transparent)
                                    .border(1.dp, c.border.copy(alpha = if (seconds == null) 0.4f else 0.8f), shape)
                                    .then(if (seconds != null) Modifier.clickable(onClickLabel = "open this day") { onDay(day.date) } else Modifier),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("${day.date.dayOfMonth}", style = MaterialTheme.typography.labelMedium, color = if (seconds == null) c.textFaint else c.textNormal)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        val most = UseHeat.most(heat)
        Text(
            most?.let { "Most: ${UseWords.short(it.seconds ?: 0)}, " + it.date.format(DateTimeFormatter.ofPattern("EEEE d", Locale.getDefault())) }
                ?: "Nothing kept for these days yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.textMuted,
        )
        Spacer(Modifier.height(8.dp))
        // The scale: calm, then over, then twice over.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(colour(budget / 2)))
            Text("within", style = MaterialTheme.typography.labelSmall, color = c.textFaint)
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(colour(budget + budget / 10)))
            Text("over", style = MaterialTheme.typography.labelSmall, color = c.textFaint)
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(colour(budget * 2)))
            Text("twice or more", style = MaterialTheme.typography.labelSmall, color = c.textFaint)
        }
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
