package app.bedtime.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import java.io.File
import java.io.InputStream
import java.io.RandomAccessFile
import java.util.zip.GZIPInputStream

/** One word's entry: the word as the dictionary has it, which dictionary, and what it says. */
data class Definition(val word: String, val dictionary: String, val text: String)

/**
 * A StarDict dictionary kept in the app's own storage: its name, its index (each word, where its entry
 * starts and how long it is) and its entries' file. Read only on this phone; nothing is fetched.
 */
class StarDict(val name: String, private val words: List<String>, private val offsets: LongArray, private val sizes: IntArray, private val dict: File, private val types: String?) {
    /** The entry for [word], matched without regard to case; null if it isn't in here. */
    fun define(word: String): Definition? {
        val at = find(word.trim())
        return if (at < 0) null else Definition(words[at], name, read(at))
    }

    /** Up to [limit] words starting with [prefix], in the dictionary's order. */
    fun startingWith(prefix: String, limit: Int = 8): List<String> {
        val p = prefix.trim()
        if (p.isEmpty()) return emptyList()
        var i = lowerBound(p)
        val out = mutableListOf<String>()
        while (i < words.size && out.size < limit && words[i].startsWith(p, ignoreCase = true)) {
            if (out.none { it.equals(words[i], ignoreCase = true) }) out += words[i]
            i++
        }
        return out
    }

    private fun find(word: String): Int {
        val i = lowerBound(word)
        return if (i < words.size && words[i].equals(word, ignoreCase = true)) i else -1
    }

    /** The first index whose word isn't before [word], comparing as StarDict sorts: case aside first. */
    private fun lowerBound(word: String): Int {
        var lo = 0
        var hi = words.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (compare(words[mid], word) < 0) lo = mid + 1 else hi = mid
        }
        return lo
    }

    private fun read(at: Int): String {
        val bytes = ByteArray(sizes[at])
        RandomAccessFile(dict, "r").use { f ->
            f.seek(offsets[at])
            f.readFully(bytes)
        }
        return StarDicts.plain(bytes, types)
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

    /** The first entry for [word] in each dictionary that has it. */
    fun define(context: Context, word: String): List<Definition> = all(context).mapNotNull { it.define(word) }

    /** Words starting with [prefix], from every dictionary, each once. */
    fun suggest(context: Context, prefix: String, limit: Int = 8): List<String> =
        all(context).flatMap { it.startingWith(prefix, limit) }.distinctBy { it.lowercase() }.take(limit)

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
            val into = File(root(context), base.replace(Regex("[^A-Za-z0-9._-]"), "_"))
            into.mkdirs()
            val resolver = context.contentResolver
            try {
                resolver.openInputStream(files.getValue(ifo))?.use { copy(it, File(into, "dict.ifo")) }
                resolver.openInputStream(files.getValue(idx))?.use { copy(unpacked(it, idx), File(into, "dict.idx")) }
                resolver.openInputStream(files.getValue(dict))?.use { copy(unpacked(it, dict), File(into, "dict.dict")) }
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
        StarDict(info["bookname"] ?: dir.name, words, offsets, sizes, File(dir, "dict.dict"), info["sametypesequence"])
    }.getOrNull()

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
     * An entry as plain text. With [types] (one letter per field: m plain, h html, x xdxf, g pango…)
     * the fields are the entry itself; without, each starts with its letter. Markup is taken out,
     * line breaks kept.
     */
    internal fun plain(bytes: ByteArray, types: String?): String {
        val text = if (types != null) {
            String(bytes, Charsets.UTF_8)
        } else {
            // Each field: a type letter, then its text ended by a zero; only text fields are read.
            buildString {
                var i = 0
                while (i < bytes.size) {
                    val type = bytes[i].toInt().toChar()
                    var end = i + 1
                    while (end < bytes.size && bytes[end] != 0.toByte()) end++
                    if (type.isLowerCase()) append(String(bytes, i + 1, end - i - 1, Charsets.UTF_8)).append('\n')
                    i = end + 1
                }
            }
        }
        return text
            .replace(Regex("(?i)<br\\s*/?>|</p>|</div>|</li>"), "\n")
            .replace(Regex("<[^>]+>"), "")
            .replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&#39;", "'").replace("&nbsp;", " ").replace("&amp;", "&")
            .replace('\u0000', '\n')
            .lines().joinToString("\n") { it.trimEnd() }
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
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
