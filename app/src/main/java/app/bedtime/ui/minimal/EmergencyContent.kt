package app.bedtime.ui.minimal

import androidx.compose.ui.unit.sp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.border
import app.bedtime.ui.components.LineIcons
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Call
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.BedtimeIcons
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import app.bedtime.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.PlainButton
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.theme.Obsidian

/**
 * Emergency options from minimal mode that keep the session in place: calling, digital refuge, and a
 * short break. Always-available apps are in the full list behind a swipe up.
 */
@Composable
internal fun EmergencyContent(
    onBack: () -> Unit,
    onCall: () -> Unit,
    onOpenRefuge: () -> Unit = {},
    onPauseBlocking: () -> Unit = {},
    breakMinutes: Int = 1,
    pauseEnabled: Boolean = true,
    preview: Boolean = false,
) {
    val minutes = "$breakMinutes ${if (breakMinutes == 1) "minute" else "minutes"}"
    val c = Obsidian.colors
    Scaffold(
        containerColor = c.bgPrimary,
        topBar = { ObsidianTopBar("emergency", onBack = onBack, subtitle = "for when something really can't wait. your session stays in place.") },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .then(if (preview) Modifier else Modifier.navigationBarsPadding())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PageGutter)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Calling first and largest: in a hurry, it's the one thing to find without reading.
            Row(
                Modifier.fillMaxWidth().height(84.dp).clip(RoundedCornerShape(22.dp)).background(c.accentFill)
                    .clickable(onClickLabel = "open phone", onClick = onCall).padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(LineIcons.Call, contentDescription = null, tint = c.textOnAccent, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(16.dp))
                Text("phone", fontSize = 20.sp, color = c.textOnAccent, modifier = Modifier.weight(1f))
                Icon(LineIcons.KeyboardArrowRight, contentDescription = null, tint = c.textOnAccent, modifier = Modifier.size(20.dp))
            }

            // Works no matter how the session is set up, so there is always something here.
            SectionCard(title = "always works") {
                ActionRow(onClick = onOpenRefuge, label = "digital refuge") {
                    Box(
                        Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(c.interactive),
                        contentAlignment = Alignment.Center,
                    ) { Icon(BedtimeIcons.Refuge, contentDescription = null, tint = c.accentText, modifier = Modifier.size(20.dp)) }
                }
            }

            if (pauseEnabled) {
                // The break is the last resort, so it sits quietly at the foot, outlined rather than filled.
                Spacer(Modifier.height(8.dp))
                HorizontalDivider(color = c.border)
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("emergency break", style = MaterialTheme.typography.bodyLarge, color = c.textNormal)
                    Body("steps out for $minutes. a full screen brings you back when it ends.")
                    Box(
                        Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(50))
                            .border(1.dp, c.border, RoundedCornerShape(50)).clickable(onClick = onPauseBlocking),
                        contentAlignment = Alignment.Center,
                    ) { Text("pause blocking for $minutes", style = MaterialTheme.typography.bodyLarge, color = c.textNormal) }
                }
            }
        }
    }
}

/** One tappable line: icon, name, chevron. Everything on this screen opens something. */
@Composable
private fun ActionRow(onClick: () -> Unit, label: String, icon: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClickLabel = "open $label", onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon()
        Spacer(Modifier.width(14.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Obsidian.colors.textNormal, modifier = Modifier.weight(1f))
        Chevron()
    }
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = Obsidian.colors.textMuted)
}
