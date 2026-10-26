package app.bedtime.ui.notes

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
fun NotesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val notes by repo.notes.collectAsStateWithLifecycle(initialValue = null)
    NotesContent(
        notes = notes ?: return,
        onSave = { id, text ->
            scope.launch {
                when {
                    id == null -> if (text.isNotBlank()) repo.addNote(text)
                    text.isBlank() -> repo.deleteNote(id)
                    else -> repo.editNote(id, text)
                }
            }
        },
        onDelete = { id -> scope.launch { repo.deleteNote(id) } },
        onBack = onBack,
    )
}

/** The list of notes, or one open to edit. [onSave] with a null id is a new note. */
@Composable
internal fun NotesContent(
    notes: List<Note>,
    onSave: (id: String?, text: String) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
    initialOpen: String? = null,
) {
    // The note open to edit, by id; "" for a new one.
    var open by rememberSaveable { mutableStateOf(initialOpen) }
    val editing = open
    if (editing != null) {
        val note = notes.firstOrNull { it.id == editing }
        NoteEditor(
            initial = note?.text.orEmpty(),
            onDone = { text ->
                if (text != note?.text.orEmpty()) onSave(note?.id, text)
                open = null
            },
            onDelete = note?.let { { onDelete(it.id); open = null } },
        )
        return
    }
    val c = Obsidian.colors
    Scaffold(
        containerColor = c.bgPrimary,
        topBar = { ObsidianTopBar("Notes", onBack = onBack, subtitle = "Written in Markdown, kept on this phone") },
        floatingActionButton = { AddCircleButton("new note", onClick = { open = "" }) },
    ) { padding ->
        if (notes.isEmpty()) {
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
            items(notes, key = { it.id }) { note ->
                SwipeToDelete(onDelete = { onDelete(note.id) }) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(CardRadius))
                            .background(c.bgSecondary)
                            .clickable(onClickLabel = "edit") { open = note.id }
                            .padding(16.dp),
                    ) {
                        VerbatimText(
                            Markdown.render(note.text, c.textFaint),
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
                }
            }
        }
    }
}

/** One note, styled as Markdown as you type, with a few marks a tap away; saved when you go back. */
@Composable
private fun NoteEditor(initial: String, onDone: (String) -> Unit, onDelete: (() -> Unit)?) {
    val c = Obsidian.colors
    var value by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    BackHandler { onDone(value.text) }
    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            ObsidianTopBar(if (onDelete == null) "New note" else "Note", onBack = { onDone(value.text) }) {
                if (onDelete != null) {
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "delete", tint = c.textMuted) }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            BasicTextField(
                value = value,
                onValueChange = { value = Markdown.continueList(value, it) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.textNormal),
                cursorBrush = SolidColor(c.accent),
                visualTransformation = remember(c.textFaint, c.accent) { MarkdownTransformation(c.textFaint, c.accent) },
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = PageGutter),
                decorationBox = { field ->
                    Box(contentAlignment = Alignment.TopStart) {
                        if (value.text.isEmpty()) VerbatimText("# A title\n\n- a list\n- [ ] a to-do", style = MaterialTheme.typography.bodyLarge, color = c.textFaint)
                        field()
                    }
                },
            )
            MarkdownBar { change -> value = change(value) }
        }
    }
}

/** The marks Obsidian keeps above its keyboard: brackets, bold, italic, headings, a list, a to-do, and indenting. */
@Composable
private fun MarkdownBar(onEdit: ((TextFieldValue) -> TextFieldValue) -> Unit) {
    val c = Obsidian.colors
    Row(
        Modifier.fillMaxWidth().background(c.bgSecondary).horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
private fun BarKey(label: String, description: String, bold: Boolean = false, italic: Boolean = false, onClick: () -> Unit) {
    val c = Obsidian.colors
    Box(
        Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).clickable(onClickLabel = description, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        VerbatimText(
            label,
            color = c.textNormal,
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
