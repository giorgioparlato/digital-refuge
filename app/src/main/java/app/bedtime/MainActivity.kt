package app.bedtime

import androidx.lifecycle.lifecycleScope
import app.bedtime.ui.settings.DistractionScreen
import app.bedtime.ui.settings.SearchSettingsScreen
import app.bedtime.ui.homestyle.StylePage
import app.bedtime.data.Screen
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.apps.AppCatalog
import app.bedtime.data.Changelog
import app.bedtime.data.Repository
import app.bedtime.data.Templates
import app.bedtime.engine.Engine
import app.bedtime.service.SessionAlarms
import app.bedtime.service.SystemApps
import app.bedtime.ui.groups.GroupsScreen
import app.bedtime.ui.settings.AlwaysAvailableScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.ProvideAppIcons
import app.bedtime.ui.create.CreateScreen
import app.bedtime.ui.edit.ScheduleEditScreen
import app.bedtime.ui.home.HomeScreen
import app.bedtime.ui.limits.LimitEditScreen
import app.bedtime.ui.limits.LimitsScreen
import app.bedtime.ui.stats.StatsScreen
import app.bedtime.ui.onboarding.OnboardingScreen
import app.bedtime.ui.onboarding.WalkthroughScreen
import app.bedtime.ui.homestyle.HomeStyleScreen
import app.bedtime.ui.notes.NoteTargetsScreen
import app.bedtime.ui.notes.NotesScreen
import app.bedtime.ui.quotes.QuotesScreen
import app.bedtime.ui.settings.SettingsScreen
import app.bedtime.ui.settings.SettingsPlace
import app.bedtime.ui.setup.SetupScreen
import app.bedtime.ui.theme.BedtimeTheme
import app.bedtime.ui.theme.Obsidian
import app.bedtime.unlock.UnlockFlow

/** String routes for the app's small back stack; also used by the Quick Settings tile. */
object Routes {
    /** The four tabs; everything else is pushed on top of them. */
    /** focus (still "home" to links and widgets), limits, insights, settings. */
    val TABS = listOf(HOME, LIMITS, INSIGHTS, SETTINGS)

    const val HOME = "home"
    const val SCHEDULES = "schedules"
    const val LIMITS = "limits"
    const val SETTINGS = "settings"
    const val INSIGHTS = "insights"
    const val SETUP = "setup"
    const val HOME_STYLE = "homestyle"
    const val STYLE_SHOWN = "homestyle/shown"
    /** Look and widgets, opened on the lock screen's own. */
    const val HOME_STYLE_LOCK = "homestyle/lock"
    const val STYLE_SHOWN_LOCK = "homestyle/shown/lock"
    const val STYLE_SWIPES = "homestyle/swipes"
    const val STYLE_NOTIFICATIONS = "homestyle/notifications"
    const val STYLE_GESTURES = "homestyle/gestures"
    const val SEARCH = "search"
    const val DISTRACTION = "distraction"
    const val HABITS = "habits"
    const val GROUPS = "groups"
    const val ALWAYS_AVAILABLE = "alwaysavailable"
    const val ONBOARDING = "onboarding"
    const val WALKTHROUGH = "walkthrough"
    const val CREATE = "create"
    const val EDIT = "edit/"
    const val TEMPLATE = "template/"
    const val UNLOCK = "unlock/"
    const val LIMIT = "limit/"
    const val STATS = "stats"
    const val QUOTES = "quotes"
    const val NOTES = "notes"
    /** One of your notes, open to edit: "note/<id>". */
    const val NOTE = "note/"
    const val QUICK_NOTES = "quicknotes"
    /** Settings' own pages, one per place ([app.bedtime.ui.settings.SettingsPlace]), and those under them. */
    const val SETTINGS_HOME = "settings/home"
    const val SETTINGS_LOCK = "settings/lock"
    const val SETTINGS_APPS = "settings/apps"
    const val SETTINGS_SHORTCUTS = "settings/shortcuts"
    const val SETTINGS_ABOUT = "settings/about"
    const val APP_LIST = "settings/applist"
    const val COVER_MESSAGES = "settings/messages"
    const val PROBLEMS = "settings/problems"

    fun unlock(scheduleId: String) = UNLOCK + scheduleId

    fun note(id: String) = NOTE + id
}

