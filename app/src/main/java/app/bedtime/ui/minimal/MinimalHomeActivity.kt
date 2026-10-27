package app.bedtime.ui.minimal

import androidx.compose.material.icons.filled.Edit
import app.bedtime.data.NoteWidget
import app.bedtime.ui.notes.NoteWidgetView
import app.bedtime.ui.minimal.WidgetSheet
import androidx.compose.foundation.shape.CircleShape
import app.bedtime.data.Schedule
import app.bedtime.ui.components.rememberNowPlaying
import app.bedtime.ui.components.NowPlayingWidget
import app.bedtime.data.Habits
import app.bedtime.ui.habits.rememberHabits
import app.bedtime.ui.habits.HabitDetailContent
import app.bedtime.ui.habits.HabitsWidget
import app.bedtime.data.Keywords
import app.bedtime.data.Module
import app.bedtime.data.Modules
import app.bedtime.data.Screen
import app.bedtime.data.ChatApp
import app.bedtime.data.Calculator
import app.bedtime.data.ClockActions
import app.bedtime.data.FolderIndex
import app.bedtime.data.IndexedFile
import app.bedtime.data.SettingsPages
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.BoxWithConstraints
import app.bedtime.ui.components.StatusBarVisibility
import app.bedtime.data.ContactSearch
import app.bedtime.data.ContactHit
import app.bedtime.ui.components.rememberTodayHours
import app.bedtime.ui.components.HourDetailDialog
import app.bedtime.data.HourlyUse
import app.bedtime.ui.components.HourDots
import app.bedtime.data.HourState
import app.bedtime.data.SearchOrder
import app.bedtime.data.NoteTarget
import app.bedtime.data.HomeGesture
import app.bedtime.data.HomeActions
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import android.provider.MediaStore
import android.os.Build
import android.accessibilityservice.AccessibilityService
import app.bedtime.data.HomeApps
import android.content.pm.ShortcutInfo
import android.os.UserHandle
import android.content.pm.LauncherApps
import app.bedtime.data.Weather
import app.bedtime.data.WeatherView
import app.bedtime.ui.notes.NotesScreen
import app.bedtime.data.SearchTargets
import app.bedtime.data.SearchTarget
import app.bedtime.data.ParsedEvent
import app.bedtime.data.Notes
import app.bedtime.data.EventParser
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.produceState
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.KeyboardArrowUp
import app.bedtime.ui.components.BedtimeIcons
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.widget.Toast
import android.net.Uri
import android.Manifest
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.combinedClickable
import app.bedtime.ui.quotes.rememberQuoteActions
import app.bedtime.ui.quotes.QuoteMenu
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import app.bedtime.ui.theme.WithFontFamily
import app.bedtime.ui.theme.AppFonts
import app.bedtime.ui.components.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.apps.AppCatalog
import app.bedtime.apps.AppEntry
import app.bedtime.data.AppSettings
import app.bedtime.data.HomeStyle
import app.bedtime.data.Quote
import app.bedtime.data.Quotes
import app.bedtime.data.Repository
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.launch
import app.bedtime.engine.Engine
import app.bedtime.service.BlockingState
import app.bedtime.service.SystemApps
import app.bedtime.ui.components.AppIcon
import app.bedtime.ui.components.ProvideAppIcons
import app.bedtime.ui.formatTime
import app.bedtime.ui.homestyle.palette
import app.bedtime.ui.theme.BedtimeTheme
import app.bedtime.ui.theme.Obsidian
import app.bedtime.unlock.UnlockFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * The minimal launcher: a clock and a plain list of allowed apps, styled by the user's [HomeStyle].
 * Made your home screen in settings, it's also there outside sessions, with your favourite apps.
 */
class MinimalHomeActivity : ComponentActivity() {
    /** Presses of Home while this is already the home screen, which bring it back to its start. */
    private val homePresses = mutableIntStateOf(0)

    /** Installs, removals and updates, which refresh the lists. */
    private val appsChanged = mutableIntStateOf(0)

