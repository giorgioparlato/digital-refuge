package app.bedtime.ui.minimal

import androidx.compose.foundation.lazy.itemsIndexed
import app.bedtime.data.Keywords
import app.bedtime.data.ChatApp
import androidx.compose.material.icons.filled.Add
import app.bedtime.data.ClockAction
import app.bedtime.data.ClockActions
import app.bedtime.data.FolderIndex
import app.bedtime.data.IndexedFile
import app.bedtime.data.SettingsPage
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Call
import app.bedtime.data.ContactHit
import app.bedtime.data.key
import app.bedtime.data.SearchOrder
import app.bedtime.data.SearchChoice
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bedtime.apps.AppEntry
import app.bedtime.data.HomeStyle
import app.bedtime.data.NoteTarget
import app.bedtime.data.ParsedEvent
import app.bedtime.data.SearchTarget
import app.bedtime.ui.components.AppIcon
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.VerbatimText
import app.bedtime.ui.formatTime
import app.bedtime.ui.homestyle.HomePalette
import app.bedtime.ui.homestyle.palette
import app.bedtime.ui.theme.AppFonts
import app.bedtime.ui.theme.WithFontFamily
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** One line in the search's results. */
sealed interface DrawerResult {
    data class App(val app: AppEntry) : DrawerResult
    data class Search(val target: SearchTarget) : DrawerResult
    /** A quick note, to one of your files, or to the app's own notes when [target] is null. */
    data class AddNote(val target: NoteTarget?) : DrawerResult
    data class AddEvent(val event: ParsedEvent) : DrawerResult
    data object OpenNotes : DrawerResult
    /**
     * A contact: tapped, the phone app with their number. Its buttons are [Chat]s, carrying [message]
     * when the words went on past their name ("@anna running late").
     */
    data class Contact(val hit: ContactHit, val message: String = "") : DrawerResult
    data class Chat(val hit: ContactHit, val app: ChatApp, val text: String) : DrawerResult
    /** The answer to a sum, copied when tapped. */
    data class Calculation(val answer: String) : DrawerResult
    data class Clock(val action: ClockAction) : DrawerResult
    data class Setting(val page: SettingsPage) : DrawerResult
    /** A note in your Obsidian vault, found by its title. */
    data class VaultNote(val file: IndexedFile) : DrawerResult
    /** A file in the folder you chose, found by its name. */
    data class File(val file: IndexedFile) : DrawerResult
}

object AppDrawer {
    /** The apps A–Z under their first letter; anything not starting with a letter goes under "#", last. */
    fun sections(apps: List<AppEntry>): List<Pair<String, List<AppEntry>>> =
        apps.sortedBy { it.label.lowercase() }
            .groupBy { app -> app.label.firstOrNull()?.takeIf { it.isLetter() }?.uppercase() ?: "#" }
            .toList()
            .sortedBy { (letter, _) -> if (letter == "#") "￿" else letter }

