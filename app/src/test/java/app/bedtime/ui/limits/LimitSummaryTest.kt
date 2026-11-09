package app.bedtime.ui.limits

import app.bedtime.data.LimitRules
import app.bedtime.data.OutOfTime
import app.bedtime.data.PauseKind
import app.bedtime.data.UnlockConfig
import org.junit.Assert.assertEquals
import org.junit.Test

/** The lines each fold of a limit's page shows while shut. */
class LimitSummaryTest {
    @Test
    fun thePauseSaysWhatItIsAndWhatComesAfter() {
        assertEquals("no pause", pauseSummary(LimitRules(pauseSeconds = null)))
        assertEquals("a 10 s wait · then open until you leave", pauseSummary(LimitRules(pauseSeconds = 10)))
        assertEquals("ask why · then open 15 min", pauseSummary(LimitRules(pauseSeconds = 10, pauseKind = PauseKind.ASK, pauseGrowth = 50, openMinutes = 15)))
    }

    @Test
    fun eachDayAndWhenItRunsOut() {
        assertEquals("no daily budget", dailySummary(LimitRules()))
        assertEquals("30 min a day · each app", dailySummary(LimitRules(apps = setOf("a", "b"), dailyMinutes = 30, shared = false)))
        assertEquals("wait for tomorrow", runsOutSummary(LimitRules(outOfTime = OutOfTime.TOMORROW), false))
        val more = LimitRules(outOfTime = OutOfTime.MORE_TIME, extraMinutes = 10, unlock = UnlockConfig(waitEnabled = true, waitSeconds = 30, textEnabled = false))
        assertEquals("10 min more · wait 30 s", runsOutSummary(more, false))
    }
}
