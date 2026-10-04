package app.bedtime.ui.minimal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Call
import app.bedtime.ui.components.BedtimeIcons
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import app.bedtime.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.bedtime.apps.AppEntry
import app.bedtime.ui.components.AppIcon
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.PlainButton
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.theme.Obsidian

/** Emergency options from minimal mode that keep the session in place: calling, and always-available apps. */
@Composable
internal fun EmergencyContent(
    onBack: () -> Unit,
    onCall: () -> Unit,
    alwaysAvailable: List<AppEntry>,
    onOpenApp: (String) -> Unit,
    onOpenRefuge: () -> Unit = {},
    onPauseBlocking: () -> Unit = {},
    breakMinutes: Int = 1,
    pauseEnabled: Boolean = true,
    preview: Boolean = false,
) {
    val minutes = "$breakMinutes ${if (breakMinutes == 1) "minute" else "minutes"}"
    val c = Obsidian.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(c.bgPrimary)
            .then(if (preview) Modifier else Modifier.safeDrawingPadding())
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "back", tint = c.textMuted) }
            Text("emergency", style = MaterialTheme.typography.headlineSmall, color = c.textNormal)
        }
        Text(
            "for when something really can't wait. your session stays in place.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.textMuted,
        )

        // Two that work no matter how the session is set up, so there is always something here.
        SectionCard(title = "always works") {
            ActionRow(onClick = onOpenRefuge, label = "digital refuge") {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(BedtimeIcons.Refuge, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp)) }
            }
            ActionRow(onClick = onCall, label = "phone") {
                Box(
                    Modifier.size(36.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Default.Call, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp)) }
            }
        }

        SectionCard(title = "always available", subtitle = "never blocked, whatever the session") {
            if (alwaysAvailable.isEmpty()) {
                Body("none yet. add a few — maps, rides, your authenticator — in settings \u2192 emergency.")
            } else {
                alwaysAvailable.forEach { app ->
                    ActionRow(onClick = { onOpenApp(app.packageName) }, label = app.label) {
                        AppIcon(app.packageName, app.label, 36.dp)
                    }
                }
            }
        }

        if (pauseEnabled) {
            SectionCard(title = "emergency break") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Body("steps out for $minutes. a full screen brings you back when it ends.")
                    PlainButton("pause blocking for $minutes", onClick = onPauseBlocking, modifier = Modifier.fillMaxWidth())
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
