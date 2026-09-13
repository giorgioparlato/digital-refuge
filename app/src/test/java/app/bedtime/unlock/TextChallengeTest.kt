package app.bedtime.unlock

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
        val text = TextChallenge.generate(300, Random(42))
        assertEquals(300, text.length)
        assertTrue(text.all { it in TextChallenge.ALPHABET })
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
