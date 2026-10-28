package app.bedtime.service

/**
 * Time on the phone in a row: it starts at an unlock, stops when the screen goes off, and starts again
 * when you carry on past a nudge, so the next comes as long after.
 */
class RunClock {
    /** When this run began; null with the screen off. */
    var since: Long? = null
        private set

    /** Unlocked: a run begins, unless one is already going. */
    fun start(at: Long) {
        if (since == null) since = at
    }

    fun stop() {
        since = null
    }

    /** Past a nudge: the count begins again from here. */
    fun restart(at: Long) {
        since = at
    }

    /** When the nudge is due after [minutes] in a row; null when off, or with the screen off. */
    fun dueAt(minutes: Int?): Long? {
        val start = since ?: return null
        return minutes?.takeIf { it > 0 }?.let { start + it * 60_000L }
    }
}
