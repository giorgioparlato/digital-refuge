package app.bedtime.ui.limits

import app.bedtime.data.AppLimit
import app.bedtime.data.LimitRules
import app.bedtime.data.OutOfTime
import app.bedtime.data.UnlockConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LimitProblemTest {
    private val ok = AppLimit(rules = LimitRules(apps = setOf("a"), pauseSeconds = 10))

    @Test
    fun aLimitNeedsAppsAndSomethingToDo() {
        assertNull(limitProblem(ok, ""))
        assertEquals("Pick at least one app for this limit.", limitProblem(ok.copy(rules = ok.rules.copy(apps = emptySet())), ""))
        val nothing = ok.copy(rules = ok.rules.copy(pauseSeconds = null))
        assertEquals("Turn on a pause, minutes a day, or opens a day.", limitProblem(nothing, ""))
        // Opens alone are enough.
        assertNull(limitProblem(nothing.copy(rules = nothing.rules.copy(dailyOpens = 3)), ""))
    }

    @Test
    fun aPasswordStepNeedsAPassword() {
        val stepped = ok.copy(rules = ok.rules.copy(dailyOpens = 3, outOfTime = OutOfTime.MORE_TIME, unlock = UnlockConfig(passwordEnabled = true)))
        assertEquals("Choose a password, or switch that step off.", limitProblem(stepped, ""))
        assertNull(limitProblem(stepped, "secret"))
    }
}
