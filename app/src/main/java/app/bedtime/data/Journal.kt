package app.bedtime.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/**
 * One kind of journalling, with a line on how to go about it and the questions it asks, each perhaps
 * with an example to show what an answer looks like ([hints], by question). Yours to change.
 */
@Serializable
data class JournalSection(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val guide: String = "",
    val prompts: List<String> = emptyList(),
    val hints: Map<String, String> = emptyMap(),
)

/** A day's microjournal entry: what was written, and the sections it drew on, by name, as they were then. */
@Serializable
data class JournalEntry(
    val day: Long,
    val text: String,
    val aims: List<String> = emptyList(),
    val editedAt: Long = 0L,
    /** How you felt, each time you said, for the mood tracker as well as the entry's title. */
    val moods: List<MoodMark> = emptyList(),
)

/** A mood tapped, and when. */
@Serializable
data class MoodMark(val at: Long, val mood: String)

/**
 * The microjournal: an entry a day, and sections of prompts to write from. The suggested sections come
 * from Struthless's "The Journalling Techniques that Changed My Life", and self compassion's three steps
 * from Kristin Neff; their words live in docs/text/journal.md. Each is only a start, to rename,
 * rewrite, delete or add to.
 */
object Journal {
    /** A section from its questions, each with its example, or null for none. Written by docs/text/journal_to_code.py. */
    private fun section(id: String, name: String, guide: String, vararg prompts: Pair<String, String?>) =
        JournalSection(id, name, guide, prompts.map { it.first }, prompts.mapNotNull { (q, h) -> h?.let { q to it } }.toMap())

    val SUGGESTED: List<JournalSection> = listOf(
        section(
            "clarity", "clarity",
            "write every single thought as you have it. in one sitting, or jot each new thought down as it comes. once you see the words in front of you, they don't look as scary.",
        ),
        section(
            "fixation", "pattern interrupt",
            "for when you get fixated on something that you can't think about other stuff.",
            "imagine six impossible things and write them down" to "from lewis carroll. e.g a camel and his best friend skating over an exploding pigeon.",
            "how can i make someone else happy right now?" to "you get out of serving yourself and start serving someone else.",
            "what's something in my immediate environment that i have never noticed?" to "if you're stuck in the past or the future, doing this immediately draws you to the present.",
        ),
        section(
            "reflection", "daily reflections",
            "reflect on your day with the same questions. do them most days for a month and you'll learn so much about yourself.",
            "what excited me?" to null,
            "what drained me of energy?" to null,
            "what did i learn?" to null,
            "what are 10 things i'm grateful for?" to null,
            "how did i move towards what i long for?" to null,
        ),
        section(
            "bad", "when bad things happen",
            "“there is nothing either good or bad, but thinking makes it so.”",
            "what happened, objectively?" to "separate what actually happened from the meaning you instinctively gave it.",
            "what did i make it mean?" to "here's where you get a glimpse of your cognitive distortions.",
            "how would i comfort a friend i loved if this happened to them?" to "instead of beating yourself up, treat yourself like a friend you love, and give yourself a little compassion.",
            "how is this the best thing that has ever happened to me?" to "this question can be challenging, but finding answers to it can help us reframe how we view events in our life.",
        ),
        section(
            "anxiety", "anxieties",
            "divide the page into three columns: fears, fixes, and the outcome you'd bet on.",
            "will this matter in 10 days time?" to null,
        ),
        section(
            "todo", "to-do list and direction",
            "start with a full page: a scattered list of all your obligations and the things you'd like to do. then order it with a few questions, often down to less than 15 points.",
            "everything i have to do, and would like to do" to null,
            "what is non-negotiable? what is exciting?" to "the musts, and the ones you can't wait to do.",
            "what on my list, if done, would make everything else easier?" to null,
            "what of these, if taken away from me, would i fight to get back?" to null,
            "what 20% of activities are producing 80% of the results?" to null,
        ),
        section(
            "decision", "making a decision",
            "a problem well defined is a problem half solved.",
            "what is the decision, or the problem? defined clearly" to null,
            "what are the options, and are they mutually exclusive?" to null,
            "if i had to make this decision in 60 seconds, what would i choose?" to "put a timer on and write your decision: with it ticking down, your intuition speaks up.",
            "could i live with this outcome?" to "ask it of your 60-second answer.",
            "what would this look like if it were easy?" to null,
        ),
        section(
            "direction", "direction in life",
            "like a graph: one point gives no direction; two give a relationship, and the more points, the clearer the direction.",
            "what did i want five years ago?" to null,
            "what do i want now?" to null,
            "what do i think i'll want in five years' time?" to null,
            "if i knew i couldn't fail, what would i do?" to null,
        ),
        section(
            "compassion", "self compassion",
            "two ways in: a short love letter to yourself, or three steps, noticing what you felt, seeing it's human, and answering it with kindness.",
            "write a short love letter to yourself" to "as you would to someone you love: what you appreciate, what you forgive, what you wish for them.",
            "go over the day, note down anything you've felt bad about, or" to "name what caused the difficult experience, the emotions you felt, and how they felt in the body",
            "how is this part of being human?" to "think about how that experience is connected to being an imperfect human being",
            "write yourself some kind, gentle words of understanding with a reassuring tone. show yourself some care and kindness." to null,
        ),
    )

    /** Your sections once you've changed any ([saved] isn't null), the suggested ones until then. */
    fun sections(saved: List<JournalSection>?): List<JournalSection> = saved ?: SUGGESTED

