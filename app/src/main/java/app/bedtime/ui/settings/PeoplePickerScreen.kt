package app.bedtime.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.bedtime.data.ContactSearch
import app.bedtime.ui.components.ObsidianTextField
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.VerbatimText
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Choose people from your contacts, by name: whose notifications show on the lock screen. */
@Composable
fun PeoplePickerScreen(title: String, initial: Set<String>, onDone: (Set<String>) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val names by produceState<List<String>?>(null) { value = withContext(Dispatchers.IO) { ContactSearch.names(context) } }
    BackHandler(onBack = onBack)
    PeoplePickerContent(title, names, initial, onDone, onBack)
}

@Composable
internal fun PeoplePickerContent(title: String, names: List<String>?, initial: Set<String>, onDone: (Set<String>) -> Unit, onBack: () -> Unit) {
    val c = Obsidian.colors
    var chosen by remember { mutableStateOf(initial) }
    var query by remember { mutableStateOf("") }
    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            ObsidianTopBar(title, onBack = onBack) {
                TextButton(onClick = { onDone(chosen) }) { Text("Done", color = c.accentText) }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = PageGutter)) {
            ObsidianTextField(query, { query = it }, placeholder = "Search your contacts", modifier = Modifier.fillMaxWidth())
            if (names == null) return@Column
            if (names.isEmpty()) {
                Text("No contacts to choose from, or contacts access wasn't allowed.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.padding(top = 16.dp))
                return@Column
            }
            // Those already chosen first, so they're easy to take off again.
            val shown = names.filter { it.contains(query.trim(), ignoreCase = true) }.sortedBy { it !in initial }
            LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                items(shown, key = { it }) { name ->
                    val on = name in chosen
                    Row(
                        Modifier.fillMaxWidth().clickable { chosen = if (on) chosen - name else chosen + name }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(on, { chosen = if (on) chosen - name else chosen + name }, colors = CheckboxDefaults.colors(checkedColor = c.accent))
                        VerbatimText(name, style = MaterialTheme.typography.bodyLarge, color = c.textNormal)
                    }
                }
            }
        }
    }
}
