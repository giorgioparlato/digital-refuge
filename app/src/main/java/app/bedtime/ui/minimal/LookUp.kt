package app.bedtime.ui.minimal

import androidx.compose.foundation.gestures.detectTapGestures

import androidx.compose.ui.input.pointer.pointerInput

import kotlinx.coroutines.launch

import androidx.compose.runtime.rememberCoroutineScope

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.bedtime.data.Definition
import app.bedtime.data.HomeStyle
import app.bedtime.data.StarDicts
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.VerbatimText
import app.bedtime.ui.homestyle.palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A note found by the look-up: its title, and its text once opened. */
private data class FoundNote(
    val title: String,
    val where: String,
    val refugeId: String? = null,
    val refugeText: String? = null,
    val vaultFile: app.bedtime.data.IndexedFile? = null,
)

/**
 * Where notes open when they mustn't be read here (on the lock screen): a refuge note by its id, an
 * Obsidian note by its file, each once you've unlocked. Then notes are found by their titles alone.
 */
internal class NotesOpenElsewhere(val refuge: (String) -> Unit, val vault: (app.bedtime.data.IndexedFile) -> Unit)

/**
 * A word looked up in your dictionaries, right where you are: on the home screen, or the lock screen
 * without unlocking. With [notes], your refuge notes and Obsidian notes ([vault]) too, to read in place.
 */
@Composable
internal fun LookUpDialog(
    style: HomeStyle,
    onDismiss: () -> Unit,
    initial: String = "",
    notes: Boolean = true,
    vault: String? = null,
    elsewhere: NotesOpenElsewhere? = null,
) {
    Dialog(onDismissRequest = onDismiss) { LookUpContent(style, initial, notes = notes, vault = vault, elsewhere = elsewhere) }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LookUpContent(
    style: HomeStyle,
    initial: String = "",
    focus: Boolean = true,
    notes: Boolean = true,
    vault: String? = null,
    /** With this, notes aren't read here but found by title and handed on: on the lock screen, to open once unlocked. */
    elsewhere: NotesOpenElsewhere? = null,
) {
    val context = LocalContext.current
    val p = style.palette()
    var words by remember { mutableStateOf(initial) }
    var reading by remember { mutableStateOf<FoundNote?>(null) }
    /** The words looked up before this one by tapping, to go back through. */
    var back by remember { mutableStateOf(emptyList<String>()) }
    val focusRequester = remember { FocusRequester() }
    if (focus) LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    val names by produceState<List<String>?>(null) { value = withContext(Dispatchers.IO) { StarDicts.names(context) } }
    // With several dictionaries, which to look in: all, or one; remembered for next time.
    val repo = remember { app.bedtime.data.Repository.get(context) }
    val saved by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val only = saved?.lookUpDictionary?.takeIf { it in names.orEmpty() }
    val scope = rememberCoroutineScope()
    val refugeNotes by produceState(emptyList<app.bedtime.data.Note>(), notes) {
        if (notes) app.bedtime.data.Repository.get(context).notes.collect { value = it }
    }
    // The vault's notes by name, listed once.
    val vaultFiles by produceState(emptyList<app.bedtime.data.IndexedFile>(), notes, vault) {
        if (notes && vault != null) value = withContext(Dispatchers.IO) { app.bedtime.data.FolderIndex.list(context, android.net.Uri.parse(vault)) { it.endsWith(".md") } }
    }
    // As you type: the words it could be, the entry for the word itself, and notes that have it.
    val found by produceState(emptyList<String>() to emptyList<Definition>(), words, only) {
        val w = words.trim()
        value = if (w.length < 2) emptyList<String>() to emptyList() else withContext(Dispatchers.IO) { StarDicts.suggest(context, w, only = only) to StarDicts.define(context, w, only) }
    }
    val (suggestions, definitions) = found
    val query = words.trim()
    val foundNotes = if (!notes || query.length < 2) emptyList() else {
        // Locked, only titles are searched: what's inside a note shouldn't show, not even as a match.
        val later = if (elsewhere != null) " · opens once unlocked" else ""
        refugeNotes.map { it to app.bedtime.data.NoteWidget.title(it.text).ifBlank { "A note" } }
            .filter { (note, title) -> (if (elsewhere != null) title else note.text).contains(query, ignoreCase = true) }
            .take(5).map { (note, title) -> FoundNote(title, "refuge notes$later", refugeId = note.id, refugeText = note.text) } +
            app.bedtime.data.FolderIndex.match(vaultFiles, query, limit = 5, name = app.bedtime.data.FolderIndex::title).map { FoundNote(app.bedtime.data.FolderIndex.title(it), "obsidian$later", vaultFile = it) }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(p.background)
            .border(1.dp, p.faint.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        reading?.let { note ->
            NoteReader(note, style) { reading = null }
            return@Column
        }
        Text("Look up", style = MaterialTheme.typography.titleMedium, color = p.text)
        names?.takeIf { it.size > 1 }?.let { all ->
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                (listOf<String?>(null) + all).forEach { name ->
                    val on = name == only
                    Text(
                        name ?: "All",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (on) p.text else p.faint,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(if (on) p.accent.copy(alpha = 0.22f) else p.background)
                            .border(1.dp, if (on) p.accent else p.faint.copy(alpha = 0.4f), RoundedCornerShape(50))
                            .clickable { scope.launch { repo.updateSettings { it.copy(lookUpDictionary = name) } } }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }
        BasicTextField(
            value = words,
            onValueChange = {
                words = it
                back = emptyList()
            },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = p.text),
            cursorBrush = SolidColor(p.accent),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .border(1.dp, p.faint.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            decorationBox = { field ->
                Box {
                    if (words.isEmpty()) Text(if (notes) "A word, or a note" else "A word", style = MaterialTheme.typography.bodyLarge, color = p.faint)
                    field()
                }
            },
        )
        back.lastOrNull()?.let { previous ->
            Text(
                "‹ back to $previous",
                style = MaterialTheme.typography.labelLarge,
                color = p.accent,
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable {
                    words = previous
                    back = back.dropLast(1)
                }.padding(vertical = 2.dp),
            )
        }
        Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when {
                names?.isEmpty() == true && !notes -> Text(
                    "No dictionaries yet. Add one under settings › search › dictionaries.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = p.faint,
                )
                definitions.isNotEmpty() -> definitions.forEach { d ->
                    Column {
                        Text(d.dictionary, style = MaterialTheme.typography.labelMedium, color = p.accent)
                        VerbatimText(d.word, style = MaterialTheme.typography.titleMedium, color = p.text)
                        EntryText(d, style) { word ->
                            // Another word from the entry: looked up in its place, with a way back.
                            if (!word.equals(words.trim(), ignoreCase = true)) {
                                back = back + words.trim()
                                words = word
                            }
                        }
                    }
                }
                suggestions.isNotEmpty() -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    suggestions.forEach { word ->
                        VerbatimText(
                            word,
                            style = MaterialTheme.typography.bodyMedium,
                            color = p.text,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .border(1.dp, p.faint.copy(alpha = 0.4f), RoundedCornerShape(50))
                                .clickable { words = word }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                        )
                    }
                }
            }
            if (foundNotes.isNotEmpty()) {
                Column {
                    Text("Notes", style = MaterialTheme.typography.labelMedium, color = p.accent)
                    foundNotes.forEach { note ->
                        val open = {
                            val file = note.vaultFile
                            val id = note.refugeId
                            when {
                                elsewhere == null -> reading = note
                                file != null -> elsewhere.vault(file)
                                id != null -> elsewhere.refuge(id)
                            }
                        }
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { open() }.padding(vertical = 8.dp)) {
                            VerbatimText(note.title, style = MaterialTheme.typography.bodyLarge, color = p.text)
                            Text(note.where, style = MaterialTheme.typography.labelSmall, color = p.faint)
                        }
                    }
                }
            }
            if (query.length >= 2 && definitions.isEmpty() && suggestions.isEmpty() && foundNotes.isEmpty()) {
                Text(if (notes) "Nothing in your dictionaries or notes." else "Not in your dictionaries.", style = MaterialTheme.typography.bodyMedium, color = p.faint)
            }
        }
    }
}

/** A note read in place: refuge's own, or a file from your vault, read from the phone. */
@Composable
private fun NoteReader(note: FoundNote, style: HomeStyle, onBack: () -> Unit) {
    val context = LocalContext.current
    val p = style.palette()
    val text by produceState(note.refugeText, note) {
        val file = note.vaultFile
        if (file != null) value = withContext(Dispatchers.IO) {
            runCatching { context.contentResolver.openInputStream(android.net.Uri.parse(file.uri))?.use { it.readBytes().decodeToString().take(MAX_READ) } }.getOrNull() ?: "This note couldn't be read."
        }
    }
    Text("‹ back", style = MaterialTheme.typography.labelLarge, color = p.accent, modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onBack).padding(vertical = 4.dp))
    VerbatimText(note.title, style = MaterialTheme.typography.titleMedium, color = p.text)
    Text(note.where, style = MaterialTheme.typography.labelSmall, color = p.faint)
    Column(Modifier.heightIn(max = 460.dp).verticalScroll(rememberScrollState())) {
        VerbatimText(text.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = p.text)
    }
}

