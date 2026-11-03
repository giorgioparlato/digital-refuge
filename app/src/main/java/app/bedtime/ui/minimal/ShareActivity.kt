package app.bedtime.ui.minimal

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.Notes
import app.bedtime.data.Repository
import app.bedtime.ui.components.ProvideAppIcons
import app.bedtime.ui.theme.BedtimeTheme
import kotlinx.coroutines.launch

/**
 * Text shared from any app (a link, a quote, an address) as a quick note: the same box, with what was
 * shared in it, to keep in refuge notes, one of your files, or today's microjournal.
 */
class ShareActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val shared = sharedText(intent)
        setContent {
            BedtimeTheme {
                ProvideAppIcons {
                    val repo = remember { Repository.get(this) }
                    // Saving outlives the box: it closes, and the activity with it, as soon as you pick.
                    val scope = remember { kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Main) }
                    val app = applicationContext
                    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
                    val s = settings ?: return@ProvideAppIcons
                    val journal = journalWith(app, repo, scope, s)
                    // Shared straight to the microjournal: how you feel, or skip, and it's in.
                    if (intent.component?.className == JOURNAL_SHARE && journal != null) {
                        JournalShareDialog(shared, s.homeStyle, journal, onDismiss = { finish() })
                        return@ProvideAppIcons
                    }
                    QuickNoteDialog(
                        s.homeStyle,
                        s.quickTargets,
                        onSave = { words, target ->
                            scope.launch { Toast.makeText(app, Notes.save(app, repo, words, target), Toast.LENGTH_SHORT).show() }
                        },
                        onDismiss = { finish() },
                        vault = s.obsidianVault,
                        initial = shared,
                        journal = journal,
                    )
                }
            }
        }
    }

    companion object {
        /** The share menu's "add to microjournal", an alias of this activity. */
        const val JOURNAL_SHARE = "app.bedtime.ui.minimal.JournalShare"

        /** "Add to microjournal" in the share menu while the microjournal is on, and gone while it's off. */
        fun syncJournalShare(context: android.content.Context, on: Boolean) {
            val state = if (on) android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED else android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            runCatching {
                context.packageManager.setComponentEnabledSetting(android.content.ComponentName(context, JOURNAL_SHARE), state, android.content.pm.PackageManager.DONT_KILL_APP)
            }
        }
    }

    /** What was shared: its subject first, when there's one the text doesn't already have. */
    private fun sharedText(intent: Intent): String {
        val text = intent.getStringExtra(Intent.EXTRA_TEXT).orEmpty().trim()
        val subject = intent.getStringExtra(Intent.EXTRA_SUBJECT).orEmpty().trim()
        return if (subject.isNotEmpty() && subject !in text) "$subject\n$text".trim() else text
    }
}
