package app.bedtime.unlock

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bedtime.data.Why
import app.bedtime.ui.components.SoftPillButton
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian

/**
 * Writing a few lines before unlocking: a prompt to write from (another a tap away), and at least
 * [words] words of your own before you can go on. What's written goes to [onPassed].
 */
@Composable
internal fun WriteChallenge(words: Int, prompts: List<Pair<String, String?>>, onPassed: (prompt: String, text: String) -> Unit) {
    val c = Obsidian.colors
    var which by rememberSaveable { mutableIntStateOf(0) }
    var text by rememberSaveable { mutableStateOf("") }
    val (prompt, hint) = prompts.getOrElse(which % prompts.size.coerceAtLeast(1)) { "" to null }
    val written = Why.words(text)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(prompt, fontSize = 22.sp, color = c.textNormal, textAlign = TextAlign.Center)
        hint?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                color = c.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (prompts.size > 1) {
            Text(
                "another prompt",
                style = MaterialTheme.typography.labelLarge,
                color = c.accentText,
                modifier = Modifier.padding(top = 6.dp).clip(RoundedCornerShape(50)).clickable { which++ }.padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        // Written by hand: no paste menu, and anything longer than a word or two at once is turned away.
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalTextToolbar provides NoPasting) {
        BasicTextField(
            value = text,
            onValueChange = { if (Why.typed(text, it)) text = it },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.textNormal),
            cursorBrush = SolidColor(c.accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp, max = 280.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(1.dp, c.border, RoundedCornerShape(14.dp))
                .padding(14.dp),
        )
        }
        Text(
            if (written >= words) "$written words · enough" else "$written of $words words",
            style = MaterialTheme.typography.labelMedium,
            color = if (written >= words) c.accentText else c.textFaint,
            modifier = Modifier.padding(top = 8.dp),
        )
        Spacer(Modifier.height(16.dp))
        SoftPillButton("Continue", onClick = { onPassed(prompt, text) }, enabled = written >= words)
    }
}

/** A text toolbar that never shows: no paste, so what's written is typed here. */
internal object NoPasting : androidx.compose.ui.platform.TextToolbar {
    override val status = androidx.compose.ui.platform.TextToolbarStatus.Hidden

    override fun showMenu(
        rect: androidx.compose.ui.geometry.Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) = Unit

    override fun hide() = Unit
}