/** The most of a note read here: longer ones are cut, as this is for a look, not for writing. */
private const val MAX_READ = 20_000

/** An entry with its marks shown: labels in italic in the accent, bold as bold, examples in italic and fainter. */
private fun entryText(d: Definition, p: app.bedtime.ui.homestyle.HomePalette): androidx.compose.ui.text.AnnotatedString =
    androidx.compose.ui.text.buildAnnotatedString {
        append(d.text)
        d.marks.forEach { mark ->
            val style = when (mark.kind) {
                app.bedtime.data.MarkKind.LABEL -> androidx.compose.ui.text.SpanStyle(color = p.accent, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                app.bedtime.data.MarkKind.STRONG -> androidx.compose.ui.text.SpanStyle(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                app.bedtime.data.MarkKind.EXAMPLE -> androidx.compose.ui.text.SpanStyle(color = p.faint, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                app.bedtime.data.MarkKind.LINK -> androidx.compose.ui.text.SpanStyle(color = p.accent, textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline)
            }
            if (mark.end <= d.text.length) addStyle(style, mark.start, mark.end)
        }
    }

/** An entry, any word of which can be tapped to look it up: links underlined, the rest as they read. */
@Composable
private fun EntryText(d: Definition, style: HomeStyle, onWord: (String) -> Unit) {
    val p = style.palette()
    val text = remember(d, p) { entryText(d, p) }
    var layout by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    androidx.compose.foundation.text.BasicText(
        text,
        style = MaterialTheme.typography.bodyMedium.copy(color = p.text),
        onTextLayout = { layout = it },
        modifier = Modifier.pointerInput(d) {
            detectTapGestures { position ->
                val at = layout?.getOffsetForPosition(position) ?: return@detectTapGestures
                StarDicts.wordAt(d.text, d.marks, at)?.let(onWord)
            }
        },
    )
}
