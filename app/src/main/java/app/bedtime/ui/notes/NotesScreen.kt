package app.bedtime.ui.notes

import app.bedtime.data.TrashedNote
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.ui.focus.focusRequester
import app.bedtime.data.DoneTodo
import app.bedtime.data.Todo
import kotlinx.coroutines.flow.first

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.border
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.TextButton
import app.bedtime.data.NoteShelf
import app.bedtime.data.NoteWidget

import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import app.bedtime.data.Notes
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.horizontalScroll
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.Note
import app.bedtime.data.Repository
import app.bedtime.ui.components.AddCircleButton
import app.bedtime.ui.components.CardRadius
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.SwipeToDelete
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.VerbatimText
import app.bedtime.ui.formatTime
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Your notes, kept in the app: quick ones from the minimal home's search, and any you write here. */
@Composable
fun NotesScreen(onBack: (() -> Unit)?, initialOpen: String? = null, onSettings: (() -> Unit)? = null, openJournal: Boolean = false, journalDay: Long? = null) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val notes by repo.notes.collectAsStateWithLifecycle(initialValue = null)
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val trash by repo.trash.collectAsStateWithLifecycle(initialValue = emptyList())
    // Straight into today's microjournal, prompts showing, when that's what was asked for (from the lock screen).
    var journal by rememberSaveable { mutableStateOf(openJournal || journalDay != null) }
    val journalEntries by repo.journal.collectAsStateWithLifecycle(initialValue = emptyList())
    if (journal) {
        JournalContent(onBack = { journal = false }, openToday = openJournal, openDay = journalDay)
        return
    }
    NotesContent(
        notes = notes ?: return,
        onSave = { id, text -> scope.launch { repo.saveNote(id, text) } },
        onDelete = { id -> scope.launch { repo.deleteNote(id) } },
        onBack = onBack,
        initialOpen = initialOpen,
        // The microjournal's row once it's on, or once there's anything in it (your answers to "why?").
        onJournal = if (settings?.journalOn == true || journalEntries.isNotEmpty()) ({ journal = true }) else null,
        onSettings = onSettings,
        trash = trash,
        // A voice note's recordings, a tap from your audio player.
        onPlay = settings?.voiceFolder?.let { folder -> { file -> scope.launch { app.bedtime.voice.VoicePlayer.open(context, folder, file) } } },
        onRecord = settings?.takeIf { app.bedtime.data.Voice.ready(it) }?.let { { id -> context.startActivity(app.bedtime.voice.RecordActivity.intent(context, note = id)) } },
        shelf = ShelfHandlers(
            move = { from, into -> scope.launch { repo.moveNote(from, into) } },
            merge = { ids -> scope.launch { repo.mergeNotes(ids) } },
            pin = { id, pinned -> scope.launch { repo.pinNote(id, pinned) } },
            label = { id, labels -> scope.launch { repo.labelNote(id, labels) } },
            restore = { note, at -> scope.launch { repo.restoreNote(note, at) } },
            forget = { id -> scope.launch { repo.forgetTrashed(id) } },
            remind = { id, at ->
                scope.launch {
                    repo.remindNote(id, at)
                    app.bedtime.service.NoteReminders.schedule(context, repo.notes.first())
                }
            },
        ),
    )
}

/** What a held note can do: move into another, merge with others, be pinned, labelled, put back, or remind you. */
internal class ShelfHandlers(
    val move: (from: String, into: String) -> Unit = { _, _ -> },
    val merge: (List<String>) -> Unit = {},
    val pin: (String, Boolean) -> Unit = { _, _ -> },
    val label: (String, List<String>) -> Unit = { _, _ -> },
    val restore: (Note, Int) -> Unit = { _, _ -> },
    /** Gone from the trash for good: one, or all with null. */
    val forget: (String?) -> Unit = {},
    val remind: (String, Long?) -> Unit = { _, _ -> },
)

