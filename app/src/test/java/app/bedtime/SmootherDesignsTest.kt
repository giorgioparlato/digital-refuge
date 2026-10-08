package app.bedtime

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.IconBadge
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.ui.components.OptionRow
import app.bedtime.ui.components.QuickChip
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.TimeTile
import app.bedtime.ui.theme.BedtimeTheme
import app.bedtime.ui.theme.Obsidian
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

/**
 * Throwaway drafts for the settings page, the schedule page and the wait page, to be looked at and
 * chosen from. Three of each: A keeps today's cards and only changes how the page opens, B softens
 * the cards into groups, C goes furthest and drops the boxes altogether.
 *
 * Nothing here is wired to anything. Delete once the choices are built.
 */
class SmootherDesignsTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.NoActionBar",
        showSystemUi = false,
    )

    /** Full-bleed: each draft paints its own background, so a fade can reach the edges. */
    private fun shot(content: @Composable () -> Unit) {
        paparazzi.snapshot("dark") { BedtimeTheme(darkTheme = true) { content() } }
    }

    // ---------------------------------------------------------------- shared pieces

    /** The home screen's fade. */
    @Composable
    private fun fade() = Brush.verticalGradient(listOf(Deep, Obsidian.colors.bgPrimary))

    /**
     * The way the home screen opens, reused as a page header: the app's mark in small faint type, then
     * the page's name large and light, then one quiet line of context. No divider under it — the fade
     * is the separation.
     */
    @Composable
    private fun GradientHeader(
        eyebrow: String,
        title: String,
        subtitle: String? = null,
        back: Boolean = false,
        icon: ImageVector = BedtimeIcons.Refuge,
        action: ImageVector? = null,
        content: (@Composable () -> Unit)? = null,
    ) {
        val c = Obsidian.colors
        Box(
            Modifier
                .fillMaxWidth()
                .background(fade())
                .padding(horizontal = 24.dp)
                .padding(top = 28.dp, bottom = 24.dp),
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (back) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                            tint = c.textMuted,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(Modifier.width(14.dp))
                    } else {
                        Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(26.dp))
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(eyebrow, style = MaterialTheme.typography.labelLarge, color = c.textFaint, modifier = Modifier.weight(1f))
                    if (action != null) Icon(action, contentDescription = null, tint = c.textFaint, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.height(22.dp))
                Text(title, fontSize = 28.sp, fontWeight = FontWeight.Light, color = c.textNormal)
                if (subtitle != null) {
                    Spacer(Modifier.height(7.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                }
                if (content != null) {
                    Spacer(Modifier.height(20.dp))
                    content()
                }
            }
        }
    }

    /** B's card: the label steps outside and the border goes, so the page reads as sections, not boxes. */
    @Composable
    private fun Group(
        label: String? = null,
        note: String? = null,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        val c = Obsidian.colors
        Column(Modifier.fillMaxWidth()) {
            if (label != null) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = c.textFaint,
                    modifier = Modifier.padding(start = 6.dp, bottom = 9.dp),
                )
            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(c.bgSecondary)
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                content = content,
            )
            if (note != null) {
                Text(
                    note,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textFaint,
                    modifier = Modifier.padding(start = 6.dp, top = 9.dp),
                )
            }
        }
    }

    /** C's row: nothing behind it, hairlines between. */
    @Composable
    private fun BareRow(icon: ImageVector, title: String, description: String, last: Boolean = false, trailing: @Composable () -> Unit = { Chevron() }) {
        val c = Obsidian.colors
        Column {
            OptionRow(icon, title, description = description, trailing = trailing)
            if (!last) HorizontalDivider(Modifier.padding(start = 52.dp), color = c.border.copy(alpha = 0.5f))
        }
    }

    @Composable
    private fun BareLabel(text: String) {
        Text(
            text,
            style = MaterialTheme.typography.labelLarge,
            color = Obsidian.colors.accentText,
            modifier = Modifier.padding(start = 2.dp, top = 24.dp, bottom = 4.dp),
        )
    }

    // ---------------------------------------------------------------- settings

    /** A — today's cards, opened like the home screen. */
    @Test
    fun settingsA() = shot {
        val c = Obsidian.colors
        Column(Modifier.fillMaxSize().background(c.bgPrimary).verticalScroll(rememberScrollState())) {
            GradientHeader(
                eyebrow = "digital refuge",
                title = "settings",
                subtitle = "version 0.7.7.12 · everything stays on this phone",
            )
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionCard {
                    OptionRow(Icons.Default.Info, "How it works", description = "What a session locks, and your ways out") { Chevron() }
                    OptionRow(BedtimeIcons.Leaf, "The intro again", description = "The short walk-through from the first launch") { Chevron() }
                }
                SectionCard {
                    OptionRow(Icons.Default.Build, "Permissions & setup", description = "All set") { Chevron() }
                    OptionRow(Icons.Default.Home, "Minimal home screen", description = "Colours, text size and what's shown") { Chevron() }
                    OptionRow(BedtimeIcons.Grid, "App groups", description = "2 groups") { Chevron() }
                }
                SectionCard(
                    title = "Staying blocked",
                    subtitle = "How firmly a session holds.",
                ) {
                    OptionRow(BedtimeIcons.Refuge, "Lock changes during sessions", description = "Covers the Settings screens that switch blocking off") {
                        ObsidianToggle(true, {})
                    }
                }
            }
        }
    }

    /** B — the same page, with the cards softened into labelled groups. */
    @Test
    fun settingsB() = shot {
        val c = Obsidian.colors
        Column(Modifier.fillMaxSize().background(c.bgPrimary).verticalScroll(rememberScrollState())) {
            GradientHeader(
                eyebrow = "digital refuge",
                title = "settings",
                subtitle = "version 0.7.7.12 · everything stays on this phone",
            )
            Column(Modifier.padding(horizontal = 18.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(26.dp)) {
                Group {
                    OptionRow(Icons.Default.Info, "How it works", description = "What a session locks, and your ways out") { Chevron() }
                    OptionRow(BedtimeIcons.Leaf, "The intro again", description = "The short walk-through from the first launch") { Chevron() }
                }
                Group(label = "your phone") {
                    OptionRow(Icons.Default.Build, "Permissions & setup", description = "All set") { Chevron() }
                    OptionRow(Icons.Default.Home, "Minimal home screen", description = "Colours, text size and what's shown") { Chevron() }
                    OptionRow(BedtimeIcons.Grid, "App groups", description = "2 groups") { Chevron() }
                }
                Group(label = "staying blocked", note = "How firmly a session holds. Tap “how it works” for the full picture.") {
                    OptionRow(BedtimeIcons.Refuge, "Lock changes during sessions", description = "Covers the Settings screens that switch blocking off") {
                        ObsidianToggle(true, {})
                    }
                    OptionRow(Icons.Default.Warning, "Full-screen reminder", description = "If blocking goes off mid-session, take over the screen") {
                        ObsidianToggle(true, {})
                    }
                }
            }
        }
    }

    /** C — no boxes at all: one long fade, rows on the background, hairlines between. */
    @Test
    fun settingsC() = shot {
        val c = Obsidian.colors
        Box(Modifier.fillMaxSize().background(c.bgPrimary)) {
            // The fade is its own layer so it can run far down the page and die out slowly.
            Box(Modifier.fillMaxWidth().height(420.dp).background(fade()))
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Box(Modifier.padding(horizontal = 22.dp).padding(top = 30.dp, bottom = 10.dp)) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(BedtimeIcons.Refuge, size = 46.dp)
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text("settings", fontSize = 26.sp, fontWeight = FontWeight.Light, color = c.textNormal)
                                Text("version 0.7.7.12 · all on this phone", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                            }
                        }
                    }
                }
                Column(Modifier.padding(horizontal = 22.dp)) {
                    BareLabel("getting your bearings")
                    BareRow(Icons.Default.Info, "How it works", "What a session locks, and your ways out")
                    BareRow(BedtimeIcons.Leaf, "The intro again", "The short walk-through from the first launch", last = true)
                    BareLabel("your phone")
                    BareRow(Icons.Default.Build, "Permissions & setup", "All set")
                    BareRow(Icons.Default.Home, "Minimal home screen", "Colours, text size and what's shown")
                    BareRow(BedtimeIcons.Grid, "App groups", "2 groups", last = true)
                    BareLabel("staying blocked")
                    BareRow(BedtimeIcons.Refuge, "Lock changes during sessions", "Covers the Settings screens that switch blocking off", last = true) {
                        ObsidianToggle(true, {})
                    }
                }
            }
        }
    }

    // ---------------------------------------------------------------- schedule

    /** A — today's cards, opened like the home screen, with the schedule's own summary in the header. */
    @Test
    fun scheduleA() = shot {
        val c = Obsidian.colors
        Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                GradientHeader(
                    eyebrow = "edit schedule",
                    title = "bedtime",
                    subtitle = "every day · 22:00 – 07:00 · 9h each time",
                    back = true,
                    action = Icons.Default.Info,
                )
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SectionCard(title = "Time") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TimeTile("Starts", "22:00", Modifier.weight(1f)) {}
                            TimeTile("Ends", "07:00", Modifier.weight(1f), caption = "next day") {}
                        }
                        Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuickChip("Every day", true) {}
                            QuickChip("Weekdays", false) {}
                            QuickChip("Weekends", false) {}
                        }
                    }
                    SectionCard(title = "While it's on") {
                        OptionRow(BedtimeIcons.Moon, "Blocked apps", description = "14 apps") { Chevron() }
                        OptionRow(BedtimeIcons.Leaf, "Greyscale", description = "Drain the colour out of the screen") {
                            ObsidianToggle(true, {})
                        }
                    }
                }
            }
            BottomBar("Save changes")
        }
    }

    /** B — softened groups, and the two times lifted out of their card onto the page. */
    @Test
    fun scheduleB() = shot {
        val c = Obsidian.colors
        Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                GradientHeader(
                    eyebrow = "edit schedule",
                    title = "bedtime",
                    subtitle = "every day · 9h each time",
                    back = true,
                    action = Icons.Default.Info,
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimeTile("Starts", "22:00", Modifier.weight(1f)) {}
                        TimeTile("Ends", "07:00", Modifier.weight(1f), caption = "next day") {}
                    }
                }
                Column(Modifier.padding(horizontal = 18.dp, vertical = 20.dp), verticalArrangement = Arrangement.spacedBy(26.dp)) {
                    Group(label = "repeats on") {
                        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            QuickChip("Every day", true) {}
                            QuickChip("Weekdays", false) {}
                            QuickChip("Weekends", false) {}
                        }
                    }
                    Group(label = "while it's on") {
                        OptionRow(BedtimeIcons.Moon, "Blocked apps", description = "14 apps") { Chevron() }
                        OptionRow(BedtimeIcons.Leaf, "Greyscale", description = "Drain the colour out of the screen") {
                            ObsidianToggle(true, {})
                        }
                        OptionRow(Icons.Default.Home, "Minimal home screen", description = "6 apps allowed") { Chevron() }
                    }
                    Group(label = "leaving early", note = "The steps you pick run in this order.") {
                        OptionRow(BedtimeIcons.Refuge, "Wait a while", description = "2 minutes before anything else") {
                            ObsidianToggle(true, {})
                        }
                    }
                }
            }
            BottomBar("Save changes")
        }
    }

    /** C — the schedule's hours *are* the header: the thing you came to change, in the fade. */
    @Test
    fun scheduleC() = shot {
        val c = Obsidian.colors
        Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .background(fade())
                        .padding(horizontal = 24.dp)
                        .padding(top = 28.dp, bottom = 26.dp),
                ) {
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
                            Text(
                                "  →  ",
                                fontSize = 26.sp,
                                color = c.textFaint,
                                modifier = Modifier.padding(bottom = 4.dp),
                            )
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
                Column(Modifier.padding(horizontal = 22.dp)) {
                    BareLabel("while it's on")
                    BareRow(BedtimeIcons.Moon, "Blocked apps", "14 apps")
                    BareRow(BedtimeIcons.Leaf, "Greyscale", "Drain the colour out of the screen") { ObsidianToggle(true, {}) }
                    BareRow(Icons.Default.Home, "Minimal home screen", "6 apps allowed", last = true)
                    BareLabel("leaving early")
                    BareRow(BedtimeIcons.Refuge, "Wait a while", "2 minutes before anything else", last = true) { ObsidianToggle(true, {}) }
                }
            }
            BottomBar("Save changes")
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

    // ---------------------------------------------------------------- take a breath

    /** The page around the ring, so each ring draft is seen in place. */
    @Composable
    private fun BreathPage(background: Brush, ring: @Composable () -> Unit) {
        val c = Obsidian.colors
        Column(
            Modifier.fillMaxSize().background(background).padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Unlocking bedtime",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textFaint,
                modifier = Modifier.fillMaxWidth(),
            )
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                ring()
                Spacer(Modifier.height(26.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.height(5.dp).width(18.dp).clip(CircleShape).background(c.accent))
                    Box(Modifier.size(5.dp).clip(CircleShape).background(c.textFaint.copy(alpha = 0.35f)))
                    Spacer(Modifier.width(8.dp))
                    Text("step 1 of 2", style = MaterialTheme.typography.labelSmall, color = c.textFaint)
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(c.orange.copy(alpha = 0.14f))
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(6.dp).clip(CircleShape).background(c.orange))
                    Spacer(Modifier.width(9.dp))
                    Text("your 3rd time today", style = MaterialTheme.typography.labelMedium, color = c.orange)
                }
            }
            Text(
                "Never mind, let's keep still",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textMuted,
                modifier = Modifier.padding(8.dp),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                "leaving this page starts the wait again",
                style = MaterialTheme.typography.labelSmall,
                color = c.textFaint,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    /** A — today's page, with the ring thinned and the number lightened. A gentle trim. */
    @Test
    fun breathA() = shot {
        val c = Obsidian.colors
        BreathPage(background = Brush.verticalGradient(listOf(Deep, c.bgPrimary))) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Take a breath.", style = MaterialTheme.typography.titleLarge, color = c.textMuted)
                Spacer(Modifier.height(34.dp))
                Box(Modifier.size(248.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { 0.42f },
                        modifier = Modifier.fillMaxSize(),
                        color = c.accent,
                        strokeWidth = 5.dp,
                        trackColor = c.interactive.copy(alpha = 0.55f),
                        strokeCap = StrokeCap.Round,
                    )
                    Text(
                        "1:44",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Light,
                        color = c.textNormal,
                    )
                }
            }
        }
    }

    /** B — a halo instead of a track: the ring floats on its own light, number first, words under. */
    @Test
    fun breathB() = shot {
        val c = Obsidian.colors
        BreathPage(background = Brush.verticalGradient(listOf(Deep, c.bgPrimary))) {
            Box(Modifier.size(300.dp), contentAlignment = Alignment.Center) {
                // The glow: a wide, very faint accent bloom sitting behind the ring.
                Box(
                    Modifier
                        .size(300.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(c.accent.copy(alpha = 0.16f), Color.Transparent),
                            ),
                            CircleShape,
                        ),
                )
                CircularProgressIndicator(
                    progress = { 0.42f },
                    modifier = Modifier.size(232.dp),
                    color = c.accent,
                    strokeWidth = 3.dp,
                    trackColor = Color.Transparent,
                    strokeCap = StrokeCap.Round,
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("1:44", fontSize = 62.sp, fontWeight = FontWeight.Light, color = c.textNormal)
                    Spacer(Modifier.height(8.dp))
                    Text("take a breath", style = MaterialTheme.typography.bodyLarge, color = c.textFaint)
                }
            }
        }
    }

    /** C — the fade becomes a bloom centred on the ring, so the light comes from the timer itself. */
    @Test
    fun breathC() = shot {
        val c = Obsidian.colors
        BreathPage(background = Brush.radialGradient(listOf(Deep, c.bgPrimary))) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("take a breath", style = MaterialTheme.typography.titleLarge, color = c.textMuted)
                Spacer(Modifier.height(34.dp))
                Box(Modifier.size(264.dp), contentAlignment = Alignment.Center) {
                    // An outer hairline the ring sits inside, to give it somewhere to be.
                    Box(Modifier.size(264.dp).border(1.dp, c.accent.copy(alpha = 0.12f), CircleShape))
                    CircularProgressIndicator(
                        progress = { 0.42f },
                        modifier = Modifier.size(228.dp),
                        color = c.accent,
                        strokeWidth = 7.dp,
                        trackColor = c.accent.copy(alpha = 0.10f),
                        strokeCap = StrokeCap.Round,
                    )
                    Text("1:44", fontSize = 54.sp, fontWeight = FontWeight.Light, color = c.textNormal)
                }
            }
        }
    }

    private companion object {
        /** The deep green the home screen's fade starts from. */
        val Deep = Color(0xFF1F2C25)
    }
}
