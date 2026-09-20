package app.bedtime

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.style.TextAlign
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
 * Round two of throwaway home-screen ideas, deliberately more radical. Nothing here ships; it exists
 * only to be looked at and chosen from, then deleted.
 */
class HomeRadicalDesignsTest {
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
    fun option5TabsBlocks() = shot { TabsShell(selected = 0) }

    @Test
    fun option5bTabsToday() = shot { TabsShell(selected = 2) }

    @Test
    fun option6OneButton() = shot { OneButton() }

    @Test
    fun option7Week() = shot { WeekDashboard() }

    @Test
    fun option8Deck() = shot { CardDeck() }

    @Test
    fun option9Refuge() = shot { ZenRefuge() }
}

private data class RBlock(val name: String, val length: String, val running: Boolean = false)
private data class RSched(val name: String, val time: String, val days: String, val on: Boolean)

private val rBlocks = listOf(
    RBlock("focus", "1 h", running = true),
    RBlock("deep work", "1 h 30 min"),
    RBlock("pomodoro", "25 min"),
    RBlock("reading", "30 min"),
)
private val rScheds = listOf(
    RSched("bedtime", "10:30 pm – 7:00 am", "every day", true),
    RSched("work focus", "9:00 am – 12:00 pm", "weekdays", false),
    RSched("weekend detox", "10:00 am – 6:00 pm", "sat, sun", true),
)

// ---------------------------------------------------------------- 5. bottom tabs

@Composable
private fun TabsShell(selected: Int) {
    val c = Obsidian.colors
    Scaffold(
        containerColor = c.bgPrimary,
        bottomBar = { BottomTabs(selected) },
        floatingActionButton = { if (selected < 2) SmallFab() },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (selected) {
                0 -> BlocksTab()
                2 -> TodayTab()
            }
        }
    }
}

@Composable
private fun SmallFab() {
    val c = Obsidian.colors
    ExtendedFloatingActionButton(
        text = { Text("new", fontWeight = FontWeight.SemiBold) },
        icon = { Icon(Icons.Default.Add, contentDescription = null) },
        onClick = {},
        shape = RoundedCornerShape(16.dp),
        containerColor = c.accentFill,
        contentColor = c.textOnAccent,
    )
}

@Composable
private fun BottomTabs(selected: Int) {
    val c = Obsidian.colors
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
        Row(
            Modifier.fillMaxWidth().background(c.bgSecondary).padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            TabItem(BedtimeIcons.Target, "blocks", selected == 0)
            TabItem(Icons.Default.DateRange, "schedules", selected == 1)
            TabItem(Icons.Default.List, "today", selected == 2)
            TabItem(Icons.Default.Settings, "settings", selected == 3)
        }
    }
}

