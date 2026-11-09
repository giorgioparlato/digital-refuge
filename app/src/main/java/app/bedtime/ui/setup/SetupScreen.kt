package app.bedtime.ui.setup

import androidx.compose.runtime.mutableLongStateOf
import app.bedtime.service.HomeRole
import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.AppSettings
import app.bedtime.data.Repository
import app.bedtime.engine.Engine
import app.bedtime.ui.pluralApps
import app.bedtime.service.DndController
import app.bedtime.service.GreyscaleController
import app.bedtime.service.SystemApps
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.PlainButton
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(onBack: () -> Unit, onAlwaysAvailable: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val state by Engine.state(context).collectAsStateWithLifecycle()
    val repo = remember { Repository.get(context) }
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    var serviceOn by remember { mutableStateOf(false) }
    var greyscaleOk by remember { mutableStateOf(false) }
    var dndOk by remember { mutableStateOf(false) }
    var overlayOk by remember { mutableStateOf(false) }
    var batteryOk by remember { mutableStateOf(false) }
    var notificationsOk by remember { mutableStateOf(true) }
    var askedNotifications by rememberSaveable { mutableStateOf(false) }
    var previewing by remember { mutableStateOf(false) }
    var isDefaultHome by remember { mutableStateOf(HomeRole.isDefault(context)) }
    // Android only shows its "use as your home app?" when asked for an answer back. If it comes back at
    // once without asking (declined before, or held elsewhere for good), its own home-app page is where
    // to choose instead.
    var askedHomeAt by remember { mutableLongStateOf(0L) }
    val chooseHome = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefaultHome = HomeRole.isDefault(context)
        if (!isDefaultHome && System.currentTimeMillis() - askedHomeAt < 800) context.tryStart(Intent(Settings.ACTION_HOME_SETTINGS))
    }
    val askHome = {
        askedHomeAt = System.currentTimeMillis()
        runCatching { chooseHome.launch(HomeRole.askIntent(context)) }
    }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsOk = granted
    }
    LifecycleResumeEffect(Unit) {
        serviceOn = SystemApps.isAccessibilityServiceEnabled(context)
        greyscaleOk = GreyscaleController.isAvailable(context)
        dndOk = DndController.hasAccess(context)
        overlayOk = Settings.canDrawOverlays(context)
        batteryOk = SetupSteps.batteryUnrestricted(context)
        notificationsOk = NotificationManagerCompat.from(context).areNotificationsEnabled()
        isDefaultHome = HomeRole.isDefault(context)
        onPauseOrDispose { }
    }

    SetupContent(
        serviceOn = serviceOn,
        greyscaleOk = greyscaleOk,
        dndOk = dndOk,
        previewing = previewing,
        canPreview = !previewing && state?.greyscale != true,
        onBack = onBack,
        onOpenAccessibility = { context.tryStart(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
        onOpenAppInfo = {
            context.tryStart(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
        },
        onPreview = {
            previewing = true
            scope.launch {
                GreyscaleController.apply(context, true)
                delay(5_000)
                GreyscaleController.apply(context, false)
                previewing = false
            }
        },
        onOpenDnd = { context.tryStart(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) },
        onOpenBattery = { context.tryStart(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) },
        batteryOk = batteryOk,
        overlayOk = overlayOk,
        onOpenOverlay = {
            context.tryStart(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.fromParts("package", context.packageName, null)))
        },
        alwaysAvailableCount = settings.alwaysAvailable.size,
        onAlwaysAvailable = onAlwaysAvailable,
        notificationsOk = notificationsOk,
        greyscaleViaModes = Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM,
        launcherOn = settings.homeScreen,
        isDefaultHome = isDefaultHome,
        onLauncher = { on ->
            HomeRole.setOffered(context, on)
            scope.launch { repo.updateSettings { it.copy(homeScreen = on) } }
            // Switched on, Android asks; switched off, it falls back to your other home app by itself.
            if (on) askHome()
        },
        onChooseHome = { askHome() },
        onAllowNotifications = {
            // Ask once; after that (or before Android 13) the switch lives in the app's notification settings.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !askedNotifications) {
                askedNotifications = true
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.tryStart(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
            }
        },
    )
}

@Composable
internal fun SetupContent(
    serviceOn: Boolean,
    greyscaleOk: Boolean,
    dndOk: Boolean,
    previewing: Boolean,
    canPreview: Boolean,
    onBack: () -> Unit,
    onOpenAccessibility: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onPreview: () -> Unit,
    onOpenDnd: () -> Unit,
    onOpenBattery: () -> Unit,
    batteryOk: Boolean = false,
    overlayOk: Boolean = true,
    onOpenOverlay: () -> Unit = {},
    alwaysAvailableCount: Int = 0,
    onAlwaysAvailable: () -> Unit = {},
    notificationsOk: Boolean = true,
    onAllowNotifications: () -> Unit = {},
    greyscaleViaModes: Boolean = false,
    launcherOn: Boolean = false,
    isDefaultHome: Boolean = false,
    onLauncher: (Boolean) -> Unit = {},
    onChooseHome: () -> Unit = {},
) {
    val c = Obsidian.colors
    val steps = listOf(serviceOn, alwaysAvailableCount > 0, batteryOk, dndOk, notificationsOk, overlayOk)
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Setup", onBack = onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PageGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "A few switches so refuge can do its job. Nothing ever leaves your phone.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textMuted,
            )
            Progress(steps.count { it }, steps.size)

            Group("Needed")
            Step("See which app is open", serviceOn, "How refuge notices a blocked app the moment it opens. It only learns which app is in front.") {
                AccessibilityHow(serviceOn, onOpenAppInfo, onOpenAccessibility)
            }

            Group("Recommended")
            Step(
                "Always-available apps",
                alwaysAvailableCount > 0,
                "Usable in every session, from the emergency button: maps, rides, your authenticator. Keep the list short.",
            ) {
                if (alwaysAvailableCount == 0) {
                    PlainButton("Choose apps", onClick = onAlwaysAvailable, modifier = Modifier.fillMaxWidth())
                } else {
                    Link("Change · ${pluralApps(alwaysAvailableCount)}", onAlwaysAvailable)
                }
            }
            Step("Keep refuge running", batteryOk, "Some phones put background apps to sleep, and blocking with them. Battery usage set to unrestricted keeps it awake.") {
                if (!batteryOk) PlainButton("Open battery settings", onClick = onOpenBattery, modifier = Modifier.fillMaxWidth())
            }

            Group("Optional")
            Step(
                if (greyscaleViaModes) "Do not disturb and greyscale" else "Do not disturb",
                dndOk,
                if (greyscaleViaModes) {
                    "Sessions can silence the phone, hold notifications until they end, and fade apps to black and white."
                } else {
                    "Sessions can silence the phone and hold notifications until they end. Greyscale needs Android 15 or newer."
                },
            ) {
                if (!dndOk) PlainButton("Open do not disturb access", onClick = onOpenDnd, modifier = Modifier.fillMaxWidth())
                if (greyscaleOk) Link(if (previewing) "Greyscale for 5 seconds…" else "Try greyscale for 5 seconds", onPreview.takeIf { canPreview })
            }
            Step("A lotus while a session runs", notificationsOk, "A silent notification: a small lotus in the status bar, with the time left.") {
                if (!notificationsOk) PlainButton("Allow notifications", onClick = onAllowNotifications, modifier = Modifier.fillMaxWidth())
            }
            Step("The full-screen reminder", overlayOk, "If blocking is switched off during a session, refuge covers the screen until it's back on.") {
                if (!overlayOk) PlainButton("Allow display over other apps", onClick = onOpenOverlay, modifier = Modifier.fillMaxWidth())
            }

            // The everyday launcher is a choice, not a step: it's in settings › home screen.
            Faint("Refuge's home screen all the time, not only in sessions: settings › home screen › everyday launcher.", Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(8.dp))
        }
    }
}

