package app.bedtime.ui.components

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Android's status bar hidden on this screen, or shown. Hidden, a swipe down from the very top edge
 * brings it back for a moment, as in other launchers.
 */
@Composable
fun StatusBarVisibility(hidden: Boolean) {
    val activity = LocalContext.current as? Activity ?: return
    LaunchedEffect(hidden) {
        val controller = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (hidden) controller.hide(WindowInsetsCompat.Type.statusBars()) else controller.show(WindowInsetsCompat.Type.statusBars())
    }
}
