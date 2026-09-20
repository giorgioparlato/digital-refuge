package app.bedtime.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
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
 * During a session, switching blocking off in Settings is covered by [BlockerService] and can only
 * be done deliberately (through the unlock steps, or safe mode). If it does go off, this service
 * notices in seconds, records it for the stats, keeps a reminder up until it's back, and keeps the
 * session's Do Not Disturb and greyscale going so that only apps are unblocked.
 */
class SessionGuardService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Not cancelled on destroy, so giving the phone its sound and colour back can finish. */
    private val releaseScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var state: ActiveState? = null
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
        startForeground(NOTIFICATION_ID, SessionNotifier.build(this, null, blockingOn = blockingOn))
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            false,
            accessibilityObserver,
        )
        scope.launch {
            Engine.state(this@SessionGuardService).filterNotNull().collect { next ->
                state = next
                if (!next.isActive) {
                    // Blocking was off, so nobody else will restore Do Not Disturb and colours.
                    if (!blockingOn) release()
                    stopSelf()
                } else {
                    refresh()
                }
            }
        }
        // While blocking is off the reminder is re-posted so it stays in sight, and every 15 seconds
        // Do Not Disturb and greyscale are re-asserted in case they were switched off.
        scope.launch {
            var ticks = 0
            while (true) {
                delay(1_000)
                if (!blockingOn) {
                    post()
                    if (++ticks % 15 == 0) holdQuietAndGrey()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        refresh()
        return START_STICKY
    }

    /** Re-reads whether blocking is on, records a switch-off once, and updates the notification. */
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
                Engine.refresh() // Pick the session back up straight away.
            }
        }
        holdQuietAndGrey()
        post()
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

    private fun post() {
        SessionNotifier.post(this, NOTIFICATION_ID, state, blockingOn)
    }

    override fun onDestroy() {
        contentResolver.unregisterContentObserver(accessibilityObserver)
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
