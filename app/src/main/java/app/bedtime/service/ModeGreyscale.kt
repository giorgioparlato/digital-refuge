package app.bedtime.service

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.service.notification.Condition
import android.service.notification.ZenDeviceEffects
import androidx.annotation.RequiresApi
import app.bedtime.MainActivity

/**
 * Greyscale without a computer on Android 15+. Android's modes (Settings → Modes: Do Not Disturb,
 * Bedtime…) can switch the screen to greyscale, and apps with Do Not Disturb access may create their
 * own. This keeps one "digital refuge greyscale" mode that only does greyscale, active while a session
 * wants it. (The Bedtime mode type itself is reserved for Digital Wellbeing.)
 */
object ModeGreyscale {
    private val CONDITION: Uri = Uri.parse("condition://app.bedtime/greyscale")

    /** Until when our own rule-state writes should be ignored, so re-asserting doesn't loop on itself. */
    @Volatile
    private var selfChangeUntil = 0L

    /** True while a change we just made is still echoing back as a status broadcast. */
    fun isSelfChange(): Boolean = SystemClock.elapsedRealtime() < selfChangeUntil

    fun isSupported(context: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM &&
            context.getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted

    /**
     * Switches the mode on or off. [reassert] turns it off and on again, which is how an app takes back
     * a mode the user switched off by hand.
     */
    @Synchronized
    fun apply(context: Context, on: Boolean, reassert: Boolean = false) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM || !isSupported(context)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        runCatching {
            val id = ruleId(context, manager, create = on) ?: return
            // Our own false→true toggle broadcasts a "deactivated" status; ignore it for a moment so
            // it isn't mistaken for the user switching greyscale off, which would re-assert forever.
            selfChangeUntil = SystemClock.elapsedRealtime() + 2_000
            if (on && reassert) manager.setAutomaticZenRuleState(id, condition(false))
            manager.setAutomaticZenRuleState(id, condition(on))
        }
    }

    /** True for the broadcast Android sends when our mode is switched off by hand. */
    fun isUserDeactivation(context: Context, intent: Intent): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.VANILLA_ICE_CREAM || !isSupported(context)) return false
        val status = intent.getIntExtra(NotificationManager.EXTRA_AUTOMATIC_ZEN_RULE_STATUS, -1)
        val id = intent.getStringExtra(NotificationManager.EXTRA_AUTOMATIC_ZEN_RULE_ID) ?: return false
        val manager = context.getSystemService(NotificationManager::class.java)
        return status == NotificationManager.AUTOMATIC_RULE_STATUS_DEACTIVATED && id == ruleId(context, manager, create = false)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    private fun ruleId(context: Context, manager: NotificationManager, create: Boolean): String? {
        manager.automaticZenRules.entries.firstOrNull { it.value.conditionId == CONDITION }?.let { return it.key }
        if (!create) return null
        val rule = AutomaticZenRule.Builder("digital refuge greyscale", CONDITION)
            .setType(AutomaticZenRule.TYPE_OTHER)
            .setConfigurationActivity(ComponentName(context, MainActivity::class.java))
            .setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            .setDeviceEffects(ZenDeviceEffects.Builder().setShouldDisplayGrayscale(true).build())
            .setTriggerDescription("during digital refuge sessions")
            .setManualInvocationAllowed(false)
            .build()
        return manager.addAutomaticZenRule(rule)
    }

    private fun condition(on: Boolean) =
        Condition(CONDITION, "digital refuge session", if (on) Condition.STATE_TRUE else Condition.STATE_FALSE)
}