@Composable
private fun TabItem(icon: ImageVector, label: String, on: Boolean) {
    val c = Obsidian.colors
    val tint = if (on) c.accent else c.textFaint
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.width(44.dp).height(26.dp).clip(RoundedCornerShape(50))
                .background(if (on) c.accent.copy(alpha = 0.16f) else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp)) }
        Spacer(Modifier.height(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

@Composable
private fun BlocksTab() {
    val c = Obsidian.colors
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Text("blocks", style = MaterialTheme.typography.headlineSmall, color = c.textNormal) }
        item {
            val shape = RoundedCornerShape(20.dp)
            Row(
                Modifier.fillMaxWidth().clip(shape).background(c.accent.copy(alpha = 0.12f))
                    .border(1.dp, c.accent.copy(alpha = 0.4f), shape).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(58.dp)) {
                        val s = 6.dp.toPx()
                        drawArc(Color(0x332EA873), -90f, 360f, false, style = Stroke(s, cap = StrokeCap.Round))
                        drawArc(Color(0xFF2EA873), -90f, 360f * 0.62f, false, style = Stroke(s, cap = StrokeCap.Round))
                    }
                    Text("40", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = c.textNormal)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("focus is on", style = MaterialTheme.typography.titleMedium, color = c.textNormal)
                    Text("until 3:50 pm", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                    Spacer(Modifier.height(6.dp))
                    Text("need to leave early?", style = MaterialTheme.typography.labelMedium, color = c.accentText)
                }
            }
        }
        items(rBlocks.drop(1).chunked(2).size) { i ->
            val row = rBlocks.drop(1).chunked(2)[i]
            Row(Modifier.fillMaxWidth().height(92.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { b ->
                    val shape = RoundedCornerShape(18.dp)
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clip(shape).background(c.bgSecondary)
                            .border(1.dp, c.border, shape).padding(14.dp),
                    ) {
                        Box(
                            Modifier.size(28.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.16f)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = c.accent, modifier = Modifier.size(15.dp)) }
                        Spacer(Modifier.weight(1f))
                        Text(b.name, style = MaterialTheme.typography.titleSmall, color = c.textNormal, maxLines = 1)
                        Text(b.length, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                    }
                }
                repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun TodayTab() {
    val c = Obsidian.colors
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item { Text("today", style = MaterialTheme.typography.headlineSmall, color = c.textNormal) }
        item {
            Text(
                "streak 5  ·  45 h this week  ·  1 escape",
                style = MaterialTheme.typography.labelLarge,
                color = c.textFaint,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        // A readable agenda instead of a to-scale rail: short blocks stay legible.
        item { AgendaRow("9:00 am", "work focus", "3 h · finished", past = true, running = false) }
        item { AgendaRow("3:10 pm", "focus", "40 min left", past = false, running = true) }
        item { AgendaRow("10:30 pm", "bedtime", "8 h 30 min · every day", past = false, running = false) }
        item { AgendaRow("tomorrow", "weekend detox", "10:00 am – 6:00 pm", past = false, running = false) }
    }
}

@Composable
private fun AgendaRow(time: String, name: String, detail: String, past: Boolean, running: Boolean) {
    val c = Obsidian.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape)
            .background(if (running) c.accent.copy(alpha = 0.10f) else c.bgSecondary)
            .border(1.dp, if (running) c.accent.copy(alpha = 0.45f) else Color.Transparent, shape)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            time,
            style = MaterialTheme.typography.labelLarge,
            color = if (running) c.accentText else c.textFaint,
            modifier = Modifier.width(74.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.titleMedium,
                color = if (past) c.textMuted else c.textNormal,
            )
            Text(detail, style = MaterialTheme.typography.bodySmall, color = c.textFaint)
        }
        if (running) Tag("on now")
    }
}

// ---------------------------------------------------------------- 6. one big button

/** Radically reduced: the screen is one thing to press. Everything else is a whisper. */
@Composable
private fun OneButton() {
    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("digital refuge", style = MaterialTheme.typography.labelLarge, color = c.textFaint, modifier = Modifier.weight(1f))
                IconButton(onClick = {}) { Icon(Icons.Default.Settings, contentDescription = null, tint = c.textFaint) }
            }
            Spacer(Modifier.weight(1f))

            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(260.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.06f)))
                Box(Modifier.size(210.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.10f)))
                Box(
                    Modifier.size(162.dp).clip(CircleShape).background(c.accentFill),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(BedtimeIcons.Refuge, contentDescription = null, tint = c.textOnAccent, modifier = Modifier.size(30.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("start focus", fontSize = 19.sp, fontWeight = FontWeight.SemiBold, color = c.textOnAccent)
                        Text("1 hour", style = MaterialTheme.typography.bodySmall, color = c.textOnAccent.copy(alpha = 0.75f))
                    }
                }
            }

            Spacer(Modifier.height(30.dp))
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rBlocks.drop(1).forEach { b ->
                    val shape = RoundedCornerShape(50)
                    Row(
                        Modifier.clip(shape).border(1.dp, c.border, shape).padding(horizontal = 16.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(b.name, style = MaterialTheme.typography.labelLarge, color = c.textMuted)
                        Spacer(Modifier.width(6.dp))
                        Text(b.length, style = MaterialTheme.typography.labelSmall, color = c.textFaint)
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
            Row(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("bedtime", style = MaterialTheme.typography.titleSmall, color = c.textNormal)
                    Text("starts at 10:30 pm · every day", style = MaterialTheme.typography.bodySmall, color = c.textFaint)
                }
                Text("2 more", style = MaterialTheme.typography.labelLarge, color = c.accentText)
            }
        }
    }
}

// ---------------------------------------------------------------- 7. the week

