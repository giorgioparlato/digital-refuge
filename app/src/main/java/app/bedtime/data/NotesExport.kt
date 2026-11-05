package app.bedtime.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Your refuge notes as Markdown files in a folder of yours (an Obsidian vault, say): a file a note,
 * named by its title, and the microjournal a file a day in a folder of its own. Written over each
 * time, never read back: nothing in the folder changes refuge. The trash stays out.
 */
object NotesExport {
    /** The folder, inside the one chosen, that the microjournal's days go in. */
    const val JOURNAL_FOLDER = "microjournal"

    /** Characters a file name can't have, on a phone or a computer. */
    private val NOT_IN_NAMES = Regex("""[\\/:*?"<>|#^\[\]]""")

    /** A note's file name from its title, "films to see.md"; one without a title by its day. */
    fun fileName(note: Note): String {
        val title = NoteWidget.title(note.text).replace(Regex("""[*_`~]"""), "").replace(NOT_IN_NAMES, " ").replace(Regex("""\s+"""), " ").trim().take(60).trim()
        val day = java.time.Instant.ofEpochMilli(note.createdAt).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        return (title.ifEmpty { "note ${day.format(DateTimeFormatter.ISO_LOCAL_DATE)}" }).lowercase() + ".md"
    }

    /** Each note with its file, the oldest first so a name stays with the same note; a second "films" is "films 2". */
    fun named(notes: List<Note>): List<Pair<String, Note>> {
        val taken = mutableMapOf<String, Int>()
        return notes.sortedBy { it.createdAt }.map { note ->
            val name = fileName(note)
            val n = (taken[name] ?: 0) + 1
            taken[name] = n
            (if (n == 1) name else name.removeSuffix(".md") + " $n.md") to note
        }
    }

    /** A recording's name in a line, as Obsidian embeds it, so it plays there when the file is in the vault. */
    fun withEmbeds(text: String): String =
        text.replace(Regex("""(?<!\[\[)(\d{4}-\d{2}-\d{2} \d{4,6}\.m4a)"""), "![[$1]]")

    /** A day's microjournal as a file: its title, then what was written. */
    fun journalFile(entry: JournalEntry, today: LocalDate): Pair<String, String> =
        "${LocalDate.ofEpochDay(entry.day).format(DateTimeFormatter.ISO_LOCAL_DATE)}.md" to "# ${Journal.title(entry, today)}\n\n${withEmbeds(entry.text.trim())}\n"

    /** How many notes and days went out, or null if the folder couldn't be written to. */
    data class Done(val notes: Int, val days: Int)

    fun write(context: Context, tree: Uri, notes: List<Note>, journal: List<JournalEntry>, today: LocalDate): Done? = runCatching {
        val top = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val here = children(context, tree, top)
        named(notes).forEach { (name, note) -> put(context, top, here[name], name, withEmbeds(note.text.trimEnd()) + "\n") ?: return null }
        if (journal.isNotEmpty()) {
            val folder = here[JOURNAL_FOLDER]
                ?: DocumentsContract.createDocument(context.contentResolver, top, DocumentsContract.Document.MIME_TYPE_DIR, JOURNAL_FOLDER)
                ?: return null
            val days = children(context, tree, folder)
            journal.forEach { entry ->
                val (name, text) = journalFile(entry, today)
                put(context, folder, days[name], name, text) ?: return null
            }
        }
        Done(notes.size, journal.size)
    }.getOrNull()

    /** [text] into [existing], written over, or into a new file called [name] in [folder]. */
    private fun put(context: Context, folder: Uri, existing: Uri?, name: String, text: String): Uri? {
        val resolver = context.contentResolver
        // A plain binary type keeps the name exactly as given, where "text/markdown" could gain a ".txt".
        val file = existing ?: DocumentsContract.createDocument(resolver, folder, "application/octet-stream", name) ?: return null
        resolver.openOutputStream(file, "wt")?.use { it.write(text.toByteArray()) } ?: return null
        return file
    }

    /** What's directly in [folder], by name. */
    private fun children(context: Context, tree: Uri, folder: Uri): Map<String, Uri> {
        val list = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getDocumentId(folder))
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        return context.contentResolver.query(list, columns, null, null, null)?.use { c ->
            buildMap { while (c.moveToNext()) put(c.getString(1), DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0))) }
        }.orEmpty()
    }
}
