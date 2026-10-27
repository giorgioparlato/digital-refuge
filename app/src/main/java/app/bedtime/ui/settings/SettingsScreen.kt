package app.bedtime.ui.settings

import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Clear
import app.bedtime.ui.apps.PickerMode
import app.bedtime.ui.apps.AppPickerScreen
import androidx.compose.material.icons.filled.Edit
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import app.bedtime.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.AppSettings
import app.bedtime.data.Backup
import app.bedtime.data.Repository
import app.bedtime.data.Schedule
import app.bedtime.service.DndController
import app.bedtime.service.GreyscaleController
import app.bedtime.service.SystemApps
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.IconBadge
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.ui.components.OptionRow
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.SectionLabel
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.PageGutter
import androidx.compose.material3.Scaffold
import app.bedtime.ui.formatMinutes
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.bedtime.ui.widget.BlockWidget
import kotlinx.coroutines.launch
import java.time.LocalDate
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(
    onBack: (() -> Unit)? = null,
    onSetup: () -> Unit,
    onHomeStyle: () -> Unit,
    onGroups: () -> Unit,
    onAlwaysAvailable: () -> Unit,
    onHowItWorks: () -> Unit,
    onWalkthrough: () -> Unit = {},
    onQuickNotes: () -> Unit = {},
    onShown: () -> Unit = {},
    onQuotes: () -> Unit = {},
    onSwipes: () -> Unit = {},
    onLockNotifications: () -> Unit = {},
    onGestures: () -> Unit = {},
    onSearch: () -> Unit = {},
    onDistraction: () -> Unit = {},
    onHabits: () -> Unit = {},
) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val schedules by repo.schedules.collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    var stepsLeft by remember { mutableIntStateOf(0) }
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }
    val canPinWidget = remember { BlockWidget.canPin(context) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    var pickingHidden by rememberSaveable { mutableStateOf(false) }

    fun say(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

    val exportFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            scope.launch {
                val saved = withContext(Dispatchers.IO) {
                    runCatching {
                        val json = Backup.encode(repo.snapshot())
                        checkNotNull(context.contentResolver.openOutputStream(uri)).use { it.write(json.toByteArray()) }
                    }.isSuccess
                }
                say(if (saved) "settings saved to the file" else "couldn't write that file")
            }
        }
    }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingImport = uri
    }

    pendingImport?.let { uri ->
        val c = Obsidian.colors
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            title = { Text("Import settings?", color = c.textNormal) },
            text = {
                Text(
                    "This replaces your schedules, blocks and settings with the ones in the file. Running sessions aren't affected.",
                    color = c.textMuted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch {
                        val backup = withContext(Dispatchers.IO) {
                            runCatching {
                                checkNotNull(context.contentResolver.openInputStream(uri)).use { Backup.decode(it.readBytes().decodeToString()) }
                            }.getOrNull()
                        }
                        if (backup == null) say("that file isn't a digital refuge backup") else {
                            repo.restore(backup)
                            say("settings restored")
                        }
                    }
                }) { Text("Replace", color = c.accentText, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text("Cancel", color = c.textMuted) } },
            containerColor = c.bgSecondary,
        )
    }
    LifecycleResumeEffect(Unit) {
        stepsLeft = listOf(
            SystemApps.isAccessibilityServiceEnabled(context),
            GreyscaleController.isAvailable(context),
            DndController.hasAccess(context),
        ).count { !it }
        onPauseOrDispose { }
    }
    if (pickingHidden) {
        AppPickerScreen(
            "Hidden from the lists",
            settings.hiddenApps,
            PickerMode.HIDE,
            onDone = { apps ->
                scope.launch { repo.updateSettings { it.copy(hiddenApps = apps) } }
                pickingHidden = false
            },
            onBack = { pickingHidden = false },
        )
        return
    }
    SettingsContent(
        blocks = schedules.filter { it.isBlock },
        tileBlockId = settings.tileBlockId,
        setupStepsLeft = stepsLeft,
        onBack = onBack,
        onSetup = onSetup,
        onHomeStyle = onHomeStyle,
        onQuickNotes = onQuickNotes,
        homeScreen = settings.homeScreen,
        hiddenCount = settings.hiddenApps.size,
        onHiddenApps = { pickingHidden = true },
        onShown = onShown,
        onQuotes = onQuotes,
        lockScreen = settings.homeStyle.lockScreen,
        onLockScreen = { on -> scope.launch { repo.updateSettings { it.copy(homeStyle = it.homeStyle.copy(lockScreen = on)) } } },
        lockAlways = settings.homeStyle.lockAlways,
        onLockAlways = { on -> scope.launch { repo.updateSettings { it.copy(homeStyle = it.homeStyle.copy(lockAlways = on)) } } },
        onSwipes = onSwipes,
        onLockNotifications = onLockNotifications,
        onGestures = onGestures,
        onSearch = onSearch,
        onDistraction = onDistraction,
        onHabits = onHabits,
        onTileBlock = { id -> scope.launch { repo.updateSettings { it.copy(tileBlockId = id) } } },
        groupCount = settings.groups.size,
        onGroups = onGroups,
        alwaysAvailableCount = settings.alwaysAvailable.size,
        onAlwaysAvailable = onAlwaysAvailable,
        version = version,
        canPinWidget = canPinWidget,
        onAddWidget = { block -> BlockWidget.requestPin(context, block) },
        onHowItWorks = onHowItWorks,
        onWalkthrough = onWalkthrough,
        onExport = { exportFile.launch("digital-refuge-${LocalDate.now()}.json") },
        onImport = { pickFile.launch(arrayOf("application/json", "text/plain", "*/*")) },
    )
}

