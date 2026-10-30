package app.bedtime.ui.settings

import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.layout.heightIn

import androidx.compose.material.icons.filled.ArrowDropDown

import androidx.compose.material3.DropdownMenuItem

import androidx.compose.material3.DropdownMenu

import android.widget.Toast

import androidx.compose.material.icons.filled.Lock

import androidx.compose.material.icons.filled.Info

import app.bedtime.data.StarDicts

import androidx.compose.foundation.layout.fillMaxWidth

import androidx.compose.ui.focus.focusRequester

import androidx.compose.runtime.saveable.rememberSaveable

import app.bedtime.ui.components.LocalSettingsSearch

import androidx.compose.runtime.CompositionLocalProvider

import androidx.activity.compose.BackHandler

import androidx.compose.material.icons.filled.DateRange

import androidx.compose.runtime.LaunchedEffect

import app.bedtime.ui.minimal.CalendarSource

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
import androidx.compose.material.icons.filled.Call
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
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import app.bedtime.ui.components.ObsidianTextField
import app.bedtime.data.SearchMode
import app.bedtime.data.SearchCategory
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
    // Which calendar "add to calendar" writes to: asked for only when wanted.
    var calendars by remember { mutableStateOf(emptyList<Pair<Long, String>>()) }
    var calendarAccess by remember { mutableStateOf(CalendarSource.hasPermission(context) && CalendarSource.canWrite(context)) }
    LaunchedEffect(calendarAccess) { calendars = withContext(Dispatchers.IO) { CalendarSource.writableCalendars(context) } }
    val askCalendar = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { granted ->
        calendarAccess = granted.values.all { it }
    }
    val current = settings ?: return
    val targets = found ?: return
    val obsidian = remember { AppCatalog.isLaunchable(context, FolderIndex.OBSIDIAN) }
    val arranged = SearchOrder.arrange(SearchOrder.defaults(targets, current.quickTargets, obsidian), current.searchChoices)
    val keywords = Keywords.live(
        Keywords.effective(current.keywords, current.keywordsCustom, Keywords.defaults(arranged, current.quickTargets)),
        arranged.map { it.key }.toSet(),
    ) { AppCatalog.isLaunchable(context, it) }
    // Keywords left by what's gone (an uninstalled app's look-up, say) are cleared for good.
    val stale = if (current.keywordsCustom) current.keywords.keys - keywords.keys else emptySet()
    LaunchedEffect(stale) {
        if (stale.isNotEmpty()) repo.updateSettings { it.copy(keywords = it.keywords - stale) }
    }
    // Each in every search, only by its keyword, or off; saved before that was so, an off one with a keyword is keyword-only.
    val choices = SearchOrder.withModes(arranged, keywords, current.searchModesSet)
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
            SearchOrder.SHORTCUTS to "Apps' shortcuts, by name: \"sync now\"",
            SearchOrder.DICTIONARY to "A word's meaning, from your dictionaries",
            SearchOrder.ALARM to "Alarms: \"alarm 7:30\"",
            SearchOrder.TIMER to "Timers: \"timer 10 min\"",
            SearchOrder.SETTINGS to "Settings pages",
            SearchOrder.OBSIDIAN to "Obsidian notes, by title",
            SearchOrder.FILES to "Files in your folders, by name",
        )
    val save = { updated: List<SearchChoice> -> scope.launch { repo.updateSettings { it.copy(searchChoices = updated, searchModesSet = true) } } }
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
                if (updated != null) withFolder.copy(searchChoices = updated, searchModesSet = true) else withFolder
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
    val chatApps = remember { ContactSearch.chatApps(context) }
    // Dictionaries: copied in from a folder you pick, then listed from the app's own storage.
    var dictionariesChanged by remember { mutableStateOf(0) }
    val dictionaries by produceState(emptyList<String>(), dictionariesChanged) { value = withContext(Dispatchers.IO) { StarDicts.names(context) } }
    val pickDictionaries = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val added = withContext(Dispatchers.IO) { StarDicts.import(context, uri) }
            Toast.makeText(context, if (added == 0) "no StarDict dictionary in that folder" else if (added == 1) "1 dictionary added" else "$added dictionaries added", Toast.LENGTH_SHORT).show()
            dictionariesChanged++
        }
    }
    val apps by produceState(emptyList<app.bedtime.apps.AppEntry>()) {
        value = withContext(Dispatchers.IO) { AppCatalog.launchableApps(context).sortedBy { it.label.lowercase() } }
    }
    SearchSettingsContent(
        apps = apps,
        dictionaries = dictionaries,
        onAddDictionaries = { pickDictionaries.launch(null) },
        onRemoveDictionary = { name ->
            scope.launch {
                withContext(Dispatchers.IO) { StarDicts.remove(context, name) }
                dictionariesChanged++
            }
        },
        lookUpNotes = current.lookUpNotes,
        onLookUpNotes = { on -> scope.launch { repo.updateSettings { it.copy(lookUpNotes = on) } } },
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
        categoriesOff = current.searchOff,
        onCategory = { category, on -> scope.launch { repo.updateSettings { it.copy(searchOff = if (on) it.searchOff - category else it.searchOff + category) } } },
        calendars = calendars,
        calendarId = current.eventCalendarId,
        calendarAccess = calendarAccess,
        onCalendar = { id -> scope.launch { repo.updateSettings { it.copy(eventCalendarId = id) } } },
        onAllowCalendar = { askCalendar.launch(arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)) },
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
    /** Kinds of action switched off whole, and the switch for each. */
    categoriesOff: Set<SearchCategory> = emptySet(),
    onCategory: (SearchCategory, Boolean) -> Unit = { _, _ -> },
    /** The calendars that can take new events, the one "add to calendar" uses, and whether it may write. */
    calendars: List<Pair<Long, String>> = emptyList(),
    calendarId: Long? = null,
    calendarAccess: Boolean = false,
    onCalendar: (Long) -> Unit = {},
    onAllowCalendar: () -> Unit = {},
    /** Your apps, found by the page's own search to give one a keyword. */
    apps: List<app.bedtime.apps.AppEntry> = emptyList(),
    /** Your dictionaries by name, adding and taking away; and whether the lock screen's look-up finds notes. */
    dictionaries: List<String> = emptyList(),
    onAddDictionaries: () -> Unit = {},
    onRemoveDictionary: (String) -> Unit = {},
    lookUpNotes: Boolean = false,
    onLookUpNotes: (Boolean) -> Unit = {},
) {
    val c = Obsidian.colors
    val list = choices.map { it.first }
    // The page's own search: null when closed. It finds this page's actions, and apps to give a keyword.
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = filter != null) { filter = null }
    val labels = choices.associate { (choice, label) -> choice.key to label }
    var conflict by remember { mutableStateOf<Pair<String, String>?>(null) }
    // The kind whose unused actions are being chosen from, if any.
    var adding by remember { mutableStateOf<SearchCategory?>(null) }
    conflict?.let { (keyword, owner) ->
        AlertDialog(
            onDismissRequest = { conflict = null },
            containerColor = c.bgSecondary,
            title = { Text("“$keyword” is taken") },
            text = { Text("It's already the keyword for $owner. Pick another, or change that one first.", color = c.textMuted) },
            confirmButton = { TextButton(onClick = { conflict = null }) { Text("OK", color = c.accentText) } },
        )
    }
    // Here the icon at the top searches this page, not every setting.
    CompositionLocalProvider(LocalSettingsSearch provides { filter = "" }) {
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Search", onBack = { if (filter != null) filter = null else onBack() }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val query = filter
            if (query != null) {
                PageFilter(query) { filter = it }
                val found = list.filter { SearchOrder.matches(it.key, labels[it.key] ?: it.key, query) }
                val foundApps = if (query.trim().length < 2) emptyList() else apps.filter { it.label.contains(query.trim(), ignoreCase = true) }.take(8)
                if (query.isNotBlank() && found.isEmpty() && foundApps.isEmpty()) {
                    Text("Nothing by that name.", style = MaterialTheme.typography.bodyMedium, color = c.textFaint)
                }
                if (found.isNotEmpty()) {
                    SectionCard(title = "Actions") {
                        found.forEachIndexed { i, choice ->
                            if (i > 0) Spacer(Modifier.height(12.dp))
                            ChoiceRow(choice, labels[choice.key] ?: choice.key, keywords[choice.key].orEmpty(), onKeyword = { typed -> onKeyword(choice.key, typed)?.let { owner -> conflict = Keywords.clean(typed) to owner } }) { mode ->
                                if (choice.mode != mode) onChange(SearchOrder.setMode(list, choice.key, mode))
                            }
                        }
                    }
                }
                if (foundApps.isNotEmpty()) {
                    SectionCard(title = "Apps", subtitle = "A keyword typed alone in the search opens it") {
                        foundApps.forEach { entry ->
                            val key = Keywords.appKey(entry.packageName)
                            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                                app.bedtime.ui.components.VerbatimText(entry.label, style = MaterialTheme.typography.bodyLarge, color = c.textNormal, modifier = Modifier.weight(1f))
                                KeywordBox(keywords[key].orEmpty()) { typed -> onKeyword(key, typed)?.let { owner -> conflict = Keywords.clean(typed) to owner } }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
                return@Column
            }
            Text(
                "Matching apps always come first; then what's found, what you can do, and where to look your words up, in that order. Each action is in every search, only with its keyword (type it, a space, then your words: \"m cafe\"), or off. Only apps a session allows are offered during it.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            Text(SearchOrder.summary(list), style = MaterialTheme.typography.labelLarge, color = c.accentText)
            TryIt(list, labels, keywords, categoriesOff)
            SearchCategory.entries.forEach { category ->
                val inCategory = list.filter { SearchOrder.category(it.key) == category }
                if (inCategory.isEmpty()) return@forEach
                val on = category !in categoriesOff
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(category.title, style = MaterialTheme.typography.titleMedium, color = if (on) c.textNormal else c.textFaint)
                            Text(category.hint, style = MaterialTheme.typography.bodySmall, color = c.textFaint)
                        }
                        ObsidianToggle(on, { onCategory(category, it) })
                    }
                    if (!on) {
                        Text(
                            "Switched off: none of these show, not even by keyword. Each keeps its own setting for when it's back.",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textFaint,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    } else {
                        // Only what's in use shows; what's off waits behind "Add more", to keep this short.
                        val used = inCategory.filter { it.mode != SearchMode.OFF }
                        val unused = inCategory.filter { it.mode == SearchMode.OFF }
                        Spacer(Modifier.height(8.dp))
                        DragList(used, key = { it.key }, onMoved = { moved -> onChange(list.filterNot { SearchOrder.category(it.key) == category } + moved + unused) }) { choice ->
                            ChoiceRow(
                                choice,
                                labels[choice.key] ?: choice.key,
                                keywords[choice.key].orEmpty(),
                                onKeyword = { typed -> onKeyword(choice.key, typed)?.let { owner -> conflict = Keywords.clean(typed) to owner } },
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            ) { mode -> if (choice.mode != mode) onChange(SearchOrder.setMode(list, choice.key, mode)) }
                        }
                        if (unused.isNotEmpty()) {
                            OptionRow(Icons.Default.Add, "Add more", description = "${unused.size} more to choose from", onClick = { adding = category }) { Chevron() }
                        }
                    }
                }
            }
            adding?.let { category ->
                AddMoreDialog(
                    category,
                    list.filter { SearchOrder.category(it.key) == category && it.mode == SearchMode.OFF }.map { it.key to (labels[it.key] ?: it.key) },
                    onDismiss = { adding = null },
                ) { key ->
                    adding = null
                    onChange(SearchOrder.setMode(list, key, SearchMode.EVERY))
                }
            }
            if (list.any { it.key == SearchOrder.CONTACTS && it.on } && SearchCategory.FIND !in categoriesOff && chatApps.size > 1) {
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
            if (list.any { it.key == SearchOrder.CALENDAR && it.on } && SearchCategory.DO !in categoriesOff) {
                SectionCard(title = "Adding to your calendar", subtitle = "“dentist next friday 9” offers to add it, to this calendar") {
                    if (!calendarAccess) {
                        OptionRow(Icons.Default.DateRange, "Allow calendar access", description = "Asked anyway the first time you add one", onClick = onAllowCalendar) { Chevron() }
                    } else if (calendars.isEmpty()) {
                        Text("No calendar on this phone can take new events.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                    } else {
                        // One line for the calendar in use; the others in a menu under it.
                        val chosen = calendarId?.takeIf { id -> calendars.any { it.first == id } } ?: calendars.first().first
                        var choosing by remember { mutableStateOf(false) }
                        Box {
                            OptionRow(
                                Icons.Default.DateRange,
                                calendars.first { it.first == chosen }.second,
                                description = if (calendars.size > 1) "Tap to choose another" else null,
                                onClick = if (calendars.size > 1) ({ choosing = true }) else null,
                            ) { if (calendars.size > 1) Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = c.textFaint) }
                            DropdownMenu(expanded = choosing, onDismissRequest = { choosing = false }, containerColor = c.bgSecondary) {
                                calendars.forEach { (id, name) ->
                                    DropdownMenuItem(
                                        text = { Text(name, color = if (id == chosen) c.accentText else c.textNormal) },
                                        onClick = {
                                            choosing = false
                                            onCalendar(id)
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            SectionCard(
                title = "Look up",
                subtitle = "A word's meaning from your own dictionaries, read on this phone, nothing sent anywhere",
            ) {
                Text(
                    LOOK_UP_HOW,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textMuted,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                if (dictionaries.isEmpty()) {
                    Text("No dictionary yet: add one below to start.", style = MaterialTheme.typography.bodySmall, color = c.accentText, modifier = Modifier.padding(bottom = 4.dp))
                }
                dictionaries.forEach { name ->
                    OptionRow(Icons.Default.Info, name, description = "Read on this phone") {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "take this dictionary away",
                            tint = c.textFaint,
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(50)).clickable { onRemoveDictionary(name) }.padding(8.dp),
                        )
                    }
                }
                OptionRow(
                    Icons.Default.Add,
                    "Add dictionaries from a folder",
                    description = "StarDict files (.ifo, .idx and .dict or .dict.dz), free for many languages; copied in, so the folder isn't needed after",
                    onClick = onAddDictionaries,
                ) { Chevron() }
                OptionRow(
                    Icons.Default.Lock,
                    "Notes on the lock screen too",
                    description = "Found by their titles; a note opens only once you've unlocked, so what's in it never shows there",
                ) { ObsidianToggle(lookUpNotes, onLookUpNotes) }
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
}
}

/**
 * What's off in one kind, to add to the search: each with its app's icon, or a sign for refuge's own,
 * and a box to find one by name. A pick is added to every search.
 */
@Composable
private fun AddMoreDialog(category: SearchCategory, unused: List<Pair<String, String>>, onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    val c = Obsidian.colors
    var query by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.bgSecondary,
        title = { Text("Add to ${category.title.lowercase()}") },
        text = {
            Column {
                ObsidianTextField(value = query, onValueChange = { query = it }, placeholder = "Find one", modifier = Modifier.fillMaxWidth())
                val shown = unused.filter { (key, label) -> query.isBlank() || SearchOrder.matches(key, label, query) }
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()).padding(top = 8.dp)) {
                    if (shown.isEmpty()) Text("Nothing by that name.", style = MaterialTheme.typography.bodyMedium, color = c.textFaint, modifier = Modifier.padding(vertical = 12.dp))
                    shown.forEach { (key, label) ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onAdd(key) }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val pkg = appOf(key)
                            if (pkg != null) {
                                app.bedtime.ui.components.AppIcon(pkg, label, size = 28.dp)
                            } else {
                                Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                                    Icon(signOf(key), contentDescription = null, tint = c.accent, modifier = Modifier.size(22.dp))
                                }
                            }
                            Text(label, style = MaterialTheme.typography.bodyLarge, color = c.textNormal, modifier = Modifier.weight(1f).padding(start = 14.dp))
                            Icon(Icons.Default.Add, contentDescription = "add", tint = c.textFaint, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close", color = c.textMuted) } },
    )
}

/** The app an action sends your words to, by its key ("maps:<package>"), or Obsidian; null for refuge's own. */
private fun appOf(key: String): String? = when {
    key == SearchOrder.OBSIDIAN -> FolderIndex.OBSIDIAN
    listOf("web:", "wikipedia:", "maps:", "lookup:", "spotify:").any { key.startsWith(it) } -> key.substringAfter(':')
    else -> null
}

/** A sign for one of refuge's own actions. */
private fun signOf(key: String): androidx.compose.ui.graphics.vector.ImageVector = when {
    key.startsWith("note:") -> Icons.Default.Edit
    key == SearchOrder.CALENDAR -> Icons.Default.DateRange
    key == SearchOrder.CONTACTS -> Icons.Default.Person
    key == SearchOrder.ALARM -> Icons.Default.Notifications
    key == SearchOrder.TIMER -> Icons.Default.PlayArrow
    key == SearchOrder.SETTINGS -> Icons.Default.Settings
    key == SearchOrder.BLOCKS -> Icons.Default.Lock
    key == SearchOrder.SHORTCUTS -> Icons.Default.Star
    key == SearchOrder.DICTIONARY -> Icons.Default.Info
    key == SearchOrder.FILES -> Icons.AutoMirrored.Filled.List
    else -> Icons.Default.Add
}

/** How the look-up is used, step by step, on the search page. */
private const val LOOK_UP_HOW =
    "1. Add a dictionary: StarDict files (FreeDict and WordNet have free ones), copied from a folder.\n" +
        "2. In the search, type a word: its meaning shows under “Look up”, in every search or only after its keyword (set above). Tap it for the whole entry.\n" +
        "3. Without unlocking: set a lock-screen gesture to “look up” (settings › lock screen › gestures), then type a word. Tap any word in an entry to look that up too; “timer 10” starts a timer.\n" +
        "4. With several dictionaries, the chips at the top of the look-up pick one, or all."

/** One action: its name and keyword, and whether it's in every search, only by its keyword, or off. */
@Composable
private fun ChoiceRow(choice: SearchChoice, label: String, keyword: String, onKeyword: (String) -> Unit, modifier: Modifier = Modifier, onMode: (SearchMode) -> Unit) {
    val c = Obsidian.colors
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = if (choice.mode == SearchMode.OFF) c.textFaint else c.textNormal, modifier = Modifier.weight(1f))
            if (choice.mode != SearchMode.OFF) KeywordBox(keyword, onKeyword)
        }
        Row(Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf(SearchMode.EVERY to "Every search", SearchMode.KEYWORD to "Keyword only", SearchMode.OFF to "Off").forEach { (mode, name) ->
                ModeChip(name, choice.mode == mode) { onMode(mode) }
            }
        }
        if (choice.mode == SearchMode.KEYWORD && keyword.isEmpty()) {
            Text("Give it a keyword, or nothing reaches it", style = MaterialTheme.typography.labelSmall, color = c.accentText, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/** This page's own search: what it offers, and apps, by name. */
@Composable
private fun PageFilter(query: String, onQuery: (String) -> Unit) {
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    ObsidianTextField(
        value = query,
        onValueChange = onQuery,
        placeholder = "An action, like dictionary, or an app",
        modifier = Modifier.fillMaxWidth().focusRequester(focus),
    )
}

/** One of an action's three states, as a small chip; the chosen one filled. */
@Composable
private fun ModeChip(label: String, chosen: Boolean, onClick: () -> Unit) {
    val c = Obsidian.colors
    val shape = RoundedCornerShape(50)
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = if (chosen) c.bgPrimary else c.textMuted,
        maxLines = 1,
        modifier = Modifier
            .clip(shape)
            .background(if (chosen) c.accent else Color.Transparent)
            .border(1.dp, if (chosen) c.accent else c.border, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/**
 * Type as you would in the search and see what it would offer: the one place a keyword sends it, or,
 * after matching apps, the actions in every search, where they have something for it.
 */
@Composable
private fun TryIt(list: List<SearchChoice>, labels: Map<String, String>, keywords: Map<String, String>, off: Set<SearchCategory>) {
    val c = Obsidian.colors
    var text by remember { mutableStateOf("") }
    SectionCard(title = "Try it") {
        ObsidianTextField(value = text, onValueChange = { text = it }, placeholder = "anna, t 10, m cafe…")
        if (text.isBlank()) return@SectionCard
        val usable = SearchOrder.usable(list, off)
        val hit = Keywords.match(text, keywords.filterKeys { key -> usable.any { it.key == key } })
        Spacer(Modifier.height(10.dp))
        if (hit != null) {
            Text("By its keyword, only: ${labels[hit.key] ?: hit.key}, with “${hit.words}”", style = MaterialTheme.typography.bodyMedium, color = c.textNormal)
        } else {
            val every = usable.filterNot { it.keywordOnly }
            Text("Apps whose name matches “${text.trim()}”, then, where they have something for it:", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
            every.forEach { choice ->
                Text("· ${labels[choice.key] ?: choice.key}", style = MaterialTheme.typography.bodyMedium, color = c.textNormal, modifier = Modifier.padding(top = 2.dp))
            }
            if (every.isEmpty()) Text("· nothing else", style = MaterialTheme.typography.bodyMedium, color = c.textFaint)
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
