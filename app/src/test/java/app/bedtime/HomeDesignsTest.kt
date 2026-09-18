package app.bedtime

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bedtime.data.Schedule
import app.bedtime.data.ScheduleKind
import app.bedtime.data.UnlockConfig
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.ui.components.ObsidianTopBar
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
 * direction is picked, it gets built properly in ui/home/HomeScreen.kt and this file is deleted.
 */
class HomeDesignsTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5,
        theme = "android:Theme.Material.NoActionBar",
        showSystemUi = false,
    )

    private fun shot(content: @Composable () -> Unit) {
        paparazzi.snapshot("dark") { BedtimeTheme(darkTheme = true) { content() } }
    }

    @Test
    fun optionAOneList() = shot { OptionA() }

    @Test
    fun optionBFocusFirst() = shot { OptionB() }

    @Test
    fun optionCCalmCards() = shot { OptionC() }
}

private val bedtime = Schedule(id = "b", name = "Bedtime", startMinute = 22 * 60 + 30, endMinute = 7 * 60, minimalMode = true)
private val work = Schedule(id = "w", name = "Work focus", days = (1..5).toSet(), startMinute = 9 * 60, endMinute = 12 * 60, enabled = false)
private val weekend = Schedule(id = "k", name = "Weekend detox", days = setOf(6, 7), startMinute = 10 * 60, endMinute = 18 * 60)
private val focus = Schedule(id = "f", name = "Focus", kind = ScheduleKind.BLOCK, durationMinutes = 60, unlock = UnlockConfig())
private val deep = Schedule(id = "d", name = "Deep work", kind = ScheduleKind.BLOCK, durationMinutes = 90)
private val pomodoro = Schedule(id = "p", name = "Pomodoro", kind = ScheduleKind.BLOCK, durationMinutes = 25)

@Composable
private fun Frame(stats: String? = null, content: @Composable () -> Unit) {
    val c = Obsidian.colors
    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            ObsidianTopBar("digital refuge", actions = {
                IconButton(onClick = {}) { Icon(Icons.Default.Settings, contentDescription = null) }
            })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("New", fontWeight = FontWeight.SemiBold) },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                onClick = {},
                shape = RoundedCornerShape(16.dp),
                containerColor = c.accentFill,
                contentColor = c.textOnAccent,
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            if (stats != null) {
                Text(stats, style = MaterialTheme.typography.labelLarge, color = c.textFaint, modifier = Modifier.padding(vertical = 10.dp))
            }
            content()
        }
    }
}

/** A running session, drawn the same way in every option so only the list around it differs. */
@Composable
private fun RunningStrip(compact: Boolean = false) {
    val c = Obsidian.colors
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.accent.copy(alpha = 0.10f))
            .border(1.dp, c.accent.copy(alpha = 0.45f), shape)
            .padding(if (compact) 14.dp else 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Focus is on", style = MaterialTheme.typography.titleMedium, color = c.textNormal)
                Text("38 min to go · until 16:10", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
            }
            Text("leave early", style = MaterialTheme.typography.labelLarge, color = c.accentText)
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { 0.37f },
            modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
            color = c.accent,
            trackColor = c.accent.copy(alpha = 0.18f),
            strokeCap = StrokeCap.Round,
        )
    }
}

// ---------------------------------------------------------------- Option A: one list

@Composable
private fun OptionA() {
    val c = Obsidian.colors
    Frame(stats = "5-session streak · 12 h this week · 1 escape") {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
            item { RunningStrip(); Spacer(Modifier.height(14.dp)) }
            items(4) { index ->
                when (index) {
                    0 -> ListRow("Deep work", "1 h 30 min", trailing = { StartPill() })
                    1 -> ListRow("Pomodoro", "25 min", trailing = { StartPill() })
                    2 -> ListRow("Bedtime", "22:30 – 07:00 · every day", trailing = { ObsidianToggle(true, {}) })
                    else -> ListRow("Work focus", "09:00 – 12:00 · weekdays", dim = true, trailing = { ObsidianToggle(false, {}) })
                }
            }
            item {
                Text(
                    "Weekend detox · 10:00 – 18:00 · sat, sun",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textFaint,
                    modifier = Modifier.padding(vertical = 14.dp),
                )
            }
        }
    }
}

