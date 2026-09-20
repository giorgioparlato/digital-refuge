package app.bedtime.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
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

            Spacer(Modifier.height(20.dp))
            CtaButton("got it", onClick = onDone, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Point(icon: ImageVector, title: String, body: String) {
    val c = Obsidian.colors
    Row(Modifier.fillMaxWidth().padding(bottom = 22.dp), verticalAlignment = Alignment.Top) {
        androidx.compose.foundation.layout.Box(
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