/**
 * The list of notes, or one open to edit. It's saved as you type ([onSave] with its id, new or not), so
 * nothing waits on leaving cleanly; a note left empty is deleted on the way out.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun NotesContent(
    notes: List<Note>,
    onSave: (id: String, text: String) -> Unit,
    onDelete: (String) -> Unit,
    onBack: (() -> Unit)?,
    /** One of a note's recordings, by file, opened in your audio player. */
    onPlay: ((String) -> Unit)? = null,
    /** A voice note recorded into the note open, by its id, once they're set up. */
    onRecord: ((String) -> Unit)? = null,
    initialOpen: String? = null,
    /** The microjournal, when it's switched on: a row of its own at the top. */
    onJournal: (() -> Unit)? = null,
    /** Where quick notes go, the microjournal and the rest, from the top bar. */
    onSettings: (() -> Unit)? = null,
    shelf: ShelfHandlers = ShelfHandlers(),
    /** Notes deleted in the last month, to put back. */
    trash: List<TrashedNote> = emptyList(),
) {
    // The note open to edit, by id; a new one gets its id as it opens.
    var open by rememberSaveable { mutableStateOf(initialOpen) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var reminding by remember { mutableStateOf<Note?>(null) }
    reminding?.let { note ->
        RemindDialog(note.remindAt, onDismiss = { reminding = null }) { at ->
            shelf.remind(note.id, at)
            reminding = null
        }
    }
    // Deleted with a moment to take it back.
    val deleteWithUndo: (Note) -> Unit = { note ->
        val at = notes.indexOfFirst { it.id == note.id }
        onDelete(note.id)
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            if (snackbar.showSnackbar("in the trash for a month", "undo", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) shelf.restore(note, at)
        }
    }
    val editing = open
    if (editing != null) {
        val note = notes.firstOrNull { it.id == editing }
        val todo = editing == Notes.TODO_ID
        // What the editor started from, fixed once it's open (the note itself changes as it's saved). The
        // to-do's done ones are kept apart, folded by when they were done, under what's still to do.
        val initial = remember(editing) { note?.text.orEmpty().let { if (todo) Todo.split(it).first else it } }
        var done by remember(editing) { mutableStateOf(if (todo) Todo.split(note?.text.orEmpty()).second else emptyList()) }
        val whole = { text: String -> if (todo) Todo.join(text, done) else text }
        val today = java.time.LocalDate.now()
        NoteEditor(
            initial = initial,
            isNew = note == null && initial.isEmpty(),
            // A new note opens ready to write.
            focus = note == null && initial.isEmpty(),
            onSave = { text -> whole(text).let { if (it.isNotBlank() && it != note?.text) onSave(editing, it) } },
            onDone = { text ->
                val all = whole(text)
                if (all.isBlank()) {
                    if (note != null) onDelete(editing)
                } else if (all != note?.text) {
                    onSave(editing, all)
                }
                open = null
            },
            onDelete = note?.let { { open = null; deleteWithUndo(it) } },
            outside = remember(editing) { app.bedtime.voice.VoiceRecorder.notesEdits(editing) },
            actions = {
                if (onRecord != null && !todo) {
                    IconButton(onClick = { onRecord(editing) }) {
                        Icon(app.bedtime.ui.components.BedtimeIcons.Mic, contentDescription = "record a voice note into it", tint = Obsidian.colors.accentText)
                    }
                }
                if (note != null) {
                    IconButton(onClick = { reminding = note }) {
                        Icon(Icons.Default.Notifications, contentDescription = "remind me", tint = if (note.remindAt != null) Obsidian.colors.accent else Obsidian.colors.textMuted)
                    }
                }
            },
            saveWith = done,
            panel = if (onPlay == null || app.bedtime.data.Voice.recordings(initial).isEmpty()) null else { value, _ ->
                Column(Modifier.padding(horizontal = PageGutter, vertical = 8.dp)) {
                    RecordingChips(app.bedtime.data.Voice.recordings(value.text), onPlay)
                }
            },
            onTick = if (!todo) null else { text, line ->
                val (rest, nowDone) = Todo.split(Todo.toggled(Todo.join(text, done), line, today))
                done = nowDone
                rest
            },
            below = if (!todo) null else { set, current ->
                DoneTodos(
                    done, today,
                    onUntick = { item ->
                        val all = Todo.join(current(), done)
                        val lines = all.lines()
                        val from = current().lines().size
                        val at = (from until lines.size).firstOrNull { lines[it] == item.lines.first() }
                        if (at != null) {
                            val (rest, nowDone) = Todo.split(Todo.toggled(all, at, today))
                            done = nowDone
                            set(rest)
                        }
                    },
                    onClear = { done = emptyList() },
                )
            },
        )
        return
    }
    val c = Obsidian.colors
    var trashOpen by rememberSaveable { mutableStateOf(false) }
    if (trashOpen) {
        TrashList(trash, onBack = { trashOpen = false }, onPutBack = { shelf.restore(it.note, it.at) }, onForget = shelf.forget)
        return
    }
    // Notes picked to merge, in the order picked; a held note's menu; and the dialogs it leads to.
    var picking by rememberSaveable { mutableStateOf<List<String>?>(null) }
    var menuFor by remember { mutableStateOf<String?>(null) }
    var moving by remember { mutableStateOf<Note?>(null) }
    var labelling by remember { mutableStateOf<Note?>(null) }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    // The page's own search: words anywhere in a note; null when closed.
    var searching by rememberSaveable { mutableStateOf<String?>(null) }
    val query = searching?.trim().orEmpty()
    BackHandler(enabled = searching != null && picking == null) { searching = null }
    val allLabels = NoteShelf.labels(notes)
    if (filter != null && filter !in allLabels) filter = null
    val selected = picking
    BackHandler(enabled = selected != null) { picking = null }
    moving?.let { note ->
        MoveIntoDialog(note, notes, onDismiss = { moving = null }) { into ->
            shelf.move(note.id, into)
            moving = null
        }
    }
    labelling?.let { note ->
        LabelsDialog(note.labels, allLabels, onDismiss = { labelling = null }) { labels ->
            shelf.label(note.id, labels)
            labelling = null
        }
    }
    // One note's card: tap to open (or pick, when merging), hold for its menu, swipe to the trash.
    val noteCard: @Composable (Note) -> Unit = { note ->
        val order = selected?.indexOf(note.id)?.takeIf { it >= 0 }
        SwipeToDelete(onDelete = { deleteWithUndo(note) }, swipeAway = true) {
            Box {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(CardRadius))
                        .background(if (order != null) c.accent.copy(alpha = 0.18f) else c.bgSecondary)
                        .combinedClickable(
                            onClickLabel = if (selected != null) "pick" else "edit",
                            onLongClickLabel = "more",
                            onLongClick = { if (selected == null) menuFor = note.id },
                        ) {
                            when {
                                selected == null -> open = note.id
                                order != null -> picking = selected - note.id
                                else -> picking = selected + note.id
                            }
                        }
                        .padding(16.dp),
                ) {
                    val reminder = note.remindAt?.takeIf { it > System.currentTimeMillis() }
                    if (note.pinned || note.labels.isNotEmpty() || order != null || reminder != null) {
                        Row(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            order?.let { Text("${it + 1}", style = MaterialTheme.typography.labelLarge, color = c.accentText) }
                            if (note.pinned) Text("pinned", style = MaterialTheme.typography.labelSmall, color = c.accentText)
                            reminder?.let { Text("⏰ " + reminderWords(context, it), style = MaterialTheme.typography.labelSmall, color = c.accentText) }
                            note.labels.forEach { Text("#$it", style = MaterialTheme.typography.labelSmall, color = c.textMuted) }
                        }
                    }
                    VerbatimText(
                        // Searching, the words found are marked, and the note shows from where they are.
                        highlighted(Markdown.render(fromMatch(note.text, query), c.textFaint), query, c.accent.copy(alpha = 0.35f)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textNormal,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        whenWritten(LocalContext.current, note.editedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textFaint,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                DropdownMenu(expanded = menuFor == note.id, onDismissRequest = { menuFor = null }, containerColor = c.bgSecondary) {
                    val item = @Composable { label: String, action: () -> Unit ->
                        DropdownMenuItem(text = { Text(label, color = c.textNormal) }, onClick = {
                            menuFor = null
                            action()
                        })
                    }
                    if (note.id != Notes.TODO_ID) item(if (note.pinned) "Unpin" else "Pin near the top") { shelf.pin(note.id, !note.pinned) }
                    item("Labels…") { labelling = note }
                    item(if (note.remindAt != null) "Reminder…" else "Remind me…") { reminding = note }
                    if (note.id != Notes.TODO_ID && notes.size > 1) item("Move into…") { moving = note }
                    if (notes.size > 1) item("Pick to merge") { picking = listOf(note.id) }
                    item("Share…") {
                        val send = android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, note.text)
                        runCatching { context.startActivity(android.content.Intent.createChooser(send, null).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }
                    item("Copy") {
                        context.getSystemService(android.content.ClipboardManager::class.java)?.setPrimaryClip(android.content.ClipData.newPlainText("note", note.text))
                        android.widget.Toast.makeText(context, "note copied", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    if (note.id == Notes.TODO_ID && Todo.split(note.text).second.isNotEmpty()) item("Clear done") {
                        onSave(note.id, Todo.withoutDone(note.text))
                        scope.launch {
                            if (snackbar.showSnackbar("done ones cleared", "undo", duration = SnackbarDuration.Short) == SnackbarResult.ActionPerformed) onSave(note.id, note.text)
                        }
                    }
                }
            }
        }
    }
    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            if (selected != null) {
                ObsidianTopBar(if (selected.size == 1) "1 picked" else "${selected.size} picked", onBack = { picking = null }, subtitle = "Tap notes in the order to merge them") {
                    TextButton(onClick = {
                        shelf.merge(selected)
                        picking = null
                    }, enabled = selected.size >= 2) { Text("Merge", color = if (selected.size >= 2) c.accentText else c.textFaint) }
                }
            } else {
                ObsidianTopBar("Notes", onBack = onBack, subtitle = "Written in Markdown, kept on this phone") {
                    IconButton(onClick = { searching = if (searching == null) "" else null }) { Icon(Icons.Default.Search, contentDescription = "search your notes", tint = c.textMuted) }
                    if (onSettings != null) IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, contentDescription = "notes settings", tint = c.textMuted) }
                }
            }
        },
        floatingActionButton = { if (selected == null) AddCircleButton("new note", onClick = { open = java.util.UUID.randomUUID().toString() }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        if (notes.isEmpty() && onJournal == null) {
            Text(
                "Nothing yet. Type in the minimal home's search and pick “save in refuge notes”, or start one with +.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
                modifier = Modifier.padding(padding).padding(horizontal = PageGutter, vertical = 8.dp),
            )
            return@Scaffold
        }
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = PageGutter),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            searching?.let { typed ->
                item(key = "search") {
                    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
                    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
                    app.bedtime.ui.components.ObsidianTextField(
                        value = typed,
                        onValueChange = { searching = it },
                        placeholder = "Words in any note",
                        modifier = Modifier.focusRequester(focus),
                    )
                }
            }
            // The microjournal near the top: under your "why" answers when they're pinned, otherwise first.
            val journalRow = onJournal?.takeIf { selected == null && filter == null && query.isEmpty() }
            val whyOnTop = notes.any { it.id == app.bedtime.data.Why.NOTE_ID && it.pinned }
            if (journalRow != null && !whyOnTop) item(key = "journal") { JournalRow(journalRow) }
            // Your labels, to see only one's notes.
            if (allLabels.isNotEmpty()) {
                item(key = "labels") {
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        (listOf<String?>(null) + allLabels).forEach { label -> LabelChip(label ?: "All", on = label == filter) { filter = label } }
                    }
                }
            }
            // The to-do first, then those pinned, then the rest.
            val shown = NoteShelf.labelled(NoteShelf.ordered(notes), filter).filter { query.isEmpty() || it.text.contains(query, ignoreCase = true) }
            if (query.isNotEmpty() && shown.isEmpty()) {
                item(key = "none") { Text("No note has “$query”.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted) }
            }
            items(shown, key = { it.id }) { note ->
                if (note.id == app.bedtime.data.Why.NOTE_ID && whyOnTop && journalRow != null) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        noteCard(note)
                        JournalRow(journalRow)
                    }
                } else {
                    noteCard(note)
                }
            }
            // Deleted notes, a month each.
            if (trash.isNotEmpty() && selected == null && query.isEmpty() && filter == null) {
                item(key = "trash") {
                    Text(
                        "Trash · ${trash.size} ›",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.textMuted,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { trashOpen = true }.padding(vertical = 12.dp),
                    )
                }
            }
            item(key = "foot") { Spacer(Modifier.height(80.dp)) }
        }
    }
}

/** Notes deleted in the last month: each put back where it was, or gone for good; or all of them, emptied. */
@Composable
private fun TrashList(trash: List<TrashedNote>, onBack: () -> Unit, onPutBack: (TrashedNote) -> Unit, onForget: (String?) -> Unit) {
    val c = Obsidian.colors
    var emptying by remember { mutableStateOf(false) }
    BackHandler(onBack = onBack)
    if (emptying) {
        AlertDialog(
            onDismissRequest = { emptying = false },
            containerColor = c.bgSecondary,
            title = { Text("Empty the trash?") },
            text = { Text(if (trash.size == 1) "The note in it goes for good." else "The ${trash.size} notes in it go for good.", color = c.textMuted) },
            confirmButton = {
                TextButton(onClick = {
                    emptying = false
                    onForget(null)
                }) { Text("Empty", color = c.accentText) }
            },
            dismissButton = { TextButton(onClick = { emptying = false }) { Text("Cancel", color = c.textMuted) } },
        )
    }
    val now = System.currentTimeMillis()
    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            ObsidianTopBar("Trash", onBack = onBack, subtitle = "Deleted notes stay a month, then go") {
                if (trash.isNotEmpty()) TextButton(onClick = { emptying = true }) { Text("Empty", color = c.accentText) }
            }
        },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = PageGutter), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (trash.isEmpty()) item { Text("Nothing in the trash.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted) }
            items(trash, key = { it.note.id }) { item ->
                // A swipe aside deletes it for good, as one in your notes puts it here.
                SwipeToDelete(onDelete = { onForget(item.note.id) }) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(CardRadius)).background(c.bgSecondary).padding(16.dp)) {
                        VerbatimText(Markdown.render(item.note.text, c.textFaint), style = MaterialTheme.typography.bodyMedium, color = c.textMuted, maxLines = 4, overflow = TextOverflow.Ellipsis)
                        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            val left = NoteShelf.daysLeft(item, now)
                            Text(if (left == 1L) "goes tomorrow · swipe to delete now" else "goes in $left days · swipe to delete now", style = MaterialTheme.typography.labelSmall, color = c.textFaint, modifier = Modifier.weight(1f))
                            TextButton(onClick = { onPutBack(item) }) { Text("Put back", color = c.accentText) }
                        }
                    }
                }
            }
        }
    }
}

