package app.bedtime.ui.settings

import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.width
import app.bedtime.data.Keywords
import android.content.Intent
import android.net.Uri
import app.bedtime.apps.AppCatalog
import app.bedtime.data.FolderIndex
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.OptionRow
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Create
import app.bedtime.data.ContactSearch
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.Manifest
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.key
import androidx.compose.material.icons.filled.Menu
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.SearchChoice
import app.bedtime.data.SearchOrder
import app.bedtime.data.SearchTarget
import app.bedtime.data.SearchTargets
import app.bedtime.data.Repository
import app.bedtime.data.key
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the minimal screen's search offers after apps, and in what order. */
@Composable
fun SearchSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val found by produceState<List<SearchTarget>?>(null) { value = withContext(Dispatchers.IO) { SearchTargets.find(context) } }
    val current = settings ?: return
    val targets = found ?: return
    val obsidian = remember { AppCatalog.isLaunchable(context, FolderIndex.OBSIDIAN) }
    val choices = SearchOrder.arrange(SearchOrder.defaults(targets, current.noteTargets, obsidian), current.searchChoices)
    val vaultName by produceState<String?>(null, current.obsidianVault) {
        value = current.obsidianVault?.let { withContext(Dispatchers.IO) { FolderIndex.folderName(context, Uri.parse(it)) } }
    }
    val folderName by produceState<String?>(null, current.filesFolder) {
        value = current.filesFolder?.let { withContext(Dispatchers.IO) { FolderIndex.folderName(context, Uri.parse(it)) } }
    }
    val labels = targets.associate { it.key to it.choiceLabel() } +
        current.noteTargets.associate { SearchOrder.noteKey(it) to "Add to ${it.name}" } +
        mapOf(
            SearchOrder.REFUGE_NOTES to "Save in refuge notes",
            SearchOrder.CALENDAR to "Add to calendar, when the words say when",
            SearchOrder.CONTACTS to "Contacts: call, message, WhatsApp",
            SearchOrder.CLOCK to "Alarms and timers: \"alarm 7:30\", \"timer 10 min\"",
            SearchOrder.SETTINGS to "Settings pages",
            SearchOrder.OBSIDIAN to "Obsidian notes, by title",
            SearchOrder.FILES to "Files in a folder, by name",
        )
    val save = { updated: List<SearchChoice> -> scope.launch { repo.updateSettings { it.copy(searchChoices = updated) } } }
    // Obsidian notes and files need their folder first: switching them on without one asks for it.
    var picking by remember { mutableStateOf<String?>(null) }
    var waitingFolder by remember { mutableStateOf<List<SearchChoice>?>(null) }
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        val key = picking
        picking = null
        val updated = waitingFolder
        waitingFolder = null
        if (uri == null || key == null) return@rememberLauncherForActivityResult
        runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        scope.launch {
            repo.updateSettings {
                val withFolder = if (key == SearchOrder.OBSIDIAN) it.copy(obsidianVault = uri.toString()) else it.copy(filesFolder = uri.toString())
                // Chosen from the toggle, it's switched on too; chosen from the folder row, it stays as it was.
                if (updated != null) withFolder.copy(searchChoices = updated) else withFolder
            }
        }
    }
    val choose = { key: String, updated: List<SearchChoice>? ->
        picking = key
        waitingFolder = updated
        pickFolder.launch(null)
    }
    val folderOf = mapOf(SearchOrder.OBSIDIAN to current.obsidianVault, SearchOrder.FILES to current.filesFolder)
    // Contacts are read only once you've said so, here.
    var waitingContacts by remember { mutableStateOf<List<SearchChoice>?>(null) }
    val askContacts = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        waitingContacts?.let { updated -> if (granted) save(updated) }
        waitingContacts = null
    }
    val keywords = Keywords.effective(current.keywords, current.keywordsCustom, Keywords.defaults(choices, current.noteTargets))
    SearchSettingsContent(
        choices = choices.map { it to (labels[it.key] ?: it.key) },
        keywords = keywords,
        onKeyword = { key, typed ->
            val keyword = Keywords.clean(typed)
            val updated = if (keyword.isEmpty()) keywords - key else keywords + (key to keyword)
            scope.launch { repo.updateSettings { it.copy(keywords = updated, keywordsCustom = true) } }
        },
        onChange = { updated ->
            val turnedOn = updated.filter { it.on }.map { it.key }.toSet() - choices.filter { it.on }.map { it.key }.toSet()
            val needsFolder = turnedOn.firstOrNull { it in folderOf && folderOf[it] == null }
            if (needsFolder != null) {
                choose(needsFolder, updated)
            } else if (SearchOrder.CONTACTS in turnedOn && !ContactSearch.canRead(context)) {
                waitingContacts = updated
                askContacts.launch(Manifest.permission.READ_CONTACTS)
            } else {
                save(updated)
            }
        },
        onBack = onBack,
        folders = listOfNotNull(
            Triple(SearchOrder.OBSIDIAN, "Obsidian vault", vaultName).takeIf { obsidian },
            Triple(SearchOrder.FILES, "Folder of files", folderName),
        ),
        onFolder = { key -> choose(key, null) },
    )
}

