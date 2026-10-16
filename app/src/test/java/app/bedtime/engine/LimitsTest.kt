package app.bedtime.engine

import app.bedtime.data.AppLimit
import app.bedtime.data.LimitRules
import app.bedtime.data.OutOfTime
import app.bedtime.data.UnlockConfig
import app.bedtime.data.UsageDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LimitsTest {
    private val today = 20_000L
    private val social = AppLimit(
        id = "social",
        name = "social",
        rules = LimitRules(apps = setOf("a", "b"), pauseSeconds = 10, dailyMinutes = 30, shared = true),
        createdDay = today - 5,
    )

    @Test
    fun sharedBudgetCountsEveryAppTogether() {
        val usage = UsageDay(today, seconds = mapOf("a" to 20 * 60L, "b" to 9 * 60L))
        assertEquals(60L, Limits.secondsLeft(social, "a", usage))
        assertNull(Limits.exhausted(listOf(social), "b", usage))
        // Unsaved seconds from the app in front count too.
        assertEquals(social, Limits.exhausted(listOf(social), "b", usage, mapOf("b" to 60L)))
    }

    @Test
    fun eachAppBudgetCountsOnlyThatApp() {
        val each = social.copy(rules = social.rules.copy(shared = false))
        val usage = UsageDay(today, seconds = mapOf("a" to 31 * 60L, "b" to 5 * 60L))
        assertEquals(each, Limits.exhausted(listOf(each), "a", usage))
        assertNull(Limits.exhausted(listOf(each), "b", usage))
    }

    @Test
    fun moreTimeAndLiftingExtendTheBudget() {
        val spent = UsageDay(today, seconds = mapOf("a" to 30 * 60L))
        assertEquals(0L, Limits.secondsLeft(social, "a", spent))
        val extra = spent.copy(extras = mapOf("social" to listOf(1L)))
        assertEquals(15 * 60L, Limits.secondsLeft(social, "a", extra))
        assertNull(Limits.secondsLeft(social, "a", spent.copy(lifted = setOf("social"))))
    }

    @Test
    fun noBudgetNeverRunsOut() {
        val pauseOnly = social.copy(rules = social.rules.copy(dailyMinutes = null))
        assertNull(Limits.exhausted(listOf(pauseOnly), "a", UsageDay(today, seconds = mapOf("a" to 99_999L))))
    }

    @Test
    fun strictestPauseAndShortestGraceWin() {
        val other = AppLimit(id = "x", rules = LimitRules(apps = setOf("a"), pauseSeconds = 30, graceMinutes = 0))
        assertEquals(PauseRule(30, 0, "a", setOf("a")), Limits.pauseFor(listOf(social, other), "a", today))
        assertEquals(PauseRule(10, 2, "b", setOf("b")), Limits.pauseFor(listOf(social, other), "b", today))
        assertNull(Limits.pauseFor(listOf(social), "c", today))
    }

    @Test
    fun aGroupPassCoversEveryAppOfItsLimit() {
        val group = social.copy(rules = social.rules.copy(pauseForGroup = true))
        assertEquals(PauseRule(10, 2, "limit:social", setOf("a", "b")), Limits.pauseFor(listOf(group), "b", today))
        assertTrue(Limits.isLooser(social.rules, group.rules))
        assertFalse(Limits.isLooser(group.rules, social.rules))
    }

    @Test
    fun chosenExtraMinutesCountAsGivenAndOlderOnesAsSet() {
        val spent = UsageDay(today, seconds = mapOf("a" to 30 * 60L))
        // Two older grants (15 min each, the set amount) and one chosen 40.
        val usage = spent.copy(extras = mapOf("social" to listOf(1L, 2L, 3L)), extraMinutes = mapOf("social" to listOf(40)))
        assertEquals(70, Limits.extraMinutes(social, usage))
        assertEquals(70 * 60L, Limits.secondsLeft(social, "a", usage))
        assertTrue(Limits.isLooser(social.rules, social.rules.copy(outOfTime = OutOfTime.CHOOSE)))
    }

    @Test
    fun looseningIsRecognised() {
        val rules = social.rules
        assertTrue(Limits.isLooser(rules, rules.copy(apps = setOf("a"))))
        assertTrue(Limits.isLooser(rules, rules.copy(pauseSeconds = 5)))
        assertTrue(Limits.isLooser(rules, rules.copy(pauseSeconds = null)))
        assertTrue(Limits.isLooser(rules, rules.copy(graceMinutes = 5)))
        assertTrue(Limits.isLooser(rules, rules.copy(dailyMinutes = 45)))
        assertTrue(Limits.isLooser(rules, rules.copy(dailyMinutes = null)))
        assertTrue(Limits.isLooser(rules, rules.copy(shared = false)))
        assertTrue(Limits.isLooser(rules, rules.copy(outOfTime = OutOfTime.REST_OF_DAY)))
        assertTrue(Limits.isLooser(rules, rules.copy(extraMinutes = 30)))
        assertTrue(Limits.isLooser(rules, rules.copy(unlock = UnlockConfig(textEnabled = false))))

        assertFalse(Limits.isLooser(rules, rules))
        assertFalse(Limits.isLooser(rules, rules.copy(apps = setOf("a", "b", "c"))))
        assertFalse(Limits.isLooser(rules, rules.copy(pauseSeconds = 20, dailyMinutes = 20)))
        assertFalse(Limits.isLooser(rules, rules.copy(outOfTime = OutOfTime.TOMORROW)))
        // Adding a budget where there was none only tightens.
        assertFalse(Limits.isLooser(rules.copy(dailyMinutes = null), rules))
    }

    @Test
    fun tighteningAppliesNowAndLooseningWaitsForMidnight() {
        val tighter = Limits.save(social, social.copy(rules = social.rules.copy(dailyMinutes = 20)), today)
        assertEquals(20, tighter.rules.dailyMinutes)
        assertNull(tighter.next)

        val looser = Limits.save(social, social.copy(name = "feeds", rules = social.rules.copy(dailyMinutes = 60)), today)
        assertEquals("feeds", looser.name) // The name is no loosening.
        assertEquals(30, looser.rules.dailyMinutes)
        assertEquals(60, looser.next?.dailyMinutes)
        assertEquals(today + 1, looser.nextFromDay)

        val tomorrow = Limits.settle(listOf(looser), today + 1).single()
        assertEquals(60, tomorrow.rules.dailyMinutes)
        assertNull(tomorrow.next)
    }

    @Test
    fun aLimitMadeTodayCanBeChangedOrDeletedFreely() {
        val fresh = Limits.save(null, social.copy(createdDay = 0), today)
        assertEquals(today, fresh.createdDay)
        val looser = Limits.save(fresh, fresh.copy(rules = fresh.rules.copy(dailyMinutes = 90)), today)
        assertEquals(90, looser.rules.dailyMinutes)
        assertNull(Limits.delete(fresh, today))
    }

    @Test
    fun deletingAnOlderLimitWaitsForMidnight() {
        val deleting = Limits.delete(social, today)!!
        assertEquals(1, Limits.settle(listOf(deleting), today).size)
        assertTrue(Limits.settle(listOf(deleting), today + 1).isEmpty())
        // Saving it again in the meantime keeps it.
        assertNull(Limits.save(deleting, deleting, today).deleteFromDay)
    }

    @Test
    fun usedSecondsAndWeek() {
        val usage = UsageDay(today, seconds = mapOf("a" to 600L, "b" to 300L), extras = mapOf("social" to listOf(1L, 2L)))
        assertEquals(900L, Limits.usedSeconds(social, usage))
        assertEquals(600L, Limits.usedSeconds(social.copy(rules = social.rules.copy(shared = false)), usage))
        val old = UsageDay(today - 7, seconds = mapOf("a" to 10_000L))
        assertEquals(900L to 2, Limits.week(social, listOf(old, usage), today))
    }
}
