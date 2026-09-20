package app.bedtime.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import app.bedtime.ui.components.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.apps.AppCatalog
import app.bedtime.data.DndMode
import app.bedtime.data.Repository
import app.bedtime.data.RuntimeState
import app.bedtime.data.Schedule
import app.bedtime.data.SessionLog
import app.bedtime.engine.ActiveState
import app.bedtime.engine.Engine
import app.bedtime.engine.Occurrence
import app.bedtime.engine.ScheduleEvaluator
import app.bedtime.engine.Stats
import app.bedtime.engine.WeekStats
import app.bedtime.service.DndController
import app.bedtime.service.GreyscaleController
import app.bedtime.service.SystemApps
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.IconBadge
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.StatusDot
import app.bedtime.ui.components.Tag
import app.bedtime.ui.create.TemplateGallery
import app.bedtime.ui.formatDays
import app.bedtime.ui.formatDuration
import app.bedtime.ui.formatMinuteOfDay
import app.bedtime.ui.formatMinutes
import app.bedtime.ui.formatRelative
import app.bedtime.ui.formatTime
import app.bedtime.ui.pluralApps
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.ZoneId

internal data class HomeUiState(
    val schedules: List<Schedule>,
    val active: ActiveState?,
    val runtime: RuntimeState,
    val history: List<SessionLog>,
    val serviceOn: Boolean,
    val greyscaleOk: Boolean,
    val dndOk: Boolean,
    val now: Long,
)

