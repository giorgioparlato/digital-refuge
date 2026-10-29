package app.bedtime.ui.homestyle

import app.bedtime.ui.components.TimeTile

import app.bedtime.ui.edit.TimePickerDialog

import app.bedtime.ui.settings.CircleScreen

import app.bedtime.data.Circles

import app.bedtime.data.Circle
import app.bedtime.data.RainAlert

import app.bedtime.data.ModuleChoice
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.Person
import app.bedtime.ui.settings.PeoplePickerScreen
import app.bedtime.data.ContactSearch
import app.bedtime.data.TopSpace
import androidx.compose.material.icons.filled.Refresh
import app.bedtime.data.HomeGesture
import app.bedtime.data.HomeActions
import app.bedtime.data.WeatherView
import app.bedtime.data.WeatherStyle
import app.bedtime.data.Weather
import androidx.compose.material.icons.filled.Edit
import app.bedtime.ui.apps.PickerMode
import app.bedtime.ui.apps.AppPickerScreen
import app.bedtime.service.LockNotificationListener
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.compose.material.icons.filled.Notifications
import android.provider.Settings
import android.os.Build
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.graphics.toArgb
import androidx.compose.foundation.layout.Spacer
import android.Manifest
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.heightIn
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.minimal.CalendarSource
import app.bedtime.data.GESTURE_CAMERA
import app.bedtime.data.Swipe
import app.bedtime.ui.components.QuickChip
import app.bedtime.ui.theme.AppFonts
import app.bedtime.data.AppFont
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.apps.AppCatalog
import app.bedtime.apps.AppEntry
import app.bedtime.data.AppSettings
import app.bedtime.data.HomeStyle
import app.bedtime.data.Quote
import app.bedtime.data.Quotes
import app.bedtime.data.Repository
import app.bedtime.data.TextSize
import app.bedtime.data.UseWords
import app.bedtime.data.UseWidget
import app.bedtime.data.UseLook
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Phone
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.DragList
import app.bedtime.data.Module
import app.bedtime.data.Modules
import app.bedtime.data.Screen
import app.bedtime.ui.lock.LockScreenContent
import app.bedtime.ui.components.ObsidianTextField
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.OptionRow
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.SegmentedChoice
import app.bedtime.ui.formatMinuteOfDay
import app.bedtime.ui.minimal.MinimalHomeContent
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

