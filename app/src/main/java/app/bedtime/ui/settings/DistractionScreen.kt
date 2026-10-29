package app.bedtime.ui.settings

import app.bedtime.data.Module
import app.bedtime.data.Modules
import app.bedtime.data.Screen
import app.bedtime.ui.homestyle.TextPresets
import app.bedtime.ui.homestyle.SwatchRow
import app.bedtime.ui.homestyle.ColourChoice
import app.bedtime.ui.homestyle.AccentPresets
import app.bedtime.ui.components.QuickChip
import app.bedtime.ui.components.HourDots
import app.bedtime.data.HourState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.HourlyUse
import app.bedtime.data.Repository
import app.bedtime.ui.apps.AppPickerScreen
import app.bedtime.ui.apps.PickerMode
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.OptionRow
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.SegmentedChoice
import app.bedtime.ui.components.Text
import app.bedtime.ui.pluralApps
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.launch

/** Which apps count as distracting, how long makes an hour a distracted one, and where else the day shows. */
@Composable
fun DistractionScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    var picking by rememberSaveable { mutableStateOf(false) }
    val current = settings ?: return
    if (picking) {
        AppPickerScreen(
            "Distracting apps",
            current.distractingApps,
            PickerMode.NOTIFY,
            onDone = { apps ->
                scope.launch { repo.updateSettings { it.copy(distractingApps = apps) } }
                picking = false
            },
            onBack = { picking = false },
        )
        return
    }
    DistractionContent(
        appCount = current.distractingApps.size,
        minutes = current.distractionMinutes,
        onPick = { picking = true },
        onMinutes = { m -> scope.launch { repo.updateSettings { it.copy(distractionMinutes = m) } } },
        totalMinutes = current.distractionTotalMinutes,
        onTotalMinutes = { m -> scope.launch { repo.updateSettings { it.copy(distractionTotalMinutes = m) } } },
        runNudge = current.runNudgeMinutes,
        onRunNudge = { m -> scope.launch { repo.updateSettings { it.copy(runNudgeMinutes = m) } } },
        calmColor = current.homeStyle.hourCalmColor,
        distractedColor = current.homeStyle.hourDistractedColor,
        onCalmColor = { v -> scope.launch { repo.updateSettings { it.copy(homeStyle = it.homeStyle.copy(hourCalmColor = v)) } } },
        onDistractedColor = { v -> scope.launch { repo.updateSettings { it.copy(homeStyle = it.homeStyle.copy(hourDistractedColor = v)) } } },
        onBack = onBack,
    )
}

@Composable
internal fun DistractionContent(
    appCount: Int,
    minutes: Int,
    onPick: () -> Unit,
    onMinutes: (Int) -> Unit,
    onBack: () -> Unit,
    calmColor: Long? = null,
    distractedColor: Long? = null,
    totalMinutes: Int? = null,
    onTotalMinutes: (Int?) -> Unit = {},
    runNudge: Int? = null,
    onRunNudge: (Int?) -> Unit = {},
    onCalmColor: (Long?) -> Unit = {},
    onDistractedColor: (Long?) -> Unit = {},
) {
    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Distracting apps", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Today as 24 dots on the home tab: an hour lights up when you spent more than its minutes in these apps, or, if you like, on the phone at all. Only which app was open is counted, never what was in it.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            SectionCard {
                OptionRow(BedtimeIcons.Grid, "The apps", description = if (appCount == 0) "None yet" else pluralApps(appCount), onClick = onPick) { Chevron() }
            }
            SectionCard(title = "An hour counts after") {
                Text("In these apps", style = MaterialTheme.typography.labelMedium, color = c.textMuted, modifier = Modifier.padding(bottom = 8.dp))
                SegmentedChoice(
                    options = HourlyUse.THRESHOLDS.map { "$it min" },
                    selected = HourlyUse.THRESHOLDS.indexOf(minutes).coerceAtLeast(0),
                    onSelect = { onMinutes(HourlyUse.THRESHOLDS[it]) },
                )
                Text(
                    "Or on the phone at all, in any app",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.textMuted,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                )
                val totals = listOf<Int?>(null) + HourlyUse.TOTAL_THRESHOLDS
                SegmentedChoice(
                    options = totals.map { it?.let { m -> "$m min" } ?: "Off" },
                    selected = totals.indexOf(totalMinutes).coerceAtLeast(0),
                    onSelect = { onTotalMinutes(totals[it]) },
                )
            }
            SectionCard(title = "A nudge after time in a row", subtitle = "After so long on the phone without the screen going off, in any app, a full screen asks if it's still what you meant to do. Never during a call") {
                val nudges = listOf<Int?>(null, 5, 10, 15, 20, 30, 45)
                SegmentedChoice(
                    options = nudges.map { it?.let { m -> "$m" } ?: "Off" },
                    selected = nudges.indexOf(runNudge).coerceAtLeast(0),
                    onSelect = { onRunNudge(nudges[it]) },
                )
                Text("Minutes", style = MaterialTheme.typography.labelMedium, color = c.textMuted, modifier = Modifier.padding(top = 8.dp))
            }
            Text(
                "To see it on your home or lock screen, switch on the distraction tracker in Widgets; its look and colours are there too.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textMuted,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

