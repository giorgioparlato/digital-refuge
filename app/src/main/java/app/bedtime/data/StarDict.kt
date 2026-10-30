package app.bedtime.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.zip.GZIPInputStream

/** What a stretch of an entry is: a grammar label ("adj."), something bold (the headword), an example. */
enum class MarkKind { LABEL, STRONG, EXAMPLE, LINK }

/** A stretch of an entry's [Definition.text], from [start] to [end], and what it is; a link, to [target]. */
data class Mark(val start: Int, val end: Int, val kind: MarkKind, val target: String? = null)

/** An entry as text, with what in it is a label, bold or an example. */
data class Styled(val text: String, val marks: List<Mark> = emptyList())

/** One word's entry: the word as the dictionary has it, which dictionary, what it says, and how it's marked. */
data class Definition(val word: String, val dictionary: String, val text: String, val marks: List<Mark> = emptyList())

/**
 * A StarDict dictionary kept in the app's own storage: its name, its index (each word, where its entry
 * starts and how long it is) and its entries' file. Read only on this phone; nothing is fetched.
 */
class StarDict(
    val name: String,
    private val words: List<String>,
    private val offsets: LongArray,
    private val sizes: IntArray,
    private val dict: File,
    private val types: String?,
    /** Its .syn, when it has one: other forms of words ("ran", "running"), each leading to its entry's place in [words]. */
    private val synonyms: List<String> = emptyList(),
    private val synonymOf: IntArray = IntArray(0),
) {
    /** The entry for [word], or for the word it's a form of, matched without regard to case; null if it isn't in here. */
    fun define(word: String): Definition? {
        val w = word.trim()
        val at = find(words, w).takeIf { it >= 0 } ?: find(synonyms, w).takeIf { it >= 0 }?.let { synonymOf[it] }?.takeIf { it in words.indices } ?: return null
        return read(at).let { Definition(words[at], name, it.text, it.marks) }
    }

    /** Up to [limit] words starting with [prefix], in the dictionary's order. */
    fun startingWith(prefix: String, limit: Int = 8): List<String> {
        val p = prefix.trim()
        if (p.isEmpty()) return emptyList()
        var i = lowerBound(words, p)
        val out = mutableListOf<String>()
        while (i < words.size && out.size < limit && words[i].startsWith(p, ignoreCase = true)) {
            if (out.none { it.equals(words[i], ignoreCase = true) }) out += words[i]
            i++
        }
        return out
    }

    private fun find(list: List<String>, word: String): Int {
        val i = lowerBound(list, word)
        return if (i < list.size && list[i].equals(word, ignoreCase = true)) i else -1
    }

    /** The first index in [list] whose word isn't before [word], comparing as StarDict sorts: case aside first. */
    private fun lowerBound(list: List<String>, word: String): Int {
        var lo = 0
        var hi = list.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (compare(list[mid], word) < 0) lo = mid + 1 else hi = mid
        }
        return lo
    }

    private fun read(at: Int): Styled {
        val bytes = ByteArray(sizes[at])
        RandomAccessFile(dict, "r").use { f ->
            f.seek(offsets[at])
            f.readFully(bytes)
        }
        return StarDicts.styled(bytes, types)
    }

    private fun compare(a: String, b: String): Int = StarDicts.compare(a, b)
}

/**
 * Your dictionaries: StarDict files from a folder you pick, copied into the app (unpacked, so a word
 * is found at once) and read from there, so the folder itself isn't needed afterwards.
 */
object StarDicts {
    private const val DIR = "dictionaries"

    @Volatile private var loaded: List<StarDict>? = null

    private fun root(context: Context) = File(context.filesDir, DIR)

    /** The dictionaries kept, by name; read once, then held. */
    fun all(context: Context): List<StarDict> = loaded ?: synchronized(this) {
        loaded ?: (root(context).listFiles().orEmpty().filter { it.isDirectory }.sortedBy { it.name }.mapNotNull { open(it) }).also { loaded = it }
    }

    /** The dictionaries to look in: the one named [only], or all of them when it's null (or gone). */
    private fun chosen(context: Context, only: String?): List<StarDict> = all(context).let { all -> all.filter { it.name == only }.ifEmpty { all } }

