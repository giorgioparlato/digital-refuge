package app.bedtime.service

import app.bedtime.data.HourlyUse
import app.bedtime.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.ZoneId

/**
 * Counts time in your distracting apps by the hour, from the same window changes blocking already
 * sees. Only which app was in front and for how long; nothing about what was in it.
 *
 * Kept in memory and written down only when it's about to be looked at — on refuge's own screens,
 * where the hours show — when the screen goes off, or every few minutes otherwise. Moving between
 * other apps costs no write, which would rewrite the whole store each time.
 */
class DistractionClock(private val repo: Repository, private val scope: CoroutineScope, private val ownPackage: String) {
    @Volatile
    var distracting: Set<String> = emptySet()

    private var current: String? = null
    private var since = 0L

    /** Seconds not yet written, by day: in distracting apps and in any app, by hour, and by app. */
    private class Pending {
        val distracting = mutableMapOf<Int, Long>()
        val all = mutableMapOf<Int, Long>()
        val apps = mutableMapOf<String, Long>()
    }

    private val pending = mutableMapOf<Long, Pending>()
    private var lastWrite = 0L

    /** [pkg] is now in front, or nothing is (the screen went off). */
    @Synchronized
    fun moveTo(pkg: String?) {
        if (pkg == current) return
        val now = System.currentTimeMillis()
        record(now)
        current = pkg
        since = now
        if (pkg == null || pkg == ownPackage || now - lastWrite >= WRITE_EVERY_MS) write(now)
    }

    /** Any app counts toward the day's use, except digital refuge itself (its home and lock screens). */
    private fun record(now: Long) {
        val pkg = current ?: return
        if (pkg == ownPackage) return
        val isDistracting = pkg in distracting
        HourlyUse.split(since, now, ZoneId.systemDefault()).forEach { (day, hours) ->
            val day0 = pending.getOrPut(day) { Pending() }
            hours.forEach { (hour, s) ->
                day0.all.merge(hour, s, Long::plus)
                if (isDistracting) day0.distracting.merge(hour, s, Long::plus)
            }
            if (isDistracting) day0.apps.merge(pkg, hours.values.sum(), Long::plus)
        }
    }

    private fun write(now: Long) {
        lastWrite = now
        if (pending.isEmpty()) return
        val batch = pending.toMap()
        pending.clear()
        batch.forEach { (day, p) -> scope.launch { repo.addHourly(day, p.distracting, p.all, p.apps) } }
    }

    private companion object {
        const val WRITE_EVERY_MS = 5 * 60_000L
    }
}
