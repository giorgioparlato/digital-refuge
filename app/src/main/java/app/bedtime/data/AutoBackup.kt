package app.bedtime.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * A backup written by itself, once a week, into a folder you chose: the same file as "export", so a
 * reinstall or a new phone can start from it. The newest few are kept; older ones of its own are
 * removed, and nothing else in the folder is touched.
 */
object AutoBackup {
    const val EVERY_MS = 7 * 24 * 60 * 60_000L
    const val KEEP = 4
    private const val PREFIX = "digital-refuge-auto-"

    /** Whether one is due: a folder chosen, and a week since the last. */
    fun due(settings: AppSettings, now: Long): Boolean = settings.backupFolder != null && now - settings.lastBackupAt >= EVERY_MS

    fun fileName(day: LocalDate): String = "$PREFIX$day.json"

    /** Of the names in the folder, this app's own backups beyond the newest [keep]. Others are never listed. */
    fun toDelete(names: List<String>, keep: Int = KEEP): List<String> =
        names.filter { it.startsWith(PREFIX) && it.endsWith(".json") }.distinct().sortedDescending().drop(keep)

    /** Whether the folder can still be written to; after a reinstall it has to be chosen again. */
    fun canWrite(context: Context, folder: String): Boolean =
        context.contentResolver.persistedUriPermissions.any { it.uri.toString() == folder && it.isWritePermission }

    @Volatile private var writing = false

    /** Writes one now, and notes when. False if there's no folder, it can't be written to, or it failed. */
    suspend fun write(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (writing) return@withContext false
        writing = true
        try {
            val repo = Repository.get(context)
            val folder = repo.settings.first().backupFolder ?: return@withContext false
            val tree = Uri.parse(folder)
            if (!canWrite(context, folder)) return@withContext false
            val resolver = context.contentResolver
            val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
            val now = System.currentTimeMillis()
            val name = fileName(Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate())
            val existing = children(context, tree)
            // Today's again replaces today's, rather than sitting beside it as "(1)".
            existing[name]?.let { DocumentsContract.deleteDocument(resolver, it) }
            val file = DocumentsContract.createDocument(resolver, parent, "application/json", name) ?: return@withContext false
            val json = Backup.encode(repo.snapshot())
            checkNotNull(resolver.openOutputStream(file, "wt")).use { it.write(json.toByteArray()) }
            toDelete(existing.keys.toList() + name).forEach { old -> existing[old]?.let { runCatching { DocumentsContract.deleteDocument(resolver, it) } } }
            repo.updateSettings { it.copy(lastBackupAt = now) }
            true
        } catch (e: Exception) {
            false
        } finally {
            writing = false
        }
    }

    /** The folder's files by name, as document links. */
    private fun children(context: Context, tree: Uri): Map<String, Uri> {
        val list = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        val out = mutableMapOf<String, Uri>()
        context.contentResolver.query(
            list,
            arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { c ->
            while (c.moveToNext()) out[c.getString(1)] = DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0))
        }
        return out
    }
}
