package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Test

class SearchModesTest {
    private val choices = listOf(
        SearchChoice("web:a", true),
        SearchChoice("contacts", true),
        SearchChoice("timer", true),
        SearchChoice("maps:m", false),
        SearchChoice("files", false),
    )

    @Test
    fun eachKeyBelongsToAKind() {
        assertEquals(SearchCategory.LOOK_UP, SearchOrder.category("maps:m"))
        assertEquals(SearchCategory.DO, SearchOrder.category("timer"))
        assertEquals(SearchCategory.DO, SearchOrder.category("note:refuge"))
        assertEquals(SearchCategory.FIND, SearchOrder.category("contacts"))
    }

    @Test
    fun anOldOffOneWithAKeywordBecomesKeywordOnly() {
        val moded = SearchOrder.withModes(choices, mapOf("maps:m" to "m"), alreadySet = false)
        assertEquals(SearchMode.KEYWORD, moded.first { it.key == "maps:m" }.mode)
        assertEquals(SearchMode.OFF, moded.first { it.key == "files" }.mode)
        // Once saved with the three states, off stays off.
        assertEquals(SearchMode.OFF, SearchOrder.withModes(choices, mapOf("maps:m" to "m"), alreadySet = true).first { it.key == "maps:m" }.mode)
    }

    @Test
    fun aKindSwitchedOffTakesItsActionsWithIt() {
        val usable = SearchOrder.usable(choices, setOf(SearchCategory.LOOK_UP))
        assertEquals(listOf("contacts", "timer"), usable.map { it.key })
    }

    @Test
    fun theUsableComeInTheKindsOrder() {
        assertEquals(listOf("contacts", "timer", "web:a"), SearchOrder.usable(choices, emptySet()).map { it.key })
    }

    @Test
    fun settingAStateSwitchesAndKeepsOut() {
        val keywordOnly = SearchOrder.setMode(choices, "files", SearchMode.KEYWORD).first { it.key == "files" }
        assertEquals(SearchMode.KEYWORD, keywordOnly.mode)
        val off = SearchOrder.setMode(choices, "web:a", SearchMode.OFF).first { it.key == "web:a" }
        assertEquals(SearchMode.OFF, off.mode)
        assertEquals("3 in every search · 0 by keyword · 2 off", SearchOrder.summary(choices))
    }
}
