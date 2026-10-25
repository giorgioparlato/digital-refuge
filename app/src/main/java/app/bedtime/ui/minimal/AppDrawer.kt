package app.bedtime.ui.minimal

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
    /** A contact: tapped, the phone app with their number; [Message] and [WhatsApp] are its two buttons. */
    data class Contact(val hit: ContactHit) : DrawerResult
    data class Message(val hit: ContactHit) : DrawerResult
    data class WhatsApp(val hit: ContactHit) : DrawerResult
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
    ): List<DrawerResult> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val matching = apps.filter { it.label.contains(q, ignoreCase = true) }
            .sortedWith(compareBy({ !it.label.startsWith(q, ignoreCase = true) }, { it.label.lowercase() }))
        // Everything else that could be offered now, by key; your choices say what shows and in what order.
        val offers = buildMap<String, DrawerResult> {
            targets.forEach { put(it.key, DrawerResult.Search(it)) }
            noteTargets.forEach { put(SearchOrder.noteKey(it), DrawerResult.AddNote(it)) }
            put(SearchOrder.REFUGE_NOTES, DrawerResult.AddNote(null))
            event?.let { put(SearchOrder.CALENDAR, DrawerResult.AddEvent(it)) }
        }
        return buildList {
            matching.forEach { add(DrawerResult.App(it)) }
            if (q.length >= 2 && "notes".startsWith(q.lowercase())) add(DrawerResult.OpenNotes)
            (choices ?: SearchOrder.defaults(targets, noteTargets)).filter { it.on }.forEach { choice ->
                if (choice.key == SearchOrder.CONTACTS) contacts.forEach { add(DrawerResult.Contact(it)) } else offers[choice.key]?.let(::add)
            }
        }
    }

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
    /** Whether contacts get a WhatsApp button. */
    whatsApp: Boolean = false,
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
                LazyColumn(Modifier.fillMaxSize()) {
                    items(results) { result ->
                        ResultRow(
                            result,
                            query.trim(),
                            style,
                            p,
                            onLongPress = (result as? DrawerResult.App)?.let { r -> onLongPress?.let { { it(r.app) } } },
                            whatsApp = whatsApp,
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
    whatsApp: Boolean = false,
    onAction: (DrawerResult) -> Unit = {},
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    val names = style.textSize.scale * style.listSize.scale
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).combinedClickable(onLongClick = onLongPress, onClick = onClick).padding(vertical = (16 * names).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val label = when (result) {
            is DrawerResult.App -> result.app.label
            is DrawerResult.Search -> result.target.label(query)
            is DrawerResult.AddNote -> result.target?.let { "add “$query” to ${it.name.lowercase()}" } ?: "save “$query” in refuge notes"
            is DrawerResult.AddEvent -> AppDrawer.eventLabel(result.event) { formatTime(context, it.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()) }
            DrawerResult.OpenNotes -> "open refuge notes"
            is DrawerResult.Contact -> result.hit.name
            is DrawerResult.Message, is DrawerResult.WhatsApp -> ""
        }
        val icon: ImageVector? = when (result) {
            is DrawerResult.App -> null
            is DrawerResult.Search -> when (result.target.kind) {
                SearchTarget.Kind.MAPS -> Icons.Default.LocationOn
                SearchTarget.Kind.LOOKUP -> Icons.Default.Info
                else -> Icons.Default.Search
            }
            is DrawerResult.AddEvent -> Icons.Default.DateRange
            is DrawerResult.Contact -> Icons.Default.Call
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
        if (result is DrawerResult.App || result == DrawerResult.OpenNotes) {
            Text(label, fontSize = (26 * names).sp, lineHeight = (32 * names).sp, color = p.text, maxLines = 2, overflow = TextOverflow.Ellipsis)
        } else {
            // The suggestions as large as the apps in the list.
            VerbatimText(label, color = p.text, fontSize = (26 * names).sp, lineHeight = (32 * names).sp, modifier = Modifier.weight(1f, fill = false))
        }
        if (result is DrawerResult.Contact) {
            Spacer(Modifier.weight(1f))
            ContactButton("message", p) { onAction(DrawerResult.Message(result.hit)) }
            if (whatsApp) ContactButton("whatsapp", p) { onAction(DrawerResult.WhatsApp(result.hit)) }
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
            .padding(start = 8.dp)
            .clip(RoundedCornerShape(50))
            .border(1.dp, p.faint.copy(alpha = 0.5f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}
