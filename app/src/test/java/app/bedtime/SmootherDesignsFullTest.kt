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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.TextButton
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
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.IconBadge
import app.bedtime.ui.components.NumberStepper
import app.bedtime.ui.components.ObsidianTextField
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.ui.components.OptionRow
import app.bedtime.ui.components.SegmentedChoice
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.TimeTile
import app.bedtime.ui.theme.BedtimeTheme
import app.bedtime.ui.theme.Obsidian
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

/**
 * Option C, with nothing left out: every group the real pages have, so the whole page can be judged
 * rather than its first screenful. Each page comes in two halves, top and bottom, because a single
 * tall image gets squeezed down to something unreadable.
 *
 * Throwaway, like [SmootherDesignsTest]. Delete once the real pages are built.
 */
class SmootherDesignsFullTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.NoActionBar",
        showSystemUi = false,
    )

    private fun shot(content: @Composable () -> Unit) {
        paparazzi.snapshot("dark") { BedtimeTheme(darkTheme = true) { content() } }
    }

    // ---------------------------------------------------------------- shared

    /**
     * The fade, shorter than in the first drafts. It ran 420dp there and tinted the whole page green;
     * at 260dp it behaves like the home screen's, which dies out just below its header.
     */
    @Composable
    private fun Fade(height: androidx.compose.ui.unit.Dp = 260.dp) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(height)
                .background(Brush.verticalGradient(listOf(Deep, Obsidian.colors.bgPrimary))),
        )
    }

    /** A group's name, in the accent, with its explanation underneath where there is one. */
    @Composable
    private fun Label(text: String, note: String? = null) {
        val c = Obsidian.colors
        Column(Modifier.padding(start = 2.dp, top = 26.dp, bottom = 4.dp)) {
            Text(text, style = MaterialTheme.typography.labelLarge, color = c.accentText)
            if (note != null) {
                Text(
                    note,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textFaint,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }

    /** A row on the bare background, with a hairline under it unless it closes its group. */
    @Composable
    private fun Row_(
        icon: ImageVector,
        title: String,
        description: String,
        last: Boolean = false,
        trailing: @Composable () -> Unit = { Chevron() },
    ) {
        val c = Obsidian.colors
        Column {
            OptionRow(icon, title, description = description, trailing = trailing)
            if (!last) HorizontalDivider(Modifier.padding(start = 52.dp), color = c.border.copy(alpha = 0.5f))
        }
    }

    /** An indented control under a row, for the steppers and the segmented choices. */
    @Composable
    private fun Sub(label: String, last: Boolean = false, content: @Composable () -> Unit) {
        val c = Obsidian.colors
        Column {
            Row(
                Modifier.fillMaxWidth().padding(start = 52.dp, top = 2.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.weight(1f))
                content()
            }
            if (!last) HorizontalDivider(Modifier.padding(start = 52.dp), color = c.border.copy(alpha = 0.5f))
        }
    }

    @Composable
    private fun BottomBar(label: String) {
        val c = Obsidian.colors
        Column(Modifier.fillMaxWidth().background(c.bgPrimary)) {
            HorizontalDivider(color = c.border)
            Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                CtaButton(label, onClick = {}, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    // ---------------------------------------------------------------- settings, whole

    @Test
    fun settingsCWhole() = shot {
        val c = Obsidian.colors
        Box(Modifier.fillMaxSize().background(c.bgPrimary)) {
            Fade()
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.padding(horizontal = 22.dp).padding(top = 30.dp, bottom = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(BedtimeIcons.Refuge, size = 46.dp)
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("settings", fontSize = 26.sp, fontWeight = FontWeight.Light, color = c.textNormal)
                            Text("version 0.7.7.12 · all on this phone", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                        }
                    }
                }
                Column(Modifier.padding(horizontal = 22.dp)) {
                    Label("getting your bearings")
                    Row_(Icons.Default.Info, "How it works", "What a session locks, and your ways out")
                    Row_(BedtimeIcons.Leaf, "The intro again", "The short walk-through from the first launch", last = true)

                    Label("your phone")
                    Row_(Icons.Default.Build, "Permissions & setup", "All set")
                    Row_(Icons.Default.Home, "Minimal home screen", "Colours, text size and what's shown")
                    Row_(BedtimeIcons.Grid, "App groups", "2 groups", last = true)

                    Label("emergency", "What the emergency button on the minimal home offers, besides calling.")
                    Row_(Icons.Default.Warning, "Always-available apps", "3 apps, never blocked", last = true)

                    Label("staying blocked", "How firmly a session holds. Tap “how it works” for the full picture.")
                    Row_(BedtimeIcons.Refuge, "Lock changes during sessions", "Covers the Settings screens that switch blocking off") {
                        ObsidianToggle(true, {})
                    }
                    Row_(Icons.Default.Warning, "Full-screen reminder if blocking goes off", "Takes over the screen until it's back on", last = true) {
                        ObsidianToggle(true, {})
                    }

                    Label("backup", "Keep your schedules and settings in a file, to restore after reinstalling.")
                    Row_(Icons.Default.Share, "Export settings", "Save schedules, blocks, groups and stats to a file")
                    Row_(BedtimeIcons.Refuge, "Import settings", "Replace everything with a saved file", last = true)

                    Label("home-screen widget", "A one-tap button on your home screen that starts a block. Add one for:")
                    BlockLine("Deep work · 45 min")
                    BlockLine("Reading · 30 min", last = true)

                    Label("quick settings tile", "Edit your quick settings and add the “refuge” tile. Tapping it starts:")
                    TileChoice("Deep work · 45 min", selected = true)
                    TileChoice("Reading · 30 min", selected = false, last = true)
                    Spacer(Modifier.height(28.dp))
                }
            }
        }
    }

    @Composable
    private fun BlockLine(name: String, last: Boolean = false) {
        val c = Obsidian.colors
        Column {
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.bodyLarge, color = c.textNormal, modifier = Modifier.weight(1f))
                TextButton(onClick = {}) { Text("Add", color = c.accentText, fontWeight = FontWeight.SemiBold) }
            }
            if (!last) HorizontalDivider(color = c.border.copy(alpha = 0.5f))
        }
    }

    @Composable
    private fun TileChoice(name: String, selected: Boolean, last: Boolean = false) {
        val c = Obsidian.colors
        Column {
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = selected,
                    onClick = {},
                    colors = RadioButtonDefaults.colors(selectedColor = c.accent, unselectedColor = c.textFaint),
                )
                Text(name, style = MaterialTheme.typography.bodyLarge, color = c.textNormal)
            }
            if (!last) HorizontalDivider(Modifier.padding(start = 52.dp), color = c.border.copy(alpha = 0.5f))
        }
    }

    // ---------------------------------------------------------------- schedule, whole

    @Test
    fun scheduleCWhole() = shot {
        val c = Obsidian.colors
        Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
            Box(Modifier.weight(1f)) {
                Fade(300.dp)
                Column(Modifier.fillMaxSize()) {
                    ScheduleHeader()
                    Column(Modifier.padding(horizontal = 22.dp)) {
                        Label("name")
                        Box(Modifier.padding(top = 6.dp, bottom = 4.dp)) {
                            ObsidianTextField(value = "Bedtime", onValueChange = {}, placeholder = "e.g. Bedtime")
                        }

                        Label("while it's on")
                        Row_(BedtimeIcons.Block, "Block apps", "14 apps")
                        Row_(BedtimeIcons.Contrast, "Greyscale", "Fade apps to black and white") { ObsidianToggle(true, {}) }
                        Row_(BedtimeIcons.Contrast, "Keep home screen in colour", "Only apps turn grey") { ObsidianToggle(true, {}) }
                        Row_(Icons.Default.Home, "Minimal mode", "Swap your home screen for a calm list") { ObsidianToggle(true, {}) }
                        Row_(BedtimeIcons.Grid, "Allowed apps", "6 apps, plus the phone app", last = true)

                        Label("notifications")
                        Box(Modifier.padding(top = 6.dp, bottom = 8.dp)) {
                            SegmentedChoice(listOf("Off", "Priority", "Silence"), selected = 2, onSelect = {})
                        }
                        Text(
                            "No calls or notification sounds. Music, videos and alarms still play.",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textMuted,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                        Row_(Icons.Default.Notifications, "Hide notifications", "Held back while it's on, then they all come back", last = true) {
                            ObsidianToggle(false, {})
                        }

                        Label("leaving early", "Make unlocking take a little effort. The steps you pick run in this order.")
                        Row_(BedtimeIcons.Hourglass, "Wait it out", "Start a timer, come back when it's done") { ObsidianToggle(true, {}) }
                        Sub("Timer") {
                            NumberStepper(120, {}, 5..3600, step = 5, presets = listOf(10, 30, 60, 120), title = "timer", format = { "${it / 60} min" })
                        }
                        Row_(BedtimeIcons.Book, "Type a passage", "Copy a few lines out, one letter at a time") { ObsidianToggle(true, {}) }
                        Row_(Icons.Default.Lock, "Password", "Something long you'd rather not type", last = true) { ObsidianToggle(false, {}) }

                        Label("when you unlock")
                        Box(Modifier.padding(top = 6.dp, bottom = 8.dp)) {
                            SegmentedChoice(listOf("End for today", "Take a break"), selected = 1, onSelect = {})
                        }
                        Text(
                            "It switches itself back on after your break.",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textMuted,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                        Sub("Break length") {
                            NumberStepper(5, {}, 1..240, suffix = " min", presets = listOf(5, 10, 15), title = "break length")
                        }
                        Row_(Icons.Default.Lock, "Lock settings afterwards", "Keeps this shut until the session would have ended", last = true) {
                            ObsidianToggle(true, {})
                        }

                        Label("when it can be changed", "Put this schedule out of reach of the hours that would talk you out of it.")
                        Row_(Icons.Default.Lock, "Only at certain hours", "Outside them it can't be edited, switched off or deleted.", last = true) {
                            ObsidianToggle(true, {})
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TimeTile("From", "08:00", Modifier.weight(1f)) {}
                            TimeTile("Until", "20:00", Modifier.weight(1f)) {}
                        }

                        Label("emergency break", "A short step out for whatever genuinely can't wait, without ending the session.")
                        Row_(BedtimeIcons.Hourglass, "Allow an emergency break", "A full screen brings you back. It counts as an escape.", last = true) {
                            ObsidianToggle(true, {})
                        }
                        Sub("Break length", last = true) {
                            NumberStepper(2, {}, 1..60, suffix = " min", presets = listOf(1, 2, 3, 5), title = "break length")
                        }

                        Spacer(Modifier.height(10.dp))
                        TextButton(onClick = {}, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Delete schedule", color = c.red, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
            BottomBar("Save changes")
        }
    }

    /** The hours as the headline, with the days under them. */
    @Composable
    private fun ScheduleHeader() {
        val c = Obsidian.colors
        Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 28.dp, bottom = 10.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        tint = c.textMuted,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(Modifier.width(14.dp))
                    Text("bedtime", style = MaterialTheme.typography.labelLarge, color = c.textFaint, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Info, contentDescription = null, tint = c.textFaint, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(26.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("22:00", fontSize = 40.sp, fontWeight = FontWeight.Light, color = c.textNormal)
                    Text("  →  ", fontSize = 26.sp, color = c.textFaint, modifier = Modifier.padding(bottom = 4.dp))
                    Text("07:00", fontSize = 40.sp, fontWeight = FontWeight.Light, color = c.textNormal)
                }
                Spacer(Modifier.height(6.dp))
                Text("nine hours, every night · ends next day", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                        Box(
                            Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(c.accent.copy(alpha = 0.18f))
                                .border(1.dp, c.accent.copy(alpha = 0.45f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) { Text(day, style = MaterialTheme.typography.labelMedium, color = c.accentText) }
                    }
                }
            }
        }
    }

    /**
     * A block has no hours, only a length, so the same header carries that instead — otherwise the
     * page would open on an empty space where a schedule shows its times.
     */
    @Test
    fun blockCHeader() = paparazzi.snapshot("dark") {
        BedtimeTheme(darkTheme = true) {
            val c = Obsidian.colors
            Box(Modifier.fillMaxSize().background(c.bgPrimary)) {
                Fade()
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 28.dp, bottom = 10.dp)) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    tint = c.textMuted,
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(Modifier.width(14.dp))
                                Text("edit block", style = MaterialTheme.typography.labelLarge, color = c.textFaint, modifier = Modifier.weight(1f))
                                Icon(Icons.Default.Info, contentDescription = null, tint = c.textFaint, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.height(26.dp))
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text("45", fontSize = 44.sp, fontWeight = FontWeight.Light, color = c.textNormal)
                                Text(
                                    " minutes",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Light,
                                    color = c.textMuted,
                                    modifier = Modifier.padding(bottom = 4.dp),
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text("deep work · you can change it each time you start", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                            Spacer(Modifier.height(18.dp))
                            Box(Modifier.width(220.dp)) {
                                NumberStepper(45, {}, 5..480, step = 5, suffix = " min", presets = listOf(25, 45, 60, 90), title = "default length")
                            }
                        }
                    }
                    Column(Modifier.padding(horizontal = 22.dp)) {
                        Label("while it's on")
                        Row_(BedtimeIcons.Block, "Block apps", "22 apps")
                        Row_(BedtimeIcons.Contrast, "Greyscale", "Fade apps to black and white") { ObsidianToggle(true, {}) }
                        Row_(Icons.Default.Home, "Minimal mode", "Swap your home screen for a calm list", last = true) {
                            ObsidianToggle(false, {})
                        }
                    }
                }
            }
        }
    }

    private companion object {
        val Deep = Color(0xFF1F2C25)
    }
}
