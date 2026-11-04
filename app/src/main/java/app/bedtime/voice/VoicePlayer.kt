package app.bedtime.voice

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A voice note's recording, found by name in your folder and opened in whichever audio player you use. */
object VoicePlayer {
    suspend fun open(context: Context, folder: String, file: String) {
        val uri = withContext(Dispatchers.IO) { find(context, Uri.parse(folder), file) }
        if (uri == null) {
            Toast.makeText(context, "that recording isn't in the voice notes folder anymore", Toast.LENGTH_SHORT).show()
            return
        }
        val play = Intent(Intent.ACTION_VIEW).setDataAndType(uri, "audio/mp4")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(play) }
            .onFailure { Toast.makeText(context, "no app here plays it", Toast.LENGTH_SHORT).show() }
    }

    /** [file] in your folder, or in the microjournal's inside it. */
    private fun find(context: Context, tree: Uri, file: String): Uri? = runCatching {
        val top = DocumentsContract.getTreeDocumentId(tree)
        val here = children(context, tree, top)
        here.firstOrNull { it.second == file }?.let { return@runCatching DocumentsContract.buildDocumentUriUsingTree(tree, it.first) }
        val journal = here.firstOrNull { it.second == app.bedtime.data.Voice.JOURNAL_FOLDER } ?: return@runCatching null
        children(context, tree, journal.first).firstOrNull { it.second == file }?.let { DocumentsContract.buildDocumentUriUsingTree(tree, it.first) }
    }.getOrNull()

    /** A folder's contents, as document id and name. */
    private fun children(context: Context, tree: Uri, id: String): List<Pair<String, String>> {
        val list = DocumentsContract.buildChildDocumentsUriUsingTree(tree, id)
        return context.contentResolver.query(list, arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)?.use { c ->
            buildList { while (c.moveToNext()) add(c.getString(0) to c.getString(1)) }
        }.orEmpty()
    }
}
