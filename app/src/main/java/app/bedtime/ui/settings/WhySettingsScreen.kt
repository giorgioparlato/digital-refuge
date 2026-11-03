package app.bedtime.ui.settings

import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import app.bedtime.data.AppSettings
import app.bedtime.data.AskWhen
import app.bedtime.data.Repository
import app.bedtime.data.Why
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.ObsidianTextField
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.OptionRow
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.SegmentedChoice
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.launch

/**
 * "Why am I unlocking my phone right now?" after unlocking: when it's asked, the questions it takes
 * in turn, and where the answers are. A limit's own "ask why" is set in that limit.
 */
@Composable
fun WhySettingsScreen(onBack: () -> Unit, onOpenAnswers: () -> Unit, onOpenMoods: () -> Unit = {}) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val s = settings ?: return
    val change = { update: (AppSettings) -> AppSettings -> scope.launch { repo.updateSettings(update) } }
    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Ask why", onBack = onBack, subtitle = "A question after you unlock") }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "After you unlock: one of your questions, a different one each time, and a box to write in. Then put the phone down, or go on: going on is always one tap.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            SectionCard(title = "Ask after unlocking", subtitle = "Every unlock, however you unlock; not again within a few minutes") {
                SegmentedChoice(
                    options = listOf("Never", "Always"),
                    selected = if (s.askOnUnlock == AskWhen.ALWAYS) 1 else 0,
                    onSelect = { i -> change { it.copy(askOnUnlock = if (i == 1) AskWhen.ALWAYS else AskWhen.OFF) } },
                )
            }
            SectionCard {
                OptionRow(Icons.Default.Edit, "Moods", description = moodsWords(s), onClick = onOpenMoods) { Chevron() }
            }
            SectionCard(title = "The questions", subtitle = "Asked in turn. Change any, delete some, add your own") {
                QuestionList(
                    Why.questions(s),
                    changed = s.whyQuestions != null,
                    onChange = { list -> change { it.copy(whyQuestions = list) } },
                    onReset = { change { it.copy(whyQuestions = null) } },
                )
            }
            SectionCard {
                OptionRow(Icons.Default.Edit, "Your answers", description = "In the microjournal: a line in that day's entry, with its time", onClick = onOpenAnswers) { Chevron() }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** "hungry, angry, tired and 4 more", or off. */
internal fun moodsWords(s: AppSettings): String {
    if (!s.moodsOn) return "Off"
    val moods = Why.moods(s)
    return if (moods.size <= 3) moods.joinToString(", ") else moods.take(3).joinToString(", ") + " and ${moods.size - 3} more"
}

/**
 * The moods to tap, when answering why and in the microjournal: on or off, and yours to rename, take
 * out or add to. Insights shows them over time.
 */
@Composable
fun MoodsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val s = settings ?: return
    val change = { update: (AppSettings) -> AppSettings -> scope.launch { repo.updateSettings(update) } }
    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Moods", onBack = onBack, subtitle = "How you feel, a tap away") }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Tap how you feel when answering why, and in the microjournal: each tap is kept with its time. Insights shows them over the month.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            SectionCard {
                OptionRow(Icons.Default.Edit, "Moods to tap") { app.bedtime.ui.components.ObsidianToggle(s.moodsOn, { on -> change { it.copy(moodsOn = on) } }) }
            }
            if (s.moodsOn) {
                SectionCard(title = "How do you feel now?", subtitle = "Your moods to tap after a stretch on the phone, to see how it leaves you") {
                    app.bedtime.ui.components.SubOptionRow("After this long on the phone") {
                        app.bedtime.ui.components.NumberStepper(
                            s.moodAfterMinutes,
                            { v -> change { it.copy(moodAfterMinutes = v) } },
                            0..180,
                            step = 5,
                            presets = listOf(0, 15, 30, 45, 60, 90),
                            title = "minutes on the phone",
                            format = { if (it == 0) "never" else "$it min" },
                        )
                    }
                    Text(
                        "In one go, until the screen goes off. Schedules and blocks can ask once an unlock's break is over, and limits once their time open after the pause is over: each in its own settings. Never more than every 15 minutes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textMuted,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                SectionCard(title = "Your moods", subtitle = "Tap one to rename it; the days it was tapped follow. × takes it out") {
                    MoodList(
                        Why.moods(s),
                        changed = s.moodTags != null,
                        onChange = { list -> change { it.copy(moodTags = list) } },
                        onRename = { old, new -> scope.launch { repo.renameMoodTag(old, new) } },
                        onReset = { change { it.copy(moodTags = null) } },
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** The questions asked before a limit's apps, when its pause is "ask why": reached from the limit. */
@Composable
fun WhyAppQuestionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val s = settings ?: return
    val change = { update: (AppSettings) -> AppSettings -> scope.launch { repo.updateSettings(update) } }
    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Ask why", onBack = onBack, subtitle = "Before a limit's apps") }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "When a limit's pause is “ask why”: “about to open Instagram”, one of these in turn, and a box to write in. Every limit that asks uses these.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            SectionCard(title = "The questions", subtitle = "Asked in turn. Change any, delete some, add your own") {
                QuestionList(
                    Why.appQuestions(s),
                    changed = s.whyAppQuestions != null,
                    onChange = { list -> change { it.copy(whyAppQuestions = list) } },
                    onReset = { change { it.copy(whyAppQuestions = null) } },
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * Moods as chips, each a tap from renaming and with an × to take it out, a box to add one, and, once
 * changed, back to the suggested ones.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun MoodList(moods: List<String>, changed: Boolean, onChange: (List<String>) -> Unit, onRename: (String, String) -> Unit, onReset: () -> Unit) {
    val c = Obsidian.colors
    var renaming by remember { mutableStateOf<String?>(null) }
    renaming?.let { old ->
        var name by remember(old) { mutableStateOf(old) }
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { renaming = null },
            containerColor = c.bgSecondary,
            title = { Text("Rename $old", color = c.textNormal) },
            text = {
                ObsidianTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = old,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = ImeAction.Done),
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    name.trim().lowercase().takeIf { it.isNotEmpty() && it != old }?.let { onRename(old, it) }
                    renaming = null
                }) { Text("Rename", color = c.accentText) }
            },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { renaming = null }) { Text("Cancel", color = c.textMuted) } },
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            moods.forEach { mood ->
                Row(
                    Modifier.clip(RoundedCornerShape(50)).border(1.dp, c.border, RoundedCornerShape(50)).padding(start = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(mood, style = MaterialTheme.typography.labelLarge, color = c.textNormal, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClickLabel = "rename $mood") { renaming = mood }.padding(vertical = 6.dp))
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "take $mood out",
                        tint = c.textFaint,
                        modifier = Modifier.size(32.dp).clip(RoundedCornerShape(50)).clickable { onChange(moods - mood) }.padding(8.dp),
                    )
                }
            }
        }
        Committed("", "Add a mood", clearAfter = true) { typed -> typed.trim().lowercase().takeIf { it.isNotEmpty() && it !in moods }?.let { onChange(moods + it) } }
        if (changed) {
            Text(
                "Back to the suggested moods",
                style = MaterialTheme.typography.labelLarge,
                color = c.accentText,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onReset).padding(vertical = 6.dp),
            )
        }
    }
}