/**
 * Switching on the accessibility service: Android hides it for apps from outside the Play Store, so two
 * parts, in order, each with its button; once on, a way back to it. Android's warning explained a tap away.
 */
@Composable
internal fun AccessibilityHow(serviceOn: Boolean, onOpenAppInfo: () -> Unit, onOpenAccessibility: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!serviceOn) {
            Part(1, "Allow restricted settings", "In app info, tap ⋮ at the top right, then \u201callow restricted settings\u201d. Not there? Skip to 2.")
            PlainButton("Open app info", onClick = onOpenAppInfo, modifier = Modifier.fillMaxWidth())
            Part(2, "Switch on digital refuge blocker", "In accessibility settings, usually under \u201cdownloaded apps\u201d. Confirm what Android asks.")
            CtaButton("Open accessibility settings", onClick = onOpenAccessibility, modifier = Modifier.fillMaxWidth())
            Faint("Switch still greyed out? Check the ⋮ menu in app info again: the option shows only once, and again after a reinstall.")
        } else {
            Link("Accessibility settings", onOpenAccessibility)
        }
        AndroidWarning()
    }
}

/** How many are done, in words and as a thin bar. */
@Composable
private fun Progress(done: Int, of: Int) {
    val c = Obsidian.colors
    Column(Modifier.padding(top = 4.dp)) {
        Text(if (done == of) "All done" else "$done of $of done", style = MaterialTheme.typography.labelLarge, color = c.textNormal)
        Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(c.interactive)) {
            Box(Modifier.fillMaxWidth(done.toFloat() / of).height(4.dp).clip(RoundedCornerShape(2.dp)).background(c.accentFill))
        }
    }
}

