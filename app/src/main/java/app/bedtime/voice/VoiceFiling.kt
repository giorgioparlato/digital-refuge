package app.bedtime.voice

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.core.app.NotificationCompat
import app.bedtime.R
import app.bedtime.data.AppJson
import app.bedtime.data.PendingVoice
import app.bedtime.data.Problems
import app.bedtime.data.Repository
import app.bedtime.data.Voice
import app.bedtime.data.VoiceTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * A recording from the moment it's stopped until its words are in: its sound kept in refuge's own
 * storage with a note of where it's up to ([PendingVoice]), so that nothing is lost if refuge is
 * closed on the way. Then the file into your folder, its line where it goes, and Whisper's words;
 * and only then is the copy here let go. What's left unfinished is picked up when refuge starts.
 */
object VoiceFiling {
    /** Carries on whatever screen is open, or none. */
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val one = Mutex()

    private fun dir(context: Context) = File(context.filesDir, "voice/pending").apply { mkdirs() }
    private fun record(context: Context, start: Long) = File(dir(context), "$start.json")
    private fun sound(context: Context, start: Long) = File(dir(context), "$start.pcm")

    /** The recording begun at [start], [bytes] of 16 kHz sound, kept here before anything else; [to] where it goes, or null to ask. */
    fun keep(context: Context, start: Long, bytes: ByteArray, to: VoiceTo?): PendingVoice {
        val pending = PendingVoice(start, bytes.size / 2 / VoiceRecorder.RATE, to?.let(Voice::code))
        sound(context, start).writeBytes(bytes)
        write(context, pending)
        return pending
    }

    private fun write(context: Context, pending: PendingVoice) =
        record(context, pending.start).writeText(AppJson.encodeToString(PendingVoice.serializer(), pending))

    private fun read(context: Context, start: Long): PendingVoice? =
        runCatching { AppJson.decodeFromString(PendingVoice.serializer(), record(context, start).readText()) }.getOrNull()

    /** Every recording not done with yet, the oldest first. */
    fun pending(context: Context): List<PendingVoice> =
        dir(context).listFiles { f -> f.name.endsWith(".json") }.orEmpty().mapNotNull { read(context, it.nameWithoutExtension.toLongOrNull() ?: return@mapNotNull null) }.sortedBy { it.start }

    /** Where the recording begun at [start] goes, now that you've said. */
    fun choose(context: Context, start: Long, to: VoiceTo) {
        val pending = read(context, start) ?: return
        if (pending.to == null) write(context, pending.copy(to = Voice.code(to)))
    }

    /** Done with: the copy here goes. */
    private fun forget(context: Context, start: Long) {
        sound(context, start).delete()
        record(context, start).delete()
    }

