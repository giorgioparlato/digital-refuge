package app.bedtime.ui.onboarding

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.Routes
import app.bedtime.data.AppSettings
import app.bedtime.data.HomeActions
import app.bedtime.data.HomeGesture
import app.bedtime.data.Repository
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.launch

/**
 * What else refuge does, when you want it: a card each, with a switch where it starts off and a link
 * to the place itself. Search, gestures and widgets come together, as they live on refuge's own home screen.
 */
@Composable
fun TourScreen(onBack: () -> Unit, onOpen: (String) -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val s = settings ?: return
    val set = { change: (AppSettings) -> AppSettings -> scope.launch { repo.updateSettings(change) }; Unit }
    val c = Obsidian.colors

    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("A tour", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("What else refuge does, when you want it. Each stays off or quiet until you choose, and is in settings too.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)

            Feature("Habits", "Small things done daily, and how strong each is getting. A habit can be a set of steps, for a routine.", "Your habits", { onOpen(Routes.HABITS) }) {
                Switch("A habits tab", s.habitsTab) { on -> set { it.copy(habitsTab = on) } }
            }
            Feature(
                "Notes and the microjournal",
                "Notes kept in refuge, and the microjournal: a page a day of timed lines. The quick note writes into either from the lock screen, without unlocking.",
                "Your notes",
                { onOpen(Routes.NOTES) },
            ) {
                Switch("A notes tab", s.notesTab) { on -> set { it.copy(notesTab = on) } }
                Switch("The microjournal", s.journalOn) { on -> set { it.copy(journalOn = on) } }
            }
            Feature(
                "Voice notes",
                "Speak and it's written out on the phone itself, by Whisper; nothing is sent anywhere. It needs a folder for the recordings and a Whisper model you download once.",
                "Set it up",
                { onOpen(Routes.QUICK_NOTES) },
            )
            Feature(
                "Check-ins and moods",
                "A question when you pick up the phone (why now?), moods to tap there and in the microjournal, and a nudge after a long stretch. What you answer shows in insights.",
                "Check-ins",
                { onOpen(Routes.SETTINGS_FOCUS) },
            )
            Feature(
                "Obsidian",
                "Quick notes straight into a file in your vault, your vault's notes in search, and your notes and microjournal exported as markdown files.",
                "Notes settings",
                { onOpen(Routes.QUICK_NOTES) },
            )

            Column(Modifier.padding(top = 8.dp)) {
                Text("On refuge's home screen", style = MaterialTheme.typography.titleMedium, color = c.textNormal)
                Text(
                    "Widgets, search and gestures live on refuge's own home screen: as your everyday launcher, or as the quiet home screen during a session.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textMuted,
                )
                if (!s.homeScreen) Link("Use refuge as my home screen") { onOpen(Routes.LAUNCHER) }
            }
            Feature(
                "Widgets",
                "The time, weather, the player, your habits, a note beside the clock, the day against your budget. Long-press any widget for its options. Refuge's lock screen has its own.",
                "Home screen look",
                { onOpen(Routes.HOME_STYLE) },
            )
            Feature(
                "Search",
                "Type to find anything: apps, contacts, notes, events, a word's meaning. A short word first says where to look, like m for maps, d for the dictionary or @ for contacts.",
                "Search settings",
                { onOpen(Routes.SEARCH) },
            )
            Feature("Gestures", "What yours do now:", "Gestures", { onOpen(Routes.STYLE_GESTURES) }) {
                Column(Modifier.padding(top = 4.dp)) {
                    gestureLines(s, context).forEach { (gesture, what) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                            Text(gesture, style = MaterialTheme.typography.bodyMedium, color = c.textNormal, modifier = Modifier.weight(1f))
                            Text(what, style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                        }
                    }
                }
            }
            Spacer(Modifier.padding(bottom = 16.dp))
        }
    }
}

/** Each home-screen gesture and what it does now: an action, an app by its name, or nothing yet. */
private fun gestureLines(s: AppSettings, context: android.content.Context): List<Pair<String, String>> {
    val names = mapOf(
        HomeGesture.SWIPE_DOWN to "Swipe down",
        HomeGesture.SWIPE_LEFT to "Swipe left",
        HomeGesture.SWIPE_RIGHT to "Swipe right",
        HomeGesture.DOUBLE_TAP to "Double tap",
        HomeGesture.CLOCK_DOUBLE_TAP to "Double tap the clock",
    )
    val set = s.homeStyle.homeGestures
    return listOf("Swipe up" to "All your apps") + HomeGesture.entries.map { gesture ->
        val action = set[gesture]
        names.getValue(gesture) to when {
            action == null -> if (gesture == HomeGesture.CLOCK_DOUBLE_TAP) "As a double tap" else "Nothing yet"
            else -> HomeActions.label(action)
                ?: runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(action, 0)).toString() }.getOrDefault("An app")
        }
    }
}

@Composable
private fun Feature(title: String, what: String, link: String, onLink: () -> Unit, extra: @Composable ColumnScope.() -> Unit = {}) {
    SectionCard(title = title) {
        Text(what, style = MaterialTheme.typography.bodyMedium, color = Obsidian.colors.textMuted)
        extra()
        Link(link, onLink)
    }
}

@Composable
private fun Switch(label: String, on: Boolean, onOn: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = Obsidian.colors.textNormal, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        ObsidianToggle(on, onOn)
    }
}

@Composable
private fun Link(label: String, onClick: () -> Unit) {
    Text(
        "$label ›",
        style = MaterialTheme.typography.labelLarge,
        color = Obsidian.colors.accentText,
        modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
    )
}
