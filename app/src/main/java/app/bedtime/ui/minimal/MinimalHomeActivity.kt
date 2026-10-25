package app.bedtime.ui.minimal

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

/** The minimal launcher: a clock and a plain list of allowed apps, styled by the user's [HomeStyle]. */
class MinimalHomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(this) { /* Stay here; there is nowhere to go back to. */ }
        setContent {
            BedtimeTheme {
                ProvideAppIcons { MinimalHomeScreen(onFinish = ::finish, onLightBackground = ::useLightSystemBars) }
            }
        }
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
private fun MinimalHomeScreen(onFinish: () -> Unit, onLightBackground: (Boolean) -> Unit) {
    val context = LocalContext.current
    val c = Obsidian.colors
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val state by Engine.state(context).collectAsStateWithLifecycle()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val occurrence = state?.minimalOccurrence
    var unlocking by rememberSaveable { mutableStateOf(false) }
    var emergency by rememberSaveable { mutableStateOf(false) }
    var quoteOffset by rememberSaveable { mutableIntStateOf(0) }
    var week by remember { mutableStateOf<List<CalendarDay>?>(null) }
    var drawer by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable { mutableStateOf("") }
    var notesOpen by rememberSaveable { mutableStateOf(false) }
    val searchTargets by produceState(emptyList<SearchTarget>()) { value = withContext(Dispatchers.IO) { SearchTargets.find(context) } }
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
    val style = settings.homeStyle
    val light = style.palette().isLight

    LaunchedEffect(state) {
        if (state != null && occurrence == null) onFinish()
    }
    LaunchedEffect(light, unlocking, emergency) { onLightBackground(if (unlocking || emergency) !c.isDark else light) }

    val allowed = state?.minimalAllowlist.orEmpty()
    var apps by remember { mutableStateOf(emptyList<AppEntry>()) }
    LaunchedEffect(allowed) { apps = withContext(Dispatchers.IO) { entries(context, allowed + listOfNotNull(SystemApps.dialerPackage(context))) } }
    var alwaysAvailableApps by remember { mutableStateOf(emptyList<AppEntry>()) }
    LaunchedEffect(settings.alwaysAvailable) {
        alwaysAvailableApps = withContext(Dispatchers.IO) { entries(context, settings.alwaysAvailable) }
    }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000)
        }
    }

    if (occurrence == null) {
        Surface(Modifier.fillMaxSize(), color = style.palette().background) { }
        return
    }
    val shownQuote = Quotes.forPeriod(now, style.quoteRefresh, quoteOffset, settings.quotes)
    // What the drawer and its search can open: the session's apps and the always-available ones.
    val openable = (apps + alwaysAvailableApps).distinctBy { it.packageName }
    val closeDrawer = {
        drawer = false
        query = ""
    }
    val onResult: (DrawerResult) -> Unit = { result ->
        val words = query.trim()
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
                val target = result.target
                scope.launch {
                    if (target == null) {
                        repo.addNote(words)
                        Toast.makeText(context, "saved in refuge notes", Toast.LENGTH_SHORT).show()
                    } else {
                        val added = withContext(Dispatchers.IO) { Notes.append(context, Uri.parse(target.uri), Notes.line(words, LocalDateTime.now())) }
                        // Never lost: if the file can't be reached, it waits in the app's notes.
                        if (!added) repo.addNote(words)
                        Toast.makeText(
                            context,
                            if (added) "added to ${target.name.lowercase()}" else "couldn't reach ${target.name.lowercase()}, so it's in refuge notes",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                    query = ""
                }
            }
            is DrawerResult.AddEvent -> if (CalendarSource.hasPermission(context) && CalendarSource.canWrite(context)) {
                saveEvent(result.event)
            } else {
                pendingEvent = result.event
                askCalendar.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR))
            }
            DrawerResult.OpenNotes -> {
                notesOpen = true
                closeDrawer()
            }
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
                    SearchTargets.offered(searchTargets, openable.map { it.packageName }.toSet()),
                    settings.noteTargets,
                    EventParser.parse(query, LocalDateTime.now()),
                ),
                onLaunch = { pkg ->
                    AppCatalog.launch(context, pkg)
                    closeDrawer()
                },
                onResult = onResult,
            )
        }
        emergency -> {
            BackHandler { emergency = false }
            EmergencyContent(
                onBack = { emergency = false },
                onCall = { SystemApps.dialerPackage(context)?.let { AppCatalog.launch(context, it) } },
                alwaysAvailable = alwaysAvailableApps,
                onOpenApp = { AppCatalog.launch(context, it) },
                onOpenRefuge = { AppCatalog.launch(context, context.packageName) },
                onPauseBlocking = {
                    if (!BlockingState.pause(occurrence.schedule.breakMinutes)) context.startActivity(BlockingState.accessibilityIntent())
                    emergency = false
                },
                breakMinutes = occurrence.schedule.breakMinutes,
                pauseEnabled = occurrence.schedule.pauseEnabled,
            )
        }
        unlocking -> Surface(Modifier.fillMaxSize(), color = c.bgPrimary) {
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
            scheduleName = occurrence.schedule.name,
            until = formatTime(context, occurrence.end),
            apps = apps,
            style = style,
            onLaunch = { AppCatalog.launch(context, it) },
            onUnlock = { unlocking = true },
            onEmergency = { emergency = true },
            // Re-read every second with the clock, so it goes back to "unlock" once the moment passes.
            onUndo = if (occurrence.schedule.isBlock && Repository.canUndo(occurrence.start, System.currentTimeMillis())) {
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
            weather = rememberWeatherLine(style, now),
            onNextQuote = { quoteOffset++ },
        )
    }
    week?.let { CalendarWeekDialog(it, style, onDismiss = { week = null }) }
}

