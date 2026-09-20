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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
 * Throwaway home-screen designs, rendered only so one can be chosen. None of this ships: once a
 * direction is picked it gets built properly in ui/home/HomeScreen.kt and this file is deleted.
 */
class HomeDesignsTest {
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
    fun option1Calm() = shot { OptionCalm() }

    @Test
    fun option2Hero() = shot { OptionHero() }

    @Test
    fun option3Timeline() = shot { OptionTimeline() }

    @Test
    fun option4Cards() = shot { OptionCards() }
}

// ---------------------------------------------------------------- shared sample data

private data class Block(val name: String, val length: String, val runningUntil: String? = null)
private data class Sched(val name: String, val time: String, val days: String, val on: Boolean, val tags: List<String>)

private val blocks = listOf(
    Block("focus", "1 h", runningUntil = "3:50 pm"),
    Block("deep work", "1 h 30 min"),
    Block("pomodoro", "25 min"),
    Block("reading", "30 min"),
)

private val scheds = listOf(
    Sched("bedtime", "10:30 pm – 7:00 am", "every day", true, listOf("minimal mode", "5 apps", "greyscale")),
    Sched("work focus", "9:00 am – 12:00 pm", "weekdays", false, listOf("2 apps")),
    Sched("weekend detox", "10:00 am – 6:00 pm", "sat, sun", true, listOf("minimal mode", "greyscale")),
)

private const val PROGRESS = 0.62f

@Composable
private fun Fab() {
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

// ---------------------------------------------------------------- 1. calm canvas

/** Type-led and airy: no cards at all, just space, weight and a hairline of progress. */
@Composable
private fun OptionCalm() {
    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary, floatingActionButton = { Fab() }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 110.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("digital refuge", style = MaterialTheme.typography.titleMedium, color = c.textFaint, modifier = Modifier.weight(1f))
                    IconButton(onClick = {}) { Icon(Icons.Default.Settings, contentDescription = null, tint = c.textFaint) }
                }
                Spacer(Modifier.height(28.dp))

                Text("focus is on", fontSize = 34.sp, fontWeight = FontWeight.SemiBold, color = c.textNormal)
                Spacer(Modifier.height(6.dp))
                Text("40 min to go · until 3:50 pm", style = MaterialTheme.typography.bodyLarge, color = c.textMuted)
                Spacer(Modifier.height(18.dp))
                Box(Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(50)).background(c.accent.copy(alpha = 0.15f))) {
                    Box(Modifier.fillMaxWidth(PROGRESS).height(3.dp).clip(RoundedCornerShape(50)).background(c.accent))
                }
                Spacer(Modifier.height(14.dp))
                Text("need to leave early?", style = MaterialTheme.typography.bodyMedium, color = c.accentText)

                Spacer(Modifier.height(34.dp))
                Text("streak 5  ·  45 h this week  ·  1 escape", style = MaterialTheme.typography.labelLarge, color = c.textFaint)
                Spacer(Modifier.height(30.dp))
            }

            items(blocks.size) { i ->
                val b = blocks[i]
                CalmRow(b.name, b.runningUntil?.let { "until $it" } ?: b.length, running = b.runningUntil != null) {
                    if (b.runningUntil == null) {
                        Text("start", style = MaterialTheme.typography.labelLarge, color = c.accentText)
                    } else {
                        Text("on", style = MaterialTheme.typography.labelLarge, color = c.accentText)
                    }
                }
            }
            item { Spacer(Modifier.height(26.dp)) }
            items(scheds.size) { i ->
                val s = scheds[i]
                CalmRow(s.name, "${s.time} · ${s.days}", running = false, dim = !s.on) {
                    Box(
                        Modifier.size(9.dp).clip(CircleShape)
                            .background(if (s.on) c.accent else c.textFaint.copy(alpha = 0.5f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun CalmRow(name: String, detail: String, running: Boolean, dim: Boolean = false, trailing: @Composable () -> Unit) {
    val c = Obsidian.colors
    Column {
        Row(Modifier.fillMaxWidth().padding(vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    fontSize = 19.sp,
                    fontWeight = if (running) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (dim) c.textMuted else c.textNormal,
                )
                Spacer(Modifier.height(2.dp))
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = c.textFaint)
            }
            trailing()
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.border.copy(alpha = 0.5f)))
    }
}

// ---------------------------------------------------------------- 2. hero + grid

/** One strong focal point — a progress ring — then blocks as generous two-up tiles. */
@Composable
private fun OptionHero() {
    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary, floatingActionButton = { Fab() }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("digital refuge", style = MaterialTheme.typography.titleLarge, color = c.textNormal, modifier = Modifier.weight(1f))
                    IconButton(onClick = {}) { Icon(Icons.Default.Settings, contentDescription = null, tint = c.textMuted) }
                }
            }
            item {
                val shape = RoundedCornerShape(24.dp)
                Column(
                    Modifier.fillMaxWidth().clip(shape).background(c.accent.copy(alpha = 0.10f))
                        .border(1.dp, c.accent.copy(alpha = 0.35f), shape).padding(22.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center) {
                            Canvas(Modifier.size(82.dp)) {
                                val stroke = 8.dp.toPx()
                                drawArc(
                                    color = Color(0x332EA873), startAngle = -90f, sweepAngle = 360f,
                                    useCenter = false, style = Stroke(stroke, cap = StrokeCap.Round),
                                )
                                drawArc(
                                    color = Color(0xFF2EA873), startAngle = -90f, sweepAngle = 360f * PROGRESS,
                                    useCenter = false, style = Stroke(stroke, cap = StrokeCap.Round),
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("40", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = c.textNormal)
                                Text("min left", style = MaterialTheme.typography.labelSmall, color = c.textMuted)
                            }
                        }
                        Spacer(Modifier.width(20.dp))
                        Column(Modifier.weight(1f)) {
                            Text("focus is on", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = c.textNormal)
                            Text("until 3:50 pm", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                            Spacer(Modifier.height(10.dp))
                            Text("need to leave early?", style = MaterialTheme.typography.labelLarge, color = c.accentText)
                        }
                    }
                }
            }
            item {
                Text(
                    "streak 5  ·  45 h this week  ·  1 escape",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.textFaint,
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
                )
            }
            item { Heading("start a block") }
            items(blocks.chunked(2).size) { rowIndex ->
                val row = blocks.chunked(2)[rowIndex]
                Row(Modifier.fillMaxWidth().height(96.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { b -> HeroTile(b, Modifier.weight(1f).fillMaxHeight()) }
                    repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            item { Heading("schedules") }
            items(scheds.size) { i ->
                val s = scheds[i]
                val shape = RoundedCornerShape(16.dp)
                Row(
                    Modifier.fillMaxWidth().clip(shape).background(c.bgSecondary).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(s.name, style = MaterialTheme.typography.titleMedium, color = c.textNormal)
                        Text("${s.time} · ${s.days}", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                    }
                    ObsidianToggle(s.on, {})
                }
            }
        }
    }
}

@Composable
private fun HeroTile(b: Block, modifier: Modifier) {
    val c = Obsidian.colors
    val on = b.runningUntil != null
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier.clip(shape)
            .background(if (on) c.accent.copy(alpha = 0.12f) else c.bgSecondary)
            .border(1.dp, if (on) c.accent.copy(alpha = 0.5f) else c.border, shape)
            .padding(14.dp),
    ) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (on) BedtimeIcons.Target else Icons.Default.PlayArrow,
                contentDescription = null, tint = c.accent, modifier = Modifier.size(16.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Text(b.name, style = MaterialTheme.typography.titleSmall, color = c.textNormal, maxLines = 1)
        Text(
            b.runningUntil?.let { "until $it" } ?: b.length,
            style = MaterialTheme.typography.bodySmall,
            color = if (on) c.accentText else c.textMuted,
        )
    }
}

@Composable
private fun Heading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = Obsidian.colors.textMuted,
        modifier = Modifier.padding(start = 4.dp, top = 10.dp),
    )
}

