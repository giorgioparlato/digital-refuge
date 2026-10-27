package app.bedtime.ui.homestyle

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
import app.bedtime.data.TempUnit
import app.bedtime.data.WeatherView
import app.bedtime.data.WeatherStyle
import app.bedtime.data.Weather
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
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
import app.bedtime.data.ClockSize
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
import app.bedtime.data.AppNameSize
import app.bedtime.data.AppSettings
import app.bedtime.data.HomeStyle
import app.bedtime.data.Quote
import app.bedtime.data.Quotes
import app.bedtime.data.Repository
import app.bedtime.data.QuoteRefresh
import app.bedtime.data.TextSize
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
import java.time.LocalDate
import java.time.LocalDateTime

@Composable
fun HomeStyleScreen(onBack: () -> Unit, onQuotes: () -> Unit = {}, page: StylePage = StylePage.LOOK) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings: AppSettings? by repo.settings.collectAsStateWithLifecycle(initialValue = null)
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
    var pickingPeople by rememberSaveable { mutableStateOf(false) }
    // Choosing people reads your contacts: asked for the first time here.
    val askContacts = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pickingPeople = true else Toast.makeText(context, "contacts access wasn't allowed", Toast.LENGTH_SHORT).show()
    }
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
    if (pickingPeople) {
        PeoplePickerScreen(
            "Notifications from",
            current.homeStyle.lockNotificationPeople,
            onDone = { people ->
                scope.launch { repo.updateSettings { it.copy(homeStyle = it.homeStyle.copy(lockNotificationPeople = people)) } }
                pickingPeople = false
            },
            onBack = { pickingPeople = false },
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
        onPickPeople = { if (ContactSearch.canRead(context)) pickingPeople = true else askContacts.launch(Manifest.permission.READ_CONTACTS) },
        weatherHeardAt = weather?.at,
        page = page,
        // The preview shows the real weather if there is some, otherwise an example.
        weatherPreview = current.homeStyle.takeIf { Modules.onAnywhere(it, Module.WEATHER) }?.let { style ->
            Weather.view(weather ?: Weather.SAMPLE, (weather?.at ?: Weather.SAMPLE.at) + 1, Weather.fahrenheit(style.weatherUnit), style.weatherStyle)
        },
        now = LocalDateTime.now(),
        onBack = onBack,
        quote = Quotes.forPeriod(LocalDateTime.now(), current.homeStyle.quoteRefresh, prefs = current.quotes),
        onQuotes = onQuotes,
        onAddFont = { forQuote ->
            addingForQuote = forQuote
            pickFont.launch(arrayOf("font/ttf", "font/otf", "font/sfnt", "application/x-font-ttf", "application/x-font-otf", "application/octet-stream"))
        },
    )
}

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
    /** Opens the quotes page: your own, favourites, hidden ones. */
    onQuotes: () -> Unit = {},
    /** Switches lock-screen notifications, sending you to grant notification access first if needed. */
    onLockNotifications: (Boolean) -> Unit = {},
    notificationAccess: Boolean = false,
    /** Opens the choice of apps whose notifications the lock screen shows. */
    onPickNotificationApps: () -> Unit = {},
    /** And of people, from any app. */
    onPickPeople: () -> Unit = {},
    /** When Breezy Weather last sent the weather, if ever. */
    weatherHeardAt: Long? = null,
    /** The weather as the preview shows it. */
    weatherPreview: WeatherView? = null,
    /** Which of the home and lock screen's pages this is. */
    page: StylePage = StylePage.LOOK,
) {
    val context = LocalContext.current
    val c = Obsidian.colors
    var pickingSwipe by rememberSaveable { mutableStateOf<Swipe?>(null) }
    var widgetScreen by rememberSaveable { mutableStateOf(Screen.HOME) }
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
            // Which screen's widgets: first, so the lock screen's are easy to find; the preview follows it.
            if (page == StylePage.SHOWN) {
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
                // The lock screen while its widgets are being set; otherwise the minimal home.
                if (page == StylePage.SHOWN && widgetScreen == Screen.LOCK) {
                    LockScreenContent(
                        now = now,
                        scheduleName = "Bedtime",
                        until = formatMinuteOfDay(context, 7 * 60),
                        style = style,
                        quote = quote,
                        weather = weatherPreview,
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
                        weather = weatherPreview,
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
                    selected = style.topSpace.ordinal,
                    onSelect = { onChange(style.copy(topSpace = TopSpace.entries[it])) },
                )
                SizeLabel("Clock", Modifier.padding(top = 16.dp, bottom = 8.dp))
                    SegmentedChoice(
                        options = listOf("Very small", "Small", "Medium", "Large"),
                        selected = style.clockSize.ordinal,
                        onSelect = { onChange(style.copy(clockSize = ClockSize.entries[it])) },
                    )
                    SizeLabel("Text", Modifier.padding(top = 16.dp, bottom = 8.dp))
                    SegmentedChoice(
                        options = listOf("Small", "Medium", "Large"),
                        selected = style.textSize.ordinal,
                        onSelect = { onChange(style.copy(textSize = TextSize.entries[it])) },
                    )
                    SizeLabel("App names", Modifier.padding(top = 16.dp, bottom = 8.dp))
                    SegmentedChoice(
                        options = listOf("Very small", "Small", "Medium", "Large"),
                        selected = style.appNameSize.ordinal,
                        onSelect = { onChange(style.copy(appNameSize = AppNameSize.entries[it])) },
                    )
                    SizeLabel("Full app list and search", Modifier.padding(top = 16.dp, bottom = 8.dp))
                    SegmentedChoice(
                        options = listOf("Very small", "Small", "Medium", "Large"),
                        selected = style.listSize.ordinal,
                        onSelect = { onChange(style.copy(listSize = AppNameSize.entries[it])) },
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
                // Your own font's file stays, so resetting keeps it on offer.
                TextButton(onClick = { onChange(HomeStyle(customFontName = style.customFontName)) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Reset to default", color = c.textMuted)
                }
            }
            if (page == StylePage.SHOWN) {
                SectionCard(
                    title = if (widgetScreen == Screen.HOME) "On the home screen" else "On the lock screen",
                    subtitle = "Drag by the handle to change the order",
                ) {
                    WidgetList(style, widgetScreen, onChange, onShowCalendar)
                }
                SectionCard(title = "Show") {
                    OptionRow(Icons.Default.Notifications, "Android's status bar", description = "Off hides it here and on the lock screen; a swipe down from the top edge shows it") {
                        ObsidianToggle(!style.hideStatusBar, { onChange(style.copy(hideStatusBar = !it)) })
                    }
                    OptionRow(BedtimeIcons.Grid, "App icons", description = "Small icons next to app names") {
                        ObsidianToggle(style.showIcons, { onChange(style.copy(showIcons = it)) })
                    }
                    OptionRow(BedtimeIcons.Refuge, "The refuge mark", description = "Bottom left; a tap opens digital refuge") {
                        ObsidianToggle(style.showLogo, { onChange(style.copy(showLogo = it)) })
                    }
                    OptionRow(Icons.Default.KeyboardArrowUp, "The arrow to all apps", description = "At the foot; swiping up works either way") {
                        ObsidianToggle(style.showSwipeArrow, { onChange(style.copy(showSwipeArrow = it)) })
                    }
                }
                if (Modules.onAnywhere(style, Module.QUOTE)) SectionCard(title = "Quote", subtitle = "Tap it for another, hold it to favourite or hide it") {
                    run {
                        SizeLabel("A new one every", Modifier.padding(bottom = 8.dp))
                        SegmentedChoice(
                            options = listOf("Hour", "6 hours", "Day", "Week"),
                            selected = style.quoteRefresh.ordinal,
                            onSelect = { onChange(style.copy(quoteRefresh = QuoteRefresh.entries[it])) },
                        )
                        OptionRow(
                            Icons.Default.FavoriteBorder,
                            "Quotes",
                            description = "Add your own, pick favourites, hide some",
                            onClick = onQuotes,
                        ) { Chevron() }
                    }
                }
                if (Modules.onAnywhere(style, Module.WEATHER)) SectionCard(title = "Weather", subtitle = "From Breezy Weather, a free app that does the fetching; this one never goes online") {
                    OptionRow(
                        Icons.Default.Info,
                        "Last heard",
                        description = when (weatherHeardAt) {
                            null -> "Nothing from Breezy Weather yet"
                            else -> "${formatRelativePast(System.currentTimeMillis() - weatherHeardAt).replaceFirstChar { it.uppercase() }}. Hidden when over 3 hours old"
                        },
                    )
                    run {
                        SizeLabel("Show", Modifier.padding(top = 12.dp, bottom = 8.dp))
                        val styles = listOf(WeatherStyle.TODAY, WeatherStyle.DAYS, WeatherStyle.WEEK, WeatherStyle.FIVE_DAYS)
                        SegmentedChoice(
                            options = listOf("Today", "3 days", "Now + 5", "5 days"),
                            selected = styles.indexOf(style.weatherStyle).coerceAtLeast(0),
                            onSelect = { onChange(style.copy(weatherStyle = styles[it])) },
                        )
                        SizeLabel("In", Modifier.padding(top = 12.dp, bottom = 8.dp))
                        SegmentedChoice(
                            options = listOf("Automatic", "°C", "°F"),
                            selected = style.weatherUnit.ordinal,
                            onSelect = { onChange(style.copy(weatherUnit = TempUnit.entries[it])) },
                        )
                        Text(
                            "Install Breezy Weather (it's on F-Droid). In its settings, under widgets, open “Send Gadgetbridge data” and tick digital refuge. It sends the weather each time it updates.",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textFaint,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
                if (Modules.isOn(style, Screen.LOCK, Module.EVENT)) SectionCard(title = "Next event", subtitle = "Read from the calendar already on this phone. Nothing is fetched") {
                    OptionRow(
                        Icons.Default.Lock,
                        "Event names on the lock screen",
                        description = "Off, the lock screen only says when the next one is",
                    ) { ObsidianToggle(style.calendarNamesOnLock, { onChange(style.copy(calendarNamesOnLock = it)) }) }
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
                        val people = style.lockNotificationPeople.sorted()
                        OptionRow(
                            Icons.Default.Person,
                            "And from these people",
                            description = when (people.size) {
                                0 -> "From any app: messages, calls, mail from them"
                                in 1..3 -> people.joinToString(", ")
                                else -> people.take(2).joinToString(", ") + " and ${people.size - 2} more"
                            },
                            onClick = onPickPeople,
                        ) { Chevron() }
                        OptionRow(
                            Icons.Default.Edit,
                            "Quick replies",
                            description = "${style.quickReplies.size} ready to send with a tap. Typing your own works too",
                            onClick = { editingReplies = true },
                        ) { Chevron() }
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
    LOOK("Look", "Colours, sizes and fonts, for your home screen, the minimal one in sessions, and the lock screen. Changes save as you go."),
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
private fun WidgetList(style: HomeStyle, screen: Screen, onChange: (HomeStyle) -> Unit, onShowCalendar: (HomeStyle) -> Unit) {
    ModuleListEditor(Modules.of(style, screen), onChange = { list -> onChange(Modules.with(style, screen, list)) }) { list, module ->
        val updated = Modules.with(style, screen, list)
        if (module == Module.EVENT) onShowCalendar(updated) else onChange(updated)
    }
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
