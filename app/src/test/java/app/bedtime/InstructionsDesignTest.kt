package app.bedtime

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.BedtimeTheme
import app.bedtime.ui.theme.Obsidian
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

/**
 * Throwaway drafts of the instructions, to be looked at and chosen from. Two shapes:
 * a paged walk-through for the first run, and a one-page reference for settings and the ⓘ button.
 */
class InstructionsDesignTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 2000),
        theme = "android:Theme.Material.NoActionBar",
        showSystemUi = false,
    )

    private fun shot(content: @Composable () -> Unit) {
        paparazzi.snapshot("dark") { BedtimeTheme(darkTheme = true) { content() } }
    }

    @Test
    fun pagedA1Welcome() = shot {
        Page(
            step = 0,
            icon = BedtimeIcons.Refuge,
            title = "welcome to your refuge",
            body = "digital refuge sets aside hours when your phone asks less of you. a bedtime that repeats, " +
                "or a block you start whenever you need one.",
            note = "nothing you do here leaves your phone. there is no account, no sync, and the app has no " +
                "internet permission at all.",
            cta = "next",
        )
    }

    @Test
    fun pagedA2Session() = shot {
        Page(
            step = 1,
            icon = BedtimeIcons.Moon,
            title = "what a session does",
            body = "the apps you name step aside behind a calm screen. your home screen can become a short list " +
                "of only what you chose.",
            note = "the screen can fade to grey, and notifications wait quietly until it ends. music and alarms " +
                "always keep playing.",
            cta = "next",
        )
    }

    @Test
    fun pagedA3Leaving() = shot {
        Page(
            step = 2,
            icon = BedtimeIcons.Hourglass,
            title = "leaving early asks something",
            body = "want out before the end? the steps you chose stand in the way: a wait, a passage to type, " +
                "a password.",
            note = "just enough of a pause for the urge to pass. each one can be set to ask a little more than " +
                "the last, and that resets each morning.",
            cta = "next",
        )
    }

    @Test
    fun pagedA5Levels() = shot {
        val c = Obsidian.colors
        Scaffold(containerColor = c.bgPrimary) { padding ->
            Column(
                Modifier.fillMaxSize().padding(padding)
                    .background(Brush.verticalGradient(listOf(Color(0xFF1F2C25), c.bgPrimary)))
                    .padding(horizontal = 28.dp),
            ) {
                Spacer(Modifier.height(34.dp))
                Text("how firm to make it", fontSize = 27.sp, fontWeight = FontWeight.Light, color = c.textNormal)
                Spacer(Modifier.height(10.dp))
                Text(
                    "each block keeps its own settings, so a gentle morning and an unbending bedtime can sit side by side.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = c.textMuted,
                )
                Spacer(Modifier.height(28.dp))
                LevelCard("gentle", "a short wait, and an emergency break if you need one", listOf("wait 30s", "break on"))
                LevelCard("firm", "a wait and typing, growing with each unlock the same day", listOf("wait 2 min", "type 150", "1.5×"))
                LevelCard("uncompromising", "a password someone else chose, and no break at all", listOf("password", "break off"))
                Spacer(Modifier.weight(1f))
                Dots(4, 5)
                Spacer(Modifier.height(16.dp))
                CtaButton("get started", onClick = {}, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(28.dp))
            }
        }
    }

    @Test
    fun sheetReference() = shot { ReferenceSheet() }
}

// ---------------------------------------------------------------- A: paged walk-through

