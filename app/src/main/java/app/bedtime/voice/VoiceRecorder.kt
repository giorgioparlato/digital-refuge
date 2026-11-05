package app.bedtime.voice

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.bedtime.R
import app.bedtime.data.Repository
import app.bedtime.data.Voice
import app.bedtime.data.VoiceTo
import app.bedtime.data.Problems
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * A voice note being made: the microphone heard at 16 kHz, as Whisper wants it, until you stop. Then the
 * recording is kept as an m4a file in your folder, a line goes in today's microjournal, and Whisper's
 * words take the place of its "transcribing…". In the foreground all the while, so it carries on with the
 * screen off; "stop" is on its notification too.
 */
class VoiceRecorder : Service() {
    sealed interface State {
        data object Idle : State
        data class Recording(val since: Long) : State
        /** Stopped, and asking where it goes: [seconds] long. */
        data class Waiting(val seconds: Int) : State
        data object Saving : State
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile private var recording = false
    private var thread: Thread? = null
    private val heard = ByteArrayOutputStream()
    private var since = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            START -> if (_state.value == State.Idle) begin() else ensureForeground("recording")
            STOP -> {
                // Asked at the end, from the notification: where it goes comes with the stop.
                when (intent.getStringExtra(EXTRA_TO)) {
                    TO_JOURNAL -> target.value = VoiceTo.Journal
                    TO_NOTE -> target.value = VoiceTo.NewNote
                }
                finish()
            }
        }
        return START_NOT_STICKY
    }

    private fun ensureForeground(text: String) {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE else 0
        runCatching { ServiceCompat.startForeground(this, NOTIFICATION, notification(text), type) }
            .onFailure { Problems.note("recording a voice note", it) }
    }

    private fun begin() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            stopSelf()
            return
        }
        ensureForeground("recording")
        val size = maxOf(AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT), RATE * 2)
        val mic = runCatching { AudioRecord(MediaRecorder.AudioSource.MIC, RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, size) }.getOrNull()
        if (mic == null || mic.state != AudioRecord.STATE_INITIALIZED) {
            mic?.release()
            scope.launch { say("the microphone isn't free") }
            stopSelf()
            return
        }
        heard.reset()
        since = System.currentTimeMillis()
        recording = true
        _state.value = State.Recording(since)
        thread = Thread {
            val buffer = ByteArray(RATE / 5)
            mic.startRecording()
            while (recording) {
                val n = mic.read(buffer, 0, buffer.size)
                if (n > 0) synchronized(heard) { heard.write(buffer, 0, n) }
                // Long enough: it stops itself, and is kept as if you had.
                if (System.currentTimeMillis() - since >= Voice.LONGEST_SECONDS * 1000L) {
                    recording = false
                    scope.launch { finish() }
                }
            }
            mic.stop()
            mic.release()
        }.also { it.start() }
    }

    /**
     * Stops listening and keeps the sound here straight away (see [VoiceFiling]), so nothing is lost
     * from then on; then files it, or first asks where it goes when that wasn't settled at the start
     * (the screen, the notification and the lock screen ask until you say).
     */
    private fun finish() {
        when (_state.value) {
            is State.Recording -> {
                recording = false
                thread?.join(2_000)
                val bytes = synchronized(heard) { heard.toByteArray() }
                if (bytes.size / 2 < RATE) return done()
                val kept = VoiceFiling.keep(this, since, bytes, target.value)
                if (target.value == null) {
                    _state.value = State.Waiting(kept.seconds)
                    ensureForeground("where should it go?")
                } else {
                    file()
                }
            }
            is State.Waiting -> target.value?.let { to ->
                VoiceFiling.choose(this, since, to)
                file()
            }
            State.Idle -> stopSelf()
            State.Saving -> Unit
        }
    }

    private fun file() {
        _state.value = State.Saving
        ensureForeground("saving, then transcribing")
        val start = since
        scope.launch {
            try {
                VoiceFiling.process(this@VoiceRecorder, start)
            } catch (e: Exception) {
                // What was kept stays kept: it's taken on again when refuge next starts.
                Problems.note("keeping a voice note", e)
            } finally {
                done()
            }
        }
    }

    private fun done() {
        _state.value = State.Idle
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notification(text: String): Notification {
        val system = getSystemService(NotificationManager::class.java)
        if (system.getNotificationChannel(CHANNEL) == null) {
            system.createNotificationChannel(NotificationChannel(CHANNEL, "voice notes", NotificationManager.IMPORTANCE_LOW))
        }
        val open = PendingIntent.getActivity(this, 0, RecordActivity.intent(this, start = false), PendingIntent.FLAG_IMMUTABLE)
        val stop = { requestCode: Int, to: String? ->
            PendingIntent.getService(this, requestCode, Intent(this, VoiceRecorder::class.java).setAction(STOP).putExtra(EXTRA_TO, to), PendingIntent.FLAG_IMMUTABLE)
        }
        val recordingNow = _state.value is State.Recording || text == "recording"
        val waiting = _state.value is State.Waiting
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle("voice note")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(open)
            .apply {
                // Stop while it records; then, if it wasn't settled at the start, where it goes.
                if (waiting) {
                    addAction(0, "to the microjournal", stop(1, TO_JOURNAL))
                    addAction(0, "to a note", stop(2, TO_NOTE))
                } else if (recordingNow) {
                    addAction(0, "stop", stop(3, null))
                }
            }
            .build()
    }

    private suspend fun say(text: String) = withContext(Dispatchers.Main) { Toast.makeText(this@VoiceRecorder, text, Toast.LENGTH_SHORT).show() }

    override fun onDestroy() {
        recording = false
        super.onDestroy()
    }

    companion object {
        /** What Whisper hears in. */
        const val RATE = 16_000
        private const val START = "start"
        private const val STOP = "stop"
        private const val CHANNEL = "voice_notes"
        private const val NOTIFICATION = 7_301

        private val _state = MutableStateFlow<State>(State.Idle)
        val state: StateFlow<State> = _state

        /** Where the recording goes, set when it starts; null asks when you stop it. */
        val target = MutableStateFlow<VoiceTo?>(null)

        /** What a recording did to an entry or a note, for a page open on it to take in as well. */
        sealed interface Edit {
            data class Added(val to: VoiceTo, val day: Long, val line: String) : Edit
            data class Heard(val file: String, val words: String?) : Edit
        }

        val edits = kotlinx.coroutines.flow.MutableSharedFlow<Edit>(extraBufferCapacity = 8)

        /** For the microjournal open on [day]: a recording's line there, then the words of any. */
        fun journalEdits(day: Long): kotlinx.coroutines.flow.Flow<(String) -> String> = edits.mapNotNull { e ->
            when (e) {
                is Edit.Added -> if (e.to == VoiceTo.Journal && e.day == day) { text: String -> Voice.added(text, e.line) } else null
                is Edit.Heard -> { text: String -> Voice.transcribed(text, e.file, e.words) }
            }
        }

        /** For note [id] open: a recording's line in it, then the words of any. */
        fun notesEdits(id: String): kotlinx.coroutines.flow.Flow<(String) -> String> = edits.mapNotNull { e ->
            when (e) {
                is Edit.Added -> if (e.to == VoiceTo.Into(id)) { text: String -> app.bedtime.data.Journal.withLine(text, e.line) } else null
                is Edit.Heard -> { text: String -> Voice.transcribed(text, e.file, e.words) }
            }
        }

        private const val EXTRA_TO = "to"
        private const val TO_JOURNAL = "journal"
        private const val TO_NOTE = "note"

        /** A new recording, going to [to]; with null, you're asked where when it stops. */
        fun start(context: Context, to: VoiceTo?) {
            target.value = to
            ContextCompat.startForegroundService(context, Intent(context, VoiceRecorder::class.java).setAction(START))
        }

        /** Stops it; [to] is where it goes, when that was asked at the end. */
        fun stop(context: Context, to: VoiceTo? = null) {
            if (to != null) target.value = to
            context.startService(Intent(context, VoiceRecorder::class.java).setAction(STOP))
        }

        /** The Whisper model, copied into refuge's own storage when you choose it. */
        fun modelFile(context: Context): File = File(context.filesDir, "whisper/model.bin")
    }
}
