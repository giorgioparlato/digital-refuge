package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WorthTest {
    private val now = 10_000_000L

    @Test
    fun askedRarelyAndOnlyWhenSwitchedOn() {
        // Off, too short, already asked today about this app, three already today, or too soon after another check-in: not asked.
        assertFalse(Worth.due(null, 600, "ig", emptySet(), 0, 0, now))
        assertFalse(Worth.due(5, 299, "ig", emptySet(), 0, 0, now))
        assertFalse(Worth.due(5, 600, "ig", setOf("ig"), 1, 0, now))
        assertFalse(Worth.due(5, 600, "yt", setOf("a", "b", "c"), 3, 0, now))
        assertFalse(Worth.due(5, 600, "ig", emptySet(), 0, now - 60_000, now))
        assertTrue(Worth.due(5, 300, "ig", setOf("yt"), 1, now - Worth.AGAIN_MS, now))
    }

    @Test
    fun theAnswersAddUpOnceThereAreEnough() {
        fun answers(vararg worth: Boolean) = worth.map { WorthAnswer(1, "ig", 600, it) }
        assertNull(Worth.words(answers(true, false)))
        assertEquals("mostly worth it · 3 answers", Worth.words(answers(true, true, false)))
        assertEquals("mostly not worth it · 4 answers", Worth.words(answers(false, false, false, true)))
        assertEquals("half worth it · 4 answers", Worth.words(answers(true, true, false, false)))
        // The oldest go once there are more than are kept.
        val many = (1..Worth.KEPT).map { WorthAnswer(it.toLong(), "ig", 1, true) }
        assertEquals(Worth.KEPT, Worth.added(many, WorthAnswer(0, "x", 1, false)).size)
    }
}
