package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class VoiceTest {
    private val at = LocalDateTime.of(2026, 11, 4, 14, 32, 7)

    @Test
    fun aRecordingsNameAndLength() {
        assertEquals("2026-11-04 1432.m4a", Voice.fileName(at))
        // Another in the same minute keeps its seconds, so neither writes over the other.
        assertEquals("2026-11-04 143207.m4a", Voice.fileName(at, setOf("2026-11-04 1432.m4a")))
        assertEquals("0:42", Voice.length(42))
        assertEquals("12:05", Voice.length(725))
    }

    @Test
    fun theWordsTakeThePlaceOfTranscribing() {
        val file = "2026-11-04 1432.m4a"
        val text = "- 09:00 · coffee\n" + Voice.line("14:32", 42, file)
        assertEquals("- 09:00 · coffee\n- 14:32 · 🎙 0:42 · transcribing… · $file", text)
        assertEquals("- 09:00 · coffee\n- 14:32 · 🎙 0:42 · walked home, thought about it · $file", Voice.transcribed(text, file, " Walked home,\n thought about it "))
        // No words (no model, or nothing heard): just the recording.
        assertEquals("- 09:00 · coffee\n- 14:32 · 🎙 0:42 · $file", Voice.transcribed(text, file, null))
        // Another recording's line is left as it is.
        assertEquals(text, Voice.transcribed(text, "2026-11-04 1500.m4a", "other"))
    }

    @Test
    fun readyOnlyWhenSwitchedOnWithAFolderAndAModel() {
        assertFalse(Voice.ready(AppSettings()))
        assertFalse(Voice.ready(AppSettings(voiceOn = true, voiceFolder = "content://tree")))
        assertFalse(Voice.ready(AppSettings(voiceFolder = "content://tree", voiceModel = "ggml-base-q5_1.bin")))
        assertTrue(Voice.ready(AppSettings(voiceOn = true, voiceFolder = "content://tree", voiceModel = "ggml-base-q5_1.bin")))
    }

    @Test
    fun anEntrysRecordingsToPlay() {
        val text = "- 09:00 · coffee\n- 14:32 · 🎙 0:42 · walked home · 2026-11-04 1432.m4a\n- 15:10 · 🎙 1:05 · 2026-11-04 1510.m4a"
        assertEquals(listOf("14:32 · 0:42" to "2026-11-04 1432.m4a", "15:10 · 1:05" to "2026-11-04 1510.m4a"), Voice.recordings(text))
        assertEquals(emptyList<Pair<String, String>>(), Voice.recordings("- 09:00 · a file.m4a I mentioned"))
    }

    @Test
    fun aVoiceNotesOwnNote() {
        val line = Voice.line("14:32", 42, "2026-11-04 1432.m4a")
        assertEquals("# voice note, 4 november\n\n$line", Voice.noteText(at, line))
        assertEquals(listOf("14:32 · 0:42" to "2026-11-04 1432.m4a"), Voice.recordings(Voice.noteText(at, line)))
    }

    @Test
    fun aRecordingComesIntoAnOpenPageAboveTheFreshLine() {
        val line = "- 14:32 · 🎙 0:42 · transcribing… · 2026-11-04 1432.m4a"
        assertEquals("- 09:00 · coffee\n$line\n- 14:40 · ", Voice.added("- 09:00 · coffee\n- 14:40 · ", line))
        assertEquals("- 09:00 · coffee\n$line", Voice.added("- 09:00 · coffee", line))
        assertEquals(line, Voice.added("", line))
    }
}
