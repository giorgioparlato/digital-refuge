package app.bedtime.ui.notes

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
import androidx.compose.foundation.text.KeyboardActions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.bedtime.data.Note
import app.bedtime.data.NoteWidget
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.VerbatimText

/**
 * One of your refuge notes on the screen: its title, its first lines with boxes to tick, and a line to
 * add to it. A tap on the words opens the note; a long press chooses another.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteWidgetView(
    note: Note?,
    text: Color,
    faint: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 6,
    onToggle: (lineIndex: Int) -> Unit = {},
    onAdd: (String) -> Unit = {},
    onOpen: () -> Unit = {},
    onHold: (() -> Unit)? = null,
) {
    val openOrHold = Modifier.combinedClickable(onClickLabel = "open the note", onLongClick = onHold, onClick = onOpen)
    Column(modifier.fillMaxWidth()) {
        if (note == null) {
            Text("hold here to choose a note to show", style = MaterialTheme.typography.bodyMedium, color = faint, modifier = Modifier.clip(RoundedCornerShape(8.dp)).then(openOrHold))
            return@Column
        }
        VerbatimText(
            NoteWidget.title(note.text).ifBlank { "Note" },
            style = MaterialTheme.typography.labelLarge,
            color = faint,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).then(openOrHold).padding(vertical = 2.dp),
        )
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
                    style = MaterialTheme.typography.bodyLarge.copy(textDecoration = if (line.box == true) TextDecoration.LineThrough else null),
                    color = if (line.box == true) faint else text,
                    modifier = Modifier.weight(1f).clip(RoundedCornerShape(6.dp)).then(openOrHold),
                )
            }
        }
        var adding by remember { mutableStateOf("") }
        BasicTextField(
            value = adding,
            onValueChange = { adding = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = text),
            cursorBrush = SolidColor(accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (adding.isNotBlank()) onAdd(adding.trim())
                adding = ""
            }),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            decorationBox = { field ->
                Box {
                    if (adding.isEmpty()) Text("+ add a line", style = MaterialTheme.typography.bodyMedium, color = faint)
                    field()
                }
            },
        )
    }
}
