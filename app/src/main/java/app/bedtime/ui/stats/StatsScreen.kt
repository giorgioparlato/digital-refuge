package app.bedtime.ui.stats

import java.time.LocalDateTime

import app.bedtime.engine.LookBacks

import app.bedtime.engine.LookBack

import androidx.compose.runtime.mutableStateOf
import app.bedtime.data.Habit
import app.bedtime.ui.habits.rememberHabits
import app.bedtime.ui.habits.HabitsBody
import app.bedtime.ui.components.AddCircleButton
import app.bedtime.ui.components.SegmentedChoice
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
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
/**
 * Insights: your phone use, how your sessions held, and your habits. As a tab it has no way back; opened
 * from elsewhere (the hour dots' report, a widget) it has one. [initialSection]: 0 phone, 1 sessions, 2 habits.
 */
fun StatsScreen(onBack: (() -> Unit)?, initialSection: Int = 0, onOpen: (String) -> Unit = {}) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val history by repo.history.collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val usage by repo.usage.collectAsStateWithLifecycle(initialValue = emptyList())
    val journal by repo.journal.collectAsStateWithLifecycle(initialValue = emptyList())
    val schedules by repo.schedules.collectAsStateWithLifecycle(initialValue = emptyList())
    // The last week app by app, as Android keeps it, for the apps used inside sessions.
    val sessionEvents by produceState(emptyList<app.bedtime.data.UseEvent>()) {
        value = withContext(Dispatchers.IO) { UsageReader.events(context, System.currentTimeMillis() - 8 * 86_400_000L, System.currentTimeMillis()) }
    }
    val excludedApps = remember { UsageReader.excluded(context) }
    val sessionLabels by produceState(emptyMap<String, String>(), sessionEvents) {
        val packages = sessionEvents.map { it.pkg }.filter { it.isNotEmpty() }.toSet()
        value = withContext(Dispatchers.IO) { packages.associateWith { AppCatalog.label(context, it) } }
    }
    val (habits, habitMarks) = rememberHabits()
    val zone = ZoneId.systemDefault()
    var editingHabit by remember { mutableStateOf<Habit?>(null) }
    // Nine weeks: a week against the four before it, and the last eight weeks.
    val use = rememberUseDays(63, live = false)
    val packages = use.days.values.flatMap { it.apps.keys }.toSet()
    val labels by produceState(emptyMap<String, String>(), packages) {
        value = withContext(Dispatchers.IO) { packages.associateWith { AppCatalog.label(context, it) } }
    }
    // Only while it's fresh, Sunday evening to Monday night, and until it's put away.
    val now = LocalDateTime.now()
    val weekStart = LookBacks.weekStart(now)
    val lookBack = settings?.takeIf { s -> LookBacks.fresh(now) && s.lookBackPutAway != weekStart.toEpochDay() }?.let { s ->
        LookBacks.of(weekStart, history, use.days, s.distractingApps, habits, habitMarks, zone)
    }
    StatsContent(
        lookBack = lookBack,
        onPutAwayLookBack = { scope.launch { repo.updateSettings { it.copy(lookBackPutAway = weekStart.toEpochDay()) } } },
        history = history,
        limits = settings?.limits.orEmpty(),
        usage = usage,
        now = System.currentTimeMillis(),
        zone = zone,
        onBack = onBack,
        use = use,
        months = app.bedtime.ui.components.rememberUseMonths(),
        rule = settings?.let { HourlyUse.rule(it) } ?: HourRule(5),
        labels = labels,
        onAllowAccess = { runCatching { context.startActivity(UsageReader.accessIntent(context)) } },
        versus = settings?.statsVersus ?: app.bedtime.data.Versus.LAST,
        onVersus = { v -> scope.launch { repo.updateSettings { it.copy(statsVersus = v) } } },
        homeStyle = settings?.homeStyle ?: app.bedtime.data.HomeStyle(),
        // Changed where it's kept: the lock screen's tracker if that's the one with your budget, else the home screen's.
        onBudget = { whole, minutes ->
            scope.launch {
                repo.updateSettings { s ->
                    val style = s.homeStyle
                    val lock = style.lockUseWidget
                    s.copy(
                        homeStyle = if (lock != null && lock.chosen(whole) && !style.useWidget.chosen(whole)) style.copy(lockUseWidget = lock.withAllowance(whole, minutes))
                        else style.copy(useWidget = style.useWidget.withAllowance(whole, minutes)),
                    )
                }
            }
        },
        // One unlock budget, kept on both trackers so either shows it.
        onUnlockBudget = { n ->
            scope.launch {
                repo.updateSettings { s ->
                    val style = s.homeStyle
                    s.copy(homeStyle = style.copy(useWidget = style.useWidget.copy(unlockBudget = n), lockUseWidget = style.lockUseWidget?.copy(unlockBudget = n)))
                }
            }
        },
        onOpen = onOpen,
        moods = settings?.takeIf { it.moodsOn && it.insightsMoods }?.let { s ->
            {
                MoodStats(journal, app.bedtime.data.Why.moods(s), java.time.LocalDate.now(), onDay = { day -> onOpen(app.bedtime.Routes.journalDay(day.toEpochDay())) }, onMoods = { onOpen(app.bedtime.Routes.MOODS) }, days = use.days, habits = habits, habitMarks = habitMarks)
            }
        },
        habits = if (settings?.insightsHabits == false) null else ({
            HabitsBody(
                habits, habitMarks, HourlyUse.today(), editingHabit, { editingHabit = it },
                onMark = { habit, day, value -> scope.launch { repo.markHabit(habit.id, day, value) } },
                onSave = { habit ->
                    scope.launch {
                        repo.updateSettings { s ->
                            s.copy(habits = if (s.habits.any { it.id == habit.id }) s.habits.map { if (it.id == habit.id) habit else it } else s.habits + habit)
                        }
                    }
                },
                onMove = { moved -> scope.launch { repo.updateSettings { it.copy(habits = moved) } } },
                onDelete = { habit -> scope.launch { repo.deleteHabit(habit.id) } },
            )
        }),
        onAddHabit = { editingHabit = Habit() },
        // Habits and moods for those who use them: switched off here, they leave insights.
        moodMarks = journal.flatMap { it.moods },
        sessionEvents = sessionEvents,
        excludedApps = excludedApps,
        sessionLabels = sessionLabels,
        blockIds = schedules.filter { it.isBlock }.map { it.id }.toSet(),
        sectionsShown = settings?.let { s -> listOf(s.insightsSessions, s.insightsHabits, s.insightsMoods && s.moodsOn) },
        onSectionsShown = { sessionsOn, habitsOn, moodsOn ->
            scope.launch { repo.updateSettings { it.copy(insightsSessions = sessionsOn, insightsHabits = habitsOn, insightsMoods = moodsOn, moodsOn = it.moodsOn || moodsOn) } }
        },
        initialSection = initialSection,
    )
}

