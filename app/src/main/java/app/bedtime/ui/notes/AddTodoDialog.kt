package app.bedtime.ui.notes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian

/** One line for the to-do list, without opening it. */
@Composable
internal fun AddTodoDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    val c = Obsidian.colors
    var words by remember { mutableStateOf("") }
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val add = { words.trim().takeIf { it.isNotEmpty() }?.let(onAdd) ?: Unit }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.bgSecondary,
        title = { Text("Add a to-do", color = c.textNormal) },
        text = {
            app.bedtime.ui.components.ObsidianTextField(
                value = words,
                onValueChange = { words = it },
                placeholder = "what to do",
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    capitalization = androidx.compose.ui.text.input.KeyboardCapitalization.None,
                    imeAction = androidx.compose.ui.text.input.ImeAction.Done,
                ),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { add() }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        },
        confirmButton = { TextButton(onClick = { add() }, enabled = words.isNotBlank()) { Text("Add", color = c.accentText) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.textMuted) } },
    )
}

