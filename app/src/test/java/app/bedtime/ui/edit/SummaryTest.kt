package app.bedtime.ui.edit

import app.bedtime.data.DndMode
import app.bedtime.data.LockNotes
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
        assertEquals("nothing shut yet", shutsSummary(bare))
        assertEquals("as usual", feelsSummary(bare))
    }

    @Test
    fun whatItShutsSaysWhichWay() {
        assertEquals("3 apps", shutsSummary(bare.copy(blockedApps = setOf("a", "b", "c"), allowOnly = false)))
        assertEquals("all but 2 apps", shutsSummary(bare.copy(allowOnly = true, allowedApps = setOf("a", "b"))))
    }

    @Test
    fun howThePhoneFeelsReadsInOrder() {
        val full = bare.copy(minimalMode = true, greyscale = true, dnd = DndMode.SILENCE, hideNotifications = true, lockNotes = LockNotes.NONE)
        assertEquals("greyscale · quiet home screen · nothing rings · out of sight · lock screen: none", feelsSummary(full))
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
    fun moreOptionsNameWhatsSetOrWhatsInThem() {
        assertEquals("emergency break 1 min", moreSummary(bare.copy(pauseEnabled = true, breakMinutes = 1), null))
        assertEquals("no emergency break · asks why · changeable 08:00–20:00", moreSummary(bare.copy(pauseEnabled = false, askWhy = true), "08:00–20:00"))
    }
}
