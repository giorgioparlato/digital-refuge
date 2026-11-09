package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Test

class FootButtonsTest {
    @Test
    fun theOlderSwitchesGiveTheFirstOrder() {
        assertEquals(listOf(FootButton.UNLOCK, FootButton.ARROW, FootButton.EMERGENCY), HomeStyle().foot())
        assertEquals(
            listOf(FootButton.MARK, FootButton.UNLOCK, FootButton.NOTES, FootButton.EMERGENCY),
            HomeStyle(showLogo = true, showSwipeArrow = false, showNotesButton = true).foot(),
        )
        // Unlock and emergency are never lost, whatever was saved.
        assertEquals(listOf(FootButton.HABITS, FootButton.UNLOCK, FootButton.EMERGENCY), HomeStyle(footButtons = listOf(FootButton.HABITS)).foot())
    }

    @Test
    fun threeChosenAtMostAndTheTwoStay() {
        var foot = HomeStyle().foot()
        foot = FootButtons.with(foot, FootButton.HABITS)
        foot = FootButtons.with(foot, FootButton.INSIGHTS)
        assertEquals(foot, FootButtons.with(foot, FootButton.NOTES))
        assertEquals(5, foot.size)
        assertEquals(foot, FootButtons.without(foot, FootButton.UNLOCK))
        assertEquals(4, FootButtons.without(foot, FootButton.ARROW).size)
        assertEquals(Triple(2, 1, 2), FootButtons.gathered(5))
        assertEquals(Triple(2, 0, 2), FootButtons.gathered(4))
    }
}
