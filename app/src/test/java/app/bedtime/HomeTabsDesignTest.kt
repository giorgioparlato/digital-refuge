package app.bedtime

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.ui.components.Tag
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.BedtimeTheme
import app.bedtime.ui.theme.Obsidian
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

/**
 * The chosen direction, drawn before it is built: three tabs (home, schedules, settings) wearing the
 * calmer voice — soft gradient, light type, the quote on the home screen. Throwaway; delete after.
 */
class HomeTabsDesignTest {
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
    fun tab1HomeRunning() = shot { Shell(0) { HomeTab(running = true) } }

    @Test
    fun tab1HomeIdle() = shot { Shell(0) { HomeTab(running = false) } }

    @Test
    fun tab2Schedules() = shot { Shell(1, fab = "new schedule") { SchedulesTab() } }

    @Test
    fun tab3Settings() = shot { Shell(2) { SettingsTab() } }
}

private data class TBlock(val name: String, val length: String, val running: Boolean = false)

private val tBlocks = listOf(
    TBlock("focus", "1 h", running = true),
    TBlock("deep work", "1 h 30 min"),
    TBlock("pomodoro", "25 min"),
    TBlock("reading", "30 min"),
)

private val QUOTE = "“all that you touch you change. all that you change changes you.”"
private const val QUOTE_BY = "— octavia e. butler, parable of the sower"

// ---------------------------------------------------------------- the shell

@Composable
private fun Shell(selected: Int, fab: String? = null, content: @Composable () -> Unit) {
    val c = Obsidian.colors
    Scaffold(
        containerColor = c.bgPrimary,
        bottomBar = { Tabs(selected) },
        floatingActionButton = {
            if (fab != null) {
                ExtendedFloatingActionButton(
                    text = { Text(fab, fontWeight = FontWeight.SemiBold) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = {},
                    shape = RoundedCornerShape(16.dp),
                    containerColor = c.accentFill,
                    contentColor = c.textOnAccent,
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) { content() }
    }
}

@Composable
private fun Tabs(selected: Int) {
    val c = Obsidian.colors
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
        Row(
            Modifier.fillMaxWidth().background(c.bgSecondary).padding(top = 10.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            Tab(BedtimeIcons.Refuge, "home", selected == 0)
            Tab(Icons.Default.DateRange, "schedules", selected == 1)
            Tab(Icons.Default.Settings, "settings", selected == 2)
        }
    }
}

@Composable
private fun Tab(icon: ImageVector, label: String, on: Boolean) {
    val c = Obsidian.colors
    val tint = if (on) c.accent else c.textFaint
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.width(52.dp).height(28.dp).clip(RoundedCornerShape(50))
                .background(if (on) c.accent.copy(alpha = 0.16f) else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp)) }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

// ---------------------------------------------------------------- home

@Composable
private fun HomeTab(running: Boolean) {
    val c = Obsidian.colors
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 28.dp)) {
        item { HomeHeader(running) }
        item { Spacer(Modifier.height(20.dp)) }
        item { StatsCard() }
        item {
            Text(
                if (running) "start another" else "start a block",
                style = MaterialTheme.typography.labelLarge,
                color = c.textMuted,
                modifier = Modifier.padding(start = 24.dp, top = 26.dp, bottom = 12.dp),
            )
        }
        val rows = (if (running) tBlocks.drop(1) else tBlocks).chunked(2)
        items(rows.size) { i ->
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(88.dp).padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                rows[i].forEach { b -> BlockTile(b, Modifier.weight(1f).fillMaxHeight()) }
                repeat(2 - rows[i].size) { Spacer(Modifier.weight(1f)) }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 24.dp).padding(top = 26.dp)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.border.copy(alpha = 0.6f)))
                Spacer(Modifier.height(22.dp))
                Text(
                    QUOTE,
                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp, lineHeight = 25.sp),
                    color = c.textMuted,
                )
                Spacer(Modifier.height(8.dp))
                Text(QUOTE_BY, style = MaterialTheme.typography.labelMedium, color = c.textFaint)
            }
        }
    }
}

@Composable
private fun HomeHeader(running: Boolean) {
    val c = Obsidian.colors
    Box(
        Modifier.fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF1F2C25), c.bgPrimary)))
            .padding(horizontal = 24.dp).padding(top = 30.dp, bottom = 28.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(BedtimeIcons.Refuge, contentDescription = null, tint = c.accent, modifier = Modifier.size(30.dp))
                Spacer(Modifier.width(10.dp))
                Text("digital refuge", style = MaterialTheme.typography.labelLarge, color = c.textFaint)
            }
            Spacer(Modifier.height(26.dp))
            if (running) {
                Text("you are in focus", fontSize = 29.sp, fontWeight = FontWeight.Light, color = c.textNormal)
                Spacer(Modifier.height(8.dp))
                Text("for another 40 minutes, until 3:50 pm", style = MaterialTheme.typography.bodyLarge, color = c.textMuted)
                Spacer(Modifier.height(18.dp))
                Box(Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(50)).background(c.accent.copy(alpha = 0.18f))) {
                    Box(Modifier.fillMaxWidth(0.62f).height(2.dp).clip(RoundedCornerShape(50)).background(c.accent))
                }
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("leave early", style = MaterialTheme.typography.labelLarge, color = c.accentText)
                    Spacer(Modifier.width(14.dp))
                    Tag("3 apps blocked")
                    Spacer(Modifier.width(6.dp))
                    Tag("greyscale")
                }
            } else {
                Text("all clear for now", fontSize = 29.sp, fontWeight = FontWeight.Light, color = c.textNormal)
                Spacer(Modifier.height(8.dp))
                Text("bedtime begins at 10:30 pm, in 7 hours", style = MaterialTheme.typography.bodyLarge, color = c.textMuted)
            }
        }
    }
}