/** "today 18:00", "tomorrow 09:00", "fri 6 nov 09:00": when a reminder is. */
private fun reminderWords(context: android.content.Context, at: Long): String {
    val date = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate()
    val today = java.time.LocalDate.now()
    val day = when (date) {
        today -> "today"
        today.plusDays(1) -> "tomorrow"
        else -> DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()).format(date)
    }
    return "$day ${formatTime(context, at)}".lowercase()
}

/** [text] from a line or so before the first line with [query], so what was found shows in the card. */
private fun fromMatch(text: String, query: String): String {
    if (query.isEmpty()) return text
    val lines = text.lines()
    val at = lines.indexOfFirst { it.contains(query, ignoreCase = true) }
    return if (at < 6) text else lines.drop(at - 1).joinToString("\n")
}

/** [text] with every [query] in it marked in [colour]. */
private fun highlighted(text: androidx.compose.ui.text.AnnotatedString, query: String, colour: androidx.compose.ui.graphics.Color): androidx.compose.ui.text.AnnotatedString {
    if (query.isEmpty()) return text
    return androidx.compose.ui.text.AnnotatedString.Builder(text).apply {
        var from = text.text.indexOf(query, ignoreCase = true)
        while (from >= 0) {
            addStyle(androidx.compose.ui.text.SpanStyle(background = colour), from, from + query.length)
            from = text.text.indexOf(query, from + query.length, ignoreCase = true)
        }
    }.toAnnotatedString()
}

