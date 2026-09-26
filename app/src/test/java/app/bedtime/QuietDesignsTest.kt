package app.bedtime

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.BedtimeTheme
import app.bedtime.ui.theme.Obsidian
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

/**
 * Throwaway drafts of the quieter unlock pages, to be looked at and chosen from. Three degrees of
 * quiet for the wait page, and the same idea applied to the typing page.
 */
class QuietDesignsTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.NoActionBar",
        showSystemUi = false,
    )

    private fun shot(content: @Composable () -> Unit) {
        paparazzi.snapshot("dark") {
            BedtimeTheme(darkTheme = true) {
                Scaffold(containerColor = Obsidian.colors.bgPrimary) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding).padding(20.dp)) { content() }
                }
            }
        }
    }

    // ---------------------------------------------------------------- shared pieces

    /** The countdown ring, at whatever size a draft wants it. */
    @Composable
    private fun Ring(size: Int, label: String = "06:12", progress: Float = 0.38f) {
        val c = Obsidian.colors
        Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = c.accent,
                strokeWidth = (size / 26).dp,
                trackColor = c.interactive,
                strokeCap = StrokeCap.Round,
            )
            Text(
                label,
                style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Medium,
                color = c.textNormal,
            )
        }
    }

    /** The small print, wherever a draft puts it. */
    @Composable
    private fun Footnote(text: String, modifier: Modifier = Modifier) {
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = Obsidian.colors.textFaint,
            textAlign = TextAlign.Center,
            modifier = modifier.fillMaxWidth(),
        )
    }

    @Composable
    private fun StepChips(current: Int) {
        val c = Obsidian.colors
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("wait", "type").forEachIndexed { index, label ->
                val on = index == current
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (on) c.textOnAccent else c.textMuted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (on) c.accentFill else c.interactive)
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }
    }

    /** Two dots instead of two words: the same information, less furniture. */
    @Composable
    private fun StepDots(current: Int) {
        val c = Obsidian.colors
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(2) { index ->
                Box(
                    Modifier
                        .height(5.dp)
                        .width(if (index == current) 18.dp else 5.dp)
                        .clip(CircleShape)
                        .background(if (index == current) c.accent else c.textFaint.copy(alpha = 0.35f)),
                )
            }
            Spacer(Modifier.width(6.dp))
            Text("step ${current + 1} of 2", style = MaterialTheme.typography.labelSmall, color = c.textFaint)
        }
    }

    private val smallPrint = "the timer only runs while this screen is open, and starts over if you back out."

    // ---------------------------------------------------------------- wait page, option A

    /** As asked: header as it is, "take a breath" alone over the timer, the rest small at the bottom. */
    @Test
    fun waitA() = shot {
        val c = Obsidian.colors
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Leaving early?", style = MaterialTheme.typography.headlineSmall, color = c.textNormal)
                Text(
                    "Finish these steps to unlock Bedtime. This ends the current session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textMuted,
                )
            }
            StepChips(current = 0)
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text("Take a breath.", style = MaterialTheme.typography.bodyLarge, color = c.textMuted)
                Ring(208)
                CtaButton("Waiting…", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
            }
            Spacer(Modifier.weight(1f))
            Footnote(smallPrint)
            Text(
                "Never mind, I'll stay focused",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            )
        }
    }

    // ---------------------------------------------------------------- wait page, option B

    /** Quieter still: the header shrinks to one line, the chips become dots, the timer leads. */
    @Test
    fun waitB() = shot {
        val c = Obsidian.colors
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Unlocking Bedtime", style = MaterialTheme.typography.bodyMedium, color = c.textFaint)
            StepDots(current = 0)
            Spacer(Modifier.weight(0.6f))
            Column(
                Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(26.dp),
            ) {
                Text("Take a breath.", style = MaterialTheme.typography.titleMedium, color = c.textMuted)
                Ring(232)
            }
            Spacer(Modifier.weight(1f))
            CtaButton("Waiting…", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
            Footnote("$smallPrint this ends the current session.")
            Text(
                "Never mind, I'll stay focused",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
        }
    }

    // ---------------------------------------------------------------- wait page, option C

    /** Bare: the timer and three words. Everything else is small print at the foot of the page. */
    @Test
    fun waitC() = shot {
        val c = Obsidian.colors
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Spacer(Modifier.weight(1f))
            Text("Take a breath.", style = MaterialTheme.typography.titleLarge, color = c.textMuted)
            Spacer(Modifier.height(34.dp))
            Ring(248)
            Spacer(Modifier.height(1f.let { 34.dp }))
            StepDots(current = 0)
            Spacer(Modifier.weight(1f))
            CtaButton("Waiting…", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            Footnote("unlocking bedtime ends the current session. $smallPrint")
            Spacer(Modifier.height(10.dp))
            Text(
                "Never mind, I'll stay focused",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
        }
    }

    // ---------------------------------------------------------------- typing page, same treatment

    /** The typing page as it is now, for comparison: guidance on top, above the text. */
    @Test
    fun typeNow() = shot {
        val c = Obsidian.colors
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Leaving early?", style = MaterialTheme.typography.headlineSmall, color = c.textNormal)
                Text(
                    "Finish these steps to unlock Bedtime. This ends the current session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textMuted,
                )
            }
            StepChips(current = 1)
            Text(
                "Copy the text below, one character at a time. Spaces and punctuation fill themselves in; " +
                    "pasting and typos are rejected.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            TargetText()
            Progress()
            Field()
        }
    }

    /** Quieter: the guidance goes to the foot, and the text to copy is the first thing you see. */
    @Test
    fun typeQuiet() = shot {
        val c = Obsidian.colors
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("Unlocking Bedtime", style = MaterialTheme.typography.bodyMedium, color = c.textFaint)
            StepDots(current = 1)
            Spacer(Modifier.height(2.dp))
            TargetText()
            Progress()
            Field()
            Spacer(Modifier.weight(1f))
            Footnote(
                "spaces and punctuation fill themselves in; pasting and typos are rejected. " +
                    "unlocking ends the current session.",
            )
            Text(
                "Never mind, I'll stay focused",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            )
        }
    }

    @Composable
    private fun TargetText() {
        val c = Obsidian.colors
        val target = "the land knows you, even when you are lost. our indigenous herbalists say to pay " +
            "attention when plants come to you"
        val done = 46
        Text(
            buildAnnotatedString {
                append(target)
                addStyle(SpanStyle(color = c.textFaint), 0, done)
                addStyle(SpanStyle(color = c.textNormal, background = c.accent.copy(alpha = 0.35f)), done, done + 1)
                addStyle(SpanStyle(color = c.textNormal), done + 1, target.length)
            },
            fontSize = 17.sp,
            lineHeight = 26.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(c.bgPrimaryAlt)
                .border(1.dp, c.border, RoundedCornerShape(12.dp))
                .padding(16.dp),
        )
    }

    @Composable
    private fun Progress() {
        val c = Obsidian.colors
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(50))
                    .background(c.interactive),
            ) {
                Box(Modifier.fillMaxWidth(0.37f).height(6.dp).clip(RoundedCornerShape(50)).background(c.accent))
            }
            Spacer(Modifier.width(12.dp))
            Text("46/124", style = MaterialTheme.typography.labelLarge, color = c.textMuted)
        }
    }

    @Composable
    private fun Field() {
        val c = Obsidian.colors
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, c.border, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(
                "the land knows you, even when yo",
                fontFamily = FontFamily.Monospace,
                fontSize = 16.sp,
                color = c.textNormal,
            )
        }
    }
}
