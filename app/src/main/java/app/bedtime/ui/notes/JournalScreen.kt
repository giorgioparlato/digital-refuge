package app.bedtime.ui.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
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

/** The credits for the suggested sections, on their settings page, each with where it's from. */
internal val JOURNAL_CREDITS = listOf(
    "The suggested sections and questions come from Struthless's video “The Journalling Techniques that Changed My Life” ›" to "https://www.youtube.com/watch?v=dArgOrm98Bk",
    "Self compassion's three steps are Kristin Neff's self-compassion journal ›" to "https://self-compassion.org/exercises/exercise-6-self-compassion-journal/",
)

/**
 * The microjournal: an entry a day, newest first, today's always on top (to start, until it exists).
 * An entry opens in the note editor, with your sections' prompts a toggle away.
 */
@Composable
internal fun JournalContent(onBack: () -> Unit, openToday: Boolean = false, openDay: Long? = null) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val entries by repo.journal.collectAsStateWithLifecycle(initialValue = null)
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val sections = Journal.sections(settings?.journalSections)
    val today = LocalDate.now()
    var open by rememberSaveable { mutableStateOf(openDay ?: if (openToday) today.toEpochDay() else null) }
    val all = entries ?: return
    open?.let { day ->
        val entry = all.firstOrNull { it.day == day }
        JournalEditor(
            entry ?: JournalEntry(day, ""),
            sections,
            today,
            onSave = { text, aims -> scope.launch { repo.saveJournal(day, text, aims) } },
            onDone = { open = null },
            prompts = settings?.journalPrompts != false,
            moods = app.bedtime.data.Why.moods(settings ?: app.bedtime.data.AppSettings()),
            onMood = { mood, on -> scope.launch { if (on) repo.addMood(day, mood) else repo.removeLatestMood(day, mood) } },
            onAddMood = { mood -> scope.launch { repo.addMoodTag(mood) } },
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

/**
 * A day's entry in the note editor, a line at a time: today's opens on a fresh line begun at the time,
 * each new line timed in turn, and a mood tapped goes on the line you're on. The prompts stay a button
 * away, when you want them at all.
 */
@Composable
private fun JournalEditor(
    entry: JournalEntry,
    sections: List<JournalSection>,
    today: LocalDate,
    onSave: (String, List<String>) -> Unit,
    onDone: () -> Unit,
    /** The prompts a button away; off, no button at all. */
    prompts: Boolean = true,
    /** How you feel, each tap kept with its time: on (true) or taken off the line again. */
    moods: List<String> = emptyList(),
    onMood: (String, Boolean) -> Unit = { _, _ -> },
    /** A mood of your own, kept among them from now on. */
    onAddMood: (String) -> Unit = {},
) {
    val c = Obsidian.colors
    var aims by remember { mutableStateOf(entry.aims) }
    var prompting by rememberSaveable { mutableStateOf(false) }
    val clock = remember { java.time.format.DateTimeFormatter.ofPattern("HH:mm") }
    val now = { java.time.LocalTime.now().format(clock) }
    // Only today's opens on a fresh line; another day's is read, and changed, as it is.
    val isToday = entry.day == today.toEpochDay()
    NoteEditor(
        initial = remember { if (isToday) Journal.opened(entry.text, now()).first else entry.text },
        isNew = entry.text.isEmpty(),
        onSave = { text -> Journal.tidy(text).let { kept -> if (kept != entry.text || aims != entry.aims) onSave(kept, aims) } },
        onDone = { text ->
            onSave(Journal.tidy(text), aims)
            onDone()
        },
        onDelete = null,
        title = Journal.title(entry.copy(aims = aims), today),
        actions = {
            if (prompts) TextButton(onClick = { prompting = !prompting }) { Text(if (prompting) "Hide prompts" else "Prompts", color = c.accentText) }
        },
        panel = if (!prompting && moods.isEmpty()) null else { value, edit ->
            val tap = { mood: String, all: List<String> ->
                val (text, cursor, on) = Journal.withMood(value.text, value.selection.start, mood, all, now())
                edit { androidx.compose.ui.text.input.TextFieldValue(text, androidx.compose.ui.text.TextRange(cursor)) }
                onMood(mood, on)
            }
            Column(Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 8.dp)) {
                if (moods.isNotEmpty()) {
                    app.bedtime.ui.blocked.MoodChips(
                        moods,
                        Journal.moodsAt(value.text, value.selection.start, moods),
                        onAdd = { mood ->
                            onAddMood(mood)
                            tap(mood, moods + mood)
                        },
                        onTap = { mood -> tap(mood, moods) },
                    )
                    if (prompting) Spacer(Modifier.height(12.dp))
                }
                if (prompting && prompts) {
                    JournalPrompts(
                        sections, aims, onPick = { aims = it },
                        onPrompt = { prompt ->
                            edit { v ->
                                val (text, cursor) = Journal.prompted(v.text, v.selection.start, prompt, now())
                                androidx.compose.ui.text.input.TextFieldValue(text, androidx.compose.ui.text.TextRange(cursor))
                            }
                        },
                        text = c.textNormal, faint = c.textFaint, accent = c.accent, background = c.bgPrimary,
                    )
                }
            }
        },
        saveWith = aims,
        onType = if (!isToday) null else { old, new ->
            Journal.typed(old.text, new.text, new.selection.start, now())?.let { (text, cursor) ->
                androidx.compose.ui.text.input.TextFieldValue(text, androidx.compose.ui.text.TextRange(cursor))
            }
        },
        focus = isToday,
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
                    // The question, and under it an example of an answer, for a sense of what it asks.
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClickLabel = "add this prompt") { onPrompt(prompt) }.padding(vertical = 6.dp)) {
                        Text("› $prompt", style = MaterialTheme.typography.bodyMedium, color = accent)
                        section.hints[prompt]?.let { hint ->
                            Text(hint, style = MaterialTheme.typography.bodySmall.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), color = faint, modifier = Modifier.padding(start = 12.dp, top = 2.dp))
                        }
                    }
                }
            }
        }
        if (sections.isEmpty()) Text("No sections yet: add some under settings › notes › microjournal.", style = MaterialTheme.typography.bodySmall, color = faint)
    }
}
