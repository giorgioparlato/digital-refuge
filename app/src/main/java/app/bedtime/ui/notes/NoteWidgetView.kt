package app.bedtime.ui.notes

import androidx.compose.ui.graphics.luminance
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlinx.coroutines.delay
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
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
 * One of your refuge notes on the screen: its title, its first lines with boxes to tick. A tap anywhere
 * on it types into it right here, Enter starting a new line (and carrying a list on); it's saved as you
 * go, and closing the keyboard or going back stops typing. Only then is there a cursor. A long press
 * chooses another note.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun NoteWidgetView(
    note: Note?,
    screenText: Color,
    screenFaint: Color,
    accent: Color,
    modifier: Modifier = Modifier,
    maxLines: Int = 6,
    onToggle: (lineIndex: Int) -> Unit = {},
    /** The note's new text, once you're done typing. */
    onSave: (String) -> Unit = {},
    onHold: (() -> Unit)? = null,
    /** Small, beside the clock: fewer lines and a smaller type. */
    compact: Boolean = false,
    /** A card colour of its own; null sits it on the screen's background. */
    card: Color? = null,
    /** On a card: dark words, light ones, or null to suit the card. */
    darkText: Boolean? = null,
    /** The size of its words, from [app.bedtime.data.TextSize]. */
    textScale: Float = 1f,
) {
    // On a card of its own, words in ink or cream: as chosen, or to suit it.
    val onCard = card?.let { if (inkOnCard(it, darkText)) Color(0xFF2B2620) else Color(0xFFEDE6DA) }
    fun TextStyle.sized() = copy(fontSize = fontSize * textScale, lineHeight = lineHeight * textScale)
    val text = onCard ?: screenText
    val faint = onCard?.copy(alpha = 0.6f) ?: screenFaint
    val holdOnly = Modifier.combinedClickable(onClickLabel = "choose a note", onLongClick = onHold, onClick = { onHold?.invoke() })
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
    Column(
        modifier
            .fillMaxWidth()
            .then(if (card != null) Modifier.clip(RoundedCornerShape(18.dp)).background(card).padding(horizontal = 16.dp, vertical = 12.dp) else Modifier),
    ) {
        if (note == null) {
            Text("hold here to choose a note to show", style = MaterialTheme.typography.bodyMedium, color = faint, modifier = Modifier.clip(RoundedCornerShape(8.dp)).then(holdOnly))
            return@Column
        }
        if (typing) {
            LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
            var layout by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
            Box(Modifier.fillMaxWidth().heightIn(max = 280.dp).verticalScroll(rememberScrollState())) {
                BasicTextField(
                    value = value,
                    onValueChange = { value = Markdown.tickOnTap(value, it) ?: Markdown.continueList(value, it) },
                    textStyle = MaterialTheme.typography.bodyLarge.sized().copy(color = text),
                    cursorBrush = SolidColor(accent),
                    visualTransformation = remember(faint, accent) { MarkdownTransformation(faint, accent) },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    onTextLayout = { layout = it },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus).todoBoxes(value.text, { layout }, text.copy(alpha = 0.18f), accent),
                )
            }
            return@Column
        }
        val startTyping = Modifier.combinedClickable(onClickLabel = "write in the note", onLongClick = onHold) {
            value = TextFieldValue(note.text, TextRange(note.text.length))
            typing = true
        }
        VerbatimText(
            NoteWidget.title(note.text).ifBlank { "Note" },
            style = MaterialTheme.typography.labelLarge.sized(),
            color = faint,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).then(startTyping).padding(vertical = 2.dp),
        )
        val body = (if (compact) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.bodyLarge).sized()
        NoteWidget.lines(note.text, maxLines).forEach { line ->
            Row(Modifier.fillMaxWidth().padding(start = (16 * line.indent).dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                when (line.box) {
                    null -> if (line.bullet) Text("•", style = body, color = faint, modifier = Modifier.width((18 * textScale).dp))
                    else -> Box(
                        Modifier
                            .size((28 * textScale).dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(onClickLabel = if (line.box) "untick" else "tick") { onToggle(line.index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            Modifier.size((19 * textScale).dp).clip(RoundedCornerShape(5.dp)).background(if (line.box) accent else text.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (line.box) androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size((14 * textScale).dp))
                        }
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

/** Whether words on a card of colour [card] are dark: as [dark] says, or, left to suit it, on a light card. */
internal fun inkOnCard(card: Color, dark: Boolean?): Boolean = dark ?: (card.luminance() > 0.5f)
