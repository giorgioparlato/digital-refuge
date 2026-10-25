package app.bedtime

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import app.bedtime.apps.AppEntry
import app.bedtime.data.AppGroup
import app.bedtime.data.DndMode
import app.bedtime.ui.apps.PickerMode
import app.bedtime.ui.groups.GroupsContent
import app.bedtime.ui.minimal.EmergencyContent
import app.bedtime.data.HomeStyle
import app.bedtime.data.Quote
import app.bedtime.data.Run
import app.bedtime.data.RuntimeState
import app.bedtime.data.Schedule
import app.bedtime.data.ScheduleKind
import app.bedtime.data.SessionLog
import app.bedtime.data.TextSize
import app.bedtime.data.UnlockAction
import app.bedtime.data.UnlockConfig
import app.bedtime.data.UnlockMode
import app.bedtime.engine.ScheduleEvaluator
import androidx.compose.foundation.layout.fillMaxWidth
import app.bedtime.ui.apps.AppPickerContent
import app.bedtime.ui.apps.PickerLock
import app.bedtime.ui.blocked.BlockedContent
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.create.CreateContent
import app.bedtime.ui.edit.ScheduleEditContent
import app.bedtime.ui.home.HomeContent
import app.bedtime.ui.home.HomeUiState
import app.bedtime.ui.homestyle.HomeStyleContent
import app.bedtime.ui.minimal.MinimalHomeContent
import app.bedtime.ui.settings.SettingsContent
import app.bedtime.ui.setup.SetupContent
import app.bedtime.ui.theme.BedtimeTheme
import app.bedtime.ui.theme.Obsidian
import app.bedtime.unlock.TextChallenge
import app.bedtime.unlock.TextChallengeContent
import app.bedtime.unlock.UnlockLayout
import app.bedtime.unlock.UnlockSuccess
import app.bedtime.unlock.WaitChallengeContent
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Random

/**
 * Renders every screen to PNG, dark and light, without an emulator.
 * Record with `./gradlew recordPaparazziDebug`; images land in app/src/test/snapshots/images/.
 */
class ScreenshotTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.NoActionBar",
        showSystemUi = false,
    )

    private fun shot(tall: Boolean = false, height: Int = 5200, content: @Composable () -> Unit) {
        if (tall) paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = height))
        // The app is dark only (v0.8.3).
        paparazzi.snapshot("dark") { BedtimeTheme { content() } }
    }

    /** Background + padding for pieces that don't draw their own screen. */
    @Composable
    private fun Screen(content: @Composable () -> Unit) {
        Box(Modifier.fillMaxSize().background(Obsidian.colors.bgPrimary).padding(20.dp)) { content() }
    }

    /** For pages that reach the screen edges themselves, like the unlock flow's fade. */
    @Composable
    private fun FullScreen(content: @Composable () -> Unit) {
        Box(Modifier.fillMaxSize().background(Obsidian.colors.bgPrimary)) { content() }
    }

    @Composable
    private fun Home(ui: HomeUiState) {
        HomeContent(
            ui = ui,
            onEdit = {},
            onCreate = {},
            onTemplate = {},
            onStartBlock = { _, _ -> },
            onSetup = {},
            onUnlock = {},
        )
    }

    @Composable
    private fun Edit(draft: Schedule, readOnly: Boolean = false, dndAvailable: Boolean = true, isNew: Boolean = false) {
        ScheduleEditContent(
            draft = draft,
            onDraftChange = {},
            isNew = isNew,
            readOnly = readOnly,
            newPassword = "",
            onNewPasswordChange = {},
            greyscaleAvailable = true,
            dndAvailable = dndAvailable,
            error = null,
            saving = false,
            onBack = {},
            onSave = {},
            onDelete = {},
            canDuplicate = true,
            onUnlock = {},
            onPickBlocked = {},
            onPickAllowed = {},
            onEditStart = {},
            onEditEnd = {},
            onOpenSetup = {},
            appLabel = Samples::label,
        )
    }

    @Test
    fun homeEmpty() = shot(tall = true) { Home(Samples.home(emptyList(), serviceOn = false, greyscaleOk = false)) }

    @Test
    fun homeIdle() = shot(tall = true) { Home(Samples.home()) }

    @Test
    fun homeActive() = shot { Home(Samples.home(hour = 15, runningFocus = true)) }

    @Test
    fun homeUndo() = shot { Home(Samples.home(hour = 15, runningFocus = true).copy(undoBlockId = "focus")) }

    @Test
    fun homeWhatsNew() = shot(tall = true) { Home(Samples.home().copy(whatsNew = app.bedtime.data.Changelog.entries.last())) }

    @Test
    fun schedules() = shot(tall = true) {
        val ui = Samples.home()
        app.bedtime.ui.schedules.SchedulesContent(
            schedules = ui.schedules.filterNot { it.isBlock },
            active = ui.active,
            now = ui.now,
            onEdit = {},
            onCreate = {},
            onToggle = { _, _ -> },
        )
    }

    @Test
    fun limits() = shot(tall = true) {
        val ui = Samples.home()
        val today = LocalDate.now().toEpochDay()
        app.bedtime.ui.limits.LimitsContent(
            limits = Samples.limits,
            usage = listOf(
                app.bedtime.data.UsageDay(
                    today,
                    seconds = mapOf("com.instagram.android" to 21 * 60L, "com.zhiliaoapp.musically" to 9 * 60L, "com.google.android.youtube" to 38 * 60L),
                    extras = mapOf("youtube" to listOf(1L)),
                ),
            ),
            today = today,
            onEdit = {},
            onCreate = {},
        )
    }

    @Test
    fun limitsEmpty() = shot {
        app.bedtime.ui.limits.LimitsContent(emptyList(), emptyList(), LocalDate.now().toEpochDay(), {}, {})
    }

    @Test
    fun limitEdit() = shot(tall = true) {
        app.bedtime.ui.limits.LimitEditContent(
            draft = Samples.limits.first().let { it.copy(rules = it.rules.copy(pauseForGroup = true, outOfTime = app.bedtime.data.OutOfTime.CHOOSE)) },
            onDraftChange = {},
            isNew = false,
            loosens = true,
            newPassword = "",
            onNewPasswordChange = {},
            error = null,
            onBack = {},
            onSave = {},
            onDelete = {},
            onPickApps = {},
            appLabel = Samples::label,
        )
    }

    @Test
    fun stats() = shot(tall = true) {
        val ui = Samples.home()
        val today = LocalDate.now().toEpochDay()
        app.bedtime.ui.stats.StatsContent(
            history = Samples.fortnight(ui.now),
            limits = Samples.limits,
            usage = listOf(app.bedtime.data.UsageDay(today, seconds = mapOf("com.instagram.android" to 41 * 60L), extras = mapOf("social" to listOf(1L)))),
            now = ui.now,
            zone = ZoneId.systemDefault(),
            onBack = {},
        )
    }

    @Test
    fun homeStats() = shot(tall = true) { Home(Samples.home().let { it.copy(history = Samples.fortnight(it.now)) }) }

    @Test
    fun unlockChoose() = shot {
        FullScreen { app.bedtime.unlock.UnlockLengthChoice("Bedtime", maxMinutes = 95, restLabel = "the rest of the session", onChoose = {}, onCancel = {}) }
    }

    @Test
    fun limitEditShut() = shot(tall = true) {
        app.bedtime.ui.limits.LimitEditContent(
            draft = Samples.limits.first().copy(editWindow = app.bedtime.data.EditWindow(9 * 60, 10 * 60)),
            onDraftChange = {},
            isNew = false,
            loosens = false,
            newPassword = "",
            onNewPasswordChange = {},
            error = null,
            onBack = {},
            onSave = {},
            onDelete = {},
            onPickApps = {},
            appLabel = Samples::label,
            shutWindow = app.bedtime.data.EditWindow(9 * 60, 10 * 60),
        )
    }

    @Test
    fun blockedWithMessage() = shot {
        BlockedContent("Instagram", "Bedtime", "07:00", onHome = {}, onUnlock = {}, message = "You said you'd read tonight.")
    }

    @Test
    fun pauseBeforeOpening() = shot { app.bedtime.ui.blocked.PauseContent("Instagram", secondsLeft = 7, opens = 4, onOpen = {}, onHome = {}) }

    @Test
    fun pauseDone() = shot { app.bedtime.ui.blocked.PauseContent("Instagram", secondsLeft = 0, opens = 1, onOpen = {}, onHome = {}) }

    @Test
    fun limitOut() = shot {
        app.bedtime.ui.blocked.LimitContent("YouTube", "youtube", 30, app.bedtime.data.OutOfTime.MORE_TIME, 15, onHome = {}, onMore = {})
    }

    @Test
    fun homeManyBlocks() = shot(tall = true) { Home(Samples.home(Samples.manyBlocks, hour = 15, runningFocus = true)) }

    @Test
    fun create() = shot(tall = true) { CreateContent(onBack = {}, onPick = {}) }

    @Test
    fun editSchedule() = shot { Edit(Samples.bedtime) }

    /** A new schedule opens on its first section, which is where the folds get their coverage. */
    @Test
    fun editScheduleOpen() = shot(tall = true) { Edit(Samples.bedtime, isNew = true) }

    @Test
    fun editBlock() = shot { Edit(Samples.focus, dndAvailable = false) }

    @Test
    fun editReadOnly() = shot { Edit(Samples.bedtime, readOnly = true) }

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    @Test
    fun timeDial() = shot {
        Screen {
            app.bedtime.ui.edit.TimeDialContent("Starts at", androidx.compose.material3.rememberTimePickerState(22, 30, is24Hour = false), {}, {})
        }
    }

    @OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
    @Test
    fun timeDial24() = shot {
        Screen {
            app.bedtime.ui.edit.TimeDialContent("Ends at", androidx.compose.material3.rememberTimePickerState(7, 0, is24Hour = true), {}, {})
        }
    }

    @Test
    fun appPicker() = shot {
        AppPickerContent(
            "Apps to block", Samples.apps, Samples.bedtime.blockedApps, "", {}, {}, {}, {},
            groups = Samples.groups,
            onSaveGroup = {},
        )
    }

    @Test
    fun appPickerAllow() = shot {
        AppPickerContent(
            "Apps to allow", Samples.apps, setOf(Samples.PHONE, Samples.MAPS, Samples.MESSAGES), "", {}, {}, {}, {},
            mode = PickerMode.ALLOW,
            essentials = setOf(Samples.SETTINGS, Samples.PHONE, Samples.MESSAGES),
            alwaysAllowed = setOf(Samples.PHONE),
            groups = Samples.groups,
            onSaveGroup = {},
        )
    }

    @Test
    fun appPickerLocked() = shot {
        AppPickerContent(
            "Always available", Samples.apps, setOf(Samples.MAPS, Samples.SPOTIFY), "", {}, {}, {}, {},
            mode = PickerMode.ALWAYS,
            lock = PickerLock(
                title = "A session is running",
                body = "Apps here are never blocked, so adding one now would be a way out of the session you are in " +
                    "\u2014 and of every session after it. The list opens again at 07:00.",
                button = "Locked until 07:00",
            ),
            groups = Samples.groups,
            onSaveGroup = {},
        )
    }

    @Test
    fun emergency() = shot(tall = true) { EmergencyContent(
            onBack = {},
            onCall = {},
            alwaysAvailable = Samples.apps.filter { it.packageName in setOf(Samples.MAPS, Samples.SPOTIFY) },
            onOpenApp = {},
            preview = true,
        )
    }

    @Test
    fun appPickerAlways() = shot {
        AppPickerContent(
            "Always available", Samples.apps, setOf(Samples.MAPS, Samples.SETTINGS), "", {}, {}, {}, {},
            mode = PickerMode.ALWAYS,
            riskyPackages = setOf(Samples.SETTINGS),
        ) }

    @Test
    fun groups() = shot { GroupsContent(groups = Samples.groups, onBack = {}, onOpen = {}, onNew = {}, appLabel = Samples::label) }

    @Test
    fun setup() = shot(tall = true) {
        SetupContent(
            serviceOn = true,
            greyscaleOk = false,
            dndOk = false,
            previewing = false,
            canPreview = true,
            onBack = {},
            onOpenAccessibility = {},
            onOpenAppInfo = {},
            onPreview = {},
            onOpenDnd = {},
            onOpenBattery = {},
            alwaysAvailableCount = 2,
            onAlwaysAvailable = {},
        )
    }

    @Test
    fun settings() = shot {
        SettingsContent(
            blocks = listOf(Samples.focus, Samples.deepWork),
            tileBlockId = null,
            setupStepsLeft = 1,
            onBack = {},
            onSetup = {},
            onHomeStyle = {},
            onTileBlock = {},
            groupCount = 2,
            onGroups = {},
            alwaysAvailableCount = 2,
            onAlwaysAvailable = {},
        )
    }

    @Test
    fun homeStyle() = shot(tall = true, height = 9000) {
        HomeStyleContent(
            style = HomeStyle(showCalendar = true, lockGestures = mapOf(app.bedtime.data.Swipe.UP to app.bedtime.data.GESTURE_CAMERA)),
            onChange = {},
            previewApps = Samples.minimalApps,
            now = Samples.lateEvening,
            onBack = {},
            quote = Samples.shortQuote,
        )
    }

    @Test
    fun quotes() = shot {
        val mine = listOf(
            app.bedtime.data.Quote("Rest is not idleness.", "John Lubbock", "The Use of Life"),
            app.bedtime.data.Quote("Phone down, eyes up."),
        )
        val all = app.bedtime.data.Quotes.all
        val prefs = app.bedtime.data.QuotePrefs(
            mine = mine,
            favourites = setOf(all[1].text, all[4].text, mine[1].text),
            hidden = setOf(all[2].text, mine[0].text),
        )
        app.bedtime.ui.quotes.QuotesContent(prefs = prefs, recent = all.take(4), onChange = {}, onBack = {})
    }

    @Test
    fun homeStyleNotifications() = shot(tall = true, height = 9000) {
        HomeStyleContent(
            style = HomeStyle(lockNotifications = true, lockNotificationApps = setOf("com.android.dialer", "org.thoughtcrime.securesms")),
            onChange = {},
            previewApps = Samples.minimalApps,
            now = Samples.lateEvening,
            onBack = {},
            notificationAccess = true,
        )
    }

    @Test
    fun blocked() = shot { BlockedContent("Instagram", "Bedtime", "07:00", {}, {}) }

    @Test
    fun onboarding() = shot(tall = true) { app.bedtime.ui.onboarding.OnboardingScreen(onDone = {}) }

    @Test
    fun walkthrough() = shot { app.bedtime.ui.onboarding.WalkthroughScreen(onDone = {}) }

    @Test
    fun settingsGuard() = shot { app.bedtime.ui.blocked.SettingsGuardContent("Bedtime", "07:00", 1, {}, {}, {}) }

    @Test
    fun minimalHome() = shot {
        MinimalHomeContent(
            now = Samples.lateEvening,
            scheduleName = "Bedtime",
            until = "07:00",
            apps = Samples.minimalApps,
            style = HomeStyle(),
            onLaunch = {},
            onUnlock = {},
            quote = Samples.shortQuote,
        )
    }

    @Test
    fun minimalHomeSerifQuote() = shot {
        MinimalHomeContent(
            now = Samples.lateEvening,
            scheduleName = "Bedtime",
            until = "07:00",
            apps = Samples.minimalApps,
            style = HomeStyle(quoteFont = app.bedtime.data.AppFont.LITERATA),
            onLaunch = {},
            onUnlock = {},
            quote = Samples.shortQuote,
        )
    }

    @Test
    fun minimalHomeSmallClock() = shot {
        MinimalHomeContent(
            now = Samples.lateEvening,
            scheduleName = "Bedtime",
            until = "07:00",
            apps = Samples.minimalApps,
            style = HomeStyle(clockSize = app.bedtime.data.ClockSize.VERY_SMALL, textColor = 0xFFFFFFFF),
            onLaunch = {},
            onUnlock = {},
            quote = Samples.shortQuote,
            calendar = "next · dentist at 9:30 am",
        )
    }

    @Test
    fun minimalHomeFraunces() = shot {
        MinimalHomeContent(
            now = Samples.lateEvening,
            scheduleName = "Bedtime",
            until = "07:00",
            apps = Samples.minimalApps,
            style = HomeStyle(font = app.bedtime.data.AppFont.FRAUNCES, quoteFont = app.bedtime.data.AppFont.FRAUNCES),
            onLaunch = {},
            onUnlock = {},
            quote = Samples.shortQuote,
        )
    }

    @Test
    fun minimalHomeCustom() = shot {
        MinimalHomeContent(
            now = Samples.lateEvening,
            scheduleName = "Bedtime",
            until = "07:00",
            apps = Samples.minimalApps,
            style = HomeStyle(background = 0xFFF8F5F1, accent = 0xFFE0B243, textSize = TextSize.LARGE, showIcons = true),
            onLaunch = {},
            onUnlock = {},
            quote = Samples.mediumQuote,
        )
    }

    @Test
    fun minimalHomeVerySmall() = shot {
        MinimalHomeContent(
            now = Samples.lateEvening,
            scheduleName = "Bedtime",
            until = "07:00",
            apps = Samples.apps,
            style = HomeStyle(appNameSize = app.bedtime.data.AppNameSize.VERY_SMALL, showIcons = true),
            onLaunch = {},
            onUnlock = {},
            quote = Samples.mediumQuote,
        )
    }

    @Test
    fun lockScreen() = shot {
        app.bedtime.ui.lock.LockScreenContent(now = Samples.lateEvening, scheduleName = "Bedtime", until = "07:00", style = HomeStyle(), quote = Samples.mediumQuote)
    }

    private fun sampleNotes(): List<app.bedtime.data.LockNotification> {
        val at = Samples.lateEvening.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        val full = app.bedtime.data.NoteDetail.FULL
        fun note(i: Int, label: String, title: String?, text: String?, detail: app.bedtime.data.NoteDetail = full) =
            app.bedtime.data.LockNotification("k$i", "com.example.$i", label, title, text, at - i * 7 * 60_000L, detail)
        return listOf(
            note(0, "Signal", "Anna", "call me tomorrow?").copy(
                who = "Anna",
                count = 3,
                messages = listOf(
                    app.bedtime.data.NoteLine("Anna", "are you up?"),
                    app.bedtime.data.NoteLine("Anna", "no worries if not"),
                    app.bedtime.data.NoteLine("Anna", "call me tomorrow?"),
                ),
                actions = listOf(
                    app.bedtime.data.NoteAction("Reply", 0, isReply = true, needsUnlock = true),
                    app.bedtime.data.NoteAction("Mark as read", 1, isReply = false, needsUnlock = false),
                ),
            ),
            note(1, "Phone", "Missed call", "Mum").copy(missedCall = true),
            note(2, "Messages", null, null, app.bedtime.data.NoteDetail.APP_ONLY),
            note(3, "Mail", "Your order has shipped", "It's on its way").copy(body = "It's on its way, and should reach you on Tuesday."),
            note(4, "Calendar", "Dentist", "Tomorrow 09:00"),
            note(5, "Mail", null, null, app.bedtime.data.NoteDetail.APP_ONLY),
        )
    }

    @Test
    fun lockScreenNotifications() = shot {
        app.bedtime.ui.lock.LockScreenContent(
            now = Samples.lateEvening, scheduleName = "Bedtime", until = "07:00", style = HomeStyle(), quote = Samples.shortQuote,
            notifications = sampleNotes(),
        )
    }

    @Test
    fun lockScreenWeather() = shot {
        app.bedtime.ui.lock.LockScreenContent(
            now = Samples.lateEvening, scheduleName = "Bedtime", until = "07:00", style = HomeStyle(), quote = Samples.shortQuote,
            weather = "12° · light rain", calendar = "next · dentist at 9:00 am",
        )
    }

    @Test
    fun lockScreenNotificationsOpen() = shot {
        app.bedtime.ui.lock.LockScreenContent(
            now = Samples.lateEvening, scheduleName = "Bedtime", until = "07:00", style = HomeStyle(), quote = Samples.shortQuote,
            notifications = sampleNotes(), listOpen = true, initialExpanded = "k0",
        )
    }

    @Test
    fun lockScreenReply() = shot {
        app.bedtime.ui.lock.LockScreenContent(
            now = Samples.lateEvening, scheduleName = "Bedtime", until = "07:00", style = HomeStyle(), quote = Samples.shortQuote,
            notifications = sampleNotes(), listOpen = true, initialExpanded = "k0", initialReplying = "k0",
            quickReplies = app.bedtime.data.DEFAULT_QUICK_REPLIES,
        )
    }

    @Test
    fun calendarWeek() = shot {
        Screen {
            val zone = java.time.ZoneId.systemDefault()
            val today = java.time.LocalDate.of(2026, 10, 25)
            fun at(days: Long, h: Int, m: Int = 0) = today.plusDays(days).atTime(h, m).atZone(zone).toInstant().toEpochMilli()
            val utc = { days: Long -> today.plusDays(days).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli() }
            val ev = { t: String, b: Long, e: Long, all: Boolean -> app.bedtime.ui.minimal.CalendarEvent(t, b, e, all) }
            val events = listOf(
                ev("Dinner with Sam", at(0, 19), at(0, 21), false),
                ev("Dentist", at(1, 9), at(1, 10), false),
                ev("Yoga", at(1, 7, 30), at(1, 8, 30), false),
                ev("Stockholm trip", utc(3), utc(5), true),
                ev("Team call", at(3, 14), at(3, 15), false),
            )
            app.bedtime.ui.minimal.CalendarWeekContent(app.bedtime.ui.minimal.calendarWeek(events, today, zone), HomeStyle())
        }
    }

    private val drawerApps = listOf("Signal", "Maps", "Calendar", "Camera", "Clock", "Phone", "Messages", "Bank", "Authenticator", "Notes", "Weather", "Wikipedia")
        .map { app.bedtime.apps.AppEntry("pkg.$it", it) }

    @Test
    fun drawer() = shot {
        app.bedtime.ui.minimal.AppDrawerContent(
            apps = drawerApps, style = HomeStyle(), query = "", onQuery = {}, results = emptyList(), onLaunch = {}, onResult = {}, preview = true,
        )
    }

    @Test
    fun drawerSearch() = shot {
        val query = "dentist friday 9"
        val now = java.time.LocalDateTime.of(2026, 10, 25, 21, 0)
        val targets = listOf(
            app.bedtime.data.SearchTarget(app.bedtime.data.SearchTarget.Kind.WEB, "pkg.browser", "Browser"),
            app.bedtime.data.SearchTarget(app.bedtime.data.SearchTarget.Kind.WIKIPEDIA, "pkg.wiki", "Wikipedia"),
            app.bedtime.data.SearchTarget(app.bedtime.data.SearchTarget.Kind.LOOKUP, "pkg.dict", "Dictionary", "x"),
        )
        app.bedtime.ui.minimal.AppDrawerContent(
            apps = drawerApps, style = HomeStyle(), query = query, onQuery = {},
            results = app.bedtime.ui.minimal.AppDrawer.results(
                query, drawerApps, targets,
                listOf(app.bedtime.data.NoteTarget("scratchpad", "x"), app.bedtime.data.NoteTarget("movies list", "y")),
                app.bedtime.data.EventParser.parse(query, now),
            ),
            onLaunch = {}, onResult = {}, preview = true,
        )
    }

    @Test
    fun notes() = shot {
        val at = 1_761_420_000_000L
        app.bedtime.ui.notes.NotesContent(
            notes = listOf(
                app.bedtime.data.Note("a", "# Films to see\n- [ ] Perfect Days\n- [x] **Paterson**", at, at),
                app.bedtime.data.Note("b", "Call the plumber about the *kitchen* tap", at - 86_400_000L, at - 86_400_000L),
            ),
            onSave = { _, _ -> }, onDelete = {}, onBack = {},
        )
    }

    @Test
    fun quickNotes() = shot {
        app.bedtime.ui.notes.NoteTargetsContent(
            targets = listOf(app.bedtime.data.NoteTarget("scratchpad", "x"), app.bedtime.data.NoteTarget("movies list", "y")),
            calendars = listOf(1L to "Personal", 2L to "Work"),
            calendarId = null,
            calendarAccess = true,
            onAdd = {}, onRemove = {}, onCalendar = {}, onAllowCalendar = {}, onOpenNotes = {}, onBack = {},
        )
    }

    @Test
    fun lockScreenCustom() = shot {
        app.bedtime.ui.lock.LockScreenContent(
            now = Samples.lateEvening,
            scheduleName = "Focus",
            until = "16:10",
            style = HomeStyle(background = 0xFFF8F5F1, accent = 0xFFE0B243, showGreeting = false),
            quote = Samples.longQuote,
        )
    }

    @Test
    fun widgetPicker() = shot {
        app.bedtime.ui.widget.WidgetBlockPickerContent(blocks = listOf(Samples.focus, Samples.deepWork), onPick = {}, onCreate = {}, onBack = {})
    }

    @Test
    fun widget() {
        val context = paparazzi.context
        val column = android.widget.LinearLayout(context).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            setBackgroundColor(0xFFF8F5F1.toInt())
            setPadding(48, 48, 48, 48)
        }
        listOf(
            Triple("deep work", "1 h 30 min", R.drawable.widget_background),
            Triple("focus", "on · until 4:10 pm", R.drawable.widget_background_on),
        ).forEach { (title, subtitle, background) ->
            val widget = paparazzi.inflate<android.view.View>(R.layout.widget_block)
            widget.setBackgroundResource(background)
            widget.findViewById<android.widget.TextView>(R.id.widget_title).text = title
            widget.findViewById<android.widget.TextView>(R.id.widget_subtitle).text = subtitle
            column.addView(widget, android.widget.LinearLayout.LayoutParams(420, 150).apply { bottomMargin = 40 })
        }
        paparazzi.snapshot(column)
    }

    @Test
    fun unlockWait() = shot {
        FullScreen {
            UnlockLayout(
                "Bedtime",
                listOf("Wait", "Type"),
                current = 0,
                showCancel = true,
                onCancel = {},
                footnote = "This ends the current session. The timer only runs while this screen is open, " +
                    "and starts over if you back out.",
                note = "2nd unlock today \u00b7 the steps are longer",
                action = { app.bedtime.ui.components.SoftPillButton("Waiting\u2026", onClick = {}, enabled = false) },
            ) {
                WaitChallengeContent(remaining = 372_000, total = 600_000)
            }
        }
    }

    @Test
    fun unlockPassage() = shot {
        val target = TextChallenge.generate(220, app.bedtime.data.TextSource.PASSAGES, Random(4))
        FullScreen {
            UnlockLayout(
                "Bedtime",
                listOf("Wait", "Type"),
                current = 1,
                showCancel = true,
                onCancel = {},
                footnote = "This ends the current session. Spaces and punctuation fill themselves in; " +
                    "pasting and typos are rejected.",
            ) {
                TextChallengeContent(target = target, typed = target.take(48), rejected = 0, onValueChange = {})
            }
        }
    }

    @Test
    fun unlockText() = shot {
        val target = TextChallenge.generate(225, random = Random(7))
        FullScreen {
            UnlockLayout(
                "Bedtime",
                listOf("Wait", "Type"),
                current = 1,
                showCancel = true,
                onCancel = {},
                footnote = "This ends the current session. Spaces and punctuation fill themselves in; " +
                    "pasting and typos are rejected.",
                note = "2nd unlock today \u00b7 the steps are longer",
            ) {
                TextChallengeContent(target = target, typed = target.take(37), rejected = 2, onValueChange = {})
            }
        }
    }

    @Test
    fun unlockSuccess() = shot {
        FullScreen {
            UnlockLayout("Bedtime", listOf("Wait", "Type"), current = 2, showCancel = false, onCancel = {}) {
                UnlockSuccess("Bedtime is off until its next scheduled start.")
            }
        }
    }

    @Test
    fun launcherIcon() = shot {
        Box(Modifier.fillMaxSize().background(Obsidian.colors.bgPrimary), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(40.dp)) {
                Box(Modifier.size(300.dp).clip(RoundedCornerShape(72.dp)).background(Color(0xFF282828))) {
                    Image(painterResource(R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.fillMaxSize())
                }
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    Icon(painterResource(R.drawable.ic_tile), contentDescription = null, tint = Obsidian.colors.textNormal, modifier = Modifier.size(48.dp))
                    Icon(BedtimeIcons.Refuge, contentDescription = null, tint = Obsidian.colors.accent, modifier = Modifier.size(48.dp))
                }
            }
        }
    }
}