@Composable
fun HomeStyleScreen(
    onBack: () -> Unit,
    page: StylePage = StylePage.LOOK,
    screen: Screen = Screen.HOME,
    onOpen: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings: AppSettings? by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val notes by repo.notes.collectAsStateWithLifecycle(initialValue = emptyList())
    val weather by repo.weather.collectAsStateWithLifecycle(initialValue = null)
    var allApps by remember { mutableStateOf(emptyList<AppEntry>()) }
    LaunchedEffect(Unit) {
        allApps = withContext(Dispatchers.IO) { AppCatalog.launchableApps(context).sortedBy { it.label.lowercase() } }
    }
    // Calendar access is asked for only when the next event is switched on, and it stays off without it.
    var waitingCalendar by remember { mutableStateOf<HomeStyle?>(null) }
    val askCalendar = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val style = waitingCalendar
        waitingCalendar = null
        if (granted && style != null) {
            scope.launch { repo.updateSettings { it.copy(homeStyle = style) } }
        } else if (!granted) {
            Toast.makeText(context, "calendar access wasn't allowed", Toast.LENGTH_SHORT).show()
        }
    }
    // Notifications on the lock screen need notification access, granted on Android's own page. Asked
    // for only when switched on; it switches on by itself on return, once granted.
    var notificationAccess by remember { mutableStateOf(LockNotificationListener.hasAccess(context)) }
    var askedForNotifications by rememberSaveable { mutableStateOf(false) }
    var pickingNotificationApps by rememberSaveable { mutableStateOf(false) }
    /** The circle open to change, by id. */
    var editingCircle by rememberSaveable { mutableStateOf<String?>(null) }
    val switchOnNotifications = {
        scope.launch { repo.updateSettings { it.copy(homeStyle = it.homeStyle.copy(lockNotifications = true)) } }
        // Nothing shows until you choose whose notifications to see, so the choice comes straight away.
        if (settings?.homeStyle?.lockNotificationApps.isNullOrEmpty()) pickingNotificationApps = true
    }
    LifecycleResumeEffect(Unit) {
        notificationAccess = LockNotificationListener.hasAccess(context)
        if (askedForNotifications && notificationAccess) {
            askedForNotifications = false
            switchOnNotifications()
        }
        onPauseOrDispose { }
    }
    // Which choice "add your own font" was tapped under, so the new font lands there.
    var addingForQuote by rememberSaveable { mutableStateOf(false) }
    val pickFont = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val name = withContext(Dispatchers.IO) { AppFonts.importCustom(context, uri) }
            if (name == null) {
                Toast.makeText(context, "that file isn't a font this phone can read", Toast.LENGTH_SHORT).show()
                return@launch
            }
            repo.updateSettings { s ->
                val style = s.homeStyle.copy(customFontName = name)
                s.copy(homeStyle = if (addingForQuote) style.copy(quoteFont = AppFont.CUSTOM) else style.copy(font = AppFont.CUSTOM))
            }
        }
    }
    val current = settings ?: return
    // Circles are saved whole, as a list, the first time any of them changes.
    fun saveCircles(change: (List<Circle>) -> List<Circle>) {
        scope.launch { repo.updateSettings { it.copy(homeStyle = it.homeStyle.copy(circles = change(Circles.of(it.homeStyle)))) } }
    }
    val circles = Circles.of(current.homeStyle)
    circles.firstOrNull { it.id == editingCircle }?.let { circle ->
        CircleScreen(
            circle,
            onChange = { changed -> saveCircles { list -> list.map { if (it.id == changed.id) changed else it } } },
            onDelete = {
                saveCircles { list -> list.filterNot { it.id == circle.id } }
                editingCircle = null
            },
            onBack = { editingCircle = null },
        )
        return
    }
    if (pickingNotificationApps) {
        AppPickerScreen(
            "Notifications from",
            current.homeStyle.lockNotificationApps,
            PickerMode.NOTIFY,
            onDone = { apps ->
                scope.launch { repo.updateSettings { it.copy(homeStyle = it.homeStyle.copy(lockNotificationApps = apps)) } }
                pickingNotificationApps = false
            },
            onBack = { pickingNotificationApps = false },
        )
        return
    }
    HomeStyleContent(
        style = current.homeStyle,
        onChange = { style -> scope.launch { repo.updateSettings { it.copy(homeStyle = style) } } },
        previewApps = allApps.take(4),
        allApps = allApps,
        onShowCalendar = { style ->
            if (!CalendarSource.hasPermission(context)) {
                waitingCalendar = style
                askCalendar.launch(Manifest.permission.READ_CALENDAR)
            } else {
                scope.launch { repo.updateSettings { it.copy(homeStyle = style) } }
            }
        },
        onLockNotifications = { on ->
            if (on && !notificationAccess) {
                askedForNotifications = true
                val detail = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                val intent = if (detail) {
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                        .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, LockNotificationListener.component(context).flattenToString())
                } else {
                    Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                }
                runCatching { context.startActivity(intent) }
                    .recoverCatching { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
                Toast.makeText(context, "allow notification access for digital refuge", Toast.LENGTH_LONG).show()
            } else if (on) {
                switchOnNotifications()
            } else {
                scope.launch { repo.updateSettings { it.copy(homeStyle = it.homeStyle.copy(lockNotifications = false)) } }
            }
        },
        notificationAccess = notificationAccess,
        onPickNotificationApps = { pickingNotificationApps = true },
        weeklyNote = current.weeklyNote,
        onWeeklyNote = { on -> scope.launch { repo.updateSettings { it.copy(weeklyNote = on) } } },
        circles = circles,
        onEditCircle = { id -> editingCircle = id },
        onAddCircle = {
            val added = Circle(name = "Circle ${circles.size + 1}")
            saveCircles { it + added }
            editingCircle = added.id
        },
        weatherHeardAt = weather?.at,
        page = page,
        initialScreen = screen,
        notes = notes,
        habits = current.habits,
        onHabitShown = { on ->
            { id, shown -> scope.launch { repo.updateSettings { s -> s.copy(habits = s.habits.map { if (it.id == id) it.withShownOn(on, shown) else it }) } } }
        },
        onOpen = onOpen,
        // The preview shows the real weather if there is some, otherwise an example.
        weatherPreview = current.homeStyle.takeIf { Modules.onAnywhere(it, Module.WEATHER) }?.let { style ->
            Weather.view(weather ?: Weather.SAMPLE, (weather?.at ?: Weather.SAMPLE.at) + 1, Weather.fahrenheit(style.weatherUnit), style.weatherStyle)
        },
        weatherPreviewFor = { ws ->
            current.homeStyle.takeIf { Modules.onAnywhere(it, Module.WEATHER) }?.let { style ->
                Weather.view(weather ?: Weather.SAMPLE, (weather?.at ?: Weather.SAMPLE.at) + 1, Weather.fahrenheit(style.weatherUnit), ws)
            }
        },
        now = LocalDateTime.now(),
        onBack = onBack,
        quote = Quotes.forPeriod(LocalDateTime.now(), current.homeStyle.quoteRefresh, prefs = current.quotes),
        onAddFont = { forQuote ->
            addingForQuote = forQuote
            pickFont.launch(arrayOf("font/ttf", "font/otf", "font/sfnt", "application/x-font-ttf", "application/x-font-otf", "application/octet-stream"))
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun HomeStyleContent(
    style: HomeStyle,
    onChange: (HomeStyle) -> Unit,
    previewApps: List<AppEntry>,
    now: LocalDateTime,
    onBack: () -> Unit,
    quote: Quote? = null,
    /** Opens the file picker for your own font, for the quote or for the rest. */
    onAddFont: (forQuote: Boolean) -> Unit = {},
    /** Every app that can be opened, for the lock-screen swipes. */
    allApps: List<AppEntry> = emptyList(),
    /** [HomeStyle] with the next event switched on somewhere, saved once calendar access is there (asked for if needed). */
    onShowCalendar: (HomeStyle) -> Unit = {},
    /** Switches lock-screen notifications, sending you to grant notification access first if needed. */
    onLockNotifications: (Boolean) -> Unit = {},
    notificationAccess: Boolean = false,
    /** Opens the choice of apps whose notifications the lock screen shows. */
    onPickNotificationApps: () -> Unit = {},
    /** The Sunday note looking back on the week, and its switch. */
    weeklyNote: Boolean = false,
    onWeeklyNote: (Boolean) -> Unit = {},
    /** Your circles of people and group chats: each opens to change; another can be added, up to [Circles.MAX]. */
    circles: List<Circle> = emptyList(),
    onEditCircle: (String) -> Unit = {},
    onAddCircle: () -> Unit = {},
    /** When Breezy Weather last sent the weather, if ever. */
    weatherHeardAt: Long? = null,
    /** The weather as the preview shows it, and as it would be in another style. */
    weatherPreview: WeatherView? = null,
    weatherPreviewFor: ((WeatherStyle) -> WeatherView?)? = null,
    /** Which of the home and lock screen's pages this is. */
    page: StylePage = StylePage.LOOK,
    /** The screen Look and Widgets open on: the one you came from. */
    initialScreen: Screen = Screen.HOME,
    /** Your notes and habits, for the note and habits widgets to show some of. */
    notes: List<app.bedtime.data.Note> = emptyList(),
    habits: List<app.bedtime.data.Habit> = emptyList(),
    /** Shows or hides a habit in the habits widget on a screen. */
    onHabitShown: (Screen) -> (String, Boolean) -> Unit = { { _, _ -> } },
    /** Opens one of digital refuge's pages, from a widget's options. */
    onOpen: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val c = Obsidian.colors
    var pickingSwipe by rememberSaveable { mutableStateOf<Swipe?>(null) }
    var widgetScreen by rememberSaveable { mutableStateOf(initialScreen) }
    // Look and widgets are each screen's own: the lock screen's choices sit over the home screen's, so
    // these pages show the screen picked, and what's changed here changes for that screen.
    val screen = if (page == StylePage.LOOK || page == StylePage.SHOWN) widgetScreen else Screen.HOME
    val base = style
    val saveBase = onChange
    @Suppress("NAME_SHADOWING")
    val style = base.on(screen)
    @Suppress("NAME_SHADOWING")
    val onChange: (HomeStyle) -> Unit = { changed -> saveBase(base.edit(screen) { changed }) }
    var pickingGesture by rememberSaveable { mutableStateOf<HomeGesture?>(null) }
    pickingGesture?.let { gesture ->
        HomeGestureDialog(
            gesture = gesture,
            apps = allApps,
            onDismiss = { pickingGesture = null },
            onPick = { action ->
                onChange(style.copy(homeGestures = if (action == null) style.homeGestures - gesture else style.homeGestures + (gesture to action)))
                pickingGesture = null
            },
        )
    }
    var editingReplies by rememberSaveable { mutableStateOf(false) }
    if (editingReplies) {
        QuickRepliesDialog(style.quickReplies, onChange = { onChange(style.copy(quickReplies = it)) }, onDismiss = { editingReplies = false })
    }
    pickingSwipe?.let { swipe ->
        SwipeTargetDialog(
            swipe = swipe,
            apps = allApps,
            onDismiss = { pickingSwipe = null },
            onPick = { target ->
                onChange(style.copy(lockGestures = if (target == null) style.lockGestures - swipe else style.lockGestures + (swipe to target)))
                pickingSwipe = null
            },
        )
    }
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar(page.title, onBack = onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PageGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(page.intro, style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
            // Which screen: first, so the lock screen's own are easy to find; the preview follows it.
            if (page == StylePage.SHOWN || page == StylePage.LOOK) {
                SegmentedChoice(
                    options = listOf("Home screen", "Lock screen"),
                    selected = widgetScreen.ordinal,
                    onSelect = { widgetScreen = Screen.entries[it] },
                )
            }
            val previewShape = RoundedCornerShape(24.dp)
            if (page == StylePage.LOOK || page == StylePage.SHOWN) Box(
                Modifier
                    .fillMaxWidth()
                    .height(460.dp)
                    .clip(previewShape)
                    .border(1.dp, c.border, previewShape),
            ) {
                // The lock screen while it's the one being set; otherwise the minimal home.
                if (screen == Screen.LOCK) {
                    LockScreenContent(
                        now = now,
                        scheduleName = "Bedtime",
                        until = formatMinuteOfDay(context, 7 * 60),
                        style = style,
                        quote = quote,
                        weather = weatherPreviewFor?.invoke(style.weatherStyleFor(if (page == StylePage.SHOWN) widgetScreen else Screen.HOME)) ?: weatherPreview,
                        preview = true,
                    )
                } else {
                    MinimalHomeContent(
                        now = now,
                        scheduleName = "Bedtime",
                        until = formatMinuteOfDay(context, 7 * 60),
                        apps = previewApps,
                        style = style,
                        onLaunch = {},
                        onUnlock = {},
                        quote = quote,
                        weather = weatherPreviewFor?.invoke(style.weatherStyleFor(if (page == StylePage.SHOWN) widgetScreen else Screen.HOME)) ?: weatherPreview,
                        preview = true,
                    )
                }
            }

            if (page == StylePage.LOOK) {
                SectionCard(title = "Background") {
                    SwatchRow(BackgroundPresets, style.background) { onChange(style.copy(background = it)) }
                    HexField(style.background) { onChange(style.copy(background = it)) }
                }
                SectionCard(title = "Accent") {
                    SwatchRow(AccentPresets, style.accent) { onChange(style.copy(accent = it)) }
                    HexField(style.accent) { onChange(style.copy(accent = it)) }
                }
                SectionCard(title = "Text", subtitle = "Automatic is cream on a dark background, ink on a light one") {
                    QuickChip("Automatic", style.textColor == null) { onChange(style.copy(textColor = null)) }
                    Spacer(Modifier.height(12.dp))
                    SwatchRow(TextPresets, style.textColor ?: -1L) { onChange(style.copy(textColor = it)) }
                    HexField(style.textColor ?: style.palette().text.toArgb().toLong() and 0xFFFFFFFFL) { onChange(style.copy(textColor = it)) }
                }
                SectionCard(title = "Sizes") {
                    SizeLabel("Space at the top", Modifier.padding(bottom = 8.dp))
                    SegmentedChoice(
                        options = listOf("None", "Small", "Medium", "Large"),
                        selected = (if (screen == Screen.LOCK) style.lockTopSpace ?: style.topSpace else style.topSpace).ordinal,
                        onSelect = { onChange(if (screen == Screen.LOCK) style.copy(lockTopSpace = TopSpace.entries[it]) else style.copy(topSpace = TopSpace.entries[it])) },
                    )
                    SizeLabel("Text", Modifier.padding(top = 16.dp, bottom = 8.dp))
                    SegmentedChoice(
                        options = listOf("Small", "Medium", "Large"),
                        selected = style.textSize.ordinal,
                        onSelect = { onChange(style.copy(textSize = TextSize.entries[it])) },
                    )
                    Text(
                        if (screen == Screen.HOME) "The clock's size is in its widget's options; app names are with the app list." else "The clock's size is in its widget's options.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textFaint,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                SectionCard(title = "Fonts", subtitle = "Two choices: one for the clock, date and apps, one for the quote") {
                    SizeLabel("Clock, date and apps", Modifier.padding(bottom = 8.dp))
                    FontChoices(style.font, style.customFontName, onPick = { onChange(style.copy(font = it)) }, onAdd = { onAddFont(false) })
                    SizeLabel("Quote", Modifier.padding(top = 16.dp, bottom = 8.dp))
                    FontChoices(style.quoteFont, style.customFontName, onPick = { onChange(style.copy(quoteFont = it)) }, onAdd = { onAddFont(true) })
                    Text(
                        "Serifs set the quote in italics. Your own font is a .ttf or .otf file; it stays on this phone and isn't part of a backup.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textFaint,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                // Only this screen's colours, sizes and fonts: its widgets, gestures and notifications stay.
                TextButton(onClick = { onChange(style.withDefaultLook(screen)) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Reset to default", color = c.textMuted)
                }
                SectionCard(title = "Show") {
                    OptionRow(Icons.Default.Notifications, "Android's status bar", description = "On both screens. Off hides it; a swipe down from the top edge shows it") {
                        ObsidianToggle(!style.hideStatusBar, { onChange(style.copy(hideStatusBar = !it)) })
                    }
                    // The rest are the home screen's own: the lock screen has no app list around them.
                    if (screen == Screen.HOME) {
                        OptionRow(BedtimeIcons.Refuge, "The refuge mark", description = "Bottom left; a tap opens digital refuge") {
                            ObsidianToggle(style.showLogo, { onChange(style.copy(showLogo = it)) })
                        }
                        OptionRow(Icons.Default.KeyboardArrowUp, "The arrow to all apps", description = "At the foot; swiping up works either way") {
                            ObsidianToggle(style.showSwipeArrow, { onChange(style.copy(showSwipeArrow = it)) })
                        }
                        OptionRow(Icons.Default.Edit, "A notes button", description = "Bottom right; a tap opens refuge notes") {
                            ObsidianToggle(style.showNotesButton, { onChange(style.copy(showNotesButton = it)) })
                        }
                    }
                }
            }
            if (page == StylePage.SHOWN) {
                // Each widget's options, opened in place from its row: the same list holding the widget shows.
                val heard = weatherHeardAt?.let { "Last heard ${formatRelativePast(System.currentTimeMillis() - it)}; hidden when over 3 hours old." }
                val optionsOf: (Module) -> List<WidgetOption> = { module ->
                    WidgetOptions.of(module, style, widgetScreen, onChange, notes, habits, onHabitShown(widgetScreen), heard)
                }
                val moduleOptions: @Composable (Module) -> Unit = { module -> WidgetOptionsPage(optionsOf(module), onOpen) }
                SectionCard(
                    title = if (widgetScreen == Screen.HOME) "On the home screen" else "On the lock screen",
                    subtitle = "Tap one for its options; drag by the handle to change the order",
                ) {
                    WidgetList(style, widgetScreen, onChange, onShowCalendar, Module.entries.filter { optionsOf(it).isNotEmpty() }.toSet(), moduleOptions)
                }
            }
            if (page == StylePage.NOTIFICATIONS) {
                SectionCard {
                    OptionRow(
                        Icons.Default.Notifications,
                        "Notifications",
                        description = if (style.lockNotifications && !notificationAccess) {
                            "Notification access is off, so none can show. Switch this on again to allow it"
                        } else {
                            "Drag one down to read it; tap to unlock and open it, or swipe it aside to clear it"
                        },
                    ) { ObsidianToggle(style.lockNotifications && notificationAccess, onLockNotifications) }
                    if (style.lockNotifications && notificationAccess) {
                        val labels = style.lockNotificationApps.map { pkg -> allApps.firstOrNull { it.packageName == pkg }?.label ?: pkg }.sorted()
                        OptionRow(
                            BedtimeIcons.Grid,
                            "From these apps",
                            description = when (labels.size) {
                                0 -> "None yet. Pick the apps you want to hear from"
                                in 1..3 -> labels.joinToString(", ")
                                else -> labels.take(2).joinToString(", ") + " and ${labels.size - 2} more"
                            },
                            onClick = onPickNotificationApps,
                        ) { Chevron() }
                        OptionRow(
                            Icons.Default.Edit,
                            "Quick replies",
                            description = "${style.quickReplies.size} ready to send with a tap. Typing your own works too",
                            onClick = { editingReplies = true },
                        ) { Chevron() }
                    }
                }
                SectionCard(title = "Your week", subtitle = "Time in refuge, how sessions held, the phone and habits, against the week before") {
                    OptionRow(Icons.Default.Notifications, "A note on Sunday evening", description = "Once the week is done; insights shows it too, until Monday ends") {
                        ObsidianToggle(weeklyNote, onWeeklyNote)
                    }
                }
                SectionCard(title = "Rain today", subtitle = "From Breezy Weather's forecast (see the weather widget's options to set it up)") {
                    OptionRow(Icons.Default.Notifications, "A rain alert", description = "In the morning, when rain is likely today: how likely, and when") {
                        ObsidianToggle(style.rainAlert, { onChange(style.copy(rainAlert = it)) })
                    }
                    if (style.rainAlert) {
                        var pickingTime by rememberSaveable { mutableStateOf(false) }
                        if (pickingTime) {
                            TimePickerDialog(
                                "Rain alert at",
                                style.rainAlertMinute,
                                onDismiss = { pickingTime = false },
                                onConfirm = { minute ->
                                    onChange(style.copy(rainAlertMinute = minute))
                                    pickingTime = false
                                },
                            )
                        }
                        // Any time of day; it covers the hours left from then.
                        TimeTile("At", formatMinuteOfDay(context, style.rainAlertMinute), Modifier.fillMaxWidth().padding(top = 12.dp)) { pickingTime = true }
                        SizeLabel("When the chance is at least", Modifier.padding(top = 16.dp, bottom = 8.dp))
                        SegmentedChoice(
                            options = RainAlert.THRESHOLDS.map { "$it%" },
                            selected = RainAlert.THRESHOLDS.indexOf(style.rainAlertFrom).coerceAtLeast(0),
                            onSelect = { onChange(style.copy(rainAlertFrom = RainAlert.THRESHOLDS[it])) },
                        )
                    }
                }
                if (style.lockNotifications && notificationAccess) {
                    SectionCard(title = "Circles", subtitle = "People and group chats whose notifications show from any app, first, in their colour") {
                        circles.forEach { circle ->
                            val parts = listOfNotNull(
                                circle.people.sorted().takeIf { it.isNotEmpty() }?.let { if (it.size <= 2) it.joinToString(", ") else "${it.size} people" },
                                circle.groups.size.takeIf { it > 0 }?.let { if (it == 1) "1 group" else "$it groups" },
                            )
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onEditCircle(circle.id) }.padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.size(14.dp).clip(CircleShape).background(circle.colour?.let { Color(it) } ?: Color(style.accent)))
                                Column(Modifier.weight(1f).padding(start = 14.dp)) {
                                    Text(circle.name, style = MaterialTheme.typography.bodyLarge, color = c.textNormal)
                                    Text(parts.joinToString(" · ").ifEmpty { "Nobody yet" }, style = MaterialTheme.typography.bodySmall, color = c.textFaint)
                                }
                                Chevron()
                            }
                        }
                        if (circles.size < Circles.MAX) {
                            Row(
                                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onAddCircle).padding(vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = c.accent, modifier = Modifier.size(18.dp))
                                Text("Add a circle", style = MaterialTheme.typography.bodyLarge, color = c.accentText, modifier = Modifier.padding(start = 10.dp))
                            }
                        }
                    }
                }
            }
            if (page == StylePage.GESTURES) {
                SectionCard {
                    OptionRow(Icons.Default.KeyboardArrowUp, "Swipe up", description = "All apps and search")
                    HomeGesture.entries.forEach { gesture ->
                        val action = style.homeGestures[gesture]
                        OptionRow(
                            gesture.icon(),
                            gesture.label(),
                            description = HomeActions.label(action) ?: action?.let { pkg -> allApps.firstOrNull { it.packageName == pkg }?.label?.let { "Open $it" } ?: pkg } ?: "Nothing",
                            onClick = { pickingGesture = gesture },
                        ) { Chevron() }
                    }
                }
            }
            if (page == StylePage.SWIPES) {
                SectionCard {
                    Swipe.entries.forEach { swipe ->
                        val target = style.lockGestures[swipe]
                        OptionRow(
                            swipe.icon(),
                            swipe.label(),
                            description = when (target) {
                                null -> if (swipe == Swipe.DOUBLE_TAP) "Nothing" else "Just unlocks"
                                GESTURE_CAMERA -> "Camera"
                                HomeActions.NOTE -> "Write a quick note"
                                HomeActions.NOTIFICATIONS -> "Show notifications"
                                HomeActions.LOCK -> "Turn the screen off"
                                else -> allApps.firstOrNull { it.packageName == target }?.label?.let { "Open $it" } ?: target
                            },
                            onClick = { pickingSwipe = swipe },
                        ) { Chevron() }
                    }
                }
            }
        }
    }
}

/** The pages the home and lock screen's choices are split into, each reached from settings. */
enum class StylePage(val title: String, val intro: String) {
    LOOK("Look", "Colours, sizes and fonts, each screen its own. Changes save as you go."),
    SHOWN("Widgets", "What your home screen and lock screen show, each its own, in the order you like. Pick the screen here first. Changes save as you go."),
    SWIPES(
        "Lock-screen gestures",
        "Swipes start around the clock; a double tap is anywhere. Each can open an app once you've unlocked, or straight away the camera, a quick note or your notifications. The middle is for swiping notifications away; from the bottom, any swipe just unlocks.",
    ),
    GESTURES(
        "Gestures",
        "On your home screen, and the minimal one during sessions. Swipes start anywhere but the very top edge, which stays Android's; a double tap is on an empty spot.",
    ),
    NOTIFICATIONS("Lock-screen notifications", "Those the session lets through, from the apps you choose, as private as Android's own lock screen."),
}

private fun HomeGesture.icon(): ImageVector = when (this) {
    HomeGesture.SWIPE_DOWN -> Icons.Default.KeyboardArrowDown
    HomeGesture.SWIPE_LEFT -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
    HomeGesture.SWIPE_RIGHT -> Icons.AutoMirrored.Filled.KeyboardArrowRight
    HomeGesture.DOUBLE_TAP -> Icons.Default.Refresh
}

private fun HomeGesture.label(): String = when (this) {
    HomeGesture.SWIPE_DOWN -> "Swipe down"
    HomeGesture.SWIPE_LEFT -> "Swipe left"
    HomeGesture.SWIPE_RIGHT -> "Swipe right"
    HomeGesture.DOUBLE_TAP -> "Double tap"
}

/** What a home-screen gesture does: nothing, one of the app's own actions, or opening any app. */
@Composable
private fun HomeGestureDialog(gesture: HomeGesture, apps: List<AppEntry>, onDismiss: () -> Unit, onPick: (String?) -> Unit) {
    GestureDialog(
        title = "${gesture.label()} does",
        actions = listOf<Pair<String?, String>>(null to "Nothing") + HomeActions.all.map { (action, label) -> action to label },
        appsTitle = "Open an app",
        apps = apps,
        onDismiss = onDismiss,
        onPick = onPick,
    )
}

/**
 * What a gesture does: a short list of actions, and "open an app", which turns into a search of your
 * apps rather than a list of all of them.
 */
@Composable
private fun GestureDialog(
    title: String,
    actions: List<Pair<String?, String>>,
    appsTitle: String,
    apps: List<AppEntry>,
    onDismiss: () -> Unit,
    onPick: (String?) -> Unit,
) {
    val c = Obsidian.colors
    var choosingApp by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.bgSecondary,
        title = { Text(if (choosingApp) appsTitle else title) },
        text = {
            if (!choosingApp) {
                Column {
                    actions.forEach { (action, label) -> DialogChoice(label) { onPick(action) } }
                    Row(
                        Modifier.fillMaxWidth().clickable { choosingApp = true }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("$appsTitle…", style = MaterialTheme.typography.bodyLarge, color = c.accentText, modifier = Modifier.weight(1f))
                        Chevron()
                    }
                }
            } else {
                Column {
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.textNormal),
                        cursorBrush = SolidColor(c.accent),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, c.border, RoundedCornerShape(12.dp)).padding(12.dp),
                        decorationBox = { field ->
                            Box {
                                if (query.isEmpty()) Text("Search your apps", style = MaterialTheme.typography.bodyLarge, color = c.textFaint)
                                field()
                            }
                        },
                    )
                    val shown = apps.filter { it.label.contains(query.trim(), ignoreCase = true) }
                    LazyColumn(Modifier.heightIn(max = 380.dp).padding(top = 8.dp)) {
                        items(shown.size) { i -> DialogChoice(shown[i].label) { onPick(shown[i].packageName) } }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = { if (choosingApp) choosingApp = false else onDismiss() }) {
                Text(if (choosingApp) "Back" else "Cancel", color = c.textMuted)
            }
        },
    )
}