/** Launchable apps among [packages], with labels, sorted by name. */
private fun entries(context: Context, packages: Set<String>): List<AppEntry> =
    packages
        .filter { AppCatalog.isLaunchable(context, it) }
        .map { AppEntry(it, AppCatalog.label(context, it)) }
        .sortedBy { it.label.lowercase() }

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun MinimalHomeContent(
    now: LocalDateTime,
    scheduleName: String,
    until: String,
    apps: List<AppEntry>,
    style: HomeStyle,
    onLaunch: (String) -> Unit,
    onUnlock: () -> Unit,
    onEmergency: () -> Unit = {},
    /** Set for the first seconds of a block, when starting it can still be taken back. */
    onUndo: (() -> Unit)? = null,
    /** Today's next event, when the calendar line is on. */
    calendar: String? = null,
    /** Opens the week ahead, from a tap on the calendar line. */
    onCalendar: (() -> Unit)? = null,
    /** The weather after the date, when it's on. */
    weather: String? = null,
    /** Opens every available app with search, from a swipe up or the handle at the foot. */
    onOpenDrawer: () -> Unit = {},
    quote: Quote? = null,
    /** Whether [quote] is one of your favourites, and the long-press menu's two choices for it. */
    quoteFavourite: Boolean = false,
    onQuoteFavourite: () -> Unit = {},
    onQuoteHide: () -> Unit = {},
    onNextQuote: () -> Unit = {},
    preview: Boolean = false,
) {
    val p = style.palette()
    val scale = style.textSize.scale
    val names = scale * style.appNameSize.scale
    val pull = with(LocalDensity.current) { 72.dp.toPx() }
    // Pulling on past the end of the app list opens the drawer, as a swipe up does anywhere else.
    val overscroll = remember(onOpenDrawer) {
        object : NestedScrollConnection {
            var travel = 0f

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y < 0) {
                    travel -= available.y
                    if (travel > pull) {
                        travel = 0f
                        onOpenDrawer()
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
                .pointerInput(onOpenDrawer) {
                    var travel = 0f
                    detectVerticalDragGestures(onDragStart = { travel = 0f }, onDragEnd = { if (travel < -pull) onOpenDrawer() }) { _, amount -> travel += amount }
                }
                .then(if (preview) Modifier else Modifier.safeDrawingPadding())
                .padding(horizontal = 28.dp, vertical = 24.dp),
        ) {
            Spacer(Modifier.height(if (preview) 12.dp else 32.dp))
            SessionHeader(now, scheduleName, until, style, calendar, onCalendar, weather)
            if (quote != null && style.showQuote) {
                Spacer(Modifier.height(if (preview) 20.dp else 28.dp))
                // A tap moves on, as it always has; a long press offers to keep it or never see it again.
                var menu by remember { mutableStateOf(false) }
                Box {
                    QuoteBlock(
                        quote,
                        style,
                        Modifier.clip(RoundedCornerShape(8.dp)).combinedClickable(
                            onClickLabel = "show another quote",
                            onLongClickLabel = "favourite or hide this quote",
                            onLongClick = { menu = true },
                            onClick = onNextQuote,
                        ),
                    )
                    QuoteMenu(
                        expanded = menu,
                        onDismiss = { menu = false },
                        favourite = quoteFavourite,
                        onFavourite = onQuoteFavourite,
                        onHide = onQuoteHide,
                        onAnother = onNextQuote,
                    )
                }
            }
            Spacer(Modifier.height(if (preview) 28.dp else 44.dp))
            LazyColumn(Modifier.weight(1f).fillMaxWidth().nestedScroll(overscroll)) {
                items(apps, key = { it.packageName }) { app ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .clickable(onClickLabel = "open ${app.label}") { onLaunch(app.packageName) }
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
                if (apps.isEmpty()) {
                    item { Text("nothing to open right now. enjoy the quiet.", fontSize = (18 * scale).sp, color = p.faint) }
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
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
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "all apps and search", tint = p.faint)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onEmergency) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = p.faint, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("emergency", color = p.faint)
                }
            }
        }
    }
}
