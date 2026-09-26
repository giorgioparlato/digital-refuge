package app.bedtime.ui.blocked

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import app.bedtime.ui.components.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.apps.AppCatalog
import app.bedtime.engine.Engine
import app.bedtime.service.BlockingState
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.StatusPill
import app.bedtime.ui.formatTime
import app.bedtime.ui.theme.BedtimeTheme
import app.bedtime.ui.theme.Obsidian
import app.bedtime.unlock.UnlockFlow

/** Full-screen cover shown on top of a blocked app. */
class BlockedActivity : ComponentActivity() {
    private var blockedPackage by mutableStateOf<String?>(null)

    /** Covering one of the Settings screens that switch blocking off, rather than a blocked app. */
    private var guardingSettings by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        blockedPackage = intent.getStringExtra(EXTRA_PACKAGE)
        guardingSettings = intent.getBooleanExtra(EXTRA_SETTINGS, false)
        // Back would reveal the blocked app underneath, so it goes home instead.
        onBackPressedDispatcher.addCallback(this) { goHome() }
        setContent {
            BedtimeTheme {
                BlockedScreen(blockedPackage, guardingSettings, onHome = ::goHome, onFinish = ::finish)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        blockedPackage = intent.getStringExtra(EXTRA_PACKAGE)
        guardingSettings = intent.getBooleanExtra(EXTRA_SETTINGS, false)
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    companion object {
        private const val EXTRA_PACKAGE = "package"
        private const val EXTRA_SETTINGS = "settings"

        fun intent(context: Context, pkg: String): Intent =
            Intent(context, BlockedActivity::class.java)
                .putExtra(EXTRA_PACKAGE, pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        /** Covers a Settings screen that would switch blocking off. */
        fun settingsIntent(context: Context): Intent =
            Intent(context, BlockedActivity::class.java)
                .putExtra(EXTRA_SETTINGS, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

@Composable
private fun BlockedScreen(pkg: String?, guardingSettings: Boolean, onHome: () -> Unit, onFinish: () -> Unit) {
    val context = LocalContext.current
    val c = Obsidian.colors
    val state by Engine.state(context).collectAsStateWithLifecycle()
    // Guarding Settings, the session to leave is the longest-running one; otherwise, whichever blocks this app.
    val blocker = if (guardingSettings) state?.active?.maxByOrNull { it.end } else pkg?.let { state?.blockerOf(it) }
    var unlocking by rememberSaveable { mutableStateOf(false) }
    val appLabel = remember(pkg) { pkg?.let { AppCatalog.label(context, it) }.orEmpty() }

    // Unlocked, schedule ended, or opened without a package: nothing left to cover.
    LaunchedEffect(state, pkg) {
        if (state != null && blocker == null) onFinish()
    }

    Surface(Modifier.fillMaxSize(), color = c.bgPrimary) {
        if (blocker == null) return@Surface
        if (unlocking) {
            // Keyed so a second blocking schedule gets a fresh flow instead of this one's finished state.
            key(blocker.schedule.id) {
                UnlockFlow(
                    blocker,
                    onUnlocked = { },
                    onCancel = { unlocking = false },
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .imePadding(),
                )
            }
        } else if (guardingSettings) {
            SettingsGuardContent(
                scheduleName = blocker.schedule.name,
                until = formatTime(context, blocker.end),
                breakMinutes = blocker.schedule.breakMinutes,
                onPause = if (blocker.schedule.pauseEnabled) {
                    { if (BlockingState.pause(blocker.schedule.breakMinutes)) onFinish() }
                } else {
                    null
                },
                onUnlock = { unlocking = true },
                onBack = onHome,
            )
        } else {
            BlockedContent(
                appLabel = appLabel,
                scheduleName = blocker.schedule.name,
                until = formatTime(context, blocker.end),
                onHome = onHome,
                onUnlock = { unlocking = true },
            )
        }
    }
}

@Composable
internal fun BlockedContent(appLabel: String, scheduleName: String, until: String, onHome: () -> Unit, onUnlock: () -> Unit) {
    val c = Obsidian.colors
    Column(
        Modifier
            .fillMaxSize()
            .background(c.bgPrimary)
            .safeDrawingPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(124.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(88.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(BedtimeIcons.Moon, contentDescription = null, tint = c.accent, modifier = Modifier.size(44.dp))
            }
        }
        Spacer(Modifier.height(28.dp))
        StatusPill("$scheduleName is on", c.accentText)
        Spacer(Modifier.height(16.dp))
        Text(
            "$appLabel can wait",
            style = MaterialTheme.typography.headlineMedium,
            color = c.textNormal,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "You'll have it back at $until. Until then, maybe put the phone down for a bit.",
            style = MaterialTheme.typography.bodyLarge,
            color = c.textMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1.3f))
        CtaButton("Go to home screen", onClick = onHome, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = onUnlock, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text("I really need it", color = c.textMuted)
        }
    }
}

/** Shown over the Settings screens that would switch blocking off during a session. */
@Composable
internal fun SettingsGuardContent(
    scheduleName: String,
    until: String,
    breakMinutes: Int,
    onPause: (() -> Unit)?,
    onUnlock: () -> Unit,
    onBack: () -> Unit,
) {
    val c = Obsidian.colors
    val minutes = "$breakMinutes ${if (breakMinutes == 1) "minute" else "minutes"}"
    Column(
        Modifier
            .fillMaxSize()
            .background(c.bgPrimary)
            .safeDrawingPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(124.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.08f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(88.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(BedtimeIcons.Refuge, contentDescription = null, tint = c.accent, modifier = Modifier.size(44.dp))
            }
        }
        Spacer(Modifier.height(28.dp))
        StatusPill("$scheduleName is on", c.accentText)
        Spacer(Modifier.height(16.dp))
        Text(
            "This stays locked until $until",
            style = MaterialTheme.typography.headlineMedium,
            color = c.textNormal,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (onPause != null) {
                "Switching blocking off and uninstalling are locked while $scheduleName runs. If something truly can't " +
                    "wait, take an emergency break; otherwise end the session with the unlock steps."
            } else {
                "Switching blocking off and uninstalling are locked while $scheduleName runs. To do either, end the session " +
                    "first — the unlock steps still work."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = c.textMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1.3f))
        if (onPause != null) {
            CtaButton("Pause blocking for $minutes", onClick = onPause, modifier = Modifier.fillMaxWidth())
            TextButton(onClick = onUnlock, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text("End $scheduleName early", color = c.textMuted)
            }
        } else {
            CtaButton("End $scheduleName early", onClick = onUnlock, modifier = Modifier.fillMaxWidth())
        }
        TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Go back", color = c.textMuted)
        }
    }
}
