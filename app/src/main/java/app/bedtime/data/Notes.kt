package app.bedtime.data

import kotlinx.coroutines.flow.first

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

/** A note kept in the app, written in Markdown; it can be pinned near the top, and given labels to find it by. */
@Serializable
data class Note(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val createdAt: Long,
    val editedAt: Long = createdAt,
    val pinned: Boolean = false,
    val labels: List<String> = emptyList(),
    /** When to be reminded of it, as epoch millis; null for never. */
    val remindAt: Long? = null,
)

/** A note in the trash: when it went, and where it was, to go back there. */
@Serializable
data class TrashedNote(val note: Note, val deletedAt: Long, val at: Int = 0)

/** Refuge notes kept in order, moved into one another, merged, pinned and labelled; deleted ones kept a month. */
object NoteShelf {
    /** How long a deleted note waits in the trash before it's gone. */
    const val TRASH_DAYS = 30L

    /** [trash] without those older than a month at [now]. */
    fun keptTrash(trash: List<TrashedNote>, now: Long): List<TrashedNote> = trash.filter { now - it.deletedAt < TRASH_DAYS * 86_400_000L }

    /** Days left before a trashed note goes, at least one while it's still there. */
    fun daysLeft(item: TrashedNote, now: Long): Long = (TRASH_DAYS - (now - item.deletedAt) / 86_400_000L).coerceAtLeast(1)

    /** The "why" answers first while pinned, then the to-do, then those pinned, then the rest, each in the order they were. */
    fun ordered(notes: List<Note>): List<Note> = notes.sortedBy {
        when {
            it.id == Why.NOTE_ID && it.pinned -> 0
            it.id == Notes.TODO_ID -> 1
            it.pinned -> 2
            else -> 3
        }
    }

    /** Only those with [label]; all of them with none. */
    fun labelled(notes: List<Note>, label: String?): List<Note> = if (label == null) notes else notes.filter { label in it.labels }

    /** Every label in use, A–Z, each once. */
    fun labels(notes: List<Note>): List<String> = notes.flatMap { it.labels }.distinctBy { it.lowercase() }.sortedBy { it.lowercase() }

    /**
     * [from] moved to the end of [into] and gone from the list: a single line as a list item (a to-do
     * in the to-do note, above those done), anything longer after a blank line. Its labels go with it.
     */
    fun movedInto(notes: List<Note>, from: String, into: String, at: Long): List<Note> {
        val source = notes.firstOrNull { it.id == from } ?: return notes
        val target = notes.firstOrNull { it.id == into }?.takeIf { from != into } ?: return notes
        val words = source.text.trim()
        val lines = words.lines().filter { it.isNotBlank() }
        val text = when {
            words.isEmpty() -> target.text
            target.id == Notes.TODO_ID && lines.size == 1 -> Todo.added(target.text, "- [ ] " + lines.single().trim().removePrefix("- ").removePrefix("[ ] "))
            lines.size == 1 -> Notes.appended(target.text, lines.single().trim().let { if (it.startsWith("- ") || it.startsWith("* ")) it else "- $it" })
            else -> target.text.trimEnd() + "\n\n" + words + "\n"
        }
        val moved = target.copy(text = text, editedAt = at, labels = (target.labels + source.labels).distinct())
        return notes.filterNot { it.id == from }.map { if (it.id == into) moved else it }
    }

    /** The notes [ids], in that order, made one: the first keeps its place, the rest follow it after blank lines. */
    fun merged(notes: List<Note>, ids: List<String>, at: Long): List<Note> {
        val chosen = ids.mapNotNull { id -> notes.firstOrNull { it.id == id } }
        if (chosen.size < 2) return notes
        val first = chosen.first()
        val one = first.copy(
            text = chosen.joinToString("\n\n") { it.text.trim() } + "\n",
            editedAt = at,
            pinned = chosen.any { it.pinned },
            labels = chosen.flatMap { it.labels }.distinct(),
        )
        val gone = chosen.drop(1).map { it.id }.toSet()
        return notes.filterNot { it.id in gone }.map { if (it.id == first.id) one else it }
    }

    fun pinned(notes: List<Note>, id: String, pinned: Boolean): List<Note> = notes.map { if (it.id == id) it.copy(pinned = pinned) else it }

    fun reminded(notes: List<Note>, id: String, at: Long?): List<Note> = notes.map { if (it.id == id) it.copy(remindAt = at) else it }

