package app.bedtime.ui.settings

import androidx.compose.material.icons.filled.DateRange

import app.bedtime.data.FolderIndex

import app.bedtime.data.AutoBackup

import androidx.compose.runtime.produceState

import android.content.Intent

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.Routes
import app.bedtime.data.AppSettings
import app.bedtime.data.Backup
import app.bedtime.data.HomeStyle
import app.bedtime.data.Repository
import app.bedtime.data.Schedule
import app.bedtime.service.HomeRole
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.OptionRow
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.SectionLabel
import app.bedtime.ui.components.Text
import app.bedtime.ui.formatMinutes
import app.bedtime.ui.setup.SetupSteps
import app.bedtime.ui.theme.Obsidian
import app.bedtime.ui.widget.BlockWidget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * The pages settings opens onto, one per place: each holds everything about that place, and nothing
 * about it lives anywhere else. Search, notes and cover messages are pages of their own already.
 */
enum class SettingsPlace(val route: String, val title: String, val intro: String) {
    HOME(Routes.SETTINGS_HOME, "Home screen", "Refuge's home screen takes over in sessions with minimal mode. It can be your everyday launcher too."),
    LOCK(Routes.SETTINGS_LOCK, "Lock screen", "Over Android's own lock screen: the clock and your widgets. A tap, or a swipe from the bottom, unlocks."),
    APPS(Routes.SETTINGS_APPS, "Apps", "Groups to tick many at once, the apps no session blocks, and the ones that count as distracting."),
    SHORTCUTS(Routes.SETTINGS_SHORTCUTS, "Shortcuts", "Two ways to start a block without opening the app."),
    ABOUT(Routes.SETTINGS_ABOUT, "About & backup", "How sessions work, and your settings in a file of their own."),
}

