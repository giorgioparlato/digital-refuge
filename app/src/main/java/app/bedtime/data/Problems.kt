package app.bedtime.data

import android.content.Context
import android.util.Log
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * What went wrong lately, kept in a small file so it can be read on the phone (settings › about &
 * backup › recent problems) and copied, since there's no other way to see it. Only what failed and
 * where: never what was on screen, in a note or in a notification.
 */
object Problems {
    const val MAX = 200
    private const val FILE = "problems.log"
    private const val TAG = "refuge"
    private val time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.systemDefault())

    @Volatile private var dir: File? = null
    private var lastWhat: String? = null
    private var lastAt = 0L

    /** Where the file lives; set once, when the app starts. */
    fun init(context: Context) {
        dir = context.applicationContext.filesDir
    }

    /** One line: when, [where], and what went wrong. Before [init] it only reaches Android's log. */
    fun note(where: String, error: Throwable? = null) {
        val now = System.currentTimeMillis()
        val line = line(now, where, error)
        Log.w(TAG, line, error)
        val file = dir?.let { File(it, FILE) } ?: return
        synchronized(this) {
            // The same failure again and again (a value read on every change) is written once a minute.
            val what = line.substringAfter(" · ")
            if (what == lastWhat && now - lastAt < 60_000L) return
            lastWhat = what
            lastAt = now
            runCatching {
                file.appendText(line + "\n")
                // Kept short: past twice the limit, the oldest half goes.
                if (file.length() > MAX * 400L) file.writeText(trimmed(file.readLines()).joinToString("\n", postfix = "\n"))
            }
        }
    }

    /** The lines written, newest first. */
    fun read(): List<String> = dir?.let { File(it, FILE) }?.takeIf { it.exists() }?.let { f -> runCatching { f.readLines() }.getOrNull() }.orEmpty().reversed()

    fun clear() {
        synchronized(this) { dir?.let { File(it, FILE).delete() } }
    }

    internal fun line(at: Long, where: String, error: Throwable?): String =
        listOfNotNull(time.format(Instant.ofEpochMilli(at)), where, error?.let { "${it.javaClass.simpleName}: ${it.message.orEmpty()}".take(300) })
            .joinToString(" · ")

    internal fun trimmed(lines: List<String>): List<String> = lines.takeLast(MAX)

    /** [block], and if it fails, a line in the log for it; null then. For the failures worth knowing about. */
    inline fun <T> attempt(where: String, block: () -> T): T? =
        try {
            block()
        } catch (e: Exception) {
            note(where, e)
            null
        }
}
