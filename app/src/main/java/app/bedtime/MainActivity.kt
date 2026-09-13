package app.bedtime

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.apps.AppCatalog
import app.bedtime.data.Repository
import app.bedtime.data.Templates
import app.bedtime.engine.Engine
import app.bedtime.service.SystemApps
import app.bedtime.ui.groups.GroupsScreen
import app.bedtime.ui.settings.AlwaysAvailableScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.ProvideAppIcons
import app.bedtime.ui.create.CreateScreen
import app.bedtime.ui.edit.ScheduleEditScreen
import app.bedtime.ui.home.HomeScreen
import app.bedtime.ui.homestyle.HomeStyleScreen
import app.bedtime.ui.settings.SettingsScreen
import app.bedtime.ui.setup.SetupScreen
import app.bedtime.ui.theme.BedtimeTheme
import app.bedtime.ui.theme.Obsidian
import app.bedtime.unlock.UnlockFlow

/** String routes for the app's small back stack; also used by the Quick Settings tile. */
object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val SETUP = "setup"
    const val HOME_STYLE = "homestyle"
    const val GROUPS = "groups"
    const val ALWAYS_AVAILABLE = "alwaysavailable"
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
    val stack = rememberSaveable(
        saver = listSaver<SnapshotStateList<String>, String>(save = { it.toList() }, restore = { it.toMutableStateList() }),
    ) { mutableStateListOf(Routes.HOME) }

    fun push(route: String) {
        stack.add(route)
    }

    fun pop(route: String) {
        if (stack.size > 1 && stack.last() == route) stack.removeAt(stack.lastIndex)
    }

    fun popToHome() {
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
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
    }

    LaunchedEffect(pendingRoute) {
        if (pendingRoute != null) {
            if (stack.last() != pendingRoute) push(pendingRoute)
            onRouteConsumed()
        }
    }

    val top = stack.last()
    BackHandler(enabled = stack.size > 1) { pop(top) }

    key(top) {
        when {
            top == Routes.HOME -> HomeScreen(
                onEdit = { id -> push(Routes.EDIT + id) },
                onCreate = { push(Routes.CREATE) },
                onTemplate = { key -> push(Routes.TEMPLATE + key) },
                onSettings = { push(Routes.SETTINGS) },
                onSetup = { push(Routes.SETUP) },
                onUnlock = { id -> push(Routes.unlock(id)) },
            )
            top == Routes.CREATE -> CreateScreen(onBack = { pop(top) }, onPick = { key -> push(Routes.TEMPLATE + key) })
            top == Routes.SETTINGS -> SettingsScreen(
                onBack = { pop(top) },
                onSetup = { push(Routes.SETUP) },
                onHomeStyle = { push(Routes.HOME_STYLE) },
                onGroups = { push(Routes.GROUPS) },
                onAlwaysAvailable = { push(Routes.ALWAYS_AVAILABLE) },
            )
            top == Routes.SETUP -> SetupScreen(onBack = { pop(top) })
            top == Routes.HOME_STYLE -> HomeStyleScreen(onBack = { pop(top) })
            top == Routes.GROUPS -> GroupsScreen(onBack = { pop(top) })
            top == Routes.ALWAYS_AVAILABLE -> AlwaysAvailableScreen(onBack = { pop(top) })
            top.startsWith(Routes.EDIT) -> ScheduleEditScreen(
                scheduleId = top.removePrefix(Routes.EDIT).ifEmpty { null },
                template = null,
                onBack = { pop(top) },
                onSaved = ::popToHome,
                onUnlock = { id -> push(Routes.unlock(id)) },
                onSetup = { push(Routes.SETUP) },
            )
            top.startsWith(Routes.TEMPLATE) -> ScheduleEditScreen(
                scheduleId = null,
                template = top.removePrefix(Routes.TEMPLATE),
                onBack = { pop(top) },
                onSaved = ::popToHome,
                onUnlock = { id -> push(Routes.unlock(id)) },
                onSetup = { push(Routes.SETUP) },
            )
            top.startsWith(Routes.UNLOCK) -> UnlockScreen(top.removePrefix(Routes.UNLOCK), onBack = { pop(top) })
        }
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
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        )
    }
}
