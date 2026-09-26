package app.bedtime

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
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
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.ProvideAppIcons
import app.bedtime.ui.create.CreateScreen
import app.bedtime.ui.edit.ScheduleEditScreen
import app.bedtime.ui.home.HomeScreen
import app.bedtime.ui.onboarding.OnboardingScreen
import app.bedtime.ui.onboarding.WalkthroughScreen
import app.bedtime.ui.homestyle.HomeStyleScreen
import app.bedtime.ui.schedules.SchedulesScreen
import app.bedtime.ui.settings.SettingsScreen
import app.bedtime.ui.setup.SetupScreen
import app.bedtime.ui.theme.BedtimeTheme
import app.bedtime.ui.theme.Obsidian
import app.bedtime.unlock.UnlockFlow

/** String routes for the app's small back stack; also used by the Quick Settings tile. */
object Routes {
    /** The three tabs; everything else is pushed on top of them. */
    val TABS = listOf(HOME, SCHEDULES, SETTINGS)

    const val HOME = "home"
    const val SCHEDULES = "schedules"
    const val SETTINGS = "settings"
    const val SETUP = "setup"
    const val HOME_STYLE = "homestyle"
    const val GROUPS = "groups"
    const val ALWAYS_AVAILABLE = "alwaysavailable"
    const val ONBOARDING = "onboarding"
    const val WALKTHROUGH = "walkthrough"
    const val CREATE = "create"
    const val EDIT = "edit/"
    const val TEMPLATE = "template/"
    const val UNLOCK = "unlock/"

    fun unlock(scheduleId: String) = UNLOCK + scheduleId
}

class MainActivity : ComponentActivity() {
    private val pendingRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
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

    // One-time starter groups: "Social & feeds" (installed ones) and "Essentials".
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        val social = withContext(Dispatchers.IO) { Templates.DISTRACTIONS.filter { AppCatalog.isLaunchable(context, it) }.toSet() }
        Repository.get(context).seedGroups(social, SystemApps.essentials(context))
        val suggested = withContext(Dispatchers.IO) {
            SystemApps.ALWAYS_AVAILABLE_SUGGESTIONS.filter { AppCatalog.isLaunchable(context, it) }.toSet()
        }
        Repository.get(context).seedAlwaysAvailable(suggested)
        SessionAlarms.schedule(context)
    }

    LaunchedEffect(pendingRoute) {
        if (pendingRoute != null) {
            if (pendingRoute in Routes.TABS) {
                tab = pendingRoute
                stack.clear()
            } else if (stack.lastOrNull() != pendingRoute) {
                push(pendingRoute)
            }
            onRouteConsumed()
        }
    }

    val repo = remember { Repository.get(context) }
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val loaded = settings
    if (loaded != null && !loaded.onboarded) {
        WalkthroughScreen(onDone = { scope.launch { repo.updateSettings { it.copy(onboarded = true) } } })
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
                    Routes.SCHEDULES -> SchedulesScreen(
                        onEdit = { id -> push(Routes.EDIT + id) },
                        onCreate = { push(Routes.CREATE) },
                    )
                    Routes.SETTINGS -> SettingsScreen(
                        onSetup = { push(Routes.SETUP) },
                        onHomeStyle = { push(Routes.HOME_STYLE) },
                        onGroups = { push(Routes.GROUPS) },
                        onAlwaysAvailable = { push(Routes.ALWAYS_AVAILABLE) },
                        onHowItWorks = { push(Routes.ONBOARDING) },
                        onWalkthrough = { push(Routes.WALKTHROUGH) },
                    )
                    else -> HomeScreen(
                        onEdit = { id -> push(Routes.EDIT + id) },
                        onCreate = { push(Routes.CREATE) },
                        onTemplate = { key -> push(Routes.TEMPLATE + key) },
                        onSetup = { push(Routes.SETUP) },
                        onUnlock = { id -> push(Routes.unlock(id)) },
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
            top == Routes.GROUPS -> GroupsScreen(onBack = ::pop)
            top == Routes.ALWAYS_AVAILABLE -> AlwaysAvailableScreen(onBack = ::pop)
            top == Routes.ONBOARDING -> OnboardingScreen(onDone = ::pop)
            top == Routes.WALKTHROUGH -> WalkthroughScreen(onDone = ::pop)
            top.startsWith(Routes.EDIT) -> ScheduleEditScreen(
                scheduleId = top.removePrefix(Routes.EDIT).ifEmpty { null },
                template = null,
                onBack = ::pop,
                onSaved = ::popToTab,
                onUnlock = { id -> push(Routes.unlock(id)) },
                onSetup = { push(Routes.SETUP) },
                onHowItWorks = { push(Routes.ONBOARDING) },
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
            top.startsWith(Routes.UNLOCK) -> UnlockScreen(top.removePrefix(Routes.UNLOCK), onBack = ::pop)
        }
    }
}

/** The three-tab frame every root screen sits in. */
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
                    TabItem(BedtimeIcons.Refuge, "home", tab == Routes.HOME) { onTab(Routes.HOME) }
                    TabItem(Icons.Default.DateRange, "schedules", tab == Routes.SCHEDULES) { onTab(Routes.SCHEDULES) }
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
    Scaffold(containerColor = Obsidian.colors.bgPrimary, topBar = { ObsidianTopBar("Unlock", onBack = onBack) }) { padding ->
        UnlockFlow(
            occurrence,
            onUnlocked = { },
            onCancel = onBack,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
        )
    }
}