private fun SearchTarget.choiceLabel(): String = when (kind) {
    SearchTarget.Kind.WEB -> "Search the web · $appLabel"
    SearchTarget.Kind.WIKIPEDIA -> "Wikipedia · $appLabel"
    SearchTarget.Kind.MAPS -> "Maps · $appLabel"
    SearchTarget.Kind.LOOKUP -> "Look up in $appLabel"
    SearchTarget.Kind.SPOTIFY -> "Search in $appLabel"
}

@Composable
internal fun SearchSettingsContent(
    choices: List<Pair<SearchChoice, String>>,
    onChange: (List<SearchChoice>) -> Unit,
    onBack: () -> Unit,
    keywords: Map<String, String> = emptyMap(),
    onKeyword: (key: String, keyword: String) -> Unit = { _, _ -> },
    /** The folders searched by name: their choice key, what they are, and the one chosen (null if none). */
    folders: List<Triple<String, String, String?>> = emptyList(),
    onFolder: (key: String) -> Unit = {},
) {
    val c = Obsidian.colors
    val list = choices.map { it.first }
    val labels = choices.associate { (choice, label) -> choice.key to label }
    // While a row is dragged, the order lives here; it's saved when the row is let go.
    var local by remember(list) { mutableStateOf(list) }
    var dragging by remember { mutableStateOf<String?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableIntStateOf(1) }
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Search", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Matching apps always come first. Then, in this order, whatever is switched on. Drag a row by its handle to move it; switched off, it moves below the rest. Only apps a session allows are offered during it.\n\nA keyword sends straight to one of them: type it, a space, then your words (\"m cafe\"). A sign like @ needs no space. It works even for those switched off here.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            SectionCard {
                local.forEach { choice ->
                    key(choice.key) {
                        val lifted = dragging == choice.key
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .onSizeChanged { rowHeight = it.height.coerceAtLeast(1) }
                                .zIndex(if (lifted) 1f else 0f)
                                .graphicsLayer { translationY = if (lifted) offset else 0f }
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (lifted) c.bgPrimaryAlt else Color.Transparent)
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .pointerInput(choice.key) {
                                        detectDragGestures(
                                            onDragStart = {
                                                dragging = choice.key
                                                offset = 0f
                                            },
                                            onDragEnd = {
                                                dragging = null
                                                offset = 0f
                                                onChange(SearchOrder.onFirst(local))
                                            },
                                            onDragCancel = {
                                                dragging = null
                                                offset = 0f
                                                local = list
                                            },
                                        ) { change, amount ->
                                            change.consume()
                                            offset += amount.y
                                            // Past half a row, it trades places with its neighbour.
                                            val at = local.indexOfFirst { it.key == choice.key }
                                            if (offset > rowHeight / 2f && at < local.lastIndex) {
                                                local = SearchOrder.move(local, at, at + 1)
                                                offset -= rowHeight
                                            } else if (offset < -rowHeight / 2f && at > 0) {
                                                local = SearchOrder.move(local, at, at - 1)
                                                offset += rowHeight
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Default.Menu, contentDescription = "drag to move", tint = c.textFaint, modifier = Modifier.size(20.dp)) }
                            Text(
                                labels[choice.key] ?: choice.key,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (choice.on) c.textNormal else c.textFaint,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            )
                            KeywordBox(keywords[choice.key].orEmpty()) { onKeyword(choice.key, it) }
                            Spacer(Modifier.width(8.dp))
                            ObsidianToggle(choice.on, { on -> onChange(SearchOrder.switched(local, choice.key, on)) })
                        }
                    }
                }
            }
            if (folders.isNotEmpty()) {
                SectionCard(title = "Folders", subtitle = "Only names are read, to match what you type; nothing inside the files.") {
                    folders.forEach { (key, title, chosen) ->
                        OptionRow(
                            icon = if (key == SearchOrder.OBSIDIAN) Icons.Default.Create else Icons.AutoMirrored.Filled.List,
                            title = title,
                            description = chosen ?: "None chosen yet",
                            onClick = { onFolder(key) },
                            trailing = { Chevron() },
                        )
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** A short box for a keyword, saved as you type; empty means none. */
@Composable
private fun KeywordBox(keyword: String, onChange: (String) -> Unit) {
    val c = Obsidian.colors
    var text by remember(keyword) { mutableStateOf(keyword) }
    BasicTextField(
        value = text,
        onValueChange = {
            text = Keywords.clean(it)
            onChange(text)
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = c.accentText, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(c.accent),
        modifier = Modifier
            .width(48.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, c.border, RoundedCornerShape(8.dp))
            .padding(vertical = 8.dp, horizontal = 4.dp),
        decorationBox = { field ->
            Box(contentAlignment = Alignment.Center) {
                if (text.isEmpty()) Text("–", style = MaterialTheme.typography.bodyMedium, color = c.textFaint)
                field()
            }
        },
    )
}