/**
 * The to-do's done ones, under what's still to do: folded by when they were done (this week open, the
 * rest a tap away), each unticked with a tap on its box, and all cleared at once.
 */
@Composable
private fun DoneTodos(done: List<DoneTodo>, today: java.time.LocalDate, onUntick: (DoneTodo) -> Unit, onClear: () -> Unit) {
    if (done.isEmpty()) return
    val c = Obsidian.colors
    var clearing by remember { mutableStateOf(false) }
    if (clearing) {
        AlertDialog(
            onDismissRequest = { clearing = false },
            containerColor = c.bgSecondary,
            title = { Text(if (done.size == 1) "Clear the one done?" else "Clear the ${done.size} done?") },
            confirmButton = {
                TextButton(onClick = {
                    clearing = false
                    onClear()
                }) { Text("Clear", color = c.accentText) }
            },
            dismissButton = { TextButton(onClick = { clearing = false }) { Text("Cancel", color = c.textMuted) } },
        )
    }
    val groups = Todo.groups(done, today)
    var openGroups by remember { mutableStateOf(setOfNotNull(groups.firstOrNull()?.first?.takeIf { it == "This week" })) }
    Column(Modifier.fillMaxWidth().padding(top = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Done · ${done.size}", style = MaterialTheme.typography.labelLarge, color = c.accentText, modifier = Modifier.weight(1f))
            Text("clear", style = MaterialTheme.typography.labelLarge, color = c.textMuted, modifier = Modifier.clip(RoundedCornerShape(50)).clickable { clearing = true }.padding(horizontal = 8.dp, vertical = 6.dp))
        }
        groups.forEach { (name, items) ->
            val isOpen = name in openGroups
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { openGroups = if (isOpen) openGroups - name else openGroups + name }.padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("$name · ${items.size}", style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.weight(1f))
                Icon(if (isOpen) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = c.textFaint)
            }
            if (isOpen) {
                items.forEach { item ->
                    Row(Modifier.fillMaxWidth().padding(start = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).clickable(onClickLabel = "not done after all") { onUntick(item) },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Default.Check, contentDescription = null, tint = c.accent, modifier = Modifier.size(18.dp)) }
                        VerbatimText(
                            androidx.compose.ui.text.AnnotatedString(item.text),
                            style = MaterialTheme.typography.bodyMedium.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                            color = c.textFaint,
                            modifier = Modifier.weight(1f),
                        )
                        item.date?.let { Text(DateTimeFormatter.ofPattern("d MMM", Locale.getDefault()).format(it), style = MaterialTheme.typography.labelSmall, color = c.textFaint) }
                    }
                }
            }
        }
    }
}

