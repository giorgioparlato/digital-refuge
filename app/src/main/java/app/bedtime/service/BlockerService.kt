package app.bedtime.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import app.bedtime.data.AppSettings
import app.bedtime.data.DndMode
import app.bedtime.data.Repository
import app.bedtime.engine.ActiveState
import app.bedtime.engine.Engine
import app.bedtime.ui.blocked.BlockedActivity
import app.bedtime.ui.lock.LockScreenActivity
import app.bedtime.ui.minimal.MinimalHomeActivity
import app.bedtime.ui.widget.BlockWidget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch

/**
 * Watches which app comes to the foreground and covers it with our own screen when a schedule
 * forbids it. Also drives greyscale, Do Not Disturb, the lock screen, the session notification and
 * session history, since this service is the long-lived part of the app.
 */
class BlockerService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var state: ActiveState? = null
    private var settings = AppSettings()
    private var alwaysAllowed: Set<String> = emptySet()

    /** Windows that float over the current app (shade, keyboards) and don't change what's "in front". */
    private var overlays: Set<String> = emptySet()
    private var lastPackage: String? = null
    private var homeInFront = false
    private var receiverRegistered = false
    private var watchersRegistered = false

    /** Our screens that stay in colour while greyscale is on (if the home style says so). */
    private val colourScreens = setOf(MinimalHomeActivity::class.java.name, LockScreenActivity::class.java.name)

    /** Do Not Disturb switched off from quick settings mid-session: switch it straight back on. */
    private val zenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val current = state ?: return
            val owner = current.active.firstOrNull { it.schedule.dnd != DndMode.OFF || it.schedule.hideNotifications } ?: return
            scope.launch {
                if (DndController.apply(this@BlockerService, current.dnd, current.hideNotifications)) {
                    notice("Do Not Disturb stays on during ${owner.schedule.name}.")
                }
            }
        }
    }

    /** Colour correction switched off mid-session: switch greyscale straight back on. */
    private val greyscaleObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            val owner = state?.active?.firstOrNull { it.schedule.greyscale } ?: return
            scope.launch {
                if (applyGreyscale()) notice("Greyscale stays on during ${owner.schedule.name}.")
            }
        }
    }

    /** Our greyscale mode (Android 15+) switched off by hand mid-session: switch it straight back on. */
    private val modeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val owner = state?.active?.firstOrNull { it.schedule.greyscale } ?: return
            if (homeInFront && settings.homeStyle.keepInColour) return
            if (GreyscaleController.hasPermission(context) || !ModeGreyscale.isUserDeactivation(context, intent)) return
            ModeGreyscale.apply(context, on = true, reassert = true)
            notice("Greyscale stays on during ${owner.schedule.name}.")
        }
    }
    private var modeReceiverRegistered = false

    /** Not cancelled on destroy, so restoring colours and sound can finish after the service stops. */
    private val releaseScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_SCREEN_OFF) {
                showLockScreen()
                return
            }
            Engine.refresh()
            refreshSystemPackages()
            // Catches up if notifications were allowed after the session started.
            SessionNotifier.update(this@BlockerService, state)
            // Re-assert greyscale in case it was switched off from quick settings.
            scope.launch { applyGreyscale() }
        }
    }

    override fun onServiceConnected() {
        refreshSystemPackages()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        receiverRegistered = true
        ContextCompat.registerReceiver(
            this,
            zenReceiver,
            IntentFilter(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        GreyscaleController.observedUris.forEach { contentResolver.registerContentObserver(it, false, greyscaleObserver) }
        watchersRegistered = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            ContextCompat.registerReceiver(
                this,
                modeReceiver,
                IntentFilter(NotificationManager.ACTION_AUTOMATIC_ZEN_RULE_STATUS_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED,
            )
            modeReceiverRegistered = true
        }

        val repo = Repository.get(this)
        scope.launch {
            repo.settings.collect {
                settings = it
                applyGreyscale()
            }
        }
        scope.launch {
            Engine.state(this@BlockerService).filterNotNull().collect { next ->
                state = next
                applyGreyscale()
                DndController.apply(this@BlockerService, next.dnd, next.hideNotifications)
                SessionNotifier.update(this@BlockerService, next)
                BlockWidget.updateAll(this@BlockerService)
                if (next.isActive) {
                    enforce(lastPackage)
                    repo.recordOccurrences(next.active)
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg !in overlays) {
            val home = pkg == packageName && event.className?.toString() in colourScreens
            if (home != homeInFront) {
                homeInFront = home
                scope.launch { applyGreyscale() }
            }
        }
        lastPackage = pkg
        val boundary = state?.nextBoundary
        if (boundary != null && System.currentTimeMillis() >= boundary) Engine.refresh()
        enforce(pkg)
    }

    private suspend fun applyGreyscale(): Boolean =
        GreyscaleController.apply(
            this,
            wanted = state?.greyscale == true,
            pausedForHome = homeInFront && settings.homeStyle.keepInColour,
        )

    /** Screen just went off mid-session: put the session's clock over the lock screen for next time. */
    private fun showLockScreen() {
        if (state?.isActive != true || !settings.homeStyle.lockScreen) return
        runCatching { startActivity(LockScreenActivity.intent(this)) }
    }

    private fun notice(message: String) {
        Toast.makeText(this, message.lowercase(), Toast.LENGTH_SHORT).show()
    }

    private fun refreshSystemPackages() {
        alwaysAllowed = SystemApps.alwaysAllowed(this)
        overlays = SystemApps.keyboards(this) + "com.android.systemui"
    }

    private fun enforce(pkg: String?) {
        val current = state ?: return
        if (pkg == null || !current.isActive || pkg == packageName || pkg in alwaysAllowed) return
        // Always-available apps (maps, rides, authenticators…) are never blocked by any session.
        if (pkg in settings.alwaysAvailable) return
        val minimal = current.minimalAllowlist
        when {
            pkg in current.blocked -> startActivity(BlockedActivity.intent(this, pkg))
            // In minimal mode the stock launcher and recents are "not allowed" too, so Home lands here.
            minimal != null && pkg !in minimal -> startActivity(MinimalHomeActivity.intent(this))
        }
    }

    override fun onInterrupt() = Unit

    /** Switched off (e.g. through the emergency exit): give the phone its normal colours and sounds back. */
    override fun onUnbind(intent: Intent?): Boolean {
        val context = applicationContext
        SessionNotifier.cancel(context)
        releaseScope.launch {
            GreyscaleController.apply(context, wanted = false)
            DndController.apply(context, DndMode.OFF, hide = false)
        }
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (receiverRegistered) unregisterReceiver(receiver)
        if (watchersRegistered) {
            unregisterReceiver(zenReceiver)
            contentResolver.unregisterContentObserver(greyscaleObserver)
        }
        if (modeReceiverRegistered) unregisterReceiver(modeReceiver)
        scope.cancel()
        super.onDestroy()
    }
}
