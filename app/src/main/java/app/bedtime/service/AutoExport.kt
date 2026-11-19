package app.bedtime.service

import android.content.Context
import android.net.Uri
import app.bedtime.data.NotesExport
import app.bedtime.data.Problems
import app.bedtime.data.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * With "export on every change" on: your notes and microjournal written to the export folder a few
 * seconds after each change, while refuge runs. The first time after refuge starts writes everything
 * (what changed while it wasn't running included); after that, only what changed. One way only:
 * nothing in the folder comes back.
 */
object AutoExport {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var started = false

    @OptIn(FlowPreview::class)
    fun start(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        val repo = Repository.get(app)
        var written: NotesExport.Written? = null
        var folder: String? = null
        scope.launch {
            combine(repo.notes, repo.journal, repo.settings) { notes, journal, settings ->
                Triple(notes, journal, settings.notesExportFolder?.takeIf { settings.notesAutoExport })
            }
                // Typing saves as you go: wait for a pause before writing.
                .debounce(3_000)
                .collect { (notes, journal, to) ->
                    if (to == null) {
                        written = null
                        return@collect
                    }
                    // A new folder starts from nothing written there.
                    if (to != folder) written = null
                    folder = to
                    val done = NotesExport.writeChanged(app, Uri.parse(to), notes, journal, LocalDate.now(), written)
                    if (done == null) {
                        Problems.note("exporting notes on each change: the folder couldn't be written to; choose it again")
                    } else {
                        written = NotesExport.Written.of(notes, journal)
                    }
                }
        }
    }
}
