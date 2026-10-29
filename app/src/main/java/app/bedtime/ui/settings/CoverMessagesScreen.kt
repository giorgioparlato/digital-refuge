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
 * The words a cover can say when you open something a session or limit holds back: the app's own,
 * and yours, each in the mix of "a different one each time" or not. Which one a cover uses is chosen
 * in each schedule, block and limit.
 */
@Composable
fun CoverMessagesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    CoverMessagesContent(
        custom = settings?.customMessages.orEmpty(),
        onChange = { transform -> scope.launch { repo.updateSettings { it.copy(customMessages = transform(it.customMessages)) } } },
        onBack = onBack,
    )
}

@Composable
internal fun CoverMessagesContent(custom: List<CustomMessage>, onChange: ((List<CustomMessage>) -> List<CustomMessage>) -> Unit, onBack: () -> Unit) {
    val c = Obsidian.colors
    var draft by rememberSaveable { mutableStateOf("") }
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Cover messages", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "What a cover says when you open an app a session or a limit holds back. Each schedule, block and limit picks one, or a different one each time.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            SectionCard(title = "Yours", subtitle = "Switched on, it's in the mix of a different one each time") {
                if (custom.isEmpty()) Text("None yet.", style = MaterialTheme.typography.bodyMedium, color = c.textFaint, modifier = Modifier.padding(bottom = 8.dp))
                custom.forEach { message ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("“${message.text}”", style = MaterialTheme.typography.bodyLarge, color = c.textNormal, modifier = Modifier.weight(1f).padding(end = 8.dp))
                        ObsidianToggle(message.inRandom, { on -> onChange { list -> list.map { if (it == message) it.copy(inRandom = on) else it } } })
                        Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClickLabel = "delete") { onChange { it - message } }, contentAlignment = Alignment.Center) {
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
                            if (custom.none { it.text == text }) onChange { it + CustomMessage(text) }
                            draft = ""
                        },
                    ) { Text("Add", color = if (draft.isNotBlank()) c.accentText else c.textFaint, fontWeight = FontWeight.SemiBold) }
                }
            }
            SectionCard(title = "The app's own", subtitle = "Always in the mix") {
                CoverMessages.presets.forEach { text ->
                    Text("“$text”", style = MaterialTheme.typography.bodyLarge, color = c.textMuted, modifier = Modifier.padding(vertical = 6.dp))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
