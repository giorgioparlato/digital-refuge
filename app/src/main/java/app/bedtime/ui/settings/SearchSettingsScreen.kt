package app.bedtime.ui.settings

import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.width
import app.bedtime.data.Keywords
import app.bedtime.data.ChatApp
import app.bedtime.ui.components.DragList
import app.bedtime.ui.components.SectionLabel
import app.bedtime.ui.components.CardRadius
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Call
import androidx.compose.ui.window.Dialog
import android.content.Intent
import android.net.Uri
import app.bedtime.apps.AppCatalog
import app.bedtime.data.FolderIndex
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.OptionRow
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.foundation.clickable
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
    val choices = SearchOrder.arrange(SearchOrder.defaults(targets, current.quickTargets, obsidian), current.searchChoices)
    val vaultName by produceState<String?>(null, current.obsidianVault) {
        value = current.obsidianVault?.let { withContext(Dispatchers.IO) { FolderIndex.folderName(context, Uri.parse(it)) } }
    }
    val folderNames by produceState(emptyList<String>(), current.fileFolders) {
        value = withContext(Dispatchers.IO) { current.fileFolders.map { FolderIndex.folderName(context, Uri.parse(it)) } }
    }
    val labels = targets.associate { it.key to it.choiceLabel() } +
        current.quickTargets.associate { SearchOrder.noteKey(it) to "Add to ${it.name}" } +
        mapOf(
            SearchOrder.REFUGE_NOTES to "Save in refuge notes",
            SearchOrder.CALENDAR to "Add to calendar, when the words say when",
            SearchOrder.CONTACTS to "Contacts: call and message",
            SearchOrder.CALCULATOR to "Calculator: \"12 * 3\"",
            SearchOrder.BLOCKS to "Start a block, by its name",
            SearchOrder.ALARM to "Alarms: \"alarm 7:30\"",
            SearchOrder.TIMER to "Timers: \"timer 10 min\"",
            SearchOrder.SETTINGS to "Settings pages",
            SearchOrder.OBSIDIAN to "Obsidian notes, by title",
            SearchOrder.FILES to "Files in your folders, by name",
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
                val withFolder = if (key == SearchOrder.OBSIDIAN) {
                    it.copy(obsidianVault = uri.toString())
                } else {
                    it.copy(filesFolders = (it.fileFolders + uri.toString()).distinct(), filesFolder = null)
                }
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
    val folderOf = mapOf(SearchOrder.OBSIDIAN to current.obsidianVault, SearchOrder.FILES to current.fileFolders.firstOrNull())
    // Contacts are read only once you've said so, here.
    var waitingContacts by remember { mutableStateOf<List<SearchChoice>?>(null) }
    val askContacts = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        waitingContacts?.let { updated -> if (granted) save(updated) }
        waitingContacts = null
    }
    val keywords = Keywords.effective(current.keywords, current.keywordsCustom, Keywords.defaults(choices, current.quickTargets))
    val chatApps = remember { ContactSearch.chatApps(context) }
    SearchSettingsContent(
        choices = choices.map { it to (labels[it.key] ?: it.key) },
        keywords = keywords,
        onKeyword = { key, typed ->
            val keyword = Keywords.clean(typed)
            // A keyword already in use isn't taken: you're told what has it instead.
            val taken = Keywords.takenBy(keywords, key, keyword)
            if (taken != null) {
                taken.removePrefix(Keywords.APP).takeIf { taken.startsWith(Keywords.APP) }?.let { AppCatalog.label(context, it) } ?: labels[taken] ?: taken
            } else {
                val updated = if (keyword.isEmpty()) keywords - key else keywords + (key to keyword)
                scope.launch { repo.updateSettings { it.copy(keywords = updated, keywordsCustom = true) } }
                null
            }
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
        folders = listOfNotNull(Triple(SearchOrder.OBSIDIAN, "Obsidian vault", vaultName).takeIf { obsidian }) +
            folderNames.mapIndexed { i, name -> Triple("${SearchOrder.FILES}:$i", "Folder of files", name) },
        onFolder = { key -> if (key == SearchOrder.OBSIDIAN || key == SearchOrder.FILES) choose(key, null) },
        chatApps = chatApps,
        chosenChats = ChatApp.chosen(current.contactApps, chatApps),
        onChats = { chosen -> scope.launch { repo.updateSettings { it.copy(contactApps = chosen.map { app -> app.key }) } } },
        onRemoveFolder = { key ->
            val at = key.substringAfter(':').toIntOrNull()
            if (at != null) scope.launch {
                repo.updateSettings { s -> s.copy(filesFolders = s.fileFolders.filterIndexed { i, _ -> i != at }, filesFolder = null) }
            }
        },
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
    /** Sets [key]'s keyword; returns what already has it instead, if anything, and then nothing changes. */
    onKeyword: (key: String, keyword: String) -> String? = { _, _ -> null },
    /** The folders searched by name: their choice key, what they are, and the one chosen (null if none). */
    folders: List<Triple<String, String, String?>> = emptyList(),
    onFolder: (key: String) -> Unit = {},
    /** Takes away one of the folders of files ("files:<n>"). */
    onRemoveFolder: (key: String) -> Unit = {},
    /** The messaging apps on the phone, those beside contacts first, in their order. */
    chatApps: List<ChatApp> = emptyList(),
    chosenChats: List<ChatApp> = emptyList(),
    onChats: (List<ChatApp>) -> Unit = {},
) {
    val c = Obsidian.colors
    val list = choices.map { it.first }
    val labels = choices.associate { (choice, label) -> choice.key to label }
    val added = list.filter { it.on }
    val left = list.filterNot { it.on }
    var adding by remember { mutableStateOf(false) }
    var conflict by remember { mutableStateOf<Pair<String, String>?>(null) }
    conflict?.let { (keyword, owner) ->
        AlertDialog(
            onDismissRequest = { conflict = null },
            containerColor = c.bgSecondary,
            title = { Text("“$keyword” is taken") },
            text = { Text("It's already the keyword for $owner. Pick another, or change that one first.", color = c.textMuted) },
            confirmButton = { TextButton(onClick = { conflict = null }) { Text("OK", color = c.accentText) } },
        )
    }
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Search", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Matching apps always come first. Then what you've added here, in this order; drag a row by its handle to move it. Only apps a session allows are offered during it.\n\nA keyword sends straight to one of them: type it, a space, then your words (\"m cafe\"). A sign like @ needs no space.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            SectionCard {
                DragList(added, key = { it.key }, onMoved = { moved -> onChange(moved + left) }) { choice ->
                    Text(
                        labels[choice.key] ?: choice.key,
                        style = MaterialTheme.typography.bodyLarge,
                        color = c.textNormal,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )
                    KeywordBox(keywords[choice.key].orEmpty()) { typed -> onKeyword(choice.key, typed)?.let { owner -> conflict = Keywords.clean(typed) to owner } }
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "take it out of the search",
                        tint = c.textFaint,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(50)).clickable { onChange(SearchOrder.switched(list, choice.key, false)) }.padding(10.dp),
                    )
                }
                if (added.isEmpty()) Text("Only apps, for now.", style = MaterialTheme.typography.bodyMedium, color = c.textFaint, modifier = Modifier.padding(8.dp))
                if (left.isNotEmpty()) {
                    OptionRow(Icons.Default.Add, "Add to the search", description = "${left.size} more to choose from", onClick = { adding = true }, trailing = { Chevron() })
                }
            }
            if (added.any { it.key == SearchOrder.CONTACTS } && chatApps.size > 1) {
                SectionCard(title = "Beside each contact", subtitle = "Up to ${ChatApp.MAX} ways to message them, in the order you pick them") {
                    chatApps.forEach { app ->
                        val on = app in chosenChats
                        OptionRow(Icons.Default.Call, app.label.replaceFirstChar { it.uppercase() }, description = if (app.takesText) null else "A message typed after their name is copied, to paste") {
                            ObsidianToggle(
                                on,
                                { now -> onChats(if (now) chosenChats + app else chosenChats - app) },
                                enabled = on || chosenChats.size < ChatApp.MAX,
                            )
                        }
                    }
                }
            }
            SectionCard(title = "Folders", subtitle = "Only names are read, to match what you type; nothing inside the files.") {
                folders.forEach { (key, title, chosen) ->
                    val removable = key.startsWith("${SearchOrder.FILES}:")
                    OptionRow(
                        icon = if (key == SearchOrder.OBSIDIAN) Icons.Default.Create else Icons.AutoMirrored.Filled.List,
                        title = title,
                        description = chosen ?: "None chosen yet",
                        onClick = if (removable) null else ({ onFolder(key) }),
                        trailing = {
                            if (removable) {
                                Icon(
                                    Icons.Default.Clear,
                                    contentDescription = "take this folder away",
                                    tint = c.textFaint,
                                    modifier = Modifier.size(36.dp).clip(RoundedCornerShape(50)).clickable { onRemoveFolder(key) }.padding(8.dp),
                                )
                            } else {
                                Chevron()
                            }
                        },
                    )
                }
                OptionRow(Icons.Default.Add, "Add a folder of files", onClick = { onFolder(SearchOrder.FILES) }, trailing = { Chevron() })
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (adding) {
        Dialog(onDismissRequest = { adding = false }) {
            AddToSearchContent(left.map { it.key to (labels[it.key] ?: it.key) }) { key ->
                adding = false
                onChange(SearchOrder.switched(list, key, true))
            }
        }
    }
}

