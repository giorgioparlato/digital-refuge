package app.bedtime.ui.settings

import app.bedtime.data.ContactSearch
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.rememberLauncherForActivityResult
import android.Manifest
import androidx.compose.ui.zIndex
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.key
import androidx.compose.material.icons.filled.Menu
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
        mapOf(
            SearchOrder.REFUGE_NOTES to "Save in refuge notes",
            SearchOrder.CALENDAR to "Add to calendar, when the words say when",
            SearchOrder.CONTACTS to "Contacts: call, message, WhatsApp",
        )
    val save = { updated: List<SearchChoice> -> scope.launch { repo.updateSettings { it.copy(searchChoices = updated) } } }
    // Contacts are read only once you've said so, here.
    var waitingContacts by remember { mutableStateOf<List<SearchChoice>?>(null) }
    val askContacts = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        waitingContacts?.let { updated -> if (granted) save(updated) }
        waitingContacts = null
    }
    SearchSettingsContent(
        choices = choices.map { it to (labels[it.key] ?: it.key) },
        onChange = { updated ->
            val wantsContacts = updated.any { it.key == SearchOrder.CONTACTS && it.on } && choices.none { it.key == SearchOrder.CONTACTS && it.on }
            if (wantsContacts && !ContactSearch.canRead(context)) {
                waitingContacts = updated
                askContacts.launch(Manifest.permission.READ_CONTACTS)
            } else {
                save(updated)
            }
        },
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
    val labels = choices.associate { (choice, label) -> choice.key to label }
    // While a row is dragged, the order lives here; it's saved when the row is let go.
    var local by remember(list) { mutableStateOf(list) }
    var dragging by remember { mutableStateOf<String?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableIntStateOf(1) }
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Search", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Matching apps always come first. Then, in this order, whatever is switched on. Drag a row by its handle to move it; switched off, it moves below the rest. Only apps a session allows are offered during it.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            SectionCard {
                local.forEach { choice ->
                    key(choice.key) {
                        val lifted = dragging == choice.key
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .onSizeChanged { rowHeight = it.height.coerceAtLeast(1) }
                                .zIndex(if (lifted) 1f else 0f)
                                .graphicsLayer { translationY = if (lifted) offset else 0f }
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (lifted) c.bgPrimaryAlt else Color.Transparent)
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(40.dp)
                                    .pointerInput(choice.key) {
                                        detectDragGestures(
                                            onDragStart = {
                                                dragging = choice.key
                                                offset = 0f
                                            },
                                            onDragEnd = {
                                                dragging = null
                                                offset = 0f
                                                onChange(SearchOrder.onFirst(local))
                                            },
                                            onDragCancel = {
                                                dragging = null
                                                offset = 0f
                                                local = list
                                            },
                                        ) { change, amount ->
                                            change.consume()
                                            offset += amount.y
                                            // Past half a row, it trades places with its neighbour.
                                            val at = local.indexOfFirst { it.key == choice.key }
                                            if (offset > rowHeight / 2f && at < local.lastIndex) {
                                                local = SearchOrder.move(local, at, at + 1)
                                                offset -= rowHeight
                                            } else if (offset < -rowHeight / 2f && at > 0) {
                                                local = SearchOrder.move(local, at, at - 1)
                                                offset += rowHeight
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Default.Menu, contentDescription = "drag to move", tint = c.textFaint, modifier = Modifier.size(20.dp)) }
                            Text(
                                labels[choice.key] ?: choice.key,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (choice.on) c.textNormal else c.textFaint,
                                modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                            )
                            ObsidianToggle(choice.on, { on -> onChange(SearchOrder.switched(local, choice.key, on)) })
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
