package app.bedtime.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchPageFindTest {
    @Test
    fun foundByNameOrByWhatItIs() {
        assertTrue(SearchOrder.matches("lookup:com.dict", "Look up in Livio", "dictionary"))
        assertTrue(SearchOrder.matches("maps:com.google.android.apps.maps", "Maps · Google Maps", "maps"))
        assertTrue(SearchOrder.matches(SearchOrder.ALARM, "Alarms: \"alarm 7:30\"", "wake"))
        assertFalse(SearchOrder.matches(SearchOrder.ALARM, "Alarms", "dictionary"))
        assertFalse(SearchOrder.matches(SearchOrder.ALARM, "Alarms", " "))
    }
}