private fun Swipe.label(): String = if (this == Swipe.DOUBLE_TAP) "Double tap" else "Swipe ${name.lowercase()}"

private fun Swipe.icon(): ImageVector = when (this) {
    Swipe.DOUBLE_TAP -> Icons.Default.Refresh
    Swipe.UP -> Icons.Default.KeyboardArrowUp
    Swipe.DOWN -> Icons.Default.KeyboardArrowDown
    Swipe.LEFT -> Icons.AutoMirrored.Filled.KeyboardArrowLeft
    Swipe.RIGHT -> Icons.AutoMirrored.Filled.KeyboardArrowRight
}

/** What a lock-screen gesture does: nothing (a swipe just unlocks), an action right here, or an app once unlocked. */
@Composable
private fun SwipeTargetDialog(swipe: Swipe, apps: List<AppEntry>, onDismiss: () -> Unit, onPick: (String?) -> Unit) {
    GestureDialog(
        title = "${swipe.label()} does",
        actions = listOf(
            null to if (swipe == Swipe.DOUBLE_TAP) "Nothing" else "Nothing, it just unlocks",
            GESTURE_CAMERA to "Camera, without unlocking",
            HomeActions.NOTE to "Write a quick note, without unlocking",
            HomeActions.NOTIFICATIONS to "Show notifications",
            HomeActions.LOCK to "Turn the screen off",
        ),
        appsTitle = "Open an app, once unlocked",
        apps = apps,
        onDismiss = onDismiss,
        onPick = onPick,
    )
}

