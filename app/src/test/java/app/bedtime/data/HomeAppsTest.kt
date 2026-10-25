package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeAppsTest {
    private val installed = listOf("phone", "maps", "signal", "chrome", "insta", "bank", "camera")
    private val favourites = listOf("signal", "insta", "camera", "gone")

    private fun lists(minimal: Set<String>? = null, blocked: Set<String> = emptySet(), hidden: Set<String> = setOf("bank")) =
        HomeApps.lists(installed, favourites, hidden, minimal, blocked, alwaysAvailable = setOf("maps"), dialer = "phone")

    @Test
    fun outsideSessionsYourFavouritesAndEverythingNotHidden() {
        val l = lists()
        // In your order; one that's been uninstalled just isn't there.
        assertEquals(listOf("signal", "insta", "camera"), l.home)
        assertEquals(installed - "bank", l.all)
    }

    @Test
    fun aSessionThatBlocksLeavesThoseOut() {
        val l = lists(blocked = setOf("insta", "chrome", "maps"))
        assertEquals(listOf("signal", "camera"), l.home)
        // Always-available apps are never blocked.
        assertEquals(listOf("phone", "maps", "signal", "camera"), l.all)
    }

    @Test
    fun minimalModeIsWhatItAllowsAsBefore() {
        val l = lists(minimal = setOf("signal", "camera"))
        assertEquals(listOf("phone", "signal", "camera"), l.home)
        assertEquals(listOf("phone", "signal", "camera", "maps"), l.all)
    }

    @Test
    fun movingAFavourite() {
        assertEquals(listOf("insta", "signal", "camera"), HomeApps.moved(listOf("signal", "insta", "camera"), "insta", up = true))
        assertEquals(listOf("signal", "camera", "insta"), HomeApps.moved(listOf("signal", "insta", "camera"), "insta", up = false))
        // At either end it stays put.
        assertEquals(listOf("signal", "insta"), HomeApps.moved(listOf("signal", "insta"), "signal", up = true))
        assertEquals(listOf("signal", "insta"), HomeApps.moved(listOf("signal", "insta"), "nope", up = true))
    }

    @Test
    fun oldSettingsLoadAsNotYourHome() {
        val s = AppJson.decodeFromString(AppSettings.serializer(), """{"onboarded":true}""")
        assertEquals(false, s.homeScreen)
        assertEquals(emptyList<String>(), s.homeFavourites)
        assertEquals(emptySet<String>(), s.hiddenApps)
        assertEquals(emptyMap<String, String>(), s.appNames)
    }
}
