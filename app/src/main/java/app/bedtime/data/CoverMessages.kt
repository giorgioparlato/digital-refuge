package app.bedtime.data

import kotlin.random.Random

/** The words a cover can show: a few that ship with the app, and any you write. */
object CoverMessages {
    /** Short, and never a scolding: a reason to pause, not a lecture. */
    val presets: List<String> = listOf(
        "Is this what you came to the phone for?",
        "Maybe put the phone down for a bit.",
        "What were you about to do before this?",
        "This will still be here later.",
        "Take a breath. Then decide.",
        "Look up for a moment.",
    )

    /** Everything a cover set to "a different one each time" draws from. */
    fun pool(custom: List<CustomMessage>): List<String> = presets + custom.filter { it.inRandom }.map { it.text }

    /**
     * The words for [choice], or null for the cover's usual ones. A chosen message that has since been
     * deleted, or a mix that is somehow empty, falls back to the usual words rather than to nothing.
     */
    fun pick(choice: CoverMessage, custom: List<CustomMessage>, random: Random = Random.Default): String? = when (choice.mode) {
        MessageMode.USUAL -> null
        MessageMode.CHOSEN -> choice.text?.takeIf { text -> text in presets || custom.any { it.text == text } }
        MessageMode.RANDOM -> pool(custom).takeIf { it.isNotEmpty() }?.random(random)
    }

    /** The choice in a few words, for the row that opens it. */
    fun summary(choice: CoverMessage): String = when (choice.mode) {
        MessageMode.USUAL -> "The usual words"
        MessageMode.RANDOM -> "A different one each time"
        MessageMode.CHOSEN -> "“${choice.text.orEmpty()}”"
    }
}
