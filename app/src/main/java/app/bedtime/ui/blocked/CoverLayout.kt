package app.bedtime.ui.blocked

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.StatusPill
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian

/** A way off a cover: its words, what it does, and whether it can be taken yet. */
internal data class CoverAction(val label: String, val onClick: () -> Unit, val enabled: Boolean = true)

/**
 * Every full screen that stands between you and something — a blocked app, a locked Settings page,
 * the pause before opening, a day's minutes gone — is laid out the same way: an emblem, a pill
 * saying why, a large light title, a line of explanation, and at the foot one main way on with the
 * quieter ones under it. The fade is the one the unlock pages and the home header use.
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
) {
    val c = Obsidian.colors
    Column(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.fade, c.bgPrimary)))
            .safeDrawingPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        emblem()
        Spacer(Modifier.height(28.dp))
        if (pill != null) {
            StatusPill(pill, pillColor)
            Spacer(Modifier.height(16.dp))
        }
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Light, lineHeight = 34.sp, color = c.textNormal, textAlign = TextAlign.Center)
        if (body != null) {
            Spacer(Modifier.height(12.dp))
            Text(body, style = MaterialTheme.typography.bodyLarge, color = c.textMuted, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.weight(1.3f))
        CtaButton(primary.label, onClick = primary.onClick, enabled = primary.enabled, modifier = Modifier.fillMaxWidth())
        secondary.forEachIndexed { i, action ->
            TextButton(
                onClick = action.onClick,
                enabled = action.enabled,
                modifier = Modifier.fillMaxWidth().padding(top = if (i == 0) 4.dp else 0.dp),
            ) {
                Text(action.label, color = if (action.enabled) c.textMuted else c.textFaint)
            }
        }
    }
}

/** The covers' emblem: an icon on two soft rings of the accent. */
@Composable
internal fun CoverEmblem(icon: ImageVector) {
    val c = Obsidian.colors
    Box(Modifier.size(124.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
        Box(Modifier.size(88.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(44.dp))
        }
    }
}
