package app.bedtime.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import kotlinx.coroutines.launch
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.Voice
import app.bedtime.ui.components.SoftPillButton
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.BedtimeTheme
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * Making a voice note: the time so far and a way to stop, over the lock screen too, without unlocking:
 * it only ever adds. Leaving it keeps recording; its notification brings it back, or stops it.
 */
class RecordActivity : ComponentActivity() {
    /** Where it goes, as asked for by what opened this: null asks when it stops. */
    private var to: app.bedtime.data.VoiceTo? = null

    private val askMic = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) VoiceRecorder.start(this, to) else finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Over the lock screen, as refuge's own lock screen is.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        val repo = app.bedtime.data.Repository.get(this)
        val note = intent.getStringExtra(EXTRA_NOTE)
        val starting = savedInstanceState == null && intent.getBooleanExtra(EXTRA_START, true) && VoiceRecorder.state.value == VoiceRecorder.State.Idle
        lifecycleScope.launch {
            val settings = repo.settings.first()
            // From the microjournal or a note, it goes there; from anywhere else, it asks when you stop,
            // unless there's no microjournal to choose.
            to = when {
                intent.getBooleanExtra(EXTRA_JOURNAL, false) -> app.bedtime.data.VoiceTo.Journal
                note != null -> app.bedtime.data.VoiceTo.Into(note)
                !settings.journalOn -> app.bedtime.data.VoiceTo.NewNote
                else -> null
            }
            if (!starting) return@launch
            if (ContextCompat.checkSelfPermission(this@RecordActivity, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                VoiceRecorder.start(this@RecordActivity, to)
            } else {
                askMic.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
        setContent {
            BedtimeTheme {
                val c = Obsidian.colors
                val state by VoiceRecorder.state.collectAsStateWithLifecycle()
                val target by VoiceRecorder.target.collectAsStateWithLifecycle()
                var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
                var begun by remember { mutableLongStateOf(0L) }
                LaunchedEffect(state) {
                    when (state) {
                        is VoiceRecorder.State.Recording -> while (true) {
                            now = System.currentTimeMillis()
                            delay(250)
                        }
                        // Kept: it goes on in the background, and this can go.
                        VoiceRecorder.State.Saving -> {
                            delay(1_200)
                            finish()
                        }
                        VoiceRecorder.State.Idle -> if (begun > 0) finish()
                        is VoiceRecorder.State.Waiting -> Unit
                    }
                }
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)).clickable(onClickLabel = "leave it recording") { finish() }.safeDrawingPadding(),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(c.bgPrimary)
                            .clickable(enabled = false) {}
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        when (val s = state) {
                            is VoiceRecorder.State.Recording -> {
                                begun = s.since
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(Modifier.size(12.dp).clip(CircleShape).background(Color(0xFFD7263D)))
                                    Text("recording", style = MaterialTheme.typography.titleMedium, color = c.textNormal)
                                }
                                Text(Voice.length(((now - s.since) / 1000L).toInt().coerceAtLeast(0)), fontSize = 40.sp, color = c.textNormal)
                                // Where it goes, asked straight away; left unanswered, it's asked again when it stops.
                                val choosable by VoiceRecorder.choosable.collectAsStateWithLifecycle()
                                if (choosable) {
                                    Text("where should it go?", style = MaterialTheme.typography.labelLarge, color = c.textMuted)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        listOf(app.bedtime.data.VoiceTo.Journal to "microjournal", app.bedtime.data.VoiceTo.NewNote to "new note").forEach { (where, name) ->
                                            val on = target == where
                                            Text(
                                                name,
                                                style = MaterialTheme.typography.labelLarge,
                                                color = if (on) c.bgPrimary else c.textNormal,
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(50))
                                                    .background(if (on) c.accent else Color.Transparent)
                                                    .border(1.dp, if (on) c.accent else c.border, RoundedCornerShape(50))
                                                    .clickable { VoiceRecorder.choose(this@RecordActivity, where) }
                                                    .padding(vertical = 10.dp),
                                            )
                                        }
                                    }
                                }
                                Text(
                                    when (target) {
                                        null -> "If you don't choose now, you will when it stops"
                                        app.bedtime.data.VoiceTo.Journal -> "It goes in today's microjournal"
                                        app.bedtime.data.VoiceTo.NewNote -> "It goes in a new note"
                                        is app.bedtime.data.VoiceTo.Into -> "It goes in this note"
                                    } + "; the words follow once Whisper has heard them. Leave this and it keeps recording.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = c.textMuted,
                                )
                                SoftPillButton("Stop", onClick = { VoiceRecorder.stop(this@RecordActivity) }, modifier = Modifier.fillMaxWidth())
                            }
                            // Stopped: where this one goes, asked every time, never remembered.
                            is VoiceRecorder.State.Waiting -> {
                                Text("where should it go?", style = MaterialTheme.typography.titleMedium, color = c.textNormal)
                                Text("${Voice.length(s.seconds)} recorded", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                                SoftPillButton("In today's microjournal", onClick = { VoiceRecorder.stop(this@RecordActivity, app.bedtime.data.VoiceTo.Journal) }, modifier = Modifier.fillMaxWidth())
                                SoftPillButton("As a new note", onClick = { VoiceRecorder.stop(this@RecordActivity, app.bedtime.data.VoiceTo.NewNote) }, modifier = Modifier.fillMaxWidth())
                            }
                            VoiceRecorder.State.Saving -> Text(
                                if (target == app.bedtime.data.VoiceTo.Journal) "Kept. The words follow in the microjournal." else "Kept. The words follow in your notes.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = c.textNormal,
                            )
                            VoiceRecorder.State.Idle -> Text("Starting…", style = MaterialTheme.typography.bodyLarge, color = c.textMuted)
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_START = "start"
        private const val EXTRA_JOURNAL = "journal"
        private const val EXTRA_NOTE = "note"
        const val ACTION = "app.bedtime.RECORD"

        /** A new recording from a gesture, once voice notes are set up; until then, where to set them up. */
        fun startIfReady(context: Context, settings: app.bedtime.data.AppSettings) {
            if (app.bedtime.data.Voice.ready(settings)) context.startActivity(intent(context))
            else android.widget.Toast.makeText(context, "set up voice notes first: settings › quick notes", android.widget.Toast.LENGTH_SHORT).show()
        }

        /**
         * The recording screen; with [start], a new recording begins if none is going. From the
         * microjournal ([journal]) it goes there, from a note ([note]) into it; from anywhere else, you
         * choose where when you stop.
         */
        fun intent(context: Context, start: Boolean = true, journal: Boolean = false, note: String? = null): Intent =
            Intent(context, RecordActivity::class.java).setAction(ACTION).putExtra(EXTRA_START, start).putExtra(EXTRA_JOURNAL, journal).putExtra(EXTRA_NOTE, note)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