    /**
     * Takes the recording begun at [start] as far as it'll go: its file into your folder and its line
     * where it goes, then its words. Each step is written down as it's done, so it carries on from there.
     */
    suspend fun process(context: Context, start: Long) = one.withLock {
        var pending = read(context, start) ?: return@withLock
        val to = Voice.target(pending.to) ?: return@withLock
        val bytes = runCatching { sound(context, start).readBytes() }.getOrNull() ?: return@withLock forget(context, start)
        val samples = ShortArray(bytes.size / 2).also { ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(it) }
        if (samples.size < VoiceRecorder.RATE) return@withLock forget(context, start)
        val repo = Repository.get(context)
        val settings = repo.settings.first()
        val at = Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val day = at.toLocalDate().toEpochDay()
        val journal = to == VoiceTo.Journal
        if (pending.file == null) {
            val tree = settings.voiceFolder?.let(Uri::parse) ?: return@withLock stuck(context, pending, "there's no folder for voice notes yet: choose one in settings › quick notes")
            // The microjournal's in a folder of their own; a note's in yours.
            val folder = (if (journal) subfolder(context, tree, Voice.JOURNAL_FOLDER) else top(tree))
                ?: return@withLock stuck(context, pending, "refuge can't write to the voice notes folder: choose it again in settings › quick notes")
            val name = Voice.fileName(at, names(context, tree, folder))
            val encoded = File(context.cacheDir, "voice-$start.m4a")
            encode(samples, encoded)
            val copied = copyInto(context, folder, name, encoded)
            encoded.delete()
            if (!copied) return@withLock stuck(context, pending, "refuge can't write to the voice notes folder: choose it again in settings › quick notes")
            val line = Voice.line(at.format(DateTimeFormatter.ofPattern("HH:mm")), pending.seconds, name)
            when (to) {
                VoiceTo.Journal -> repo.addVoiceLine(day, line)
                VoiceTo.NewNote -> repo.addNote(Voice.noteText(at, line))
                is VoiceTo.Into -> repo.addToNote(to.noteId, line)
            }
            pending = pending.copy(file = name)
            write(context, pending)
            // A page open on it takes the line in too, so its own saving doesn't write over it.
            VoiceRecorder.edits.tryEmit(VoiceRecorder.Companion.Edit.Added(to, day, line))
            // Filed at last: any "waiting" notice about it goes.
            context.getSystemService(NotificationManager::class.java)?.cancel(notice(pending.start))
            say(context, if (journal) "voice note kept in the microjournal" else "voice note kept in your notes")
        }
        val name = pending.file ?: return@withLock
        // The words, when there's a model to hear them with; without, the line keeps just the recording.
        val model = VoiceRecorder.modelFile(context)
        val words = when {
            !model.exists() || !Whisper.available -> null
            // Twice begun and never finished: it's what closed refuge, so it's kept without its words.
            pending.tries >= 2 -> null.also { Problems.note("transcribing a voice note closed refuge twice, so it's kept without its words") }
            else -> {
                // Written down first, so a crash in Whisper is known next time.
                write(context, pending.copy(tries = pending.tries + 1))
                Whisper.words(model.path, FloatArray(samples.size) { samples[it] / 32768f }, settings.voiceLanguage)
                    .also { if (it == null) Problems.note("transcribing a voice note: Whisper couldn't load the model, or failed") }
            }
        }
        if (journal) repo.voiceTranscribed(day, name, words) else repo.voiceNoteTranscribed(name, words)
        VoiceRecorder.edits.tryEmit(VoiceRecorder.Companion.Edit.Heard(name, words))
        forget(context, start)
    }

    /**
     * When refuge starts: whatever was left on the way is taken on from where it was, and a recording
     * still waiting to be told where it goes asks again.
     */
    fun resume(context: Context) {
        val app = context.applicationContext
        val waiting = pending(app)
        if (waiting.isEmpty()) return
        // One in the making now is the recorder's to finish.
        if (VoiceRecorder.state.value !is VoiceRecorder.State.Idle) return
        scope.launch {
            waiting.forEach { p ->
                if (p.tries > 0) Problems.note("transcribing a voice note didn't finish last time (refuge was closed, or crashed); trying again")
                if (p.to == null) ask(app, p) else runCatching { process(app, p.start) }.onFailure { Problems.note("finishing a voice note", it) }
            }
        }
    }

