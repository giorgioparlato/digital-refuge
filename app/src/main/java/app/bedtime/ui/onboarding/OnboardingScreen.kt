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
            Text("how a session holds", style = MaterialTheme.typography.headlineMedium, color = c.textNormal)
            Spacer(Modifier.height(8.dp))
            Text(
                "digital refuge adds friction to impulses, not a prison. here's what a running schedule or block does, and how you leave.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textMuted,
            )
            Spacer(Modifier.height(28.dp))

            Point(BedtimeIcons.Moon, "it blocks and quiets", "distracting apps step aside, the screen can fade to grey, and notifications wait until the session ends.")
            Point(BedtimeIcons.Hourglass, "leaving early takes effort", "the unlock steps you choose — wait, type, password — run in order. you can make each early unlock harder than the last.")
            Point(BedtimeIcons.Refuge, "it stays put", "during a session, the settings screens that switch blocking off, and the uninstall prompt, are covered. no two-tap escape. (you can turn this off.)")
            Point(BedtimeIcons.Target, "if blocking still goes off", "should it be switched off anyway, a full-screen reminder brings you back until you switch it on again. do not disturb and greyscale stay on meanwhile.")
            Point(BedtimeIcons.Contrast, "banking and id apps", "some (like bankid) refuse to run while blocking is on. if you allow it, you can pause for a short break, set per block. it counts as an escape. for total strictness, leave the pause off.")

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
