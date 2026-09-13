package app.bedtime.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.provider.Telephony
import android.telecom.TelecomManager
import android.view.inputmethod.InputMethodManager

object SystemApps {
    /** Never blocked: system UI, permission dialogs and emergency calling. */
    private val base = setOf(
        "android",
        "com.android.systemui",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.android.phone",
        "com.android.server.telecom",
        "com.android.emergency",
        "com.google.android.apps.safetyhub",
    )

    /** Pre-filled "always available" apps when installed: maps, rides and authenticators. */
    val ALWAYS_AVAILABLE_SUGGESTIONS = listOf(
        "com.google.android.apps.maps",
        "com.waze",
        "com.ubercab",
        "ee.mtakso.client", // Bolt
        "me.lyft.android",
        "com.google.android.apps.authenticator2",
        "com.azure.authenticator",
        "com.authy.authy",
    )

    fun alwaysAllowed(context: Context): Set<String> =
        base + context.packageName + keyboards(context) + listOfNotNull(dialerPackage(context))

    fun keyboards(context: Context): Set<String> =
        context.getSystemService(InputMethodManager::class.java)
            ?.enabledInputMethodList?.map { it.packageName }.orEmpty().toSet()

    fun dialerPackage(context: Context): String? =
        context.getSystemService(TelecomManager::class.java)?.defaultDialerPackage

    fun smsPackage(context: Context): String? = Telephony.Sms.getDefaultSmsPackage(context)

    fun settingsPackage(context: Context): String? =
        context.packageManager.resolveActivity(Intent(Settings.ACTION_SETTINGS), 0)?.activityInfo?.packageName

    /** Settings, Phone and Messages: pre-selected for minimal mode so there's always a way to call, text and reach Settings. */
    fun essentials(context: Context): Set<String> =
        listOfNotNull(settingsPackage(context), dialerPackage(context), smsPackage(context)).toSet()

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val expected = ComponentName(context, BlockerService::class.java)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == expected }
    }
}
