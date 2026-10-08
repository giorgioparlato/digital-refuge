package app.bedtime

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.IconBadge
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.ui.components.OptionRow
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.BedtimeTheme
import app.bedtime.ui.theme.Obsidian
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

/**
 * Three ways to shorten the settings page, all wearing option C's clothes so only the structure
 * differs: S1 regroups and moves the once-ever things behind two rows, S2 folds every group behind
 * its heading, S3 keeps what you actually revisit in the open and folds the rest.
 *
 * Throwaway. Delete once the real page is built.
 */
class SettingsStructureTest {
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

    /** Shorter than the first drafts' 420dp, which tinted the whole page green. */
    @Composable
    private fun Fade() {
        Box(
            Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(Brush.verticalGradient(listOf(Deep, Obsidian.colors.bgPrimary))),
        )
    }

    @Composable
    private fun Header(title: String = "settings", subtitle: String = "version 0.7.7.12 · all on this phone", back: Boolean = false) {
        val c = Obsidian.colors
        Row(
            Modifier.padding(horizontal = 22.dp).padding(top = 28.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconBadge(BedtimeIcons.Refuge, size = if (back) 0.dp else 46.dp)
            Spacer(Modifier.width(if (back) 0.dp else 14.dp))
            Column {
                Text(title, fontSize = 26.sp, fontWeight = FontWeight.Light, color = c.textNormal)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            }
        }
    }

    @Composable
    private fun Label(text: String, note: String? = null, top: Int = 22) {
        val c = Obsidian.colors
        Column(Modifier.padding(start = 2.dp, top = top.dp, bottom = 2.dp)) {
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

    /** A heading you can open: its name, what's inside in a word, and an arrow that turns. */
    @Composable
    private fun FoldHeading(title: String, summary: String, open: Boolean) {
        val c = Obsidian.colors
        Column {
            Row(
                Modifier.fillMaxWidth().heightIn(min = 54.dp).padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
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

    // ---------------------------------------------------------------- S1: regrouped, two sub-pages

    /**
     * Eight groups become four. The widget list and the tile list were the same feature twice over
     * and both grew with every block, so they fold into one "shortcuts" row; backup joins them as
     * housekeeping. The one-row "emergency" group joins the enforcement group, where it belongs.
     */
    @Test
    fun s1Regrouped() = shot {
        val c = Obsidian.colors
        Box(Modifier.fillMaxSize().background(c.bgPrimary)) {
            Fade()
            Column(Modifier.fillMaxSize()) {
                Header()
                Column(Modifier.padding(horizontal = 22.dp)) {
                    Label("getting your bearings")
                    Row_(Icons.Default.Info, "How it works", "What a session locks, and your ways out")
                    Row_(BedtimeIcons.Leaf, "The intro again", "The walk-through from the first launch", last = true)

                    Label("your phone")
                    Row_(Icons.Default.Build, "Permissions & setup", "All set")
                    Row_(Icons.Default.Home, "Minimal home screen", "Colours, text size and what's shown")
                    Row_(BedtimeIcons.Grid, "App groups", "2 groups", last = true)

                    Label("while a session runs")
                    Row_(Icons.Default.Warning, "Always-available apps", "3 apps, never blocked")
                    Row_(BedtimeIcons.Refuge, "Lock changes during sessions", "On") { ObsidianToggle(true, {}) }
                    Row_(BedtimeIcons.Contrast, "Full-screen reminder", "On", last = true) { ObsidianToggle(true, {}) }

                    Label("this app")
                    Row_(BedtimeIcons.Grid, "Shortcuts", "Widget and quick-settings tile")
                    Row_(Icons.Default.Share, "Backup", "Export or restore from a file", last = true)
                }
            }
        }
    }

    /** What sits behind the "shortcuts" row: the two lists that used to be on the main page. */
    @Test
    fun s1Shortcuts() = shot {
        val c = Obsidian.colors
        Box(Modifier.fillMaxSize().background(c.bgPrimary)) {
            Fade()
            Column(Modifier.fillMaxSize()) {
                Header("shortcuts", "two ways to start a block without opening the app", back = true)
                Column(Modifier.padding(horizontal = 22.dp)) {
                    Label("home-screen widget", "A one-tap button on your home screen. Add one for:")
                    BlockLine("Deep work · 45 min")
                    BlockLine("Reading · 30 min", last = true)

                    Label("quick settings tile", "Edit your quick settings and add the “refuge” tile. Tapping it starts:")
                    TileChoice("Deep work · 45 min", selected = true)
                    TileChoice("Reading · 30 min", selected = false, last = true)
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

    // ---------------------------------------------------------------- S2: everything folds

    /** Every group behind its own heading, all shut: the whole page is six lines you can read at once. */
    @Test
    fun s2Folded() = shot {
        val c = Obsidian.colors
        Box(Modifier.fillMaxSize().background(c.bgPrimary)) {
            Fade()
            Column(Modifier.fillMaxSize()) {
                Header()
                Column(Modifier.padding(horizontal = 22.dp, vertical = 10.dp)) {
                    FoldHeading("Getting your bearings", "How it works · the intro again", open = false)
                    FoldHeading("Your phone", "Permissions · minimal home · app groups", open = false)
                    FoldHeading("Emergency", "3 always-available apps", open = false)
                    FoldHeading("Staying blocked", "Changes locked · full-screen reminder on", open = false)
                    FoldHeading("Backup", "Export or restore from a file", open = false)
                    FoldHeading("Shortcuts", "Widget and quick-settings tile", open = false)
                }
            }
        }
    }

    /** The same page with one heading open, to see what the fold costs in height. */
    @Test
    fun s2Expanded() = shot {
        val c = Obsidian.colors
        Box(Modifier.fillMaxSize().background(c.bgPrimary)) {
            Fade()
            Column(Modifier.fillMaxSize()) {
                Header()
                Column(Modifier.padding(horizontal = 22.dp, vertical = 10.dp)) {
                    FoldHeading("Getting your bearings", "How it works · the intro again", open = false)
                    FoldHeading("Your phone", "Permissions · minimal home · app groups", open = true)
                    Column(Modifier.padding(start = 4.dp, bottom = 6.dp)) {
                        Row_(Icons.Default.Build, "Permissions & setup", "All set")
                        Row_(Icons.Default.Home, "Minimal home screen", "Colours, text size and what's shown")
                        Row_(BedtimeIcons.Grid, "App groups", "2 groups", last = true)
                    }
                    HorizontalDivider(color = c.border.copy(alpha = 0.5f))
                    FoldHeading("Emergency", "3 always-available apps", open = false)
                    FoldHeading("Staying blocked", "Changes locked · full-screen reminder on", open = false)
                    FoldHeading("Backup", "Export or restore from a file", open = false)
                    FoldHeading("Shortcuts", "Widget and quick-settings tile", open = false)
                }
            }
        }
    }

    // ---------------------------------------------------------------- S3: open where it matters

    /**
     * The middle road. What you come back to stays in the open; the once-ever things go behind one
     * fold at the foot, so nothing you actually use costs a tap.
     */
    @Test
    fun s3Hybrid() = shot {
        val c = Obsidian.colors
        Box(Modifier.fillMaxSize().background(c.bgPrimary)) {
            Fade()
            Column(Modifier.fillMaxSize()) {
                Header()
                Column(Modifier.padding(horizontal = 22.dp)) {
                    Label("your phone")
                    Row_(Icons.Default.Build, "Permissions & setup", "All set")
                    Row_(Icons.Default.Home, "Minimal home screen", "Colours, text size and what's shown")
                    Row_(BedtimeIcons.Grid, "App groups", "2 groups", last = true)

                    Label("while a session runs")
                    Row_(Icons.Default.Warning, "Always-available apps", "3 apps, never blocked")
                    Row_(BedtimeIcons.Refuge, "Lock changes during sessions", "On") { ObsidianToggle(true, {}) }
                    Row_(BedtimeIcons.Contrast, "Full-screen reminder", "On", last = true) { ObsidianToggle(true, {}) }

                    Spacer(Modifier.height(26.dp))
                    FoldHeading("Set up once", "How it works · the intro · shortcuts · backup", open = false)
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "digital refuge 0.7.7.12 · everything stays on this phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textFaint,
                        modifier = Modifier.fillMaxWidth().padding(start = 2.dp),
                    )
                }
            }
        }
    }

    // ---------------------------------------------------------------- S4: the hybrid, re-cut

    /**
     * S3's shape with honest groups. "Your phone" was a name doing no work — app groups has nothing
     * to do with the phone — so the two surviving groups are cut by what they shape: what a session
     * is like, and how hard it is to get around one.
     *
     * Permissions leaves the groups altogether. It is not a preference but a health check, the one
     * row that can be wrong and nag, so it sits on its own above everything.
     */
    @Test
    fun s4Recut() = shot {
        val c = Obsidian.colors
        Box(Modifier.fillMaxSize().background(c.bgPrimary)) {
            Fade()
            Column(Modifier.fillMaxSize()) {
                Header()
                Column(Modifier.padding(horizontal = 22.dp)) {
                    Spacer(Modifier.height(14.dp))
                    Row_(Icons.Default.Build, "Permissions & setup", "All set", last = true)

                    Label("what a session is like")
                    Row_(Icons.Default.Home, "Minimal home screen", "Colours, text size and what's shown")
                    Row_(BedtimeIcons.Grid, "App groups", "Tick many apps at once when choosing")
                    Row_(Icons.Default.Warning, "Always-available apps", "3 apps, never blocked", last = true)

                    Label("how strict")
                    Row_(BedtimeIcons.Refuge, "Lock changes during sessions", "On") { ObsidianToggle(true, {}) }
                    Row_(BedtimeIcons.Contrast, "Full-screen reminder", "On", last = true) { ObsidianToggle(true, {}) }

                    Spacer(Modifier.height(26.dp))
                    FoldHeading("Once and done", "How it works · the intro · shortcuts · backup", open = false)
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "digital refuge 0.7.7.12 · everything stays on this phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textFaint,
                        modifier = Modifier.fillMaxWidth().padding(start = 2.dp),
                    )
                }
            }
        }
    }

    // ---------------------------------------------------------------- S5: the chosen one

    /**
     * S4 with app groups lifted out of the session group — it is a tool for building a schedule, not
     * something a session does — so the top of the page is now the two rows that are about the app
     * itself rather than about sessions. The fold becomes "other", and the two strictness rows get
     * their explanations back: a toggle says whether it is on, not what it does.
     */
    @Test
    fun s5Chosen() = shot {
        val c = Obsidian.colors
        Box(Modifier.fillMaxSize().background(c.bgPrimary)) {
            Fade()
            Column(Modifier.fillMaxSize()) {
                Header()
                Column(Modifier.padding(horizontal = 22.dp)) {
                    Spacer(Modifier.height(14.dp))
                    Row_(Icons.Default.Build, "Permissions & setup", "All set")
                    Row_(BedtimeIcons.Grid, "App groups", "Tick many apps at once when choosing", last = true)

                    Label("what a session is like")
                    Row_(Icons.Default.Home, "Minimal home screen", "Colours, text size and what's shown")
                    Row_(Icons.Default.Warning, "Always-available apps", "3 apps, never blocked", last = true)

                    Label("how strict")
                    Row_(
                        BedtimeIcons.Refuge,
                        "Lock changes during sessions",
                        "Covers the Settings screens that switch blocking off or uninstall the app",
                    ) { ObsidianToggle(true, {}) }
                    Row_(
                        BedtimeIcons.Contrast,
                        "Full-screen reminder",
                        "If blocking is switched off mid-session, take over the screen until it's back on",
                        last = true,
                    ) { ObsidianToggle(true, {}) }

                    Spacer(Modifier.height(26.dp))
                    FoldHeading("Other", "How it works · the intro · shortcuts · backup", open = false)
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "digital refuge 0.7.7.12 · everything stays on this phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textFaint,
                        modifier = Modifier.fillMaxWidth().padding(start = 2.dp),
                    )
                }
            }
        }
    }

    // ---------------------------------------------------------------- S6: strictness moves out

    /**
     * What is left once "lock changes during sessions" and "full-screen reminder" become a property
     * of each schedule rather than of the app. The "how strict" group goes with them, and settings
     * keeps only what is genuinely one setting for the whole phone.
     */
    @Test
    fun s6StrictnessMovedOut() = shot {
        val c = Obsidian.colors
        Box(Modifier.fillMaxSize().background(c.bgPrimary)) {
            Fade()
            Column(Modifier.fillMaxSize()) {
                Header()
                Column(Modifier.padding(horizontal = 22.dp)) {
                    Spacer(Modifier.height(14.dp))
                    Row_(Icons.Default.Build, "Permissions & setup", "All set")
                    Row_(BedtimeIcons.Grid, "App groups", "Tick many apps at once when choosing", last = true)

                    Label("what a session is like")
                    Row_(Icons.Default.Home, "Minimal home screen", "Colours, text size and what's shown")
                    Row_(Icons.Default.Warning, "Always-available apps", "3 apps, never blocked, whichever session is on", last = true)

                    Spacer(Modifier.height(26.dp))
                    FoldHeading("Other", "How it works · the intro · shortcuts · backup", open = false)
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "digital refuge 0.7.7.12 · everything stays on this phone",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textFaint,
                        modifier = Modifier.fillMaxWidth().padding(start = 2.dp),
                    )
                }
            }
        }
    }

    private companion object {
        val Deep = Color(0xFF1F2C25)
    }
}
