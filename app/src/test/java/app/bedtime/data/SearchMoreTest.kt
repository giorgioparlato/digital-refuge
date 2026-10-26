package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SearchMoreTest {
    @Test
    fun sumsAndOnlySums() {
        assertEquals("36", Calculator.format(Calculator.evaluate("12 * 3")!!))
        assertEquals("7", Calculator.format(Calculator.evaluate("1 + 2 × 3")!!))
        assertEquals("9", Calculator.format(Calculator.evaluate("(1 + 2) x 3")!!))
        assertEquals("1024", Calculator.format(Calculator.evaluate("2^10")!!))
        assertEquals("15", Calculator.format(Calculator.evaluate("10% * 150")!!))
        assertEquals("2.5", Calculator.format(Calculator.evaluate("5 ÷ 2")!!))
        assertEquals("0.3333333333", Calculator.format(Calculator.evaluate("1/3")!!))
        assertEquals("-4", Calculator.format(Calculator.evaluate("-2 - 2")!!))
        // A plain number, words, a broken sum, or dividing by nothing aren't answers.
        assertNull(Calculator.evaluate("42"))
        assertNull(Calculator.evaluate("-42"))
        assertNull(Calculator.evaluate("box"))
        assertNull(Calculator.evaluate("2 +"))
        assertNull(Calculator.evaluate("1/0"))
    }

    @Test
    fun aNameThenAMessage() {
        val names = listOf("Anna", "Anna Maria", "Anna Svensson", "Bo")
        assertEquals(listOf("Anna Maria") to "hi", ContactSearch.split("anna maria hi", names))
        assertEquals(listOf("Anna", "Anna Maria", "Anna Svensson") to "running late", ContactSearch.split("Anna running late", names))
        assertEquals(listOf("Bo") to "", ContactSearch.split("bo", names))
        // A name must end where a word does.
        assertEquals(emptyList<String>() to "", ContactSearch.split("bob hi", names))
    }

    @Test
    fun chatAppsYouChoseThatAreThere() {
        val here = listOf(ChatApp.SMS, ChatApp.SIGNAL, ChatApp.TELEGRAM)
        assertEquals(listOf(ChatApp.SIGNAL, ChatApp.SMS), ChatApp.chosen(listOf("signal", "whatsapp", "sms"), here))
        assertEquals(3, ChatApp.chosen(listOf("sms", "signal", "telegram", "whatsapp"), ChatApp.entries).size)
    }

    @Test
    fun choicesSitUnderTheirHeading() {
        assertEquals("The web, maps and look-ups", SearchOrder.group("maps:g"))
        assertEquals("On the phone", SearchOrder.group(SearchOrder.CALCULATOR))
        assertEquals("Notes and calendar", SearchOrder.group("note:content://x"))
        assertEquals("Files", SearchOrder.group(SearchOrder.OBSIDIAN))
        assertEquals("Other apps", SearchOrder.group("spotify:s"))
    }
}