@Composable
private fun Page(step: Int, icon: ImageVector, title: String, body: String, note: String, cta: String) {
    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
                .background(Brush.verticalGradient(listOf(Color(0xFF1F2C25), c.bgPrimary)))
                .padding(horizontal = 28.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), horizontalArrangement = Arrangement.End) {
                Text("skip", style = MaterialTheme.typography.labelLarge, color = c.textFaint)
            }
            Spacer(Modifier.weight(0.8f))
            Box(
                Modifier.size(104.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(72.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) { Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(34.dp)) }
            }
            Spacer(Modifier.height(34.dp))
            Text(title, fontSize = 28.sp, fontWeight = FontWeight.Light, color = c.textNormal)
            Spacer(Modifier.height(14.dp))
            Text(body, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp), color = c.textMuted)
            Spacer(Modifier.height(18.dp))
            Row {
                Box(Modifier.width(3.dp).height(52.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.5f)))
                Spacer(Modifier.width(14.dp))
                Text(note, style = MaterialTheme.typography.bodyMedium, color = c.textFaint)
            }
            Spacer(Modifier.weight(1f))
            Dots(step, 5)
            Spacer(Modifier.height(16.dp))
            CtaButton(cta, onClick = {}, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun Dots(current: Int, total: Int) {
    val c = Obsidian.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(total) { i ->
            Box(
                Modifier.padding(horizontal = 4.dp).height(6.dp)
                    .width(if (i == current) 20.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (i == current) c.accent else c.textFaint.copy(alpha = 0.35f)),
            )
        }
    }
}

@Composable
private fun LevelCard(name: String, body: String, chips: List<String>) {
    val c = Obsidian.colors
    Column(
        Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(16.dp))
            .background(c.bgSecondary).padding(16.dp),
    ) {
        Text(name, style = MaterialTheme.typography.titleMedium, color = c.accentText, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            chips.forEach { chip ->
                Text(
                    chip,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textFaint,
                    modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, c.border, RoundedCornerShape(50))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

// ---------------------------------------------------------------- B: one-page reference

@Composable
private fun ReferenceSheet() {
    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 22.dp, end = 22.dp, top = 30.dp, bottom = 30.dp),
        ) {
            item {
                Text("how it works", fontSize = 27.sp, fontWeight = FontWeight.Light, color = c.textNormal)
                Spacer(Modifier.height(6.dp))
                Text("a page to come back to. every setting here lives in the block itself.", style = MaterialTheme.typography.bodyMedium, color = c.textFaint)
                Spacer(Modifier.height(26.dp))
            }
            item {
                Heading("a session can")
                Bullet("hide the apps you name behind a calm screen")
                Bullet("swap your home screen for a short list of what you chose")
                Bullet("fade the screen to grey")
                Bullet("hold notifications until it ends — music and alarms always play")
                Spacer(Modifier.height(22.dp))
            }
            item {
                Heading("your ways out")
                Bullet("**unlock early** — the steps you chose: wait, type, password")
                Bullet("**emergency break** — a short pause, if the block allows one")
                Bullet("**emergency button** — calling, and your always-available apps, without ending the session")
                Spacer(Modifier.height(22.dp))
            }
            item {
                Heading("while a session runs")
                Bullet("its settings can't be changed")
                Bullet("the screens that switch blocking off, and uninstalling, are covered")
                Bullet("if blocking goes off anyway, a full screen waits with you until it's back")
                Spacer(Modifier.height(22.dp))
            }
            item {
                Heading("three levels")
                Spacer(Modifier.height(4.dp))
                LevelRow("gentle", "wait 30s · break on")
                LevelRow("firm", "wait 2 min · type 150 · harder each time · break on")
                LevelRow("uncompromising", "password · no break · settings stay shut after unlocking")
                Spacer(Modifier.height(22.dp))
            }
            item {
                Heading("where to change it")
                Bullet("open a block or schedule — everything above is set there")
                Bullet("the ⓘ at the top of it brings you back to this page")
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = Obsidian.colors.accentText,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

@Composable
private fun Bullet(text: String) {
    val c = Obsidian.colors
    val bold = text.startsWith("**")
    val clean = text.replace("**", "")
    Row(Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
        Box(Modifier.padding(top = 8.dp).size(4.dp).clip(CircleShape).background(c.textFaint))
        Spacer(Modifier.width(12.dp))
        Text(
            clean,
            style = MaterialTheme.typography.bodyMedium,
            color = if (bold) c.textNormal else c.textMuted,
        )
    }
}

@Composable
private fun LevelRow(name: String, body: String) {
    val c = Obsidian.colors
    Row(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        Box(Modifier.width(3.dp).height(34.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.55f)))
        Spacer(Modifier.width(12.dp))
        Column {
            Text(name, style = MaterialTheme.typography.titleSmall, color = c.textNormal)
            Text(body, style = MaterialTheme.typography.bodySmall, color = c.textFaint)
        }
    }
}
