package app.bedtime.ui.minimal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.Definition
import app.bedtime.data.HomeStyle
import app.bedtime.data.StarDicts
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.VerbatimText
import app.bedtime.ui.homestyle.palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

/** A note found by the look-up: its title, and its text once opened. */
internal data class FoundNote(
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
 * The quick note's look-up: a single word typed brings your dictionaries' entries for it and, with
 * [notes], your notes that have it. With [elsewhere] (on the lock screen) notes are found by their
 * titles alone and open once you've unlocked, never here.
 */
internal data class LookUpWith(val notes: Boolean = true, val elsewhere: NotesOpenElsewhere? = null)

/**
 * A panel at the top of the screen, clear of the keyboard and of Android's swipe at the foot. A tap
 * beside it or Back puts it away; so does a swipe up or down on it while [swipeAway] says so (not
 * once there's a note in it that a stray swipe would lose).
 */
@Composable
internal fun TopPanel(onDismiss: () -> Unit, swipeAway: () -> Boolean = { true }, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        val away = with(LocalDensity.current) { 48.dp.toPx() }
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(onDismiss) { detectTapGestures { onDismiss() } }
                .pointerInput(onDismiss) {
                    var travel = 0f
                    detectVerticalDragGestures(
                        onDragStart = { travel = 0f },
                        onDragEnd = { if (abs(travel) > away && swipeAway()) onDismiss() },
                    ) { _, dy -> travel += dy }
                }
                .safeDrawingPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            contentAlignment = Alignment.TopCenter,
        ) { content() }
    }
}

/** A single word, for the look-up: no spaces in it, and at least two letters. */
internal fun lookUpWord(words: String): String? = words.trim().takeIf { it.length >= 2 && it.none(Char::isWhitespace) }

/**
 * What your dictionaries and notes have for [word]: the dictionaries to choose from, each one's
 * entry cut to three lines (all of it a tap away, or from the start when [whole]), words it could be
 * when there's no entry, and notes. Any word in an entry can be tapped to look that up ([onWord]).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WordResults(
    style: HomeStyle,
    word: String,
    lookUp: LookUpWith,
    vault: String?,
    whole: Boolean,
    onWord: (String) -> Unit,
    onRead: (FoundNote) -> Unit,
) {
    val context = LocalContext.current
    val p = style.palette()
    val names by produceState<List<String>?>(null) { value = withContext(Dispatchers.IO) { StarDicts.names(context) } }
    // With several dictionaries, which to look in: all, or one; remembered for next time.
    val repo = remember { app.bedtime.data.Repository.get(context) }
    val saved by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val only = saved?.lookUpDictionary?.takeIf { it in names.orEmpty() }
    val scope = rememberCoroutineScope()
    val notes = lookUp.notes
    val elsewhere = lookUp.elsewhere
    val refugeNotes by produceState(emptyList<app.bedtime.data.Note>(), notes) {
        if (notes) repo.notes.collect { value = it }
    }
    // The vault's notes by name, listed once.
    val vaultFiles by produceState(emptyList<app.bedtime.data.IndexedFile>(), notes, vault) {
        if (notes && vault != null) value = withContext(Dispatchers.IO) { app.bedtime.data.FolderIndex.list(context, android.net.Uri.parse(vault)) { it.endsWith(".md") } }
    }
    // The entry for the word itself, or the words it could be.
    val found by produceState(emptyList<String>() to emptyList<Definition>(), word, only) {
        value = withContext(Dispatchers.IO) { StarDicts.suggest(context, word, only = only) to StarDicts.define(context, word, only) }
    }
    val (suggestions, definitions) = found
    val foundNotes = if (!notes) emptyList() else {
        // Locked, only titles are searched: what's inside a note shouldn't show, not even as a match.
        val later = if (elsewhere != null) " · opens once unlocked" else ""
        refugeNotes.map { it to app.bedtime.data.NoteWidget.title(it.text).ifBlank { "A note" } }
            .filter { (note, title) -> (if (elsewhere != null) title else note.text).contains(word, ignoreCase = true) }
            .take(5).map { (note, title) -> FoundNote(title, "refuge notes$later", refugeId = note.id, refugeText = note.text) } +
            app.bedtime.data.FolderIndex.match(vaultFiles, word, limit = 5, name = app.bedtime.data.FolderIndex::title).map { FoundNote(app.bedtime.data.FolderIndex.title(it), "obsidian$later", vaultFile = it) }
    }
    // Entries opened in full, by dictionary.
    var opened by remember(word) { mutableStateOf(emptySet<String>()) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        when {
            definitions.isNotEmpty() -> definitions.forEach { d ->
                Column {
                    Text(d.dictionary, style = MaterialTheme.typography.labelMedium, color = p.accent)
                    VerbatimText(d.word, style = MaterialTheme.typography.titleMedium, color = p.text)
                    val full = whole || d.dictionary in opened
                    var cut by remember(d) { mutableStateOf(false) }
                    EntryText(d, style, maxLines = if (full) Int.MAX_VALUE else 3, onCut = { cut = it }, onWord = onWord)
                    if (!full && cut) {
                        Text(
                            "the whole entry ›",
                            style = MaterialTheme.typography.labelLarge,
                            color = p.accent,
                            modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(50)).clickable { opened = opened + d.dictionary }.padding(vertical = 2.dp),
                        )
                    }
                }
            }
            suggestions.isNotEmpty() -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { suggestion ->
                    VerbatimText(
                        suggestion,
                        style = MaterialTheme.typography.bodyMedium,
                        color = p.text,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(1.dp, p.faint.copy(alpha = 0.4f), RoundedCornerShape(50))
                            .clickable { onWord(suggestion) }
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
                            elsewhere == null -> onRead(note)
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
    }
}

/** A note read in place: refuge's own, or a file from your vault, read from the phone. */
@Composable
internal fun NoteReader(note: FoundNote, style: HomeStyle, onBack: () -> Unit) {
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

/**
 * An entry, any word of which can be tapped to look it up: links underlined, the rest as they read.
 * At most [maxLines]; [onCut] hears whether that left some out.
 */
@Composable
private fun EntryText(d: Definition, style: HomeStyle, maxLines: Int, onCut: (Boolean) -> Unit, onWord: (String) -> Unit) {
    val p = style.palette()
    val text = remember(d, p) { entryText(d, p) }
    var layout by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
    androidx.compose.foundation.text.BasicText(
        text,
        style = MaterialTheme.typography.bodyMedium.copy(color = p.text),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        onTextLayout = {
            layout = it
            onCut(it.hasVisualOverflow)
        },
        modifier = Modifier.pointerInput(d) {
            detectTapGestures { position ->
                val at = layout?.getOffsetForPosition(position) ?: return@detectTapGestures
                StarDicts.wordAt(d.text, d.marks, at)?.let(onWord)
            }
        },
    )
}