// ---------------------------------------------------------------- 3. today's timeline

/** The day drawn as a rail, so what's running and what's coming is one glance. */
@Composable
private fun OptionTimeline() {
    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary, floatingActionButton = { Fab() }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("today", style = MaterialTheme.typography.titleLarge, color = c.textNormal, modifier = Modifier.weight(1f))
                IconButton(onClick = {}) { Icon(Icons.Default.Settings, contentDescription = null, tint = c.textMuted) }
            }
            Text("streak 5  ·  45 h this week  ·  1 escape", style = MaterialTheme.typography.labelLarge, color = c.textFaint)
            Spacer(Modifier.height(18.dp))

            // 6:00 → 24:00 mapped onto the rail.
            val railHeight = 430.dp
            Box(Modifier.fillMaxWidth().height(railHeight)) {
                listOf(6, 9, 12, 15, 18, 21, 24).forEach { hour ->
                    val top = railHeight * ((hour - 6) / 18f)
                    Text(
                        if (hour == 24) "00" else "%02d".format(hour),
                        style = MaterialTheme.typography.labelSmall,
                        color = c.textFaint,
                        modifier = Modifier.offset(y = top - 7.dp),
                    )
                    Box(
                        Modifier.offset(y = top).padding(start = 34.dp).fillMaxWidth().height(1.dp)
                            .background(c.border.copy(alpha = 0.4f)),
                    )
                }
                TimelineBar("work focus", 9f, 12f, railHeight, running = false)
                TimelineBar("focus", 15.17f, 15.83f, railHeight, running = true)
                TimelineBar("bedtime", 22.5f, 24f, railHeight, running = false)
                // "now"
                val nowTop = railHeight * ((15.17f - 6) / 18f)
                Box(Modifier.offset(y = nowTop).padding(start = 34.dp).fillMaxWidth().height(2.dp).background(c.orange))
            }

            Spacer(Modifier.height(20.dp))
            Text("start a block", style = MaterialTheme.typography.labelLarge, color = c.textMuted, modifier = Modifier.padding(bottom = 10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                blocks.take(3).forEach { b ->
                    val shape = RoundedCornerShape(50)
                    Column(
                        Modifier.weight(1f).clip(shape)
                            .background(if (b.runningUntil != null) c.accent.copy(alpha = 0.14f) else c.bgSecondary)
                            .border(1.dp, if (b.runningUntil != null) c.accent.copy(alpha = 0.5f) else c.border, shape)
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(b.name, style = MaterialTheme.typography.labelLarge, color = c.textNormal, maxLines = 1)
                        Text(b.length, style = MaterialTheme.typography.labelSmall, color = c.textFaint)
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineBar(name: String, startHour: Float, endHour: Float, railHeight: androidx.compose.ui.unit.Dp, running: Boolean) {
    val c = Obsidian.colors
    val top = railHeight * ((startHour - 6f) / 18f)
    val height = railHeight * ((endHour - startHour) / 18f)
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier.offset(y = top).padding(start = 34.dp).fillMaxWidth().height(height).padding(vertical = 2.dp)
            .clip(shape)
            .background(if (running) c.accent.copy(alpha = 0.18f) else c.bgSecondary)
            .border(1.dp, if (running) c.accent else c.border, shape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            color = if (running) c.textNormal else c.textMuted,
            fontWeight = if (running) FontWeight.SemiBold else FontWeight.Normal,
        )
        if (running) {
            Spacer(Modifier.width(8.dp))
            Tag("on now")
        }
    }
}

// ---------------------------------------------------------------- 4. polished cards

/** Closest to today's screen, but dressed: accent spines, stat chips, round start buttons. */
@Composable
private fun OptionCards() {
    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary, floatingActionButton = { Fab() }) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("digital refuge", style = MaterialTheme.typography.titleLarge, color = c.textNormal, modifier = Modifier.weight(1f))
                    IconButton(onClick = {}) { Icon(Icons.Default.Settings, contentDescription = null, tint = c.textMuted) }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatChip("5", "streak", Modifier.weight(1f))
                    StatChip("45 h", "this week", Modifier.weight(1f))
                    StatChip("1", "escape", Modifier.weight(1f))
                }
            }
            item {
                val shape = RoundedCornerShape(20.dp)
                Column(
                    Modifier.fillMaxWidth().clip(shape).background(c.accent.copy(alpha = 0.10f))
                        .border(1.dp, c.accent.copy(alpha = 0.4f), shape).padding(18.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(BedtimeIcons.Target, contentDescription = null, tint = c.accent, modifier = Modifier.size(22.dp)) }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("focus is on", style = MaterialTheme.typography.titleLarge, color = c.textNormal)
                            Text("40 min to go · until 3:50 pm", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)).background(c.accent.copy(alpha = 0.18f))) {
                        Box(Modifier.fillMaxWidth(PROGRESS).height(6.dp).clip(RoundedCornerShape(50)).background(c.accent))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("need to leave early?", style = MaterialTheme.typography.labelLarge, color = c.accentText)
                }
            }
            item { Heading("focus blocks") }
            items(blocks.size) { i -> SpineCard(blocks[i]) }
            item { Heading("schedules") }
            items(scheds.size) { i -> SchedSpineCard(scheds[i]) }
        }
    }
}

