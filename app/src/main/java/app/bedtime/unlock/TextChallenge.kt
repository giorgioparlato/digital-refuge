package app.bedtime.unlock

import app.bedtime.data.Passages
import app.bedtime.data.TextSource
import java.security.SecureRandom
import java.util.Random

/** Cold Turkey–style "type this random text" challenge. */
object TextChallenge {
    /** Lowercase letters and digits, minus look-alikes (i, l, o, 0, 1). */
    const val ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789"

    enum class Outcome { ACCEPTED, IGNORED, REJECTED }

    data class Step(val typed: String, val outcome: Outcome)

    fun generate(length: Int, source: TextSource = TextSource.LETTERS, random: Random = SecureRandom()): String =
        when (source) {
            TextSource.LETTERS -> buildString(length) { repeat(length) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }
            TextSource.WORDS -> fromPieces(length, random) { Passages.WORDS[random.nextInt(Passages.WORDS.size)] }
            TextSource.PASSAGES -> fromPieces(length, random) { Passages.ALL[random.nextInt(Passages.ALL.size)] }
        }

    /**
     * Strings pieces together until there's at least [length] to copy, then cuts back to the last
     * whole word so nobody is asked to type half of one. Trailing punctuation goes too, so finishing
     * the last letter finishes the challenge.
     */
    private inline fun fromPieces(length: Int, random: Random, piece: () -> String): String {
        val text = StringBuilder(piece())
        while (text.length < length) text.append(' ').append(piece())
        if (text.length > length) {
            val cut = text.lastIndexOf(" ", length)
            text.setLength(if (cut > 0) cut else length)
        }
        while (text.isNotEmpty() && !text.last().isLetterOrDigit()) text.setLength(text.length - 1)
        return text.toString()
    }

    /**
     * The most characters one field change may add. A keyboard sometimes delivers two or three at
     * once when typing quickly, and rejecting those looked like the app dropping letters. Anything
     * longer than a burst is a paste.
     */
    const val MAX_BURST = 8

    /**
     * Applies a text-field change. Progress only moves forward, and only over correct characters:
     * insertions longer than [MAX_BURST] (paste) and wrong characters are rejected; deletions and
     * whitespace are ignored. Spaces and punctuation in the target fill themselves in, so a passage
     * is typed letter by letter without hunting for the comma key.
     *
     * A burst that starts correctly and then goes wrong keeps the letters that were right, so a
     * typo at speed costs no more than a typo typed slowly.
     */
    fun advance(target: String, typed: String, newValue: String): Step {
        if (newValue.length <= typed.length) return Step(typed, Outcome.IGNORED)
        if (!newValue.startsWith(typed)) return Step(typed, Outcome.REJECTED)
        val added = newValue.substring(typed.length)
        if (added.length > MAX_BURST) return Step(typed, Outcome.REJECTED)
        var progress = typed
        var moved = false
        for (typedChar in added) {
            val char = typedChar.lowercaseChar()
            if (char.isWhitespace()) continue
            var next = progress.length
            while (next < target.length && !target[next].isLetterOrDigit()) next++
            if (next >= target.length || char != target[next].lowercaseChar()) {
                return Step(progress, Outcome.REJECTED)
            }
            progress = target.substring(0, next + 1)
            moved = true
        }
        return Step(progress, if (moved) Outcome.ACCEPTED else Outcome.IGNORED)
    }
}
