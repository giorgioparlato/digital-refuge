package app.bedtime.unlock

import org.junit.Assert.assertEquals
import org.junit.Test

class WaitAwayTest {
    @Test
    fun theScreenOffCountsUntilYouAreBack() {
        // Screen off for five minutes, unlocked, straight back to the wait: all of it.
        assertEquals(302_000L, WaitAway.counted(awayAt = 0L, backAt = 300_000L, resumedAt = 302_000L))
        // Still locked when it came back in front (refuge's own lock screen): all of it.
        assertEquals(60_000L, WaitAway.counted(awayAt = 0L, backAt = null, resumedAt = 60_000L))
    }

    @Test
    fun otherAppsAfterUnlockingDontCount() {
        // Unlocked after a minute, then ten minutes elsewhere: only the minute.
        assertEquals(60_000L, WaitAway.counted(awayAt = 0L, backAt = 60_000L, resumedAt = 660_000L))
    }
}