/**
 * When to be reminded of a note: an hour from now, three, this evening, tomorrow or next week
 * morning, or a day and time of your own; and, when one is set, taking it away.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun RemindDialog(current: Long?, onDismiss: () -> Unit, onPick: (Long?) -> Unit) {
    val c = Obsidian.colors
    val context = LocalContext.current
    var choosingDay by remember { mutableStateOf(false) }
    var day by remember { mutableStateOf<java.time.LocalDate?>(null) }
    val chosenDay = day
    when {
        chosenDay != null -> {
            val time = androidx.compose.material3.rememberTimePickerState(9, 0, android.text.format.DateFormat.is24HourFormat(context))
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = c.bgSecondary,
                title = { Text("At what time?") },
                text = { androidx.compose.material3.TimePicker(time) },
                confirmButton = {
                    TextButton(onClick = {
                        onPick(chosenDay.atTime(time.hour, time.minute).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                    }) { Text("Remind me", color = c.accentText) }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.textMuted) } },
            )
        }
        choosingDay -> {
            val utc = java.time.ZoneOffset.UTC
            val today = java.time.LocalDate.now()
            val state = androidx.compose.material3.rememberDatePickerState(
                initialSelectedDateMillis = today.plusDays(1).atStartOfDay(utc).toInstant().toEpochMilli(),
                selectableDates = object : androidx.compose.material3.SelectableDates {
                    override fun isSelectableDate(utcTimeMillis: Long) = !Instant.ofEpochMilli(utcTimeMillis).atZone(utc).toLocalDate().isBefore(today)
                },
            )
            androidx.compose.material3.DatePickerDialog(
                onDismissRequest = onDismiss,
                confirmButton = {
                    TextButton(onClick = { state.selectedDateMillis?.let { day = Instant.ofEpochMilli(it).atZone(utc).toLocalDate() } }) { Text("Next", color = c.accentText) }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.textMuted) } },
            ) { androidx.compose.material3.DatePicker(state) }
        }
        else -> AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = c.bgSecondary,
            title = { Text("Remind me") },
            text = {
                Column {
                    current?.takeIf { it > System.currentTimeMillis() }?.let {
                        Text("Set for " + reminderWords(context, it), style = MaterialTheme.typography.bodyMedium, color = c.accentText, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    NoteShelf.reminderChoices(java.time.LocalDateTime.now()).forEach { (label, at) ->
                        Row(Modifier.fillMaxWidth().clickable { onPick(at) }.padding(vertical = 12.dp)) {
                            Text(label, style = MaterialTheme.typography.bodyLarge, color = c.textNormal, modifier = Modifier.weight(1f))
                            Text(reminderWords(context, at), style = MaterialTheme.typography.bodySmall, color = c.textFaint)
                        }
                    }
                    Text("A day and time…", style = MaterialTheme.typography.bodyLarge, color = c.accentText, modifier = Modifier.fillMaxWidth().clickable { choosingDay = true }.padding(vertical = 12.dp))
                    if (current != null) {
                        Text("No reminder", style = MaterialTheme.typography.bodyLarge, color = c.textMuted, modifier = Modifier.fillMaxWidth().clickable { onPick(null) }.padding(vertical = 12.dp))
                    }
                }
            },
            confirmButton = {},
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.textMuted) } },
        )
    }
}

/** The microjournal's row in your notes. */
@Composable
private fun JournalRow(onClick: () -> Unit) {
    val c = Obsidian.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(CardRadius)).background(c.bgSecondary).clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Microjournal", style = MaterialTheme.typography.titleSmall, color = c.textNormal)
            Text("A few lines a day, with prompts if you want them", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
        app.bedtime.ui.components.Chevron()
    }
}

