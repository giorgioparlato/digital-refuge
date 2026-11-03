package app.bedtime.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LimitPassesTest {
    private val minute = 60_000L

    @Test
    fun noPassMeansThePause() {
        assertFalse(LimitPasses.admit("never.granted", null, 0))
    }

    @Test
    fun untilYouLeaveWithAMinuteToComeBack() {
        val pkg = "visit.app"
        LimitPasses.grant(pkg, 1_000)
        assertTrue(LimitPasses.admit(pkg, null, 1_500))
        LimitPasses.left(pkg, 10_000)
        assertTrue(LimitPasses.admit(pkg, null, 10_000 + minute))
        LimitPasses.left(pkg, 100_000)
        assertFalse(LimitPasses.admit(pkg, null, 100_000 + 2 * minute))
        // Once refused, the pass is gone.
        assertFalse(LimitPasses.admit(pkg, null, 100_000 + 2 * minute + 1))
    }

    @Test
    fun aTimeOpenLetsYouComeAndGoUntilItsOver() {
        val pkg = "timed.app"
        LimitPasses.grant(pkg, 0)
        assertTrue(LimitPasses.admit(pkg, 10 * minute, 500))
        LimitPasses.left(pkg, minute)
        // Away five minutes and back: still open.
        assertTrue(LimitPasses.admit(pkg, 10 * minute, 6 * minute))
        assertEquals(10 * minute, LimitPasses.endsAt(pkg, 10 * minute))
        assertFalse(LimitPasses.admit(pkg, 10 * minute, 10 * minute))
    }

    @Test
    fun aPassNotUsedSoonExpires() {
        val pkg = "late.app"
        LimitPasses.grant(pkg, 0)
        assertFalse(LimitPasses.admit(pkg, null, 60_000))
    }
}