/** Data-forward: the week at a glance, so refuge becomes something you can see accumulating. */
@Composable
private fun WeekDashboard() {
    val c = Obsidian.colors
    val days = listOf("m" to 0.55f, "t" to 0.40f, "w" to 0.70f, "t" to 0.35f, "f" to 0.62f, "s" to 0.18f, "s" to 0.0f)
    Scaffold(containerColor = c.bgPrimary, floatingActionButton = { SmallFab() }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("this week", style = MaterialTheme.typography.headlineSmall, color = c.textNormal, modifier = Modifier.weight(1f))
                    IconButton(onClick = {}) { Icon(Icons.Default.Settings, contentDescription = null, tint = c.textMuted) }
                }
            }
            item {
                val shape = RoundedCornerShape(20.dp)
                Column(Modifier.fillMaxWidth().clip(shape).background(c.bgSecondary).padding(18.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("45", fontSize = 40.sp, fontWeight = FontWeight.Bold, color = c.textNormal)
                        Spacer(Modifier.width(6.dp))
                        Text("hours of refuge", style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.padding(bottom = 7.dp))
                    }
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth().height(96.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        days.forEachIndexed { i, (label, fill) ->
                            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.weight(1f).width(22.dp).clip(RoundedCornerShape(50)).background(c.accent.copy(alpha = 0.12f))) {
                                    Box(
                                        Modifier.align(Alignment.BottomCenter).fillMaxHeight(fill).width(22.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(if (i == 4) c.accent else c.accent.copy(alpha = 0.55f)),
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Text(label, style = MaterialTheme.typography.labelSmall, color = if (i == 4) c.accentText else c.textFaint)
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Tag("streak 5")
                        Tag("1 escape")
                    }
                }
            }
            item {
                val shape = RoundedCornerShape(18.dp)
                Row(
                    Modifier.fillMaxWidth().clip(shape).background(c.accent.copy(alpha = 0.12f))
                        .border(1.dp, c.accent.copy(alpha = 0.4f), shape).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("focus is on", style = MaterialTheme.typography.titleMedium, color = c.textNormal)
                        Text("40 min to go · until 3:50 pm", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                    }
                    Text("leave early", style = MaterialTheme.typography.labelLarge, color = c.accentText)
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rBlocks.drop(1).take(3).forEach { b ->
                        val shape = RoundedCornerShape(14.dp)
                        Column(
                            Modifier.weight(1f).clip(shape).background(c.bgSecondary).padding(vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(b.name, style = MaterialTheme.typography.labelLarge, color = c.textNormal, maxLines = 1)
                            Text(b.length, style = MaterialTheme.typography.labelSmall, color = c.textFaint)
                        }
                    }
                }
            }
            items(rScheds.size) { i ->
                val s = rScheds[i]
                Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(s.name, style = MaterialTheme.typography.titleSmall, color = if (s.on) c.textNormal else c.textMuted)
                        Text("${s.time} · ${s.days}", style = MaterialTheme.typography.bodySmall, color = c.textFaint)
                    }
                    ObsidianToggle(s.on, {})
                }
            }
        }
    }
}

// ---------------------------------------------------------------- 8. the deck

