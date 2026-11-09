package app.bedtime.engine

/** What happens when an app comes to the front. */
enum class Verdict {
    /** Nothing at all: ours, the system's, always available, or no state read yet. */
    LET_THROUGH,

    /** A session blocks it: its cover goes over it. */
    COVER,

    /** "All but these" doesn't allow it, with the quiet home screen on: back to it. */
    MINIMAL_HOME,

    /** No session stops it; its limits, if any, have their say. */
    CHECK_LIMITS,
}

/** The blocking service's decision, apart from the service so it can be tested. */
object Enforcement {
    /**
     * What to do now that [pkg] is in front. Always-allowed and always-available apps are never held
     * back, by a session or a limit, and nor is an app refuge itself just opened for you ([Passes]).
     * With "all but these" the recent-apps view passes (whatever is opened from it is judged as itself),
     * and so does anything that isn't an app you could have opened: a keyboard or a popup over the app
     * you're in ([opensAsApp]). An app it doesn't allow goes back to the quiet home screen, or without
     * one gets the cover, your own [launchers] passing so Home still works.
     */
    fun decide(
        pkg: String,
        ownPackage: String,
        state: ActiveState?,
        alwaysAllowed: Set<String>,
        alwaysAvailable: Set<String>,
        recentsInFront: Boolean,
        opensAsApp: (String) -> Boolean,
        launchers: Set<String> = emptySet(),
        now: Long = System.currentTimeMillis(),
    ): Verdict {
        if (state == null || pkg == ownPackage || pkg in alwaysAllowed || pkg in alwaysAvailable || Passes.has(pkg, now)) return Verdict.LET_THROUGH
        if (state.isActive) {
            if (pkg in state.blocked) return Verdict.COVER
            val allowlist = state.allowlist
            if (allowlist != null && pkg !in allowlist && !recentsInFront && opensAsApp(pkg)) {
                return when {
                    state.minimalOccurrence != null -> Verdict.MINIMAL_HOME
                    pkg in launchers -> Verdict.CHECK_LIMITS
                    else -> Verdict.COVER
                }
            }
        }
        return Verdict.CHECK_LIMITS
    }
}
