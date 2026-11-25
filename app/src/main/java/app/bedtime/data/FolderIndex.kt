package app.bedtime.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract

/** A file (or a folder inside) found in a folder you chose: its name, where it sits in that folder, and how to open it. */
data class IndexedFile(val name: String, val path: String, val uri: String, val mime: String) {
    val isFolder: Boolean get() = mime == DocumentsContract.Document.MIME_TYPE_DIR
}

/**
 * The files in a folder you picked, read through Android's file picker permission only (no storage
 * access), and matched by name as you type. Your Obsidian vault and one folder of files use it.
 */
object FolderIndex {
    private const val MAX = 5_000
    const val OBSIDIAN = "md.obsidian"

    /**
     * Every file under [tree], folders inside included, but none whose name starts with a dot (.obsidian, .trash).
     * With [folders], the folders inside are found too, to open in the files app.
     */
    fun list(context: Context, tree: Uri, folders: Boolean = false, keep: (String) -> Boolean = { true }): List<IndexedFile> = runCatching {
        val resolver = context.contentResolver
        val out = mutableListOf<IndexedFile>()
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE)
        val pending = ArrayDeque(listOf(DocumentsContract.getTreeDocumentId(tree) to ""))
        while (pending.isNotEmpty() && out.size < MAX) {
            val (dir, prefix) = pending.removeFirst()
            resolver.query(DocumentsContract.buildChildDocumentsUriUsingTree(tree, dir), columns, null, null, null)?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getString(0)
                    val name = c.getString(1) ?: continue
                    val mime = c.getString(2).orEmpty()
                    if (name.startsWith(".")) continue
                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        pending.add(id to "$prefix$name/")
                        if (folders) out.add(IndexedFile(name, "$prefix$name/", DocumentsContract.buildDocumentUriUsingTree(tree, id).toString(), mime))
                    } else if (keep(name)) {
                        out.add(IndexedFile(name, "$prefix$name", DocumentsContract.buildDocumentUriUsingTree(tree, id).toString(), mime))
                    }
                }
            }
        }
        out
    }.getOrDefault(emptyList())

    /** The folder's own name, as the picker shows it. */
    fun folderName(context: Context, tree: Uri): String = runCatching {
        val doc = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        context.contentResolver.query(doc, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull() ?: tree.lastPathSegment?.substringAfterLast(':')?.substringAfterLast('/').orEmpty()

    /**
     * Words that walk into a folder: "books/", or "books/sci" to narrow it, when "books/" is one of
     * [files]' folders. The folder and the words after it; null for words that don't.
     */
    fun browsing(files: List<IndexedFile>, words: String): Pair<IndexedFile, String>? {
        val cut = words.lastIndexOf('/')
        if (cut < 0) return null
        val path = words.substring(0, cut + 1)
        val folder = files.firstOrNull { it.isFolder && it.path.equals(path, ignoreCase = true) } ?: return null
        return folder to words.substring(cut + 1).trim()
    }

    /** What's directly in [folder] among [files], folders first, then by name; only names with [words] in them. */
    fun inside(files: List<IndexedFile>, folder: IndexedFile, words: String = ""): List<IndexedFile> =
        files.filter { f ->
            val rest = f.path.removePrefix(folder.path)
            f.path.startsWith(folder.path) && rest.isNotEmpty() && '/' !in rest.removeSuffix("/") && f.name.contains(words, ignoreCase = true)
        }.sortedWith(compareBy({ !it.isFolder }, { it.name.lowercase() }))

    /** The folder above [folder], as words to search with ("books/"), or "" at the top. */
    fun parentPath(folder: IndexedFile): String =
        folder.path.removeSuffix("/").substringBeforeLast('/', "").let { if (it.isEmpty()) "" else "$it/" }

    /** A note's title: its name without ".md". */
    fun title(file: IndexedFile): String = file.name.removeSuffix(".md")

    /** Files whose [name] contains [query] (two letters or more), those starting with it first, at most [limit]. */
    fun match(files: List<IndexedFile>, query: String, limit: Int = 3, name: (IndexedFile) -> String = { it.name }): List<IndexedFile> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        return files.filter { name(it).contains(q, ignoreCase = true) }
            .sortedWith(compareBy({ !name(it).startsWith(q, ignoreCase = true) }, { name(it).length }, { name(it).lowercase() }))
            .take(limit)
    }

    /** Opens [note] in Obsidian; [vault] is the vault's name, as [folderName] reads it, not its folder's address. */
    fun obsidianIntent(vault: String, note: IndexedFile): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse("obsidian://open?vault=${Uri.encode(vault)}&file=${Uri.encode(note.path.removeSuffix(".md"))}"))
            .setPackage(OBSIDIAN)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** Opens [file] in whichever app takes its kind, allowed to read just that file; a folder, in the files app. */
    fun openIntent(file: IndexedFile): Intent =
        Intent(Intent.ACTION_VIEW)
            .setDataAndType(Uri.parse(file.uri), file.mime.ifEmpty { "*/*" })
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
