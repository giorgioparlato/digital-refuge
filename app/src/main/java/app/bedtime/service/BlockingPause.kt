package app.bedtime.service

import android.content.Context
import android.content.Intent
import android.provider.Settings

/**
 * Switching blocking off for a moment, for banking and ID apps that refuse to run beside an
 * accessibility service (Sweden's BankID, for one). There is no way back on our own: only the user
 * can re-enable an accessibility service, unless the app holds WRITE_SECURE_SETTINGS. So the pause
 * is short, reminded about by [SessionGuardService], and counted in the stats like an early unlock.
 */
object BlockingPause {
    /** How long a deliberate pause lasts before the reminder turns urgent. */
    const val LENGTH_MS = 60_000L

    /** Set while [BlockerService] is connected, so the pause can switch it off from the UI. */
    @Volatile
    internal var service: BlockerService? = null

    fun isBlockingOn(context: Context): Boolean = SystemApps.isAccessibilityServiceEnabled(context)

    /**
     * Starts a pause: tells the guard to expect it, then switches the accessibility service off.
     * Returns false if the service wasn't running, in which case there is nothing to pause.
     */
    fun start(context: Context): Boolean {
        val running = service ?: return false
        SessionGuardService.pauseStarting(context)
        running.disableSelf()
        return true
    }

    /** Settings → Accessibility, where blocking is switched back on. */
    fun settingsIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