@Composable
private fun StatsCard() {
    val c = Obsidian.colors
    Row(
        Modifier.padding(horizontal = 20.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp))
            .background(c.bgSecondary).padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Stat("5", "day streak", Modifier.weight(1f))
        Box(Modifier.width(1.dp).height(30.dp).background(c.border))
        Stat("45 h", "this week", Modifier.weight(1f))
        Box(Modifier.width(1.dp).height(30.dp).background(c.border))
        Stat("1", "escape", Modifier.weight(1f))
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    val c = Obsidian.colors
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = c.textNormal)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.textFaint)
    }
}

@Composable
private fun BlockTile(b: TBlock, modifier: Modifier) {
    val c = Obsidian.colors
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier.clip(shape)
            .background(if (b.running) c.accent.copy(alpha = 0.12f) else c.bgSecondary)
            .border(1.dp, if (b.running) c.accent.copy(alpha = 0.5f) else c.border, shape)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(b.name, style = MaterialTheme.typography.titleSmall, color = c.textNormal, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(b.length, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = c.accent, modifier = Modifier.size(18.dp)) }
    }
}

// ---------------------------------------------------------------- schedules

@Composable
private fun SchedulesTab() {
    val c = Obsidian.colors
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 30.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column {
                Text("schedules", fontSize = 27.sp, fontWeight = FontWeight.Light, color = c.textNormal)
                Spacer(Modifier.height(6.dp))
                Text("hours that look after themselves", style = MaterialTheme.typography.bodyMedium, color = c.textFaint)
                Spacer(Modifier.height(14.dp))
            }
        }
        item {
            ScheduleCard("bedtime", "10:30 pm – 7:00 am", "every day", true, listOf("minimal mode", "5 apps", "greyscale"), next = "starts in 7 hours")
        }
        item { ScheduleCard("work focus", "9:00 am – 12:00 pm", "weekdays", false, listOf("2 apps"), next = null) }
        item {
            ScheduleCard("weekend detox", "10:00 am – 6:00 pm", "sat, sun", true, listOf("minimal mode", "greyscale"), next = "starts tomorrow")
        }
    }
}

@Composable
private fun ScheduleCard(name: String, time: String, days: String, on: Boolean, tags: List<String>, next: String?) {
    val c = Obsidian.colors
    val shape = RoundedCornerShape(18.dp)
    Column(
        Modifier.fillMaxWidth().clip(shape).background(c.bgSecondary).padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleMedium, color = if (on) c.textNormal else c.textMuted)
                Spacer(Modifier.height(3.dp))
                Text("$time · $days", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
            }
            ObsidianToggle(on, {})
        }
        if (tags.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { tags.forEach { Tag(it) } }
        }
        if (next != null) {
            Spacer(Modifier.height(10.dp))
            Text(next, style = MaterialTheme.typography.labelMedium, color = c.accentText)
        }
    }
}

// ---------------------------------------------------------------- settings

@Composable
private fun SettingsTab() {
    val c = Obsidian.colors
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 30.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Column {
                Text("settings", fontSize = 27.sp, fontWeight = FontWeight.Light, color = c.textNormal)
                Spacer(Modifier.height(6.dp))
                Text("version 0.6.3 · everything stays on this phone", style = MaterialTheme.typography.bodySmall, color = c.textFaint)
                Spacer(Modifier.height(10.dp))
            }
        }
        item {
            Card {
                SettingRow(Icons.Default.Build, "permissions & setup", "1 step left")
                SettingRow(BedtimeIcons.Moon, "minimal home screen", "colours, text size and what's shown")
                SettingRow(BedtimeIcons.Grid, "app groups", "2 groups")
            }
        }
        item {
            Card {
                SettingRow(Icons.Default.Warning, "always-available apps", "2 apps, never blocked")
            }
        }
        item {
            Card {
                SettingRow(BedtimeIcons.Refuge, "how it works", "what a session locks, and your ways out")
                SettingRow(BedtimeIcons.Target, "staying blocked", "lock changes · full-screen reminder")
                SettingRow(Icons.Default.DateRange, "backup", "save or restore your settings")
            }
        }
    }
}

@Composable
private fun Card(content: @Composable () -> Unit) {
    val c = Obsidian.colors
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.bgSecondary).padding(vertical = 6.dp)) {
        content()
    }
}

@Composable
private fun SettingRow(icon: ImageVector, title: String, subtitle: String) {
    val c = Obsidian.colors
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(c.accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(19.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = c.textNormal)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
        Text("›", style = MaterialTheme.typography.titleLarge, color = c.textFaint)
    }
}