/** Tactile: blocks as a deck you thumb through, one large card at a time. */
@Composable
private fun CardDeck() {
    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("digital refuge", style = MaterialTheme.typography.titleLarge, color = c.textNormal, modifier = Modifier.weight(1f))
                IconButton(onClick = {}) { Icon(Icons.Default.Settings, contentDescription = null, tint = c.textMuted) }
            }
            Text(
                "streak 5  ·  45 h this week",
                style = MaterialTheme.typography.labelLarge,
                color = c.textFaint,
                modifier = Modifier.padding(start = 20.dp, top = 2.dp),
            )
            Spacer(Modifier.height(22.dp))

            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                rBlocks.forEach { b -> DeckCard(b) }
            }

            Spacer(Modifier.height(28.dp))
            Text("schedules", style = MaterialTheme.typography.labelLarge, color = c.textMuted, modifier = Modifier.padding(start = 20.dp, bottom = 6.dp))
            rScheds.forEach { s ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(if (s.on) c.accent else c.textFaint.copy(alpha = 0.4f)))
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(s.name, style = MaterialTheme.typography.titleSmall, color = if (s.on) c.textNormal else c.textMuted)
                        Text("${s.time} · ${s.days}", style = MaterialTheme.typography.bodySmall, color = c.textFaint)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckCard(b: RBlock) {
    val c = Obsidian.colors
    val shape = RoundedCornerShape(26.dp)
    Column(
        Modifier.width(210.dp).height(250.dp).clip(shape)
            .background(
                if (b.running) {
                    Brush.verticalGradient(listOf(Color(0xFF1B5E45), Color(0xFF14392F)))
                } else {
                    Brush.verticalGradient(listOf(Color(0xFF323232), Color(0xFF2A2A2A)))
                },
            )
            .border(1.dp, if (b.running) c.accent.copy(alpha = 0.6f) else c.border, shape)
            .padding(20.dp),
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (b.running) BedtimeIcons.Target else Icons.Default.PlayArrow,
                contentDescription = null, tint = c.accent, modifier = Modifier.size(22.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Text(b.name, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = c.textNormal)
        Spacer(Modifier.height(4.dp))
        Text(
            if (b.running) "40 min left · until 3:50 pm" else b.length,
            style = MaterialTheme.typography.bodyMedium,
            color = if (b.running) c.accentText else c.textMuted,
        )
        Spacer(Modifier.height(16.dp))
        val pill = RoundedCornerShape(50)
        Box(
            Modifier.fillMaxWidth().clip(pill)
                .background(if (b.running) Color.Transparent else c.accentFill)
                .border(1.dp, if (b.running) c.accent.copy(alpha = 0.6f) else Color.Transparent, pill)
                .padding(vertical = 11.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (b.running) "leave early" else "start",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (b.running) c.accentText else c.textOnAccent,
            )
        }
    }
}

// ---------------------------------------------------------------- 9. the refuge

/** Atmosphere first: the quote comes to the home screen, and the session is stated plainly. */
@Composable
private fun ZenRefuge() {
    val c = Obsidian.colors
    Box(
        Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF1E2A24), Color(0xFF282828), Color(0xFF282828)))),
    ) {
        Column(Modifier.fillMaxSize().padding(horizontal = 28.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                IconButton(onClick = {}) { Icon(Icons.Default.Settings, contentDescription = null, tint = c.textFaint) }
            }
            Spacer(Modifier.height(20.dp))
            Icon(BedtimeIcons.Refuge, contentDescription = null, tint = c.accent.copy(alpha = 0.9f), modifier = Modifier.size(40.dp))
            Spacer(Modifier.height(26.dp))
            Text("you are in focus", fontSize = 30.sp, fontWeight = FontWeight.Light, color = c.textNormal)
            Spacer(Modifier.height(8.dp))
            Text("for another 40 minutes, until 3:50 pm", style = MaterialTheme.typography.bodyLarge, color = c.textMuted)
            Spacer(Modifier.height(16.dp))
            Box(Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(50)).background(c.accent.copy(alpha = 0.18f))) {
                Box(Modifier.fillMaxWidth(0.62f).height(2.dp).clip(RoundedCornerShape(50)).background(c.accent))
            }

            Spacer(Modifier.height(36.dp))
            Text(
                "“All that you touch you change. All that you change changes you.”",
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp, lineHeight = 26.sp),
                color = c.textMuted,
            )
            Spacer(Modifier.height(8.dp))
            Text("— Octavia E. Butler, Parable of the Sower", style = MaterialTheme.typography.labelMedium, color = c.textFaint)

            Spacer(Modifier.weight(1f))

            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rBlocks.drop(1).forEach { b ->
                    val shape = RoundedCornerShape(50)
                    Row(
                        Modifier.clip(shape).border(1.dp, c.border, shape).padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(b.name, style = MaterialTheme.typography.labelLarge, color = c.textMuted)
                        Spacer(Modifier.width(6.dp))
                        Text(b.length, style = MaterialTheme.typography.labelSmall, color = c.textFaint)
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Row(Modifier.fillMaxWidth().padding(bottom = 26.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "bedtime at 10:30 pm  ·  2 more schedules",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textFaint,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start,
                )
                Text("leave early", style = MaterialTheme.typography.labelLarge, color = c.accentText)
            }
        }
    }
}
