package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Test

class GoneAppsTest {
    @Test
    fun aRemovedAppsKeywordNameAndPlaceGo() {
        val settings = AppSettings(
            keywords = mapOf(Keywords.appKey("gone.app") to "g", Keywords.appKey("kept.app") to "k", SearchOrder.CALENDAR to "c"),
            appNames = mapOf("gone.app" to "Old", "kept.app" to "Mine"),
            homeFavourites = listOf("gone.app", "kept.app"),
            alwaysAvailable = setOf("gone.app"),
        )
        val after = settings.withoutGoneApps { it != "gone.app" }
        assertEquals(mapOf(Keywords.appKey("kept.app") to "k", SearchOrder.CALENDAR to "c"), after.keywords)
        assertEquals(mapOf("kept.app" to "Mine"), after.appNames)
        assertEquals(listOf("kept.app"), after.homeFavourites)
        // What sessions and limits hold isn't touched.
        assertEquals(setOf("gone.app"), after.alwaysAvailable)
    }
}
