package app.bedtime.ui.notes

import kotlinx.coroutines.delay
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.bedtime.data.Note
import app.bedtime.data.NoteWidget
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.VerbatimText

/**
 * One of your refuge notes on the screen: its title, its first lines with boxes to tick. A tap on the
 * title opens the whole note; a tap on the lines types into it right here, Enter starting a new line
 * (and carrying a list on). Closing the keyboard or going back saves it and stops typing; only then is
 * there a cursor. A long press chooses another note.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun NoteWidgetView(
    note: Note?,
    text: Color,
    faint: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 6,
    onToggle: (lineIndex: Int) -> Unit = {},
    /** The note's new text, once you're done typing. */
    onSave: (String) -> Unit = {},
    onOpen: () -> Unit = {},
    onHold: (() -> Unit)? = null,
    /** Small, beside the clock: fewer lines, a smaller type, and a tap on the words opens the note. */
    compact: Boolean = false,
) {
    val holdOnly = Modifier.combinedClickable(onClickLabel = "open the note", onLongClick = onHold, onClick = onOpen)
    var typing by remember(note?.id) { mutableStateOf(false) }
    var value by remember(note?.id) { mutableStateOf(TextFieldValue(note?.text.orEmpty())) }
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    fun done() {
        if (!typing) return
        typing = false
        focusManager.clearFocus()
        if (note != null && value.text != note.text) onSave(value.text)
    }
    // Saved as you type, a moment after you pause, so nothing waits on leaving cleanly.
    LaunchedEffect(value.text, typing) {
        if (!typing || note == null) return@LaunchedEffect
        delay(600)
        if (value.text != note.text) onSave(value.text)
    }
    // Leaving typing: back, or the keyboard closing.
    BackHandler(enabled = typing) { done() }
    val keyboardUp = WindowInsets.isImeVisible
    var keyboardSeen by remember { mutableStateOf(false) }
    LaunchedEffect(keyboardUp, typing) {
        if (typing && keyboardUp) keyboardSeen = true
        if (typing && keyboardSeen && !keyboardUp) {
            keyboardSeen = false
            done()
        }
        if (!typing) keyboardSeen = false
    }
    Column(modifier.fillMaxWidth()) {
        if (note == null) {
            Text("hold here to choose a note to show", style = MaterialTheme.typography.bodyMedium, color = faint, modifier = Modifier.clip(RoundedCornerShape(8.dp)).then(holdOnly))
            return@Column
        }
        if (typing) {
            LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
            BasicTextField(
                value = value,
                onValueChange = { value = Markdown.tickOnTap(value, it) ?: Markdown.continueList(value, it) },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = text),
                cursorBrush = SolidColor(accent),
                visualTransformation = remember(faint, accent) { MarkdownTransformation(faint, accent) },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp).focusRequester(focus),
            )
            return@Column
        }
        VerbatimText(
            NoteWidget.title(note.text).ifBlank { "Note" },
            style = MaterialTheme.typography.labelLarge,
            color = faint,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).then(holdOnly).padding(vertical = 2.dp),
        )
        val startTyping = if (compact) {
            holdOnly
        } else {
            Modifier.combinedClickable(onClickLabel = "write in the note", onLongClick = onHold) {
                value = TextFieldValue(note.text, TextRange(note.text.length))
                typing = true
            }
        }
        val body = if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge
        NoteWidget.lines(note.text, maxLines).forEach { line ->
            Row(Modifier.fillMaxWidth().padding(start = (16 * line.indent).dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                when (line.box) {
                    null -> if (line.bullet) Text("•", style = MaterialTheme.typography.bodyLarge, color = faint, modifier = Modifier.width(18.dp))
                    else -> Box(
                        Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClickLabel = if (line.box) "untick" else "tick") { onToggle(line.index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.size(16.dp).clip(RoundedCornerShape(4.dp))
                                .then(if (line.box) Modifier.background(accent) else Modifier.border(1.5.dp, faint, RoundedCornerShape(4.dp))),
                        )
                    }
                }
                if (line.box != null) Spacer(Modifier.width(4.dp))
                VerbatimText(
                    line.text,
                    style = body.copy(textDecoration = if (line.box == true) TextDecoration.LineThrough else null),
                    color = if (line.box == true) faint else text,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).then(startTyping),
                )
            }
        }
        if (NoteWidget.lines(note.text, maxLines).isEmpty()) {
            Text("tap to write", style = MaterialTheme.typography.bodyMedium, color = faint, modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(6.dp)).then(startTyping))
        }
    }
}