@Composable
private fun DialogChoice(label: String, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.bodyLarge,
        color = Obsidian.colors.textNormal,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
    )
}

/** The fonts as chips, then your own one if you have added it, and a way to add or replace it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FontChoices(selected: AppFont, customName: String?, onPick: (AppFont) -> Unit, onAdd: () -> Unit) {
    val choices = AppFont.entries.filter { it != AppFont.CUSTOM || customName != null }
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        choices.forEach { font -> QuickChip(AppFonts.label(font, customName), selected == font) { onPick(font) } }
        QuickChip(if (customName == null) "+ your own font" else "+ another", false, onClick = onAdd)
    }
}

@Composable
private fun SizeLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.labelMedium, color = Obsidian.colors.textMuted, modifier = modifier)
}

@Composable
internal fun SwatchRow(presets: List<Pair<String, Long>>, selected: Long, onPick: (Long) -> Unit) {
    val c = Obsidian.colors
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        presets.forEach { (name, value) ->
            val isSelected = value == selected
            val colour = Color(value)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colour)
                        .border(if (isSelected) 3.dp else 1.dp, if (isSelected) c.accent else c.border, CircleShape)
                        .clickable(onClickLabel = name) { onPick(value) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "$name selected",
                            tint = if (colour.luminance() > 0.5f) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Text(name, style = MaterialTheme.typography.labelSmall, color = c.textMuted, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

/**
 * A colour from a set, or your own: an optional [automatic] choice (null), the swatches, and "Custom",
 * which opens a field for any colour as #RRGGBB.
 */
