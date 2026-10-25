package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeywordsTest {
    private val keywords = mapOf(
        "maps:g" to "m",
        "web:c" to "w",
        "wikipedia:org.wikipedia" to "wp",
        SearchOrder.CONTACTS to "@",
        "app:com.spotify" to "sp",
    )

    @Test
    fun aKeywordAndASpaceSendTheRestOn() {
        assertEquals(Keywords.Hit("maps:g", "cafe"), Keywords.match("m cafe", keywords))
        assertEquals(Keywords.Hit("maps:g", "cafe near me"), Keywords.match("M cafe near me", keywords))
        // The longer keyword wins over the shorter it begins with.
        assertEquals(Keywords.Hit("wikipedia:org.wikipedia", "tea"), Keywords.match("wp tea", keywords))
        assertEquals(Keywords.Hit("web:c", "tea"), Keywords.match("w tea", keywords))
    }

    @Test
    fun aSignNeedsNoSpaceAndAnAppsKeywordStandsAlone() {
        assertEquals(Keywords.Hit(SearchOrder.CONTACTS, "anna"), Keywords.match("@anna", keywords))
        assertEquals(Keywords.Hit(SearchOrder.CONTACTS, "anna"), Keywords.match("@ anna", keywords))
        assertEquals(Keywords.Hit("app:com.spotify", ""), Keywords.match("sp", keywords))
    }

    @Test
    fun nothingWithoutWordsOrInsideAWord() {
        assertNull(Keywords.match("m", keywords))
        assertNull(Keywords.match("m ", keywords))
        assertNull(Keywords.match("map", keywords))
        assertNull(Keywords.match("milk", keywords))
    }

    @Test
    fun suggestedOnesDontClash() {
        val choices = listOf(SearchChoice("web:c", true), SearchChoice("maps:g", false), SearchChoice("lookup:d", false))
        val d = Keywords.defaults(choices, listOf(NoteTarget("scratchpad", "x"), NoteTarget("movies list", "y"), NoteTarget("music", "z")))
        assertEquals("w", d["web:c"])
        assertEquals("m", d["maps:g"])
        assertEquals("d", d["lookup:d"])
        assertEquals("n", d[SearchOrder.REFUGE_NOTES])
        assertEquals("c", d[SearchOrder.CALENDAR])
        assertEquals("@", d[SearchOrder.CONTACTS])
        assertEquals("sc", d["note:x"])
        assertEquals("mo", d["note:y"])
        // "mu" is free too; a second "mo…" wouldn't be.
        assertEquals("mu", d["note:z"])
        assertEquals("m", Keywords.clean(" M "))
    }
}