@Composable
internal fun StatsContent(
    history: List<SessionLog>,
    limits: List<AppLimit>,
    usage: List<UsageDay>,
    now: Long,
    zone: ZoneId,
    onBack: (() -> Unit)?,
    /** Phone use, and what makes an hour a distracted one; no phone sections without it. */
    use: UseDays? = null,
    /** Every month known, for the month view. */
    months: Map<String, app.bedtime.data.UsageMonth> = emptyMap(),
    rule: HourRule = HourRule(5),
    labels: Map<String, String> = emptyMap(),
    onAllowAccess: (() -> Unit)? = null,
    versus: app.bedtime.data.Versus = app.bedtime.data.Versus.LAST,
    onVersus: (app.bedtime.data.Versus) -> Unit = {},
    /** Your home screen's look: the tracker's budget and colours, for the month's heatmap. */
    homeStyle: app.bedtime.data.HomeStyle = app.bedtime.data.HomeStyle(),
    /** Whether habits and moods have sections here, and the menu that switches them. */
    /** Android's app-by-app record of the last week, for the apps used in sessions; the apps not counted, and their names. */
    sessionEvents: List<app.bedtime.data.UseEvent> = emptyList(),
    excludedApps: Set<String> = emptySet(),
    sessionLabels: Map<String, String> = emptyMap(),
    /** The moods tapped, for why sessions break; and which schedules are blocks, for their focus. */
    moodMarks: List<app.bedtime.data.MoodMark> = emptyList(),
    blockIds: Set<String> = emptySet(),
    /** Whether sessions, habits and moods have sections here, and the menu that switches them. */
    sectionsShown: List<Boolean>? = null,
    onSectionsShown: (sessions: Boolean, habits: Boolean, moods: Boolean) -> Unit = { _, _, _ -> },
    onBudget: (Boolean, Int) -> Unit = { _, _ -> },
    onUnlockBudget: (Int) -> Unit = {},
    onOpen: (String) -> Unit = {},
    /** Your moods, as a section of their own when they're on. */
    moods: (@Composable () -> Unit)? = null,
    /** Your habits, as their third section, and the round + that adds one there; without them, two sections. */
    habits: (@Composable () -> Unit)? = null,
    onAddHabit: (() -> Unit)? = null,
    /** Phone use, sessions or habits first. */
    initialSection: Int = 0,
    /** The week looked back on, above the rest while it's fresh; [onPutAwayLookBack] puts it away until the next. */
    lookBack: LookBack? = null,
    onPutAwayLookBack: () -> Unit = {},
) {
    val c = Obsidian.colors
    val week = Stats.week(history, now)
    val streak = Stats.streak(history, now)
    val best = Stats.bestStreak(history, now)
    val today = java.time.Instant.ofEpochMilli(now).atZone(zone).toLocalDate().toEpochDay()
    // Phone use, sessions and habits, one at a time: what the phone took, how your sessions held, what you kept.
    // Sessions off, unless nothing else would be left to show.
    val sessionsOn = sectionsShown?.getOrNull(0) != false
    val others = listOfNotNull("Phone".takeIf { use != null }, "Habits".takeIf { habits != null }, "Moods".takeIf { moods != null })
    val sections = listOfNotNull(
        "Phone".takeIf { use != null },
        "Sessions".takeIf { sessionsOn || others.isEmpty() },
        "Habits".takeIf { habits != null },
        "Moods".takeIf { moods != null },
    )
    var picked by rememberSaveable { mutableStateOf(sections.getOrElse(initialSection) { sections.first() }) }
    // One switched off while it was showing gives way to the first that's left.
    val shown = picked.takeIf { it in sections } ?: sections.first()
    val section = when (shown) {
        "Phone" -> 0
        "Habits" -> 2
        "Moods" -> 3
        else -> 1
    }
    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            ObsidianTopBar("insights", onBack = onBack) {
                sectionsShown?.let { (sessionsOn, habitsOn, moodsOn) -> SectionsMenu(sessionsOn, habitsOn, moodsOn, onSectionsShown) }
            }
        },
        floatingActionButton = { if (section == 2 && onAddHabit != null) AddCircleButton("add a habit", onClick = onAddHabit) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(2.dp))
            lookBack?.let { LookBackCard(it, { pkg -> labels[pkg] ?: pkg }, onPutAway = onPutAwayLookBack) }
            if (sections.size > 1) {
                SegmentedChoice(options = sections, selected = sections.indexOf(shown), onSelect = { picked = sections[it] })
            }
            if (section == 2 && habits != null) {
                habits()
                Spacer(Modifier.height(96.dp))
                return@Column
            }
            if (section == 3 && moods != null) {
                moods()
                Spacer(Modifier.height(48.dp))
                return@Column
            }
            if (use != null && section == 0) {
                val nowHour = java.time.Instant.ofEpochMilli(now).atZone(zone).hour
                PhoneStats(
                    use, java.time.LocalDate.ofEpochDay(today), nowHour, labels, onAllowAccess, months,
                    limitedApps = limits.flatMap { it.rules.apps }.toSet(),
                    chosenVersus = versus,
                    onVersus = onVersus,
                    homeStyle = homeStyle,
                    onBudget = onBudget,
                    onUnlockBudget = onUnlockBudget,
                    onOpen = onOpen,
                )
                Spacer(Modifier.height(16.dp))
                return@Column
            }
            // What sessions did, rather than how long they ran: most of that is night, for most.
            SessionStats(
                history, use?.days.orEmpty(), moodMarks, blockIds, now, zone,
                events = sessionEvents, excluded = excludedApps, label = { pkg -> sessionLabels[pkg] ?: labels[pkg] ?: pkg }, onOpen = onOpen,
            )
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
            SessionTotals(week.protectedMinutes, streak, best)
            Spacer(Modifier.height(16.dp))
        }
    }
}

/** Which of sessions, habits and moods insights shows, for those who don't use them: a menu at the top. Nothing is lost. */
@Composable
private fun SectionsMenu(sessions: Boolean, habits: Boolean, moods: Boolean, onChange: (Boolean, Boolean, Boolean) -> Unit) {
    val c = app.bedtime.ui.theme.Obsidian.colors
    var open by remember { mutableStateOf(false) }
    Box {
        androidx.compose.material3.IconButton(onClick = { open = true }) {
            androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.MoreVert, contentDescription = "what insights shows", tint = c.textMuted)
        }
        androidx.compose.material3.DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = c.bgSecondary) {
            listOf("Sessions" to sessions, "Habits" to habits, "Moods" to moods).forEach { (name, on) ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(name, color = c.textNormal) },
                    trailingIcon = { if (on) androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Check, contentDescription = "shown", tint = c.accent) },
                    onClick = {
                        when (name) {
                            "Sessions" -> onChange(!sessions, habits, moods)
                            "Habits" -> onChange(sessions, !habits, moods)
                            else -> onChange(sessions, habits, !moods)
                        }
                    },
                )
            }
        }
    }
}
