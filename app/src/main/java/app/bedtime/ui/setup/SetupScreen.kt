package app.bedtime.ui.setup

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
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
import app.bedtime.ui.components.Callout
import app.bedtime.ui.components.CalloutKind
import app.bedtime.ui.components.CodeBlock
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.PlainButton
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.StatusDot
import app.bedtime.ui.components.StatusPill
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SetupScreen(onBack: () -> Unit, onAlwaysAvailable: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val state by Engine.state(context).collectAsStateWithLifecycle()
    val repo = remember { Repository.get(context) }
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    var serviceOn by remember { mutableStateOf(false) }
    var greyscaleOk by remember { mutableStateOf(false) }
    var dndOk by remember { mutableStateOf(false) }
    var notificationsOk by remember { mutableStateOf(true) }
    var askedNotifications by rememberSaveable { mutableStateOf(false) }
    var previewing by remember { mutableStateOf(false) }
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsOk = granted
    }
    LifecycleResumeEffect(Unit) {
        serviceOn = SystemApps.isAccessibilityServiceEnabled(context)
        greyscaleOk = GreyscaleController.hasPermission(context)
        dndOk = DndController.hasAccess(context)
        notificationsOk = NotificationManagerCompat.from(context).areNotificationsEnabled()
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
        onCopyCommand = { clipboard.setText(AnnotatedString(GreyscaleController.ADB_GRANT_COMMAND)) },
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
        alwaysAvailableCount = settings.alwaysAvailable.size,
        onAlwaysAvailable = onAlwaysAvailable,
        notificationsOk = notificationsOk,
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
    onCopyCommand: () -> Unit,
    onPreview: () -> Unit,
    onOpenDnd: () -> Unit,
    onOpenBattery: () -> Unit,
    alwaysAvailableCount: Int = 0,
    onAlwaysAvailable: () -> Unit = {},
    notificationsOk: Boolean = true,
    onAllowNotifications: () -> Unit = {},
) {
    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Setup", onBack = onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "A few one-time steps so digital refuge can do its job. Nothing ever leaves your phone.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textMuted,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            StepCard(1, "Let digital refuge see which app is open", done = serviceOn, badge = "Required") {
                Body("digital refuge uses Android's accessibility service to notice when a blocked app opens. It doesn't read what's on your screen or what you type.")
                CtaButton("Open accessibility settings", onClick = onOpenAccessibility, modifier = Modifier.fillMaxWidth())
                Callout(
                    title = "Switch greyed out?",
                    kind = CalloutKind.INFO,
                    body = "Android 13 and newer are extra careful with apps installed outside the Play Store. Open App info, tap ⋮ in the top-right corner, choose “Allow restricted settings”, then try again.",
                    actionLabel = "Open App info",
                    onAction = onOpenAppInfo,
                )
            }

            StepCard(2, "Allow greyscale", done = greyscaleOk, badge = "Optional") {
                Body("Android doesn't let apps switch on greyscale by themselves, so this needs a computer, just once:")
                NumberedLine(1, "On your phone, turn on Developer options → USB debugging.")
                NumberedLine(2, "Plug it into a computer that has Android Studio installed.")
                NumberedLine(3, "Run this command:")
                CodeBlock(GreyscaleController.ADB_GRANT_COMMAND, onCopy = onCopyCommand)
                if (greyscaleOk) {
                    PlainButton(
                        if (previewing) "Previewing…" else "Try it for 5 seconds",
                        onClick = onPreview,
                        enabled = canPreview,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            StepCard(3, "Allow Do Not Disturb", done = dndOk, badge = "Optional") {
                Body("Lets schedules and blocks silence your phone and hold notifications back until they end. Find digital refuge in the list and switch it on.")
                PlainButton("Open Do Not Disturb access", onClick = onOpenDnd, modifier = Modifier.fillMaxWidth())
            }

            StepCard(4, "Show a lotus while a session runs", done = notificationsOk, badge = "Optional") {
                Body("A quiet notification puts a small lotus in the status bar during schedules and blocks, with the time left. It never makes a sound.")
                if (!notificationsOk) {
                    PlainButton("Allow notifications", onClick = onAllowNotifications, modifier = Modifier.fillMaxWidth())
                }
            }

            StepCard(5, "Choose always-available apps", done = alwaysAvailableCount > 0, badge = "Recommended") {
                Body(
                    "Apps that stay usable during every session, reached from the emergency button: maps, rides, your authenticator. " +
                        "Keep the list short. You can change it later in settings → emergency.",
                )
                PlainButton(
                    if (alwaysAvailableCount == 0) "Choose apps" else "Edit · ${pluralApps(alwaysAvailableCount)}",
                    onClick = onAlwaysAvailable,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            StepCard(6, "Keep digital refuge running", done = false, badge = "Recommended") {
                Body("Some phones (Samsung, Xiaomi, OnePlus…) put background apps to sleep. If blocking ever stops working, set digital refuge's battery usage to Unrestricted.")
                PlainButton("Open battery settings", onClick = onOpenBattery, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StepCard(number: Int, title: String, done: Boolean, badge: String, content: @Composable ColumnScope.() -> Unit) {
    val c = Obsidian.colors
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(done, number)
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.textNormal, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            if (done) {
                StatusPill("Done", c.green)
            } else {
                StatusPill(badge, if (badge == "Required") c.orange else c.textMuted)
            }
        }
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { content() }
    }
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = Obsidian.colors.textMuted)
}

@Composable
private fun NumberedLine(number: Int, text: String) {
    val c = Obsidian.colors
    Row {
        Text("$number.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = c.accentText, modifier = Modifier.width(22.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = c.textNormal)
    }
}

private fun Context.tryStart(intent: Intent) {
    runCatching { startActivity(intent) }
}
