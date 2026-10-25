package app.bedtime.data

import android.content.Context
import android.net.Uri
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

/** A note kept in the app, written in Markdown. */
@Serializable
data class Note(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val createdAt: Long,
    val editedAt: Long = createdAt,
)

/** A text file of yours that quick notes can be added to, such as an Obsidian note, by the name you gave it. */
@Serializable
data class NoteTarget(val name: String, val uri: String)

object Notes {
    /** How many files quick notes can go to, besides the app's own notes. */
    const val MAX_TARGETS = 5

    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /** A quick note as a Markdown list item with when it was written: "- 2026-10-25 22:41 your words". */
    fun line(text: String, at: LocalDateTime): String =
        "- ${stamp.format(at)} ${text.trim().replace(Regex("\\s*\\n\\s*"), " ")}"

    /** [existing] with [line] added at the end, on a line of its own. */
    fun appended(existing: String, line: String): String =
        if (existing.isEmpty() || existing.endsWith("\n")) "$existing$line\n" else "$existing\n$line\n"

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
}
