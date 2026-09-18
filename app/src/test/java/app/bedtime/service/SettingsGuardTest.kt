package app.bedtime.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsGuardTest {
    private val settings = "com.android.settings"
    private val guarded = setOf(settings) + SettingsGuard.INSTALLER_PACKAGES
    private val names = setOf("digital refuge blocker", "digital refuge")

    private fun matches(pkg: String, vararg texts: String) = SettingsGuard.matches(pkg, texts.toList(), guarded, names)

    @Test
    fun ourAccessibilityPageIsCovered() {
        assertTrue(matches(settings, "digital refuge blocker"))
    }

    @Test
    fun theStopDialogIsCovered() {
        assertTrue(matches(settings, "Stop digital refuge blocker?"))
    }

    @Test
    fun theUninstallPromptIsCovered() {
        assertTrue(matches("com.google.android.packageinstaller", "digital refuge", "Do you want to uninstall this app?"))
    }

    @Test
    fun matchingIgnoresCase() {
        assertTrue(matches(settings, "Digital Refuge Blocker"))
    }

    @Test
    fun otherSettingsPagesAreLeftAlone() {
        assertFalse(matches(settings, "Wi-Fi"))
        assertFalse(matches(settings, "Accessibility"))
        assertFalse(matches(settings, "Display"))
    }

    @Test
    fun ourNameElsewhereIsLeftAlone() {
        // A message or a note that happens to mention the app mustn't be covered.
        assertFalse(matches("com.google.android.apps.messaging", "digital refuge is great"))
    }
}
