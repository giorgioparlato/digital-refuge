package app.bedtime.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.CoverMessages
import app.bedtime.data.CustomMessage
import app.bedtime.data.Repository
import app.bedtime.ui.components.ObsidianTextField
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.launch

/**
 * The words a cover can say when you open something a session or limit holds back: one list, the
 * suggested ones and yours, each to reword, delete, or keep out of the mix of "a different one each
 * time". Which one a cover uses is chosen in each schedule, block and limit.
 */
@Composable
fun CoverMessagesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val s = settings ?: return
    CoverMessagesContent(
        messages = CoverMessages.all(s),
        edited = s.coverMessagesEdited,
        onChange = { transform -> scope.launch { repo.updateSettings { CoverMessages.edited(it, transform) } } },
        onReset = { scope.launch { repo.updateSettings { CoverMessages.reset(it) } } },
        onBack = onBack,
    )
}

@Composable
internal fun CoverMessagesContent(
    messages: List<CustomMessage>,
    onChange: ((List<CustomMessage>) -> List<CustomMessage>) -> Unit,
    onBack: () -> Unit,
    edited: Boolean = false,
    onReset: () -> Unit = {},
) {
    val c = Obsidian.colors
    var draft by rememberSaveable { mutableStateOf("") }
    var resetting by remember { mutableStateOf(false) }
    if (resetting) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { resetting = false },
            containerColor = c.bgSecondary,
            title = { Text("Back to the suggested ones?") },
            text = { Text("This list is replaced by the suggested messages; any you wrote go.", color = c.textMuted) },
            confirmButton = {
                TextButton(onClick = {
                    resetting = false
                    onReset()
                }) { Text("Replace", color = c.accentText) }
            },
            dismissButton = { TextButton(onClick = { resetting = false }) { Text("Cancel", color = c.textMuted) } },
        )
    }
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Cover messages", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "What a cover says when you open an app a session or a limit holds back. Each schedule, block and limit picks one, or a different one each time from those in the mix.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            SectionCard(title = "The messages", subtitle = "Reword any, delete some, add your own. Switched on, it's in the mix") {
                messages.forEachIndexed { i, message ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        MessageField(message.text, Modifier.weight(1f).padding(end = 8.dp)) { typed ->
                            onChange { list -> if (typed.isBlank()) list.filterIndexed { j, _ -> j != i } else list.mapIndexed { j, m -> if (j == i) m.copy(text = typed.trim()) else m } }
                        }
                        ObsidianToggle(message.inRandom, { on -> onChange { list -> list.mapIndexed { j, m -> if (j == i) m.copy(inRandom = on) else m } } })
                        Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClickLabel = "delete") { onChange { list -> list.filterIndexed { j, _ -> j != i } } }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Clear, contentDescription = "delete", tint = c.textFaint, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                ObsidianTextField(value = draft, onValueChange = { draft = it }, label = "Write your own", modifier = Modifier.padding(top = 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        enabled = draft.isNotBlank(),
                        onClick = {
                            val text = draft.trim()
                            if (messages.none { it.text == text }) onChange { it + CustomMessage(text) }
                            draft = ""
                        },
                    ) { Text("Add", color = if (draft.isNotBlank()) c.accentText else c.textFaint, fontWeight = FontWeight.SemiBold) }
                }
            }
            if (edited) {
                Text(
                    "Back to the suggested ones",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.accentText,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { resetting = true }.padding(vertical = 8.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** A message's words, kept once you're done with them (Done, or moving on); emptied, it goes. */
@Composable
private fun MessageField(value: String, modifier: Modifier, onCommit: (String) -> Unit) {
    var text by remember(value) { mutableStateOf(value) }
    var focused by remember { mutableStateOf(false) }
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    ObsidianTextField(
        value = text,
        onValueChange = { text = it },
        singleLine = false,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focus.clearFocus() }),
        modifier = modifier.onFocusChanged { state ->
            if (focused && !state.isFocused && text != value) onCommit(text)
            focused = state.isFocused
        },
    )
}