    /** "A voice note is waiting": where should it go? Asked from a notification, after refuge was closed. */
    private fun ask(context: Context, pending: PendingVoice) {
        val system = channel(context)
        val choose = { code: Int, to: String ->
            PendingIntent.getBroadcast(
                context, (pending.start % 100_000).toInt() * 2 + code,
                Intent(context, VoiceChoice::class.java).putExtra(EXTRA_START, pending.start).putExtra(EXTRA_TO, to),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
        }
        val at = Instant.ofEpochMilli(pending.start).atZone(ZoneId.systemDefault()).toLocalDateTime()
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle("a voice note is waiting")
            .setContentText("${Voice.length(pending.seconds)} from ${at.format(DateTimeFormatter.ofPattern("HH:mm"))}: where should it go?")
            .addAction(0, "to the microjournal", choose(0, "journal"))
            .addAction(0, "to a note", choose(1, "note"))
            .build()
        runCatching { system.notify(notice(pending.start), notification) }
    }

    /** One notice per recording: where it goes, or why it's still waiting. */
    private fun notice(start: Long) = (start % Int.MAX_VALUE).toInt()

    private fun channel(context: Context): NotificationManager {
        val system = context.getSystemService(NotificationManager::class.java)
        if (system.getNotificationChannel(CHANNEL) == null) {
            system.createNotificationChannel(NotificationChannel(CHANNEL, "voice notes waiting", NotificationManager.IMPORTANCE_DEFAULT))
        }
        return system
    }

    /**
     * The recording couldn't reach your folder: why, kept in recent problems and told in a notice that
     * stays (not a toast gone in a blink), with the reassurance that it's safe and tried again.
     */
    private fun stuck(context: Context, pending: PendingVoice, why: String) {
        Problems.note("filing a voice note: $why")
        val text = "$why. it's kept safe in refuge, and filed when refuge next opens."
        val notification = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle("a voice note is waiting")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .build()
        runCatching { channel(context).notify(notice(pending.start), notification) }
    }

    /** The answer to "a voice note is waiting", from its notification. */
    class VoiceChoice : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val start = intent.getLongExtra(EXTRA_START, 0L)
            val to = Voice.target(intent.getStringExtra(EXTRA_TO)) ?: return
            context.getSystemService(NotificationManager::class.java)?.cancel(notice(start))
            choose(context, start, to)
            val app = context.applicationContext
            scope.launch { runCatching { process(app, start) }.onFailure { Problems.note("finishing a voice note", it) } }
        }
    }

    private const val CHANNEL = "voice_waiting"
    private const val EXTRA_START = "start"
    private const val EXTRA_TO = "to"

    private suspend fun say(context: Context, text: String) = withContext(Dispatchers.Main) { Toast.makeText(context, text, Toast.LENGTH_SHORT).show() }

    /** The folder you chose, as a document to write in. */
    private fun top(tree: Uri): Uri = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))

    /** The folder called [name] inside yours, made if it isn't there. */
    private fun subfolder(context: Context, tree: Uri, name: String): Uri? = runCatching {
        children(context, tree, top(tree)).firstOrNull { it.second == name && it.third }?.first
            ?: DocumentsContract.createDocument(context.contentResolver, top(tree), DocumentsContract.Document.MIME_TYPE_DIR, name)
    }.getOrNull()

    /** What's in [folder]: each as its document, name, and whether it's a folder. */
    private fun children(context: Context, tree: Uri, folder: Uri): List<Triple<Uri, String, Boolean>> = runCatching {
        val list = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getDocumentId(folder))
        val columns = arrayOf(DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME, DocumentsContract.Document.COLUMN_MIME_TYPE)
        context.contentResolver.query(list, columns, null, null, null)?.use { c ->
            buildList {
                while (c.moveToNext()) add(Triple(DocumentsContract.buildDocumentUriUsingTree(tree, c.getString(0)), c.getString(1), c.getString(2) == DocumentsContract.Document.MIME_TYPE_DIR))
            }
        }.orEmpty()
    }.getOrDefault(emptyList())

    /** The folder's files, so a recording never writes over another. */
    private fun names(context: Context, tree: Uri, folder: Uri): Set<String> = children(context, tree, folder).map { it.second }.toSet()

    private fun copyInto(context: Context, parent: Uri, name: String, file: File): Boolean = runCatching {
        val target = DocumentsContract.createDocument(context.contentResolver, parent, "audio/mp4", name) ?: return false
        context.contentResolver.openOutputStream(target, "w")?.use { out -> file.inputStream().use { it.copyTo(out) } } ?: return false
        true
    }.getOrDefault(false)

    /** [samples] as AAC in an m4a file: small, and every player and Obsidian take it. */
    private fun encode(samples: ShortArray, out: File) {
        val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, VoiceRecorder.RATE, 1).apply {
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
                        val time = at * 1_000_000L / VoiceRecorder.RATE
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

}