private object Samples {
    const val INSTAGRAM = "com.instagram.android"
    const val TIKTOK = "com.zhiliaoapp.musically"
    const val YOUTUBE = "com.google.android.youtube"
    const val REDDIT = "com.reddit.frontpage"
    const val X = "com.twitter.android"
    const val MAPS = "com.google.android.apps.maps"
    const val SPOTIFY = "com.spotify.music"
    const val PHONE = "com.google.android.dialer"
    const val KINDLE = "com.amazon.kindle"
    const val SETTINGS = "com.android.settings"
    const val MESSAGES = "com.google.android.apps.messaging"

    private const val MIN = 60_000L
    private const val HOUR = 60 * MIN
    private const val DAY = 24 * HOUR

    val apps = listOf(
        AppEntry(INSTAGRAM, "Instagram"),
        AppEntry(YOUTUBE, "YouTube"),
        AppEntry(TIKTOK, "TikTok"),
        AppEntry(REDDIT, "Reddit"),
        AppEntry(X, "X"),
        AppEntry("com.android.chrome", "Chrome"),
        AppEntry("com.google.android.gm", "Gmail"),
        AppEntry(KINDLE, "Kindle"),
        AppEntry(MAPS, "Maps"),
        AppEntry(PHONE, "Phone"),
        AppEntry(SPOTIFY, "Spotify"),
        AppEntry("com.whatsapp", "WhatsApp"),
        AppEntry(MESSAGES, "Messages"),
        AppEntry(SETTINGS, "Settings"),
    )