    /**
     * What typing [query] offers, in order: apps whose name matches (those starting with it first), the
     * places to search, opening your notes, a quick note to each of your files and to the app's notes,
     * and an event if the words say when.
     */
    fun results(
        query: String,
        apps: List<AppEntry>,
        targets: List<SearchTarget>,
        noteTargets: List<NoteTarget>,
        event: ParsedEvent?,
        /** Your choices, in your order; the defaults when null. */
        choices: List<SearchChoice>? = null,
        /** Contacts matching the words, looked up only when contacts are switched on. */
        contacts: List<ContactHit> = emptyList(),
        /** A keyword typed first: then only its one destination is offered, with the words after it. */
        keyword: Keywords.Hit? = null,
        /** What else the words found, by choice key: an alarm, settings pages, notes and files by name. */
        found: Map<String, List<DrawerResult>> = emptyMap(),
        /** What the words say after a contact's name, for their messaging buttons. */
        message: String = "",
    ): List<DrawerResult> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        if (keyword != null && keyword.key.startsWith(Keywords.APP)) {
            return listOfNotNull(apps.firstOrNull { Keywords.appKey(it.packageName) == keyword.key }?.let { DrawerResult.App(it) })
        }
        val matching = apps.filter { it.label.contains(q, ignoreCase = true) }
            .sortedWith(compareBy({ !it.label.startsWith(q, ignoreCase = true) }, { it.label.lowercase() }))
        // Everything else that could be offered now, by key; your choices say what shows and in what order.
        val offers = buildMap<String, List<DrawerResult>> {
            putAll(found)
            targets.forEach { put(it.key, listOf(DrawerResult.Search(it))) }
            noteTargets.forEach { put(SearchOrder.noteKey(it), listOf(DrawerResult.AddNote(it))) }
            put(SearchOrder.REFUGE_NOTES, listOf(DrawerResult.AddNote(null)))
            event?.let { put(SearchOrder.CALENDAR, listOf(DrawerResult.AddEvent(it))) }
            put(SearchOrder.CONTACTS, contacts.map { DrawerResult.Contact(it, message) })
        }
        if (keyword != null) return offers[keyword.key].orEmpty()
        // What was found (apps, contacts, notes, files, settings pages) comes before what would be done
        // with the words, each part in your order, so the two never interleave.
        val chosen = (choices ?: SearchOrder.defaults(targets, noteTargets)).filter { it.on }.flatMap { offers[it.key].orEmpty() }
        return buildList {
            matching.forEach { add(DrawerResult.App(it)) }
            if (q.length >= 2 && "notes".startsWith(q.lowercase())) add(DrawerResult.OpenNotes)
            addAll(chosen.filterNot(::isAction))
            addAll(chosen.filter(::isAction))
        }
    }

    /** Whether [result] does something with the words (searches, saves a note or an event, sets the clock), rather than opening something found. */
    fun isAction(result: DrawerResult): Boolean =
        result is DrawerResult.Search || result is DrawerResult.AddNote || result is DrawerResult.AddEvent || result is DrawerResult.Clock

    /** Where the line between what was found and the actions goes: before the first action, if anything found comes before it. */
    fun dividerAt(results: List<DrawerResult>): Int? =
        results.indexOfFirst(::isAction).takeIf { it > 0 }

    /** How an event reads before it's saved: "add to calendar: Dentist · fri 30 oct, 09:00–10:00", the title as typed. */
    fun eventLabel(event: ParsedEvent, time: (java.time.LocalDateTime) -> String): String {
        val day = DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()).format(event.start).lowercase()
        val wh = if (event.allDay) "$day, all day" else "$day, ${time(event.start).lowercase()}–${time(event.end).lowercase()}"
        return "add to calendar: ${event.title} · $wh"
    }
}

/**
 * Every app you can open right now, A–Z, with a letter strip to jump through it, and a search field
 * that also searches the web, takes a quick note, or adds an event.
 */
