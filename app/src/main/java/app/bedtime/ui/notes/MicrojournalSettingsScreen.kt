package app.bedtime.ui.notes

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.Repository
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.OptionRow
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.launch

/** The microjournal's own settings: on or off, prompts while writing and their sections, and moods. */
@Composable
fun MicrojournalSettingsScreen(onBack: () -> Unit, onOpenToday: () -> Unit, onOpenPrompts: () -> Unit, onOpenMoods: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val s = settings ?: return
    val c = Obsidian.colors
    fun set(change: (app.bedtime.data.AppSettings) -> app.bedtime.data.AppSettings) = scope.launch { repo.updateSettings(change) }
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Microjournal", onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp)) {
            Text(
                "An entry a day in your notes, a line at a time, each with its time; from the quick note and the share menu too.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            Spacer(Modifier.height(16.dp))
            SectionCard {
                OptionRow(Icons.Default.DateRange, "Microjournal", description = if (s.journalOn) "On" else "Off until you want it") {
                    ObsidianToggle(s.journalOn, { on -> set { it.copy(journalOn = on) } })
                }
                if (s.journalOn) {
                    OptionRow(Icons.Default.Edit, "Today's entry", onClick = onOpenToday) { Chevron() }
                    OptionRow(Icons.Default.Edit, "Prompts while writing", description = "A button away, hidden until you ask; off, no button at all") {
                        ObsidianToggle(s.journalPrompts, { on -> set { it.copy(journalPrompts = on) } })
                    }
                    if (s.journalPrompts) {
                        OptionRow(Icons.Default.Edit, "Sections and prompts", description = "Start from the suggestions; change, delete or add your own", onClick = onOpenPrompts) { Chevron() }
                    }
                    OptionRow(Icons.Default.Edit, "Moods", description = app.bedtime.ui.settings.moodsWords(s), onClick = onOpenMoods) { Chevron() }
                }
            }
            Spacer(Modifier.height(28.dp))
        }
    }
}