    val minimalApps: List<AppEntry> get() = apps.filter { it.packageName in setOf(MAPS, SPOTIFY, PHONE, KINDLE) }

    val lateEvening: LocalDateTime get() = LocalDate.now().atTime(22, 41)

    val shortQuote = Quote("The land knows you, even when you are lost.", "Robin Wall Kimmerer", "Braiding Sweetgrass")
    val mediumQuote = Quote(
        "In my walks I would fain return to my senses. What business have I in the woods, if I am thinking of something out of the woods?",
        "Henry David Thoreau",
        "Walking",
    )
    val longQuote = Quote(
        "Knowing that you love the earth changes you, activates you to defend and protect and celebrate. But when you feel that the earth " +
            "loves you in return, that feeling transforms the relationship from a one-way street into a sacred bond.",
        "Robin Wall Kimmerer",
        "Braiding Sweetgrass",
    )

    fun label(pkg: String): String = apps.firstOrNull { it.packageName == pkg }?.label ?: pkg

    val groups = listOf(
        AppGroup(id = "social", name = "Social & feeds", packages = setOf(INSTAGRAM, TIKTOK, YOUTUBE, REDDIT, X)),
        AppGroup(id = "essentials", name = "Essentials", packages = setOf(SETTINGS, PHONE, MESSAGES)),
    )

