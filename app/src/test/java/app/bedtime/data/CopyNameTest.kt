package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Test

class CopyNameTest {
    @Test
    fun theFirstCopyIsJustACopy() {
        assertEquals("Bedtime copy", copyName("Bedtime", setOf("Bedtime")))
    }

    @Test
    fun theSecondCopyIsNumbered() {
        assertEquals("Bedtime copy 2", copyName("Bedtime", setOf("Bedtime", "Bedtime copy")))
    }

    @Test
    fun itKeepsCountingPastTheNamesAlreadyTaken() {
        val taken = setOf("Bedtime", "Bedtime copy", "Bedtime copy 2", "Bedtime copy 3")
        assertEquals("Bedtime copy 4", copyName("Bedtime", taken))
    }

    @Test
    fun aGapInTheNumbersIsFilled() {
        // "copy 2" was deleted, so the next copy takes its place rather than becoming 4.
        assertEquals("Bedtime copy 2", copyName("Bedtime", setOf("Bedtime", "Bedtime copy", "Bedtime copy 3")))
    }

    @Test
    fun copyingACopyDoesNotStack() {
        assertEquals("Bedtime copy 2", copyName("Bedtime copy", setOf("Bedtime", "Bedtime copy")))
    }
}