    /** The next reminder still to come after [now], if any. */
    fun nextReminder(notes: List<Note>, now: Long): Long? = notes.mapNotNull { it.remindAt }.filter { it > now }.minOrNull()

    /** Those due by [now], to be told about. */
    fun due(notes: List<Note>, now: Long): List<Note> = notes.filter { (it.remindAt ?: Long.MAX_VALUE) <= now }

    /**
     * When "remind me" can be, from [now]: in an hour, in three, this evening (while that's more than
     * an hour off), tomorrow morning, and next Monday morning. Each with when, as epoch millis.
     */
    fun reminderChoices(now: LocalDateTime): List<Pair<String, Long>> {
        val zone = java.time.ZoneId.systemDefault()
        fun millis(at: LocalDateTime) = at.atZone(zone).toInstant().toEpochMilli()
        val evening = now.toLocalDate().atTime(18, 0)
        val monday = now.toLocalDate().with(java.time.temporal.TemporalAdjusters.next(java.time.DayOfWeek.MONDAY)).atTime(9, 0)
        return listOfNotNull(
            "In an hour" to millis(now.plusHours(1)),
            "In 3 hours" to millis(now.plusHours(3)),
            ("This evening" to millis(evening)).takeIf { now.plusHours(1).isBefore(evening) },
            "Tomorrow morning" to millis(now.toLocalDate().plusDays(1).atTime(9, 0)),
            "Next week" to millis(monday),
        )
    }

    fun withLabels(notes: List<Note>, id: String, labels: List<String>): List<Note> =
        notes.map { if (it.id == id) it.copy(labels = labels.map { l -> l.trim() }.filter { l -> l.isNotEmpty() }.distinctBy { l -> l.lowercase() }) else it }
}

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

    /** The to-do note's id among the app's notes; it's kept first in the list. */
    const val TODO_ID = "todo"

    /** The note words saved from the dictionary look-up go in. */
    const val WORDS_ID = "words"

    /** A word's line in the words note: the word in bold, then a little of its meaning. */
    fun wordLine(word: String, meaning: String): String = "- **$word** — $meaning".trimEnd(' ', '—')

    /** [text] with [word]'s line added at the foot, or null if the word is there already. */
    fun withWord(text: String, word: String, meaning: String): String? =
        if (text.lines().any { it.startsWith("- **$word**", ignoreCase = true) }) null
        else text.trimEnd() + "\n" + wordLine(word, meaning) + "\n"

    /** The to-do note as a place quick notes and the search can add to, each as an unticked item. */
    val TODO = NoteTarget("To-do", "refuge:todo", NoteStyle.TASK, withDate = false)

    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /** Whether a note target's file or folder [uri] lies in [folder] (your Obsidian vault), by the paths the two uris name. */
    fun inFolder(uri: String, folder: String): Boolean {
        fun path(u: String) = java.net.URLDecoder.decode(u.substringAfterLast('/'), "UTF-8").trimEnd('/')
        val inside = path(uri)
        val root = path(folder)
        return root.isNotEmpty() && (inside == root || inside.startsWith("$root/"))
    }

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

    private const val WAITING = "syncWhenUnlocked"

    private fun prefs(context: Context) = context.getSharedPreferences("notes", Context.MODE_PRIVATE)

    /** A note went into one of your files on the lock screen: its "then" shortcut runs once you're in. */
    fun syncWhenUnlocked(context: Context) {
        prefs(context).edit().putBoolean(WAITING, true).apply()
    }

    /** Just unlocked: the shortcut a note on the lock screen left waiting, if one did. */
    suspend fun syncIfWaiting(context: Context, repo: Repository) {
        val prefs = prefs(context)
        if (!prefs.getBoolean(WAITING, false)) return
        prefs.edit().remove(WAITING).apply()
        repo.settings.first().afterNoteShortcut?.let { Shortcuts.start(context, it, passing = true) }
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
        if (target.uri == TODO.uri) {
            repo.addTodo(words)
            return "added to your to-do"
        }
        val at = LocalDateTime.now()
        val line = line(words, at, target.style, target.withDate)
        val added = withContext(Dispatchers.IO) {
            if (target.daily) appendDaily(context, Uri.parse(target.uri), target.dailyFormat, at.toLocalDate(), line) else append(context, Uri.parse(target.uri), line)
        }
        if (!added) repo.addNote(words)
        // Then, say, a sync app's "Sync now", so the file reaches your other devices. From the lock
        // screen it waits until you're in: there it would only sit behind the lock, or ask to unlock.
        val locked = context.getSystemService(android.app.KeyguardManager::class.java)?.isKeyguardLocked == true
        if (added) repo.settings.first().afterNoteShortcut?.let { if (locked) syncWhenUnlocked(context) else Shortcuts.start(context, it, passing = true) }
        val name = target.name.lowercase()
        return if (added) "added to $name" else "couldn't reach $name, so it's in refuge notes"
    }
}