    val bedtime = Schedule(
        id = "bedtime",
        name = "Bedtime",
        startMinute = 22 * 60 + 30,
        endMinute = 7 * 60,
        blockedApps = setOf(INSTAGRAM, TIKTOK, YOUTUBE, REDDIT, X),
        greyscale = true,
        minimalMode = true,
        allowedApps = setOf(MAPS, SPOTIFY),
        dnd = DndMode.PRIORITY,
        hideNotifications = true,
        unlock = UnlockConfig(waitEnabled = true, waitMinutes = 10, textEnabled = true, textLength = 150),
    )

    private val work = Schedule(
        id = "work",
        name = "Work focus",
        days = (1..5).toSet(),
        startMinute = 9 * 60,
        endMinute = 12 * 60,
        enabled = false,
        blockedApps = setOf(INSTAGRAM, TIKTOK),
        greyscale = false,
        hideNotifications = true,
        unlock = UnlockConfig(waitEnabled = true, waitMinutes = 5, textEnabled = false),
        unlockAction = UnlockAction(UnlockMode.PAUSE, 10),
    )

    val focus = Schedule(
        id = "focus",
        name = "Focus",
        kind = ScheduleKind.BLOCK,
        durationMinutes = 60,
        blockedApps = setOf(INSTAGRAM, TIKTOK, YOUTUBE),
        greyscale = false,
        dnd = DndMode.PRIORITY,
        hideNotifications = true,
        unlock = UnlockConfig(waitEnabled = true, waitMinutes = 5, textEnabled = true, textLength = 100),
    )