/** A label to filter by, or "all"; the one in use filled. */
@Composable
private fun LabelChip(label: String, on: Boolean, onClick: () -> Unit) {
    val c = Obsidian.colors
    Text(
        label,
        style = MaterialTheme.typography.labelMedium,
        color = if (on) c.bgPrimary else c.textMuted,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (on) c.accent else androidx.compose.ui.graphics.Color.Transparent)
            .border(1.dp, if (on) c.accent else c.border, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

/** Another note to move [note] into, by its title; it goes to the end of that one. */
@Composable
private fun MoveIntoDialog(note: Note, notes: List<Note>, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val c = Obsidian.colors
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.bgSecondary,
        title = { Text("Move into") },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(NoteShelf.ordered(notes).filter { it.id != note.id }, key = { it.id }) { other ->
                    VerbatimText(
                        androidx.compose.ui.text.AnnotatedString(NoteWidget.title(other.text).ifBlank { "A note" }),
                        style = MaterialTheme.typography.bodyLarge,
                        color = c.textNormal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().clickable { onPick(other.id) }.padding(vertical = 12.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.textMuted) } },
    )
}

/** A note's labels: those in use to switch on or off, and a new one. */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun LabelsDialog(current: List<String>, all: List<String>, onDismiss: () -> Unit, onSave: (List<String>) -> Unit) {
    val c = Obsidian.colors
    var chosen by remember { mutableStateOf(current) }
    var typed by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.bgSecondary,
        title = { Text("Labels") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val shown = (all + chosen).distinctBy { it.lowercase() }
                if (shown.isNotEmpty()) {
                    androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        shown.forEach { label ->
                            val on = chosen.any { it.equals(label, ignoreCase = true) }
                            LabelChip(label, on) { chosen = if (on) chosen.filterNot { it.equals(label, ignoreCase = true) } else chosen + label }
                        }
                    }
                }
                app.bedtime.ui.components.ObsidianTextField(
                    value = typed,
                    onValueChange = { typed = it },
                    placeholder = "A new label, like books",
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = {
                        if (typed.isNotBlank()) chosen = chosen + typed.trim()
                        typed = ""
                    }),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(if (typed.isNotBlank()) chosen + typed.trim() else chosen) }) { Text("Save", color = c.accentText) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.textMuted) } },
    )
}

