package app.bedtime.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * "Why am I unlocking my phone right now?": a question after an unlock, or in a limit's pause, a
 * different one each time, with room to write an answer, all kept in one note. After Struthless.
 */
object Why {
    const val NOTE_ID = "why"
    const val NOTE_TITLE = "# why i picked up the phone"

    /** Asked in turn after unlocking: the first and the "trillion dollar" one are Struthless's. Written by docs/text/journal_to_code.py. */
    val QUESTIONS = listOf(
        "why am i unlocking my phone right now?",
        "what am i hoping to find here?",
        "what do i actually need right now?",
        "is this a choice, or a habit?",
        "how do i want to feel when i put it down?",
        "how long do i want to spend on my phone?",
        "is there something i'm avoiding?",
        "will i be glad i opened it, an hour from now?",
        "why do i want to get brainwashed by a trillion dollar addictive panic machine right now?",
    )

    /** Asked in turn before a limit's apps, under "about to open …". Written by docs/text/journal_to_code.py. */
    val APP_QUESTIONS = listOf(
        "why am i opening this right now?",
        "what am i looking for in here?",
        "what am i hoping to find here?",
        "what do i actually need right now?",
        "is this a choice, or a habit?",
        "how long do i want to spend in it?",
        "what will i do when i close it?",
        "is there something i'm avoiding?",
        "will i be glad i opened it, an hour from now?",
        "why do i want to get brainwashed by a trillion dollar addictive panic machine right now?",
    )


    /** Not asked again for the same app this soon after, nor again after an unlock. */
    const val APP_AGAIN_MS = 10 * 60_000L
    const val UNLOCK_AGAIN_MS = 3 * 60_000L

    fun questions(settings: AppSettings): List<String> = settings.whyQuestions?.takeIf { it.isNotEmpty() } ?: QUESTIONS

    fun appQuestions(settings: AppSettings): List<String> = settings.whyAppQuestions?.takeIf { it.isNotEmpty() } ?: APP_QUESTIONS

    /** One of [questions], a different one each time: not the same as [last] when there's a choice. */
    fun next(questions: List<String>, last: String?, random: kotlin.random.Random = kotlin.random.Random.Default): String =
        questions.filter { it != last }.ifEmpty { questions }.random(random)

    /** The question asked last, so the next is another. */
    @Volatile var lastAsked: String? = null

    /** The question to ask now from [questions], noted as asked. */
    fun ask(questions: List<String>): String = next(questions, lastAsked).also { lastAsked = it }


    /** Whether [mode] asks now, with a session running or not. */
    fun asks(mode: AskWhen, inSession: Boolean): Boolean = mode == AskWhen.ALWAYS || (mode == AskWhen.SESSIONS && inSession)

    /** Whether it's been long enough since [last] (0 for never) to ask again, at [now]. */
    fun due(last: Long, now: Long, again: Long): Boolean = last == 0L || now - last >= again

    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /**
     * An answer as a line of the note: "- 2026-10-31 21:40 · unlocking · put it down · What do I actually
     * need right now? sleep". [what] is "unlocking" or the app's name; [wentOn], whether you went on.
     */
    fun line(at: LocalDateTime, what: String, question: String, words: String, wentOn: Boolean): String =
        listOf("- ${stamp.format(at)}", what.lowercase(), if (wentOn) "went on" else "put it down").joinToString(" · ") +
            words.trim().replace(Regex("\\s*\\n\\s*"), " ").takeIf { it.isNotEmpty() }?.let { " · ${question.lowercase()} $it" }.orEmpty()

    /** The most one change may add while writing: a word or two at a time, as typing, swiping or a suggestion gives them. */
    const val MAX_ADDED_CHARS = 30
    const val MAX_ADDED_WORDS = 2

    /**
     * Whether [new] could have been written from [old] by hand: anything taken away, or what's put in
     * at most a word or two at once. More in one go is a paste, and doesn't count towards the words.
     */
    fun typed(old: String, new: String): Boolean {
        if (new.length <= old.length) return true
        var start = 0
        while (start < old.length && old[start] == new[start]) start++
        var end = 0
        while (end < old.length - start && old[old.length - 1 - end] == new[new.length - 1 - end]) end++
        val added = new.substring(start, new.length - end)
        return added.length <= MAX_ADDED_CHARS && words(added) <= MAX_ADDED_WORDS
    }

    /** Words in [text], for a writing step's count. */
    fun words(text: String): Int = text.split(Regex("\\s+")).count { w -> w.any { it.isLetterOrDigit() } }
}
