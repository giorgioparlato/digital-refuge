package app.bedtime.ui.settings

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
        onHome = current.homeStyle.hoursOnHome,
        onLock = current.homeStyle.hoursOnLock,
        onPick = { picking = true },
        onMinutes = { m -> scope.launch { repo.updateSettings { it.copy(distractionMinutes = m) } } },
        onShowHome = { on -> scope.launch { repo.updateSettings { it.copy(homeStyle = it.homeStyle.copy(hoursOnHome = on)) } } },
        onShowLock = { on -> scope.launch { repo.updateSettings { it.copy(homeStyle = it.homeStyle.copy(hoursOnLock = on)) } } },
        onBack = onBack,
    )
}

@Composable
internal fun DistractionContent(
    appCount: Int,
    minutes: Int,
    onHome: Boolean,
    onLock: Boolean,
    onPick: () -> Unit,
    onMinutes: (Int) -> Unit,
    onShowHome: (Boolean) -> Unit,
    onShowLock: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Distracting apps", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Today as 24 dots on the home tab: an hour lights up when you spent more than its minutes in these apps. Only which app was open is counted, never what was in it.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            SectionCard {
                OptionRow(BedtimeIcons.Grid, "The apps", description = if (appCount == 0) "None yet" else pluralApps(appCount), onClick = onPick) { Chevron() }
            }
            SectionCard(title = "An hour counts after") {
                SegmentedChoice(
                    options = HourlyUse.THRESHOLDS.map { "$it min" },
                    selected = HourlyUse.THRESHOLDS.indexOf(minutes).coerceAtLeast(0),
                    onSelect = { onMinutes(HourlyUse.THRESHOLDS[it]) },
                )
            }
            SectionCard(title = "Also show it") {
                OptionRow(Icons.Default.Home, "On the minimal screen", description = "A row under the date") { ObsidianToggle(onHome, onShowHome) }
                OptionRow(Icons.Default.Lock, "On the lock screen") { ObsidianToggle(onLock, onShowLock) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
