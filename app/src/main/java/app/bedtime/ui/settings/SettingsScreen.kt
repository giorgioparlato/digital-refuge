package app.bedtime.ui.settings

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
import app.bedtime.ui.formatMinutes
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.bedtime.ui.widget.BlockWidget
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun SettingsScreen(
    onBack: (() -> Unit)? = null,
    onSetup: () -> Unit,
    onHomeStyle: () -> Unit,
    onGroups: () -> Unit,
    onAlwaysAvailable: () -> Unit,
    onHowItWorks: () -> Unit,
    onWalkthrough: () -> Unit = {},
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
    SettingsContent(
        blocks = schedules.filter { it.isBlock },
        tileBlockId = settings.tileBlockId,
        setupStepsLeft = stepsLeft,
        onBack = onBack,
        onSetup = onSetup,
        onHomeStyle = onHomeStyle,
        onTileBlock = { id -> scope.launch { repo.updateSettings { it.copy(tileBlockId = id) } } },
        groupCount = settings.groups.size,
        onGroups = onGroups,
        alwaysAvailableCount = settings.alwaysAvailable.size,
        onAlwaysAvailable = onAlwaysAvailable,
        version = version,
        canPinWidget = canPinWidget,
        onAddWidget = { block -> BlockWidget.requestPin(context, block) },
        lockSettings = settings.lockSettingsDuringSessions,
        onLockSettings = { on -> scope.launch { repo.updateSettings { it.copy(lockSettingsDuringSessions = on) } } },
        fullScreenAlert = settings.fullScreenAlert,
        onFullScreenAlert = { on -> scope.launch { repo.updateSettings { it.copy(fullScreenAlert = on) } } },
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
    version: String = "0.7.7.2",
    canPinWidget: Boolean = true,
    onAddWidget: (Schedule) -> Unit = {},
    onExport: () -> Unit = {},
    onImport: () -> Unit = {},
    lockSettings: Boolean = true,
    onLockSettings: (Boolean) -> Unit = {},
    fullScreenAlert: Boolean = true,
    onFullScreenAlert: (Boolean) -> Unit = {},
    onHowItWorks: () -> Unit = {},
    onWalkthrough: () -> Unit = {},
) {
    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Settings", onBack = onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconBadge(BedtimeIcons.Refuge, size = 56.dp)
                Spacer(Modifier.width(16.dp))
                Column {
                    Text("digital refuge", style = MaterialTheme.typography.titleLarge, color = c.textNormal)
                    Text("Version $version · everything stays on this phone", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                }
            }

            SectionCard {
                OptionRow(
                    Icons.Default.Info,
                    "How it works",
                    description = "What a session locks, and your ways out",
                    onClick = onHowItWorks,
                ) { Chevron() }
                OptionRow(
                    BedtimeIcons.Leaf,
                    "The intro again",
                    description = "The short walk-through from the first launch",
                    onClick = onWalkthrough,
                ) { Chevron() }
            }

            SectionCard {
                OptionRow(
                    Icons.Default.Build,
                    "Permissions & setup",
                    description = when (setupStepsLeft) {
                        0 -> "All set"
                        1 -> "1 step left"
                        else -> "$setupStepsLeft steps left"
                    },
                    onClick = onSetup,
                ) { Chevron() }
                OptionRow(
                    Icons.Default.Home,
                    "Minimal home screen",
                    description = "Colours, text size and what's shown",
                    onClick = onHomeStyle,
                ) { Chevron() }
                OptionRow(
                    BedtimeIcons.Grid,
                    "App groups",
                    description = when (groupCount) {
                        0 -> "Tick many apps at once when choosing"
                        1 -> "1 group"
                        else -> "$groupCount groups"
                    },
                    onClick = onGroups,
                ) { Chevron() }
            }

            SectionCard(title = "Emergency", subtitle = "What the Emergency button on the minimal home screen offers, besides calling.") {
                OptionRow(
                    Icons.Default.Warning,
                    "Always-available apps",
                    description = when (alwaysAvailableCount) {
                        0 -> "None yet · maps, rides, authenticators…"
                        1 -> "1 app, never blocked"
                        else -> "$alwaysAvailableCount apps, never blocked"
                    },
                    onClick = onAlwaysAvailable,
                ) { Chevron() }
            }

            SectionCard(
                title = "Staying blocked",
                subtitle = "How firmly a session holds. Tap \u201chow it works\u201d for the full picture.",
            ) {
                OptionRow(
                    BedtimeIcons.Refuge,
                    "Lock changes during sessions",
                    description = "Covers the Settings screens that switch blocking off or uninstall the app",
                ) { ObsidianToggle(lockSettings, onLockSettings) }
                OptionRow(
                    Icons.Default.Warning,
                    "Full-screen reminder if blocking goes off",
                    description = "If blocking is switched off mid-session, take over the screen until it's back on. Allow it in setup.",
                ) { ObsidianToggle(fullScreenAlert, onFullScreenAlert) }
            }

            SectionCard(
                title = "Backup",
                subtitle = "Keep your schedules and settings in a file, to restore after reinstalling.",
            ) {
                OptionRow(
                    Icons.Default.Share,
                    "Export settings",
                    description = "Save schedules, blocks, groups and stats to a file",
                    onClick = onExport,
                ) { Chevron() }
                OptionRow(
                    BedtimeIcons.Refuge,
                    "Import settings",
                    description = "Replace everything with a saved file",
                    onClick = onImport,
                ) { Chevron() }
            }

            SectionCard(
                title = "Home-screen widget",
                subtitle = if (canPinWidget) {
                    "A one-tap button on your home screen that starts a block. Add one for:"
                } else {
                    "Long-press your home screen → widgets → digital refuge, then pick a block."
                },
            ) {
                if (blocks.isEmpty()) {
                    Text("Create a focus block first.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                } else if (canPinWidget) {
                    blocks.forEach { block ->
                        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
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
                    }
                }
            }

            SectionCard(
                title = "Quick Settings tile",
                subtitle = "Edit your quick settings and add the “refuge” tile. Tapping it starts:",
            ) {
                if (blocks.isEmpty()) {
                    Text("Create a focus block first.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                } else {
                    val selected = blocks.firstOrNull { it.id == tileBlockId }?.id ?: blocks.first().id
                    blocks.forEach { block ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .clickable { onTileBlock(block.id) },
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
                    }
                }
            }
        }
    }
}
