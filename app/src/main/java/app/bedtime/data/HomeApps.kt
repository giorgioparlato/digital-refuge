package app.bedtime.data

/** What the home screen lists, worked out from what's installed, your choices and any session. */
object HomeApps {
    /** The apps on the home screen itself, and those in the full list behind a swipe up. */
    data class Lists(val home: List<String>, val all: List<String>)

    /**
     * On a session's quiet home screen ([minimal] not null) home lists its apps, plus the phone app.
     * Otherwise home is your favourites in your order. The full list is everything that opens: with
     * "all but these" ([allowed] not null) what's allowed and the always-available apps, otherwise
     * everything installed but hidden. A session's [blocked] apps are left out, since they wouldn't open.
     */
    fun lists(
        installed: List<String>,
        favourites: List<String>,
        hidden: Set<String>,
        minimal: Set<String>?,
        blocked: Set<String>,
        alwaysAvailable: Set<String>,
        dialer: String?,
        allowed: Set<String>? = null,
    ): Lists {
        val present = installed.toSet()
        // Always-available apps are never shut, whatever the session says.
        fun open(pkg: String) = pkg in present && (pkg in alwaysAvailable || (pkg !in blocked && (allowed == null || pkg in allowed)))
        val home = if (minimal != null) installed.filter { it in minimal || it == dialer } else favourites.filter(::open)
        val all = if (allowed != null) (home + installed.filter(::open)).distinct() else installed.filter { open(it) && it !in hidden }
        return Lists(home, all)
    }

    /** [favourites] with [pkg] moved one place up (or down), staying put at either end. */
    fun moved(favourites: List<String>, pkg: String, up: Boolean): List<String> {
        val i = favourites.indexOf(pkg)
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j !in favourites.indices) return favourites
        return favourites.toMutableList().also { it[i] = it[j]; it[j] = pkg }
    }
}
