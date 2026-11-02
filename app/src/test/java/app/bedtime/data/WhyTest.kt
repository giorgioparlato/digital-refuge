package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class WhyTest {
    @Test
    fun askedAfterUnlockingAlwaysOrInASessionThatAsks() {
        assertEquals(null, Why.afterUnlock(AskWhen.OFF, emptyList()))
        assertEquals(0, Why.afterUnlock(AskWhen.ALWAYS, emptyList()))
        // A running session that asks does so whatever the general setting, with its own words at least.
        val asking = Schedule(askWhy = true, askWhyWords = 20)
        assertEquals(20, Why.afterUnlock(AskWhen.OFF, listOf(Schedule(), asking)))
        assertEquals(20, Why.afterUnlock(AskWhen.ALWAYS, listOf(asking)))
        assertEquals(null, Why.afterUnlock(AskWhen.OFF, listOf(Schedule())))
        // "Sessions", from before, is now never.
        assertEquals(null, Why.afterUnlock(AskWhen.SESSIONS, emptyList()))
        assertTrue(Why.due(0, 1_000, Why.APP_AGAIN_MS))
        assertFalse(Why.due(1_000, 1_000 + Why.APP_AGAIN_MS - 1, Why.APP_AGAIN_MS))
        assertTrue(Why.due(1_000, 1_000 + Why.APP_AGAIN_MS, Why.APP_AGAIN_MS))
    }

    @Test
    fun anAnswerIsOneLineOfTheDaysEntry() {
        val at = LocalDateTime.of(2026, 10, 31, 21, 40)
        assertEquals(
            "- 21:40 · unlocking · put it down · tired, bored · what do i actually need right now? sleep, not mail",
            Why.line(at, "unlocking", "What do I actually need right now?", "sleep,\nnot mail", wentOn = false, moods = listOf("tired", "bored")),
        )
        // Nothing written, just when and what came of it.
        assertEquals("- 21:40 · instagram · went on", Why.line(at, "Instagram", "Why?", " ", wentOn = true))
    }

    @Test
    fun moodsUnlessSwitchedOff() {
        assertEquals(Why.MOODS, Why.moods(AppSettings()))
        assertEquals(listOf("calm"), Why.moods(AppSettings(moodTags = listOf("calm"))))
        assertEquals(emptyList<String>(), Why.moods(AppSettings(moodsOn = false)))
    }

    @Test
    fun aDifferentQuestionEachTime() {
        assertEquals(Why.QUESTIONS, Why.questions(AppSettings()))
        assertEquals(listOf("What for?"), Why.questions(AppSettings(whyQuestions = listOf("What for?"))))
        // An emptied list still asks something.
        assertEquals(Why.APP_QUESTIONS, Why.appQuestions(AppSettings(whyAppQuestions = emptyList())))
        val asked = (1..50).map { Why.next(listOf("a", "b", "c"), "a", kotlin.random.Random(it)) }
        assertFalse("a" in asked)
        assertEquals("a", Why.next(listOf("a"), "a"))
        assertEquals(4, Why.words("one two, three — four"))
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
