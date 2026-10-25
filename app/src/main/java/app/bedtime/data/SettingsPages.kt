package app.bedtime.data

import android.content.Intent
import android.provider.Settings

/** One of Android's settings pages the search can open: what it's called, other words for it, and how to open it. */
data class SettingsPage(val name: String, val words: List<String>, val action: String)

/**
 * The settings pages any app may open directly. Android doesn't let other apps search inside its
 * Settings, so this is a list of the common ones, matched by name and a few other words.
 */
object SettingsPages {
    val all: List<SettingsPage> = listOf(
        SettingsPage("Wi-Fi", listOf("wifi", "wi-fi", "internet", "network"), Settings.ACTION_WIFI_SETTINGS),
        SettingsPage("Bluetooth", listOf("bluetooth", "headphones", "pair"), Settings.ACTION_BLUETOOTH_SETTINGS),
        SettingsPage("Mobile data", listOf("mobile", "data", "sim", "cellular", "roaming"), Settings.ACTION_DATA_ROAMING_SETTINGS),
        SettingsPage("Airplane mode", listOf("airplane", "flight", "plane"), Settings.ACTION_AIRPLANE_MODE_SETTINGS),
        SettingsPage("Display", listOf("display", "brightness", "dark", "screen", "font size"), Settings.ACTION_DISPLAY_SETTINGS),
        SettingsPage("Sound", listOf("sound", "volume", "ringtone", "vibrate"), Settings.ACTION_SOUND_SETTINGS),
        SettingsPage("Battery", listOf("battery", "power", "saver"), "android.intent.action.POWER_USAGE_SUMMARY"),
        SettingsPage("Location", listOf("location", "gps"), Settings.ACTION_LOCATION_SOURCE_SETTINGS),
        SettingsPage("Apps", listOf("apps", "applications"), Settings.ACTION_APPLICATION_SETTINGS),
        SettingsPage("Default apps", listOf("default", "browser", "launcher", "home app"), Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
        SettingsPage("Do Not Disturb", listOf("do not disturb", "dnd", "silence", "priority"), "android.settings.ZEN_MODE_SETTINGS"),
        SettingsPage("Storage", listOf("storage", "space", "memory"), Settings.ACTION_INTERNAL_STORAGE_SETTINGS),
        SettingsPage("Date and time", listOf("date", "time", "clock", "time zone"), Settings.ACTION_DATE_SETTINGS),
        SettingsPage("Language", listOf("language", "locale"), Settings.ACTION_LOCALE_SETTINGS),
        SettingsPage("Keyboard", listOf("keyboard", "input"), Settings.ACTION_INPUT_METHOD_SETTINGS),
        SettingsPage("Security", listOf("security", "lock screen", "fingerprint", "pin", "password"), Settings.ACTION_SECURITY_SETTINGS),
        SettingsPage("Accessibility", listOf("accessibility"), Settings.ACTION_ACCESSIBILITY_SETTINGS),
        SettingsPage("NFC", listOf("nfc", "tap to pay", "contactless"), Settings.ACTION_NFC_SETTINGS),
        SettingsPage("VPN", listOf("vpn"), Settings.ACTION_VPN_SETTINGS),
        SettingsPage("Cast", listOf("cast", "screen mirroring"), Settings.ACTION_CAST_SETTINGS),
        SettingsPage("Developer options", listOf("developer"), Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS),
        SettingsPage("About phone", listOf("about", "phone info", "android version"), Settings.ACTION_DEVICE_INFO_SETTINGS),
        SettingsPage("All settings", listOf("settings"), Settings.ACTION_SETTINGS),
    )

    /** Pages whose name or words begin with what's typed (three letters or more), best first, at most [limit]. */
    fun match(query: String, limit: Int = 3): List<SettingsPage> {
        val q = query.trim().lowercase()
        if (q.length < 3) return emptyList()
        return all.filter { page -> page.name.lowercase().startsWith(q) || page.words.any { it.startsWith(q) || q.startsWith("$it ") } }
            .sortedBy { if (it.name.lowercase().startsWith(q)) 0 else 1 }
            .take(limit)
    }

    fun intent(page: SettingsPage): Intent = Intent(page.action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