/**
 * One note, styled as Markdown as you type, with a few marks a tap away; saved when you go back. A
 * [title] of its own, more buttons at the top ([actions]), and a [panel] above the text, which can
 * put words in where the cursor is, serve the microjournal; a change to [saveWith] saves too.
 */
@Composable
internal fun NoteEditor(
    initial: String,
    isNew: Boolean,
    onSave: (String) -> Unit,
    onDone: (String) -> Unit,
    onDelete: (() -> Unit)?,
    title: String? = null,
    actions: @Composable () -> Unit = {},
    /** Above the text: it reads the text with its cursor, and changes it there. */
    panel: (@Composable (value: TextFieldValue, edit: ((TextFieldValue) -> TextFieldValue) -> Unit) -> Unit)? = null,
    saveWith: Any? = null,
    /** Typing read first by the note's own rules (the microjournal's timed lines); null leaves it to the lists'. */
    onType: ((old: TextFieldValue, new: TextFieldValue) -> TextFieldValue?)? = null,
    /** The keyboard up as it opens, ready to write. */
    focus: Boolean = false,
    /** Changes made to the note from elsewhere while it's open (a voice note's line, then its words), taken in here. */
    outside: kotlinx.coroutines.flow.Flow<(String) -> String>? = null,
    /** A box ticked or unticked at a line: the text it leaves, when the note does more than tick it (the to-do's). */
    onTick: ((text: String, line: Int) -> String)? = null,
    /** Under the text, scrolling with it: the to-do's done ones; it can set the text, and read it. */
    below: (@Composable (set: (String) -> Unit, current: () -> String) -> Unit)? = null,
) {
    val c = Obsidian.colors
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    // Undo and redo, as Obsidian has them: what was written before, a pause in typing (or a mark) at a time.
    var undo by remember { mutableStateOf(emptyList<TextFieldValue>()) }
    var redo by remember { mutableStateOf(emptyList<TextFieldValue>()) }
    var lastStep by remember { mutableStateOf(0L) }
    val edit: (TextFieldValue, Boolean) -> Unit = { new, step ->
        if (new.text != value.text) {
            val now = System.currentTimeMillis()
            if (step || undo.isEmpty() || now - lastStep > UNDO_PAUSE_MS) undo = (undo + value).takeLast(UNDO_STEPS)
            lastStep = now
            redo = emptyList()
        }
        value = new
    }
    BackHandler { onDone(value.text) }
    if (outside != null) LaunchedEffect(outside) {
        outside.collect { change ->
            val text = change(value.text)
            if (text != value.text) {
                // Writing at the end, the cursor stays at the end; elsewhere, where it was.
                val atEnd = value.selection.start >= value.text.trimEnd().length
                value = TextFieldValue(text, TextRange(if (atEnd) text.length else value.selection.start.coerceAtMost(text.length)))
            }
        }
    }
    val focusText = remember { androidx.compose.ui.focus.FocusRequester() }
    if (focus) LaunchedEffect(Unit) { runCatching { focusText.requestFocus() } }
    // Saved a moment after you pause typing.
    LaunchedEffect(value.text, saveWith) {
        delay(600)
        onSave(value.text)
    }
    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            ObsidianTopBar(title ?: if (isNew) "New note" else "Note", onBack = { onDone(value.text) }) {
                actions()
                if (onDelete != null) {
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "delete", tint = c.textMuted) }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            panel?.invoke(value) { change -> edit(change(value), true) }
            var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
            // The note grows inside a scrolling page, so its boxes can be drawn where its lines are.
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = PageGutter)) {
            BasicTextField(
                value = value,
                onValueChange = { new ->
                    val ticked = Markdown.tickOnTap(value, new)
                    when {
                        ticked != null && onTick != null -> {
                            val line = value.text.substring(0, new.selection.start).count { it == '\n' }
                            val text = onTick(value.text, line)
                            // The cursor stays at the end of the line where the item was, so the page doesn't jump.
                            val end = text.lines().take(line + 1).sumOf { it.length + 1 } - 1
                            value = TextFieldValue(text, TextRange(end.coerceIn(0, text.length)))
                            // Moved elsewhere with what it does, a tick isn't undone here: untick it instead.
                            undo = emptyList()
                            redo = emptyList()
                        }
                        ticked != null -> edit(ticked, true)
                        else -> edit(onType?.invoke(value, new) ?: Markdown.continueList(value, new), false)
                    }
                },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.textNormal),
                cursorBrush = SolidColor(c.accent),
                visualTransformation = remember(c.textFaint, c.accent) { MarkdownTransformation(c.textFaint, c.accent) },
                onTextLayout = { layout = it },
                modifier = Modifier.fillMaxWidth().heightIn(min = if (below != null) 160.dp else 400.dp).todoBoxes(value.text, { layout }, c.textNormal.copy(alpha = 0.18f), c.accent)
                    .then(if (focus) Modifier.focusRequester(focusText) else Modifier),
                decorationBox = { field ->
                    Box(contentAlignment = Alignment.TopStart) {
                        if (value.text.isEmpty()) VerbatimText("# A title\n\n- a list\n- [ ] a to-do", style = MaterialTheme.typography.bodyLarge, color = c.textFaint)
                        field()
                    }
                },
            )
            below?.invoke({ text ->
                value = TextFieldValue(text, TextRange(text.length))
                undo = emptyList()
                redo = emptyList()
            }, { value.text })
            Spacer(Modifier.height(24.dp))
            }
            MarkdownBar(
                canUndo = undo.isNotEmpty(),
                canRedo = redo.isNotEmpty(),
                onUndo = {
                    redo = redo + value
                    value = undo.last()
                    undo = undo.dropLast(1)
                },
                onRedo = {
                    undo = undo + value
                    value = redo.last()
                    redo = redo.dropLast(1)
                },
            ) { change -> edit(change(value), true) }
        }
    }
}

