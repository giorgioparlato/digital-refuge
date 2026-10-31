package app.bedtime.data

import kotlin.random.Random

/**
 * The words a cover can show: one list, the suggested ones and yours, each to reword, delete, or keep
 * out of the mix. The suggestions live in docs/text/journal.md.
 */
object CoverMessages {
    /** Short, and never a scolding: a reason to pause, not a lecture. Written by docs/text/journal_to_code.py. */
    val presets: List<String> = listOf(
        "is this what you picked up the phone for?",
        "what were you about to do before this?",
        "what do you actually need right now?",
        "one breath first. then decide.",
        "look up for a moment. what's around you?",
        "you set this up for a reason. trust that version of you.",
        "nothing in here is urgent.",
        "the feed never ends. your evening does.",
        "boredom is where good ideas start.",
        "who would you rather spend this minute with?",
        "do i want to get brainwashed by a trillion dollar addictive panic machine right now?",
    )

    /** Every message, in order: until you change any, the suggested ones, then those you wrote. */
    fun all(settings: AppSettings): List<CustomMessage> =
        if (settings.coverMessagesEdited) settings.customMessages else presets.map { CustomMessage(it) } + settings.customMessages

    /** [settings] with its messages changed by [transform]; from then on the list is all yours. */
    fun edited(settings: AppSettings, transform: (List<CustomMessage>) -> List<CustomMessage>): AppSettings =
        settings.copy(customMessages = transform(all(settings)), coverMessagesEdited = true)

    /** Back to the suggested ones. */
    fun reset(settings: AppSettings): AppSettings = settings.copy(customMessages = emptyList(), coverMessagesEdited = false)

    /** Everything a cover set to "a different one each time" draws from. */
    fun pool(messages: List<CustomMessage>): List<String> = messages.filter { it.inRandom }.map { it.text }

    /**
     * The words for [choice]: one from the mix, or the one you chose. A chosen message that has since
     * been deleted falls back to the mix; null only if there's nothing to say, and the cover keeps its
     * own words.
     */
    fun pick(choice: CoverMessage, messages: List<CustomMessage>, random: Random = Random.Default): String? {
        val chosen = choice.text?.takeIf { choice.mode == MessageMode.CHOSEN }?.takeIf { text -> messages.any { it.text == text } }
        return chosen ?: pool(messages).takeIf { it.isNotEmpty() }?.random(random)
    }

    /** The choice in a few words, for the row that opens it. */
    fun summary(choice: CoverMessage): String = when (choice.mode) {
        MessageMode.CHOSEN -> "“${choice.text.orEmpty()}”"
        else -> "A different one each time"
    }
}
