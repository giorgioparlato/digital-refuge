package app.bedtime.ui.apps

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.apps.AppCatalog
import app.bedtime.apps.AppEntry
import app.bedtime.data.AppGroup
import app.bedtime.data.AppSettings
import app.bedtime.data.Repository
import app.bedtime.data.toggleGroup
import app.bedtime.service.SystemApps
import app.bedtime.ui.components.AppIcon
import app.bedtime.ui.components.BottomActionBar
import app.bedtime.ui.components.Callout
import app.bedtime.ui.components.CalloutKind
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.ObsidianTextField
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.QuickChip
import app.bedtime.ui.pluralApps
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the picked apps are for; changes the guidance and whether groups are offered. */
enum class PickerMode { BLOCK, ALLOW, ALWAYS, GROUP }

@Composable
fun AppPickerScreen(title: String, initial: Set<String>, mode: PickerMode, onDone: (Set<String>) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    var apps by remember { mutableStateOf<List<AppEntry>?>(null) }
    var selected by remember { mutableStateOf(initial) }
    var query by remember { mutableStateOf("") }
    val essentials = remember { SystemApps.essentials(context) }
    val alwaysAllowed = remember { setOfNotNull(SystemApps.dialerPackage(context)) }
    val risky = remember { setOfNotNull(SystemApps.settingsPackage(context)) }
    BackHandler(onBack = onBack)

    LaunchedEffect(Unit) {
        // Previously selected apps first; order is fixed at load so rows don't jump while toggling.
        apps = withContext(Dispatchers.IO) {
            AppCatalog.launchableApps(context).sortedByDescending { it.packageName in initial }
        }
    }

    AppPickerContent(
        title = title,
        apps = apps,
        selected = selected,
        query = query,
        onQueryChange = { query = it },
        onToggle = { pkg -> selected = if (pkg in selected) selected - pkg else selected + pkg },
        onDone = { onDone(selected) },
        onBack = onBack,
        mode = mode,
        essentials = essentials,
        alwaysAllowed = alwaysAllowed,
        riskyPackages = risky,
        groups = if (mode == PickerMode.GROUP) emptyList() else settings.groups,
        onToggleGroup = { group -> selected = toggleGroup(selected, group) },
        onSaveGroup = if (mode == PickerMode.GROUP) {
            null
        } else {
            { name -> scope.launch { repo.saveGroup(AppGroup(name = name, packages = selected)) } }
        },
    )
}

@Composable
internal fun AppPickerContent(
    title: String,
    apps: List<AppEntry>?,
    selected: Set<String>,
    query: String,
    onQueryChange: (String) -> Unit,
    onToggle: (String) -> Unit,
    onDone: () -> Unit,
    onBack: () -> Unit,
    mode: PickerMode = PickerMode.BLOCK,
    essentials: Set<String> = emptySet(),
    alwaysAllowed: Set<String> = emptySet(),
    /** Apps that would make an escape trivial if always available (Settings). */
    riskyPackages: Set<String> = emptySet(),
    groups: List<AppGroup> = emptyList(),
    onToggleGroup: (Set<String>) -> Unit = {},
    onSaveGroup: ((String) -> Unit)? = null,
) {
    val c = Obsidian.colors
    var naming by remember { mutableStateOf(false) }
    if (naming && onSaveGroup != null) {
        SaveGroupDialog(onDismiss = { naming = false }, onSave = { name ->
            onSaveGroup(name)
            naming = false
        })
    }

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = { ObsidianTopBar(title, onBack = onBack) },
        bottomBar = {
            BottomActionBar {
                CtaButton(
                    if (selected.isEmpty()) "Done" else "Done · ${pluralApps(selected.size)}",
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ObsidianTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                placeholder = "Search apps",
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = c.textMuted) },
            )
            if (apps == null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = c.accent)
                }
                return@Scaffold
            }
            val installed = remember(apps) { apps.map { it.packageName }.toSet() }
            val usableGroups = groups.map { it to (it.packages intersect installed) }.filter { it.second.isNotEmpty() }
            val canSaveGroup = onSaveGroup != null && selected.isNotEmpty()
            val filtered = if (query.isBlank()) apps else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }

            LazyColumn(Modifier.fillMaxSize()) {
                if (mode == PickerMode.ALLOW && query.isBlank()) {
                    item(key = "info") { AllowInfo(apps, selected, essentials, alwaysAllowed) }
                }
                if (mode == PickerMode.ALWAYS && query.isBlank()) {
                    item(key = "info") { AlwaysInfo(apps, selected, riskyPackages) }
                }
                if (query.isBlank() && (usableGroups.isNotEmpty() || canSaveGroup)) {
                    item(key = "groups") { GroupChips(usableGroups, selected, onToggleGroup, canSaveGroup, onSave = { naming = true }) }
                }
                if (filtered.isEmpty()) {
                    item(key = "none") {
                        Text(
                            "No apps match “${query.trim()}”.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = c.textMuted,
                            modifier = Modifier.padding(24.dp),
                        )
                    }
                }
                items(filtered, key = { it.packageName }) { app ->
                    AppRow(app, checked = app.packageName in selected, onToggle = { onToggle(app.packageName) })
                }
            }
        }
    }
}