class MainActivity : ComponentActivity() {
    private val pendingRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Your blocks on the app icon's long press, kept as they change (the blocking service does it too).
        lifecycleScope.launch { app.bedtime.data.Repository.get(this@MainActivity).schedules.collect { app.bedtime.service.BlockShortcuts.update(this@MainActivity, it) } }
        if (savedInstanceState == null) pendingRoute.value = intent.getStringExtra(EXTRA_ROUTE)
        setContent {
            BedtimeTheme {
                ProvideAppIcons {
                    BedtimeNavigation(pendingRoute.value, onRouteConsumed = { pendingRoute.value = null })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingRoute.value = intent.getStringExtra(EXTRA_ROUTE)
    }

    companion object {
        private const val EXTRA_ROUTE = "route"

        fun intent(context: Context, route: String): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_ROUTE, route)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}

@Composable
private fun BedtimeNavigation(pendingRoute: String?, onRouteConsumed: () -> Unit) {
    // Tabs are roots, not history: switching one never stacks. Detail screens push on top.
    var tab by rememberSaveable { mutableStateOf(Routes.HOME) }
    val stack = rememberSaveable(
        saver = listSaver<SnapshotStateList<String>, String>(save = { it.toList() }, restore = { it.toMutableStateList() }),
    ) { mutableStateListOf<String>() }

    fun push(route: String) {
        stack.add(route)
    }

    fun pop() {
        if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
    }

    fun popToTab() {
        stack.clear()
    }

    /** Swaps the screen you are on for another, so Back doesn't return to the one it replaced. */
    fun replace(route: String) {
        pop()
        push(route)
    }

    // One-time starter groups: "Social & feeds" (installed ones) and "Essentials".
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val social = withContext(Dispatchers.IO) { Templates.DISTRACTIONS.filter { AppCatalog.isLaunchable(context, it) }.toSet() }
        Repository.get(context).seedGroups(social, SystemApps.essentials(context))
        val suggested = withContext(Dispatchers.IO) {
            SystemApps.ALWAYS_AVAILABLE_SUGGESTIONS.filter { AppCatalog.isLaunchable(context, it) }.toSet()
        }
        Repository.get(context).seedAlwaysAvailable(suggested)
        Repository.get(context).migrateHomeGreen()
        SessionAlarms.schedule(context)
    }

    LaunchedEffect(pendingRoute) {
        if (pendingRoute != null) {
            // Schedules now live on focus, the home tab.
            val route = if (pendingRoute == Routes.SCHEDULES) Routes.HOME else pendingRoute
            if (route in Routes.TABS) {
                tab = route
                stack.clear()
            } else if (stack.lastOrNull() != route) {
                push(route)
            }
            onRouteConsumed()
        }
    }

    val repo = remember { Repository.get(context) }
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val loaded = settings
    if (loaded != null && !loaded.onboarded) {
        // A fresh install has nothing to catch up on, so its version counts as seen.
        WalkthroughScreen(onDone = {
            scope.launch {
                val installed = Changelog.installedVersion(context)
                repo.updateSettings { it.copy(onboarded = true, lastSeenVersion = installed) }
            }
        })
        return
    }

    val top = stack.lastOrNull()
    BackHandler(enabled = top != null || tab != Routes.HOME) {
        if (top != null) pop() else tab = Routes.HOME
    }

    if (top == null) {
        TabShell(tab, onTab = { tab = it }) {
            key(tab) {
                when (tab) {
                    Routes.INSIGHTS -> StatsScreen(onBack = null)
                    Routes.LIMITS -> LimitsScreen(
                        onEdit = { id -> push(Routes.LIMIT + id) },
                        onCreate = { push(Routes.LIMIT) },
                    )
                    Routes.SETTINGS -> SettingsScreen(onOpen = ::push)
                    else -> HomeScreen(
                        onEdit = { id -> push(Routes.EDIT + id) },
                        onCreate = { push(Routes.CREATE) },
                        onTemplate = { key -> push(Routes.TEMPLATE + key) },
                        onSetup = { push(Routes.SETUP) },
                        onUnlock = { id -> push(Routes.unlock(id)) },
                        onNotes = { push(Routes.NOTES) },
                    )
                }
            }
        }
        return
    }

    key(top) {
        when {
            top == Routes.CREATE -> CreateScreen(onBack = ::pop, onPick = { key -> push(Routes.TEMPLATE + key) })
            top == Routes.SETUP -> SetupScreen(onBack = ::pop, onAlwaysAvailable = { push(Routes.ALWAYS_AVAILABLE) })
            top == Routes.HOME_STYLE -> HomeStyleScreen(onBack = ::pop)
            top == Routes.STYLE_SHOWN -> HomeStyleScreen(onBack = ::pop, page = StylePage.SHOWN, onOpen = ::push)
            top == Routes.HOME_STYLE_LOCK -> HomeStyleScreen(onBack = ::pop, screen = Screen.LOCK)
            top == Routes.STYLE_SHOWN_LOCK -> HomeStyleScreen(onBack = ::pop, page = StylePage.SHOWN, screen = Screen.LOCK, onOpen = ::push)
            top == Routes.STYLE_SWIPES -> HomeStyleScreen(onBack = ::pop, page = StylePage.SWIPES)
            top == Routes.STYLE_NOTIFICATIONS -> HomeStyleScreen(onBack = ::pop, page = StylePage.NOTIFICATIONS)
            top == Routes.STYLE_GESTURES -> HomeStyleScreen(onBack = ::pop, page = StylePage.GESTURES)
            top == Routes.SEARCH -> SearchSettingsScreen(onBack = ::pop)
            top == Routes.APP_LIST -> app.bedtime.ui.settings.AppListScreen(onBack = ::pop)
            top == Routes.COVER_MESSAGES -> app.bedtime.ui.settings.CoverMessagesScreen(onBack = ::pop)
            top == Routes.PROBLEMS -> app.bedtime.ui.settings.ProblemsScreen(onBack = ::pop)
            SettingsPlace.entries.any { it.route == top } -> SettingsScreen(SettingsPlace.entries.first { it.route == top }, onBack = ::pop, onOpen = ::push)
            top == Routes.DISTRACTION -> DistractionScreen(onBack = ::pop)
            top == Routes.HABITS -> app.bedtime.ui.habits.HabitsScreen(onBack = ::pop)
            top == Routes.GROUPS -> GroupsScreen(onBack = ::pop)
            top == Routes.ALWAYS_AVAILABLE -> AlwaysAvailableScreen(onBack = ::pop)
            top == Routes.ONBOARDING -> OnboardingScreen(onDone = ::pop)
            top == Routes.WALKTHROUGH -> WalkthroughScreen(onDone = ::pop)
            top == Routes.STATS -> StatsScreen(onBack = ::pop)
            top == Routes.QUOTES -> QuotesScreen(onBack = ::pop)
            top == Routes.NOTES -> NotesScreen(onBack = ::pop)
            top == Routes.QUICK_NOTES -> NoteTargetsScreen(onBack = ::pop, onOpenNotes = { push(Routes.NOTES) })
            top.startsWith(Routes.EDIT) -> ScheduleEditScreen(
                scheduleId = top.removePrefix(Routes.EDIT).ifEmpty { null },
                template = null,
                onBack = ::pop,
                onSaved = ::popToTab,
                onUnlock = { id -> push(Routes.unlock(id)) },
                onSetup = { push(Routes.SETUP) },
                onHowItWorks = { push(Routes.ONBOARDING) },
                // The copy opens in place of the original, so Back goes to the list, not to a page
                // you have just finished with.
                onDuplicated = { id -> replace(Routes.EDIT + id) },
            )
            top.startsWith(Routes.TEMPLATE) -> ScheduleEditScreen(
                scheduleId = null,
                template = top.removePrefix(Routes.TEMPLATE),
                onBack = ::pop,
                onSaved = ::popToTab,
                onUnlock = { id -> push(Routes.unlock(id)) },
                onSetup = { push(Routes.SETUP) },
                onHowItWorks = { push(Routes.ONBOARDING) },
            )
            top.startsWith(Routes.NOTE) -> NotesScreen(onBack = ::pop, initialOpen = top.removePrefix(Routes.NOTE))
            top.startsWith(Routes.UNLOCK) -> UnlockScreen(top.removePrefix(Routes.UNLOCK), onBack = ::pop)
            top.startsWith(Routes.LIMIT) -> LimitEditScreen(top.removePrefix(Routes.LIMIT).ifEmpty { null }, onBack = ::pop)
        }
    }
}

/** The four-tab frame every root screen sits in. */
@Composable
private fun TabShell(tab: String, onTab: (String) -> Unit, content: @Composable () -> Unit) {
    val c = Obsidian.colors
    Scaffold(
        containerColor = c.bgPrimary,
        bottomBar = {
            Column {
                Box(Modifier.fillMaxWidth().height(1.dp).background(c.border))
                Row(
                    Modifier.fillMaxWidth().background(c.bgSecondary)
                        .navigationBarsPadding()
                        .padding(top = 10.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    TabItem(BedtimeIcons.Refuge, "focus", tab == Routes.HOME || tab == Routes.SCHEDULES) { onTab(Routes.HOME) }
                    TabItem(BedtimeIcons.Hourglass, "limits", tab == Routes.LIMITS) { onTab(Routes.LIMITS) }
                    TabItem(BedtimeIcons.Leaf, "insights", tab == Routes.INSIGHTS) { onTab(Routes.INSIGHTS) }
                    TabItem(Icons.Default.Settings, "settings", tab == Routes.SETTINGS) { onTab(Routes.SETTINGS) }
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding())) { content() }
    }
}

@Composable
private fun TabItem(icon: ImageVector, label: String, on: Boolean, onClick: () -> Unit) {
    val c = Obsidian.colors
    val tint = if (on) c.accent else c.textFaint
    Column(
        Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClickLabel = label, onClick = onClick).padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.width(52.dp).height(28.dp).clip(RoundedCornerShape(50))
                .background(if (on) c.accent.copy(alpha = 0.16f) else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp)) }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

@Composable
private fun UnlockScreen(scheduleId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val state by Engine.state(context).collectAsStateWithLifecycle()
    val occurrence = state?.occurrenceOf(scheduleId)
    // Leaves once the schedule stops enforcing, whether through unlocking or because it ended.
    LaunchedEffect(state) {
        if (state != null && occurrence == null) onBack()
    }
    if (occurrence == null) return
    // The unlock pages carry their own way back ("never mind") and say what they unlock, so no bar.
    Box(Modifier.fillMaxSize().background(Obsidian.colors.bgPrimary)) {
        UnlockFlow(
            occurrence,
            onUnlocked = { },
            onCancel = onBack,
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .imePadding(),
        )
    }
}
