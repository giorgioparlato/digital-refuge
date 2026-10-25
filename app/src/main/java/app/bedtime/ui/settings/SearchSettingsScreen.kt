package app.bedtime.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.SearchChoice
import app.bedtime.data.SearchOrder
import app.bedtime.data.SearchTarget
import app.bedtime.data.SearchTargets
import app.bedtime.data.Repository
import app.bedtime.data.key
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the minimal screen's search offers after apps, and in what order. */
@Composable
fun SearchSettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val found by produceState<List<SearchTarget>?>(null) { value = withContext(Dispatchers.IO) { SearchTargets.find(context) } }
    val current = settings ?: return
    val targets = found ?: return
    val choices = SearchOrder.arrange(SearchOrder.defaults(targets, current.noteTargets), current.searchChoices)
    val labels = targets.associate { it.key to it.choiceLabel() } +
        current.noteTargets.associate { SearchOrder.noteKey(it) to "Add to ${it.name}" } +
        mapOf(SearchOrder.REFUGE_NOTES to "Save in refuge notes", SearchOrder.CALENDAR to "Add to calendar, when the words say when")
    SearchSettingsContent(
        choices = choices.map { it to (labels[it.key] ?: it.key) },
        onChange = { updated -> scope.launch { repo.updateSettings { it.copy(searchChoices = updated) } } },
        onBack = onBack,
    )
}

private fun SearchTarget.choiceLabel(): String = when (kind) {
    SearchTarget.Kind.WEB -> "Search the web · $appLabel"
    SearchTarget.Kind.WIKIPEDIA -> "Wikipedia · $appLabel"
    SearchTarget.Kind.MAPS -> "Maps · $appLabel"
    SearchTarget.Kind.LOOKUP -> "Look up in $appLabel"
}

@Composable
internal fun SearchSettingsContent(choices: List<Pair<SearchChoice, String>>, onChange: (List<SearchChoice>) -> Unit, onBack: () -> Unit) {
    val c = Obsidian.colors
    val list = choices.map { it.first }
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Search", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Matching apps always come first. Then, in this order, whatever is switched on. Only apps a session allows are offered during it.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            SectionCard {
                choices.forEachIndexed { i, (choice, label) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.size(36.dp, 64.dp), verticalArrangement = Arrangement.SpaceEvenly) {
                            Arrow(Icons.Default.KeyboardArrowUp, "move up", enabled = i > 0) { onChange(SearchOrder.moved(list, choice.key, up = true)) }
                            Arrow(Icons.Default.KeyboardArrowDown, "move down", enabled = i < choices.lastIndex) { onChange(SearchOrder.moved(list, choice.key, up = false)) }
                        }
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (choice.on) c.textNormal else c.textFaint,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        )
                        ObsidianToggle(choice.on, { on -> onChange(list.map { if (it.key == choice.key) it.copy(on = on) else it }) })
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Arrow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(30.dp).clip(CircleShape).alpha(if (enabled) 1f else 0.25f).clickable(enabled = enabled, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = label, tint = Obsidian.colors.textMuted, modifier = Modifier.size(20.dp)) }
}
