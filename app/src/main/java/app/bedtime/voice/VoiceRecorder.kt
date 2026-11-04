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

    /** Stops listening, keeps the recording and its line, then hears the words in it. */
    private fun finish() {
        if (_state.value !is State.Recording) {
            if (_state.value == State.Idle) stopSelf()
            return
        }
        recording = false
        thread?.join(2_000)
        _state.value = State.Saving
        ensureForeground("saving, then transcribing")
        val bytes = synchronized(heard) { heard.toByteArray() }
        val started = since
        scope.launch {
            try {
                keep(bytes, started)
            } catch (e: Exception) {
                Problems.note("keeping a voice note", e)
                say("couldn't keep the voice note")
            } finally {
                _state.value = State.Idle
                ServiceCompat.stopForeground(this@VoiceRecorder, ServiceCompat.STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private suspend fun keep(bytes: ByteArray, started: Long) {
        val samples = ShortArray(bytes.size / 2).also { ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(it) }
        val seconds = samples.size / RATE
        if (seconds < 1) return
        val repo = Repository.get(this)
        val settings = repo.settings.first()
        val at = Instant.ofEpochMilli(started).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val tree = settings.voiceFolder?.let(Uri::parse) ?: return say("choose a folder for voice notes first")
        // Never chosen (stopped some other way): a note of its own, where nothing is lost.
        val to = target.value ?: VoiceTo.NewNote
        // The microjournal's in a folder of their own; a note's in yours.
        val journal = to == VoiceTo.Journal
        val folder = (if (journal) subfolder(tree, Voice.JOURNAL_FOLDER) else top(tree))
            ?: return say("couldn't write to the voice notes folder; choose it again")
        val name = Voice.fileName(at, names(tree, folder))
        val encoded = File(cacheDir, "voice.m4a")
        encode(samples, encoded)
        if (!copyInto(folder, name, encoded)) return say("couldn't write to the voice notes folder; choose it again")
        encoded.delete()
        val day = at.toLocalDate().toEpochDay()
        val line = Voice.line(at.format(DateTimeFormatter.ofPattern("HH:mm")), seconds, name)
        when (to) {
            VoiceTo.Journal -> repo.addVoiceLine(day, line)
            VoiceTo.NewNote -> repo.addNote(Voice.noteText(at, line))
            is VoiceTo.Into -> repo.addToNote(to.noteId, line)
        }
        // A page open on it takes the line in too, so its own saving doesn't write over it.
        edits.tryEmit(Edit.Added(to, day, line))
        say(if (journal) "voice note kept in the microjournal" else "voice note kept in your notes")
        // The words, when there's a model to hear them with; without, the line keeps just the recording.
        val model = modelFile(this)
        val words = if (model.exists() && Whisper.available) {
            Whisper.words(model.path, FloatArray(samples.size) { samples[it] / 32768f }, settings.voiceLanguage)
        } else {
            null
        }
        if (journal) repo.voiceTranscribed(day, name, words) else repo.voiceNoteTranscribed(name, words)
        edits.tryEmit(Edit.Heard(name, words))
    }

    /** The folder you chose, as a document to write in. */
    private fun top(tree: Uri): Uri = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))

    /** The folder called [name] inside yours, made if it isn't there. */
    private fun subfolder(tree: Uri, name: String): Uri? = runCatching {
        children(tree, top(tree)).firstOrNull { it.second == name && it.third }?.first
            ?: DocumentsContract.createDocument(contentResolver, top(tree), DocumentsContract.Document.MIME_TYPE_DIR, name)
    }.getOrNull()

    /** What's in [folder]: each as its document, name, and whether it's a folder. */
    private fun children(tree: Uri, folder: Uri): List<Triple<Uri, String, Boolean>> = runCatching {
        val list = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getDocumentId(folder))
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE)
        contentResolver.query(list, columns, null, null, null)?.use { c ->
            buildList {
                while (c.moveToNext()) add(Triple(DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0)), c.getString(1), c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR))
            }
        }.orEmpty()
    }.getOrDefault(emptyList())

    /** The folder's files, so a recording never writes over another. */
    private fun names(tree: Uri, folder: Uri): Set<String> = children(tree, folder).map { it.second }.toSet()

    private fun copyInto(parent: Uri, name: String, file: File): Boolean = runCatching {
        val target = DocumentsContract.createDocument(contentResolver, parent, "audio/mp4", name) ?: return false
        contentResolver.openOutputStream(target, "w")?.use { out -> file.inputStream().use { it.copyTo(out) } } ?: return false
        true
    }.getOrDefault(false)

    /** [samples] as AAC in an m4a file: small, and every player and Obsidian take it. */
    private fun encode(samples: ShortArray, out: File) {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, RATE, 1).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 48_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16_384)
        }
        val codec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        codec.start()
        val muxer = MediaMuxer(out.path, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        val info = MediaCodec.BufferInfo()
        var track = -1
        var at = 0
        var fed = false
        var done = false
        try {
            while (!done) {
                if (!fed) {
                    val i = codec.dequeueInputBuffer(10_000)
                    if (i >= 0) {
                        val buffer = checkNotNull(codec.getInputBuffer(i)).apply { clear(); order(ByteOrder.LITTLE_ENDIAN) }
                        val count = minOf(buffer.remaining() / 2, samples.size - at)
                        val time = at * 1_000_000L / RATE
                        if (count <= 0) {
                            codec.queueInputBuffer(i, 0, 0, time, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            fed = true
                        } else {
                            buffer.asShortBuffer().put(samples, at, count)
                            codec.queueInputBuffer(i, 0, count * 2, time, 0)
                            at += count
                        }
                    }
                }
                val o = codec.dequeueOutputBuffer(info, 10_000)
                if (o == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    track = muxer.addTrack(codec.outputFormat)
                    muxer.start()
                } else if (o >= 0) {
                    val buffer = checkNotNull(codec.getOutputBuffer(o))
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) info.size = 0
                    if (info.size > 0 && track >= 0) {
                        buffer.position(info.offset)
                        buffer.limit(info.offset + info.size)
                        muxer.writeSampleData(track, buffer, info)
                    }
                    codec.releaseOutputBuffer(o, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) done = true
                }
            }
        } finally {
            codec.stop()
            codec.release()
            if (track >= 0) muxer.stop()
            muxer.release()
        }
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
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle("voice note")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(open)
            .apply {
                // Where it goes, asked at the end here too when it wasn't decided at the start.
                if (recordingNow && target.value == null) {
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