    val deepWork = Schedule(
        id = "deep",
        name = "Deep work",
        kind = ScheduleKind.BLOCK,
        durationMinutes = 90,
        greyscale = true,
        minimalMode = true,
        dnd = DndMode.SILENCE,
        hideNotifications = true,
        unlock = UnlockConfig(waitEnabled = true, textEnabled = true),
    )

    private val all = listOf(focus, deepWork, bedtime, work)

    private fun block(id: String, name: String, minutes: Int) =
        Schedule(id = id, name = name, kind = ScheduleKind.BLOCK, durationMinutes = minutes, greyscale = false)

    /** Five blocks, so the grid needs a second row. */
    val manyBlocks: List<Schedule> get() = listOf(
        focus, deepWork, block("pomodoro", "Pomodoro", 25), block("study", "Study", 45), block("read", "Reading", 30), bedtime, work,
    )

    fun home(
        schedules: List<Schedule> = all,
        hour: Int = 18,
        minute: Int = 10,
        runningFocus: Boolean = false,
        serviceOn: Boolean = true,
        greyscaleOk: Boolean = true,
        dndOk: Boolean = true,
    ): HomeUiState {
        val zone = ZoneId.systemDefault()
        val now = LocalDate.now().atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()
        val runtime = if (runningFocus) RuntimeState(activeRuns = mapOf("focus" to Run(now - 20 * MIN, now + 40 * MIN))) else RuntimeState()
        return HomeUiState(
            quote = app.bedtime.data.Quotes.all.first(),
            schedules = schedules,
            active = ScheduleEvaluator.evaluate(now, zone, schedules, runtime),
            runtime = runtime,
            history = if (schedules.isEmpty()) emptyList() else history(now),
            serviceOn = serviceOn,
            greyscaleOk = greyscaleOk,
            dndOk = dndOk,
            now = now,
        )
    }

