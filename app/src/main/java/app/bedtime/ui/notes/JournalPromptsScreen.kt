package app.bedtime.ui.notes

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.Journal
import app.bedtime.data.JournalSection
import app.bedtime.data.Repository
import app.bedtime.ui.components.ObsidianTextField
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.launch

/**
 * The microjournal's sections and their prompts, starting from the suggestions: each name, guide and
 * prompt can be changed in place or deleted, whole sections too, and new ones added. "Back to the
 * suggestions" brings the first set back; what you've written stays as it is.
 */
@Composable
fun JournalPromptsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val sections = Journal.sections((settings ?: return).journalSections)
    val save = { changed: List<JournalSection>? -> scope.launch { repo.updateSettings { it.copy(journalSections = changed) } } }
    JournalPromptsContent(sections, onChange = { save(it) }, onRestore = { save(null) }, onBack = onBack)
}

@Composable
internal fun JournalPromptsContent(sections: List<JournalSection>, onChange: (List<JournalSection>) -> Unit, onRestore: () -> Unit, onBack: () -> Unit) {
    val c = Obsidian.colors
    var deleting by remember { mutableStateOf<JournalSection?>(null) }
    var restoring by remember { mutableStateOf(false) }
    deleting?.let { section ->
        val count = section.prompts.size
        AlertDialog(
            onDismissRequest = { deleting = null },
            containerColor = c.bgSecondary,
            title = { Text("Delete ${section.name.lowercase()}" + if (count > 0) " and its ${if (count == 1) "prompt" else "$count prompts"}?" else "?") },
            text = { Text("Entries already written keep it in their titles.", color = c.textMuted) },
            confirmButton = {
                TextButton(onClick = {
                    onChange(Journal.withoutSection(sections, section.id))
                    deleting = null
                }) { Text("Delete", color = c.accentText) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("Cancel", color = c.textMuted) } },
        )
    }
    if (restoring) {
        AlertDialog(
            onDismissRequest = { restoring = false },
            containerColor = c.bgSecondary,
            title = { Text("Back to the suggestions?") },
            text = { Text("Your sections and prompts here are replaced by the suggested ones. Nothing you've written changes.", color = c.textMuted) },
            confirmButton = {
                TextButton(onClick = {
                    onRestore()
                    restoring = false
                }) { Text("Restore", color = c.accentText) }
            },
            dismissButton = { TextButton(onClick = { restoring = false }) { Text("Cancel", color = c.textMuted) } },
        )
    }
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Sections and prompts", onBack = onBack, subtitle = JOURNAL_CREDIT) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Pick sections while writing in the microjournal, and tap a prompt to write from it. These are only a start: change any, delete what you don't use, add your own.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            sections.forEach { section ->
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CommitField(section.name, "Its name", Modifier.weight(1f)) { name ->
                            if (name.isNotBlank()) onChange(Journal.renamed(sections, section.id, name.trim()))
                        }
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "delete this section",
                            tint = c.textFaint,
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(50)).clickable { deleting = section }.padding(10.dp),
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    CommitField(section.guide, "A line on how to go about it", Modifier.fillMaxWidth(), singleLine = false) { guide ->
                        onChange(Journal.withGuide(sections, section.id, guide.trim()))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Prompts", style = MaterialTheme.typography.labelLarge, color = c.accentText)
                    section.prompts.forEachIndexed { i, prompt ->
                        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            CommitField(prompt, "A prompt", Modifier.weight(1f), singleLine = false) { edited ->
                                onChange(if (edited.isBlank()) Journal.withoutPrompt(sections, section.id, i) else Journal.editedPrompt(sections, section.id, i, edited.trim()))
                            }
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "delete this prompt",
                                tint = c.textFaint,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(50)).clickable { onChange(Journal.withoutPrompt(sections, section.id, i)) }.padding(10.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    CommitField("", "Add a prompt", Modifier.fillMaxWidth(), clearAfter = true) { added ->
                        if (added.isNotBlank()) onChange(Journal.withPrompt(sections, section.id, added.trim()))
                    }
                }
            }
            SectionCard(title = "A section of your own") {
                CommitField("", "Its name, then add its prompts", Modifier.fillMaxWidth(), clearAfter = true) { name ->
                    if (name.isNotBlank()) onChange(Journal.withSection(sections, name.trim()))
                }
            }
            Text(
                "Back to the suggestions",
                style = MaterialTheme.typography.labelLarge,
                color = c.accentText,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { restoring = true }.padding(vertical = 8.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * A field kept once you're done with it (Done, or moving on), not as each letter is typed; with
 * [clearAfter], emptied again for the next one.
 */
@Composable
private fun CommitField(value: String, placeholder: String, modifier: Modifier, singleLine: Boolean = true, clearAfter: Boolean = false, onCommit: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    var focused by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    fun commit() {
        if (text != value) onCommit(text)
        if (clearAfter) text = ""
    }
    ObsidianTextField(
        value = text,
        onValueChange = { text = it },
        placeholder = placeholder,
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = if (singleLine) ImeAction.Done else ImeAction.Default),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        modifier = modifier.onFocusChanged { state ->
            if (focused && !state.isFocused) commit()
            focused = state.isFocused
        },
    )
}
