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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import app.bedtime.apps.AppEntry
import app.bedtime.ui.components.AppIcon
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.ObsidianTextField
import app.bedtime.ui.components.PlainButton
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.theme.Obsidian
import app.bedtime.unlock.EmergencyBreaks

/**
 * Emergency options from minimal mode that keep the session in place: calling, the user's
 * always-available apps, and a short, weekly-limited break with a stated reason.
 */
@Composable
internal fun EmergencyContent(
    onBack: () -> Unit,
    onCall: () -> Unit,
    alwaysAvailable: List<AppEntry>,
    onOpenApp: (String) -> Unit,
    breaksLeft: Int,
    onBreak: (reason: String) -> Unit,
    preview: Boolean = false,
) {
    val c = Obsidian.colors
    var reason by rememberSaveable { mutableStateOf("") }
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
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = c.textMuted) }
            Text("Emergency", style = MaterialTheme.typography.headlineSmall, color = c.textNormal)
        }
        Text(
            "For when something really can't wait. Your session stays in place.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.textMuted,
        )

        SectionCard(title = "Call someone") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Body("The phone app always works here, including emergency calls.")
                PlainButton("Open Phone", onClick = onCall, modifier = Modifier.fillMaxWidth())
            }
        }

        SectionCard(title = "Always available", subtitle = "Never blocked, whatever the session") {
            if (alwaysAvailable.isEmpty()) {
                Body("No apps here yet. After this session, add a few (maps, rides, your authenticator) in Settings → Emergency.")
            } else {
                alwaysAvailable.forEach { app ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable(onClickLabel = "Open ${app.label}") { onOpenApp(app.packageName) }
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

        SectionCard(
            title = "Take a ${EmergencyBreaks.MINUTES}-minute break",
            subtitle = "Everything pauses, then your session picks up again by itself.",
        ) {
            if (breaksLeft > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ObsidianTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = "What's the emergency?",
                        placeholder = "e.g. waiting for a call from the doctor",
                    )
                    CtaButton(
                        "Start break",
                        onClick = { onBreak(reason.trim()) },
                        enabled = reason.trim().length >= 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Body("$breaksLeft of ${EmergencyBreaks.PER_WEEK} left this week.")
                }
            } else {
                Body("You've used this week's ${EmergencyBreaks.PER_WEEK} emergency breaks. The unlock steps on the home screen still work.")
            }
        }
    }
}

@Composable
private fun Body(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = Obsidian.colors.textMuted)
}
