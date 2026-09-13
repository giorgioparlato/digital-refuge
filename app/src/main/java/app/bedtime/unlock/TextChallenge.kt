package app.bedtime.unlock

import java.security.SecureRandom
import java.util.Random

/** Cold Turkey–style "type this random text" challenge. */
object TextChallenge {
    /** Lowercase letters and digits, minus look-alikes (i, l, o, 0, 1). */
    const val ALPHABET = "abcdefghjkmnpqrstuvwxyz23456789"

    enum class Outcome { ACCEPTED, IGNORED, REJECTED }

    data class Step(val typed: String, val outcome: Outcome)

    fun generate(length: Int, random: Random = SecureRandom()): String =
        buildString(length) { repeat(length) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }

    /**
     * Applies a text-field change. Progress only moves forward, one correct character at a time:
     * multi-character insertions (paste, autocomplete) and wrong characters are rejected;
     * deletions and whitespace are ignored.
     */
    fun advance(target: String, typed: String, newValue: String): Step {
        if (newValue.length <= typed.length) return Step(typed, Outcome.IGNORED)
        if (newValue.length > typed.length + 1 || !newValue.startsWith(typed)) return Step(typed, Outcome.REJECTED)
        val char = newValue.last().lowercaseChar()
        if (char.isWhitespace()) return Step(typed, Outcome.IGNORED)
        return if (typed.length < target.length && char == target[typed.length]) {
            Step(typed + char, Outcome.ACCEPTED)
        } else {
            Step(typed, Outcome.REJECTED)
        }
    }
}
