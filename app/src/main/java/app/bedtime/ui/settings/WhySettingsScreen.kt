package app.bedtime.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
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
 * "Why am I unlocking my phone right now?": when it's asked (after unlocking, before distracting and
 * limited apps), its words, and where the answers are. After Struthless, credited here.
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
    val modeNames = listOf("Off", "During sessions", "Always")
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Ask why", onBack = onBack, subtitle = "A moment before you pick up the phone") }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "A question, a few words if you want, and how you feel: then put it down, or go on. Never a wall: going on is always one tap.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            SectionCard(title = "After unlocking", subtitle = "Every unlock, however you unlock; not again within a few minutes") {
                SegmentedChoice(options = modeNames, selected = modes.indexOf(s.askOnUnlock), onSelect = { i -> change { it.copy(askOnUnlock = modes[i]) } })
            }
            SectionCard(title = "Before distracting and limited apps", subtitle = "Not again for the same app within 10 minutes") {
                SegmentedChoice(options = modeNames, selected = modes.indexOf(s.askOnApps), onSelect = { i -> change { it.copy(askOnApps = modes[i]) } })
            }
            SectionCard(title = "The words", subtitle = "Leave one empty for the suggested words") {
                Committed(s.whyQuestion.orEmpty(), Why.QUESTION) { typed -> change { it.copy(whyQuestion = typed.trim().ifEmpty { null }) } }
                Spacer(Modifier.height(10.dp))
                Committed(s.whyNudge.orEmpty(), Why.NUDGE, singleLine = false) { typed -> change { it.copy(whyNudge = typed.trim().ifEmpty { null }) } }
                Spacer(Modifier.height(10.dp))
                Committed(Why.tags(s).joinToString(", "), "Tags, with commas") { typed ->
                    val tags = typed.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                    change { it.copy(whyTags = if (tags == Why.TAGS) null else tags) }
                }
                Text("Before an app, the question names it: “why am I opening Instagram right now?”", style = MaterialTheme.typography.bodySmall, color = c.textFaint, modifier = Modifier.padding(top = 8.dp))
            }
            SectionCard {
                OptionRow(Icons.Default.Edit, "Your answers", description = "All in one note, pinned at the top of your notes", onClick = onOpenAnswers) { Chevron() }
            }
            Text(
                "The question, the four tags and the nudge come from Struthless.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textFaint,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** A field kept once you're done with it (Done, or moving on), not as each letter is typed. */
@Composable
private fun Committed(value: String, placeholder: String, singleLine: Boolean = true, onCommit: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    var focused by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    ObsidianTextField(
        value = text,
        onValueChange = { text = it },
        placeholder = placeholder,
        singleLine = singleLine,
        keyboardOptions = KeyboardOptions(imeAction = if (singleLine) ImeAction.Done else ImeAction.Default),
        keyboardActions = KeyboardActions(onDone = { focus.clearFocus() }),
        modifier = Modifier.fillMaxWidth().onFocusChanged { state ->
            if (focused && !state.isFocused && text != value) onCommit(text)
            focused = state.isFocused
        },
    )
}
