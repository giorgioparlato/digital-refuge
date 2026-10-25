package app.bedtime.ui.blocked

import app.bedtime.data.TextSource
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
import app.bedtime.data.Repository
import app.bedtime.data.CoverMessages
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

    /** Not a session at all but a limit: the pause before opening, or a day's minutes gone. */
    private var mode by mutableStateOf<String?>(null)
    private var limitId by mutableStateOf<String?>(null)
    private var pauseSeconds by mutableIntStateOf(0)
    private var opens by mutableIntStateOf(0)
    private var passKey by mutableStateOf<String?>(null)
    /** Characters to type before opening, when the pause asks for typing; 0 for a wait. */
    private var typing by mutableIntStateOf(0)
    private var typingSource by mutableStateOf(TextSource.WORDS)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        read(intent)
        // Back would reveal the blocked app underneath, so it goes home instead.
        onBackPressedDispatcher.addCallback(this) { goHome() }
        setContent {
            BedtimeTheme {
                val pkg = blockedPackage
                when {
                    mode == MODE_PAUSE && pkg != null -> key(pkg, opens) {
                        PauseScreen(pkg, pauseSeconds, opens, passKey ?: pkg, onHome = ::goHome, onFinish = ::finish, typing = typing, source = typingSource)
                    }
                    mode == MODE_LIMIT && pkg != null -> key(pkg, limitId) {
                        LimitScreen(pkg, limitId.orEmpty(), onHome = ::goHome, onFinish = ::finish)
                    }
                    else -> BlockedScreen(pkg, guardingSettings, onHome = ::goHome, onFinish = ::finish)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        read(intent)
    }

    private fun read(intent: Intent) {
        blockedPackage = intent.getStringExtra(EXTRA_PACKAGE)
        guardingSettings = intent.getBooleanExtra(EXTRA_SETTINGS, false)
        mode = intent.getStringExtra(EXTRA_MODE)
        limitId = intent.getStringExtra(EXTRA_LIMIT)
        pauseSeconds = intent.getIntExtra(EXTRA_SECONDS, 0)
        opens = intent.getIntExtra(EXTRA_OPENS, 0)
        passKey = intent.getStringExtra(EXTRA_PASS)
        typing = intent.getIntExtra(EXTRA_TYPING, 0)
        typingSource = intent.getStringExtra(EXTRA_SOURCE)?.let { runCatching { TextSource.valueOf(it) }.getOrNull() } ?: TextSource.WORDS
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    companion object {
        private const val EXTRA_PACKAGE = "package"
        private const val EXTRA_SETTINGS = "settings"
        private const val EXTRA_MODE = "mode"
        private const val EXTRA_LIMIT = "limit"
        private const val EXTRA_SECONDS = "seconds"
        private const val EXTRA_OPENS = "opens"
        private const val EXTRA_PASS = "pass"
        private const val EXTRA_TYPING = "typing"
        private const val EXTRA_SOURCE = "source"
        private const val MODE_PAUSE = "pause"
        private const val MODE_LIMIT = "limit"

        fun intent(context: Context, pkg: String): Intent =
            Intent(context, BlockedActivity::class.java)
                .putExtra(EXTRA_PACKAGE, pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        /**
         * The pause before opening [pkg]: [seconds] to breathe, or [typing] characters to type, then
         * "open" (granting [passKey]) or "never mind".
         */
        fun pauseIntent(context: Context, pkg: String, seconds: Int, opens: Int, passKey: String, typing: Int = 0, source: TextSource = TextSource.WORDS): Intent =
            intent(context, pkg).putExtra(EXTRA_MODE, MODE_PAUSE).putExtra(EXTRA_SECONDS, seconds)
                .putExtra(EXTRA_OPENS, opens).putExtra(EXTRA_PASS, passKey)
                .putExtra(EXTRA_TYPING, typing).putExtra(EXTRA_SOURCE, source.name)

        /** [pkg]'s minutes for today are gone under limit [limitId]. */
        fun limitIntent(context: Context, pkg: String, limitId: String): Intent =
            intent(context, pkg).putExtra(EXTRA_MODE, MODE_LIMIT).putExtra(EXTRA_LIMIT, limitId)

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
    val settings by Repository.get(context).settings.collectAsStateWithLifecycle(initialValue = null)
    // Drawn once each time the cover comes up, so "a different one each time" doesn't change as you read.
    val message = remember(pkg, blocker?.schedule?.id, settings == null) {
        val loaded = settings ?: return@remember null
        blocker?.let { CoverMessages.pick(it.schedule.message, loaded.customMessages) }
    }

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
                message = message,
            )
        }
    }
}

@Composable
internal fun BlockedContent(
    appLabel: String,
    scheduleName: String,
    until: String,
    onHome: () -> Unit,
    onUnlock: () -> Unit,
    message: String? = null,
) {
    CoverLayout(
        emblem = { CoverEmblem(BedtimeIcons.Moon) },
        pill = "$scheduleName is on",
        title = "$appLabel can wait",
        // With words of its own, the cover says only when; the words say the rest.
        body = if (message == null) "You'll have it back at $until. Until then, maybe put the phone down for a bit." else "You'll have it back at $until.",
        message = message,
        primary = CoverAction("Go to home screen", onHome),
        secondary = listOf(CoverAction("I really need it", onUnlock)),
    )
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
    val minutes = "$breakMinutes ${if (breakMinutes == 1) "minute" else "minutes"}"
    val end = CoverAction("End $scheduleName early", onUnlock)
    CoverLayout(
        emblem = { CoverEmblem(BedtimeIcons.Refuge) },
        pill = "$scheduleName is on",
        title = "This stays locked until $until",
        body = if (onPause != null) {
            "Switching blocking off and uninstalling are locked while $scheduleName runs. If something truly can't " +
                "wait, take an emergency break; otherwise end the session with the unlock steps."
        } else {
            "Switching blocking off and uninstalling are locked while $scheduleName runs. To do either, end the session " +
                "first — the unlock steps still work."
        },
        primary = if (onPause != null) CoverAction("Pause blocking for $minutes", onPause) else end,
        secondary = listOfNotNull(end.takeIf { onPause != null }, CoverAction("Go back", onBack)),
    )
}
