package app.bedtime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import app.bedtime.data.CoverMessage
import app.bedtime.data.CoverMessages
import app.bedtime.data.CustomMessage
import app.bedtime.data.MessageMode
import app.bedtime.data.Repository
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.launch

/** "What the cover says": the current choice in a line, and a tap to change it. */
@Composable
fun MessageChoiceRow(title: String, message: CoverMessage, onChange: (CoverMessage) -> Unit, enabled: Boolean = true) {
    var choosing by rememberSaveable { mutableStateOf(false) }
    if (choosing) {
        MessageChoiceDialog(
            current = message,
            onPick = {
                onChange(it)
                choosing = false
            },
            onDismiss = { choosing = false },
        )
    }
    OptionRow(
        BedtimeIcons.Leaf,
        title,
        description = CoverMessages.summary(message),
        enabled = enabled,
        onClick = { choosing = true },
    ) { Chevron() }
}

/**
 * Every way a cover can choose its words: the usual ones, a different one each time, or one in
 * particular — the app's or yours. Yours are written here, shared by every schedule, block and limit,
 * and each can join the mix or stay out of it.
 */
@Composable
private fun MessageChoiceDialog(current: CoverMessage, onPick: (CoverMessage) -> Unit, onDismiss: () -> Unit) {
    val c = Obsidian.colors
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val custom = settings?.customMessages.orEmpty()
    var draft by rememberSaveable { mutableStateOf("") }

    fun updateCustom(transform: (List<CustomMessage>) -> List<CustomMessage>) {
        scope.launch { repo.updateSettings { it.copy(customMessages = transform(it.customMessages)) } }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.bgSecondary,
        title = { Text("What the cover says") },
        text = {
            LazyColumn(Modifier.heightIn(max = 480.dp)) {
                item { Choice("The usual words", current.mode == MessageMode.USUAL) { onPick(CoverMessage()) } }
                item {
                    Choice("A different one each time", current.mode == MessageMode.RANDOM, note = "From the app's and yours that are in the mix") {
                        onPick(CoverMessage(MessageMode.RANDOM))
                    }
                }
                item { Heading("Or always one of these") }
                CoverMessages.presets.forEach { text ->
                    item { Choice("“$text”", current.mode == MessageMode.CHOSEN && current.text == text) { onPick(CoverMessage(MessageMode.CHOSEN, text)) } }
                }
                if (custom.isNotEmpty()) item { Heading("Yours") }
                custom.forEach { message ->
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                Choice(
                                    "“${message.text}”",
                                    current.mode == MessageMode.CHOSEN && current.text == message.text,
                                    note = if (message.inRandom) "In the mix · tap the dot to take it out" else "Not in the mix · tap the dot to add it",
                                ) { onPick(CoverMessage(MessageMode.CHOSEN, message.text)) }
                            }
                            // The mix switch and the delete, small and to the side of the message itself.
                            Box(
                                Modifier.size(32.dp).clip(CircleShape)
                                    .clickable(onClickLabel = if (message.inRandom) "take out of the mix" else "add to the mix") {
                                        updateCustom { list -> list.map { if (it == message) it.copy(inRandom = !it.inRandom) else it } }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier.size(10.dp).clip(CircleShape)
                                        .background(if (message.inRandom) c.accent else c.bgSecondary)
                                        .border(1.5.dp, c.accent, CircleShape),
                                )
                            }
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "delete",
                                tint = c.textFaint,
                                modifier = Modifier.size(32.dp).clip(CircleShape).clickable { updateCustom { it - message } }.padding(7.dp),
                            )
                        }
                    }
                }
                item {
                    Heading("Write your own")
                    ObsidianTextField(value = draft, onValueChange = { draft = it }, label = "A few words to read")
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(
                            enabled = draft.isNotBlank(),
                            onClick = {
                                val text = draft.trim()
                                if (custom.none { it.text == text }) updateCustom { it + CustomMessage(text) }
                                draft = ""
                                onPick(CoverMessage(MessageMode.CHOSEN, text))
                            },
                        ) { Text("Add and use it", color = if (draft.isNotBlank()) c.accentText else c.textFaint, fontWeight = FontWeight.SemiBold) }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done", color = c.textMuted) } },
    )
}

@Composable
private fun Heading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = Obsidian.colors.accentText,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
}

/** One choice: a dot that fills when it's the one, the words, and an optional line under them. */
@Composable
private fun Choice(label: String, selected: Boolean, note: String? = null, onClick: () -> Unit) {
    val c = Obsidian.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.padding(top = 4.dp).size(14.dp).clip(CircleShape)
                .border(1.5.dp, if (selected) c.accent else c.textFaint, CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (selected) Box(Modifier.size(7.dp).clip(CircleShape).background(c.accent)) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge, color = c.textNormal)
            if (note != null) Text(note, style = MaterialTheme.typography.bodySmall, color = c.textFaint)
        }
    }
}
