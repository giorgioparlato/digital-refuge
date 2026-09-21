package app.bedtime.service

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.net.Uri
import android.provider.Settings

/**
 * Whether blocking (the accessibility service) is on, and the deliberate "pause for a banking or ID
 * app" that switches it off for a set break. There's no way to switch it back on ourselves, so once
 * the break runs out [SessionGuardService] takes over the screen until the user re-enables it.
 */
object BlockingState {
    /** Set while [BlockerService] is connected, so a pause can switch it off from the UI. */
    @Volatile
    internal var service: BlockerService? = null

    /** When the current break ends (elapsed-realtime millis); 0 means no break. */
    @Volatile
    private var breakUntil = 0L

    /** Until when the takeover wall stays down, e.g. while the user is on the accessibility screen. */
    @Volatile
    private var overlaySuppressUntil = 0L

    fun isOn(context: Context): Boolean = SystemApps.isAccessibilityServiceEnabled(context)

    fun isOnBreak(): Boolean = SystemClock.elapsedRealtime() < breakUntil

    fun breakRemainingMs(): Long = (breakUntil - SystemClock.elapsedRealtime()).coerceAtLeast(0L)

    /**
     * Starts a break of [minutes] and switches blocking off. Returns false if the service wasn't
     * running, in which case there's nothing to pause.
     *
     * A paused session can't be made tamper-proof: once blocking is off, any permission this app
     * holds can be withdrawn. A block that shouldn't have that door sets `pauseEnabled = false`.
     */
    fun pause(minutes: Int): Boolean {
        val running = service ?: return false
        breakUntil = SystemClock.elapsedRealtime() + minutes.coerceAtLeast(1) * 60_000L
        running.disableSelf()
        return true
    }

    /** Extends the break by [minutes] (the "take X more minutes" button on the takeover). */
    fun snooze(minutes: Int) {
        breakUntil = SystemClock.elapsedRealtime() + minutes.coerceAtLeast(1) * 60_000L
    }

    /** Cleared when blocking comes back on, so the next switch-off isn't treated as still on break. */
    fun clearBreak() {
        breakUntil = 0L
        overlaySuppressUntil = 0L
    }

    fun suppressOverlay(ms: Long) {
        overlaySuppressUntil = SystemClock.elapsedRealtime() + ms
    }

    fun isOverlaySuppressed(): Boolean = SystemClock.elapsedRealtime() < overlaySuppressUntil

    /** Settings → Accessibility, where blocking is switched back on. */
    fun accessibilityIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Settings → Display over other apps, for putting the takeover back. */
    fun overlayIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
