package app.bedtime.ui.minimal

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
    val focusRequester = remember { FocusRequester() }
    if (focus) LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    val names by produceState<List<String>?>(null) { value = withContext(Dispatchers.IO) { StarDicts.names(context) } }
    val refugeNotes by produceState(emptyList<app.bedtime.data.Note>(), notes) {
        if (notes) app.bedtime.data.Repository.get(context).notes.collect { value = it }
    }
    // The vault's notes by name, listed once.
    val vaultFiles by produceState(emptyList<app.bedtime.data.IndexedFile>(), notes, vault) {
        if (notes && vault != null) value = withContext(Dispatchers.IO) { app.bedtime.data.FolderIndex.list(context, android.net.Uri.parse(vault)) { it.endsWith(".md") } }
    }
    // As you type: the words it could be, the entry for the word itself, and notes that have it.
    val found by produceState(emptyList<String>() to emptyList<Definition>(), words) {
        val w = words.trim()
        value = if (w.length < 2) emptyList<String>() to emptyList() else withContext(Dispatchers.IO) { StarDicts.suggest(context, w) to StarDicts.define(context, w) }
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
        BasicTextField(
            value = words,
            onValueChange = { words = it },
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
                        VerbatimText(d.text, style = MaterialTheme.typography.bodyMedium, color = p.text)
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
