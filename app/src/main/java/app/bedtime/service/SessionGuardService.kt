package app.bedtime.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
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
 * It notices blocking going off mid-session (from the app's own pause, from Settings, or from the
 * link a banking app offers), records it so the stats show it, and keeps a reminder up until
 * blocking is back on.
 */
class SessionGuardService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var state: ActiveState? = null
    private var blockingOn = true

    /** Set when the user asked for a pause, so the first minute is a countdown and not a telling-off. */
    private var pausedUntil = 0L

    /** So one switch-off is recorded once, not on every settings change. */
    private var recorded = false

    private val accessibilityObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            refresh()
        }
    }

    override fun onCreate() {
        super.onCreate()
        blockingOn = BlockingPause.isBlockingOn(this)
        startForeground(NOTIFICATION_ID, SessionNotifier.build(this, null, blockingOn = blockingOn, pausedUntil = 0L))
        contentResolver.registerContentObserver(
            Settings.Secure.getUriFor(Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            false,
            accessibilityObserver,
        )
        scope.launch {
            Engine.state(this@SessionGuardService).filterNotNull().collect { next ->
                state = next
                if (!next.isActive) stopSelf() else refresh()
            }
        }
        // While blocking is off the reminder is re-posted, so it stays in sight and the countdown moves.
        scope.launch {
            while (true) {
                delay(1_000)
                if (!blockingOn) post()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_PAUSE_STARTING) {
            pausedUntil = System.currentTimeMillis() + BlockingPause.LENGTH_MS
        }
        refresh()
        return START_STICKY
    }

    /** Re-reads whether blocking is on, records a switch-off once, and updates the notification. */
    private fun refresh() {
        val nowOn = BlockingPause.isBlockingOn(this)
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
                pausedUntil = 0L
                Engine.refresh() // Pick the session back up straight away.
            }
        }
        post()
    }

    private fun post() {
        SessionNotifier.post(this, NOTIFICATION_ID, state, blockingOn, pausedUntil)
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
        private const val ACTION_PAUSE_STARTING = "app.bedtime.guard.PAUSE_STARTING"

        /** Starts the guard, or wakes it, whenever a session is running. */
        fun start(context: Context, action: String? = null) {
            val intent = Intent(context, SessionGuardService::class.java).setAction(action)
            runCatching { context.startForegroundService(intent) }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, SessionGuardService::class.java)) }
        }

        /** Called just before blocking is switched off on purpose, so the first minute is a countdown. */
        fun pauseStarting(context: Context) = start(context, ACTION_PAUSE_STARTING)
    }
}