    /** The first entry for [word] (or the word it's a form of, where the dictionary lists forms) in each dictionary that has it; with [only], in that one alone. */
    fun define(context: Context, word: String, only: String? = null): List<Definition> = chosen(context, only).mapNotNull { it.define(word) }

    /**
     * An entry in a line, for the search: what it says, its lines run together, without a first line
     * that only repeats the word, perhaps with its pronunciation ("katt /kat/"), and cut at about
     * [length] letters. A first line with more than that ("run: to move fast") stays.
     */
    fun preview(d: Definition, length: Int = 90): String {
        val lines = d.text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val onlyTheWord = lines.size > 1 && lines.first().startsWith(d.word, ignoreCase = true) &&
            lines.first().drop(d.word.length).replace(PRONUNCIATION, "").isBlank()
        val rest = if (onlyTheWord) lines.drop(1) else lines
        val line = rest.joinToString(" · ")
        return if (line.length <= length) line else line.take(length).substringBeforeLast(' ') + "…"
    }

    private val PRONUNCIATION = Regex("""/[^/]*/|\[[^\]]*]""")

    /** Words starting with [prefix], from every dictionary (or [only] that one), each once. */
    fun suggest(context: Context, prefix: String, limit: Int = 8, only: String? = null): List<String> =
        chosen(context, only).flatMap { it.startingWith(prefix, limit) }.distinctBy { it.lowercase() }.take(limit)

    /**
     * StarDict's own order: the words' UTF-8 bytes, A–Z taken as a–z and nothing else changed. Its
     * index is sorted so, and searching it any other way would miss words with accents.
     */
    internal fun compare(a: String, b: String): Int {
        val x = a.toByteArray(Charsets.UTF_8)
        val y = b.toByteArray(Charsets.UTF_8)
        for (i in 0 until minOf(x.size, y.size)) {
            val p = lower(x[i])
            val q = lower(y[i])
            if (p != q) return p - q
        }
        return x.size - y.size
    }

    private fun lower(b: Byte): Int {
        val v = b.toInt() and 0xFF
        return if (v in 'A'.code..'Z'.code) v + 32 else v
    }

    /** The dictionaries' names, to show which are kept. */
    fun names(context: Context): List<String> = all(context).map { it.name }

    /** Takes one away. */
    fun remove(context: Context, name: String) {
        root(context).listFiles().orEmpty().firstOrNull { it.isDirectory && readIfo(File(it, "dict.ifo"))["bookname"] == name }?.deleteRecursively()
        loaded = null
    }

    /**
     * Copies every dictionary in the folder [tree] (a .ifo with its .idx and .dict, each perhaps
     * compressed) into the app. Returns how many were added.
     */
    fun import(context: Context, tree: Uri): Int {
        val files = children(context, tree)
        var added = 0
        files.keys.filter { it.endsWith(".ifo") }.forEach { ifo ->
            val base = ifo.removeSuffix(".ifo")
            val idx = listOf("$base.idx", "$base.idx.gz").firstOrNull { it in files } ?: return@forEach
            val dict = listOf("$base.dict", "$base.dict.dz").firstOrNull { it in files } ?: return@forEach
            val syn = listOf("$base.syn", "$base.syn.dz").firstOrNull { it in files }
            val resolver = context.contentResolver
            // Kept under its own title, not its file's name: two dictionaries both called "dict" must not
            // overwrite each other, and adding the same one again replaces it.
            val title = resolver.openInputStream(files.getValue(ifo))?.use { stream ->
                stream.bufferedReader().readLines().firstOrNull { it.startsWith("bookname=") }?.substringAfter('=')?.trim()
            }?.takeIf { it.isNotEmpty() } ?: base
            val into = File(root(context), title.replace(Regex("[^A-Za-z0-9._-]"), "_"))
            into.deleteRecursively()
            into.mkdirs()
            try {
                resolver.openInputStream(files.getValue(ifo))?.use { copy(it, File(into, "dict.ifo")) }
                resolver.openInputStream(files.getValue(idx))?.use { copy(unpacked(it, idx), File(into, "dict.idx")) }
                resolver.openInputStream(files.getValue(dict))?.use { copy(unpacked(it, dict), File(into, "dict.dict")) }
                syn?.let { name -> resolver.openInputStream(files.getValue(name))?.use { copy(unpacked(it, name), File(into, "dict.syn")) } }
                if (open(into) != null) added++ else into.deleteRecursively()
            } catch (e: Exception) {
                Problems.note("adding the dictionary $base", e)
                into.deleteRecursively()
            }
        }
        loaded = null
        return added
    }