/** One line of a note as the note widget shows it: a box (ticked or not), a bullet, or plain words. */
data class NoteWidgetLine(val index: Int, val text: String, val box: Boolean? = null, val bullet: Boolean = false, val indent: Int = 0)

/** A note read for its widget: its title, its first lines, and ticking a box from there. */
object NoteWidget {
    private val box = Regex("""^(\s*)[-*+]\s+\[([ xX])]\s+(.*)$""")
    private val bullet = Regex("""^(\s*)[-*+]\s+(.*)$""")
    private val heading = Regex("""^#{1,6}\s+(.*)$""")

    /** The note's title: its first line, without the heading marks. */
    fun title(text: String): String = text.lines().firstOrNull { it.isNotBlank() }?.let { heading.find(it.trim())?.groupValues?.get(1) ?: it.trim() }.orEmpty()

    /** The lines after the title that have something in them, at most [max], each with its place in the note. */
    fun lines(text: String, max: Int): List<NoteWidgetLine> {
        val all = text.lines()
        val titleAt = all.indexOfFirst { it.isNotBlank() }
        return all.withIndex()
            .filter { (i, line) -> i != titleAt && line.isNotBlank() }
            .take(max)
            .map { (i, line) ->
                box.find(line)?.let { NoteWidgetLine(i, it.groupValues[3], box = it.groupValues[2].isNotBlank(), indent = it.groupValues[1].length / 2) }
                    ?: bullet.find(line)?.let { NoteWidgetLine(i, it.groupValues[2], bullet = true, indent = it.groupValues[1].length / 2) }
                    ?: NoteWidgetLine(i, heading.find(line.trim())?.groupValues?.get(1) ?: line.trim())
            }
    }

    /** [text] with the box on line [index] ticked, or unticked. */
    fun toggled(text: String, index: Int): String {
        val lines = text.lines().toMutableList()
        if (index !in lines.indices) return text
        lines[index] = lines[index].replace(Regex("""\[([ xX])]""")) { if (it.groupValues[1].isBlank()) "[x]" else "[ ]" }
        return lines.joinToString("\n")
    }
}

/** A to-do done: its lines (the item and any under it), what it says, and the day it was done, if known. */
data class DoneTodo(val lines: List<String>, val text: String, val date: LocalDate?)

/**
 * The to-do note's own ways. A ticked item goes to the done ones kept together at the end of the note,
 * newest first, with the day it was done ("✅ 2026-10-31", as Obsidian's Tasks writes it); unticked it
 * comes back to the end of the list, without it. A new one goes in above the done. Items under an
 * item move with it; one ticked under another stays among its own, dated.
 */
object Todo {
    private val item = Regex("""^(\s*)[-*+]\s+\[([ xX])]\s*(.*)$""")
    private val listLine = Regex("""^\s*[-*+]\s""")
    private val doneDate = Regex("""\s+✅\s*(\d{4}-\d{2}-\d{2})\s*$""")

    private fun indent(line: String) = line.length - line.trimStart().length

    /** In a list: an item, or anything indented under one. */
    private fun inList(line: String) = line.isNotBlank() && (listLine.containsMatchIn(line) || indent(line) > 0)

    private fun ticked(line: String) = item.find(line)?.groupValues?.get(2)?.isNotBlank() == true

    /** Where the item at [at] ends: its own line and those indented under it. */
    private fun extent(lines: List<String>, at: Int): Int {
        var end = at + 1
        while (end < lines.size && lines[end].isNotBlank() && indent(lines[end]) > indent(lines[at])) end++
        return end
    }

    /** The lines from [at]'s list holding its siblings: the same indent, under the same parent, unbroken. */
    private fun siblings(lines: List<String>, at: Int, depth: Int): IntRange {
        var start = at
        while (start > 0 && inList(lines[start - 1]) && indent(lines[start - 1]) >= depth) start--
        var end = at
        while (end < lines.size && inList(lines[end]) && indent(lines[end]) >= depth) end++
        return start until end
    }

    private fun done(lines: List<String>): DoneTodo {
        val first = lines.first()
        val date = doneDate.find(first)?.groupValues?.get(1)?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val text = item.find(first.replace(doneDate, ""))?.groupValues?.get(3)?.trim().orEmpty()
        return DoneTodo(lines, text, date)
    }

