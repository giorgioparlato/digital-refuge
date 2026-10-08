package app.bedtime.ui.edit

import app.bedtime.data.DndMode
import app.bedtime.data.Schedule
import app.bedtime.data.ScheduleKind
import app.bedtime.data.TextSource
import app.bedtime.data.UnlockAction
import app.bedtime.data.UnlockConfig
import app.bedtime.data.UnlockMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The lines each folded section shows while shut. They are the only thing on the page when it is
 * closed, so they have to be true and readable rather than a list of field names.
 */
class SummaryTest {
    private val bare = Schedule(
        blockedApps = emptySet(),
        greyscale = false,
        minimalMode = false,
        dnd = DndMode.OFF,
        hideNotifications = false,
        unlock = UnlockConfig(waitEnabled = false, textEnabled = false, passwordEnabled = false),
    )

    @Test
    fun anEmptyScheduleSaysSoRatherThanShowingNothing() {
        assertEquals("nothing blocked yet", whatItDoesSummary(bare))
    }

    @Test
    fun whatItDoesReadsInOrder() {
        val full = bare.copy(
            blockedApps = setOf("a", "b", "c"),
            minimalMode = true,
            greyscale = true,
            dnd = DndMode.SILENCE,
            hideNotifications = true,
        )
        assertEquals("3 apps blocked · minimal home · greyscale · silenced · notifications held", whatItDoesSummary(full))
    }

    @Test
    fun doNotDisturbOffIsNotWorthMentioning() {
        val quiet = bare.copy(blockedApps = setOf("a"), dnd = DndMode.OFF)
        assertEquals("1 app blocked", whatItDoesSummary(quiet))
    }

    @Test
    fun noStepsIsSaidPlainly() {
        assertEquals("a single tap — no steps picked", leavingSummary(bare, hasNewPassword = false))
    }

    @Test
    fun theStepsAreListedInTheOrderTheyRun() {
        val guarded = bare.copy(
            unlock = UnlockConfig(waitEnabled = true, waitSeconds = 120, textEnabled = true, textSource = TextSource.PASSAGES),
            unlockAction = UnlockAction(mode = UnlockMode.PAUSE, pauseMinutes = 5),
        )
        assertEquals("wait 2 min, then copy out a passage · a 5-minute break", leavingSummary(guarded, hasNewPassword = false))
    }

    @Test
    fun aPasswordOnlyCountsOnceThereIsOne() {
        val unset = bare.copy(unlock = UnlockConfig(waitEnabled = false, textEnabled = false, passwordEnabled = true))
        assertEquals("a single tap — no steps picked", leavingSummary(unset, hasNewPassword = false))
        assertTrue(leavingSummary(unset, hasNewPassword = true).startsWith("a password"))
    }

    @Test
    fun endingForTodayReadsDifferentlyForABlock() {
        val stepped = bare.copy(unlock = UnlockConfig(waitEnabled = true, waitSeconds = 60, textEnabled = false))
        assertTrue(leavingSummary(stepped, false).endsWith("off until its next start"))
        assertTrue(leavingSummary(stepped.copy(kind = ScheduleKind.BLOCK), false).endsWith("ends the block"))
    }

    @Test
    fun theWaysAroundNameTheDoorsThatAreShut() {
        val firm = bare.copy(pauseEnabled = false, lockSettings = true, fullScreenAlert = true)
        assertEquals("no emergency break · settings locked · reminder on", waysAroundSummary(firm, null))
    }

    @Test
    fun theHoursAppearOnlyWhenThereAreSome() {
        val open = bare.copy(pauseEnabled = true, breakMinutes = 2, lockSettings = false, fullScreenAlert = false)
        assertEquals("emergency break 2 min", waysAroundSummary(open, null))
        assertEquals("emergency break 2 min · changeable 08:00–20:00", waysAroundSummary(open, "08:00–20:00"))
    }
}
