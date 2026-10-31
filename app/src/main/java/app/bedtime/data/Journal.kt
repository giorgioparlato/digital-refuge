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
data class JournalEntry(val day: Long, val text: String, val aims: List<String> = emptyList(), val editedAt: Long = 0L)

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
            "clarity", "Clarity",
            "Write every single thought as you have it. In one sitting, or jot each new thought down as it comes. Once you see the words in front of you, they don't look as scary.",
        ),
        section(
            "fixation", "Pattern interrupt",
            "For when you get fixated on something that you can't think about other stuff.",
            "Imagine six impossible things and write them down" to "From Lewis Carroll. E.g a camel and his best friend skating over an exploding pigeon.",
            "How can I make someone else happy right now?" to "You get out of serving yourself and start serving someone else.",
            "What's something in my immediate environment that I have never noticed?" to "If you're stuck in the past or the future, doing this immediately draws you to the present.",
        ),
        section(
            "reflection", "Daily reflections",
            "Reflect on your day with the same questions. Do them most days for a month and you'll learn so much about yourself.",
            "What excited me?" to null,
            "What drained me of energy?" to null,
            "What did I learn?" to null,
            "What are 10 things I'm grateful for?" to null,
            "How did I move towards what I long for?" to null,
        ),
        section(
            "bad", "When bad things happen",
            "“There is nothing either good or bad, but thinking makes it so.”",
            "What happened, objectively?" to "Separate what actually happened from the meaning you instinctively gave it.",
            "What did I make it mean?" to "Here's where you get a glimpse of your cognitive distortions.",
            "How would I comfort a friend I loved if this happened to them?" to "Instead of beating yourself up, treat yourself like a friend you love, and give yourself a little compassion.",
            "How is this the best thing that has ever happened to me?" to "This question can be challenging, but finding answers to it can help us reframe how we view events in our life.",
        ),
        section(
            "anxiety", "Anxieties",
            "Divide the page into three columns: fears, fixes, and the outcome you'd bet on.",
            "Will this matter in 10 days time?" to null,
        ),
        section(
            "todo", "To-do list and direction",
            "Start with a full page: a scattered list of all your obligations and the things you'd like to do. Then order it with a few questions, often down to less than 15 points.",
            "Everything I have to do, and would like to do" to null,
            "What is non-negotiable? What is exciting?" to "The musts, and the ones you can't wait to do.",
            "What on my list, if done, would make everything else easier?" to null,
            "What of these, if taken away from me, would I fight to get back?" to null,
            "What 20% of activities are producing 80% of the results?" to null,
        ),
        section(
            "decision", "Making a decision",
            "A problem well defined is a problem half solved.",
            "What is the decision, or the problem? Defined clearly" to null,
            "What are the options, and are they mutually exclusive?" to null,
            "If I had to make this decision in 60 seconds, what would I choose?" to "Put a timer on and write your decision: with it ticking down, your intuition speaks up.",
            "Could I live with this outcome?" to "Ask it of your 60-second answer.",
            "What would this look like if it were easy?" to null,
        ),
        section(
            "direction", "Direction in life",
            "Like a graph: one point gives no direction; two give a relationship, and the more points, the clearer the direction.",
            "What did I want five years ago?" to null,
            "What do I want now?" to null,
            "What do I think I'll want in five years' time?" to null,
            "If I knew I couldn't fail, what would I do?" to null,
        ),
        section(
            "compassion", "Self compassion",
            "Two ways in: a short love letter to yourself, or three steps, noticing what you felt, seeing it's human, and answering it with kindness.",
            "Write a short love letter to yourself" to "As you would to someone you love: what you appreciate, what you forgive, what you wish for them.",
            "Go over the day, note down anything you've felt bad about, or" to "name what caused the difficult experience, the emotions you felt, and how they felt in the body",
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

    /** "saturday 31 october · vent, mindset"; just the day without any sections, and the year when it isn't this one. */
    fun title(entry: JournalEntry, today: LocalDate): String {
        val date = LocalDate.ofEpochDay(entry.day)
        val day = date.format(DateTimeFormatter.ofPattern(if (date.year == today.year) "EEEE d MMMM" else "EEEE d MMMM yyyy", Locale.getDefault())).lowercase()
        return if (entry.aims.isEmpty()) day else "$day · " + entry.aims.joinToString(", ") { it.lowercase() }
    }

    /** [text] with [prompt] put in at [cursor] in bold, on a line of its own, and where to write next: the line under it. */
    fun inserted(text: String, cursor: Int, prompt: String): Pair<String, Int> {
        val at = cursor.coerceIn(0, text.length)
        val before = text.substring(0, at)
        val after = text.substring(at)
        val lead = if (before.isEmpty() || before.endsWith("\n")) "" else "\n"
        val block = "$lead**${prompt.trim()}**\n"
        val trail = if (after.isEmpty() || after.startsWith("\n")) "" else "\n"
        return before + block + trail + after to (before.length + block.length)
    }

    /** [entries] newest first. */
    fun entries(entries: List<JournalEntry>): List<JournalEntry> = entries.sortedByDescending { it.day }

    /** [entries] with [day]'s set to [text] and [aims]; an entry left empty goes. */
    fun saved(entries: List<JournalEntry>, day: Long, text: String, aims: List<String>, at: Long): List<JournalEntry> {
        val rest = entries.filterNot { it.day == day }
        return if (text.isBlank()) rest else rest + JournalEntry(day, text, aims, at)
    }

    /** [entries] with [text] added to the end of [day]'s, after a blank line (or starting it), and [aims] joined to its own. */
    fun added(entries: List<JournalEntry>, day: Long, text: String, aims: List<String>, at: Long): List<JournalEntry> {
        if (text.isBlank()) return entries
        val now = entries.firstOrNull { it.day == day }
        val joined = now?.text?.trimEnd()?.takeIf { it.isNotEmpty() }?.let { "$it\n\n${text.trim()}" } ?: text.trim()
        return saved(entries, day, joined, (now?.aims.orEmpty() + aims).distinct(), at)
    }
}