@Composable
internal fun ColourChoice(presets: List<Pair<String, Long>>, selected: Long?, onPick: (Long?) -> Unit, automatic: String? = null) {
    var custom by remember { mutableStateOf(selected != null && presets.none { it.second == selected }) }
    Column {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (automatic != null) QuickChip(automatic, selected == null && !custom) { custom = false; onPick(null) }
            QuickChip("Custom", custom) { custom = !custom }
        }
        Spacer(Modifier.height(10.dp))
        SwatchRow(presets, if (custom) -1L else selected ?: -1L) { custom = false; onPick(it) }
        if (custom) HexField(selected ?: presets.first().second) { onPick(it) }
    }
}

@Composable
internal fun HexField(value: Long, onPick: (Long) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toHexColor()) }
    ObsidianTextField(
        value = text,
        onValueChange = {
            text = it
            parseHexColor(it)?.let(onPick)
        },
        label = "Custom colour (#RRGGBB)",
        modifier = Modifier.padding(top = 12.dp),
    )
}

/** "just now", "12 min ago", "2 h ago": how long since the weather last came. */
private fun formatRelativePast(millis: Long): String {
    val minutes = millis / 60_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 48 * 60 -> "${minutes / 60} h ago"
        else -> "${minutes / (24 * 60)} days ago"
    }
}

