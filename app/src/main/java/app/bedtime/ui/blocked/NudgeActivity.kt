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

@Composable
internal fun NudgeScreen(minutes: Int, onLock: () -> Unit, onKeepGoing: () -> Unit) {
    CoverLayout(
        emblem = { CoverEmblem(BedtimeIcons.Hourglass) },
        pill = "$minutes minutes in a row",
        title = "A moment to look up",
        body = "You've been on your phone for $minutes minutes without a break. Is this still what you meant to do?",
        primary = CoverAction("Lock the phone", onLock),
        secondary = listOf(CoverAction("Keep going", onKeepGoing)),
    )
}
