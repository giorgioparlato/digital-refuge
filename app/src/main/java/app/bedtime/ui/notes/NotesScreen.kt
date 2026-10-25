package app.bedtime.ui.notes

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

/** One note as plain Markdown, saved when you go back. */
@Composable
private fun NoteEditor(initial: String, onDone: (String) -> Unit, onDelete: (() -> Unit)?) {
    val c = Obsidian.colors
    var text by rememberSaveable { mutableStateOf(initial) }
    BackHandler { onDone(text) }
    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            ObsidianTopBar(if (onDelete == null) "New note" else "Note", onBack = { onDone(text) }) {
                if (onDelete != null) {
                    IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "delete", tint = c.textMuted) }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).imePadding().padding(horizontal = PageGutter)) {
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.textNormal),
                cursorBrush = SolidColor(c.accent),
                modifier = Modifier.fillMaxSize(),
                decorationBox = { field ->
                    Box(contentAlignment = Alignment.TopStart) {
                        if (text.isEmpty()) Text("# A title\n\n- a list\n- [ ] a to-do", style = MaterialTheme.typography.bodyLarge, color = c.textFaint)
                        field()
                    }
                },
            )
        }
    }
}

/** "25 oct 2026, 22:41": when a note was last written. */
private fun whenWritten(context: android.content.Context, at: Long): String =
    DateTimeFormatter.ofPattern("d MMM yyyy", Locale.getDefault()).format(Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault())) +
        ", " + formatTime(context, at)