    /** The note's to-dos still to do (and all else in it), and those done, as they're kept: newest first. */
    fun split(text: String): Pair<String, List<DoneTodo>> {
        val lines = text.lines()
        val open = mutableListOf<String>()
        val done = mutableListOf<DoneTodo>()
        var i = 0
        while (i < lines.size) {
            if (indent(lines[i]) == 0 && ticked(lines[i])) {
                val end = extent(lines, i)
                done += done(lines.subList(i, end))
                i = end
            } else {
                open += lines[i]
                i++
            }
        }
        return open.joinToString("\n").trimEnd() to done
    }

    /** The note again: what's still to do, then the done ones after a blank line. */
    fun join(open: String, done: List<DoneTodo>): String {
        val head = open.trimEnd()
        if (done.isEmpty()) return if (head.isEmpty()) "" else "$head\n"
        return (if (head.isEmpty()) "" else "$head\n\n") + done.flatMap { it.lines }.joinToString("\n") + "\n"
    }

    /** [text] with the to-do on line [index] ticked (to the done ones, dated [today]) or unticked (back to the list, undated). */
    fun toggled(text: String, index: Int, today: LocalDate): String {
        val lines = text.lines().toMutableList()
        val m = lines.getOrNull(index)?.let { item.find(it) } ?: return text
        val depth = m.groupValues[1].length
        val ticking = m.groupValues[2].isBlank()
        val moving = lines.subList(index, extent(lines, index)).toMutableList()
        moving[0] = if (ticking) {
            moving[0].replaceFirst(Regex("""\[ ]"""), "[x]").trimEnd() + " ✅ " + today
        } else {
            moving[0].replaceFirst(Regex("""\[[xX]]"""), "[ ]").replace(doneDate, "")
        }
        repeat(moving.size) { lines.removeAt(index) }
        if (depth == 0) {
            val (open, done) = split(lines.joinToString("\n"))
            return if (ticking) join(open, listOf(done(moving)) + done) else join(addedLines(open, moving), done)
        }
        // One under another: it stays among its own, done ones after those to do.
        val anchor = listOf(index, index - 1).firstOrNull { it in lines.indices && inList(lines[it]) && indent(lines[it]) >= depth }
        if (anchor == null) {
            lines.addAll(index, moving)
            return lines.joinToString("\n")
        }
        val around = siblings(lines, anchor, depth)
        val tops = around.filter { indent(lines[it]) == depth }
        val to = if (ticking) around.last + 1 else tops.firstOrNull { ticked(lines[it]) } ?: (around.last + 1)
        lines.addAll(to, moving)
        return lines.joinToString("\n")
    }

    /** [open] with [moving] at the end of its last list, or at the end. */
    private fun addedLines(open: String, moving: List<String>): String {
        val lines = open.lines().toMutableList()
        val last = lines.indexOfLast { inList(it) && indent(it) == 0 }
        if (last < 0) return (if (open.isBlank()) "" else open.trimEnd() + "\n") + moving.joinToString("\n")
        lines.addAll(siblings(lines, last, 0).last + 1, moving)
        return lines.joinToString("\n")
    }

    /** [text] with [line], a new to-do, at the end of the list, above those done. */
    fun added(text: String, line: String): String {
        val (open, done) = split(text)
        return join(addedLines(open, listOf(line)), done)
    }

    /** [text] without its done ones. */
    fun withoutDone(text: String): String = join(split(text).first, emptyList())

    /**
     * The done ones folded by when: this week, last week, earlier this month, last month, then each
     * month of this year by name, and each year before; those without a date last.
     */
    fun groups(done: List<DoneTodo>, today: LocalDate): List<Pair<String, List<DoneTodo>>> {
        val week = today.with(java.time.temporal.TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY))
        val month = today.withDayOfMonth(1)
        fun group(date: LocalDate?): String = when {
            date == null -> "Undated"
            !date.isBefore(week) -> "This week"
            !date.isBefore(week.minusWeeks(1)) -> "Last week"
            !date.isBefore(month) -> "Earlier this month"
            !date.isBefore(month.minusMonths(1)) -> "Last month"
            date.year == today.year -> date.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
            else -> date.year.toString()
        }
        val order = done.sortedWith(compareBy<DoneTodo> { it.date == null }.thenByDescending { it.date })
        return order.groupBy { group(it.date) }.toList()
    }
}
