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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import app.bedtime.ui.components.NumberStepper
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
 * The settings page's treatment applied to the schedule page: the hours become the header, nine
 * groups become two, and every row says what it is set to instead of carrying its controls.
 *
 * Throwaway. Delete once the real page is built.
 */
class ScheduleStructureTest {
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

    @Composable
    private fun Fade(height: Int = 300) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(height.dp)
                .background(Brush.verticalGradient(listOf(Deep, Obsidian.colors.bgPrimary))),
        )
    }

    @Composable
    private fun Label(text: String, note: String? = null) {
        val c = Obsidian.colors
        Column(Modifier.padding(start = 2.dp, top = 22.dp, bottom = 2.dp)) {
            Text(text, style = MaterialTheme.typography.labelLarge, color = c.accentText)
            if (note != null) {
                Text(note, style = MaterialTheme.typography.bodySmall, color = c.textFaint, modifier = Modifier.padding(top = 2.dp))
            }
        }
    }

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

    /** The hours as the headline. The pencil by the name is where renaming lives. */
    @Composable
    private fun Header() {
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
                    Text("bedtime", style = MaterialTheme.typography.labelLarge, color = c.textFaint)
                    Spacer(Modifier.width(7.dp))
                    Icon(Icons.Default.Create, contentDescription = null, tint = c.textFaint, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.weight(1f))
                    Icon(Icons.Default.Info, contentDescription = null, tint = c.textFaint, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("22:00", fontSize = 40.sp, fontWeight = FontWeight.Light, color = c.textNormal)
                    Text("  →  ", fontSize = 26.sp, color = c.textFaint, modifier = Modifier.padding(bottom = 4.dp))
                    Text("07:00", fontSize = 40.sp, fontWeight = FontWeight.Light, color = c.textNormal)
                }
                Spacer(Modifier.height(6.dp))
                Text("nine hours, every night · ends next day", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                Spacer(Modifier.height(18.dp))
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

    // ---------------------------------------------------------------- A: every row states its value

    /**
     * Nine groups become two, and no row carries a control it doesn't need: each says what it is set
     * to and opens its own small page. Eight rows for what was twenty-odd.
     */
    @Test
    fun schedA() = shot {
        val c = Obsidian.colors
        Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
            Box(Modifier.weight(1f)) {
                Fade()
                Column(Modifier.fillMaxSize()) {
                    Header()
                    Column(Modifier.padding(horizontal = 22.dp)) {
                        Label("what it does")
                        Row_(BedtimeIcons.Block, "Blocked apps", "14 apps")
                        Row_(Icons.Default.Home, "Minimal home screen", "On · 6 apps allowed")
                        Row_(BedtimeIcons.Contrast, "Greyscale", "On · this app keeps its colour")
                        Row_(Icons.Default.Notifications, "Notifications", "Silenced, and held back until morning", last = true)

                        Label("how hard to leave")
                        Row_(BedtimeIcons.Hourglass, "The way out", "Wait 2 min, then type a passage")
                        Row_(Icons.Default.Lock, "When you unlock", "A 5-minute break, then locked until morning")
                        Row_(BedtimeIcons.Refuge, "Emergency break", "2 minutes, any time")
                        Row_(Icons.Default.Lock, "When it can be changed", "Only between 08:00 and 20:00", last = true)

                        Spacer(Modifier.height(14.dp))
                        TextButton(onClick = {}, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Delete schedule", color = c.red, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            BottomBar("Save changes")
        }
    }

    // ---------------------------------------------------------------- B: controls stay, strictness folds

    /**
     * The conservative middle: what a session does keeps its switches on the page, because those are
     * the ones you flick while deciding. Everything about escaping goes behind a single row that
     * states the whole bargain in one line.
     */
    @Test
    fun schedB() = shot {
        val c = Obsidian.colors
        Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
            Box(Modifier.weight(1f)) {
                Fade()
                Column(Modifier.fillMaxSize()) {
                    Header()
                    Column(Modifier.padding(horizontal = 22.dp)) {
                        Label("what it does")
                        Row_(BedtimeIcons.Block, "Blocked apps", "14 apps")
                        Row_(BedtimeIcons.Contrast, "Greyscale", "Fade apps to black and white") { ObsidianToggle(true, {}) }
                        Row_(Icons.Default.Home, "Minimal home screen", "Swap your home screen for a calm list") {
                            ObsidianToggle(true, {})
                        }
                        Row_(BedtimeIcons.Grid, "Allowed apps", "6 apps, plus the phone app")
                        Row_(Icons.Default.Notifications, "Notifications", "Silenced, and held back until morning", last = true)

                        Label("how hard to leave")
                        Row_(
                            BedtimeIcons.Hourglass,
                            "The way out",
                            "Wait 2 min, then a passage · 5-min break · changeable 08:00–20:00",
                            last = true,
                        )

                        Spacer(Modifier.height(14.dp))
                        TextButton(onClick = {}, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Delete schedule", color = c.red, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            BottomBar("Save changes")
        }
    }

    // ---------------------------------------------------------------- the page behind "how hard to leave"

    /** Everything about escaping, in one place, where the four old groups become three questions. */
    @Test
    fun schedWayOut() = shot {
        val c = Obsidian.colors
        Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
            Box(Modifier.weight(1f)) {
                Fade(240)
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 28.dp, bottom = 6.dp)) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null,
                                    tint = c.textMuted,
                                    modifier = Modifier.size(22.dp),
                                )
                                Spacer(Modifier.width(14.dp))
                                Text("bedtime", style = MaterialTheme.typography.labelLarge, color = c.textFaint)
                            }
                            Spacer(Modifier.height(20.dp))
                            Text("how hard to leave", fontSize = 26.sp, fontWeight = FontWeight.Light, color = c.textNormal)
                            Spacer(Modifier.height(5.dp))
                            Text(
                                "turn them all on and there is no way out but the steps you set yourself",
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.textMuted,
                            )
                        }
                    }
                    Column(Modifier.padding(horizontal = 22.dp)) {
                        Label("the steps", "They run in this order.")
                        Row_(BedtimeIcons.Hourglass, "Wait it out", "Start a timer, come back when it's done") {
                            ObsidianToggle(true, {})
                        }
                        Sub("Timer") {
                            NumberStepper(120, {}, 5..3600, step = 5, presets = listOf(10, 30, 60, 120), title = "timer", format = { "${it / 60} min" })
                        }
                        Row_(BedtimeIcons.Book, "Type a passage", "Copy a few lines out, one letter at a time") {
                            ObsidianToggle(true, {})
                        }
                        Row_(Icons.Default.Lock, "Password", "Something long you'd rather not type", last = true) {
                            ObsidianToggle(false, {})
                        }

                        Label("what unlocking gets you")
                        Box(Modifier.padding(top = 6.dp, bottom = 8.dp)) {
                            SegmentedChoice(listOf("End for today", "Take a break"), selected = 1, onSelect = {})
                        }
                        Sub("Break length") {
                            NumberStepper(5, {}, 1..240, suffix = " min", presets = listOf(5, 10, 15), title = "break length")
                        }
                        Row_(Icons.Default.Lock, "Lock settings afterwards", "Keeps this shut until it would have ended", last = true) {
                            ObsidianToggle(true, {})
                        }

                        Label("the doors left open")
                        Row_(BedtimeIcons.Refuge, "Emergency break", "A short step out. It counts as an escape.") {
                            ObsidianToggle(true, {})
                        }
                        Sub("How long you can step out") {
                            NumberStepper(2, {}, 1..60, suffix = " min", presets = listOf(1, 2, 3, 5), title = "step out")
                        }
                        Row_(Icons.Default.Lock, "Changeable only at certain hours", "Outside them this page is read-only", last = true) {
                            ObsidianToggle(true, {})
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TimeTile("From", "08:00", Modifier.weight(1f)) {}
                            TimeTile("Until", "20:00", Modifier.weight(1f)) {}
                        }
                    }
                }
            }
            BottomBar("Save changes")
        }
    }

    // ---------------------------------------------------------------- C: folds, in place

    /** A heading you can open, carrying the whole section's answer in one line. */
    @Composable
    private fun FoldHeading(title: String, summary: String, open: Boolean) {
        val c = Obsidian.colors
        Column {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 58.dp).padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f).padding(end = 10.dp)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (open) c.accentText else c.textNormal,
                    )
                    Text(summary, style = MaterialTheme.typography.bodySmall, color = c.textFaint)
                }
                Icon(
                    if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = c.textFaint,
                )
            }
            HorizontalDivider(color = c.border.copy(alpha = if (open) 0f else 0.5f))
        }
    }

    /**
     * The whole schedule as three sentences, each of which opens where it stands.
     *
     * This page is a form with a draft and a save button, not a menu of destinations, so detail that
     * opens in place beats detail that opens elsewhere: the hours stay in sight at the top, save stays
     * at the foot, and settings that only matter together stay on the same screen.
     */
    @Test
    fun schedC() = shot {
        val c = Obsidian.colors
        Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
            Box(Modifier.weight(1f)) {
                Fade()
                Column(Modifier.fillMaxSize()) {
                    Header()
                    Column(Modifier.padding(horizontal = 22.dp, vertical = 10.dp)) {
                        FoldHeading("What it does", "14 apps blocked · minimal home · greyscale · silenced", open = false)
                        FoldHeading("How hard to leave", "Wait 2 min, then a passage · a 5-minute break", open = false)
                        FoldHeading("The doors left open", "Emergency break 2 min · changeable 08:00–20:00", open = false)
                        Spacer(Modifier.height(24.dp))
                        TextButton(onClick = {}, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                            Text("Delete schedule", color = c.red, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
            BottomBar("Save changes")
        }
    }

    /** The same page with one section open, to see what opening in place actually costs. */
    @Test
    fun schedCOpen() = shot {
        val c = Obsidian.colors
        Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
            Box(Modifier.weight(1f)) {
                Fade(200)
                Column(Modifier.fillMaxSize()) {
                    // Scrolled down, so the open section is what you are looking at.
                    Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 22.dp, bottom = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = c.textMuted,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(14.dp))
                            Text("bedtime · 22:00 → 07:00", style = MaterialTheme.typography.labelLarge, color = c.textFaint)
                        }
                    }
                    Column(Modifier.padding(horizontal = 22.dp, vertical = 8.dp)) {
                        FoldHeading("What it does", "14 apps blocked · minimal home · greyscale · silenced", open = false)
                        FoldHeading("How hard to leave", "Wait 2 min, then a passage · a 5-minute break", open = true)
                        Column(Modifier.padding(start = 4.dp, bottom = 8.dp)) {
                            Label("the steps", "They run in this order.")
                            Row_(BedtimeIcons.Hourglass, "Wait it out", "Start a timer, come back when it's done") {
                                ObsidianToggle(true, {})
                            }
                            Sub("Timer") {
                                NumberStepper(120, {}, 5..3600, step = 5, presets = listOf(10, 30, 60, 120), title = "timer", format = { "${it / 60} min" })
                            }
                            Row_(BedtimeIcons.Book, "Type a passage", "Copy a few lines out, one letter at a time") {
                                ObsidianToggle(true, {})
                            }
                            Row_(Icons.Default.Lock, "Password", "Something long you'd rather not type", last = true) {
                                ObsidianToggle(false, {})
                            }
                            Label("what unlocking gets you")
                            Box(Modifier.padding(top = 6.dp, bottom = 8.dp)) {
                                SegmentedChoice(listOf("End for today", "Take a break"), selected = 1, onSelect = {})
                            }
                        }
                        HorizontalDivider(color = c.border.copy(alpha = 0.5f))
                        FoldHeading("The doors left open", "Emergency break 2 min · changeable 08:00–20:00", open = false)
                    }
                }
            }
            BottomBar("Save changes")
        }
    }

    // ---------------------------------------------------------------- the ways around it

    /**
     * The third section once the two app-wide strictness switches become per-schedule. "The doors
     * left open" no longer fits, because two of these four shut a door rather than leave one open —
     * so it takes the walk-through's own words: every way around a session has a switch of its own.
     */
    @Test
    fun schedWaysAround() = shot {
        val c = Obsidian.colors
        Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
            Box(Modifier.weight(1f)) {
                Fade(200)
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 22.dp, bottom = 4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null,
                                tint = c.textMuted,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(14.dp))
                            Text("bedtime · 22:00 → 07:00", style = MaterialTheme.typography.labelLarge, color = c.textFaint)
                        }
                    }
                    Column(Modifier.padding(horizontal = 22.dp, vertical = 8.dp)) {
                        FoldHeading("What it does", "14 apps blocked · minimal home · greyscale · silenced", open = false)
                        FoldHeading("How hard to leave", "Wait 2 min, then a passage · a 5-minute break", open = false)
                        FoldHeading(
                            "The ways around it",
                            "Emergency break 2 min · settings locked · reminder on · changeable 08:00–20:00",
                            open = true,
                        )
                        Column(Modifier.padding(start = 4.dp, bottom = 8.dp, top = 4.dp)) {
                            Row_(BedtimeIcons.Refuge, "Emergency break", "A short step out. It counts as an escape.") {
                                ObsidianToggle(true, {})
                            }
                            Sub("How long you can step out") {
                                NumberStepper(2, {}, 1..60, suffix = " min", presets = listOf(1, 2, 3, 5), title = "step out")
                            }
                            Row_(
                                Icons.Default.Lock,
                                "Lock changes during sessions",
                                "Covers the Settings screens that switch blocking off or uninstall the app",
                            ) { ObsidianToggle(true, {}) }
                            Row_(
                                BedtimeIcons.Contrast,
                                "Full-screen reminder",
                                "If blocking is switched off while this runs, take over the screen until it's back on",
                            ) { ObsidianToggle(true, {}) }
                            Row_(
                                Icons.Default.Lock,
                                "Changeable only at certain hours",
                                "Outside them this page is read-only",
                                last = true,
                            ) { ObsidianToggle(true, {}) }
                            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                TimeTile("From", "08:00", Modifier.weight(1f)) {}
                                TimeTile("Until", "20:00", Modifier.weight(1f)) {}
                            }
                        }
                    }
                }
            }
            BottomBar("Save changes")
        }
    }

    private companion object {
        val Deep = Color(0xFF1F2C25)
    }
}
