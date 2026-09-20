package app.bedtime.ui.minimal

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import app.bedtime.ui.components.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.apps.AppCatalog
import app.bedtime.apps.AppEntry
import app.bedtime.data.AppSettings
import app.bedtime.data.HomeStyle
import app.bedtime.data.Quote
import app.bedtime.data.Quotes
import app.bedtime.data.Repository
import app.bedtime.engine.Engine
import app.bedtime.service.BlockingState
import app.bedtime.service.SystemApps
import app.bedtime.ui.components.AppIcon
import app.bedtime.ui.components.ProvideAppIcons
import app.bedtime.ui.formatTime
import app.bedtime.ui.homestyle.palette
import app.bedtime.ui.theme.BedtimeTheme
import app.bedtime.ui.theme.Obsidian
import app.bedtime.unlock.UnlockFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/** The minimal launcher: a clock and a plain list of allowed apps, styled by the user's [HomeStyle]. */
class MinimalHomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(this) { /* Stay here; there is nowhere to go back to. */ }
        setContent {
            BedtimeTheme {
                ProvideAppIcons { MinimalHomeScreen(onFinish = ::finish, onLightBackground = ::useLightSystemBars) }
            }
        }
    }

    /** Dark status-bar icons on light home backgrounds, light icons on dark ones. */
    private fun useLightSystemBars(light: Boolean) {
        val transparent = android.graphics.Color.TRANSPARENT
        val style = if (light) SystemBarStyle.light(transparent, transparent) else SystemBarStyle.dark(transparent)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, MinimalHomeActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

@Composable
private fun MinimalHomeScreen(onFinish: () -> Unit, onLightBackground: (Boolean) -> Unit) {
    val context = LocalContext.current
    val c = Obsidian.colors
    val repo = remember { Repository.get(context) }
    val state by Engine.state(context).collectAsStateWithLifecycle()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val occurrence = state?.minimalOccurrence
    var unlocking by rememberSaveable { mutableStateOf(false) }
    var emergency by rememberSaveable { mutableStateOf(false) }
    var quoteOffset by rememberSaveable { mutableIntStateOf(0) }
    val style = settings.homeStyle
    val light = style.palette().isLight

    LaunchedEffect(state) {
        if (state != null && occurrence == null) onFinish()
    }
    LaunchedEffect(light, unlocking, emergency) { onLightBackground(if (unlocking || emergency) !c.isDark else light) }

    val allowed = state?.minimalAllowlist.orEmpty()
    var apps by remember { mutableStateOf(emptyList<AppEntry>()) }
    LaunchedEffect(allowed) { apps = withContext(Dispatchers.IO) { entries(context, allowed + listOfNotNull(SystemApps.dialerPackage(context))) } }
    var alwaysAvailableApps by remember { mutableStateOf(emptyList<AppEntry>()) }
    LaunchedEffect(settings.alwaysAvailable) {
        alwaysAvailableApps = withContext(Dispatchers.IO) { entries(context, settings.alwaysAvailable) }
    }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000)
        }
    }

    if (occurrence == null) {
        Surface(Modifier.fillMaxSize(), color = style.palette().background) { }
        return
    }
    when {
        emergency -> {
            BackHandler { emergency = false }
            EmergencyContent(
                onBack = { emergency = false },
                onCall = { SystemApps.dialerPackage(context)?.let { AppCatalog.launch(context, it) } },
                alwaysAvailable = alwaysAvailableApps,
                onOpenApp = { AppCatalog.launch(context, it) },
                onPauseBlocking = {
                    if (!BlockingState.pause(occurrence.schedule.breakMinutes)) context.startActivity(BlockingState.accessibilityIntent())
                    emergency = false
                },
                breakMinutes = occurrence.schedule.breakMinutes,
                pauseEnabled = settings.pauseEnabled,
            )
        }
        unlocking -> Surface(Modifier.fillMaxSize(), color = c.bgPrimary) {
            key(occurrence.schedule.id) {
                UnlockFlow(
                    occurrence,
                    onUnlocked = { },
                    onCancel = { unlocking = false },
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                )
            }
        }
        else -> MinimalHomeContent(
            now = now,
            scheduleName = occurrence.schedule.name,
            until = formatTime(context, occurrence.end),
            apps = apps,
            style = style,
            onLaunch = { AppCatalog.launch(context, it) },
            onUnlock = { unlocking = true },
            onEmergency = { emergency = true },
            quote = Quotes.forDay(now.toLocalDate(), quoteOffset),
            onNextQuote = { quoteOffset++ },
        )
    }
}

/** Launchable apps among [packages], with labels, sorted by name. */
private fun entries(context: Context, packages: Set<String>): List<AppEntry> =
    packages
        .filter { AppCatalog.isLaunchable(context, it) }
        .map { AppEntry(it, AppCatalog.label(context, it)) }
        .sortedBy { it.label.lowercase() }

@Composable
internal fun MinimalHomeContent(
    now: LocalDateTime,
    scheduleName: String,
    until: String,
    apps: List<AppEntry>,
    style: HomeStyle,
    onLaunch: (String) -> Unit,
    onUnlock: () -> Unit,
    onEmergency: () -> Unit = {},
    quote: Quote? = null,
    onNextQuote: () -> Unit = {},
    preview: Boolean = false,
) {
    val p = style.palette()
    val scale = style.textSize.scale
    val names = scale * style.appNameSize.scale
    Column(
        Modifier
            .fillMaxSize()
            .background(p.background)
            .then(if (preview) Modifier else Modifier.safeDrawingPadding())
            .padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        Spacer(Modifier.height(if (preview) 12.dp else 32.dp))
        SessionHeader(now, scheduleName, until, style)
        if (quote != null && style.showQuote) {
            Spacer(Modifier.height(if (preview) 20.dp else 28.dp))
            QuoteBlock(
                quote,
                style,
                Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClickLabel = "show another quote", onClick = onNextQuote),
            )
        }
        Spacer(Modifier.height(if (preview) 28.dp else 44.dp))
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(apps, key = { it.packageName }) { app ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClickLabel = "open ${app.label}") { onLaunch(app.packageName) }
                        .padding(vertical = (12 * names).dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (style.showIcons) {
                        AppIcon(app.packageName, app.label, (30 * names).dp)
                        Spacer(Modifier.width(16.dp))
                    }
                    Text(app.label, fontSize = (26 * names).sp, color = p.text)
                }
            }
            if (apps.isEmpty()) {
                item { Text("nothing to open right now. enjoy the quiet.", fontSize = (18 * scale).sp, color = p.faint) }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onUnlock) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = p.faint, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("unlock", color = p.faint)
            }
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onEmergency) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = p.faint, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("emergency", color = p.faint)
            }
        }
    }
}
