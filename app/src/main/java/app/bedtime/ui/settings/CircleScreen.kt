package app.bedtime.ui.settings

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.Circle
import app.bedtime.data.ContactSearch
import app.bedtime.data.LockNotifications
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.ObsidianTextField
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.OptionRow
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.QuickChip
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.VerbatimText
import app.bedtime.ui.homestyle.AccentPresets
import app.bedtime.ui.homestyle.ColourChoice
import app.bedtime.ui.homestyle.DistractedPresets
import app.bedtime.ui.theme.Obsidian

/**
 * One circle: its name, its colour on the lock screen, the people in it and the group chats. Changes
 * are saved as they're made, through [onChange].
 */
@Composable
fun CircleScreen(circle: Circle, onChange: (Circle) -> Unit, onDelete: (() -> Unit)?, onBack: () -> Unit) {
    val context = LocalContext.current
    var pickingPeople by rememberSaveable { mutableStateOf(false) }
    val askContacts = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) pickingPeople = true else Toast.makeText(context, "contacts access wasn't allowed", Toast.LENGTH_SHORT).show()
    }
    // Group chats on the lock screen right now: the easiest way to name one is to tap it.
    val showing by LockNotifications.current.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    if (pickingPeople) {
        PeoplePickerScreen(
            circle.name,
            circle.people,
            onDone = { people ->
                onChange(circle.copy(people = people))
                pickingPeople = false
            },
            onBack = { pickingPeople = false },
        )
        return
    }
    CircleContent(
        circle = circle,
        seenGroups = showing.mapNotNull { it.group }.distinct(),
        onChange = onChange,
        onPickPeople = { if (ContactSearch.canRead(context)) pickingPeople = true else askContacts.launch(Manifest.permission.READ_CONTACTS) },
        onDelete = onDelete,
        onBack = onBack,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CircleContent(
    circle: Circle,
    seenGroups: List<String>,
    onChange: (Circle) -> Unit,
    onPickPeople: () -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit,
) {
    val c = Obsidian.colors
    var name by rememberSaveable(circle.id) { mutableStateOf(circle.name) }
    var group by rememberSaveable { mutableStateOf("") }
    fun addGroup(named: String) {
        val clean = named.trim()
        if (clean.isNotEmpty() && circle.groups.none { it.equals(clean, ignoreCase = true) }) onChange(circle.copy(groups = circle.groups + clean))
        group = ""
    }
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar(circle.name.ifBlank { "A circle" }, onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Their notifications show on the lock screen whatever app they come from, above the rest, marked in the circle's colour.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            ObsidianTextField(
                value = name,
                onValueChange = {
                    name = it
                    if (it.isNotBlank()) onChange(circle.copy(name = it.trim()))
                },
                label = "Its name",
                modifier = Modifier.fillMaxWidth(),
            )
            SectionCard(title = "Its colour") {
                ColourChoice(AccentPresets + DistractedPresets, circle.colour, { onChange(circle.copy(colour = it)) }, automatic = "Accent")
            }
            SectionCard(title = "People", subtitle = "From your contacts, in any app: messages, calls, mail") {
                OptionRow(
                    Icons.Default.Person,
                    if (circle.people.isEmpty()) "Choose people" else circle.people.sorted().joinToString(", "),
                    description = if (circle.people.isEmpty()) null else "Tap to change",
                    onClick = onPickPeople,
                ) { Chevron() }
            }
            SectionCard(title = "Group chats", subtitle = "By the group's name, as the app shows it. Someone in this circle writing in another group doesn't bring that group in") {
                circle.groups.sorted().forEach { g ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        VerbatimText(g, style = MaterialTheme.typography.bodyLarge, color = c.textNormal, modifier = Modifier.weight(1f))
                        Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClickLabel = "take $g out") { onChange(circle.copy(groups = circle.groups - g)) }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Clear, contentDescription = "take $g out", tint = c.textFaint, modifier = Modifier.size(18.dp))
                        }
                    }
                }
                val suggested = seenGroups.filter { seen -> circle.groups.none { it.equals(seen, ignoreCase = true) } }
                if (suggested.isNotEmpty()) {
                    Text("On your lock screen now", style = MaterialTheme.typography.labelMedium, color = c.textMuted, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggested.forEach { QuickChip("+ $it", false) { addGroup(it) } }
                    }
                }
                ObsidianTextField(value = group, onValueChange = { group = it }, label = "A group's name", modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(enabled = group.isNotBlank(), onClick = { addGroup(group) }) {
                        Text("Add", color = if (group.isNotBlank()) c.accentText else c.textFaint, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (onDelete != null) {
                TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Delete this circle", color = c.textMuted) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
