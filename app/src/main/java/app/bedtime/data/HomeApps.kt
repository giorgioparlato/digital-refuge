package app.bedtime.data

/** What the home screen lists, worked out from what's installed, your choices and any session. */
object HomeApps {
    /** The apps on the home screen itself, and those in the full list behind a swipe up. */
    data class Lists(val home: List<String>, val all: List<String>)

    /**
     * In a minimal session ([minimal] not null) home lists what it allows, plus the phone app, and the
     * full list adds the always-available apps, as before. Otherwise home is your favourites in your
     * order, and the full list everything installed but hidden; a session's [blocked] apps are left
     * out of both, since they wouldn't open anyway.
     */
    fun lists(
        installed: List<String>,
        favourites: List<String>,
        hidden: Set<String>,
        minimal: Set<String>?,
        blocked: Set<String>,
        alwaysAvailable: Set<String>,
        dialer: String?,
    ): Lists {
        val present = installed.toSet()
        if (minimal != null) {
            val home = installed.filter { it in minimal || it == dialer }
            return Lists(home, (home + installed.filter { it in alwaysAvailable }).distinct())
        }
        // Always-available apps are never blocked, whatever the session says.
        fun open(pkg: String) = pkg in present && (pkg !in blocked || pkg in alwaysAvailable)
        return Lists(
            home = favourites.filter(::open),
            all = installed.filter { open(it) && it !in hidden },
        )
    }

    /** [favourites] with [pkg] moved one place up (or down), staying put at either end. */
    fun moved(favourites: List<String>, pkg: String, up: Boolean): List<String> {
        val i = favourites.indexOf(pkg)
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j !in favourites.indices) return favourites
        return favourites.toMutableList().also { it[i] = it[j]; it[j] = pkg }
    }
}
