package app.bedtime.ui.notes

import java.time.LocalDateTime
import androidx.compose.ui.text.font.FontFamily
import app.bedtime.ui.components.VerbatimText
import app.bedtime.ui.components.SegmentedChoice
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.data.NoteStyle
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.NoteTarget
import app.bedtime.data.Notes
import app.bedtime.data.Repository
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.ObsidianTextField
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.OptionRow
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.launch

/** Where the minimal home's search sends notes: your files, or the app's notes. Events are on the search page. */
@Composable
fun NoteTargetsScreen(onBack: () -> Unit, onOpenNotes: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    // A file being named and set up: just picked (new) or tapped to change. The original, to replace.
    var draft by remember { mutableStateOf<NoteTarget?>(null) }
    var original by remember { mutableStateOf<NoteTarget?>(null) }
    val pickFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val kept = runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }.isSuccess
        if (!kept) {
            Toast.makeText(context, "that file can't be written to from here", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        original = null
        draft = NoteTarget(displayName(context, uri)?.substringBeforeLast('.').orEmpty(), uri.toString())
    }
    // Today's note lives in a folder: the folder is what's picked, once.
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val kept = runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }.isSuccess
        if (!kept) {
            Toast.makeText(context, "that folder can't be written to from here", Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        original = null
        draft = NoteTarget("today", uri.toString(), daily = true)
    }
    val current = settings ?: return
    draft?.let { target ->
        TargetDialog(
            target = target,
            isNew = original == null,
            onChange = { draft = it },
            onDone = {
                val done = target.copy(name = target.name.trim())
                val was = original
                scope.launch {
                    repo.updateSettings { s ->
                        val list = if (was == null) s.noteTargets + done else s.noteTargets.map { if (it == was) done else it }
                        s.copy(noteTargets = list.take(Notes.MAX_TARGETS))
                    }
                }
                draft = null
            },
            onCancel = { draft = null },
        )
    }
    NoteTargetsContent(
        targets = current.noteTargets,
        onAdd = { pickFile.launch(arrayOf("text/*", "application/octet-stream")) },
        onAddDaily = { pickFolder.launch(null) },
        onEdit = { target ->
            original = target
            draft = target
        },
        onRemove = { target ->
            runCatching { context.contentResolver.releasePersistableUriPermission(Uri.parse(target.uri), Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
            scope.launch { repo.updateSettings { it.copy(noteTargets = it.noteTargets - target) } }
        },
        onOpenNotes = onOpenNotes,
        onBack = onBack,
    )
}

@Composable
internal fun NoteTargetsContent(
    targets: List<NoteTarget>,
    onAdd: () -> Unit,
    onAddDaily: () -> Unit,
    onEdit: (NoteTarget) -> Unit,
    onRemove: (NoteTarget) -> Unit,
    onOpenNotes: () -> Unit,
    onBack: () -> Unit,
) {
    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Quick notes", onBack = onBack, subtitle = "From the minimal home's search") }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp),
        ) {
            Text(
                "Type in the minimal home's search, then pick where it goes: the app's own notes, one of your files, or your calendar if it says when.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            Spacer(Modifier.height(16.dp))
            SectionCard(title = "Refuge notes") {
                OptionRow(Icons.Default.Edit, "Your notes", description = "Kept on this phone, written in Markdown", onClick = onOpenNotes) { Chevron() }
            }
            Spacer(Modifier.height(16.dp))
            SectionCard(
                title = "Your files",
                subtitle = "Up to ${Notes.MAX_TARGETS}. Each note is added at the end; tap one to change how",
            ) {
                targets.forEach { target ->
                    val example = Notes.line("your words", SAMPLE_TIME, target.style, target.withDate)
                    OptionRow(
                        if (target.daily) Icons.Default.DateRange else Icons.Default.List,
                        target.name,
                        description = if (target.daily) "Today's note, e.g. ${Notes.dailyPath(target.dailyFormat, SAMPLE_TIME.toLocalDate())} · adds “$example”" else "Adds “$example”",
                        onClick = { onEdit(target) },
                    ) {
                        IconButton(onClick = { onRemove(target) }) { Icon(Icons.Default.Close, contentDescription = "remove", tint = c.textFaint) }
                    }
                }
                if (targets.size < Notes.MAX_TARGETS) {
                    OptionRow(Icons.Default.Add, "Add a file", description = "Pick a note, then name it, e.g. scratchpad or movies list", onClick = onAdd) { Chevron() }
                    OptionRow(Icons.Default.DateRange, "Add today's daily note", description = "Pick your daily-notes folder; today's note is made if it isn't there", onClick = onAddDaily) { Chevron() }
                }
                Text(
                    "For Obsidian, pick the note inside your vault. Vaults in Documents work; one kept in Obsidian's own app folder can't be reached by other apps.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textFaint,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.height(32.dp))
        }
    }
}

/** An example moment for the previews of how a note will be written. */
private val SAMPLE_TIME: LocalDateTime = LocalDateTime.of(2026, 10, 25, 22, 41)

/** A file's name, and how each quick note is written into it, with a preview line. */
@Composable
private fun TargetDialog(target: NoteTarget, isNew: Boolean, onChange: (NoteTarget) -> Unit, onDone: () -> Unit, onCancel: () -> Unit) {
    val c = Obsidian.colors
    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = c.bgSecondary,
        title = { Text(if (!isNew) target.name else if (target.daily) "Add today's daily note" else "Add a file") },
        text = {
            Column {
                Text("How it reads in the search: “add to …”", style = MaterialTheme.typography.bodySmall, color = c.textFaint)
                Spacer(Modifier.height(8.dp))
                ObsidianTextField(value = target.name, onValueChange = { onChange(target.copy(name = it)) }, label = if (target.daily) "e.g. today" else "e.g. scratchpad")
                if (target.daily) {
                    Spacer(Modifier.height(16.dp))
                    Text("The daily note's name", style = MaterialTheme.typography.labelLarge, color = c.accentText)
                    Text(
                        "As in Obsidian's daily notes settings; folders work too, like YYYY/MM/YYYY-MM-DD",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textFaint,
                    )
                    Spacer(Modifier.height(8.dp))
                    ObsidianTextField(value = target.dailyFormat, onValueChange = { onChange(target.copy(dailyFormat = it)) }, label = "YYYY-MM-DD")
                    VerbatimText(
                        "Today: " + Notes.dailyPath(target.dailyFormat, java.time.LocalDate.now()),
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textMuted,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text("Each note is written as", style = MaterialTheme.typography.labelLarge, color = c.accentText)
                Spacer(Modifier.height(8.dp))
                SegmentedChoice(
                    options = listOf("A list item", "A to-do", "Plain"),
                    selected = target.style.ordinal,
                    onSelect = { onChange(target.copy(style = NoteStyle.entries[it])) },
                )
                OptionRow(Icons.Default.DateRange, "Date and time first") {
                    ObsidianToggle(target.withDate, { onChange(target.copy(withDate = it)) })
                }
                VerbatimText(
                    Notes.line("Watch Paterson", SAMPLE_TIME, target.style, target.withDate),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textMuted,
                    fontFamily = FontFamily.Monospace,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDone, enabled = target.name.isNotBlank()) {
                Text(if (isNew) "Add" else "Save", color = if (target.name.isNotBlank()) c.accentText else c.textFaint, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("Cancel", color = c.textMuted) } },
    )
}

private fun displayName(context: android.content.Context, uri: Uri): String? = runCatching {
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
}.getOrNull()
