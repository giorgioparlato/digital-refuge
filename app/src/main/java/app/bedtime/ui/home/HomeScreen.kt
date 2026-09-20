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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.apps.AppCatalog
import app.bedtime.data.AppSettings
import app.bedtime.data.DndMode
import app.bedtime.data.Quote
import app.bedtime.data.Quotes
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
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.StatusDot
import app.bedtime.ui.components.Tag
import app.bedtime.ui.components.Text
import app.bedtime.ui.create.TemplateGallery
import app.bedtime.ui.formatDuration
import app.bedtime.ui.formatMinutes
import app.bedtime.ui.formatRelative
import app.bedtime.ui.formatTime
import app.bedtime.ui.pluralApps
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
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
    /** Today's quote, or null when the home style hides it. */
    val quote: Quote? = null,
)

/** The home tab: what's running now, how the week has gone, and the blocks you can start. */
@Composable
fun HomeScreen(
    onEdit: (String) -> Unit,
    onCreate: () -> Unit,
    onTemplate: (String) -> Unit,
    onSetup: () -> Unit,
    onUnlock: (String) -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val schedules: List<Schedule>? by repo.schedules.collectAsStateWithLifecycle(initialValue = null)
    val runtime by repo.runtime.collectAsStateWithLifecycle(initialValue = RuntimeState())
    val history by repo.history.collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
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
    val style = settings.homeStyle
    val quote = if (style.showQuote) {
        Quotes.forPeriod(Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDateTime(), style.quoteRefresh)
    } else {
        null
    }
    HomeContent(
        ui = HomeUiState(loaded, active, runtime, history, serviceOn, greyscaleOk, dndOk, now, quote),
        onEdit = onEdit,
        onCreate = onCreate,
        onTemplate = onTemplate,
        onStartBlock = { block, minutes -> scope.launch { repo.startBlock(block, minutes) } },
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
    onStartBlock: (Schedule, Int) -> Unit,
    onSetup: () -> Unit,
    onUnlock: (String) -> Unit,
    isInstalled: (String) -> Boolean = { false },
) {
    val c = Obsidian.colors
    val usesDnd = ui.schedules.any { it.dnd != DndMode.OFF || it.hideNotifications }
    // Greyscale is optional and needs a computer, so missing it alone doesn't warrant the card.
    val needsSetup = !ui.serviceOn || (usesDnd && !ui.dndOk)
    val blocks = ui.schedules.filter { it.isBlock }

    LazyColumn(
        Modifier.fillMaxSize().background(c.bgPrimary),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item(key = "header") { HomeHeader(ui, onUnlock) }
        if (needsSetup) {
            item(key = "setup") {
                Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                    SetupChecklist(ui.serviceOn, ui.greyscaleOk, if (usesDnd) ui.dndOk else null, onSetup)
                }
            }
        }
        if (ui.schedules.isEmpty()) {
            item(key = "empty") {
                Box(Modifier.padding(horizontal = 20.dp)) {
                    EmptyState(onCreate = onCreate, onTemplate = onTemplate, isInstalled = isInstalled)
                }
            }
        } else {
            item(key = "blocks-heading") {
                Text(
                    if (blocks.isEmpty()) "blocks" else "start a block",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.textMuted,
                    modifier = Modifier.padding(start = 24.dp, top = 20.dp, bottom = 12.dp),
                )
            }
            // Two to a row, with "new block" always closing the grid.
            val cells = blocks.map<Schedule, Schedule?> { it } + listOf(null)
            val rows = cells.chunked(2)
            items(rows.size, key = { rows[it].first()?.id ?: "new-block" }) { index ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(84.dp).padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    rows[index].forEach { block ->
                        if (block == null) {
                            NewBlockTile(onCreate, Modifier.weight(1f).fillMaxHeight())
                        } else {
                            BlockTile(
                                block = block,
                                running = ui.active?.occurrenceOf(block.id),
                                onStart = { onStartBlock(block, block.durationMinutes) },
                                onEdit = { onEdit(block.id) },
                                modifier = Modifier.weight(1f).fillMaxHeight(),
                            )
                        }
                    }
                    repeat(2 - rows[index].size) { Spacer(Modifier.weight(1f)) }
                }
            }
            if (ui.quote != null) {
                item(key = "quote") { QuoteFooter(ui.quote) }
            }
            if (ui.history.isNotEmpty()) {
                item(key = "stats") {
                    Spacer(Modifier.height(28.dp))
                    StatsCard(Stats.week(ui.history, ui.now), Stats.streak(ui.history, ui.now))
                }
            }
        }
    }
}

