package app.bedtime.ui.groups

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import app.bedtime.ui.components.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import app.bedtime.data.AppGroup
import app.bedtime.data.AppSettings
import app.bedtime.data.Repository
import app.bedtime.ui.apps.AppPickerScreen
import app.bedtime.ui.apps.PickerMode
import app.bedtime.ui.components.AppIconStack
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.BottomActionBar
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.ObsidianTextField
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.OptionRow
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.pluralApps
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.launch

/** Settings → App groups: list, create, edit and delete groups. */
@Composable
fun GroupsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    var draft by remember { mutableStateOf<AppGroup?>(null) }
    var picking by remember { mutableStateOf(false) }
    val appLabel: (String) -> String = { AppCatalog.label(context, it) }

    val current = draft
    if (current != null && picking) {
        AppPickerScreen(
            title = current.name.ifBlank { "Apps in this group" },
            initial = current.packages,
            mode = PickerMode.GROUP,
            onDone = {
                draft = current.copy(packages = it)
                picking = false
            },
            onBack = { picking = false },
        )
        return
    }
    if (current != null) {
        BackHandler { draft = null }
        GroupEditContent(
            group = current,
            isNew = settings.groups.none { it.id == current.id },
            onChange = { draft = it },
            onPickApps = { picking = true },
            onSave = {
                scope.launch { repo.saveGroup(current.copy(name = current.name.trim().ifEmpty { "My group" })) }
                draft = null
            },
            onDelete = {
                scope.launch { repo.deleteGroup(current.id) }
                draft = null
            },
            onBack = { draft = null },
            appLabel = appLabel,
        )
        return
    }
    GroupsContent(
        groups = settings.groups,
        onBack = onBack,
        onOpen = { draft = it },
        onNew = { draft = AppGroup(name = "", packages = emptySet()) },
        appLabel = appLabel,
    )
}

@Composable
internal fun GroupsContent(
    groups: List<AppGroup>,
    onBack: () -> Unit,
    onOpen: (AppGroup) -> Unit,
    onNew: () -> Unit,
    appLabel: (String) -> String = { it },
) {
    val c = Obsidian.colors
    Scaffold(
        containerColor = c.bgPrimary,
        topBar = { ObsidianTopBar("App groups", onBack = onBack) },
        bottomBar = { BottomActionBar { CtaButton("New group", onClick = onNew, modifier = Modifier.fillMaxWidth()) } },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Groups let you tick many apps in one tap when choosing apps to block or allow. " +
                    "Editing a group later doesn't change schedules you've already set up.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            if (groups.isEmpty()) {
                Text("No groups yet.", style = MaterialTheme.typography.bodyLarge, color = c.textMuted)
            } else {
                SectionCard {
                    groups.forEach { group ->
                        OptionRow(
                            BedtimeIcons.Grid,
                            group.name,
                            description = pluralApps(group.packages.size),
                            onClick = { onOpen(group) },
                        ) {
                            AppIconStack(group.packages.sorted(), appLabel)
                            Chevron()
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun GroupEditContent(
    group: AppGroup,
    isNew: Boolean,
    onChange: (AppGroup) -> Unit,
    onPickApps: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    appLabel: (String) -> String = { it },
) {
    val c = Obsidian.colors
    Scaffold(
        containerColor = c.bgPrimary,
        topBar = { ObsidianTopBar(if (isNew) "New group" else "Edit group", onBack = onBack) },
        bottomBar = {
            BottomActionBar {
                CtaButton(if (isNew) "Create group" else "Save changes", onClick = onSave, modifier = Modifier.fillMaxWidth())
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard {
                ObsidianTextField(
                    value = group.name,
                    onValueChange = { onChange(group.copy(name = it)) },
                    label = "Name",
                    placeholder = "e.g. Social, Work tools, Games",
                )
                OptionRow(
                    BedtimeIcons.Grid,
                    "Apps",
                    description = if (group.packages.isEmpty()) "None yet · tap to choose" else pluralApps(group.packages.size),
                    onClick = onPickApps,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    AppIconStack(group.packages.sorted(), appLabel)
                    Chevron()
                }
            }
            if (!isNew) {
                TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text("Delete group", color = c.red, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