    private fun List<JournalSection>.changed(id: String, change: (JournalSection) -> JournalSection) = map { if (it.id == id) change(it) else it }

    fun renamed(sections: List<JournalSection>, id: String, name: String) = sections.changed(id) { it.copy(name = name) }

    fun withGuide(sections: List<JournalSection>, id: String, guide: String) = sections.changed(id) { it.copy(guide = guide) }

    fun withoutSection(sections: List<JournalSection>, id: String) = sections.filterNot { it.id == id }

    fun withSection(sections: List<JournalSection>, name: String) = sections + JournalSection(name = name)

    fun withPrompt(sections: List<JournalSection>, id: String, prompt: String) = sections.changed(id) { it.copy(prompts = it.prompts + prompt) }

    /** A question reworded; its example stays with it. */
    fun editedPrompt(sections: List<JournalSection>, id: String, index: Int, prompt: String) =
        sections.changed(id) { s ->
            val old = s.prompts.getOrNull(index) ?: return@changed s
            val hint = s.hints[old]
            s.copy(prompts = s.prompts.mapIndexed { i, p -> if (i == index) prompt else p }, hints = (s.hints - old) + listOfNotNull(hint?.let { prompt to it }))
        }

    fun withoutPrompt(sections: List<JournalSection>, id: String, index: Int) =
        sections.changed(id) { s -> s.copy(prompts = s.prompts.filterIndexed { i, _ -> i != index }, hints = s.hints - s.prompts.getOrNull(index).orEmpty()) }

    /** A question's example set, or taken away when empty. */
    fun withHint(sections: List<JournalSection>, id: String, prompt: String, hint: String) =
        sections.changed(id) { s -> s.copy(hints = if (hint.isBlank()) s.hints - prompt else s.hints + (prompt to hint.trim())) }

    /**
     * "saturday 31 october · clarity · tired, bored": the day, the sections written from, and the moods
     * of the day; the year too when it isn't this one.
     */
    fun title(entry: JournalEntry, today: LocalDate): String {
        val date = LocalDate.ofEpochDay(entry.day)
        val day = date.format(DateTimeFormatter.ofPattern(if (date.year == today.year) "EEEE d MMMM" else "EEEE d MMMM yyyy", Locale.getDefault())).lowercase()
        val moods = entry.moods.map { it.mood.lowercase() }.distinct()
        return listOf(day, entry.aims.joinToString(", ") { it.lowercase() }, moods.joinToString(", ")).filter { it.isNotEmpty() }.joinToString(" · ")
    }

    /** Where the answers to "why?" go in a day's entry: under this, at its end, the newest first. */
    const val WHY_HEADING = "### why i picked up the phone"

    /** [text] with [line] as the newest answer under [WHY_HEADING], the heading added at the end if it isn't there. */
    fun withWhy(text: String, line: String): String {
        val lines = text.trimEnd().lines().toMutableList()
        val at = lines.indexOfFirst { it.trim() == WHY_HEADING }
        if (at < 0) return (if (text.isBlank()) "" else text.trimEnd() + "\n\n") + "$WHY_HEADING\n$line\n"
        lines.add(at + 1, line)
        return lines.joinToString("\n") + "\n"
    }

    /** [entries] with an answer to "why?" in [day]'s entry, and the moods tapped with it. */
    fun answered(entries: List<JournalEntry>, day: Long, line: String, moods: List<String>, at: Long): List<JournalEntry> {
        val now = entries.firstOrNull { it.day == day } ?: JournalEntry(day, "")
        val updated = now.copy(text = withWhy(now.text, line), editedAt = at, moods = now.moods + moods.map { MoodMark(at, it) })
        return entries.filterNot { it.day == day } + updated
    }

    /** [entries] with [mood] on [day] taken off if it's there, or added, at [at]. */
    fun moodToggled(entries: List<JournalEntry>, day: Long, mood: String, at: Long): List<JournalEntry> {
        val now = entries.firstOrNull { it.day == day } ?: JournalEntry(day, "")
        val moods = if (now.moods.any { it.mood == mood }) now.moods.filterNot { it.mood == mood } else now.moods + MoodMark(at, mood)
        val updated = now.copy(moods = moods)
        return entries.filterNot { it.day == day } + listOfNotNull(updated.takeIf { it.text.isNotBlank() || it.moods.isNotEmpty() })
    }

    /** [text] with [prompt] put in at [cursor] in bold, on a line of its own, and where to write next: the line under it. */
    fun inserted(text: String, cursor: Int, prompt: String): Pair<String, Int> {
        val at = cursor.coerceIn(0, text.length)
        val before = text.substring(0, at)
        val after = text.substring(at)
        val lead = if (before.isEmpty() || before.endsWith("\n")) "" else "\n"
        val block = "$lead**${prompt.trim().lowercase()}**\n"
        val trail = if (after.isEmpty() || after.startsWith("\n")) "" else "\n"
        return before + block + trail + after to (before.length + block.length)
    }

    /** [entries] newest first. */
    fun entries(entries: List<JournalEntry>): List<JournalEntry> = entries.sortedByDescending { it.day }

    /** [entries] with [day]'s set to [text] and [aims], its moods kept; an entry left with nothing goes. */
    fun saved(entries: List<JournalEntry>, day: Long, text: String, aims: List<String>, at: Long): List<JournalEntry> {
        val moods = entries.firstOrNull { it.day == day }?.moods.orEmpty()
        val rest = entries.filterNot { it.day == day }
        return if (text.isBlank() && moods.isEmpty()) rest else rest + JournalEntry(day, text, aims, at, moods)
    }
}
