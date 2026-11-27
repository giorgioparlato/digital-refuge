package app.bedtime.service

import app.bedtime.data.Problems

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.View
import android.view.WindowManager
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import app.bedtime.ui.blocked.Takeover
import app.bedtime.ui.blocked.TakeoverScreen
import app.bedtime.ui.formatTime
import app.bedtime.ui.theme.BedtimeTheme

/**
 * A full-screen wall shown by [SessionGuardService] while blocking is switched off during a session,
 * so it can't quietly stay off. Drawn with "display over other apps" so it covers whatever's in front,
 * even whatever is in front. Two ways out: switch blocking back on, or take another break. Laid out
 * like every other cover ([TakeoverScreen]), drawn with Compose in a window of its own.
 */
@SuppressLint("StaticFieldLeak")
object TakeoverOverlay {
    private var view: View? = null
    private var owner: OverlayOwner? = null

    /** What the wall says; changing it redraws the wall in place, so it can be updated every second. */
    private val shown = mutableStateOf<Takeover?>(null)

    /** Whether what's up is a session's wall, rather than [OffCover]'s outside one. */
    @Volatile
    private var forSession = true

    fun isShowing(): Boolean = view != null

    fun isForSession(): Boolean = view != null && forSession

    fun canShow(context: Context): Boolean = Settings.canDrawOverlays(context)

    /**
     * The same wall outside a session, for [OffCover]: blocking has been off [minutes]. Two ways out,
     * no more: switch it back on, or "later", which asks again after as long.
     */
    @Synchronized
    fun showOff(context: Context, minutes: Int) {
        val app = context.applicationContext
        if (view != null && forSession) return
        val now = System.currentTimeMillis()
        forSession = false
        shown.value = Takeover.Off(
            minutes = minutes,
            since = formatTime(app, now - minutes * 60_000L),
            later = formatTime(app, now + minutes * 60_000L),
            onEnable = {
                runCatching { app.startActivity(BlockingState.accessibilityIntent()) }
                // If it isn't switched on after all, the cover comes back as after "later".
                OffCover.schedule(app, minutes)
                hide(app)
            },
            onLater = {
                OffCover.schedule(app, minutes)
                hide(app)
            },
        )
        attach(app)
    }

    /**
     * Shows the wall (or updates it) for [scheduleName], running from [start] to [end]; [allowSnooze]
     * offers another break. [afterBreak]: an emergency break has just run out, and with [askMood] the
     * wall asks how you feel as well, so it's one screen rather than two.
     */
    @Synchronized
    fun show(
        context: Context,
        scheduleName: String,
        breakMinutes: Int,
        allowSnooze: Boolean,
        start: Long = 0L,
        end: Long = 0L,
        afterBreak: Boolean = false,
        askMood: Boolean = false,
    ) {
        val app = context.applicationContext
        forSession = true
        if (!Settings.canDrawOverlays(app)) return
        shown.value = Takeover.Session(
            name = scheduleName,
            until = if (end > 0) formatTime(app, end) else null,
            progress = if (end > start && start > 0) ((System.currentTimeMillis() - start).toFloat() / (end - start)).coerceIn(0f, 1f) else 0f,
            breakMinutes = breakMinutes,
            afterBreak = afterBreak,
            askMood = askMood,
            onEnable = {
                // Let go of the wall for a moment so the accessibility screen isn't covered by it.
                BlockingState.suppressOverlay(60_000)
                runCatching { app.startActivity(BlockingState.accessibilityIntent()) }
                hide(app)
            },
            onSnooze = if (allowSnooze) {
                {
                    BlockingState.snooze(breakMinutes)
                    hide(app)
                }
            } else {
                null
            },
        )
        attach(app)
    }

    /** The wall's window, added to the screen if it isn't there yet; null if it can't be drawn. */
    private fun attach(app: Context): View? {
        if (!Settings.canDrawOverlays(app)) return null
        val wm = app.getSystemService(WindowManager::class.java) ?: return null
        if (view == null) {
            val life = OverlayOwner()
            val v = ComposeView(app).apply {
                setViewTreeLifecycleOwner(life)
                setViewTreeSavedStateRegistryOwner(life)
                setContent { BedtimeTheme { shown.value?.let { TakeoverScreen(it) } } }
            }
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.OPAQUE,
            )
            Problems.attempt("drawing the full-screen reminder") { wm.addView(v, params) }?.let {
                view = v
                owner = life
            } ?: life.destroy()
        }
        return view
    }

    @Synchronized
    fun hide(context: Context) {
        val v = view ?: return
        val wm = context.applicationContext.getSystemService(WindowManager::class.java)
        runCatching { wm?.removeView(v) }
        owner?.destroy()
        owner = null
        view = null
    }
}

/** What a window outside any activity needs for Compose to draw in it: a lifecycle, and somewhere to save state. */
private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner {
    private val registry = LifecycleRegistry(this)
    private val saved = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = saved.savedStateRegistry

    init {
        saved.performAttach()
        saved.performRestore(null)
        registry.currentState = Lifecycle.State.RESUMED
    }

    fun destroy() {
        registry.currentState = Lifecycle.State.DESTROYED
    }
}
