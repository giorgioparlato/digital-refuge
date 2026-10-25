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
     * The words for [choice]: one from the mix, or the one you chose. A chosen message that has since
     * been deleted falls back to the mix; null only if there's somehow nothing to say, and the cover
     * keeps its own words.
     */
    fun pick(choice: CoverMessage, custom: List<CustomMessage>, random: Random = Random.Default): String? {
        val chosen = choice.text?.takeIf { choice.mode == MessageMode.CHOSEN }?.takeIf { text -> text in presets || custom.any { it.text == text } }
        return chosen ?: pool(custom).takeIf { it.isNotEmpty() }?.random(random)
    }

    /** The choice in a few words, for the row that opens it. */
    fun summary(choice: CoverMessage): String = when (choice.mode) {
        MessageMode.CHOSEN -> "“${choice.text.orEmpty()}”"
        else -> "A different one each time"
    }
}
