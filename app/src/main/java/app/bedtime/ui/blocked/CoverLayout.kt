package app.bedtime.ui.blocked

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontStyle
import app.bedtime.ui.components.SoftPillButton
import app.bedtime.ui.components.StatusPill
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian

/** A way off a cover: its words, what it does, and whether it can be taken yet. */
internal data class CoverAction(val label: String, val onClick: () -> Unit, val enabled: Boolean = true)

/**
 * Every full screen that stands between you and something — a blocked app, a locked Settings page,
 * the pause before opening, a day's minutes gone — is laid out the same way: an emblem, a pill
 * saying why, a large light title, a line of explanation, and at the foot one main way on, the full
 * width, with the quieter ones under it. The words sit to the left, like the rest of the app, unless
 * [centered] (the pause, whose emblem is the breathing disc). When there are words chosen for this
 * cover, they're what you read first: the title becomes a small line above them.
 */
@Composable
internal fun CoverLayout(
    emblem: @Composable () -> Unit,
    title: String,
    body: String?,
    primary: CoverAction,
    modifier: Modifier = Modifier,
    pill: String? = null,
    pillColor: Color = Obsidian.colors.accentText,
    secondary: List<CoverAction> = emptyList(),
    /** Words chosen for this cover (yours, or one of the app's), the largest words on it. */
    message: String? = null,
    centered: Boolean = false,
) {
    val c = Obsidian.colors
    val align = if (centered) TextAlign.Center else TextAlign.Start
    Column(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.fade, c.bgPrimary)))
            .safeDrawingPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
    ) {
        Spacer(Modifier.weight(1f))
        emblem()
        Spacer(Modifier.height(28.dp))
        if (pill != null) {
            StatusPill(pill, pillColor)
            Spacer(Modifier.height(16.dp))
        }
        if (message != null) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = c.textMuted, textAlign = align)
            Spacer(Modifier.height(14.dp))
            Text("“$message”", fontSize = 28.sp, fontWeight = FontWeight.Light, lineHeight = 35.sp, color = c.textNormal, textAlign = align)
            if (body != null) {
                Spacer(Modifier.height(14.dp))
                Text(body, style = MaterialTheme.typography.bodyLarge, color = c.textMuted, textAlign = align)
            }
        } else {
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.Light, lineHeight = 34.sp, color = c.textNormal, textAlign = align)
            if (body != null) {
                Spacer(Modifier.height(12.dp))
                Text(body, style = MaterialTheme.typography.bodyLarge, color = c.textMuted, textAlign = align)
            }
        }
        Spacer(Modifier.weight(1.3f))
        SoftPillButton(primary.label, onClick = primary.onClick, enabled = primary.enabled, modifier = Modifier.fillMaxWidth())
        secondary.forEachIndexed { i, action ->
            TextButton(
                onClick = action.onClick,
                enabled = action.enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = if (i == 0) 4.dp else 0.dp),
            ) {
                Text(action.label, color = if (action.enabled) c.textMuted else c.textFaint)
            }
        }
    }
}

/** The covers' emblem: an icon on a small soft tile, quiet enough to leave the words in charge. */
@Composable
internal fun CoverEmblem(icon: ImageVector) {
    val c = Obsidian.colors
    Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(c.bgSecondary), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = null, tint = c.accentText, modifier = Modifier.size(28.dp))
    }
}