/**
 * One screen's widgets, in order, each switched on or off. The next event asks for calendar access the
 * first time it's switched on.
 */
@Composable
private fun WidgetList(
    style: HomeStyle,
    screen: Screen,
    onChange: (HomeStyle) -> Unit,
    onShowCalendar: (HomeStyle) -> Unit,
    withOptions: Set<Module>,
    options: @Composable (Module) -> Unit,
) {
    val c = Obsidian.colors
    val list = Modules.of(style, screen)
    val shown = list.filter { it.on }
    val hidden = list.filterNot { it.on }
    var open by rememberSaveable(screen) { mutableStateOf<Module?>(null) }
    fun switch(module: Module, on: Boolean) {
        val updated = Modules.with(style, screen, list.map { if (it.module == module) it.copy(on = on) else it })
        if (on && module == Module.EVENT) onShowCalendar(updated) else onChange(updated)
    }
    // Those shown, in their order: a tap opens a widget's options right here, the − hides it.
    DragList(shown, key = { it.module.name }, onMoved = { moved -> onChange(Modules.with(style, screen, moved + hidden)) }) { choice ->
        val module = choice.module
        val opened = open == module && module in withOptions
        Column(Modifier.weight(1f)) {
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                    .then(if (module in withOptions) Modifier.clickable(onClickLabel = if (opened) "close its options" else "its options") { open = if (opened) null else module } else Modifier)
                    .padding(start = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(module.label, style = MaterialTheme.typography.bodyLarge, color = c.textNormal, modifier = Modifier.weight(1f))
                if (module in withOptions) {
                    Icon(if (opened) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = c.textFaint, modifier = Modifier.size(20.dp))
                }
                Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClickLabel = "hide ${module.label}") { switch(module, false) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Clear, contentDescription = "hide", tint = c.textFaint, modifier = Modifier.size(18.dp))
                }
            }
            if (opened) Column(Modifier.padding(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 12.dp)) { options(module) }
        }
    }
    if (shown.isEmpty()) Text("Nothing shown yet; add one below.", style = MaterialTheme.typography.bodyMedium, color = c.textFaint, modifier = Modifier.padding(vertical = 8.dp))
    // The rest, apart: each a tap away from showing.
    if (hidden.isNotEmpty()) {
        SizeLabel("Not shown", Modifier.padding(top = 16.dp, bottom = 4.dp))
        hidden.forEach { choice ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClickLabel = "show ${choice.module.label}") { switch(choice.module, true) }.padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(choice.module.label, style = MaterialTheme.typography.bodyLarge, color = c.textMuted, modifier = Modifier.weight(1f))
                Icon(Icons.Default.Add, contentDescription = "show", tint = c.accent, modifier = Modifier.size(20.dp))
            }
        }
    }
}

