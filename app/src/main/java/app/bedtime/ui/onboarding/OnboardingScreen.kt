package app.bedtime.ui.onboarding

import app.bedtime.ui.components.LineIcons
import androidx.compose.material.icons.filled.Edit

import androidx.compose.material.icons.filled.Lock

import androidx.compose.material.icons.filled.Home

import androidx.compose.material.icons.Icons

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
import androidx.compose.ui.unit.sp
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian

/** What a session does and how to leave it. Shown once, and reachable from settings and any block. */
@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("how it works", onBack = onDone) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = PageGutter)
                .padding(bottom = 20.dp),
        ) {
            Text(
                "digital refuge blocks your distracting apps when you want a break from them. here's what happens " +
                    "during a session, and how to leave early if you need to.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textMuted,
            )
            Spacer(Modifier.height(30.dp))

            Point(
                BedtimeIcons.Moon,
                "a session begins",
                "at the times you set, or when you start a block, the apps you chose stop opening. your home screen " +
                    "can switch to a short list of apps, the screen can turn grey, and notifications can wait. music " +
                    "and alarms always work.",
            )
            Point(
                BedtimeIcons.Hourglass,
                "leaving early takes the steps you chose",
                "you choose what leaving early asks for: a wait, a passage to copy, a few words of why, or a " +
                    "password. the steps can get longer each time that day, and reset every morning.",
            )
            Point(
                BedtimeIcons.Refuge,
                "it stays put while it runs",
                "you can't change a session's settings while it runs, otherwise the easiest way out would be to edit it.",
            )
            Point(
                BedtimeIcons.Contrast,
                "an emergency break",
                "for something that can't wait, take a minute with no steps, without ending the session. when it's " +
                    "over, refuge brings you back.",
            )
            Point(
                BedtimeIcons.Leaf,
                "limits hold every day",
                "limits work outside sessions too: a short pause before an app opens, or a daily time budget. making " +
                    "one stricter works straight away; loosening or deleting it starts the next day.",
            )

            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
            Spacer(Modifier.height(28.dp))

            Text("around the sessions", fontSize = 22.sp, fontWeight = FontWeight.Light, color = c.textNormal)
            Spacer(Modifier.height(10.dp))
            Text(
                "the rest of refuge helps make your phone quieter, in a session or not. all of it is optional, and " +
                    "you set it up in settings.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textMuted,
            )
            Spacer(Modifier.height(24.dp))
            Point(
                LineIcons.Home,
                "a quieter home screen",
                "use refuge as your home screen: the time, a few widgets and your apps as words. use it only during " +
                    "sessions, or as your everyday launcher, with search for apps, people and more.",
            )
            Point(
                LineIcons.Lock,
                "a lock screen of its own",
                "use refuge's lockscreen: the time, what's next today, and only the notifications from the apps and " +
                    "people you choose. tap or swipe up to unlock.",
            )
            Point(
                LineIcons.Edit,
                "notes and habits",
                "jot a quick note from anywhere into refuge's notes or your own files, and tick off habits here or " +
                    "from your screens.",
            )
            Point(
                BedtimeIcons.Target,
                "insights",
                "see your screen time, how your sessions went and which habits you kept. refuge only counts which " +
                    "app is open, never what's in it.",
            )

            Spacer(Modifier.height(6.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
            Spacer(Modifier.height(28.dp))

            Text("how firm to make it", fontSize = 22.sp, fontWeight = FontWeight.Light, color = c.textNormal)
            Spacer(Modifier.height(10.dp))
            Text(
                "two things always apply: you can't change a session while it runs, and leaving early takes the " +
                    "steps you chose. everything below is an extra switch in each schedule and block: turn on the " +
                    "ones you want.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textMuted,
            )
            Spacer(Modifier.height(24.dp))

            Switch(
                "1",
                "harder each time",
                "each time you leave early on the same day, the wait and the typing get longer. it resets every morning.",
                "how hard to leave",
            )
            Switch(
                "2",
                "keep this shut after leaving early",
                "if you leave a session early, its apps stay blocked until it would have ended.",
                "the ways around it",
            )
            Switch(
                "3",
                "no emergency break",
                "turn the emergency break off, and the unlock steps are the only way out.",
                "the ways around it",
            )
            Switch(
                "4",
                "cover android's off switches",
                "refuge covers the android settings that would turn blocking off or uninstall the app.",
                "the ways around it",
            )
            Switch(
                "5",
                "full-screen reminder",
                "if blocking gets turned off, a full screen stays up until you turn it back on.",
                "the ways around it",
            )

            Text(
                "open a block or schedule to set the first three. the last two are in settings › focus. everything stays " +
                    "on your phone: there's no account, and the app can't use the internet.",
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
