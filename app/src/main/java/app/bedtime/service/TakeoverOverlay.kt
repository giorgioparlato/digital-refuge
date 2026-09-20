package app.bedtime.service

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
 * even a banking app. Two ways out: switch blocking back on, or take another break.
 */
@SuppressLint("StaticFieldLeak", "InflateParams", "SetTextI18n")
object TakeoverOverlay {
    private var view: View? = null

    fun isShowing(): Boolean = view != null

    fun canShow(context: Context): Boolean = Settings.canDrawOverlays(context)

    /** Shows the wall (or updates it) for [scheduleName]; [allowSnooze] offers another break. */
    @Synchronized
    fun show(context: Context, scheduleName: String, breakMinutes: Int, allowSnooze: Boolean) {
        val app = context.applicationContext
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
            runCatching { wm.addView(v, params) }.onSuccess { view = v }
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