@Composable
private fun Group(name: String) {
    Text(name, style = MaterialTheme.typography.labelLarge, color = Obsidian.colors.textFaint, modifier = Modifier.padding(top = 12.dp))
}

/**
 * One step: its tick, its name and what it's for; under them, what to do while it's undone, or a
 * quiet way back to it once it's done.
 */
@Composable
private fun Step(title: String, done: Boolean, about: String, content: @Composable ColumnScope.() -> Unit = {}) {
    val c = Obsidian.colors
    SectionCard {
        Row {
            Tick(done)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Column {
                    Text(title, style = MaterialTheme.typography.titleMedium, color = c.textNormal)
                    Text(about, style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.padding(top = 2.dp))
                }
                content()
            }
        }
    }
}

/** Done: filled, with a check. Not yet: an empty ring. */
@Composable
private fun Tick(done: Boolean) {
    val c = Obsidian.colors
    Box(
        Modifier
            .padding(top = 2.dp)
            .size(22.dp)
            .clip(CircleShape)
            .background(if (done) c.accentFill else Color.Transparent)
            .border(1.5.dp, if (done) c.accentFill else c.textFaint, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (done) Icon(Icons.Default.Check, contentDescription = "done", tint = c.textOnAccent, modifier = Modifier.size(14.dp))
    }
}

/** A part of a step, numbered: what it is, then where to find it. */
@Composable
private fun Part(number: Int, title: String, how: String) {
    val c = Obsidian.colors
    Row(Modifier.padding(top = 4.dp)) {
        Text("$number", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = c.accentText, modifier = Modifier.width(20.dp))
        Column {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = c.textNormal)
            Text(how, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
    }
}

/** A quiet action, as text: for steps already done. */
@Composable
private fun Link(label: String, onClick: (() -> Unit)?) {
    val c = Obsidian.colors
    Text(
        "$label ›",
        style = MaterialTheme.typography.labelLarge,
        color = if (onClick != null) c.accentText else c.textFaint,
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 4.dp),
    )
}

@Composable
private fun Faint(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = Obsidian.colors.textFaint, modifier = modifier)
}

/**
 * What Android's warning for the switch means, folded to one line: it reads the same for every app that
 * uses it, and says far more than refuge is given.
 */
@Composable
private fun AndroidWarning() {
    val c = Obsidian.colors
    var open by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.interactive.copy(alpha = 0.5f)).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable(onClickLabel = if (open) "fold" else "read it") { open = !open },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("About Android's warning", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = c.textNormal, modifier = Modifier.weight(1f))
            Text(if (open) "▴" else "▾", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
        }
        if (open) {
            listOf(
                "Android says the app can \u201cview and control the screen\u201d. It shows that for every app that uses this switch.",
                "What refuge actually gets: a word when a different app comes to the front, and the name of that app or Settings page. It can't read what's on the screen or what you type, and it can't tap or swipe for you.",
                "Refuge has no internet access at all: Android doesn't let it go online, so nothing from it can leave your phone.",
                "What it's used for: blocking an app the moment it opens, keeping blocking from being switched off in Settings during a session, turning the screen off from refuge's lock screen, and noticing when you unlock.",
            ).forEach { Text(it, style = MaterialTheme.typography.bodySmall, color = c.textMuted, modifier = Modifier.padding(top = 8.dp)) }
        }
    }
}

internal fun Context.tryStart(intent: Intent) {
    runCatching { startActivity(intent) }
}

/** The setup steps that can be left undone, counted the same wherever "steps left" is said. */
object SetupSteps {
    fun batteryUnrestricted(context: Context): Boolean =
        context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true

    /**
     * The six steps not yet done. Greyscale comes with do not disturb access, so it's no step of its own;
     * using refuge as your launcher is a choice, not a step left.
     */
    fun left(context: Context, alwaysAvailableCount: Int): Int = listOf(
        SystemApps.isAccessibilityServiceEnabled(context),
        DndController.hasAccess(context),
        NotificationManagerCompat.from(context).areNotificationsEnabled(),
        Settings.canDrawOverlays(context),
        alwaysAvailableCount > 0,
        batteryUnrestricted(context),
    ).count { !it }
}