    /** The file's own contents: .gz and .dz (dictzip, a gzip that can be read in pieces) unpacked whole. */
    private fun unpacked(input: InputStream, name: String): InputStream =
        if (name.endsWith(".gz") || name.endsWith(".dz")) GZIPInputStream(input) else input

    private fun copy(input: InputStream, to: File) {
        to.outputStream().use { input.copyTo(it) }
    }

    /** A kept dictionary, or null if its files aren't whole. */
    internal fun open(dir: File): StarDict? = runCatching {
        val info = readIfo(File(dir, "dict.ifo"))
        val wide = info["idxoffsetbits"] == "64"
        val (words, offsets, sizes) = readIdx(File(dir, "dict.idx").readBytes(), wide)
        val (synonyms, synonymOf) = File(dir, "dict.syn").takeIf { it.exists() }?.let { readSyn(it.readBytes()) } ?: (emptyList<String>() to IntArray(0))
        StarDict(info["bookname"] ?: dir.name, words, offsets, sizes, File(dir, "dict.dict"), info["sametypesequence"], synonyms, synonymOf)
    }.getOrNull()

    /** The forms list: each form (UTF-8, ended by a zero), then the place of its word in the index. */
    internal fun readSyn(bytes: ByteArray): Pair<List<String>, IntArray> {
        val forms = ArrayList<String>()
        val places = ArrayList<Int>()
        var i = 0
        while (i < bytes.size) {
            var end = i
            while (end < bytes.size && bytes[end] != 0.toByte()) end++
            if (end + 5 > bytes.size) break
            forms += String(bytes, i, end - i, Charsets.UTF_8)
            places += ((bytes[end + 1].toInt() and 0xFF) shl 24) or ((bytes[end + 2].toInt() and 0xFF) shl 16) or ((bytes[end + 3].toInt() and 0xFF) shl 8) or (bytes[end + 4].toInt() and 0xFF)
            i = end + 5
        }
        return forms to places.toIntArray()
    }

