package app.bedtime.data

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * "Why am I unlocking my phone right now?": a question after an unlock, or before a distracting app,
 * with a few words and tags to answer it, all kept in one note. After Struthless.
 */
object Why {
    const val NOTE_ID = "why"
    const val NOTE_TITLE = "# Why I picked up the phone"

    const val QUESTION = "Why am I unlocking my phone right now?"
    const val NUDGE = "Do I want to get brainwashed by a trillion dollar addictive panic machine right now?"
    val TAGS = listOf("Hungry", "Angry", "Lonely", "Tired")

    /** Not asked again for the same app this soon after, nor again after an unlock. */
    const val APP_AGAIN_MS = 10 * 60_000L
    const val UNLOCK_AGAIN_MS = 3 * 60_000L

    fun question(settings: AppSettings, appName: String?): String =
        appName?.let { "Why am I opening $it right now?" } ?: settings.whyQuestion?.takeIf { it.isNotBlank() } ?: QUESTION

    fun nudge(settings: AppSettings): String = settings.whyNudge?.takeIf { it.isNotBlank() } ?: NUDGE

    fun tags(settings: AppSettings): List<String> = settings.whyTags ?: TAGS

    /** Whether [mode] asks now, with a session running or not. */
    fun asks(mode: AskWhen, inSession: Boolean): Boolean = mode == AskWhen.ALWAYS || (mode == AskWhen.SESSIONS && inSession)

    /** Whether it's been long enough since [last] (0 for never) to ask again, at [now]. */
    fun due(last: Long, now: Long, again: Long): Boolean = last == 0L || now - last >= again

    private val stamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    /**
     * An answer as a line of the note: "- 2026-10-31 21:40 · unlocking · tired, lonely · put it down: wanted
     * to check mail". [what] is "unlocking" or the app's name; [wentOn], whether you went on.
     */
    fun line(at: LocalDateTime, what: String, tags: List<String>, words: String, wentOn: Boolean): String =
        listOfNotNull(
            "- ${stamp.format(at)}",
            what,
            tags.joinToString(", ") { it.lowercase() }.takeIf { it.isNotEmpty() },
            if (wentOn) "went on" else "put it down",
        ).joinToString(" · ") + words.trim().replace(Regex("\\s*\\n\\s*"), " ").takeIf { it.isNotEmpty() }?.let { ": $it" }.orEmpty()

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
