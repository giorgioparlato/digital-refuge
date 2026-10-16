package app.bedtime.service

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.widget.Toast
import app.bedtime.apps.AppCatalog
import app.bedtime.data.AppLimit
import app.bedtime.data.Repository
import app.bedtime.data.UsageDay
import app.bedtime.engine.Limits
import app.bedtime.ui.blocked.BlockedActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * "Open anyway" passes from the pause before opening: an app you chose to open stays open, and
 * coming back to it within its grace doesn't ask again. Kept in memory only: a restart asks afresh.
 */
object LimitPasses {
    private class Pass(val grantedAt: Long, var leftAt: Long? = null, var entered: Boolean = false)

    /** Long enough for the app to come up after "open anyway", short enough not to be saved for later. */
    private const val ON_ITS_WAY_MS = 15_000L

    private val passes = mutableMapOf<String, Pass>()

    @Synchronized
    fun grant(pkg: String, now: Long) {
        passes[pkg] = Pass(now)
    }

    /** [pkg] just came to the front: whether it may, without the pause. */
    @Synchronized
    fun admit(pkg: String, graceMs: Long, now: Long): Boolean {
        val pass = passes[pkg] ?: return false
        val left = pass.leftAt
        val ok = when {
            left != null -> now - left <= graceMs
            !pass.entered -> now - pass.grantedAt < ON_ITS_WAY_MS
            else -> true
        }
        if (ok) {
            pass.entered = true
            pass.leftAt = null
        } else {
            passes.remove(pkg)
        }
        return ok
    }

    /** [pkg] went to the background; its grace starts now. */
    @Synchronized
    fun left(pkg: String, now: Long) {
        passes[pkg]?.takeIf { it.entered }?.leftAt = now
    }
}

/**
 * Counts the time spent in apps that have a limit, and steps in when one is due: the pause before
 * opening, or the cover once a day's minutes are gone. Driven by [BlockerService], which knows what
 * is in front; the counting is by window changes, so it is close rather than exact.
 */
