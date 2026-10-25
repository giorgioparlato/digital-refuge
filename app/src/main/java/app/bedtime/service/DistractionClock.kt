package app.bedtime.service

import app.bedtime.data.HourlyUse
import app.bedtime.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.ZoneId

/**
 * Counts time in your distracting apps by the hour, from the same window changes blocking already
 * sees. Only which app was in front and for how long; nothing about what was in it.
 */
class DistractionClock(private val repo: Repository, private val scope: CoroutineScope, private val ownPackage: String) {
    @Volatile
    var distracting: Set<String> = emptySet()

    private var current: String? = null
    private var since = 0L

    /** [pkg] is now in front, or nothing is (the screen went off). */
    @Synchronized
    fun moveTo(pkg: String?) {
        if (pkg == current) return
        val now = System.currentTimeMillis()
        record(now)
        current = pkg
        since = now
    }

    /** Writes down the time so far, so today's row is current. */
    @Synchronized
    fun flush() {
        val now = System.currentTimeMillis()
        record(now)
        since = now
    }

    /** Any app counts toward the day's use, except digital refuge itself (its home and lock screens). */
    private fun record(now: Long) {
        val pkg = current ?: return
        if (pkg == ownPackage) return
        val isDistracting = pkg in distracting
        HourlyUse.split(since, now, ZoneId.systemDefault()).forEach { (day, hours) ->
            scope.launch { repo.addHourly(day, if (isDistracting) hours else emptyMap(), hours, pkg.takeIf { isDistracting }) }
        }
    }
}