/** How long a pause in typing makes a new step to undo, and how many steps are kept. */
private const val UNDO_PAUSE_MS = 1_000L
private const val UNDO_STEPS = 100

/** The marks Obsidian keeps above its keyboard: undo and redo, brackets, bold, italic, headings, a list, a to-do, and indenting. */
@Composable
private fun MarkdownBar(canUndo: Boolean, canRedo: Boolean, onUndo: () -> Unit, onRedo: () -> Unit, onEdit: ((TextFieldValue) -> TextFieldValue) -> Unit) {
    val c = Obsidian.colors
    Row(
        Modifier.fillMaxWidth().background(c.bgSecondary).horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarKey("↶", "undo", enabled = canUndo, onClick = onUndo)
        BarKey("↷", "redo", enabled = canRedo, onClick = onRedo)
        BarKey("[", "bracket") { onEdit { Markdown.insert(it, "[") } }
        BarKey("]", "closing bracket") { onEdit { Markdown.insert(it, "]") } }
        BarKey("B", "bold", bold = true) { onEdit { Markdown.wrap(it, "**") } }
        BarKey("I", "italic", italic = true) { onEdit { Markdown.wrap(it, "*") } }
        BarKey("H", "heading: tap again for a smaller one") { onEdit { Markdown.cycleHeading(it) } }
        BarKey("•", "list") { onEdit { Markdown.toggleLine(it, "- ") } }
        BarKey("☐", "to-do") { onEdit { Markdown.toggleLine(it, "- [ ] ") } }
        BarKey("→", "indent") { onEdit { Markdown.indent(it, inward = true) } }
        BarKey("←", "outdent") { onEdit { Markdown.indent(it, inward = false) } }
    }
}

@Composable
private fun BarKey(label: String, description: String, bold: Boolean = false, italic: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val c = Obsidian.colors
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).clickable(enabled = enabled, onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        VerbatimText(
            label,
            color = if (enabled) c.textNormal else c.textFaint.copy(alpha = 0.5f),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
                fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            ),
        )
    }
}

/** "25 oct 2026, 22:41": when a note was last written. */
private fun whenWritten(context: android.content.Context, at: Long): String =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()).format(Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault())) +
        ", " + formatTime(context, at)