@Composable
internal fun AppDrawerContent(
    apps: List<AppEntry>,
    style: HomeStyle,
    query: String,
    onQuery: (String) -> Unit,
    results: List<DrawerResult>,
    onLaunch: (String) -> Unit,
    onResult: (DrawerResult) -> Unit,
    preview: Boolean = false,
    /** A long press on an app: its actions. */
    onLongPress: ((AppEntry) -> Unit)? = null,
    /** The messaging apps beside each contact, in order. */
    chatApps: List<ChatApp> = listOf(ChatApp.SMS),
    /** The words the results are about: what was typed, or what follows a keyword. */
    words: String = query,
    /** A keyword was typed: its one result is marked, and Enter runs it. */
    keyworded: Boolean = false,
) {
    val p = style.palette()
    val focus = remember { FocusRequester() }
    if (!preview) LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    WithFontFamily(AppFonts.family(LocalContext.current, style.font)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(p.background)
                .then(if (preview) Modifier else Modifier.safeDrawingPadding())
                .padding(horizontal = 28.dp, vertical = 24.dp),
        ) {
            SearchField(query, onQuery, p, focus, onGo = { results.firstOrNull()?.let(onResult) })
            Spacer(Modifier.height(20.dp))
            if (query.isBlank()) {
                AppList(apps, style, p, onLaunch, onLongPress)
            } else {
                val divider = AppDrawer.dividerAt(results)
                LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(results) { i, result ->
                        if (i == divider) {
                            // Room between opening what was found and doing something with the words, so a tap meant for one can't land on the other.
                            Box(Modifier.fillMaxWidth().padding(vertical = 14.dp).height(1.dp).background(p.faint.copy(alpha = 0.3f)))
                        }
                        ResultRow(
                            result,
                            words.trim(),
                            style,
                            p,
                            marked = keyworded && i == 0,
                            onLongPress = (result as? DrawerResult.App)?.let { r -> onLongPress?.let { { it(r.app) } } },
                            chatApps = chatApps,
                            onAction = onResult,
                        ) { onResult(result) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit, p: HomePalette, focus: FocusRequester, onGo: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.text.copy(alpha = 0.07f)).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = p.faint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        BasicTextField(
            value = query,
            onValueChange = onQuery,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = p.text),
            cursorBrush = SolidColor(p.accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { onGo() }),
            modifier = Modifier.weight(1f).focusRequester(focus),
            decorationBox = { field ->
                Box {
                    if (query.isEmpty()) Text("Apps, the web, a note, an event", style = MaterialTheme.typography.bodyLarge, color = p.faint)
                    field()
                }
            },
        )
        if (query.isNotEmpty()) {
            Icon(
                Icons.Default.Close,
                contentDescription = "clear",
                tint = p.faint,
                modifier = Modifier.size(20.dp).clip(CircleShape).clickable { onQuery("") },
            )
        }
    }
}

/** The apps under their letters, and the strip on the right edge that jumps through them. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppList(apps: List<AppEntry>, style: HomeStyle, p: HomePalette, onLaunch: (String) -> Unit, onLongPress: ((AppEntry) -> Unit)?) {
    // The full list's own size, chosen apart from the home screen's app names.
    val names = style.textSize.scale * style.listSize.scale
    val sections = remember(apps) { AppDrawer.sections(apps) }
    // Where each letter's heading sits in the list.
    val starts = remember(sections) {
        var at = 0
        sections.associate { (letter, group) -> (letter to at).also { at += group.size + 1 } }
    }
    val list = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var touching by remember { mutableStateOf<String?>(null) }
    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = list, modifier = Modifier.fillMaxSize().padding(end = 28.dp)) {
            sections.forEach { (letter, group) ->
                item(key = "h$letter") {
                    Text(letter, style = MaterialTheme.typography.labelLarge, color = p.accent, modifier = Modifier.padding(top = 14.dp, bottom = 4.dp))
                }
                items(group, key = { it.packageName }) { app ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .combinedClickable(
                                onClickLabel = "open ${app.label}",
                                onLongClick = onLongPress?.let { { it(app) } },
                                onClick = { onLaunch(app.packageName) },
                            )
                            .padding(vertical = (12 * names).dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (style.showIcons) {
                            AppIcon(app.packageName, app.label, (30 * names).dp)
                            Spacer(Modifier.width(16.dp))
                        }
                        Text(app.label, fontSize = (26 * names).sp, color = p.text)
                    }
                }
            }
        }
        val letters = sections.map { it.first }
        if (letters.size > 1) {
            fun jump(y: Float, height: Int) {
                val i = (y / height * letters.size).toInt().coerceIn(0, letters.lastIndex)
                if (touching != letters[i]) {
                    touching = letters[i]
                    scope.launch { list.scrollToItem(starts.getValue(letters[i])) }
                }
            }
            Column(
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight(0.8f)
                    .width(24.dp)
                    .pointerInput(letters) {
                        detectTapGestures(
                            onPress = { offset ->
                                jump(offset.y, size.height)
                                tryAwaitRelease()
                                touching = null
                            },
                        )
                    }
                    .pointerInput(letters) {
                        detectVerticalDragGestures(
                            onDragStart = { jump(it.y, size.height) },
                            onDragEnd = { touching = null },
                            onDragCancel = { touching = null },
                        ) { change, _ -> jump(change.position.y, size.height) }
                    },
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                letters.forEach { letter ->
                    Text(letter, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = if (letter == touching) p.accent else p.faint)
                }
            }
        }
        touching?.let { letter ->
            // The letter you're on, large, beside your thumb.
            Box(
                Modifier.align(Alignment.Center).size(88.dp).clip(RoundedCornerShape(24.dp)).background(p.text.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center,
            ) { Text(letter, fontSize = 44.sp, color = p.text) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ResultRow(
    result: DrawerResult,
    query: String,
    style: HomeStyle,
    p: HomePalette,
    onLongPress: (() -> Unit)? = null,
    chatApps: List<ChatApp> = listOf(ChatApp.SMS),
    onAction: (DrawerResult) -> Unit = {},
    marked: Boolean = false,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val names = style.textSize.scale * style.listSize.scale
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (marked) p.accent.copy(alpha = 0.14f) else androidx.compose.ui.graphics.Color.Transparent)
            .combinedClickable(onLongClick = onLongPress, onClick = onClick)
            .padding(vertical = (16 * names).dp, horizontal = if (marked) 8.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val label = when (result) {
            is DrawerResult.App -> result.app.label
            is DrawerResult.Search -> result.target.label(query)
            is DrawerResult.AddNote -> result.target?.let { "add “$query” to ${it.name.lowercase()}" } ?: "save “$query” in refuge notes"
            is DrawerResult.AddEvent -> AppDrawer.eventLabel(result.event) { formatTime(context, it.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()) }
            DrawerResult.OpenNotes -> "open refuge notes"
            is DrawerResult.Contact -> result.hit.name
            is DrawerResult.Chat -> ""
            is DrawerResult.Calculation -> "= ${result.answer}"
            is DrawerResult.Clock -> ClockActions.label(result.action) { h, m ->
                formatTime(context, java.time.LocalDate.now().atTime(h, m).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()).lowercase()
            }
            is DrawerResult.Setting -> "settings: ${result.page.name.lowercase()}"
            is DrawerResult.VaultNote -> "${FolderIndex.title(result.file)} · obsidian"
            is DrawerResult.File -> result.file.name
        }
        val icon: ImageVector? = when (result) {
            is DrawerResult.App -> null
            is DrawerResult.Search -> when (result.target.kind) {
                SearchTarget.Kind.MAPS -> Icons.Default.LocationOn
                SearchTarget.Kind.LOOKUP -> Icons.Default.Info
                SearchTarget.Kind.SPOTIFY -> Icons.Default.PlayArrow
                else -> Icons.Default.Search
            }
            is DrawerResult.AddEvent -> Icons.Default.DateRange
            is DrawerResult.Contact -> Icons.Default.Call
            is DrawerResult.Calculation -> Icons.Default.Add
            is DrawerResult.Clock -> Icons.Default.Notifications
            is DrawerResult.Setting -> Icons.Default.Settings
            is DrawerResult.VaultNote -> Icons.Default.Create
            is DrawerResult.File -> Icons.AutoMirrored.Filled.List
            else -> Icons.Default.Edit
        }
        Box(Modifier.size((30 * names).dp), contentAlignment = Alignment.Center) {
            if (result is DrawerResult.App) {
                AppIcon(result.app.packageName, result.app.label, (30 * names).dp)
            } else if (icon != null) {
                Icon(icon, contentDescription = null, tint = p.accent, modifier = Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        // App names are lowercased like the rest; what you typed keeps its case.
        if (result is DrawerResult.Contact) {
            // The name, the message if one was typed, and a button for each messaging app beneath.
            Column(Modifier.weight(1f)) {
                VerbatimText(label, color = p.text, fontSize = (26 * names).sp, lineHeight = (32 * names).sp)
                if (result.message.isNotBlank()) {
                    VerbatimText("“${result.message}”", color = p.faint, fontSize = (18 * names).sp)
                }
                Row(Modifier.padding(top = 8.dp)) {
                    chatApps.forEach { app -> ContactButton(app.label, p) { onAction(DrawerResult.Chat(result.hit, app, result.message)) } }
                }
            }
        } else if (result is DrawerResult.App || result == DrawerResult.OpenNotes) {
            Text(label, fontSize = (26 * names).sp, lineHeight = (32 * names).sp, color = p.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        } else {
            // The suggestions as large as the apps in the list.
            VerbatimText(label, color = p.text, fontSize = (26 * names).sp, lineHeight = (32 * names).sp, modifier = Modifier.weight(1f, fill = false))
        }
    }
}

@Composable
private fun ContactButton(label: String, p: HomePalette, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = p.text,
        modifier = Modifier
            .padding(end = 8.dp)
            .clip(RoundedCornerShape(50))
            .border(1.dp, p.faint.copy(alpha = 0.5f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}
