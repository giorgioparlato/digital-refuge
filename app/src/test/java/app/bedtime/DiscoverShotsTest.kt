package app.bedtime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.bedtime.ui.onboarding.DictionaryPage
import app.bedtime.ui.onboarding.HabitsPage
import app.bedtime.ui.onboarding.LockScreenPage
import app.bedtime.ui.onboarding.QuickNotesPage
import app.bedtime.ui.onboarding.SessionPage
import app.bedtime.ui.onboarding.WidgetsPage
import app.bedtime.ui.theme.BedtimeTheme
import app.bedtime.ui.theme.Obsidian
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

/** Temporary: frames from the discover-features demos, for a look. */
class DiscoverShotsTest {
    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 3000), theme = "android:Theme.Material.NoActionBar", showSystemUi = false)

    private fun frames(loopMs: Int, vararg at: Float, page: @Composable () -> Unit) = at.forEach { t ->
        paparazzi.snapshot("t${(t * 100).toInt()}") {
            androidx.compose.runtime.CompositionLocalProvider(app.bedtime.ui.onboarding.LocalDemoTime provides t) {
                BedtimeTheme {
                    Column(Modifier.fillMaxSize().background(Obsidian.colors.bgPrimary).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { page() }
                }
            }
        }
    }

    @Test fun session() = frames(14_000, 0.08f, 0.30f, 0.62f, 0.78f) { SessionPage() }
    @Test fun lock() = frames(8_000, 0.2f, 0.6f) { LockScreenPage() }
    @Test fun widgets() = frames(20_000, 0.1f, 0.4f, 0.62f, 0.88f) { WidgetsPage() }
    @Test fun quickNote() = frames(8_000, 0.66f) { QuickNotesPage() }
    @Test fun habits() = frames(8_000, 0.7f) { HabitsPage() }
    @Test fun dictionary() = frames(10_000, 0.7f) { DictionaryPage() }

    @Test fun wholePage() = paparazzi.snapshot {
        androidx.compose.runtime.CompositionLocalProvider(app.bedtime.ui.onboarding.LocalDemoTime provides 0.5f) {
            BedtimeTheme { app.bedtime.ui.onboarding.DiscoverScreen(onBack = {}, onOpen = {}, onTab = {}, startAt = "HOME_SCREEN") }
        }
    }
}
