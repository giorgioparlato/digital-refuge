package app.bedtime.service

import android.content.Context
import android.content.Intent
import android.provider.Settings

/** Whether blocking (the accessibility service) is currently on, and the way to its switch. */
object BlockingState {
    fun isOn(context: Context): Boolean = SystemApps.isAccessibilityServiceEnabled(context)

    /** Settings → Accessibility, where blocking is switched back on. */
    fun accessibilityIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