    private val appsCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String?, user: UserHandle?) = changed()
        override fun onPackageAdded(packageName: String?, user: UserHandle?) = changed()
        override fun onPackageChanged(packageName: String?, user: UserHandle?) = changed()
        override fun onPackagesAvailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = changed()
        override fun onPackagesUnavailable(packageNames: Array<out String>?, user: UserHandle?, replacing: Boolean) = changed()
        private fun changed() {
            appsChanged.intValue++
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(this) { /* Stay here; there is nowhere to go back to. */ }
        runCatching { getSystemService(LauncherApps::class.java)?.registerCallback(appsCallback) }
        val asHome = intent?.hasCategory(Intent.CATEGORY_HOME) == true
        setContent {
            BedtimeTheme {
                ProvideAppIcons {
                    MinimalHomeScreen(
                        onFinish = ::finish,
                        onLightBackground = ::useLightSystemBars,
                        launchedAsHome = asHome,
                        homePresses = homePresses.intValue,
                        appsChanged = appsChanged.intValue,
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homePresses.intValue++
    }

    override fun onDestroy() {
        runCatching { getSystemService(LauncherApps::class.java)?.unregisterCallback(appsCallback) }
        super.onDestroy()
    }

    /** Dark status-bar icons on light home backgrounds, light icons on dark ones. */
    private fun useLightSystemBars(light: Boolean) {
        val transparent = android.graphics.Color.TRANSPARENT
        val style = if (light) SystemBarStyle.light(transparent, transparent) else SystemBarStyle.dark(transparent)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, MinimalHomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

@Composable
private fun MinimalHomeScreen(
    onFinish: () -> Unit,
    onLightBackground: (Boolean) -> Unit,
    launchedAsHome: Boolean = false,
    homePresses: Int = 0,
    appsChanged: Int = 0,
) {
    val context = LocalContext.current
    val c = Obsidian.colors
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val state by Engine.state(context).collectAsStateWithLifecycle()
    val loadedSettings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val settings = loadedSettings ?: AppSettings()
    val occurrence = state?.minimalOccurrence
    // Your home screen all the time, or only the minimal one during sessions.
    val asHome = launchedAsHome || settings.homeScreen
    var actionsFor by remember { mutableStateOf<AppEntry?>(null) }
    var unlocking by rememberSaveable { mutableStateOf(false) }
    var emergency by rememberSaveable { mutableStateOf(false) }
    var quoteOffset by rememberSaveable { mutableIntStateOf(0) }
    var week by remember { mutableStateOf<List<CalendarDay>?>(null) }
    var drawer by rememberSaveable { mutableStateOf(false) }
    val breezy = remember { Weather.APPS.firstOrNull { AppCatalog.isLaunchable(context, it) } }
    var query by rememberSaveable { mutableStateOf("") }
    var notesOpen by rememberSaveable { mutableStateOf(false) }
    var quickNote by rememberSaveable { mutableStateOf(false) }
    var hoursDetail by remember { mutableStateOf(false) }
    val todayHours = rememberTodayHours()
    val (habitList, habitMarks) = rememberHabits()
    // Your blocks, to start from the refuge mark, a gesture or the search.
    val allSchedules by repo.schedules.collectAsStateWithLifecycle(initialValue = emptyList())
    val blocks = allSchedules.filter { it.isBlock && it.enabled }
    var blocksOpen by remember { mutableStateOf(false) }
    var holding by remember { mutableStateOf<Module?>(null) }
    var screenSheet by remember { mutableStateOf(false) }
    // The note the note widget shows: the one you chose, or the to-do note.
    val allNotes by repo.notes.collectAsStateWithLifecycle(initialValue = emptyList())
    val widgetNote = allNotes.firstOrNull { it.id == (settings.homeStyle.noteWidgetId ?: Notes.TODO_ID) }
    val startBlock: (Schedule) -> Unit = { block ->
        scope.launch {
            repo.startBlock(block, block.durationMinutes)
            Toast.makeText(context, "${block.name} until ${formatTime(context, System.currentTimeMillis() + block.durationMinutes * 60_000L)}".lowercase(), Toast.LENGTH_SHORT).show()
        }
    }
    // A session can bring its own widgets.
    val sessionStyle = Modules.during(settings.homeStyle, state?.active.orEmpty().map { it.start to it.schedule })
    val nowPlaying = if (Modules.isOn(sessionStyle, Screen.HOME, Module.NOW_PLAYING)) rememberNowPlaying() else null
    var openHabit by remember { mutableStateOf<String?>(null) }
    // Looked for when the drawer first opens, so Home itself comes up at once.
    val searchTargets by produceState(emptyList<SearchTarget>(), drawer) {
        if (drawer && value.isEmpty()) value = withContext(Dispatchers.IO) { SearchTargets.find(context) }
    }
    val obsidian = remember { AppCatalog.isLaunchable(context, FolderIndex.OBSIDIAN) }
    val settingsApp = remember {
        context.packageManager.resolveActivity(Intent(android.provider.Settings.ACTION_SETTINGS), 0)?.activityInfo?.packageName
    }
    // Your vault's notes (with its name, for Obsidian's links) and your folder's files, read when the drawer opens.
    val vault by produceState("" to emptyList<IndexedFile>(), drawer, settings.obsidianVault) {
        val tree = settings.obsidianVault
        if (drawer && tree != null && obsidian) value = withContext(Dispatchers.IO) {
            val uri = Uri.parse(tree)
            FolderIndex.folderName(context, uri) to FolderIndex.list(context, uri) { it.endsWith(".md") }
        }
    }
    val folderFiles by produceState(emptyList<IndexedFile>(), drawer, settings.fileFolders) {
        val trees = settings.fileFolders
        if (drawer && trees.isNotEmpty()) value = withContext(Dispatchers.IO) { trees.flatMap { FolderIndex.list(context, Uri.parse(it)) } }
    }
    // An event waiting for calendar access, saved once it's given.
    var pendingEvent by remember { mutableStateOf<ParsedEvent?>(null) }
    val saveEvent: (ParsedEvent) -> Unit = { event ->
        scope.launch {
            val saved = withContext(Dispatchers.IO) { CalendarSource.insert(context, event, settings.eventCalendarId) }
            Toast.makeText(context, if (saved) "added to your calendar" else "couldn't add it to a calendar", Toast.LENGTH_SHORT).show()
            if (saved) query = ""
        }
    }
    val askCalendar = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        val event = pendingEvent
        pendingEvent = null
        if (event != null && granted.values.all { it }) saveEvent(event)
        else Toast.makeText(context, "calendar access wasn't allowed", Toast.LENGTH_SHORT).show()
    }
    val quoteActions = rememberQuoteActions()
    val style = sessionStyle
    val light = style.palette().isLight

    LaunchedEffect(state, loadedSettings) {
        // Not your home screen, it's only for minimal sessions: once none runs, it goes.
        if (state != null && loadedSettings != null && occurrence == null && !asHome) onFinish()
    }
    // Home pressed while already here: back to the start, as home screens do.
    LaunchedEffect(homePresses) {
        if (homePresses == 0) return@LaunchedEffect
        drawer = false
        query = ""
        notesOpen = false
        quickNote = false
        week = null
        actionsFor = null
        emergency = false
        unlocking = false
    }
    LaunchedEffect(light, unlocking, emergency) { onLightBackground(if (unlocking || emergency) !c.isDark else light) }
    StatusBarVisibility(hidden = style.hideStatusBar)

    var installed by remember { mutableStateOf(emptyList<AppEntry>()) }
    // digital refuge itself among them, so your everyday home has a way into it; minimal mode leaves it to emergency.
    LaunchedEffect(appsChanged) {
        installed = withContext(Dispatchers.IO) { AppCatalog.launchableApps(context) + AppEntry(context.packageName, "digital refuge") }
    }
    val dialer = remember { SystemApps.dialerPackage(context) }
    // Apps under your own names, where you've given them one.
    val byPackage = installed.associate { app -> app.packageName to (settings.appNames[app.packageName]?.let { app.copy(label = it) } ?: app) }
    val lists = HomeApps.lists(
        installed = installed.map { it.packageName },
        favourites = settings.homeFavourites,
        hidden = settings.hiddenApps,
        minimal = if (occurrence != null) state?.minimalAllowlist.orEmpty() else null,
        blocked = if (state?.isActive == true) state?.blocked.orEmpty() else emptySet(),
        alwaysAvailable = settings.alwaysAvailable,
        dialer = dialer,
    )
    val apps = lists.home.mapNotNull(byPackage::get).let { home -> if (occurrence != null) home.sortedBy { it.label.lowercase() } else home }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000)
        }
    }

    if (loadedSettings == null || (occurrence == null && !asHome)) {
        Surface(Modifier.fillMaxSize(), color = style.palette().background) { }
        return
    }
    // The session shown in the pill: the minimal one, or any other that's on.
    val session = occurrence ?: state?.active?.maxByOrNull { it.end }
    val shownQuote = Quotes.forPeriod(now, style.quoteRefresh, quoteOffset, settings.quotes)
    // What the drawer and its search can open: in a session, its apps and the always-available ones;
    // otherwise everything you haven't hidden.
    val openable = lists.all.mapNotNull(byPackage::get)
    val closeDrawer = {
        drawer = false
        query = ""
    }
    // A quick note, from the search or a gesture: to the app's own notes, or one of your files.
    val saveNote: (String, NoteTarget?) -> Unit = { words, target ->
        scope.launch { Toast.makeText(context, Notes.save(context, repo, words, target), Toast.LENGTH_SHORT).show() }
    }
    // What a gesture on the home screen does, as you set it.
    val onGesture: (HomeGesture) -> Unit = { gesture ->
        when (val action = style.homeGestures[gesture]) {
            null -> Unit
            HomeActions.NOTIFICATIONS -> BlockingState.globalAction(context, AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            HomeActions.LOCK -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) BlockingState.globalAction(context, AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            HomeActions.NOTE -> quickNote = true
            HomeActions.SEARCH -> drawer = true
            HomeActions.NOTES -> notesOpen = true
            HomeActions.WEEK -> scope.launch { week = loadCalendarWeek(context, LocalDateTime.now()) }
            HomeActions.BLOCKS -> blocksOpen = true
            HomeActions.CAMERA -> runCatching {
                context.startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            else -> AppCatalog.launch(context, action)
        }
    }
    val searchChoices = SearchOrder.arrange(SearchOrder.defaults(searchTargets, settings.quickTargets, obsidian), settings.searchChoices)
    // A keyword first ("m cafe") sends the words after it straight to one place.
    val keywords = Keywords.effective(settings.keywords, settings.keywordsCustom, Keywords.defaults(searchChoices, settings.quickTargets))
    val keyword = Keywords.match(query, keywords)
    val words = keyword?.words ?: query.trim()
    // Contacts are looked up as you type, only when you've switched them on in the search or used their keyword.
    val contactsOn = searchChoices.any { it.key == SearchOrder.CONTACTS && it.on } || keyword?.key == SearchOrder.CONTACTS
    // Words that start with someone's name and go on are a message for them: "@anna running late".
    val contactsFound by produceState(emptyList<ContactHit>() to "", words, contactsOn) {
        value = if (contactsOn && words.length >= 2) withContext(Dispatchers.IO) {
            val byFirstWord = ContactSearch.find(context, words.substringBefore(' '), limit = 12)
            val (named, message) = ContactSearch.split(words, byFirstWord.map { it.name })
            if (named.isNotEmpty()) byFirstWord.filter { it.name in named }.take(3) to message else ContactSearch.find(context, words) to ""
        } else {
            emptyList<ContactHit>() to ""
        }
    }
    val (contacts, contactMessage) = contactsFound
    val chatApps = remember(settings.contactApps) { ChatApp.chosen(settings.contactApps, ContactSearch.chatApps(context)) }
    val openablePackages = openable.map { it.packageName }.toSet()
    // An alarm or timer, settings pages (when Settings could be opened now), and notes and files by name.
    val found = buildMap {
        Calculator.evaluate(words)?.let { put(SearchOrder.CALCULATOR, listOf(DrawerResult.Calculation(Calculator.format(it)))) }
        if (words.length >= 2) put(SearchOrder.BLOCKS, blocks.filter { it.name.contains(words, ignoreCase = true) }.map { DrawerResult.StartBlock(it) })
        // With its keyword just the time ("a 7:30", "t 10"); without, starting "alarm" or "timer".
        val clock = ClockActions.parse(words)
        (if (keyword?.key == SearchOrder.ALARM) ClockActions.alarm(words) else clock as? app.bedtime.data.ClockAction.Alarm)
            ?.let { put(SearchOrder.ALARM, listOf(DrawerResult.Clock(it))) }
        (if (keyword?.key == SearchOrder.TIMER) ClockActions.timer(words) else clock as? app.bedtime.data.ClockAction.Timer)
            ?.let { put(SearchOrder.TIMER, listOf(DrawerResult.Clock(it))) }
        if (settingsApp == null || settingsApp in openablePackages) put(SearchOrder.SETTINGS, SettingsPages.match(words).map { DrawerResult.Setting(it) })
        if (FolderIndex.OBSIDIAN in openablePackages) put(SearchOrder.OBSIDIAN, FolderIndex.match(vault.second, words, name = FolderIndex::title).map { DrawerResult.VaultNote(it) })
        put(SearchOrder.FILES, FolderIndex.match(folderFiles, words).map { DrawerResult.File(it) })
    }
    val open: (Intent) -> Unit = { intent ->
        runCatching { context.startActivity(intent) }
            .onSuccess { closeDrawer() }
            .onFailure { Toast.makeText(context, "couldn't open that", Toast.LENGTH_SHORT).show() }
    }
    val onResult: (DrawerResult) -> Unit = { result ->
        when (result) {
            is DrawerResult.App -> {
                AppCatalog.launch(context, result.app.packageName)
                closeDrawer()
            }
            is DrawerResult.Search -> {
                runCatching { context.startActivity(SearchTargets.intent(result.target, words)) }
                    .onFailure { Toast.makeText(context, "couldn't open that", Toast.LENGTH_SHORT).show() }
                closeDrawer()
            }
            is DrawerResult.AddNote -> {
                saveNote(words, result.target)
                query = ""
            }
            is DrawerResult.AddEvent -> if (CalendarSource.hasPermission(context) && CalendarSource.canWrite(context)) {
                saveEvent(result.event)
            } else {
                pendingEvent = result.event
                askCalendar.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
            }
            is DrawerResult.Contact -> {
                runCatching { context.startActivity(ContactSearch.call(result.hit)) }
                closeDrawer()
            }
            is DrawerResult.Chat -> {
                val chat = ContactSearch.chat(result.app, result.hit, result.text)
                if (chat == null || runCatching { context.startActivity(chat) }.isFailure) {
                    Toast.makeText(context, "${result.app.label} needs their number with its country code", Toast.LENGTH_SHORT).show()
                } else {
                    // Apps whose links can't carry a message get it copied, to paste.
                    if (result.text.isNotBlank() && !result.app.takesText) {
                        copy(context, result.text)
                        Toast.makeText(context, "message copied: paste it in ${result.app.label}", Toast.LENGTH_SHORT).show()
                    }
                    closeDrawer()
                }
            }
            is DrawerResult.StartBlock -> {
                startBlock(result.block)
                closeDrawer()
            }
            is DrawerResult.Calculation -> {
                copy(context, result.answer)
                Toast.makeText(context, "copied ${result.answer}", Toast.LENGTH_SHORT).show()
            }
            DrawerResult.OpenNotes -> {
                notesOpen = true
                closeDrawer()
            }
            is DrawerResult.Clock -> runCatching { context.startActivity(ClockActions.intent(result.action)) }
                .onSuccess {
                    Toast.makeText(context, if (result.action is app.bedtime.data.ClockAction.Alarm) "alarm set" else "timer started", Toast.LENGTH_SHORT).show()
                    query = ""
                }
                .onFailure { Toast.makeText(context, "no clock app took it", Toast.LENGTH_SHORT).show() }
            is DrawerResult.Setting -> open(SettingsPages.intent(result.page))
            is DrawerResult.VaultNote -> open(FolderIndex.obsidianIntent(vault.first, result.file))
            is DrawerResult.File -> open(FolderIndex.openIntent(result.file))
        }
    }
    when {
        notesOpen -> {
            BackHandler { notesOpen = false }
            NotesScreen(onBack = { notesOpen = false })
        }
        drawer -> {
            BackHandler { closeDrawer() }
            AppDrawerContent(
                apps = openable,
                style = style,
                query = query,
                onQuery = { query = it },
                results = AppDrawer.results(
                    query,
                    openable,
                    SearchTargets.offered(searchTargets, openablePackages),
                    settings.quickTargets,
                    EventParser.parse(words, LocalDateTime.now()),
                    searchChoices,
                    contacts,
                    keyword,
                    found,
                    message = contactMessage,
                ),
                chatApps = chatApps,
                words = words,
                keyworded = keyword != null,
                onLaunch = { pkg ->
                    AppCatalog.launch(context, pkg)
                    closeDrawer()
                },
                onResult = onResult,
                onLongPress = { actionsFor = it },
            )
        }
        emergency && occurrence != null -> {
            BackHandler { emergency = false }
            EmergencyContent(
                onBack = { emergency = false },
                onCall = { SystemApps.dialerPackage(context)?.let { AppCatalog.launch(context, it) } },
                onOpenRefuge = { AppCatalog.launch(context, context.packageName) },
                onPauseBlocking = {
                    if (!BlockingState.pause(occurrence.schedule.breakMinutes)) context.startActivity(BlockingState.accessibilityIntent())
                    emergency = false
                },
                breakMinutes = occurrence.schedule.breakMinutes,
                pauseEnabled = occurrence.schedule.pauseEnabled,
            )
        }
        unlocking && occurrence != null -> Surface(Modifier.fillMaxSize(), color = c.bgPrimary) {
            key(occurrence.schedule.id) {
                UnlockFlow(
                    occurrence,
                    onUnlocked = { },
                    onCancel = { unlocking = false },
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .imePadding(),
                )
            }
        }
        else -> MinimalHomeContent(
            now = now,
            scheduleName = session?.schedule?.name,
            until = session?.let { formatTime(context, it.end) },
            scheduleIcon = session?.schedule?.icon,
            apps = apps,
            style = style,
            onLaunch = { AppCatalog.launch(context, it) },
            // Unlocking and emergencies belong to minimal sessions; your everyday home has a way into the app.
            onUnlock = if (occurrence != null) ({ unlocking = true }) else null,
            onEmergency = { emergency = true },
            onAppLongPress = { actionsFor = it },
            emptyHint = if (occurrence == null) "Hold an app in the full list to add it here." else null,
            // Re-read every second with the clock, so it goes back to "unlock" once the moment passes.
            onUndo = if (occurrence != null && occurrence.schedule.isBlock && Repository.canUndo(occurrence.start, System.currentTimeMillis())) {
                { scope.launch { repo.undoStart(occurrence.schedule.id) } }
            } else {
                null
            },
            quote = shownQuote,
            quoteFavourite = shownQuote.text in settings.quotes.favourites,
            onQuoteFavourite = { quoteActions.favourite(shownQuote) },
            onQuoteHide = { quoteActions.hide(shownQuote) },
            calendar = rememberCalendarLine(style, now, onLockScreen = false),
            onCalendar = { scope.launch { week = loadCalendarWeek(context, LocalDateTime.now()) } },
            onOpenDrawer = { drawer = true },
            onOpenRefuge = { AppCatalog.launch(context, context.packageName) },
            onRefugeHold = { blocksOpen = true },
            onOpenNotes = { notesOpen = true },
            onWidgetHold = { holding = it },
            onScreenHold = { screenSheet = true },
            note = if (Modules.isOn(style, Screen.HOME, Module.NOTE)) {
                {
                    NoteWidgetView(
                        widgetNote,
                        style.palette().text, style.palette().faint, style.palette().accent,
                        Modifier.padding(top = 22.dp),
                        maxLines = style.noteLines,
                        card = style.noteBackground?.let { androidx.compose.ui.graphics.Color(it) },
                        onToggle = { i -> widgetNote?.let { n -> scope.launch { repo.editNote(n.id, NoteWidget.toggled(n.text, i)) } } },
                        onSave = { written -> widgetNote?.let { n -> scope.launch { repo.editNote(n.id, written) } } },
                        onHold = { holding = Module.NOTE },
                    )
                }
            } else {
                null
            },
            // Small, beside the clock, where you've asked and it fits.
            noteBeside = if (Modules.isOn(style, Screen.HOME, Module.NOTE) && style.noteBeside(Screen.HOME) && style.noteFitsBesideClock(Screen.HOME)) {
                {
                    NoteWidgetView(
                        widgetNote,
                        style.palette().text, style.palette().faint, style.palette().accent,
                        maxLines = 3,
                        compact = true,
                        onToggle = { i -> widgetNote?.let { n -> scope.launch { repo.editNote(n.id, NoteWidget.toggled(n.text, i)) } } },
                        onSave = { written -> widgetNote?.let { n -> scope.launch { repo.editNote(n.id, written) } } },
                        onHold = { holding = Module.NOTE },
                    )
                }
            } else {
                null
            },
            nowPlaying = nowPlaying?.takeIf { Modules.isOn(style, Screen.HOME, Module.NOW_PLAYING) }?.let { playing ->
                { NowPlayingWidget(playing, style.palette().text, style.palette().faint, Modifier.padding(top = 20.dp), onOpen = { AppCatalog.launch(context, it) }) }
            },
            habits = habitList.filter { it.shownOn(Screen.HOME) }.takeIf { it.isNotEmpty() && Modules.isOn(style, Screen.HOME, Module.HABITS) }?.let { list ->
                {
                    val today = HourlyUse.today()
                    HabitsWidget(
                        list, habitMarks, today, style.palette().text, style.palette().faint, style.palette().accent,
                        Modifier.padding(top = 22.dp),
                        onTap = { h -> scope.launch { repo.markHabit(h.id, today, Habits.tapped(h, habitMarks[h.id]?.get(today) ?: 0)) } },
                        onOpen = { openHabit = it.id },
                        onHold = { holding = Module.HABITS },
                    )
                }
            },
            onGesture = onGesture,
            hours = todayHours.takeIf { Modules.isOn(style, Screen.HOME, Module.HOURS) && HourlyUse.counting(settings) }
                ?.let { HourlyUse.states(it, now.hour, HourlyUse.rule(settings)) },
            onHours = { hoursDetail = true },
            weather = rememberWeather(style, now),
            onWeather = breezy?.let { app -> { AppCatalog.launch(context, app) } },
            onNextQuote = { quoteOffset++ },
        )
    }
    week?.let {
        CalendarWeekDialog(it, style, onDismiss = { week = null }, onOpenEvent = { event ->
            runCatching { context.startActivity(CalendarSource.openIntent(event)) }
                .onFailure { Toast.makeText(context, "no calendar app opened it", Toast.LENGTH_SHORT).show() }
            week = null
        })
    }
    habitList.firstOrNull { it.id == openHabit }?.let { habit ->
        val p = style.palette()
        androidx.compose.ui.window.Dialog(onDismissRequest = { openHabit = null }) {
            HabitDetailContent(habit, habitMarks[habit.id].orEmpty(), HourlyUse.today(), p.background, p.text, p.faint, p.accent, onSet = { day, value ->
                scope.launch { repo.markHabit(habit.id, day, value) }
            }, onOpenAll = {
                openHabit = null
                runCatching { context.startActivity(app.bedtime.MainActivity.intent(context, app.bedtime.Routes.HABITS)) }
            })
        }
    }
    holding?.let { module ->
        // Hiding goes to the running session's own list when it has one, otherwise to your usual widgets.
        val sessions = state?.active.orEmpty().map { it.start to it.schedule }
        val owner = Modules.owner(sessions, Screen.HOME)
        WidgetSheet(
            module,
            style,
            sessionOwned = owner != null,
            handlers = WidgetHandlers(
                hide = {
                    scope.launch {
                        if (owner != null) {
                            repo.upsert(owner.copy(homeModules = owner.homeModules.orEmpty().map { if (it.module == module) it.copy(on = false) else it }))
                        } else {
                            repo.updateSettings { s -> s.copy(homeStyle = Modules.switched(s.homeStyle, Screen.HOME, module, false)) }
                        }
                    }
                },
                change = { changed ->
                    scope.launch { repo.updateSettings { s -> s.copy(homeStyle = s.homeStyle.copy(weatherStyle = changed.weatherStyle, lockWeatherStyle = changed.lockWeatherStyle, clockSize = changed.clockSize, quoteRefresh = changed.quoteRefresh, noteWidgetId = changed.noteWidgetId, noteLines = changed.noteLines, noteBesideClock = changed.noteBesideClock, lockNoteBesideClock = changed.lockNoteBesideClock, calendarIconWhenEmpty = changed.calendarIconWhenEmpty, lockCalendarIconWhenEmpty = changed.lockCalendarIconWhenEmpty)) } }
                },
                open = { route -> runCatching { context.startActivity(app.bedtime.MainActivity.intent(context, route)) } },
                habitShown = { id, shown -> scope.launch { repo.updateSettings { s -> s.copy(habits = s.habits.map { if (it.id == id) it.withShownOn(Screen.HOME, shown) else it }) } } },
            ),
            onDismiss = { holding = null },
            notes = allNotes,
            screen = Screen.HOME,
            habits = habitList,
        )
    }
    if (screenSheet) {
        val sessions = state?.active.orEmpty().map { it.start to it.schedule }
        val owner = Modules.owner(sessions, Screen.HOME)
        ScreenSheet(
            Screen.HOME,
            style,
            Modules.of(style, Screen.HOME),
            sessionOwned = owner != null,
            onSwitch = { module, on ->
                scope.launch {
                    if (owner != null) {
                        repo.upsert(owner.copy(homeModules = Modules.arrange(owner.homeModules.orEmpty()).map { if (it.module == module) it.copy(on = on) else it }))
                    } else {
                        repo.updateSettings { s -> s.copy(homeStyle = Modules.switched(s.homeStyle, Screen.HOME, module, on)) }
                    }
                }
            },
            onOpen = { route -> runCatching { context.startActivity(app.bedtime.MainActivity.intent(context, route)) } },
            onDismiss = { screenSheet = false },
        )
    }
    if (blocksOpen) {
        BlockSheet(blocks, state?.active.orEmpty().map { it.schedule.id }.toSet(), style, onStart = { block ->
            startBlock(block)
            blocksOpen = false
        }, onDismiss = { blocksOpen = false })
    }
    if (quickNote) QuickNoteDialog(style, settings.quickTargets, onSave = saveNote, onDismiss = { quickNote = false })
    if (hoursDetail) {
        val p = style.palette()
        HourDetailDialog(todayHours, HourlyUse.rule(settings), p.background, p.text, p.faint, p.accent, onDismiss = { hoursDetail = false }, onStats = {
            runCatching { context.startActivity(app.bedtime.MainActivity.intent(context, app.bedtime.Routes.STATS)) }
        })
    }
    actionsFor?.let { app ->
        val shortcuts by produceState(emptyList<ShortcutInfo>(), app.packageName) {
            value = withContext(Dispatchers.IO) { AppShortcuts.of(context, app.packageName) }
        }
        fun save(transform: (AppSettings) -> AppSettings) = scope.launch { repo.updateSettings(transform) }
        val favourite = app.packageName in settings.homeFavourites
        AppActionsDialog(
            app = app,
            style = style,
            favourite = favourite,
            hidden = app.packageName in settings.hiddenApps,
            canUninstall = state?.isActive != true,
            shortcuts = shortcuts.map { AppShortcut(it.id, (it.shortLabel ?: it.longLabel ?: "").toString()) },
            handlers = AppActionHandlers(
                toggleFavourite = {
                    save { s -> s.copy(homeFavourites = if (favourite) s.homeFavourites - app.packageName else s.homeFavourites + app.packageName) }
                },
                move = { up -> save { s -> s.copy(homeFavourites = HomeApps.moved(s.homeFavourites, app.packageName, up)) } },
                rename = { name ->
                    // An empty name, or the app's own, goes back to the app's own.
                    val own = installed.firstOrNull { it.packageName == app.packageName }?.label
                    save { s -> s.copy(appNames = if (name.isBlank() || name == own) s.appNames - app.packageName else s.appNames + (app.packageName to name)) }
                },
                toggleHidden = {
                    save { s -> s.copy(hiddenApps = if (app.packageName in s.hiddenApps) s.hiddenApps - app.packageName else s.hiddenApps + app.packageName) }
                },
                info = { runCatching { context.startActivity(AppShortcuts.infoIntent(app.packageName)) } },
                uninstall = { runCatching { context.startActivity(AppShortcuts.uninstallIntent(app.packageName)) } },
                shortcut = { chosen -> shortcuts.firstOrNull { it.id == chosen.id }?.let { AppShortcuts.start(context, it) } },
                setKeyword = { typed ->
                    val key = Keywords.appKey(app.packageName)
                    val clean = Keywords.clean(typed)
                    val now = Keywords.effective(settings.keywords, settings.keywordsCustom, Keywords.defaults(searchChoices, settings.quickTargets))
                    val taken = Keywords.takenBy(now, key, clean)
                    if (taken == null) {
                        save { s -> s.copy(keywords = if (clean.isEmpty()) now - key else now + (key to clean), keywordsCustom = true) }
                        null
                    } else if (taken.startsWith(Keywords.APP)) {
                        AppCatalog.label(context, taken.removePrefix(Keywords.APP))
                    } else {
                        searchKeyLabel(taken)
                    }
                },
            ),
            keyword = keywords[Keywords.appKey(app.packageName)].orEmpty(),
            onDismiss = { actionsFor = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MinimalHomeContent(
    now: LocalDateTime,
    /** The session in the pill; both null outside sessions, when there's no pill. */
    scheduleName: String?,
    until: String?,
    apps: List<AppEntry>,
    style: HomeStyle,
    onLaunch: (String) -> Unit,
    /** Unlocking, in a minimal session; null outside one, when the foot has only the way to all apps. */
    onUnlock: (() -> Unit)?,
    onEmergency: () -> Unit = {},
    /** A long press on an app: its actions. */
    onAppLongPress: ((AppEntry) -> Unit)? = null,
    /** What an empty list says; the session's words when null. */
    emptyHint: String? = null,
    /** Set for the first seconds of a block, when starting it can still be taken back. */
    onUndo: (() -> Unit)? = null,
    /** Today's next event, when the calendar line is on. */
    calendar: String? = null,
    /** Opens the week ahead, from a tap on the calendar line. */
    onCalendar: (() -> Unit)? = null,
    /** The weather beside the clock, when it's on. */
    weather: WeatherView? = null,
    /** Opens the weather app, from a tap on the weather. */
    onWeather: (() -> Unit)? = null,
    /** Opens every available app with search, from a swipe up or the handle at the foot. */
    onOpenDrawer: () -> Unit = {},
    /** A swipe down, left or right, or a double tap on an empty spot. */
    onGesture: (HomeGesture) -> Unit = {},
    /** Today by the hour, when it's shown here, and a tap for the minutes. */
    hours: List<HourState>? = null,
    onHours: (() -> Unit)? = null,
    quote: Quote? = null,
    /** Whether [quote] is one of your favourites, and the long-press menu's two choices for it. */
    quoteFavourite: Boolean = false,
    onQuoteFavourite: () -> Unit = {},
    onQuoteHide: () -> Unit = {},
    onNextQuote: () -> Unit = {},
    /** Opens digital refuge, from a tap on its mark at the foot. */
    onOpenRefuge: () -> Unit = {},
    /** A long press on the refuge mark: your blocks, to start one. */
    onRefugeHold: (() -> Unit)? = null,
    /** The notes button at the bottom right, where you've switched it on. */
    onOpenNotes: () -> Unit = {},
    /** A long press on a widget: its own sheet, to hide or change it. */
    onWidgetHold: ((Module) -> Unit)? = null,
    /** A long press on an empty spot: this screen's widgets, and the way to their settings. */
    onScreenHold: (() -> Unit)? = null,
    /** The session's mark on its pill. */
    scheduleIcon: String? = null,
    /** The habits and now-playing widgets, drawn by the screen, when they're on. */
    habits: (@Composable () -> Unit)? = null,
    nowPlaying: (@Composable () -> Unit)? = null,
    note: (@Composable () -> Unit)? = null,
    noteBeside: (@Composable () -> Unit)? = null,
    preview: Boolean = false,
) {
    val p = style.palette()
    val scale = style.textSize.scale
    val names = scale * style.appNameSize.scale
    val pull = with(LocalDensity.current) { 72.dp.toPx() }
    // Pulling on past the end of the app list opens the drawer, as a swipe up does anywhere else.
    val overscroll = remember(onOpenDrawer, onGesture) {
        object : NestedScrollConnection {
            var travel = 0f

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y != 0f) {
                    // Past the end opens all apps; past the top is a swipe down, as anywhere else.
                    travel += available.y
                    if (travel < -pull) {
                        travel = 0f
                        onOpenDrawer()
                    } else if (travel > pull) {
                        travel = 0f
                        onGesture(HomeGesture.SWIPE_DOWN)
                    }
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                travel = 0f
                return Velocity.Zero
            }
        }
    }
    WithFontFamily(AppFonts.family(LocalContext.current, style.font)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(p.background)
                .pointerInput(onOpenDrawer, onGesture) {
                    var travel = Offset.Zero
                    detectDragGestures(
                        onDragStart = { travel = Offset.Zero },
                        onDragEnd = {
                            when (val read = readHomeDrag(travel.x, travel.y, pull)) {
                                HomeDrag.None -> Unit
                                HomeDrag.AllApps -> onOpenDrawer()
                                is HomeDrag.Gesture -> onGesture(read.gesture)
                            }
                        },
                    ) { _, amount -> travel += amount }
                }
                // Only on empty spots: apps and the quote take their own taps first.
                .pointerInput(onGesture, onScreenHold) {
                    detectTapGestures(onDoubleTap = { onGesture(HomeGesture.DOUBLE_TAP) }, onLongPress = onScreenHold?.let { { _ -> it() } })
                }
                .then(if (preview) Modifier else Modifier.safeDrawingPadding())
                .padding(horizontal = 28.dp)
                .padding(bottom = 24.dp),
        ) {
            Spacer(Modifier.height(if (preview) 12.dp else style.topSpace.dp.dp))
            SessionHeader(
                now, scheduleName, until, style, calendar, onCalendar, weather, onWeather,
                modules = Modules.shown(style, Screen.HOME),
                scheduleIcon = scheduleIcon,
                hours = hours?.let { states ->
                    {
                        HourDots(
                            states,
                            text = style.palette().text,
                            accent = style.palette().accent,
                            modifier = Modifier.padding(top = 12.dp),
                            calmColor = style.hourCalmColor,
                            distractedColor = style.hourDistractedColor,
                            onClick = onHours,
                            onLongClick = onWidgetHold?.let { { it(Module.HOURS) } },
                        )
                    }
                },
                quote = quote?.let { q -> { HomeQuote(q, style, preview, quoteFavourite, onQuoteFavourite, onQuoteHide, onNextQuote, onWidgetHold?.let { { it(Module.QUOTE) } }) } },
                habits = habits,
                nowPlaying = nowPlaying,
                note = note,
                noteBeside = noteBeside,
                onHold = onWidgetHold,
            )
            Spacer(Modifier.height(if (preview) 28.dp else 44.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth().nestedScroll(overscroll)) {
                items(apps, key = { it.packageName }) { app ->
                    // An app's tap reaches half the width (or its whole name), leaving the right side free
                    // for a double tap.
                    BoxWithConstraints(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier
                                .widthIn(min = maxWidth / 2)
                                .clip(RoundedCornerShape(8.dp))
                                .combinedClickable(
                                    onClickLabel = "open ${app.label}",
                                    onLongClickLabel = "more for ${app.label}",
                                    onLongClick = onAppLongPress?.let { { it(app) } },
                                    onClick = { onLaunch(app.packageName) },
                                )
                                .padding(vertical = (12 * names).dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (style.showIcons) {
                                AppIcon(app.packageName, app.label, (30 * names).dp)
                                Spacer(Modifier.width(16.dp))
                            }
                            Text(app.label, fontSize = (26 * names).sp, color = p.text)
                        }
                    }
                }
                if (apps.isEmpty()) {
                    item { Text(emptyHint ?: "nothing to open right now. enjoy the quiet.", fontSize = (18 * scale).sp, color = p.faint) }
                }
            }
            // A tap opens digital refuge; a long press offers your blocks.
            val logo = @Composable {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .combinedClickable(onClickLabel = "open digital refuge", onLongClickLabel = "start a block", onLongClick = onRefugeHold, onClick = onOpenRefuge),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(BedtimeIcons.Refuge, contentDescription = "digital refuge", tint = p.faint, modifier = Modifier.size(26.dp))
                }
            }
            val notesButton = @Composable {
                IconButton(onClick = onOpenNotes) {
                    Icon(Icons.Default.Edit, contentDescription = "refuge notes", tint = p.faint, modifier = Modifier.size(22.dp))
                }
            }
            if (onUnlock == null) {
                // Your everyday home: the way to all apps (digital refuge itself is among them), and its mark if you like.
                if (style.showLogo || style.showSwipeArrow || style.showNotesButton) Box(Modifier.fillMaxWidth()) {
                    if (style.showLogo) Box(Modifier.align(Alignment.CenterStart)) { logo() }
                    if (style.showNotesButton) Box(Modifier.align(Alignment.CenterEnd)) { notesButton() }
                    if (style.showSwipeArrow) {
                        IconButton(onClick = onOpenDrawer, modifier = Modifier.align(Alignment.Center)) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "all apps and search", tint = p.faint)
                        }
                    }
                }
            } else Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (style.showLogo) logo()
                if (onUndo != null) {
                    TextButton(onClick = onUndo) { Text("undo", color = p.accent, fontWeight = FontWeight.SemiBold) }
                } else {
                    TextButton(onClick = onUnlock) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = p.faint, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("unlock", color = p.faint)
                    }
                }
                Spacer(Modifier.weight(1f))
                if (style.showSwipeArrow) {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "all apps and search", tint = p.faint)
                    }
                    Spacer(Modifier.weight(1f))
                }
                if (style.showNotesButton) notesButton()
                TextButton(onClick = onEmergency) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = p.faint, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("emergency", color = p.faint)
                }
            }
        }
    }
}

