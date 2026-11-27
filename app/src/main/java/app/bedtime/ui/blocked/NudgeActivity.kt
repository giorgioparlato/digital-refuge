package app.bedtime.ui.blocked

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import app.bedtime.service.BlockingState
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.theme.BedtimeTheme
import app.bedtime.ui.theme.Obsidian
import app.bedtime.ui.formatTime
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.bedtime.data.Repository
import app.bedtime.data.WorthAnswer
import app.bedtime.ui.components.Text
import java.time.LocalDate
import kotlinx.coroutines.launch

/**
 * The nudge after so many minutes in a row on the phone: lock it, or keep going, and the count begins
 * again. Back keeps going too; nothing here is a wall.
 */
class NudgeActivity : ComponentActivity() {
    /** "Was it worth it?" answered here, kept as you leave the nudge, whichever way. */
    private var worth by mutableStateOf<Boolean?>(null)
    private var minutes = 0
    private var app: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        minutes = intent.getIntExtra(EXTRA_MINUTES, 0)
        app = intent.getStringExtra(EXTRA_APP)
        val ask = intent.getBooleanExtra(EXTRA_ASK, false)
        onBackPressedDispatcher.addCallback(this) { keepGoing() }
        setContent {
            BedtimeTheme {
                NudgeScreen(minutes, onLock = ::lock, onKeepGoing = ::keepGoing, worth = worth, onWorth = if (ask) { w -> worth = w } else null)
            }
        }
    }

    private fun keepAnswer() {
        val answer = worth ?: return
        worth = null
        val answered = WorthAnswer(LocalDate.now().toEpochDay(), app, minutes * 60L, answer, System.currentTimeMillis())
        val repo = Repository.get(applicationContext)
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { repo.addWorth(answered) }
    }

    private fun lock() {
        keepAnswer()
        BlockingState.globalAction(this, AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
        finish()
    }

    private fun keepGoing() {
        keepAnswer()
        BlockingState.service?.restartRun()
        finish()
    }

    companion object {
        private const val EXTRA_MINUTES = "minutes"
        private const val EXTRA_ASK = "ask"
        private const val EXTRA_APP = "app"

        /** After [minutes] in a row; with [ask], "was it worth it?" too, about [app] if there's one in front. */
        fun intent(context: Context, minutes: Int, ask: Boolean = false, app: String? = null): Intent =
            Intent(context, NudgeActivity::class.java)
                .putExtra(EXTRA_MINUTES, minutes)
                .putExtra(EXTRA_ASK, ask)
                .apply { app?.let { putExtra(EXTRA_APP, it) } }
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}

/** Not a session, so a plain page: the stretch as the title, the ring full, and the question left with you. */
@Composable
internal fun NudgeScreen(
    minutes: Int,
    onLock: () -> Unit,
    onKeepGoing: () -> Unit,
    /** "Was it worth it?", answered without leaving: what's picked, and picking; null not to ask. */
    worth: Boolean? = null,
    onWorth: ((Boolean) -> Unit)? = null,
) {
    val c = Obsidian.colors
    val context = LocalContext.current
    val since = remember(minutes) { formatTime(context, System.currentTimeMillis() - minutes * 60_000L) }
    CoverLayout(
        where = CoverWhere("A moment to look up", "since $since", 1f),
        emblem = { CoverRing(BedtimeIcons.Hourglass, 1f, c.textNormal) },
        title = "$minutes minutes in a row",
        titleSize = 34.sp,
        body = "You've been on your phone for $minutes minutes without a break.",
        question = "Is this still what you meant to do?",
        extra = onWorth?.let { pick ->
            {
                androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
                    Text("Was it worth it, so far?", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                    WorthChoice(worth, pick)
                }
            }
        },
        primary = CoverAction("Lock the phone", onLock),
        secondary = listOf(CoverAction("Keep going", onKeepGoing)),
    )
}
