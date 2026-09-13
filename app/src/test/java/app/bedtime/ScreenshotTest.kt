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
import app.bedtime.ui.apps.AppPickerContent
import app.bedtime.ui.blocked.BlockedContent
import app.bedtime.ui.components.BedtimeIcons
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

    private fun shot(tall: Boolean = false, content: @Composable () -> Unit) {
        if (tall) paparazzi.unsafeUpdateConfig(deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 5200))
        paparazzi.snapshot("dark") { BedtimeTheme(darkTheme = true) { content() } }
        paparazzi.snapshot("light") { BedtimeTheme(darkTheme = false) { content() } }
    }

    /** Background + padding for pieces that don't draw their own screen. */
    @Composable
    private fun Screen(content: @Composable () -> Unit) {
        Box(Modifier.fillMaxSize().background(Obsidian.colors.bgPrimary).padding(20.dp)) { content() }
    }

    @Composable
    private fun Home(ui: HomeUiState) {
        HomeContent(
            ui = ui,
            onEdit = {},
            onCreate = {},
            onTemplate = {},
            onToggle = { _, _ -> },
            onStartBlock = { _, _ -> },
            onSettings = {},
            onSetup = {},
            onUnlock = {},
        )
    }

    @Composable
    private fun Edit(draft: Schedule, readOnly: Boolean = false, dndAvailable: Boolean = true) {
        ScheduleEditContent(
            draft = draft,
            onDraftChange = {},
            isNew = false,
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
    fun create() = shot(tall = true) { CreateContent(onBack = {}, onPick = {}) }

    @Test
    fun editSchedule() = shot(tall = true) { Edit(Samples.bedtime) }

    @Test
    fun editBlock() = shot(tall = true) { Edit(Samples.focus, dndAvailable = false) }

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
            onCopyCommand = {},
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
    fun homeStyle() = shot(tall = true) {
        HomeStyleContent(style = HomeStyle(), onChange = {}, previewApps = Samples.minimalApps, now = Samples.lateEvening, onBack = {}, quote = Samples.shortQuote)
    }

    @Test
    fun blocked() = shot { BlockedContent("Instagram", "Bedtime", "07:00", {}, {}) }

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
            Triple("focus", "60 min · tap to start", R.drawable.widget_background),
            Triple("focus", "on · until 4:10 pm", R.drawable.widget_background_on),
        ).forEach { (title, subtitle, background) ->
            val widget = paparazzi.inflate<android.view.View>(R.layout.widget_block)
            widget.setBackgroundResource(background)
            widget.findViewById<android.widget.TextView>(R.id.widget_title).text = title
            widget.findViewById<android.widget.TextView>(R.id.widget_subtitle).text = subtitle
            column.addView(widget, android.widget.LinearLayout.LayoutParams(720, 190).apply { bottomMargin = 40 })
        }
        paparazzi.snapshot(column)
    }

    @Test
    fun unlockWait() = shot {
        Screen {
            UnlockLayout("Bedtime", "This ends the current session.", listOf("Wait", "Type"), current = 0, showCancel = true, onCancel = {}) {
                WaitChallengeContent(remaining = 372_000, total = 600_000, onContinue = {})
            }
        }
    }

    @Test
    fun unlockText() = shot {
        val target = TextChallenge.generate(225, Random(7))
        Screen {
            UnlockLayout(
                "Bedtime",
                "This ends the current session.",
                listOf("Wait", "Type"),
                current = 1,
                showCancel = true,
                onCancel = {},
                note = "This is early unlock #2 today, so the text is 50% longer.",
            ) {
                TextChallengeContent(target = target, typed = target.take(37), rejected = 2, onValueChange = {})
            }
        }
    }

    @Test
    fun unlockSuccess() = shot {
        Screen {
            UnlockLayout("Bedtime", "This ends the current session.", listOf("Wait", "Type"), current = 2, showCancel = false, onCancel = {}) {
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

    /** Five kept bedtimes, and an older focus block that was unlocked early. */
    private fun history(now: Long): List<SessionLog> =
        (1..5).map { d -> SessionLog("bedtime", "Bedtime", now - d * DAY - 2 * HOUR, now - d * DAY + 7 * HOUR) } +
            SessionLog(
                "focus", "Focus", now - 6 * DAY, now - 6 * DAY + HOUR, listOf(now - 6 * DAY + 30 * MIN), now - 6 * DAY + 30 * MIN,
            )
}