@Composable
fun SettingsScreen(place: SettingsPlace? = null, onBack: (() -> Unit)? = null, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val schedules by repo.schedules.collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val version = remember { runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty() }
    val canPinWidget = remember { BlockWidget.canPin(context) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }

    fun say(message: String) = Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    fun style(change: (HomeStyle) -> HomeStyle) {
        scope.launch { repo.updateSettings { it.copy(homeStyle = change(it.homeStyle)) } }
    }

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
    fun backUpNow() {
        scope.launch { say(if (AutoBackup.write(context)) "backed up" else "couldn't write to that folder; choose it again") }
    }
    // The weekly backup's folder: kept for good, then the first one is written straight away.
    val pickBackupFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val kept = runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }.isSuccess
        if (!kept) {
            say("that folder can't be written to from here")
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            repo.updateSettings { it.copy(backupFolder = uri.toString(), lastBackupAt = 0L) }
            backUpNow()
        }
    }
    val backupFolderName by produceState<String?>(null, settings.backupFolder) {
        value = settings.backupFolder?.let { withContext(Dispatchers.IO) { FolderIndex.folderName(context, Uri.parse(it)) } }
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
    // Permissions and the home app can change on Android's own pages, so they're read again on each return.
    var resumes by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        resumes++
        onPauseOrDispose { }
    }
    val stepsLeft = remember(resumes, settings.alwaysAvailable.size) { SetupSteps.left(context, settings.alwaysAvailable.size) }
    val isDefaultHome = remember(resumes) { HomeRole.isDefault(context) }
    val backupWritable = remember(resumes, settings.backupFolder) { settings.backupFolder?.let { AutoBackup.canWrite(context, it) } ?: false }

    SettingsContent(
        place = place,
        settings = settings,
        blocks = schedules.filter { it.isBlock },
        setupStepsLeft = stepsLeft,
        isDefaultHome = isDefaultHome,
        version = version,
        canPinWidget = canPinWidget,
        onBack = onBack,
        onOpen = onOpen,
        onLockScreen = { on -> style { it.copy(lockScreen = on) } },
        onLockAlways = { on -> style { it.copy(lockAlways = on) } },
        onTileBlock = { id -> scope.launch { repo.updateSettings { it.copy(tileBlockId = id) } } },
        onAddWidget = { block -> BlockWidget.requestPin(context, block) },
        onExport = { exportFile.launch("digital-refuge-${LocalDate.now()}.json") },
        onImport = { pickFile.launch(arrayOf("application/json", "text/plain", "*/*")) },
        backupFolderName = backupFolderName,
        backupWritable = backupWritable,
        onWeeklyBackup = { on ->
            if (on) {
                pickBackupFolder.launch(null)
            } else {
                settings.backupFolder?.let { old ->
                    runCatching { context.contentResolver.releasePersistableUriPermission(Uri.parse(old), Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
                }
                scope.launch { repo.updateSettings { it.copy(backupFolder = null) } }
            }
        },
        onChooseBackupFolder = { pickBackupFolder.launch(null) },
        onBackUpNow = ::backUpNow,
    )
}

@Composable
internal fun SettingsContent(
    place: SettingsPlace?,
    settings: AppSettings,
    blocks: List<Schedule>,
    setupStepsLeft: Int,
    isDefaultHome: Boolean = false,
    version: String = "0.9.20",
    canPinWidget: Boolean = true,
    onBack: (() -> Unit)? = null,
    onOpen: (String) -> Unit = {},
    onLockScreen: (Boolean) -> Unit = {},
    onLockAlways: (Boolean) -> Unit = {},
    onTileBlock: (String) -> Unit = {},
    onAddWidget: (Schedule) -> Unit = {},
    onExport: () -> Unit = {},
    onImport: () -> Unit = {},
    /** The weekly backup's folder by name, and whether it can still be written to. */
    backupFolderName: String? = null,
    backupWritable: Boolean = false,
    onWeeklyBackup: (Boolean) -> Unit = {},
    onChooseBackupFolder: () -> Unit = {},
    onBackUpNow: () -> Unit = {},
) {
    val c = Obsidian.colors
    var query by rememberSaveable { mutableStateOf("") }
    val backup = BackupRows(backupFolderName, backupWritable, onWeeklyBackup, onChooseBackupFolder, onBackUpNow)
    val entries = settingsEntries(settings, setupStepsLeft, isDefaultHome, onOpen, onLockScreen, onLockAlways, onExport, onImport, backup)
    Scaffold(
        containerColor = c.bgPrimary,
        topBar = { ObsidianTopBar(place?.title ?: "settings", onBack = onBack, subtitle = if (place == null) "set up once, then left alone" else null) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            Column(Modifier.padding(horizontal = PageGutter)) {
                if (place == null) {
                    SettingsSearch(query, { query = it })
                    if (query.isNotBlank()) {
                        // Anything on any page, by its name or a word in it.
                        val found = entries.filter { it.matches(query) }
                        Spacer(Modifier.height(8.dp))
                        if (found.isEmpty()) {
                            Text("Nothing by that name.", style = MaterialTheme.typography.bodyMedium, color = c.textFaint, modifier = Modifier.padding(vertical = 12.dp))
                        }
                        Rows(found)
                    } else {
                        val first = entries.filter { it.place == null && !it.searchOnly }
                        Spacer(Modifier.height(4.dp))
                        // Not a preference but a health check: the one row that can be wrong, so it sits alone.
                        Rows(first.take(1))
                        Spacer(Modifier.height(14.dp))
                        Rows(first.drop(1))
                        Footer(version)
                    }
                } else {
                    Text(place.intro, style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
                    if (place == SettingsPlace.SHORTCUTS) {
                        Shortcuts(blocks, settings.tileBlockId, canPinWidget, onTileBlock, onAddWidget)
                    } else {
                        Rows(entries.filter { it.place == place && !it.searchOnly })
                    }
                    if (place == SettingsPlace.ABOUT) Footer(version)
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun Rows(rows: List<Entry>) {
    rows.forEachIndexed { i, e -> SettingsRow(e.icon, e.title, e.description, onClick = e.onClick, trailing = e.trailing ?: { Chevron() }, last = i == rows.lastIndex) }
}

@Composable
private fun Footer(version: String) {
    Spacer(Modifier.height(18.dp))
    Text(
        "digital refuge $version · everything stays on this phone",
        style = MaterialTheme.typography.bodySmall,
        color = Obsidian.colors.textFaint,
        modifier = Modifier.fillMaxWidth().padding(start = 2.dp),
    )
}

/**
 * Every setting, each in the one place it lives ([Entry.place]; null is the first page). The first
 * page lists its own rows, permissions first; each place's page lists its rows; the search looks
 * through all of them.
 */
private fun settingsEntries(
    s: AppSettings,
    stepsLeft: Int,
    isDefaultHome: Boolean,
    onOpen: (String) -> Unit,
    onLockScreen: (Boolean) -> Unit,
    onLockAlways: (Boolean) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    backup: BackupRows,
): List<Entry> {
    val style = s.homeStyle
    fun go(route: String): () -> Unit = { onOpen(route) }
    return buildList {
        // ---------------------------------------------------------------- the first page
        add(
            Entry(
                null,
                Icons.Default.Build,
                "Permissions & setup",
                when (stepsLeft) {
                    0 -> "All set"
                    1 -> "1 step left"
                    else -> "$stepsLeft steps left"
                },
                "permissions setup accessibility greyscale dnd do not disturb battery overlay notifications launcher",
                go(Routes.SETUP),
            ),
        )
        add(
            Entry(
                null,
                Icons.Default.Home,
                "Home screen",
                (if (s.homeScreen) "Your launcher" else "In minimal sessions") + " · look, widgets, gestures, the app list",
                "home screen minimal launcher",
                go(SettingsPlace.HOME.route),
            ),
        )
        add(
            Entry(
                null,
                Icons.Default.Lock,
                "Lock screen",
                when {
                    s.homeScreen && style.lockAlways -> "All the time"
                    style.lockScreen -> "In sessions"
                    else -> "Off"
                } + " · look, widgets, notifications, gestures",
                "lock screen",
                go(SettingsPlace.LOCK.route),
            ),
        )
        add(
            Entry(
                null,
                Icons.Default.Search,
                "Search",
                "What it offers, keywords, contacts, folders, the calendar",
                "search keyword keywords web maps contacts messaging whatsapp signal telegram alarm timer calculator settings pages files folder obsidian spotify blocks calendar events",
                go(Routes.SEARCH),
            ),
        )
        add(Entry(null, BedtimeIcons.Grid, "Apps", "Groups · always available · distracting", "apps", go(SettingsPlace.APPS.route)))
        add(Entry(null, Icons.Default.Edit, "Notes", "Where quick notes go, and the to-do", "notes note quick to-do todo obsidian daily file", go(Routes.QUICK_NOTES)))
        add(
            Entry(
                null,
                BedtimeIcons.Block,
                "Cover messages",
                if (s.customMessages.isEmpty()) "What a blocked app's cover says" else "The app's and ${s.customMessages.size} of yours",
                "cover message messages words blocked",
                go(Routes.COVER_MESSAGES),
            ),
        )
        add(Entry(null, Icons.Default.Star, "Shortcuts", "Home-screen widget and quick-settings tile", "shortcuts widget tile quick settings start block", go(SettingsPlace.SHORTCUTS.route)))
        add(Entry(null, Icons.Default.Info, "About & backup", "How it works · the intro · export and import", "about backup version help", go(SettingsPlace.ABOUT.route)))
        // Habits live in insights; found by the search too.
        add(Entry(null, Icons.Default.CheckCircle, "Habits", "Tick them off, here or on your screens", "habits habit streak", go(Routes.HABITS), searchOnly = true))

        // ---------------------------------------------------------------- home screen
        val home = SettingsPlace.HOME
        // Switched on and off in setup, where Android is asked about it too; said here, a tap away.
        add(
            Entry(
                home,
                Icons.Default.Home,
                "Everyday launcher",
                when {
                    !s.homeScreen -> "Off · only in minimal sessions. Switched on in setup"
                    isDefaultHome -> "On · refuge's home screen all the time. Changed in setup"
                    else -> "On, but not your home app yet · choose it in setup"
                },
                "launcher home screen default home app everyday",
                go(Routes.SETUP),
            ),
        )
        add(Entry(home, Icons.Default.Edit, "Look", "Colours, sizes and fonts", "colour color theme background accent text font size top space look status bar logo mark arrow notes button", go(Routes.HOME_STYLE)))
        add(Entry(home, BedtimeIcons.Grid, "Widgets", "What it shows, and how", WIDGET_WORDS, go(Routes.STYLE_SHOWN)))
        add(Entry(home, Icons.Default.Refresh, "Gestures", "An app, a note, your blocks…", "gesture gestures swipe double tap home blocks", go(Routes.STYLE_GESTURES)))
        add(
            Entry(
                home,
                Icons.Default.Star,
                "The app list",
                "Favourites, hidden apps, your own names, keywords, sizes",
                "favourites favorites hidden hide apps rename names keyword icons app names size list",
                go(Routes.APP_LIST),
            ),
        )

        // ---------------------------------------------------------------- lock screen
        val lock = SettingsPlace.LOCK
        add(Entry(lock, Icons.Default.Lock, "In sessions", "While a schedule or block runs", "lock screen sessions", trailing = { ObsidianToggle(style.lockScreen, onLockScreen) }))
        add(
            Entry(
                lock,
                Icons.Default.Lock,
                "Outside sessions too",
                if (s.homeScreen) "All the time, as your launcher's" else "Only when refuge is your everyday launcher",
                "lock screen always launcher",
                trailing = { ObsidianToggle(style.lockAlways && s.homeScreen, onLockAlways, enabled = s.homeScreen) },
            ),
        )
        add(Entry(lock, Icons.Default.Edit, "Look", "Its own colours, sizes and fonts", "lock screen colour color theme background accent font size look", go(Routes.HOME_STYLE_LOCK)))
        add(Entry(lock, BedtimeIcons.Grid, "Widgets", "What it shows, and how", "lock screen $WIDGET_WORDS", go(Routes.STYLE_SHOWN_LOCK)))
        add(
            Entry(
                lock,
                Icons.Default.Notifications,
                "Notifications",
                if (style.lockNotifications) "From the apps and people you choose" else "Off",
                "notifications messages reply replies people contacts lock screen",
                go(Routes.STYLE_NOTIFICATIONS),
            ),
        )
        add(Entry(lock, Icons.Default.KeyboardArrowUp, "Gestures", "An app, the camera, screen off…", "gesture gestures swipe double tap lock camera screen off", go(Routes.STYLE_SWIPES)))

        // ---------------------------------------------------------------- apps
        val apps = SettingsPlace.APPS
        add(
            Entry(
                apps,
                BedtimeIcons.Grid,
                "App groups",
                when (s.groups.size) {
                    0 -> "Tick many apps at once when choosing"
                    1 -> "1 group"
                    else -> "${s.groups.size} groups"
                },
                "groups apps",
                go(Routes.GROUPS),
            ),
        )
        add(
            Entry(
                apps,
                Icons.Default.Warning,
                "Always-available apps",
                when (s.alwaysAvailable.size) {
                    0 -> "None yet · maps, rides, authenticators…"
                    1 -> "1 app, never blocked, whichever session is on"
                    else -> "${s.alwaysAvailable.size} apps, never blocked, whichever session is on"
                },
                "always available emergency maps authenticator",
                go(Routes.ALWAYS_AVAILABLE),
            ),
        )
        add(
            Entry(
                apps,
                BedtimeIcons.Hourglass,
                "Distracting apps",
                (if (s.distractingApps.isEmpty()) "None yet" else "${s.distractingApps.size} apps") + " · what counts as a distracted hour",
                "distraction distracting tracker hours dots",
                go(Routes.DISTRACTION),
            ),
        )

        // ---------------------------------------------------------------- shortcuts, found by the search
        val shortcuts = SettingsPlace.SHORTCUTS
        add(Entry(shortcuts, BedtimeIcons.Grid, "Home-screen widget", "A one-tap button that starts a block", "widget home screen start block button", go(shortcuts.route), searchOnly = true))
        add(Entry(shortcuts, Icons.Default.Star, "Quick-settings tile", "Which block the “refuge” tile starts", "tile quick settings start block", go(shortcuts.route), searchOnly = true))

        // ---------------------------------------------------------------- about & backup
        val about = SettingsPlace.ABOUT
        add(Entry(about, Icons.Default.Info, "How it works", "What a session locks, and your ways out", "how it works help", go(Routes.ONBOARDING)))
        add(Entry(about, BedtimeIcons.Leaf, "The intro again", "The walk-through from the first launch", "intro walkthrough", go(Routes.WALKTHROUGH)))
        add(Entry(about, Icons.Default.Share, "Export settings", "Save schedules, blocks, groups and stats to a file", "export backup save file", onExport))
        add(Entry(about, BedtimeIcons.Refuge, "Import settings", "Replace everything with a saved file", "import restore backup file", onImport))
        val folder = s.backupFolder
        add(
            Entry(
                about,
                Icons.Default.DateRange,
                "Weekly backup",
                when {
                    folder == null -> "Off · the same file as export, once a week, in a folder you choose; the newest ${AutoBackup.KEEP} kept"
                    !backup.writable -> "Can't write to ${backup.folderName ?: "that folder"} any more · tap to choose it again"
                    s.lastBackupAt == 0L -> "To ${backup.folderName ?: "your folder"} · none written yet"
                    else -> "To ${backup.folderName ?: "your folder"} · last on ${formatDay(s.lastBackupAt)}"
                },
                "backup automatic weekly folder save",
                onClick = if (folder != null && !backup.writable) backup.onChooseFolder else null,
                trailing = { ObsidianToggle(folder != null, backup.onWeekly) },
            ),
        )
        if (folder != null && backup.writable) {
            add(Entry(about, Icons.Default.Share, "Back up now", "Write this week's file straight away", "backup now", backup.onNow))
        }
    }
}

/** What the weekly backup's rows need: its folder, whether it's still writable, and their actions. */
private class BackupRows(
    val folderName: String?,
    val writable: Boolean,
    val onWeekly: (Boolean) -> Unit,
    val onChooseFolder: () -> Unit,
    val onNow: () -> Unit,
)

/** "28 october", the day a backup was written. */
private fun formatDay(millis: Long): String =
    java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        .format(java.time.format.DateTimeFormatter.ofPattern("d MMMM"))

/** What the widget pages are found by: each widget's name and what it has. */
private const val WIDGET_WORDS =
    "widget widgets weather calendar event date greeting clock clock size hours distraction tracker budget unlocks habits quote quotes now playing music note colours"

/** Both shortcuts, each listing your blocks: add a widget for one, or pick the tile's. */
@Composable
private fun Shortcuts(blocks: List<Schedule>, tileBlockId: String?, canPinWidget: Boolean, onTileBlock: (String) -> Unit, onAddWidget: (Schedule) -> Unit) {
    val c = Obsidian.colors
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

/**
 * One setting: the page it's on (null for the first), how it reads, other words it's found by, and
 * what a tap does (or its switch). [searchOnly] ones are found by the search but not listed.
 */
private class Entry(
    val place: SettingsPlace?,
    val icon: ImageVector,
    val title: String,
    val description: String,
    val words: String,
    val onClick: (() -> Unit)? = null,
    val trailing: (@Composable () -> Unit)? = null,
    val searchOnly: Boolean = false,
) {
    fun matches(query: String): Boolean {
        val q = query.trim().lowercase()
        return listOf(title, description, words, place?.title.orEmpty()).any { it.lowercase().contains(q) }
    }
}

/** "Find a setting": filters every page's settings as you type. */
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
