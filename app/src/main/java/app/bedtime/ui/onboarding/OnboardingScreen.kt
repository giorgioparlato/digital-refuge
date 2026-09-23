package app.bedtime.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian

/** What a session does and how to leave it. Shown once, and reachable from settings and any block. */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Spacer(Modifier.height(12.dp))
            Icon(BedtimeIcons.Refuge, contentDescription = null, tint = c.accent, modifier = Modifier.size(44.dp))
            Spacer(Modifier.height(18.dp))
            Text("how it works", style = MaterialTheme.typography.headlineMedium, color = c.textNormal)
            Spacer(Modifier.height(10.dp))
            Text(
                "digital refuge sets aside hours when your phone asks less of you. here is what happens in one of " +
                    "them, and how to leave if you need to.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textMuted,
            )
            Spacer(Modifier.height(30.dp))

            Point(
                BedtimeIcons.Moon,
                "a session begins",
                "at the times you set, or whenever you start a block. the apps you named stop opening. your home " +
                    "screen can become a short list of what you chose, the screen can fade to grey, and notifications " +
                    "wait. music and alarms are never silenced.",
            )
            Point(
                BedtimeIcons.Hourglass,
                "leaving early takes the steps you chose",
                "a wait, a passage to type, a password — as many as you want, in that order. enough of a pause for an " +
                    "urge to pass. they can grow with each unlock on the same day, and start fresh each morning.",
            )
            Point(
                BedtimeIcons.Refuge,
                "it stays put while it runs",
                "a session's own settings can't be changed while it is running. that one isn't a choice: without it, " +
                    "the easiest way out would simply be to edit the block.",
            )
            Point(
                BedtimeIcons.Contrast,
                "an emergency break",
                "if a block allows one, you can step out for a few minutes without ending the session — for whatever " +
                    "genuinely can't wait. it counts as an escape, and a full screen brings you back when it is over.",
            )

            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
            Spacer(Modifier.height(28.dp))

            Text("how firm to make it", style = MaterialTheme.typography.headlineSmall, color = c.textNormal)
            Spacer(Modifier.height(10.dp))
            Text(
                "two things are always true: while a session runs its settings can't be changed, and leaving early " +
                    "takes the steps you chose. everything below is one switch on top of that. turn them all on for " +
                    "the firmest it gets, or pick only the ones you want.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textMuted,
            )
            Spacer(Modifier.height(24.dp))

            Switch(
                "1",
                "harder each time",
                "each unlock on the same day makes the wait and the typing longer, by a multiplier you set. it starts " +
                    "fresh each morning.",
                "in the block",
            )
            Switch(
                "2",
                "lock settings afterwards",
                "leave a session early and that block stays shut for what would have been the rest of it, so unlocking " +
                    "can't be used to soften it.",
                "in the block",
            )
            Switch(
                "3",
                "no emergency break",
                "with the break switched off there is no short way out, and the unlock steps are the only way through.",
                "in the block",
            )
            Switch(
                "4",
                "lock changes during sessions",
                "covers the settings screens that would switch blocking off, and the prompt to uninstall the app.",
                "settings → staying blocked",
            )
            Switch(
                "5",
                "full-screen reminder",
                "if blocking is switched off at all, a full screen stays with you until you switch it back on.",
                "settings → staying blocked",
            )

            Text(
                "open a block or schedule to set the first three; the ⓘ at the top of it brings you back here.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textFaint,
            )

            Spacer(Modifier.height(28.dp))
            CtaButton("got it", onClick = onDone, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** One switch: what it closes off, and where to find it. */
@Composable
private fun Switch(step: String, name: String, body: String, where: String) {
    val c = Obsidian.colors
    Row(Modifier.fillMaxWidth().padding(bottom = 22.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(step, style = MaterialTheme.typography.labelMedium, color = c.accentText, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, color = c.textNormal, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
            Spacer(Modifier.height(8.dp))
            Text(
                where,
                style = MaterialTheme.typography.labelSmall,
                color = c.textFaint,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, c.border, RoundedCornerShape(50))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun Point(icon: ImageVector, title: String, body: String) {
    val c = Obsidian.colors
    Row(Modifier.fillMaxWidth().padding(bottom = 22.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(40.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.textNormal, fontWeight = FontWeight.SemiBold)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
        }
    }
}
