package app.bedtime.ui.onboarding

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian

/** A one-time explainer of how a session holds, and the ways out. Also reachable from settings. */
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
            Icon(BedtimeIcons.Refuge, contentDescription = null, tint = c.accent, modifier = Modifier.size(48.dp))
            Spacer(Modifier.height(16.dp))
            Text("welcome to your refuge", style = MaterialTheme.typography.headlineMedium, color = c.textNormal)
            Spacer(Modifier.height(8.dp))
            Text(
                "digital refuge sets aside hours when your phone asks less of you. here is what happens inside one of those hours, " +
                    "and how to step out if you truly need to.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textMuted,
            )
            Spacer(Modifier.height(28.dp))

            Point(
                BedtimeIcons.Moon,
                "a session begins",
                "at the times you choose, or whenever you start a block, the apps you have named step aside. the screen can " +
                    "fade to grey, and notifications wait quietly until the session ends.",
            )
            Point(
                BedtimeIcons.Hourglass,
                "leaving early asks something of you",
                "if you want out before the end, the steps you picked stand in the way: a wait, a passage to type, a password. " +
                    "just enough of a pause for the urge to pass. each unlock can be set to ask a little more than the last.",
            )
            Point(
                BedtimeIcons.Refuge,
                "it holds firm",
                "during a session, the screens that would switch blocking off, or remove the app, are gently covered. not to " +
                    "trap you — only so that leaving is a decision rather than a reflex.",
            )
            Point(
                BedtimeIcons.Target,
                "if blocking stops anyway",
                "should it be switched off regardless, a full screen waits with you until you turn it back on. the quiet and " +
                    "the grey stay in place meanwhile, so only your apps return.",
            )
            Point(
                BedtimeIcons.Contrast,
                "when an app needs the door open",
                "a few apps will not run while blocking is on. if you allow it, a block can offer a short break for exactly " +
                    "that, then gather itself back together. leave it off if you would rather have no door at all.",
            )

            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
            Spacer(Modifier.height(28.dp))

            Text("how firm to make it", style = MaterialTheme.typography.headlineSmall, color = c.textNormal)
            Spacer(Modifier.height(8.dp))
            Text(
                "each block and schedule keeps its own settings, so a gentle morning and an unbending bedtime can sit " +
                    "side by side. three rough levels, all set inside the block itself:",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textMuted,
            )
            Spacer(Modifier.height(22.dp))
            Level(
                "gentle",
                "a short wait to leave early, and the pause for banking apps left on. enough friction to interrupt a " +
                    "reflex, easy to step out of when you truly mean to.",
            )
            Level(
                "firm",
                "a wait and a passage to type, set to grow with each unlock the same day. settings stay shut while it " +
                    "runs — and stay shut afterwards if you left early, so unlocking can't be used to soften it.",
            )
            Level(
                "uncompromising",
                "add a password someone else chose, switch the pause off so there is no short way out, and let the full " +
                    "screen wait with you if blocking is ever switched off.",
            )
            Text(
                "to set any of this, open a block or schedule. the ⓘ button at the top of it brings you back here.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textFaint,
            )

            Spacer(Modifier.height(26.dp))
            CtaButton("got it", onClick = onDone, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** One of the three strictness levels, named and spelled out. */
@Composable
private fun Level(name: String, body: String) {
    val c = Obsidian.colors
    Row(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
        Box(Modifier.width(3.dp).height(38.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.55f)))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, color = c.accentText, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(3.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
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
