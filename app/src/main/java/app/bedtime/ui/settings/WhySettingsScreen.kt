package app.bedtime.ui.settings

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
fun WhySettingsScreen(onBack: () -> Unit, onOpenAnswers: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val s = settings ?: return
    val change = { update: (AppSettings) -> AppSettings -> scope.launch { repo.updateSettings(update) } }
    val c = Obsidian.colors
    val modes = AskWhen.entries
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
            SectionCard(title = "Ask after unlocking", subtitle = "“Sessions”: only while a session runs. Never again within a few minutes") {
                // Short, so each fits on one line at any text size.
                SegmentedChoice(options = listOf("Never", "Sessions", "Always"), selected = modes.indexOf(s.askOnUnlock), onSelect = { i -> change { it.copy(askOnUnlock = modes[i]) } })
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
                OptionRow(Icons.Default.Edit, "Your answers", description = "Whatever you write, in one note at the top of your notes", onClick = onOpenAnswers) { Chevron() }
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