    internal fun readIfo(file: File): Map<String, String> =
        file.takeIf { it.exists() }?.readLines()?.mapNotNull { line -> line.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0].trim() to it[1].trim() } }?.toMap().orEmpty()

    /** The index: each word (UTF-8, ended by a zero), then where its entry starts and its length. */
    internal fun readIdx(bytes: ByteArray, wide: Boolean): Triple<List<String>, LongArray, IntArray> {
        val words = ArrayList<String>()
        val offsets = ArrayList<Long>()
        val sizes = ArrayList<Int>()
        var i = 0
        fun int(at: Int) = ((bytes[at].toInt() and 0xFF) shl 24) or ((bytes[at + 1].toInt() and 0xFF) shl 16) or ((bytes[at + 2].toInt() and 0xFF) shl 8) or (bytes[at + 3].toInt() and 0xFF)
        while (i < bytes.size) {
            var end = i
            while (end < bytes.size && bytes[end] != 0.toByte()) end++
            if (end + (if (wide) 13 else 9) > bytes.size) break
            words += String(bytes, i, end - i, Charsets.UTF_8)
            var at = end + 1
            offsets += if (wide) (int(at).toLong() shl 32) or (int(at + 4).toLong() and 0xFFFFFFFFL) else int(at).toLong() and 0xFFFFFFFFL
            at += if (wide) 8 else 4
            sizes += int(at)
            i = at + 4
        }
        return Triple(words, offsets.toLongArray(), sizes.toIntArray())
    }

    /**
     * What a tap at [offset] in an entry looks up: a link's word, or else the word under it (letters,
     * with an inner hyphen or apostrophe). Null between words.
     */
    fun wordAt(text: String, marks: List<Mark>, offset: Int): String? {
        marks.firstOrNull { it.kind == MarkKind.LINK && offset in it.start until it.end }?.target?.let { return it }
        fun part(c: Char) = c.isLetter() || c == '-' || c == '\''
        if (offset !in text.indices || !text[offset].isLetter()) return null
        var start = offset
        while (start > 0 && part(text[start - 1])) start--
        var end = offset + 1
        while (end < text.length && part(text[end])) end++
        return text.substring(start, end).trim('-', '\'').takeIf { it.length >= 2 }
    }

    /** An entry as plain text: [styled] without its marks. */
    internal fun plain(bytes: ByteArray, types: String?): String = styled(bytes, types).text

    /**
     * An entry as text with its marks. With [types] (one letter per field: m plain, h html, x xdxf,
     * g pango…) the fields are the entry itself, split by zeros, the last taking the rest; without,
     * each starts with its letter. Markup is read for what it marks and taken out; plain text is kept
     * as written. Then grammar labels are found in either: "<adj>", "(n.)", or "n" opening a line.
     */
    internal fun styled(bytes: ByteArray, types: String?): Styled {
        val fields = fields(bytes, types)
        val out = StringBuilder()
        val marks = mutableListOf<Mark>()
        fields.forEach { (type, text) ->
            if (out.isNotEmpty()) newline(out)
            if (type in "hxgk") markup(text, out, marks) else plainWithLinks(text.replace('\u0000', '\n'), out, marks)
        }
        // Trailing spaces off each line, runs of blank lines down to one, the ends trimmed; marks follow.
        val raw = out.toString()
        val keep = BooleanArray(raw.length) { true }
        raw.lines().fold(0) { at, line ->
            val trimmed = line.trimEnd().length
            for (i in at + trimmed until at + line.length) keep[i] = false
            at + line.length + 1
        }
        var newlines = 0
        for (i in raw.indices) {
            if (!keep[i]) continue
            if (raw[i] == '\n') { newlines++; if (newlines > 2) keep[i] = false } else newlines = 0
        }
        val first = raw.indices.firstOrNull { keep[it] && !raw[it].isWhitespace() } ?: return Styled("")
        val last = raw.indices.last { keep[it] && !raw[it].isWhitespace() }
        val map = IntArray(raw.length + 1)
        val text = StringBuilder()
        for (i in raw.indices) {
            map[i] = text.length
            if (keep[i] && i in first..last) text.append(raw[i])
        }
        map[raw.length] = text.length
        val moved = marks.map { Mark(map[it.start], map[it.end], it.kind, it.target) }.filter { it.end > it.start }
        val final = text.toString()
        return Styled(final, (moved + labels(final, moved)).sortedBy { it.start })
    }

    /** The entry's fields, each with its type letter: plain ('m') when nothing says otherwise. */
    private fun fields(bytes: ByteArray, types: String?): List<Pair<Char, String>> {
        if (types != null) {
            val parts = String(bytes, Charsets.UTF_8).split('\u0000', limit = types.length.coerceAtLeast(1))
            return parts.mapIndexed { i, text -> types.getOrElse(i) { types.last() } to text }
        }
        val out = mutableListOf<Pair<Char, String>>()
        var i = 0
        while (i < bytes.size) {
            val type = bytes[i].toInt().toChar()
            var end = i + 1
            while (end < bytes.size && bytes[end] != 0.toByte()) end++
            if (type.isLowerCase()) out += type to String(bytes, i + 1, end - i - 1, Charsets.UTF_8)
            i = end + 1
        }
        return out
    }

    private fun newline(out: StringBuilder) {
        if (!out.endsWith("\n\n")) out.append('\n')
    }

    /**
     * Plain text into [out], its cross-references made links: "{shelter}", as WordNet and dictd write
     * them, shows as the word alone. A bracketed label ("{f}", gender) is left as written.
     */
    private fun plainWithLinks(text: String, out: StringBuilder, marks: MutableList<Mark>) {
        var at = 0
        CROSS_REF.findAll(text).forEach { m ->
            val word = m.groupValues[1].trim()
            if (Regex("(?i)(?:$LABEL)\\.?").matches(word)) return@forEach
            out.append(text, at, m.range.first)
            val start = out.length
            out.append(word)
            marks += Mark(start, out.length, MarkKind.LINK, word)
            at = m.range.last + 1
        }
        out.append(text, at, text.length)
    }

    private val CROSS_REF = Regex("""\{([^{}\n]{1,60})\}""")

    /** What a tag marks: italics and abbreviations are labels, bold is bold, and xdxf's <ex> an example. */
    private fun kindOf(tag: String): MarkKind? = when (tag) {
        "i", "em", "abr", "abbr", "gr", "pos" -> MarkKind.LABEL
        "b", "strong", "k" -> MarkKind.STRONG
        "ex" -> MarkKind.EXAMPLE
        "kref" -> MarkKind.LINK
        else -> null
    }

    private val ENTITIES = mapOf("lt" to "<", "gt" to ">", "quot" to "\"", "apos" to "'", "#39" to "'", "nbsp" to " ", "amp" to "&")

    /** [text]'s markup read into [out]: tags into [marks] and line breaks, entities into their letters. */
    private fun markup(text: String, out: StringBuilder, marks: MutableList<Mark>) {
        val open = ArrayDeque<Triple<String, MarkKind, Int>>()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            when {
                c == '<' && text.indexOf('>', i) > i -> {
                    val end = text.indexOf('>', i)
                    val tag = text.substring(i + 1, end).trim()
                    val closing = tag.startsWith("/")
                    val name = tag.removePrefix("/").takeWhile { it.isLetterOrDigit() }.lowercase()
                    when {
                        name == "br" || (closing && name in setOf("p", "div", "li", "tr", "def", "blockquote")) -> newline(out)
                        kindOf(name) != null && !closing && !tag.endsWith("/") -> open.addLast(Triple(name, kindOf(name)!!, out.length))
                        kindOf(name) != null && closing -> open.lastOrNull { it.first == name }?.let { started ->
                            open.remove(started)
                            if (out.length > started.third) {
                                val target = if (started.second == MarkKind.LINK) out.substring(started.third).trim() else null
                                marks += Mark(started.third, out.length, started.second, target)
                            }
                        }
                    }
                    i = end + 1
                }
                c == '&' -> {
                    val semi = text.indexOf(';', i)
                    val entity = if (semi in (i + 2)..(i + 8)) text.substring(i + 1, semi) else null
                    val letter = entity?.let { ENTITIES[it] ?: it.removePrefix("#").toIntOrNull()?.let { code -> String(Character.toChars(code)) } }
                    if (letter != null) {
                        out.append(letter)
                        i = semi + 1
                    } else {
                        out.append(c)
                        i++
                    }
                }
                else -> {
                    out.append(c)
                    i++
                }
            }
        }
    }

    private const val LABEL =
        "noun|verb|adjective|adverb|pronoun|preposition|conjunction|interjection|article|numeral|adj|adv|pron|prep|conj|interj|abbr|num|art|n|v|vt|vi|m|f|nt|pl|c"
    private val BRACKETED = Regex("""[<(\[{](?:$LABEL)\.?(?:[,;/ ]+(?:$LABEL)\.?)*[>)\]}]""", RegexOption.IGNORE_CASE)
    private val LINE_START = Regex("""(?m)^[ \t]*((?:$LABEL)\.?)(?=[ \t]|:|\d|$)""", RegexOption.IGNORE_CASE)

    /** Grammar labels in [text] not already marked: a bracketed run of them, or one opening a line. */
    private fun labels(text: String, marked: List<Mark>): List<Mark> {
        val found = BRACKETED.findAll(text).map { Mark(it.range.first, it.range.last + 1, MarkKind.LABEL) } +
            LINE_START.findAll(text).map { m -> m.groups[1]!!.range.let { Mark(it.first, it.last + 1, MarkKind.LABEL) } }
        return found.filter { f -> marked.none { it.start < f.end && f.start < it.end } }.distinctBy { it.start }.toList()
    }

    /** The folder's files by name, as document links. */
    private fun children(context: Context, tree: Uri): Map<String, Uri> {
        val list = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val out = mutableMapOf<String, Uri>()
        context.contentResolver.query(list, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
            while (c.moveToNext()) out[c.getString(1)] = DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0))
        }
        return out
    }
}