/** Questions to edit in place, delete, or add to; and, once changed, back to the suggested ones. */
@Composable
private fun QuestionList(questions: List<String>, changed: Boolean, onChange: (List<String>) -> Unit, onReset: () -> Unit) {
    val c = Obsidian.colors
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        questions.forEachIndexed { i, question ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Committed(question, "A question", modifier = Modifier.weight(1f), singleLine = false) { typed ->
                    onChange(if (typed.isBlank()) questions.filterIndexed { j, _ -> j != i } else questions.mapIndexed { j, q -> if (j == i) typed.trim() else q })
                }
                // The last one stays: with none there'd be nothing to ask.
                if (questions.size > 1) {
                    Icon(
                        Icons.Default.Clear,
                        contentDescription = "delete this question",
                        tint = c.textFaint,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(50)).clickable { onChange(questions.filterIndexed { j, _ -> j != i }) }.padding(10.dp),
                    )
                }
            }
        }
        Committed("", "Add a question", clearAfter = true) { typed -> if (typed.isNotBlank()) onChange(questions + typed.trim()) }
        if (changed) {
            Text(
                "Back to the suggested questions",
                style = MaterialTheme.typography.labelLarge,
                color = c.accentText,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onReset).padding(vertical = 6.dp),
            )
        }
    }
}

/** A field kept once you're done with it (Done, or moving on), not as each letter is typed. */
@Composable
private fun Committed(
    value: String,
    placeholder: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    singleLine: Boolean = true,
    clearAfter: Boolean = false,
    onCommit: (String) -> Unit,
) {
    var text by remember(value) { mutableStateOf(value) }
    var focused by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    ObsidianTextField(
        value = text,
        onValueChange = { text = it },
        placeholder = placeholder,
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = if (singleLine) ImeAction.Done else ImeAction.Default),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        modifier = modifier.onFocusChanged { state ->
            if (focused && !state.isFocused && text != value) {
                onCommit(text)
                if (clearAfter) text = ""
            }
            focused = state.isFocused
        },
    )
}
