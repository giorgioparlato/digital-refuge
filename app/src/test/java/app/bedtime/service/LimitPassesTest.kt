package app.bedtime.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LimitPassesTest {
    private val minute = 60_000L

    @Test
    fun noPassMeansThePause() {
        assertFalse(LimitPasses.admit("never.granted", 2 * minute, 0))
    }

    @Test
    fun openAnywayLetsTheAppInAndBackWithinItsGrace() {
        val pkg = "grace.app"
        LimitPasses.grant(pkg, 1_000)
        assertTrue(LimitPasses.admit(pkg, 2 * minute, 1_500))
        LimitPasses.left(pkg, 10_000)
        assertTrue(LimitPasses.admit(pkg, 2 * minute, 10_000 + minute))
        LimitPasses.left(pkg, 100_000)
        assertFalse(LimitPasses.admit(pkg, 2 * minute, 100_000 + 3 * minute))
        // Once refused, the pass is gone.
        assertFalse(LimitPasses.admit(pkg, 2 * minute, 100_000 + 3 * minute + 1))
    }

    @Test
    fun noGraceAsksEveryTimeButStillLetsTheFirstOpenThrough() {
        val pkg = "every.time"
        LimitPasses.grant(pkg, 0)
        assertTrue(LimitPasses.admit(pkg, 0, 400))
        LimitPasses.left(pkg, 5_000)
        assertFalse(LimitPasses.admit(pkg, 0, 6_000))
    }

    @Test
    fun aPassNotUsedSoonExpires() {
        val pkg = "late.app"
        LimitPasses.grant(pkg, 0)
        assertFalse(LimitPasses.admit(pkg, 2 * minute, 60_000))
    }
}
