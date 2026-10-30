package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LiveKeywordsTest {
    @Test
    fun aGoneAppsLookUpTakesItsKeywordWithIt() {
        val saved = mapOf(
            "lookup:com.removed.dictionary" to "d",
            "web:com.android.chrome" to "w",
            Keywords.appKey("gone.app") to "g",
            Keywords.appKey("kept.app") to "k",
        )
        val live = Keywords.live(saved, choices = setOf("web:com.android.chrome", SearchOrder.CALENDAR)) { it == "kept.app" }
        assertEquals(mapOf("web:com.android.chrome" to "w", Keywords.appKey("kept.app") to "k"), live)
        // So "d" is free again.
        assertEquals(null, Keywords.takenBy(live, SearchOrder.CALENDAR, "d"))
    }
}
