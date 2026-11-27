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

/**
 * The nudge after so many minutes in a row on the phone: lock it, or keep going, and the count begins
 * again. Back keeps going too; nothing here is a wall.
 */
class NudgeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val minutes = intent.getIntExtra(EXTRA_MINUTES, 0)
        onBackPressedDispatcher.addCallback(this) { keepGoing() }
        setContent {
            BedtimeTheme { NudgeScreen(minutes, onLock = ::lock, onKeepGoing = ::keepGoing) }
        }
    }

    private fun lock() {
        BlockingState.globalAction(this, AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
        finish()
    }

    private fun keepGoing() {
        BlockingState.service?.restartRun()
        finish()
    }

    companion object {
        private const val EXTRA_MINUTES = "minutes"

        fun intent(context: Context, minutes: Int): Intent =
            Intent(context, NudgeActivity::class.java)
                .putExtra(EXTRA_MINUTES, minutes)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}

/** Not a session, so a plain page: the stretch as the title, the ring full, and the question left with you. */
@Composable
internal fun NudgeScreen(minutes: Int, onLock: () -> Unit, onKeepGoing: () -> Unit) {
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
        primary = CoverAction("Lock the phone", onLock),
        secondary = listOf(CoverAction("Keep going", onKeepGoing)),
    )
}
