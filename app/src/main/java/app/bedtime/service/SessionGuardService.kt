package app.bedtime.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import app.bedtime.data.AppSettings
import app.bedtime.data.DndMode
import app.bedtime.data.Repository
import app.bedtime.engine.ActiveState
import app.bedtime.engine.Engine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Runs for as long as a session does, separately from [BlockerService], so that it survives the
 * accessibility service being switched off — which is exactly the moment it exists for.
 *
 * If blocking is off mid-session (a banking pause, or safe mode) it records it, keeps Do Not Disturb
 * and greyscale going so only apps are unblocked, and — once any granted break runs out — takes over
 * the screen with [TakeoverOverlay] until blocking is switched back on.
 */
class SessionGuardService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Not cancelled on destroy, so giving the phone its sound and colour back can finish. */
    private val releaseScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var state: ActiveState? = null
    private var settings = AppSettings()
    private var blockingOn = true

    /** So one switch-off is recorded once, not on every settings change. */
    private var recorded = false

    private val accessibilityObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refresh()
        }
    }

    override fun onCreate() {
        super.onCreate()
        blockingOn = BlockingState.isOn(this)
        startForeground(NOTIFICATION_ID, SessionNotifier.build(this, null, blockingOn = blockingOn, breakMs = 0))
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            false,
            accessibilityObserver,
        )
        scope.launch { Repository.get(this@SessionGuardService).settings.collect { settings = it } }
        scope.launch {
            Engine.state(this@SessionGuardService).filterNotNull().collect { next ->
                state = next
                if (!next.isActive) {
                    if (!blockingOn) release()
                    TakeoverOverlay.hide(this@SessionGuardService)
                    stopSelf()
                } else {
                    refresh()
                }
            }
        }
        // Keep the reminder, break countdown and takeover current, and re-assert quiet/grey.
        scope.launch {
            var ticks = 0
            while (true) {
                delay(1_000)
                if (!blockingOn) {
                    updateAlert()
                    if (++ticks % 15 == 0) holdQuietAndGrey()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        refresh()
        return START_STICKY
    }

    /** Re-reads whether blocking is on, records a switch-off once, and updates everything. */
    private fun refresh() {
        val nowOn = BlockingState.isOn(this)
        val wasOn = blockingOn
        blockingOn = nowOn
        when {
            wasOn && !nowOn -> {
                if (!recorded) {
                    recorded = true
                    val occurrence = state?.active?.firstOrNull()
                    if (occurrence != null) {
                        scope.launch { Repository.get(this@SessionGuardService).recordPause(occurrence, System.currentTimeMillis()) }
                    }
                }
            }
            !wasOn && nowOn -> {
                recorded = false
                BlockingState.clearBreak()
                Engine.refresh() // Pick the session back up straight away.
            }
        }
        holdQuietAndGrey()
        updateAlert()
    }

    /** Shows or hides the full-screen takeover and updates the ongoing notification for the phase. */
    private fun updateAlert() {
        val current = state
        val takeover = !blockingOn &&
            current?.isActive == true &&
            settings.fullScreenAlert &&
            !BlockingState.isOnBreak() &&
            !BlockingState.isOverlaySuppressed()
        if (takeover) {
            val schedule = current.active.maxByOrNull { it.end }?.schedule
            TakeoverOverlay.show(this, schedule?.name ?: "your session", schedule?.breakMinutes ?: 1, schedule?.pauseEnabled == true)
        } else {
            TakeoverOverlay.hide(this)
        }
        val breakMs = if (!blockingOn && BlockingState.isOnBreak()) BlockingState.breakRemainingMs() else 0L
        SessionNotifier.post(this, NOTIFICATION_ID, state, blockingOn, breakMs)
    }

    /**
     * With the accessibility service off, keep the session's Do Not Disturb, held notifications and
     * greyscale (Android 15+ modes) going from here, so switching blocking off only unblocks apps.
     */
    private fun holdQuietAndGrey() {
        val current = state ?: return
        if (blockingOn || !current.isActive) return
        scope.launch {
            DndController.apply(this@SessionGuardService, current.dnd, current.hideNotifications)
            GreyscaleController.apply(this@SessionGuardService, wanted = current.greyscale)
        }
    }

    /** The session ended while blocking was off: give the phone its normal sound and colours back. */
    private fun release() {
        val context = applicationContext
        releaseScope.launch {
            GreyscaleController.apply(context, wanted = false)
            DndController.apply(context, DndMode.OFF, hide = false)
        }
    }

    override fun onDestroy() {
        contentResolver.unregisterContentObserver(accessibilityObserver)
        TakeoverOverlay.hide(this)
        scope.cancel()
        SessionNotifier.cancel(this)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val NOTIFICATION_ID = 1

        /** Starts the guard, or wakes it, whenever a session is running. */
        fun start(context: Context) {
            runCatching { context.startForegroundService(Intent(context, SessionGuardService::class.java)) }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, SessionGuardService::class.java)) }
        }
    }
}