/** A line saying how a widget behaves, at the top of its options. */
@Composable
private fun OptionsHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = Obsidian.colors.textFaint, modifier = Modifier.padding(bottom = 8.dp))
}

/** A list of widgets to drag into order and switch on or off; [onSwitchedOn] when one is switched on, [onChange] otherwise. */
@Composable
internal fun ModuleListEditor(
    list: List<ModuleChoice>,
    enabled: Boolean = true,
    onChange: (List<ModuleChoice>) -> Unit,
    onSwitchedOn: (List<ModuleChoice>, Module) -> Unit = { l, _ -> onChange(l) },
) {
    val c = Obsidian.colors
    DragList(list, key = { it.module.name }, onMoved = { moved -> if (enabled) onChange(moved) }) { choice ->
        Text(
            choice.module.label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (choice.on) c.textNormal else c.textFaint,
            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
        )
        ObsidianToggle(choice.on, { on ->
            val updated = list.map { if (it.module == choice.module) it.copy(on = on) else it }
            if (on) onSwitchedOn(updated, choice.module) else onChange(updated)
        }, enabled = enabled)
    }
}

/** The distraction tracker's look on one screen: dots, a line, a budget bar or the top apps; its figures; the budget. */
@Composable
private fun UseWidgetOptions(use: UseWidget, onChange: (UseWidget) -> Unit) {
    SegmentedChoice(
        options = listOf("Dots", "Line", "Budget", "Apps", "Apps line"),
        selected = use.look.ordinal,
        onSelect = { i ->
            val look = UseLook.entries[i]
            // A line with nothing chosen would only say the distracting time; start it with both.
            onChange(if (look == UseLook.LINE && !use.total && !use.distracting) use.copy(look = look, total = true, distracting = true) else use.copy(look = look))
        },
    )
    Spacer(Modifier.height(8.dp))
    if (use.look == UseLook.DOTS || use.look == UseLook.LINE) {
        val under = if (use.look == UseLook.DOTS) " in a small line under the dots" else ""
        OptionRow(Icons.Default.Phone, "Time on the phone", description = "All of today, in any app$under") {
            ObsidianToggle(use.total, { onChange(use.copy(total = it)) })
        }
        OptionRow(Icons.Default.Warning, "Time in distracting apps", description = "Today so far$under") {
            ObsidianToggle(use.distracting, { onChange(use.copy(distracting = it)) })
        }
    }
    OptionRow(Icons.Default.Lock, "Unlocks", description = "How many times today; refuge counts them itself, so nothing more is asked") {
        ObsidianToggle(use.unlocks, { onChange(use.copy(unlocks = it)) })
    }
    if (use.look == UseLook.BUDGET) {
        SizeLabel("The budget counts", Modifier.padding(top = 12.dp, bottom = 8.dp))
        SegmentedChoice(
            options = listOf("Distracting apps", "The whole phone"),
            selected = if (use.budgetWholePhone) 1 else 0,
            onSelect = { onChange(use.copy(budgetWholePhone = it == 1)) },
        )
        SizeLabel("A day's allowance", Modifier.padding(top = 16.dp, bottom = 8.dp))
        val minutes = if (use.budgetWholePhone) UseWords.PHONE_BUDGETS else UseWords.DISTRACTING_BUDGETS
        val chosen = if (use.budgetWholePhone) use.phoneBudgetMinutes else use.budgetMinutes
        SegmentedChoice(
            options = minutes.map(UseWords::compact),
            selected = minutes.indexOf(chosen).coerceAtLeast(0),
            onSelect = { i -> onChange(if (use.budgetWholePhone) use.copy(phoneBudgetMinutes = minutes[i]) else use.copy(budgetMinutes = minutes[i])) },
        )
    }
}
