package app.bedtime.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.Journal
import app.bedtime.data.JournalEntry
import app.bedtime.data.JournalSection
import app.bedtime.data.Repository
import app.bedtime.ui.components.CardRadius
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.SwipeToDelete
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.VerbatimText
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.launch
import java.time.LocalDate

/** The credit for the suggested sections, wherever the prompts show. */
internal const val JOURNAL_CREDIT = "Suggested sections after the journalling system by Struthless"

/**
 * The microjournal: an entry a day, newest first, today's always on top (to start, until it exists).
 * An entry opens in the note editor, with your sections' prompts a toggle away.
 */
@Composable
internal fun JournalContent(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val entries by repo.journal.collectAsStateWithLifecycle(initialValue = null)
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val sections = Journal.sections(settings?.journalSections)
    val today = LocalDate.now()
    var open by rememberSaveable { mutableStateOf<Long?>(null) }
    val all = entries ?: return
    open?.let { day ->
        val entry = all.firstOrNull { it.day == day }
        JournalEditor(
            entry ?: JournalEntry(day, ""),
            sections,
            today,
            onSave = { text, aims -> scope.launch { repo.saveJournal(day, text, aims) } },
            onDone = { open = null },
        )
        return
    }
    BackHandler(onBack = onBack)
    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Microjournal", onBack = onBack, subtitle = "A few lines a day, kept on this phone") }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding).padding(horizontal = PageGutter), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (all.none { it.day == today.toEpochDay() }) {
                item(key = "today") {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(CardRadius)).background(c.bgSecondary).clickable { open = today.toEpochDay() }.padding(16.dp)) {
                        Text(Journal.title(JournalEntry(today.toEpochDay(), ""), today), style = MaterialTheme.typography.titleSmall, color = c.textNormal)
                        Text("Write today's ›", style = MaterialTheme.typography.bodyMedium, color = c.accentText, modifier = Modifier.padding(top = 4.dp))
                    }
                }
            }
            items(Journal.entries(all), key = { it.day }) { entry ->
                SwipeToDelete(onDelete = { scope.launch { repo.saveJournal(entry.day, "", emptyList()) } }) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(CardRadius)).background(c.bgSecondary).clickable(onClickLabel = "write") { open = entry.day }.padding(16.dp)) {
                        Text(Journal.title(entry, today), style = MaterialTheme.typography.titleSmall, color = c.textNormal)
                        VerbatimText(
                            Markdown.render(entry.text, c.textFaint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textMuted,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

/** A day's entry in the note editor, with the prompts above it once toggled on; the sections picked are the entry's own. */
@Composable
private fun JournalEditor(entry: JournalEntry, sections: List<JournalSection>, today: LocalDate, onSave: (String, List<String>) -> Unit, onDone: () -> Unit) {
    val c = Obsidian.colors
    var aims by remember { mutableStateOf(entry.aims) }
    var prompting by rememberSaveable { mutableStateOf(false) }
    NoteEditor(
        initial = remember { entry.text },
        isNew = entry.text.isEmpty(),
        onSave = { text -> if (text != entry.text || aims != entry.aims) onSave(text, aims) },
        onDone = { text ->
            onSave(text, aims)
            onDone()
        },
        onDelete = null,
        title = Journal.title(entry.copy(aims = aims), today),
        actions = {
            TextButton(onClick = { prompting = !prompting }) { Text(if (prompting) "Hide prompts" else "Prompts", color = c.accentText) }
        },
        panel = if (!prompting) null else { insert ->
            Column(Modifier.heightIn(max = 280.dp).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 8.dp)) {
                JournalPrompts(sections, aims, onPick = { aims = it }, onPrompt = insert, text = c.textNormal, faint = c.textFaint, accent = c.accent, background = c.bgPrimary)
            }
        },
        saveWith = aims,
    )
}

/**
 * Your sections as chips to pick from; for each one picked, its guide and its prompts, a tap putting
 * one into what you're writing. The same here as in the quick note, in the colours it's given.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun JournalPrompts(
    sections: List<JournalSection>,
    picked: List<String>,
    onPick: (List<String>) -> Unit,
    onPrompt: (String) -> Unit,
    text: Color,
    faint: Color,
    accent: Color,
    background: Color,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            sections.forEach { section ->
                val on = section.name in picked
                VerbatimText(
                    section.name.lowercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (on) text else faint,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (on) accent.copy(alpha = 0.22f) else background)
                        .border(1.dp, if (on) accent else faint.copy(alpha = 0.4f), RoundedCornerShape(50))
                        .clickable { onPick(if (on) picked - section.name else picked + section.name) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }
        sections.filter { it.name in picked }.forEach { section ->
            Column {
                if (section.guide.isNotBlank()) {
                    Text("${section.name}: ${section.guide}", style = MaterialTheme.typography.bodySmall, color = faint)
                }
                section.prompts.forEach { prompt ->
                    Text(
                        "› $prompt",
                        style = MaterialTheme.typography.bodyMedium,
                        color = accent,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClickLabel = "add this prompt") { onPrompt(prompt) }.padding(vertical = 6.dp),
                    )
                }
            }
        }
        if (sections.isEmpty()) Text("No sections yet: add some under settings › notes › microjournal.", style = MaterialTheme.typography.bodySmall, color = faint)
        Text(JOURNAL_CREDIT, style = MaterialTheme.typography.labelSmall, color = faint)
    }
}