/** [text] on the clipboard, to paste somewhere else. */
private fun copy(context: Context, text: String) {
    context.getSystemService(android.content.ClipboardManager::class.java)
        ?.setPrimaryClip(android.content.ClipData.newPlainText("digital refuge", text))
}

/** The quote on the minimal screen: a tap moves on, as it always has; a long press offers to keep it or never see it again. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeQuote(
    quote: Quote,
    style: HomeStyle,
    preview: Boolean,
    favourite: Boolean,
    onFavourite: () -> Unit,
    onHide: () -> Unit,
    onNext: () -> Unit,
    onWidget: (() -> Unit)? = null,
) {
    Spacer(Modifier.height(if (preview) 20.dp else 28.dp))
    var menu by remember { mutableStateOf(false) }
    Box {
        QuoteBlock(
            quote,
            style,
            Modifier.clip(RoundedCornerShape(8.dp)).combinedClickable(
                onClickLabel = "show another quote",
                onLongClickLabel = "favourite or hide this quote",
                onLongClick = { menu = true },
                onClick = onNext,
            ),
        )
        QuoteMenu(expanded = menu, onDismiss = { menu = false }, favourite = favourite, onFavourite = onFavourite, onHide = onHide, onAnother = onNext, onWidget = onWidget)
    }
}

/** A search choice in words, for saying what already has a keyword. */
private fun searchKeyLabel(key: String): String = when {
    key.startsWith("web:") -> "web search"
    key.startsWith("maps:") -> "maps"
    key.startsWith("wikipedia:") -> "Wikipedia"
    key.startsWith("lookup:") -> "look up"
    key.startsWith("spotify:") -> "Spotify search"
    key == SearchOrder.REFUGE_NOTES -> "refuge notes"
    key.startsWith("note:") -> "a note file"
    key == SearchOrder.CALENDAR -> "the calendar"
    key == SearchOrder.CONTACTS -> "contacts"
    key == SearchOrder.ALARM -> "alarms"
    key == SearchOrder.TIMER -> "timers"
    key == SearchOrder.FILES -> "files"
    key == SearchOrder.OBSIDIAN -> "Obsidian notes"
    key == SearchOrder.BLOCKS -> "blocks"
    else -> key
}
