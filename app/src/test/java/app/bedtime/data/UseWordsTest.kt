package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UseWordsTest {
    private val today = UseToday(total = 13_200, distracting = 4_320, unlocks = 34, apps = mapOf("ig" to 2_280L, "yt" to 1_260L, "rd" to 780L, "x" to 30L))

    @Test
    fun theLineSaysWhatIsChosen() {
        assertEquals("3h 40m on the phone · 1h 12m distracting · 34 unlocks", UseWords.line(UseWidget(UseLook.LINE, total = true, distracting = true, unlocks = true), today))
        assertEquals("3h 40m on the phone today", UseWords.line(UseWidget(UseLook.LINE, total = true), today))
        assertEquals("34 unlocks today", UseWords.line(UseWidget(UseLook.LINE, unlocks = true), today))
        // Nothing chosen, or unlocks not known: the distracting time.
        assertEquals("1h 12m distracting today", UseWords.line(UseWidget(UseLook.LINE), today))
        assertEquals("1h 12m distracting today", UseWords.line(UseWidget(UseLook.LINE, unlocks = true), today.copy(unlocks = null)))
    }

    @Test
    fun underTheDotsOnlyWhatIsChosen() {
        assertNull(UseWords.underDots(UseWidget(), today))
        assertEquals("1h 12m distracting · 34 unlocks", UseWords.underDots(UseWidget(distracting = true, unlocks = true), today))
    }

    @Test
    fun aBudgetForDistractingAppsOrTheWholePhone() {
        val under = UseWords.budget(UseWidget(UseLook.BUDGET, budgetMinutes = 60, budgetChosen = true), today.copy(distracting = 2_400))
        assertEquals("40 of 60 min distracting", under.line)
        assertEquals("20 min left", under.note)
        assertFalse(under.over)
        val over = UseWords.budget(UseWidget(UseLook.BUDGET, budgetWholePhone = true, phoneBudgetMinutes = 180, phoneBudgetChosen = true), today.copy(total = 12_300))
        assertEquals("3h 25m of 3h on the phone", over.line)
        assertEquals("25 min over", over.note)
        assertTrue(over.over)
        assertEquals(1f, over.filled)
        // The bar stands for all 3h 25m: the budget is its first 180 of 205 minutes, the rest over.
        assertEquals(180f / 205f, over.allowedPart, 0.001f)
        assertEquals(1f, under.allowedPart)
    }

    @Test
    fun theTopAppsLeaveOutTheBriefOnes() {
        assertEquals(listOf("ig", "yt", "rd"), UseWords.top(today).map { it.first })
        assertEquals("3h 40m · 34 unlocks · ig 38 min · yt 21 min · rd 13 min", UseWords.appsLine(UseWidget(UseLook.APPS_LINE, unlocks = true), today) { it })
        assertEquals("3h 40m · ig 38 min · yt 21 min · rd 13 min", UseWords.appsLine(UseWidget(UseLook.APPS_LINE), today) { it })
    }

    @Test
    fun allowancesFitOnAChip() {
        assertEquals(listOf("30m", "1h", "1½h", "2h", "3h", "4h", "5h"), UseWords.PHONE_BUDGETS.map(UseWords::compact))
        assertEquals("1h 15m", UseWords.compact(75))
    }

    @Test
    fun everyAppTodayMostFirst() {
        val withAll = today.copy(allApps = today.apps + ("maps" to 3_000L))
        assertEquals(listOf("maps", "ig", "yt", "rd"), UseWords.ranked(withAll).map { it.first })
    }

    @Test
    fun anAllowanceNotChosenIsTheSuggestedOne() {
        val fresh = UseWidget()
        assertEquals(60, fresh.allowance(wholePhone = true))
        assertEquals(30, fresh.allowance(wholePhone = false))
        assertEquals(false, fresh.chosen(true))
        // Saved before choosing was kept: the old suggestions (3 h, 1 h) weren't chosen; anything else was.
        assertEquals(60, UseWidget(phoneBudgetMinutes = 180).allowance(true))
        assertEquals(30, UseWidget(budgetMinutes = 60).allowance(false))
        assertEquals(45, UseWidget(budgetMinutes = 45).allowance(false))
        assertEquals(true, UseWidget(budgetMinutes = 45).chosen(false))
        // The budget bar's own is yours, even at what used to be suggested.
        assertEquals(true, UseWidget(UseLook.BUDGET, budgetMinutes = 60).chosen(false))
        assertEquals(60, UseWidget(UseLook.BUDGET, budgetMinutes = 60).allowance(false))
        assertEquals(180, UseWidget(UseLook.BUDGET, budgetWholePhone = true, phoneBudgetMinutes = 180).allowance(true))
        assertEquals(false, UseWidget(UseLook.BUDGET, budgetMinutes = 60).chosen(true))
        // Chosen, even the suggested amount, it's yours and stops asking.
        val set = fresh.withAllowance(false, 30)
        assertEquals(true, set.chosen(false))
        assertEquals(false, set.chosen(true))
        assertEquals(180, fresh.withAllowance(true, 180).allowance(true))
    }
}