    val limits: List<app.bedtime.data.AppLimit> get() = listOf(
        app.bedtime.data.AppLimit(
            id = "social",
            name = "Social",
            rules = app.bedtime.data.LimitRules(
                apps = setOf("com.instagram.android", "com.zhiliaoapp.musically"),
                pauseSeconds = 10,
                dailyMinutes = 45,
            ),
        ),
        app.bedtime.data.AppLimit(
            id = "youtube",
            name = "YouTube",
            rules = app.bedtime.data.LimitRules(apps = setOf("com.google.android.youtube"), pauseSeconds = null, dailyMinutes = 30),
        ),
        app.bedtime.data.AppLimit(
            id = "news",
            name = "News",
            rules = app.bedtime.data.LimitRules(apps = setOf("com.reddit.frontpage"), pauseSeconds = 20),
            deleteFromDay = Long.MAX_VALUE,
        ),
    )

    /**
     * Two weeks of evenings: bedtimes most nights, a few focus blocks, two nights with a way out and a
     * day with nothing on.
     */
    fun fortnight(now: Long): List<SessionLog> {
        val today = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return (1..13).filter { it != 5 }.flatMap { back ->
            val evening = today - back * DAY + 22 * HOUR + 30 * MIN
            val hours = listOf(8, 7, 8, 6, 8, 9, 7, 8, 5, 8, 7, 8, 8)[back - 1]
            val bed = SessionLog(
                "bedtime", "Bedtime", evening, evening + hours * HOUR,
                unlockTimes = if (back == 3) listOf(evening + HOUR) else emptyList(),
                endedEarlyAt = if (back == 3) evening + 4 * HOUR else null,
                breakTimes = if (back == 9) listOf(evening + 2 * HOUR) else emptyList(),
            )
            val focus = if (back % 3 == 0) {
                listOf(SessionLog("focus", "Focus", today - back * DAY + 10 * HOUR, today - back * DAY + 11 * HOUR + 30 * MIN))
            } else {
                emptyList()
            }
            listOf(bed) + focus
        } + SessionLog("focus", "Focus", today + 9 * HOUR, today + 10 * HOUR)
    }

    /** Five kept bedtimes, and an older focus block that was unlocked early. */
    private fun history(now: Long): List<SessionLog> =
        (1..5).map { d -> SessionLog("bedtime", "Bedtime", now - d * DAY - 2 * HOUR, now - d * DAY + 7 * HOUR) } +
            SessionLog(
                "focus", "Focus", now - 6 * DAY, now - 6 * DAY + HOUR, listOf(now - 6 * DAY + 30 * MIN), now - 6 * DAY + 30 * MIN,
            )
}