/** The calm top of the screen: what is happening, in as few words as possible. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeHeader(ui: HomeUiState, onUnlock: (String) -> Unit) {
    val context = LocalContext.current
    val c = Obsidian.colors
    val state = ui.active
    val current = state?.active?.firstOrNull()

    Box(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF1F2C25), c.bgPrimary)))
            .padding(horizontal = 24.dp).padding(top = 30.dp, bottom = 26.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(BedtimeIcons.Refuge, contentDescription = null, tint = c.accent, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(10.dp))
                Text("digital refuge", style = MaterialTheme.typography.labelLarge, color = c.textFaint)
            }
            Spacer(Modifier.height(24.dp))

            if (current != null && state != null) {
                val title = if (state.active.size == 1) {
                    "${current.schedule.name} is on"
                } else {
                    "${state.active.size} sessions are on"
                }
                Text(title, fontSize = 28.sp, fontWeight = FontWeight.Light, color = c.textNormal)
                Spacer(Modifier.height(8.dp))
                Text(
                    "for another ${formatDuration(current.end - ui.now)}, until ${formatTime(context, current.end)}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.textMuted,
                )
                Spacer(Modifier.height(18.dp))
                val progress = ((ui.now - current.start).toFloat() / (current.end - current.start)).coerceIn(0f, 1f)
                Box(Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(50)).background(c.accent.copy(alpha = 0.18f))) {
                    Box(Modifier.fillMaxWidth(progress).height(2.dp).clip(RoundedCornerShape(50)).background(c.accent))
                }
                Spacer(Modifier.height(16.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "leave early",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.accentText,
                        modifier = Modifier.clickable { onUnlock(current.schedule.id) }.padding(vertical = 2.dp),
                    )
                    if (state.blocked.isNotEmpty()) Tag("${pluralApps(state.blocked.size)} blocked")
                    if (state.minimalAllowlist != null) Tag("minimal mode")
                    if (state.greyscale) Tag("greyscale")
                    if (state.dnd != DndMode.OFF) Tag("do not disturb")
                }
            } else {
                val paused = ui.schedules
                    .mapNotNull { s -> ui.runtime.overrides[s.id]?.pausedUntil?.takeIf { it > ui.now }?.let { s to it } }
                    .minByOrNull { it.second }
                val next = ui.schedules
                    .mapNotNull { s -> ScheduleEvaluator.nextStart(s, ui.now, ZoneId.systemDefault())?.let { s to it } }
                    .minByOrNull { it.second }
                val (title, body) = when {
                    paused != null -> "taking a break" to
                        "${paused.first.name} picks up again at ${formatTime(context, paused.second)}"
                    next != null -> "all clear for now" to
                        "${next.first.name} begins ${formatRelative(context, next.second)}"
                    ui.schedules.any { it.isBlock } -> "all clear" to "start a block whenever you need one"
                    else -> "all clear" to "none of your schedules are switched on"
                }
                Text(title, fontSize = 28.sp, fontWeight = FontWeight.Light, color = c.textNormal)
                Spacer(Modifier.height(8.dp))
                Text(body, style = MaterialTheme.typography.bodyLarge, color = c.textMuted)
            }
        }
    }
}

/** The week in three figures, so the streak stays in sight without shouting. */
@Composable
private fun StatsCard(week: WeekStats, streak: Int) {
    val c = Obsidian.colors
    val hours = if (week.protectedMinutes >= 600) "${week.protectedMinutes / 60} h" else formatMinutes(week.protectedMinutes)
    Row(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(c.bgSecondary).padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Stat(streak.toString(), if (streak == 1) "day streak" else "day streak", Modifier.weight(1f))
        Box(Modifier.width(1.dp).height(30.dp).background(c.border))
        Stat(hours, "this week", Modifier.weight(1f))
        Box(Modifier.width(1.dp).height(30.dp).background(c.border))
        Stat(week.escapes.toString(), if (week.escapes == 1) "escape" else "escapes", Modifier.weight(1f))
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    val c = Obsidian.colors
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = c.textNormal)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.textFaint)
    }
}

/** A block: the tile opens it, the circle starts it. While it runs, it says so instead. */
@Composable
private fun BlockTile(block: Schedule, running: Occurrence?, onStart: () -> Unit, onEdit: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val c = Obsidian.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier.clip(shape)
            .background(if (running != null) c.accent.copy(alpha = 0.12f) else c.bgSecondary)
            .border(1.dp, if (running != null) c.accent.copy(alpha = 0.5f) else c.border, shape)
            .clickable(onClickLabel = "Edit ${block.name}", onClick = onEdit)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(block.name, style = MaterialTheme.typography.titleSmall, color = c.textNormal, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(
                if (running != null) "until ${formatTime(context, running.end)}" else formatMinutes(block.durationMinutes.toLong()),
                style = MaterialTheme.typography.bodySmall,
                color = if (running != null) c.accentText else c.textMuted,
                maxLines = 1,
            )
        }
        if (running == null) {
            Box(
                Modifier.size(34.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.18f))
                    .clickable(onClickLabel = "Start ${block.name}", onClick = onStart),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = c.accent, modifier = Modifier.size(18.dp)) }
        } else {
            Tag("on now")
        }
    }
}

@Composable
private fun NewBlockTile(onCreate: () -> Unit, modifier: Modifier) {
    val c = Obsidian.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier.clip(shape).border(1.dp, c.border, shape)
            .clickable(onClickLabel = "New block", onClick = onCreate)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(c.bgSecondary),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Default.Add, contentDescription = null, tint = c.textMuted, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.width(12.dp))
        Text("new block", style = MaterialTheme.typography.titleSmall, color = c.textMuted, maxLines = 1)
    }
}

@Composable
private fun QuoteFooter(quote: Quote) {
    val c = Obsidian.colors
    Column(Modifier.padding(horizontal = 24.dp).padding(top = 26.dp)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.border.copy(alpha = 0.6f)))
        Spacer(Modifier.height(22.dp))
        Text(
            "“${quote.text}”",
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 25.sp),
            color = c.textMuted,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "— " + listOfNotNull(quote.author, quote.work).joinToString(", "),
            style = MaterialTheme.typography.labelMedium,
            color = c.textFaint,
        )
    }
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

@Composable
private fun EmptyState(onCreate: () -> Unit, onTemplate: (String) -> Unit, isInstalled: (String) -> Boolean) {
    val c = Obsidian.colors
    Column(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Set quiet hours or start a focus block. Distracting apps step aside, your screen can fade to grey and notifications wait for you.",
            style = MaterialTheme.typography.bodyLarge,
            color = c.textMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
        CtaButton("Create your own", onClick = onCreate, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
        Text(
            "Or start from a template",
            style = MaterialTheme.typography.labelLarge,
            color = c.textMuted,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, start = 4.dp),
        )
        TemplateGallery(onPick = onTemplate, isInstalled = isInstalled)
    }
}
