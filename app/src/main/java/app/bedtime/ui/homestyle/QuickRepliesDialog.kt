package app.bedtime.ui.homestyle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.bedtime.ui.components.ObsidianTextField
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian

/** The ready-made replies offered under a message on the lock screen: add, or take one away. */
@Composable
internal fun QuickRepliesDialog(replies: List<String>, onChange: (List<String>) -> Unit, onDismiss: () -> Unit) {
    val c = Obsidian.colors
    var draft by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.bgSecondary,
        title = { Text("Quick replies") },
        text = {
            LazyColumn(Modifier.heightIn(max = 480.dp)) {
                item {
                    Text(
                        "One tap sends one from the lock screen, after your unlock. You can always type instead.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textFaint,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                replies.forEach { reply ->
                    item {
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("“$reply”", style = MaterialTheme.typography.bodyLarge, color = c.textNormal, modifier = Modifier.weight(1f))
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "delete",
                                tint = c.textFaint,
                                modifier = Modifier.size(32.dp).clip(CircleShape).clickable { onChange(replies - reply) }.padding(7.dp),
                            )
                        }
                    }
                }
                item {
                    ObsidianTextField(value = draft, onValueChange = { draft = it }, label = "A new reply", modifier = Modifier.padding(top = 8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            enabled = draft.isNotBlank(),
                            onClick = {
                                val text = draft.trim()
                                if (text !in replies) onChange(replies + text)
                                draft = ""
                            },
                        ) { Text("Add", color = if (draft.isNotBlank()) c.accentText else c.textFaint, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done", color = c.textMuted) } },
    )
}
