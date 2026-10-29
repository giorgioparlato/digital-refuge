package app.bedtime.engine

/** What happens when an app comes to the front. */
enum class Verdict {
    /** Nothing at all: ours, the system's, always available, or no state read yet. */
    LET_THROUGH,

    /** A session blocks it: its cover goes over it. */
    COVER,

    /** Minimal mode doesn't allow it: back to the minimal home. */
    MINIMAL_HOME,

    /** No session stops it; its limits, if any, have their say. */
    CHECK_LIMITS,
}

/** The blocking service's decision, apart from the service so it can be tested. */
object Enforcement {
    /**
     * What to do now that [pkg] is in front. Always-allowed and always-available apps are never held
     * back, by a session or a limit. In minimal mode the recent-apps view passes (whatever is opened
     * from it is judged as itself), and so does anything that isn't an app you could have opened:
     * a keyboard or a popup over the app you're in ([opensAsApp]).
     */
    fun decide(
        pkg: String,
        ownPackage: String,
        state: ActiveState?,
        alwaysAllowed: Set<String>,
        alwaysAvailable: Set<String>,
        recentsInFront: Boolean,
        opensAsApp: (String) -> Boolean,
    ): Verdict {
        if (state == null || pkg == ownPackage || pkg in alwaysAllowed || pkg in alwaysAvailable) return Verdict.LET_THROUGH
        if (state.isActive) {
            if (pkg in state.blocked) return Verdict.COVER
            val minimal = state.minimalAllowlist
            if (minimal != null && pkg !in minimal && !recentsInFront && opensAsApp(pkg)) return Verdict.MINIMAL_HOME
        }
        return Verdict.CHECK_LIMITS
    }
}