/** What could still be added to the search, under its heading; a tap adds it at the end of the list. */
@Composable
internal fun AddToSearchContent(choices: List<Pair<String, String>>, onAdd: (String) -> Unit) {
    val c = Obsidian.colors
    Column(
        Modifier
            .fillMaxWidth()
            .heightIn(max = 560.dp)
            .clip(RoundedCornerShape(CardRadius))
            .background(c.bgSecondary)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text("Add to the search", style = MaterialTheme.typography.titleMedium, color = c.textNormal)
        val grouped = choices.groupBy { (key, _) -> SearchOrder.group(key) }
        SearchOrder.GROUPS.filter { it in grouped }.forEach { group ->
            SectionLabel(group)
            grouped.getValue(group).forEach { (key, label) ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onAdd(key) }.padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(label, style = MaterialTheme.typography.bodyLarge, color = c.textNormal)
                }
            }
        }
    }
}

/**
 * A short box for a keyword; empty means none. It's kept once you're done with it (Done, or moving
 * on), so a keyword on its way to another ("w" before "wp") isn't checked halfway; if it isn't kept,
 * the box goes back to what it was.
 */
@Composable
private fun KeywordBox(keyword: String, onDone: (String) -> Unit) {
    val c = Obsidian.colors
    var text by remember(keyword) { mutableStateOf(keyword) }
    var focused by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    fun commit() {
        if (text != keyword) onDone(text)
        text = keyword
    }
    BasicTextField(
        value = text,
        onValueChange = { text = Keywords.clean(it) },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = c.accentText, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(c.accent),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        modifier = Modifier
            .width(48.dp)
            .onFocusChanged { state ->
                if (focused && !state.isFocused) commit()
                focused = state.isFocused
            }
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
