package app.bedtime.engine

import app.bedtime.data.Schedule
import org.junit.Assert.assertEquals
import org.junit.Test

class EnforcementTest {
    private val bed = Schedule(id = "bed", blockedApps = setOf("insta"))
    private val minimal = Schedule(id = "min", minimalMode = true, allowedApps = setOf("maps"))
    private fun state(vararg s: Schedule) = ActiveState(s.map { Occurrence(it, 0L, 1L) }, null)

    private fun decide(pkg: String, state: ActiveState?, recents: Boolean = false, isApp: Boolean = true, available: Set<String> = emptySet(), now: Long = 0L) =
        Enforcement.decide(
            pkg, "app.bedtime", state, alwaysAllowed = setOf("phone"), alwaysAvailable = available, recentsInFront = recents,
            opensAsApp = { isApp }, launchers = setOf("launcher"), now = now,
        )

    @Test
    fun allButTheseWithoutTheQuietHomeScreenCoversButLetsYourLauncherBe() {
        val allBut = Schedule(id = "ab", allowOnly = true, allowedApps = setOf("maps"))
        assertEquals(Verdict.COVER, decide("mail", state(allBut)))
        assertEquals(Verdict.CHECK_LIMITS, decide("maps", state(allBut)))
        // Home still works.
        assertEquals(Verdict.CHECK_LIMITS, decide("launcher", state(allBut)))
    }

    @Test
    fun anAppRefugeOpensForYouPassesForAMoment() {
        Passes.grant("sync", now = 1_000L)
        assertEquals(Verdict.LET_THROUGH, decide("sync", state(Schedule(blockedApps = setOf("sync"))), now = 5_000L))
        assertEquals(Verdict.COVER, decide("sync", state(Schedule(blockedApps = setOf("sync"))), now = 60_000L))
    }

    @Test
    fun aBlockedAppIsCovered() {
        assertEquals(Verdict.COVER, decide("insta", state(bed)))
        assertEquals(Verdict.CHECK_LIMITS, decide("mail", state(bed)))
    }

    @Test
    fun nothingHoldsBackOursTheSystemsOrTheAlwaysAvailable() {
        assertEquals(Verdict.LET_THROUGH, decide("app.bedtime", state(bed)))
        assertEquals(Verdict.LET_THROUGH, decide("phone", state(minimal)))
        // Always available beats a block, and limits too.
        assertEquals(Verdict.LET_THROUGH, decide("insta", state(bed), available = setOf("insta")))
        // Before the state is read, nothing is decided.
        assertEquals(Verdict.LET_THROUGH, decide("insta", null))
    }

    @Test
    fun minimalModeSendsHomeOnlyRealAppsNotAllowed() {
        assertEquals(Verdict.MINIMAL_HOME, decide("mail", state(minimal)))
        assertEquals(Verdict.CHECK_LIMITS, decide("maps", state(minimal)))
        // A keyboard or a popup over the allowed app, and the recent-apps view, pass.
        assertEquals(Verdict.CHECK_LIMITS, decide("keyboard", state(minimal), isApp = false))
        assertEquals(Verdict.CHECK_LIMITS, decide("mail", state(minimal), recents = true))
    }

    @Test
    fun withNoSessionOnlyLimitsCount() {
        assertEquals(Verdict.CHECK_LIMITS, decide("insta", state()))
    }
}