@Composable
internal fun SettingsContent(
    blocks: List<Schedule>,
    tileBlockId: String?,
    setupStepsLeft: Int,
    onBack: (() -> Unit)? = null,
    onSetup: () -> Unit,
    onHomeStyle: () -> Unit,
    onTileBlock: (String) -> Unit,
    groupCount: Int = 0,
    onGroups: () -> Unit = {},
    alwaysAvailableCount: Int = 0,
    onAlwaysAvailable: () -> Unit = {},
    version: String = "0.7.8.2",
    canPinWidget: Boolean = true,
    onAddWidget: (Schedule) -> Unit = {},
    onExport: () -> Unit = {},
    onImport: () -> Unit = {},
    onHowItWorks: () -> Unit = {},
    onWalkthrough: () -> Unit = {},
    onQuickNotes: () -> Unit = {},
    homeScreen: Boolean = false,
    hiddenCount: Int = 0,
    onHiddenApps: () -> Unit = {},
    onShown: () -> Unit = {},
    onQuotes: () -> Unit = {},
    lockScreen: Boolean = true,
    onLockScreen: (Boolean) -> Unit = {},
    lockAlways: Boolean = false,
    onLockAlways: (Boolean) -> Unit = {},
    onSwipes: () -> Unit = {},
    onLockNotifications: () -> Unit = {},
    onGestures: () -> Unit = {},
    onSearch: () -> Unit = {},
    onDistraction: () -> Unit = {},
    onHabits: () -> Unit = {},
) {
    val c = Obsidian.colors
    var otherOpen by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    // The version and where your data lives are said once, at the foot of the page.
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("settings", onBack = onBack, subtitle = "set up once, then left alone") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(horizontal = PageGutter)) {
                SettingsSearch(query, { query = it })
                Spacer(Modifier.height(4.dp))
                // Not a preference but a health check: the one row that can be wrong, so it sits alone.
                if (query.isBlank()) SettingsRow(
                    Icons.Default.Build,
                    "Permissions & setup",
                    when (setupStepsLeft) {
                        0 -> "All set"
                        1 -> "1 step left"
                        else -> "$setupStepsLeft steps left"
                    },
                    onClick = onSetup,
                    last = true,
                )

                // Every setting by what it's for; the search finds one by its name or a word in it.
                val lockShown = lockScreen || (homeScreen && lockAlways)
                val entries = buildList {
                    add(
                        Entry(
                            "staying focused",
                            BedtimeIcons.Grid,
                            "App groups",
                            when (groupCount) {
                                0 -> "Tick many apps at once when choosing"
                                1 -> "1 group"
                                else -> "$groupCount groups"
                            },
                            "groups apps",
                            onGroups,
                        ),
                    )
                    add(
                        Entry(
                            "staying focused",
                            Icons.Default.Warning,
                            "Always-available apps",
                            when (alwaysAvailableCount) {
                                0 -> "None yet · maps, rides, authenticators…"
                                1 -> "1 app, never blocked, whichever session is on"
                                else -> "$alwaysAvailableCount apps, never blocked, whichever session is on"
                            },
                            "always available emergency maps authenticator",
                            onAlwaysAvailable,
                        ),
                    )
                    add(Entry("staying focused", Icons.Default.DateRange, "Distraction tracker", "Your distracting apps, and today by the hour", "distraction distracting tracker hours dots colours", onDistraction))
                    add(Entry("look & feel", Icons.Default.Edit, "Look", "Colours, sizes and fonts", "colour color theme background accent text font size clock size top space", onHomeStyle))
                    add(
                        Entry(
                            "what's on my screens",
                            Icons.Default.Home,
                            "Everyday launcher",
                            if (homeScreen) "On · refuge's home screen all the time" else "Off · only in minimal sessions",
                            "launcher home screen default home app everyday minimal",
                            onSetup,
                        ),
                    )
                    add(Entry("what's on my screens", Icons.Default.Lock, "Lock screen in sessions", "The clock and your widgets over the lock screen", "lock screen sessions", trailing = { ObsidianToggle(lockScreen, onLockScreen) }))
                    if (homeScreen) {
                        add(Entry("what's on my screens", Icons.Default.Lock, "Lock screen outside sessions", "Refuge's lock screen all the time, as your launcher", "lock screen always launcher", trailing = { ObsidianToggle(lockAlways, onLockAlways) }))
                    }
                    add(Entry("what's on my screens", BedtimeIcons.Grid, "Widgets", "Weather, habits, a quote… for each screen", "widget widgets weather calendar event date greeting clock hours distraction habits quote quotes now playing music status bar logo mark arrow icons lock screen home screen", onShown))
                    if (lockShown) {
                        add(Entry("what's on my screens", Icons.Default.Notifications, "Lock-screen notifications", "From the apps and people you choose", "notifications messages reply people contacts lock screen", onLockNotifications))
                    }
                    add(Entry("gestures", Icons.Default.Refresh, "Home-screen gestures", "An app, a note, your blocks…", "gesture gestures swipe double tap home blocks", onGestures))
                    if (lockShown) {
                        add(Entry("gestures", Icons.Default.KeyboardArrowUp, "Lock-screen gestures", "An app, the camera, screen off…", "gesture gestures swipe double tap lock camera screen off", onSwipes))
                    }
                    add(Entry("finding things", Icons.Default.Search, "Search", "What it offers, keywords, and in what order", "search keyword keywords web maps contacts messaging whatsapp signal telegram alarm timer calculator settings pages files folder obsidian spotify blocks", onSearch))
                    if (homeScreen) {
                        add(
                            Entry(
                                "finding things",
                                Icons.Default.Clear,
                                "Hidden apps",
                                when (hiddenCount) {
                                    0 -> "None · hold an app to hide it from the lists"
                                    1 -> "1 app left out of the lists and search"
                                    else -> "$hiddenCount apps left out of the lists and search"
                                },
                                "hidden hide apps",
                                onHiddenApps,
                            ),
                        )
                    }
                    add(Entry("notes & habits", Icons.Default.Edit, "Notes & quick notes", "Your notes, the to-do, where quick notes go", "notes note quick to-do todo obsidian daily file", onQuickNotes))
                    add(Entry("notes & habits", Icons.Default.CheckCircle, "Habits", "Tick them off, here or on your screens", "habits habit streak", onHabits))
                    // Found by the search only: quotes live with the widgets, the rest under "setup & backup".
                    add(Entry(SEARCH_ONLY, Icons.Default.FavoriteBorder, "Quotes", "Your own, favourites, hidden ones", "quote quotes favourite hide", onQuotes))
                    add(Entry(SEARCH_ONLY, Icons.Default.Build, "Permissions & setup", "Blocking, Do Not Disturb, greyscale, your launcher", "permissions setup accessibility greyscale dnd do not disturb usage access", onSetup))
                    add(Entry(SEARCH_ONLY, Icons.Default.Share, "Export settings", "Save schedules, blocks, groups and stats to a file", "export backup save file", onExport))
                    add(Entry(SEARCH_ONLY, BedtimeIcons.Refuge, "Import settings", "Replace everything with a saved file", "import restore backup file", onImport))
                    add(Entry(SEARCH_ONLY, Icons.Default.Info, "How it works", "What a session locks, and your ways out", "how it works help", onHowItWorks))
                }
                if (query.isNotBlank()) {
                    val found = entries.filter { it.matches(query) }
                    Spacer(Modifier.height(8.dp))
                    if (found.isEmpty()) {
                        Text("Nothing by that name.", style = MaterialTheme.typography.bodyMedium, color = c.textFaint, modifier = Modifier.padding(vertical = 12.dp))
                    }
                    found.forEachIndexed { i, e -> SettingsRow(e.icon, e.title, e.description, onClick = e.onClick, trailing = e.trailing ?: { Chevron() }, last = i == found.lastIndex) }
                    Spacer(Modifier.height(28.dp))
                    return@Column
                }
                entries.filter { it.group != SEARCH_ONLY }.groupBy { it.group }.forEach { (group, rows) ->
                    SectionLabel(group)
                    if (group == "what's on my screens") {
                        Text(
                            "Refuge's home screen takes over during sessions with minimal mode, and can be your everyday launcher too. Its lock screen is up to you. Both share the settings below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textFaint,
                            modifier = Modifier.padding(start = 2.dp, bottom = 6.dp),
                        )
                    }
                    rows.forEachIndexed { i, e -> SettingsRow(e.icon, e.title, e.description, onClick = e.onClick, trailing = e.trailing ?: { Chevron() }, last = i == rows.lastIndex) }
                }

                // How firmly a session holds now belongs to each schedule, under "the ways around it".
                Spacer(Modifier.height(26.dp))
                SettingsFold(
                    title = "Setup & backup",
                    summary = "How it works · the intro · shortcuts · backup",
                    open = otherOpen,
                    onToggle = { otherOpen = !otherOpen },
                ) {
                    SettingsRow(Icons.Default.Info, "How it works", "What a session locks, and your ways out", onClick = onHowItWorks)
                    SettingsRow(BedtimeIcons.Leaf, "The intro again", "The walk-through from the first launch", onClick = onWalkthrough)
                    SettingsRow(Icons.Default.Share, "Export settings", "Save schedules, blocks, groups and stats to a file", onClick = onExport)
                    SettingsRow(BedtimeIcons.Refuge, "Import settings", "Replace everything with a saved file", onClick = onImport, last = true)

                    SectionLabel("home-screen widget")
                    Text(
                        if (canPinWidget) {
                            "A one-tap button on your home screen that starts a block. Add one for:"
                        } else {
                            "Long-press your home screen → widgets → digital refuge, then pick a block."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textFaint,
                        modifier = Modifier.padding(start = 2.dp, bottom = 6.dp),
                    )
                    if (blocks.isEmpty()) {
                        Text("Create a focus block first.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                    } else if (canPinWidget) {
                        blocks.forEachIndexed { index, block ->
                            Column {
                                Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "${block.name} · ${formatMinutes(block.durationMinutes.toLong())}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = c.textNormal,
                                        modifier = Modifier.weight(1f),
                                    )
                                    TextButton(onClick = { onAddWidget(block) }) {
                                        Text("Add", color = c.accentText, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                                if (index < blocks.lastIndex) HorizontalDivider(color = c.border.copy(alpha = 0.5f))
                            }
                        }
                    }

                    SectionLabel("quick settings tile")
                    Text(
                        "Edit your quick settings and add the “refuge” tile. Tapping it starts:",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textFaint,
                        modifier = Modifier.padding(start = 2.dp, bottom = 6.dp),
                    )
                    if (blocks.isEmpty()) {
                        Text("Create a focus block first.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                    } else {
                        val selected = blocks.firstOrNull { it.id == tileBlockId }?.id ?: blocks.first().id
                        blocks.forEachIndexed { index, block ->
                            Column {
                                Row(
                                    Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable { onTileBlock(block.id) },
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    RadioButton(
                                        selected = block.id == selected,
                                        onClick = { onTileBlock(block.id) },
                                        colors = RadioButtonDefaults.colors(selectedColor = c.accent, unselectedColor = c.textFaint),
                                    )
                                    Text(
                                        "${block.name} · ${formatMinutes(block.durationMinutes.toLong())}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = c.textNormal,
                                    )
                                }
                                if (index < blocks.lastIndex) {
                                    HorizontalDivider(Modifier.padding(start = 52.dp), color = c.border.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    "digital refuge $version · everything stays on this phone",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textFaint,
                    modifier = Modifier.fillMaxWidth().padding(start = 2.dp),
                )
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

/** A row on the bare background, with a hairline under it unless it closes its group. */
@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: (() -> Unit)? = null,
    last: Boolean = false,
    trailing: @Composable () -> Unit = { Chevron() },
) {
    val c = Obsidian.colors
    Column {
        OptionRow(icon, title, description = description, onClick = onClick, trailing = trailing)
        if (!last) HorizontalDivider(Modifier.padding(start = 52.dp), color = c.border.copy(alpha = 0.5f))
    }
}

/** The once-ever things, behind a heading that says what is inside. */
@Composable
private fun SettingsFold(
    title: String,
    summary: String,
    open: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Obsidian.colors
    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onToggle)
                .heightIn(min = 56.dp)
                .padding(vertical = 8.dp, horizontal = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f).padding(end = 10.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (open) c.accentText else c.textNormal,
                )
                Text(summary, style = MaterialTheme.typography.bodySmall, color = c.textFaint)
            }
            Icon(
                if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = c.textFaint,
            )
        }
        AnimatedVisibility(open) { Column(content = content) }
        HorizontalDivider(color = c.border.copy(alpha = if (open) 0f else 0.5f))
    }
}

/** Settings the search finds but the groups leave out, as they live elsewhere. */
private const val SEARCH_ONLY = "search only"

/** One setting in the list: its group, how it reads, other words it's found by, and what a tap does (or its switch). */
private class Entry(
    val group: String,
    val icon: ImageVector,
    val title: String,
    val description: String,
    val words: String,
    val onClick: (() -> Unit)? = null,
    val trailing: (@Composable () -> Unit)? = null,
) {
    fun matches(query: String): Boolean {
        val q = query.trim().lowercase()
        return listOf(title, description, words).any { it.lowercase().contains(q) }
    }
}

/** "Find a setting": filters the list as you type. */
@Composable
private fun SettingsSearch(query: String, onQuery: (String) -> Unit) {
    val c = Obsidian.colors
    Row(
        Modifier.fillMaxWidth().padding(top = 14.dp).clip(RoundedCornerShape(14.dp)).background(c.bgSecondary).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = c.textFaint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.textNormal),
            cursorBrush = SolidColor(c.accent),
            modifier = Modifier.weight(1f),
            decorationBox = { field ->
                Box {
                    if (query.isEmpty()) Text("Find a setting: keyword, weather, colour…", style = MaterialTheme.typography.bodyLarge, color = c.textFaint)
                    field()
                }
            },
        )
        if (query.isNotEmpty()) {
            Icon(Icons.Default.Clear, contentDescription = "clear", tint = c.textFaint, modifier = Modifier.size(20.dp).clip(RoundedCornerShape(50)).clickable { onQuery("") })
        }
    }
}
