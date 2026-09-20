package app.bedtime.unlock

import app.bedtime.data.Passages
import app.bedtime.data.TextSource
import app.bedtime.unlock.TextChallenge.Outcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Random

class TextChallengeTest {
    private val target = "ab3de"

    @Test
    fun generatesRequestedLengthFromAlphabet() {
        val text = TextChallenge.generate(300, random = Random(42))
        assertEquals(300, text.length)
        assertTrue(text.all { it in TextChallenge.ALPHABET })
    }

    @Test
    fun wordsAndPassagesEndOnAWholeWord() {
        for (source in listOf(TextSource.WORDS, TextSource.PASSAGES)) {
            val text = TextChallenge.generate(200, source, Random(9))
            assertTrue("$source is about the length asked for", text.length in 150..200)
            assertTrue("$source ends on a letter", text.last().isLetterOrDigit())
            assertTrue("$source reads as words", text.contains(' '))
        }
    }

    @Test
    fun passagesComeFromTheCheckedTexts() {
        val text = TextChallenge.generate(60, TextSource.PASSAGES, Random(3))
        assertTrue(Passages.ALL.any { it.startsWith(text.take(30)) })
    }

    @Test
    fun spacesAndPunctuationFillThemselvesIn() {
        val passage = "Be still, now."
        var step = TextChallenge.advance(passage, "", "b")
        assertEquals(TextChallenge.Step("B", Outcome.ACCEPTED), step)
        step = TextChallenge.advance(passage, step.typed, step.typed + "e")
        assertEquals(TextChallenge.Step("Be", Outcome.ACCEPTED), step)
        // The next letter carries the space along with it.
        step = TextChallenge.advance(passage, step.typed, step.typed + "s")
        assertEquals(TextChallenge.Step("Be s", Outcome.ACCEPTED), step)
        // And the comma rides along with the "n" that follows it.
        step = TextChallenge.advance(passage, "Be still", "Be still" + "n")
        assertEquals(TextChallenge.Step("Be still, n", Outcome.ACCEPTED), step)
    }

    @Test
    fun acceptsCorrectNextCharacterCaseInsensitively() {
        assertEquals(TextChallenge.Step("a", Outcome.ACCEPTED), TextChallenge.advance(target, "", "a"))
        assertEquals(TextChallenge.Step("ab", Outcome.ACCEPTED), TextChallenge.advance(target, "a", "aB"))
    }

    @Test
    fun rejectsWrongCharacter() {
        assertEquals(TextChallenge.Step("a", Outcome.REJECTED), TextChallenge.advance(target, "a", "ax"))
    }

    @Test
    fun rejectsPasteEvenWhenCorrect() {
        assertEquals(TextChallenge.Step("", Outcome.REJECTED), TextChallenge.advance(target, "", "ab3"))
    }

    @Test
    fun rejectsEditsThatChangeEarlierText() {
        assertEquals(TextChallenge.Step("ab", Outcome.REJECTED), TextChallenge.advance(target, "ab", "xb3"))
    }

    @Test
    fun ignoresDeletionAndWhitespace() {
        assertEquals(TextChallenge.Step("ab", Outcome.IGNORED), TextChallenge.advance(target, "ab", "a"))
        assertEquals(TextChallenge.Step("ab", Outcome.IGNORED), TextChallenge.advance(target, "ab", "ab "))
    }

    @Test
    fun acceptsNothingPastTheEnd() {
        assertEquals(TextChallenge.Step(target, Outcome.REJECTED), TextChallenge.advance(target, target, target + "a"))
    }

    @Test
    fun passwordHashRoundTrips() {
        val (hash, salt) = PasswordHasher.hash("correct horse")
        assertTrue(PasswordHasher.verify("correct horse", hash, salt))
        assertFalse(PasswordHasher.verify("wrong horse", hash, salt))
    }
}
