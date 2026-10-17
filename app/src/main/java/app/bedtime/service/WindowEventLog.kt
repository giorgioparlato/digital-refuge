package app.bedtime.service

import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

/**
 * A temporary test: what this phone reports when you move between screens, recorded only while the
 * test page has it switched on, kept only in memory, and gone when the app closes. It exists to learn
 * whether this phone's recent-apps view can be told apart from its home screen.
 */
object WindowEventLog {
    data class Entry(val at: Long, val pkg: String, val cls: String?, val text: String, val description: String?)

    private const val MAX = 80
    private const val RECORD_MS = 2 * 60_000L

    private val entries = ArrayDeque<Entry>()

    @Volatile
    private var until = 0L

    val recording: Boolean get() = SystemClock.elapsedRealtime() < until

    /** Records for the next two minutes, starting from an empty list. */
    @Synchronized
    fun start() {
        entries.clear()
        until = SystemClock.elapsedRealtime() + RECORD_MS
    }

    @Synchronized
    fun stop() {
        until = 0L
    }

    @Synchronized
    fun record(event: AccessibilityEvent) {
        if (!recording) return
        entries.addLast(
            Entry(
                at = System.currentTimeMillis(),
                pkg = event.packageName?.toString().orEmpty(),
                cls = event.className?.toString(),
                text = event.text.joinToString(" | "),
                description = event.contentDescription?.toString(),
            ),
        )
        while (entries.size > MAX) entries.removeFirst()
    }

    @Synchronized
    fun snapshot(): List<Entry> = entries.toList()
}
