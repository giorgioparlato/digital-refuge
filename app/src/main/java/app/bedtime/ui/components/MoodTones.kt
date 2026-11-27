package app.bedtime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.bedtime.data.Why
import app.bedtime.ui.theme.Obsidian

/** A mood's colour ([Why.TONES]) as the app draws it: sage, cream, tan or clay rose. */
@Composable
fun toneColour(tone: String): Color {
    val c = Obsidian.colors
    return when (tone) {
        "sage" -> c.accent
        "tan" -> c.textMuted
        "rose" -> c.distracting
        else -> c.textNormal
    }
}

/** The four colours a mood can have, the [picked] one ringed. */
@Composable
fun ToneSwatches(picked: String, onPick: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = Obsidian.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Why.TONES.forEach { tone ->
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(2.dp, if (tone == picked) c.textNormal else Color.Transparent, CircleShape)
                    .clickable(onClickLabel = tone, role = Role.RadioButton) { onPick(tone) }
                    .padding(6.dp)
                    .clip(CircleShape)
                    .background(toneColour(tone)),
            )
        }
    }
}
