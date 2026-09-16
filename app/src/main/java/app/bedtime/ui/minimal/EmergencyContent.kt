package app.bedtime.ui.minimal

import androidx.compose.foundation.background
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
    onPauseBlocking: () -> Unit = {},
    preview: Boolean = false,
) {
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

        SectionCard(title = "call someone") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Body("the phone app always works here, including emergency calls.")
                PlainButton("open phone", onClick = onCall, modifier = Modifier.fillMaxWidth())
            }
        }

        SectionCard(title = "banking or id app", subtitle = "for apps that refuse to run while blocking is on") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Body(
                    "some banking and id apps (bankid, for one) won't start unless blocking is off. this switches it off for a " +
                        "minute and reminds you to switch it back on. it counts as an escape in your stats.",
                )
                PlainButton("pause blocking for 1 minute", onClick = onPauseBlocking, modifier = Modifier.fillMaxWidth())
            }
        }

        SectionCard(title = "always available", subtitle = "never blocked, whatever the session") {
            if (alwaysAvailable.isEmpty()) {
                Body("no apps here yet. after this session, add a few (maps, rides, your authenticator) in settings → emergency.")
            } else {
                alwaysAvailable.forEach { app ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClickLabel = "open ${app.label}") { onOpenApp(app.packageName) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(app.packageName, app.label, 36.dp)
                        Spacer(Modifier.width(14.dp))
                        Text(app.label, style = MaterialTheme.typography.bodyLarge, color = c.textNormal, modifier = Modifier.weight(1f))
                        Chevron()
                    }
                }
            }
        }
    }
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = Obsidian.colors.textMuted)
}
