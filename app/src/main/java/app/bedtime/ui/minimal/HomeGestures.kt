package app.bedtime.ui.minimal

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.bedtime.data.HomeGesture
import app.bedtime.data.HomeStyle
import app.bedtime.data.NoteTarget
import app.bedtime.ui.components.Text
import app.bedtime.ui.homestyle.palette
import kotlin.math.abs

/** What a drag on the home screen meant: all apps (up), one of the gestures, or nothing at all. */
sealed interface HomeDrag {
    data object None : HomeDrag
    data object AllApps : HomeDrag
    data class Gesture(val gesture: HomeGesture) : HomeDrag
}

/** Reads a drag that travelled ([dx], [dy]): the longer way wins, and anything shorter than [minDistance] is nothing. */
fun readHomeDrag(dx: Float, dy: Float, minDistance: Float): HomeDrag = when {
    abs(dx) < minDistance && abs(dy) < minDistance -> HomeDrag.None
    abs(dy) >= abs(dx) -> if (dy < 0) HomeDrag.AllApps else HomeDrag.Gesture(HomeGesture.SWIPE_DOWN)
    dx < 0 -> HomeDrag.Gesture(HomeGesture.SWIPE_LEFT)
    else -> HomeDrag.Gesture(HomeGesture.SWIPE_RIGHT)
}

/**
 * A note written straight from the home screen: the words, then where they go, as from the search:
 * the app's own notes, or one of your files.
 */
@Composable
internal fun QuickNoteDialog(style: HomeStyle, targets: List<NoteTarget>, onSave: (String, NoteTarget?) -> Unit, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) { QuickNoteContent(style, targets, onSave = { words, target -> onSave(words, target); onDismiss() }) }
}

@Composable
internal fun QuickNoteContent(style: HomeStyle, targets: List<NoteTarget>, onSave: (String, NoteTarget?) -> Unit, initial: String = "", focus: Boolean = true) {
    val p = style.palette()
    var words by remember { mutableStateOf(initial) }
    val focusRequester = remember { FocusRequester() }
    if (focus) LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(p.background)
            .border(1.dp, p.faint.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("A quick note", style = MaterialTheme.typography.titleMedium, color = p.text)
        BasicTextField(
            value = words,
            onValueChange = { words = it },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = p.text),
            cursorBrush = SolidColor(p.accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 96.dp)
                .focusRequester(focusRequester)
                .border(1.dp, p.faint.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            decorationBox = { field ->
                Box {
                    if (words.isEmpty()) Text("What's on your mind", style = MaterialTheme.typography.bodyLarge, color = p.faint)
                    field()
                }
            },
        )
        val ready = words.isNotBlank()
        (listOf<NoteTarget?>(null) + targets).forEach { target ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = ready) { onSave(words.trim(), target) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (target?.daily == true) Icons.Default.DateRange else Icons.Default.Edit,
                    contentDescription = null,
                    tint = if (ready) p.accent else p.faint,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(14.dp))
                Text(
                    target?.let { "Add to ${it.name}" } ?: "Save in refuge notes",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (ready) p.text else p.faint,
                )
            }
        }
    }
}
