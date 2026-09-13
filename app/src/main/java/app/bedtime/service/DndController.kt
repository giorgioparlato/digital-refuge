package app.bedtime.service

import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import app.bedtime.data.DndMode
import app.bedtime.data.Repository
import app.bedtime.data.SavedZen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Do Not Disturb for sessions. "Hide notifications" uses Do Not Disturb's visual suppression:
 * filtered notifications stay out of the shade and status bar, and Android shows them again as soon
 * as Do Not Disturb turns off, whether the session ended or was unlocked early. Media and alarms
 * are never muted.
 */
object DndController {
    private val mutex = Mutex()

    /** Hide setting last written to the policy, so re-asserting the filter doesn't rewrite the policy each time. */
    @Volatile
    private var lastAppliedHide: Boolean? = null

    @Suppress("DEPRECATION")
    private val hideAllEffects: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        NotificationManager.Policy.SUPPRESSED_EFFECT_PEEK or
            NotificationManager.Policy.SUPPRESSED_EFFECT_STATUS_BAR or
            NotificationManager.Policy.SUPPRESSED_EFFECT_BADGE or
            NotificationManager.Policy.SUPPRESSED_EFFECT_AMBIENT or
            NotificationManager.Policy.SUPPRESSED_EFFECT_NOTIFICATION_LIST or
            NotificationManager.Policy.SUPPRESSED_EFFECT_FULL_SCREEN_INTENT or
            NotificationManager.Policy.SUPPRESSED_EFFECT_LIGHTS
    } else {
        NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_OFF or NotificationManager.Policy.SUPPRESSED_EFFECT_SCREEN_ON
    }

    /**
     * Sessions never mute media or alarms. "Silence" uses Android's alarms-only mode, where music,
     * videos and games keep playing; never INTERRUPTION_FILTER_NONE, which mutes all sound.
     */
    internal fun sessionFilter(mode: DndMode): Int =
        if (mode == DndMode.SILENCE) NotificationManager.INTERRUPTION_FILTER_ALARMS else NotificationManager.INTERRUPTION_FILTER_PRIORITY

    /** "Priority only" always lets media and alarms through, whatever the user's own DND categories are. */
    // Below API 28 these bits don't exist and are ignored; priority mode there always allows media and alarms.
    @SuppressLint("InlinedApi")
    internal fun keepMediaAndAlarms(categories: Int): Int =
        categories or NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA or NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS

    fun hasAccess(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java)?.isNotificationPolicyAccessGranted == true

    /**
     * Applies the combined needs of all active sessions; restores the user's settings when none need
     * it. Returns true if Do Not Disturb had been switched off during a session and was turned back on.
     */
    suspend fun apply(context: Context, mode: DndMode, hide: Boolean): Boolean {
        if (!hasAccess(context)) return false
        return mutex.withLock { applyLocked(context, mode, hide) }
    }

    private suspend fun applyLocked(context: Context, mode: DndMode, hide: Boolean): Boolean {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return false
        val repo = Repository.get(context)
        val saved = repo.runtime.first().savedZen
        try {
            if (mode == DndMode.OFF && !hide) {
                if (saved != null) {
                    nm.notificationPolicy = saved.toPolicy(saved.categories, saved.suppressedEffects)
                    nm.setInterruptionFilter(saved.filter)
                    repo.updateRuntime { it.copy(savedZen = null) }
                    lastAppliedHide = null
                }
                return false
            }
            val base = saved ?: snapshot(nm).also { snap -> repo.updateRuntime { it.copy(savedZen = snap) } }
            if (saved == null || lastAppliedHide != hide) {
                nm.notificationPolicy = base.toPolicy(keepMediaAndAlarms(base.categories), if (hide) hideAllEffects else base.suppressedEffects)
                lastAppliedHide = hide
            }
            val filter = sessionFilter(mode)
            if (nm.currentInterruptionFilter != filter) {
                nm.setInterruptionFilter(filter)
                // We already owned DND, so someone switched it away mid-session.
                return saved != null
            }
        } catch (_: SecurityException) {
            // Access was revoked between the check and the change.
        }
        return false
    }

    private fun snapshot(nm: NotificationManager): SavedZen {
        val policy = nm.notificationPolicy
        val filter = nm.currentInterruptionFilter
            .takeIf { it != NotificationManager.INTERRUPTION_FILTER_UNKNOWN } ?: NotificationManager.INTERRUPTION_FILTER_ALL
        return SavedZen(
            filter = filter,
            categories = policy.priorityCategories,
            callSenders = policy.priorityCallSenders,
            messageSenders = policy.priorityMessageSenders,
            suppressedEffects = policy.suppressedVisualEffects,
            conversationSenders = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) policy.priorityConversationSenders else -1,
        )
    }

    // Sender values round-trip from the system's own Policy, so they are always valid constants.
    @SuppressLint("WrongConstant")
    private fun SavedZen.toPolicy(priorityCategories: Int, suppressed: Int): NotificationManager.Policy =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && conversationSenders >= 0) {
            NotificationManager.Policy(priorityCategories, callSenders, messageSenders, suppressed, conversationSenders)
        } else {
            NotificationManager.Policy(priorityCategories, callSenders, messageSenders, suppressed)
        }
}
