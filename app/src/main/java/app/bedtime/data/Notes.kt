package app.bedtime.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/** A note kept in the app, written in Markdown. */
@Serializable
data class Note(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val createdAt: Long,
    val editedAt: Long = createdAt,
)

/** How a quick note is written into a file: a list item, a to-do, or a plain line. */
@Serializable
enum class NoteStyle { BULLET, TASK, PLAIN }

/**
 * A text file of yours that quick notes can be added to, such as an Obsidian note, by the name you
 * gave it, and how each note is written there.
 */
@Serializable
data class NoteTarget(
    val name: String,
    val uri: String,
    val style: NoteStyle = NoteStyle.BULLET,
    /** Whether each note starts with the date and time it was written. */
    val withDate: Boolean = true,
    /** [uri] is a folder, and notes go to the day's own note in it, as Obsidian's daily notes. */
    val daily: Boolean = false,
    /** The daily note's name, in Obsidian's date format; may include folders, like "YYYY/MM/YYYY-MM-DD". */
    val dailyFormat: String = "YYYY-MM-DD",
)

object Notes {
    /** How many files quick notes can go to, besides the app's own notes. */
    const val MAX_TARGETS = 5

    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /**
     * A quick note as one Markdown line: "- 2026-10-25 22:41 your words" by default, or a to-do
     * ("- [ ] …") or plain words, with or without the date and time.
     */
    fun line(text: String, at: LocalDateTime, style: NoteStyle = NoteStyle.BULLET, withDate: Boolean = true): String {
        val prefix = when (style) {
            NoteStyle.BULLET -> "- "
            NoteStyle.TASK -> "- [ ] "
            NoteStyle.PLAIN -> ""
        }
        val date = if (withDate) "${stamp.format(at)} " else ""
        return prefix + date + text.trim().replace(Regex("\\s*\\n\\s*"), " ")
    }

    /** [existing] with [line] added at the end, on a line of its own. */
    fun appended(existing: String, line: String): String =
        if (existing.isEmpty() || existing.endsWith("\n")) "$existing$line\n" else "$existing\n$line\n"

    /**
     * Today's note's path within the daily-notes folder, from Obsidian's date format: "YYYY-MM-DD"
     * gives "2026-10-25.md", "YYYY/MM/YYYY-MM-DD" gives "2026/10/2026-10-25.md". Text in [brackets] is
     * kept as written, as in Obsidian.
     */
    fun dailyPath(format: String, date: LocalDate): String {
        val tokens = listOf("YYYY" to "yyyy", "YY" to "yy", "MMMM" to "MMMM", "MMM" to "MMM", "MM" to "MM", "M" to "M", "dddd" to "EEEE", "ddd" to "EEE", "DD" to "dd", "D" to "d")
        val out = StringBuilder()
        var i = 0
        val pattern = format.trim().ifEmpty { "YYYY-MM-DD" }
        while (i < pattern.length) {
            if (pattern[i] == '[') {
                val close = pattern.indexOf(']', i)
                val literal = if (close > i) pattern.substring(i + 1, close) else pattern.substring(i + 1)
                out.append(literal)
                i = if (close > i) close + 1 else pattern.length
                continue
            }
            val token = tokens.firstOrNull { pattern.startsWith(it.first, i) }
            if (token != null) {
                out.append(DateTimeFormatter.ofPattern(token.second, Locale.getDefault()).format(date))
                i += token.first.length
            } else {
                out.append(pattern[i])
                i++
            }
        }
        return out.toString().trim('/') + ".md"
    }

    /**
     * Adds [line] to today's note in the daily-notes folder [tree], making the note (and any folders in
     * its name) if it isn't there yet. False if the folder can't be reached any more.
     */
    fun appendDaily(context: Context, tree: Uri, format: String, date: LocalDate, line: String): Boolean = runCatching {
        val resolver = context.contentResolver
        val parts = dailyPath(format, date).split('/').filter { it.isNotEmpty() }
        var dir = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        parts.dropLast(1).forEach { folder ->
            dir = child(context, tree, dir, folder)
                ?: DocumentsContract.createDocument(resolver, dir, DocumentsContract.Document.MIME_TYPE_DIR, folder)
                ?: return false
        }
        val name = parts.last()
        // A plain binary type keeps the name exactly as given, where "text/markdown" could gain a ".txt".
        val note = child(context, tree, dir, name) ?: DocumentsContract.createDocument(resolver, dir, "application/octet-stream", name) ?: return false
        append(context, note, line)
    }.getOrDefault(false)

    /** The document called [name] directly inside [dir], if there is one. */
    private fun child(context: Context, tree: Uri, dir: Uri, name: String): Uri? {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getDocumentId(dir))
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        return context.contentResolver.query(children, columns, null, null, null)?.use { c ->
            while (c.moveToNext()) if (c.getString(1) == name) return@use DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0))
            null
        }
    }

    /**
     * Adds [line] to the end of the file at [uri]. Tries a plain append; a provider that won't append
     * gets the whole file back with the line added. False if the file can't be reached any more.
     */
    fun append(context: Context, uri: Uri, line: String): Boolean {
        val resolver = context.contentResolver
        val existing = runCatching { resolver.openInputStream(uri)?.use { it.readBytes().decodeToString() } }.getOrNull() ?: return false
        val addition = appended(existing, line).removePrefix(existing)
        val appended = runCatching {
            resolver.openOutputStream(uri, "wa")?.use { it.write(addition.toByteArray()) } != null
        }.getOrDefault(false)
        if (appended) return true
        return runCatching {
            resolver.openOutputStream(uri, "wt")?.use { it.write(appended(existing, line).toByteArray()) } != null
        }.getOrDefault(false)
    }

    /**
     * Saves a quick note: in the app's own notes when [target] is null, otherwise at the end of that file
     * (or today's daily note). If the file can't be reached it waits in the app's notes, so nothing is
     * lost. Returns what to tell you.
     */
    suspend fun save(context: Context, repo: Repository, words: String, target: NoteTarget?): String {
        if (target == null) {
            repo.addNote(words)
            return "saved in refuge notes"
        }
        val at = LocalDateTime.now()
        val line = line(words, at, target.style, target.withDate)
        val added = withContext(Dispatchers.IO) {
            if (target.daily) appendDaily(context, Uri.parse(target.uri), target.dailyFormat, at.toLocalDate(), line) else append(context, Uri.parse(target.uri), line)
        }
        if (!added) repo.addNote(words)
        val name = target.name.lowercase()
        return if (added) "added to $name" else "couldn't reach $name, so it's in refuge notes"
    }
}
