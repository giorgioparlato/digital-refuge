package app.bedtime.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/** Where a voice note goes: a line in today's microjournal, or a note of its own. */
@kotlinx.serialization.Serializable
enum class VoiceTo { JOURNAL, NOTE }

/**
 * Voice notes: a recording kept as a file in your folder (the microjournal's in a folder of their own
 * inside it), and a line for it, in today's microjournal or a new note, whose words follow once
 * Whisper has heard them.
 */
object Voice {
    /** The folder, inside yours, that the microjournal's recordings go in. */
    const val JOURNAL_FOLDER = "microjournal"

    /** A voice note's own note: "# voice note, 4 november" and its line. */
    fun noteText(at: LocalDateTime, line: String): String =
        "# voice note, ${at.format(DateTimeFormatter.ofPattern("d MMMM", java.util.Locale.getDefault())).lowercase()}\n\n$line"

    private val named = DateTimeFormatter.ofPattern("yyyy-MM-dd HHmm")

    /** "2026-11-04 1432.m4a", or with seconds when a minute already has one. */
    fun fileName(at: LocalDateTime, taken: Set<String> = emptySet()): String {
        val plain = "${named.format(at)}.m4a"
        return if (plain !in taken) plain else "${named.format(at)}${"%02d".format(at.second)}.m4a"
    }

    /** "0:42", "12:05". */
    fun length(seconds: Int): String = "${seconds / 60}:${"%02d".format(seconds % 60)}"

    /** Where the words go until they're there. */
    const val TRANSCRIBING = "transcribing…"

    /** "- 14:32 · 🎙 0:42 · transcribing… · 2026-11-04 1432.m4a". */
    fun line(time: String, seconds: Int, file: String): String = Journal.line(time, "🎙 ${length(seconds)} · $TRANSCRIBING · $file")

    /**
     * [text] with the line for [file] given its [words], lowercase like everything refuge writes; with
     * none (no model, nothing heard), just the recording.
     */
    fun transcribed(text: String, file: String, words: String?): String =
        text.lines().joinToString("\n") { line ->
            if (file !in line || TRANSCRIBING !in line) line
            else line.replace(" · $TRANSCRIBING", words?.trim()?.replace(Regex("\\s+"), " ")?.lowercase()?.takeIf { it.isNotEmpty() }?.let { " · $it" }.orEmpty())
        }

    private val RECORDING = Regex("""^- (\d{1,2}:\d{2}) · 🎙 (\d+:\d{2}) · .*?(\d{4}-\d{2}-\d{2} \d{4,6}\.m4a)\s*$""")

    /** The recordings in an entry's [text], in order: when, how long ("14:32 · 0:42"), and the file. */
    fun recordings(text: String): List<Pair<String, String>> =
        text.lines().mapNotNull { RECORDING.matchEntire(it.trim()) }.map { m -> "${m.groupValues[1]} · ${m.groupValues[2]}" to m.groupValues[3] }

    /** Whether voice notes can be recorded, so their buttons show: switched on, with a folder and a model chosen. */
    fun ready(settings: AppSettings): Boolean = settings.voiceOn && settings.voiceFolder != null && settings.voiceModel != null

    /** Recordings stop on their own after this long. */
    const val LONGEST_SECONDS = 30 * 60

    /** Languages Whisper is told to listen for: "auto" finds it itself. */
    val LANGUAGES = listOf("auto" to "any", "en" to "english", "it" to "italiano", "sv" to "svenska", "de" to "deutsch", "fr" to "français", "es" to "español")
}