internal class LimitTracker(
    private val context: Context,
    private val scope: CoroutineScope,
    /** For writes, which must finish even as the service goes away. */
    private val writeScope: CoroutineScope,
) {
    private val repo = Repository.get(context)
    private val handler = Handler(Looper.getMainLooper())

    /** Set by [BlockerService] from the settings. */
    var limits: List<AppLimit> = emptyList()

    /** The latest saved usage, newest day last. */
    private var usage: List<UsageDay> = emptyList()

    /** The app in front, whether or not it's limited, and since when we've counted it (elapsed realtime). */
    private var current: String? = null
    private var since = 0L

    /** Milliseconds counted but not yet written down, by package. */
    private val unsaved = mutableMapOf<String, Long>()

    /** Seconds sent to storage but not yet seen coming back, so they aren't briefly forgotten. */
    private val writing = mutableMapOf<String, Long>()

    /** Packages already told "a minute left" today, so the toast comes once. */
    private val warned = mutableSetOf<String>()
    private var warnedDay = 0L

    private val tick = Runnable { onTick() }

    fun start() {
        scope.launch {
            repo.usage.collect {
                usage = it
                writing.clear()
            }
        }
    }

    private fun today(): Long = LocalDate.now().toEpochDay()

    private fun todayUsage(): UsageDay = usage.lastOrNull { it.day == today() } ?: UsageDay(today())

    private fun limited(pkg: String): Boolean = limits.any { pkg in it.rules.apps }

    /**
     * [pkg] is now in front, or nothing is (the screen went off). Returns true if it's a different
     * app from the one before, which is when the pause before opening is due.
     */
    fun moveTo(pkg: String?): Boolean {
        val previous = current
        if (pkg == previous) return false
        stopCounting()
        previous?.let { LimitPasses.left(it, System.currentTimeMillis()) }
        current = pkg
        since = SystemClock.elapsedRealtime()
        return true
    }

    /**
     * Applies [pkg]'s limits now that it's in front: the cover if its minutes are gone, otherwise the
     * pause if it has just [arrived]. Returns true if something was put over it.
     */
    fun check(pkg: String, arrived: Boolean): Boolean {
        if (!limited(pkg)) return false
        val day = todayUsage()
        val pending = pendingSeconds()
        Limits.exhausted(limits, pkg, day, pending)?.let { limit ->
            // Written down first, so the cover sees the same minutes we do.
            flush()
            cover(BlockedActivity.limitIntent(context, pkg, limit.id))
            return true
        }
        if (arrived) {
            val (seconds, grace) = Limits.pauseFor(limits, pkg, day.day) ?: (0 to 0)
            if (seconds > 0 && !LimitPasses.admit(pkg, grace * 60_000L, System.currentTimeMillis())) {
                writeScope.launch { repo.recordOpen(day.day, pkg) }
                cover(BlockedActivity.pauseIntent(context, pkg, seconds, opens = (day.opens[pkg] ?: 0) + 1))
                return true
            }
        }
        scheduleTick(pkg)
        return false
    }

    /** Writes down what has been counted, e.g. as the screen goes off. */
    fun flush() {
        if (current != null) {
            stopCounting()
            since = SystemClock.elapsedRealtime()
        }
        save()
    }

    fun stop() {
        handler.removeCallbacks(tick)
        moveTo(null)
        save()
    }

    /** Adds the time since [since] to the app in front, if it's one being counted. */
    private fun stopCounting() {
        handler.removeCallbacks(tick)
        val pkg = current ?: return
        if (!limited(pkg)) return
        val ms = SystemClock.elapsedRealtime() - since
        if (ms > 0) unsaved[pkg] = (unsaved[pkg] ?: 0L) + ms
    }

    private fun pendingSeconds(): Map<String, Long> {
        val running = current?.takeIf { limited(it) }?.let { it to SystemClock.elapsedRealtime() - since }
        return (unsaved.keys + writing.keys + listOfNotNull(running?.first)).associateWith { pkg ->
            (writing[pkg] ?: 0L) + ((unsaved[pkg] ?: 0L) + (if (pkg == running?.first) running.second else 0L)) / 1000
        }
    }

    private fun save() {
        if (unsaved.isEmpty()) return
        val seconds = unsaved.mapValues { it.value / 1000 }.filterValues { it > 0 }
        // Keep the leftover milliseconds, so short visits still add up.
        unsaved.replaceAll { _, ms -> ms % 1000 }
        if (seconds.isEmpty()) return
        seconds.forEach { (pkg, s) -> writing[pkg] = (writing[pkg] ?: 0L) + s }
        val day = today()
        writeScope.launch { repo.addUsage(day, seconds) }
    }

    /** While a limited app is in front, look again when its time runs out, and at least each minute. */
    private fun scheduleTick(pkg: String) {
        handler.removeCallbacks(tick)
        val left = Limits.leastLeft(limits, pkg, todayUsage(), pendingSeconds())
        val wait = if (left == null) 60_000L else ((left - if (left > 60) 60 else 0) * 1000L).coerceIn(1_000L, 60_000L)
        handler.postDelayed(tick, wait)
    }

    private fun onTick() {
        val pkg = current ?: return
        flush()
        val left = Limits.leastLeft(limits, pkg, todayUsage(), pendingSeconds())
        if (left != null && left in 1..60) warn(pkg)
        check(pkg, arrived = false)
    }

    private fun warn(pkg: String) {
        if (warnedDay != today()) {
            warned.clear()
            warnedDay = today()
        }
        if (!warned.add(pkg)) return
        val label = AppCatalog.label(context, pkg)
        Toast.makeText(context, "about a minute of $label left today".lowercase(), Toast.LENGTH_SHORT).show()
    }

    private fun cover(intent: android.content.Intent) {
        handler.removeCallbacks(tick)
        runCatching { context.startActivity(intent) }
    }
}
