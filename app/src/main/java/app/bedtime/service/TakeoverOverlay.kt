package app.bedtime.service

import app.bedtime.data.Problems

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import app.bedtime.R

/**
 * A full-screen wall shown by [SessionGuardService] while blocking is switched off during a session,
 * so it can't quietly stay off. Drawn with "display over other apps" so it covers whatever's in front,
 * even whatever is in front. Two ways out: switch blocking back on, or take another break.
 */
@SuppressLint("StaticFieldLeak", "InflateParams", "SetTextI18n")
object TakeoverOverlay {
    private var view: View? = null

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
        val v = attach(app) ?: return
        forSession = false
        v.findViewById<TextView>(R.id.takeover_subtitle).text =
            "it's been off for $minutes ${if (minutes == 1) "minute" else "minutes"}. your limits and blocks only work while it's on.".lowercase()
        v.findViewById<TextView>(R.id.takeover_snooze).apply {
            visibility = View.VISIBLE
            text = "later"
            setOnClickListener {
                OffCover.schedule(app, minutes)
                hide(app)
            }
        }
        v.findViewById<TextView>(R.id.takeover_enable).setOnClickListener {
            runCatching { app.startActivity(BlockingState.accessibilityIntent()) }
            // If it isn't switched on after all, the cover comes back as after "later".
            OffCover.schedule(app, minutes)
            hide(app)
        }
    }

    /** The wall's view, added to the screen if it isn't there yet; null if it can't be drawn. */
    private fun attach(app: Context): View? {
        if (!Settings.canDrawOverlays(app)) return null
        val wm = app.getSystemService(WindowManager::class.java) ?: return null
        if (view == null) {
            val v = LayoutInflater.from(app).inflate(R.layout.overlay_takeover, null)
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.OPAQUE,
            )
            Problems.attempt("drawing the full-screen reminder") { wm.addView(v, params) }?.let { view = v }
        }
        return view
    }

    /** Shows the wall (or updates it) for [scheduleName]; [allowSnooze] offers another break. */
    @Synchronized
    fun show(context: Context, scheduleName: String, breakMinutes: Int, allowSnooze: Boolean) {
        val app = context.applicationContext
        forSession = true
        if (!Settings.canDrawOverlays(app)) return
        val wm = app.getSystemService(WindowManager::class.java) ?: return
        if (view == null) {
            val v = LayoutInflater.from(app).inflate(R.layout.overlay_takeover, null)
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.OPAQUE,
            )
            Problems.attempt("drawing the full-screen reminder") { wm.addView(v, params) }?.let { view = v }
        }
        val v = view ?: return
        v.findViewById<TextView>(R.id.takeover_subtitle).text =
            "$scheduleName is still running. switch blocking back on to carry on.".lowercase()
        v.findViewById<TextView>(R.id.takeover_snooze).apply {
            visibility = if (allowSnooze) View.VISIBLE else View.GONE
            text = "take $breakMinutes more ${if (breakMinutes == 1) "minute" else "minutes"}"
            setOnClickListener {
                BlockingState.snooze(breakMinutes)
                hide(app)
            }
        }
        v.findViewById<TextView>(R.id.takeover_enable).setOnClickListener {
            // Let go of the wall for a moment so the accessibility screen isn't covered by it.
            BlockingState.suppressOverlay(60_000)
            runCatching { app.startActivity(BlockingState.accessibilityIntent()) }
            hide(app)
        }
    }

    @Synchronized
    fun hide(context: Context) {
        val v = view ?: return
        val wm = context.applicationContext.getSystemService(WindowManager::class.java)
        runCatching { wm?.removeView(v) }
        view = null
    }
}
