package app.bedtime.engine

import java.util.concurrent.ConcurrentHashMap

/**
 * Apps refuge itself just opened for you, let through for a moment even if a session shuts them:
 * a sync app's shortcut run after a quick note, so what you wrote is sent on.
 */
object Passes {
    private const val FOR_MS = 15_000L
    private val until = ConcurrentHashMap<String, Long>()

    fun grant(pkg: String, now: Long = System.currentTimeMillis()) {
        until[pkg] = now + FOR_MS
    }

    fun has(pkg: String, now: Long): Boolean = (until[pkg] ?: 0L) > now
}