@Composable
fun HomeScreen(
    onEdit: (String) -> Unit,
    onCreate: () -> Unit,
    onTemplate: (String) -> Unit,
    onSettings: () -> Unit,
    onSetup: () -> Unit,
    onUnlock: (String) -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val schedules: List<Schedule>? by repo.schedules.collectAsStateWithLifecycle(initialValue = null)
    val runtime by repo.runtime.collectAsStateWithLifecycle(initialValue = RuntimeState())
    val history by repo.history.collectAsStateWithLifecycle(initialValue = emptyList())
    val active by Engine.state(context).collectAsStateWithLifecycle()
    var serviceOn by remember { mutableStateOf(true) }
    var greyscaleOk by remember { mutableStateOf(true) }
    var dndOk by remember { mutableStateOf(true) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LifecycleResumeEffect(Unit) {
        serviceOn = SystemApps.isAccessibilityServiceEnabled(context)
        greyscaleOk = GreyscaleController.isAvailable(context)
        dndOk = DndController.hasAccess(context)
        now = System.currentTimeMillis()
        onPauseOrDispose { }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }
    // Show nothing for the split second before schedules load, rather than flashing the empty state.
    val loaded = schedules ?: return
    HomeContent(
        ui = HomeUiState(loaded, active, runtime, history, serviceOn, greyscaleOk, dndOk, now),
        onEdit = onEdit,
        onCreate = onCreate,
        onTemplate = onTemplate,
        onToggle = { schedule, on -> scope.launch { repo.upsert(schedule.copy(enabled = on)) } },
        onStartBlock = { block, minutes -> scope.launch { repo.startBlock(block, minutes) } },
        onSettings = onSettings,
        onSetup = onSetup,
        onUnlock = onUnlock,
        isInstalled = { AppCatalog.isLaunchable(context, it) },
    )
}

@Composable
internal fun HomeContent(
    ui: HomeUiState,
    onEdit: (String) -> Unit,
    onCreate: () -> Unit,
    onTemplate: (String) -> Unit,
    onToggle: (Schedule, Boolean) -> Unit,
    onStartBlock: (Schedule, Int) -> Unit,
    onSettings: () -> Unit,
    onSetup: () -> Unit,
    onUnlock: (String) -> Unit,
    isInstalled: (String) -> Boolean = { false },
) {
    val c = Obsidian.colors
    val usesDnd = ui.schedules.any { it.dnd != DndMode.OFF || it.hideNotifications }
    // Greyscale is optional and needs a computer, so missing it alone doesn't warrant the card.
    val needsSetup = !ui.serviceOn || (usesDnd && !ui.dndOk)
    val blocks = ui.schedules.filter { it.isBlock }
    val recurring = ui.schedules.filterNot { it.isBlock }

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            ObsidianTopBar("digital refuge", actions = {
                IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
            })
        },
        floatingActionButton = {
            if (ui.schedules.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    text = { Text("New", fontWeight = FontWeight.SemiBold) },
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (needsSetup) {
                item(key = "setup") { SetupChecklist(ui.serviceOn, ui.greyscaleOk, if (usesDnd) ui.dndOk else null, onSetup) }
            }
            if (ui.schedules.isEmpty()) {
                item(key = "empty") { EmptyState(onCreate = onCreate, onTemplate = onTemplate, isInstalled = isInstalled) }
            } else {
                item(key = "status") { StatusHero(ui, onUnlock) }
                if (ui.history.isNotEmpty()) {
                    item(key = "stats") { StatsLine(Stats.week(ui.history, ui.now), Stats.streak(ui.history, ui.now)) }
                }
                if (blocks.isNotEmpty()) {
                    item(key = "blocks-heading") { Heading("Focus blocks") }
                    items(blocks, key = { it.id }) { block ->
                        BlockRow(
                            block = block,
                            running = ui.active?.occurrenceOf(block.id),
                            onStart = { onStartBlock(block, block.durationMinutes) },
                            onEdit = { onEdit(block.id) },
                        )
                    }
                }
                if (recurring.isNotEmpty()) {
                    item(key = "schedules-heading") { Heading("Schedules") }
                    items(recurring, key = { it.id }) { schedule ->
                        ScheduleRow(
                            schedule = schedule,
                            active = ui.active?.occurrenceOf(schedule.id) != null,
                            onClick = { onEdit(schedule.id) },
                            onToggle = { onToggle(schedule, it) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = Obsidian.colors.textMuted,
        modifier = Modifier.padding(top = 16.dp, start = 4.dp),
    )
}

@Composable
private fun SetupChecklist(serviceOn: Boolean, greyscaleOk: Boolean, dndOk: Boolean?, onSetup: () -> Unit) {
    SectionCard(title = "Finish setting up", subtitle = "A few one-time steps and digital refuge is ready.") {
        ChecklistRow(serviceOn, "Let digital refuge see which app is open", "Needed for blocking and minimal mode")
        ChecklistRow(greyscaleOk, "Allow greyscale", "Optional · a one-time step")
        if (dndOk != null) ChecklistRow(dndOk, "Allow Do Not Disturb", "Lets sessions quiet your phone and hold notifications")
        Spacer(Modifier.height(10.dp))
        CtaButton("Continue setup", onClick = onSetup, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ChecklistRow(done: Boolean, title: String, subtitle: String) {
    val c = Obsidian.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        StatusDot(done)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (done) c.textMuted else c.textNormal,
                textDecoration = if (done) TextDecoration.LineThrough else TextDecoration.None,
            )
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textFaint)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StatusHero(ui: HomeUiState, onUnlock: (String) -> Unit) {
    val context = LocalContext.current
    val c = Obsidian.colors
    val state = ui.active ?: return
    val current = state.active.firstOrNull()

    if (current != null) {
        val shape = RoundedCornerShape(16.dp)
        val progress = ((ui.now - current.start).toFloat() / (current.end - current.start)).coerceIn(0f, 1f)
        Column(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(c.accent.copy(alpha = 0.10f))
                .border(1.dp, c.accent.copy(alpha = 0.45f), shape)
                .padding(18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(if (current.schedule.isBlock) BedtimeIcons.Target else BedtimeIcons.Moon, size = 44.dp)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (state.active.size == 1) "${current.schedule.name} is on" else "${state.active.size} sessions are on",
                        style = MaterialTheme.typography.titleLarge,
                        color = c.textNormal,
                    )
                    Text("Until ${formatTime(context, current.end)}", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                }
            }
            Spacer(Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                color = c.accent,
                trackColor = c.accent.copy(alpha = 0.18f),
                strokeCap = StrokeCap.Round,
            )
            Spacer(Modifier.height(8.dp))
            Text("${formatDuration(current.end - ui.now)} to go", style = MaterialTheme.typography.labelLarge, color = c.textMuted)
            FlowRow(
                Modifier.padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (state.blocked.isNotEmpty()) Tag("${pluralApps(state.blocked.size)} blocked")
                if (state.greyscale) Tag("greyscale")
                if (state.minimalAllowlist != null) Tag("minimal mode")
                when (state.dnd) {
                    DndMode.PRIORITY -> Tag("priority only")
                    DndMode.SILENCE -> Tag("silent")
                    DndMode.OFF -> Unit
                }
                if (state.hideNotifications) Tag("notifications held")
            }
            TextButton(onClick = { onUnlock(current.schedule.id) }, contentPadding = PaddingValues(0.dp)) {
                Text("Need to leave early?", color = c.accentText, fontWeight = FontWeight.Medium)
            }
        }
        return
    }

    val paused = ui.schedules
        .mapNotNull { s -> ui.runtime.overrides[s.id]?.pausedUntil?.takeIf { it > ui.now }?.let { s to it } }
        .minByOrNull { it.second }
    val next = ui.schedules
        .mapNotNull { s -> ScheduleEvaluator.nextStart(s, ui.now, ZoneId.systemDefault())?.let { s to it } }
        .minByOrNull { it.second }
    val (title, body) = when {
        paused != null -> "Taking a break" to "${paused.first.name} picks up again at ${formatTime(context, paused.second)}."
        next != null -> "All clear for now" to
            "Next up: ${next.first.name}, ${formatRelative(context, next.second)} (in ${formatDuration(next.second - ui.now)})."
        ui.schedules.any { it.isBlock } -> "All clear" to "Start a focus block whenever you need one."
        else -> "All clear" to "None of your schedules are switched on."
    }
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.bgSecondary)
            .border(1.dp, c.border, shape)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(
            if (paused != null) BedtimeIcons.Hourglass else BedtimeIcons.Refuge,
            tint = if (paused != null) c.orange else c.green,
            size = 44.dp,
        )
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.textNormal)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
        }
    }
}

@Composable
private fun EmptyState(onCreate: () -> Unit, onTemplate: (String) -> Unit, isInstalled: (String) -> Boolean) {
    val c = Obsidian.colors
    Column(
        Modifier.fillMaxWidth().padding(top = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(112.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(BedtimeIcons.Refuge, contentDescription = null, tint = c.accent, modifier = Modifier.size(54.dp))
        }
        Text(
            "Welcome to your digital refuge",
            style = MaterialTheme.typography.headlineSmall,
            color = c.textNormal,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            "Set quiet hours or start a focus block. Distracting apps step aside, your screen can fade to grey and notifications wait for you.",
            style = MaterialTheme.typography.bodyLarge,
            color = c.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        CtaButton("Create your own", onClick = onCreate, modifier = Modifier.fillMaxWidth().padding(top = 12.dp))
        Text(
            "Or start from a template",
            style = MaterialTheme.typography.labelLarge,
            color = c.textMuted,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, start = 4.dp),
        )
        TemplateGallery(onPick = onTemplate, isInstalled = isInstalled)
    }
}

/** Streak, time in refuge and escapes, in one quiet line instead of a card. */
@Composable
private fun StatsLine(week: WeekStats, streak: Int) {
    val protectedTime = if (week.protectedMinutes >= 600) "${week.protectedMinutes / 60} h" else formatMinutes(week.protectedMinutes)
    val escapes = if (week.escapes == 1) "1 escape" else "${week.escapes} escapes"
    Text(
        "streak $streak · $protectedTime of refuge this week · $escapes",
        style = MaterialTheme.typography.labelLarge,
        color = Obsidian.colors.textMuted,
        modifier = Modifier.padding(horizontal = 4.dp),
    )
}

/** A block as one slim row: name, its length, and a start pill. One tap on the pill starts it. */
@Composable
private fun BlockRow(block: Schedule, running: Occurrence?, onStart: () -> Unit, onEdit: () -> Unit) {
    val context = LocalContext.current
    val c = Obsidian.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (running != null) Modifier.background(c.accent.copy(alpha = 0.08f)).border(1.dp, c.accent.copy(alpha = 0.5f), shape) else Modifier)
            .clickable(onClickLabel = "Edit ${block.name}", onClick = onEdit)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(block.name, style = MaterialTheme.typography.titleMedium, color = c.textNormal)
                if (running != null) {
                    Spacer(Modifier.width(8.dp))
                    Tag("on now")
                }
            }
            Text(
                if (running != null) "until ${formatTime(context, running.end)}" else formatMinutes(block.durationMinutes.toLong()),
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
        }
        Spacer(Modifier.width(12.dp))
        if (running == null) StartPill(onStart)
    }
}

/** A one-tap "start" affordance on a block row. */
@Composable
private fun StartPill(onStart: () -> Unit) {
    val c = Obsidian.colors
    val shape = RoundedCornerShape(50)
    Text(
        "start",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = c.accentText,
        modifier = Modifier
            .clip(shape)
            .border(1.dp, c.accent.copy(alpha = 0.5f), shape)
            .clickable(onClickLabel = "Start now", onClick = onStart)
            .padding(horizontal = 18.dp, vertical = 8.dp),
    )
}

/** A schedule as one slim row: name, when, its key restrictions, and the on/off switch. */
@Composable
private fun ScheduleRow(schedule: Schedule, active: Boolean, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val context = LocalContext.current
    val c = Obsidian.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (active) Modifier.background(c.accent.copy(alpha = 0.08f)).border(1.dp, c.accent.copy(alpha = 0.5f), shape) else Modifier)
            .clickable(onClickLabel = "Edit ${schedule.name}", onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f).alpha(if (schedule.enabled) 1f else 0.5f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(schedule.name, style = MaterialTheme.typography.titleMedium, color = c.textNormal)
                if (active) {
                    Spacer(Modifier.width(8.dp))
                    Tag("on now")
                }
            }
            Text(
                "${formatMinuteOfDay(context, schedule.startMinute)} – ${formatMinuteOfDay(context, schedule.endMinute)} · ${formatDays(schedule.days)}",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            KeyTags(schedule)
        }
        Spacer(Modifier.width(12.dp))
        ObsidianToggle(schedule.enabled, onToggle, enabled = !active)
    }
}

/** The restrictions that matter at a glance; everything else is one tap away in the editor. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KeyTags(schedule: Schedule) {
    val tags = buildList {
        if (schedule.minimalMode) add("minimal mode")
        if (schedule.blockedApps.isNotEmpty()) add("${pluralApps(schedule.blockedApps.size)} blocked")
        if (schedule.greyscale) add("greyscale")
        when (schedule.dnd) {
            DndMode.PRIORITY -> add("priority only")
            DndMode.SILENCE -> add("silent")
            DndMode.OFF -> Unit
        }
    }
    if (tags.isEmpty()) return
    FlowRow(
        Modifier.padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tags.forEach { Tag(it) }
    }
}
