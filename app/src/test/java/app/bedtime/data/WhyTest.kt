package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class WhyTest {
    @Test
    fun askedOnlyWhenSwitchedOnAndNotTooSoon() {
        assertFalse(Why.asks(AskWhen.OFF, inSession = true))
        assertTrue(Why.asks(AskWhen.SESSIONS, inSession = true))
        assertFalse(Why.asks(AskWhen.SESSIONS, inSession = false))
        assertTrue(Why.asks(AskWhen.ALWAYS, inSession = false))
        assertTrue(Why.due(0, 1_000, Why.APP_AGAIN_MS))
        assertFalse(Why.due(1_000, 1_000 + Why.APP_AGAIN_MS - 1, Why.APP_AGAIN_MS))
        assertTrue(Why.due(1_000, 1_000 + Why.APP_AGAIN_MS, Why.APP_AGAIN_MS))
    }

    @Test
    fun anAnswerIsOneLineOfItsNote() {
        val at = LocalDateTime.of(2026, 10, 31, 21, 40)
        assertEquals("- 2026-10-31 21:40 · unlocking · tired, lonely · put it down: wanted to\ncheck mail".replace("\n", " "),
            Why.line(at, "unlocking", listOf("Tired", "Lonely"), "wanted to\ncheck mail", wentOn = false))
        assertEquals("- 2026-10-31 21:40 · Instagram · went on", Why.line(at, "Instagram", emptyList(), " ", wentOn = true))
    }

    @Test
    fun theWordsAreYoursOrSuggested() {
        assertEquals(Why.QUESTION, Why.question(AppSettings(), null))
        assertEquals("Why am I opening Instagram right now?", Why.question(AppSettings(), "Instagram"))
        assertEquals("What for?", Why.question(AppSettings(whyQuestion = "What for?"), null))
        assertEquals(Why.TAGS, Why.tags(AppSettings()))
        assertEquals(emptyList<String>(), Why.tags(AppSettings(whyTags = emptyList())))
        assertEquals(4, Why.words("one two, three — four"))
    }

    @Test
    fun theAnswersComeFirstWhilePinned() {
        val notes = listOf(Note("a", "x", 1), Note(Notes.TODO_ID, "t", 1), Note(Why.NOTE_ID, "w", 1, pinned = true))
        assertEquals(listOf(Why.NOTE_ID, Notes.TODO_ID, "a"), NoteShelf.ordered(notes).map { it.id })
    }

    @Test
    fun writingLessIsLooser() {
        val strict = UnlockConfig(writeEnabled = true, writeWords = 50)
        assertTrue(app.bedtime.engine.Limits.isLooser(strict, strict.copy(writeWords = 20)))
        assertTrue(app.bedtime.engine.Limits.isLooser(strict, strict.copy(writeEnabled = false)))
        assertFalse(app.bedtime.engine.Limits.isLooser(strict, strict.copy(writeWords = 80)))
    }

    @Test
    fun writingIsTypedNotPasted() {
        assertTrue(Why.typed("I want to", "I want to c"))
        assertTrue(Why.typed("I want to", "I want to check "))
        // A suggestion swapping a word, or swipe typing a long one, is still typing.
        assertTrue(Why.typed("I want to chek", "I want to check the"))
        assertTrue(Why.typed("I", "I unbelievably"))
        assertTrue(Why.typed("I want to check", "I want"))
        // A sentence at once is a paste.
        assertFalse(Why.typed("", "I want to check my mail and see what's new"))
        assertFalse(Why.typed("so ", "so here is a whole pasted line of words"))
    }
}