/** Minimal mode guidance: essentials start out ticked, and unticking them is called out. */
@Composable
private fun AllowInfo(apps: List<AppEntry>, selected: Set<String>, essentials: Set<String>, alwaysAllowed: Set<String>) {
    val missing = (essentials - alwaysAllowed - selected).map { pkg -> apps.firstOrNull { it.packageName == pkg }?.label ?: pkg }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Callout(
            title = "Keep a way out",
            kind = CalloutKind.INFO,
            body = "Settings, Phone and Messages start out allowed so you can always call, text and reach Settings. " +
                "Phone works no matter what, and the minimal home screen has an Emergency button too.",
        )
        if (missing.isNotEmpty()) {
            Callout(
                title = "${missing.joinToString(" and ")} ${if (missing.size == 1) "isn't" else "aren't"} allowed",
                kind = CalloutKind.WARNING,
                body = "That's fine if it's on purpose. To get out early you'd use the unlock steps or the Emergency button.",
            )
        }
    }
}

/** Always-available guidance: keep it short, and call out Settings as an easy way around sessions. */
@Composable
private fun AlwaysInfo(apps: List<AppEntry>, selected: Set<String>, riskyPackages: Set<String>) {
    val risky = (riskyPackages intersect selected).map { pkg -> apps.firstOrNull { it.packageName == pkg }?.label ?: pkg }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Callout(
            title = "What belongs here",
            kind = CalloutKind.INFO,
            body = "Apps you could need in the middle of anything: maps and public transport, taxis, your bank or payment app, " +
                "an authenticator, tickets and boarding passes, a health or care app. These are never blocked, and the " +
                "Emergency button reaches them during any session.",
        )
        Callout(
            title = "Keep the list short",
            kind = CalloutKind.INFO,
            body = "Everything here is a way around every session, so add what you would genuinely need in a hurry \u2014 " +
                "not what you would like to have.",
        )
        if (risky.isNotEmpty()) {
            Callout(
                title = "${risky.joinToString(" and ")} is an easy way out",
                kind = CalloutKind.WARNING,
                body = "From Settings, digital refuge can be switched off in a moment, which makes every session easy to skip.",
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupChips(
    groups: List<Pair<AppGroup, Set<String>>>,
    selected: Set<String>,
    onToggleGroup: (Set<String>) -> Unit,
    canSave: Boolean,
    onSave: () -> Unit,
) {
    val c = Obsidian.colors
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text("Groups", style = MaterialTheme.typography.labelLarge, color = c.textMuted, modifier = Modifier.padding(bottom = 8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            groups.forEach { (group, available) ->
                QuickChip("${group.name} · ${available.size}", selected = selected.containsAll(available)) { onToggleGroup(available) }
            }
            if (canSave) QuickChip("+ Save selection as group", selected = false, onClick = onSave)
        }
    }
}

@Composable
private fun SaveGroupDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    val c = Obsidian.colors
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.bgSecondary,
        title = { Text("Save as group") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Name it, and next time you can tick all these apps in one tap.", color = c.textMuted)
                ObsidianTextField(value = name, onValueChange = { name = it }, label = "Group name", placeholder = "e.g. Social")
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name.trim().ifEmpty { "My group" }) }) {
                Text("Save", color = c.accentText, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.textMuted) } },
    )
}

@Composable
private fun AppRow(app: AppEntry, checked: Boolean, onToggle: () -> Unit) {
    val c = Obsidian.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.packageName, app.label)
        Spacer(Modifier.width(16.dp))
        Text(app.label, style = MaterialTheme.typography.bodyLarge, color = c.textNormal, modifier = Modifier.weight(1f))
        Checkbox(
            checked = checked,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(checkedColor = c.accentFill, uncheckedColor = c.textFaint, checkmarkColor = c.textOnAccent),
        )
    }
}