@Composable
private fun ListRow(name: String, detail: String, dim: Boolean = false, trailing: @Composable () -> Unit) {
    val c = Obsidian.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 14.dp).alpha(if (dim) 0.5f else 1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, color = c.textNormal)
            Text(detail, style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
        }
        trailing()
    }
}

@Composable
private fun StartPill() {
    val c = Obsidian.colors
    Text(
        "start",
        style = MaterialTheme.typography.labelLarge,
        color = c.accentText,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .border(1.dp, c.accent.copy(alpha = 0.5f), RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 7.dp),
    )
}

// ---------------------------------------------------------------- Option B: focus first

@Composable
private fun OptionB() {
    val c = Obsidian.colors
    Frame {
        Column(Modifier.padding(top = 4.dp)) {
            RunningStrip()
            Text(
                "START A BLOCK",
                style = MaterialTheme.typography.labelMedium,
                color = c.textFaint,
                modifier = Modifier.padding(top = 22.dp, bottom = 10.dp),
            )
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BlockTile("Focus", "60 min")
                BlockTile("Deep work", "1 h 30")
                BlockTile("Pomodoro", "25 min")
            }
            Text(
                "SCHEDULES",
                style = MaterialTheme.typography.labelMedium,
                color = c.textFaint,
                modifier = Modifier.padding(top = 26.dp, bottom = 4.dp),
            )
            ListRow("Bedtime", "22:30 – 07:00 · every day", trailing = { ObsidianToggle(true, {}) })
            ListRow("Work focus", "09:00 – 12:00 · weekdays", dim = true, trailing = { ObsidianToggle(false, {}) })
            ListRow("Weekend detox", "10:00 – 18:00 · sat, sun", trailing = { ObsidianToggle(true, {}) })
        }
    }
}

@Composable
private fun BlockTile(name: String, length: String) {
    val c = Obsidian.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .width(118.dp)
            .clip(shape)
            .background(c.bgSecondary)
            .border(1.dp, c.border, shape)
            .padding(14.dp),
    ) {
        Box(Modifier.size(30.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.15f)))
        Spacer(Modifier.height(10.dp))
        Text(name, style = MaterialTheme.typography.titleSmall, color = c.textNormal, maxLines = 1)
        Text(length, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
    }
}

// ---------------------------------------------------------------- Option C: calm cards

@Composable
private fun OptionC() {
    val c = Obsidian.colors
    Frame(stats = "streak 5 · 12 h · 1 escape") {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 96.dp)) {
            item { RunningStrip(compact = true) }
            item { CalmCard("Deep work", "1 h 30 min", "start", running = false) }
            item { CalmCard("Pomodoro", "25 min", "start", running = false) }
            item { CalmCard("Bedtime", "22:30 – 07:00", "every day", running = false, toggle = true) }
            item { CalmCard("Work focus", "09:00 – 12:00", "weekdays", running = false, toggle = true, on = false) }
            item { CalmCard("Weekend detox", "10:00 – 18:00", "sat, sun", running = false, toggle = true) }
        }
    }
}

@Composable
private fun CalmCard(name: String, big: String, detail: String, running: Boolean, toggle: Boolean = false, on: Boolean = true) {
    val c = Obsidian.colors
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.bgSecondary)
            .border(1.dp, if (running) c.accent else c.border, shape)
            .padding(16.dp)
            .alpha(if (on) 1f else 0.55f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.labelLarge, color = c.textMuted)
            Text(big, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, color = c.textNormal, maxLines = 1)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = c.textFaint)
        }
        if (toggle) ObsidianToggle(on, {}) else CtaButton("Start", onClick = {}, compact = true)
    }
}

/** Kept so the sample schedules aren't flagged as unused while the options are being compared. */
private val samples = listOf(bedtime, work, weekend, focus, deep, pomodoro)