@Composable
private fun StatChip(value: String, label: String, modifier: Modifier) {
    val c = Obsidian.colors
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(c.bgSecondary).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = c.accentText)
        Text(label, style = MaterialTheme.typography.labelSmall, color = c.textFaint)
    }
}

@Composable
private fun SpineCard(b: Block) {
    val c = Obsidian.colors
    val on = b.runningUntil != null
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().height(72.dp).clip(shape).background(c.bgSecondary),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(4.dp).fillMaxHeight().background(if (on) c.accent else c.accent.copy(alpha = 0.35f)))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(b.name, style = MaterialTheme.typography.titleMedium, color = c.textNormal)
            Text(
                b.runningUntil?.let { "on now · until $it" } ?: b.length,
                style = MaterialTheme.typography.bodySmall,
                color = if (on) c.accentText else c.textMuted,
            )
        }
        if (!on) {
            Box(
                Modifier.padding(end = 14.dp).size(40.dp).clip(CircleShape).background(c.accentFill),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.PlayArrow, contentDescription = null, tint = c.textOnAccent, modifier = Modifier.size(22.dp)) }
        } else {
            Box(Modifier.padding(end = 14.dp)) { Tag("on now") }
        }
    }
}

@Composable
private fun SchedSpineCard(s: Sched) {
    val c = Obsidian.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(c.bgSecondary),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.width(4.dp).height(88.dp)
                .background(if (s.on) c.accent else c.textFaint.copy(alpha = 0.3f)),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f).padding(vertical = 14.dp)) {
            Text(s.name, style = MaterialTheme.typography.titleMedium, color = if (s.on) c.textNormal else c.textMuted)
            Text("${s.time} · ${s.days}", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { s.tags.forEach { Tag(it) } }
        }
        Box(Modifier.padding(end = 14.dp)) { ObsidianToggle(s.on, {}) }
    }
}
