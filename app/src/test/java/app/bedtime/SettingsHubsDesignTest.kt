package app.bedtime

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.OptionRow
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.SectionLabel
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.BedtimeTheme
import app.bedtime.ui.theme.Obsidian
import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import org.junit.Rule
import org.junit.Test

/**
 * Settings by place, every option with one home. A: one row per place, each a page of its own.
 * B: the two screens are pages, the rest stays open on the first page. Both share the screen pages.
 *
 * Throwaway. Delete once the real page is built.
 */
class SettingsHubsDesignTest {
    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_5.copy(screenHeight = 2700),
        theme = "android:Theme.Material.NoActionBar",
        showSystemUi = false,
    )

    private fun shot(content: @Composable () -> Unit) {
        paparazzi.snapshot("dark") { BedtimeTheme(darkTheme = true) { content() } }
    }

    @Composable
    private fun Page(title: String, subtitle: String? = null, back: Boolean = false, content: @Composable () -> Unit) {
        val c = Obsidian.colors
        Column(Modifier.fillMaxSize().background(c.bgPrimary)) {
            ObsidianTopBar(title, onBack = if (back) ({}) else null, subtitle = subtitle)
            Column(Modifier.padding(horizontal = PageGutter)) { content() }
        }
    }

    @Composable
    private fun Search() {
        val c = Obsidian.colors
        Row(
            Modifier.fillMaxWidth().padding(top = 14.dp, bottom = 4.dp).clip(RoundedCornerShape(14.dp)).background(c.bgSecondary)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.Search, contentDescription = null, tint = c.textFaint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Find a setting: keyword, weather, colour…", style = MaterialTheme.typography.bodyLarge, color = c.textFaint)
        }
    }

    @Composable
    private fun R(icon: ImageVector, title: String, description: String, last: Boolean = false, trailing: @Composable () -> Unit = { Chevron() }) {
        Column {
            OptionRow(icon, title, description = description, trailing = trailing)
            if (!last) HorizontalDivider(Modifier.padding(start = 52.dp), color = Obsidian.colors.border.copy(alpha = 0.5f))
        }
    }

    @Composable
    private fun Footer() {
        Spacer(Modifier.height(18.dp))
        Text("digital refuge 0.9.20 · everything stays on this phone", style = MaterialTheme.typography.bodySmall, color = Obsidian.colors.textFaint)
    }

    // ---------------------------------------------------------------- A: one row per place

    @Test
    fun aTopLevel() = shot {
        Page("settings", "set up once, then left alone") {
            Search()
            R(Icons.Default.Build, "Permissions & setup", "All set", last = true)
            Spacer(Modifier.height(14.dp))
            R(Icons.Default.Home, "Home screen", "Your launcher · look, widgets, gestures, the app list")
            R(Icons.Default.Lock, "Lock screen", "In sessions · look, widgets, notifications, gestures")
            R(Icons.Default.Search, "Search", "What it offers, keywords, contacts, folders")
            R(BedtimeIcons.Grid, "Apps", "Groups · always available · distracting")
            R(Icons.Default.Edit, "Notes", "Where quick notes go")
            R(BedtimeIcons.Block, "Cover messages", "What a blocked app's cover says")
            R(Icons.Default.Star, "Shortcuts", "Home-screen widget and quick-settings tile")
            R(Icons.Default.Info, "About & backup", "How it works · the intro · export and import", last = true)
            Footer()
        }
    }

    // ---------------------------------------------------------------- B: screens as pages, the rest open

    @Test
    fun bTopLevel() = shot {
        Page("settings", "set up once, then left alone") {
            Search()
            R(Icons.Default.Build, "Permissions & setup", "All set", last = true)

            SectionLabel("your screens")
            R(Icons.Default.Home, "Home screen", "Your launcher · look, widgets, gestures, the app list")
            R(Icons.Default.Lock, "Lock screen", "In sessions · look, widgets, notifications, gestures", last = true)

            SectionLabel("search & notes")
            R(Icons.Default.Search, "Search", "What it offers, keywords, contacts, folders")
            R(Icons.Default.Edit, "Notes & quick notes", "Where quick notes go", last = true)

            SectionLabel("your apps")
            R(BedtimeIcons.Grid, "App groups", "2 groups")
            R(Icons.Default.Warning, "Always-available apps", "3 apps, never blocked, whichever session is on")
            R(BedtimeIcons.Hourglass, "Distracting apps", "4 apps · what counts as a distracted hour")
            R(BedtimeIcons.Block, "Cover messages", "What a blocked app's cover says", last = true)

            SectionLabel("more")
            R(Icons.Default.Star, "Shortcuts", "Home-screen widget and quick-settings tile")
            R(Icons.Default.Info, "About & backup", "How it works · the intro · export and import", last = true)
            Footer()
        }
    }

    // ---------------------------------------------------------------- the pages both share

    @Test
    fun hubHomeScreen() = shot {
        Page("home screen", back = true) {
            Text(
                "Refuge's home screen takes over in sessions with minimal mode. It can be your everyday launcher too.",
                style = MaterialTheme.typography.bodyMedium,
                color = Obsidian.colors.textMuted,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            R(Icons.Default.Home, "Everyday launcher", "Refuge's home screen all the time") { ObsidianToggle(true, {}) }
            R(Icons.Default.Edit, "Look", "Colours, sizes and fonts")
            R(BedtimeIcons.Grid, "Widgets", "Greeting, clock, date, weather, quote")
            R(Icons.Default.Refresh, "Gestures", "Swipe down: notifications · double tap: nothing")
            R(Icons.Default.Star, "The app list", "Favourites, hidden apps, your own names, keywords", last = true)
        }
    }

    @Test
    fun hubLockScreen() = shot {
        Page("lock screen", back = true) {
            Text(
                "Over Android's own lock screen: the clock and your widgets. A tap or a swipe from the bottom unlocks.",
                style = MaterialTheme.typography.bodyMedium,
                color = Obsidian.colors.textMuted,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            R(Icons.Default.Lock, "In sessions", "While a schedule or block runs") { ObsidianToggle(true, {}) }
            R(Icons.Default.Lock, "Outside sessions too", "All the time, as your launcher's") { ObsidianToggle(false, {}) }
            R(Icons.Default.Edit, "Look", "Its own colours, sizes and fonts")
            R(BedtimeIcons.Grid, "Widgets", "Clock, date, next event, quote")
            R(Icons.Default.Notifications, "Notifications", "From WhatsApp, Signal and 2 people")
            R(Icons.Default.KeyboardArrowUp, "Gestures", "Swipe up: camera · double tap: screen off", last = true)
        }
    }

    @Test
    fun appListPage() = shot {
        Page("the app list", back = true) {
            Text(
                "On your home screen and in its search. Holding an app on the home screen changes these too.",
                style = MaterialTheme.typography.bodyMedium,
                color = Obsidian.colors.textMuted,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            R(Icons.Default.Star, "Favourites", "Kindle, Maps, Spotify · in your order")
            R(Icons.Default.Clear, "Hidden apps", "2 apps left out of the lists and search")
            R(Icons.Default.Edit, "Your own names", "1 app renamed")
            R(Icons.Default.Search, "Keywords for apps", "\"k\" opens Kindle", last = true)
            SectionLabel("sizes")
            R(BedtimeIcons.Grid, "App icons", "Small icons next to app names") { ObsidianToggle(false, {}) }
            R(Icons.Default.Edit, "App names", "Medium")
            R(Icons.Default.Edit, "Full list and search", "Small", last = true)
        }
    }
}
