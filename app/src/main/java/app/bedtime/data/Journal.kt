package app.bedtime.data

import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

/** One kind of journalling, with a line on how to go about it and the questions it asks. Yours to change. */
@Serializable
data class JournalSection(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val guide: String = "",
    val prompts: List<String> = emptyList(),
)

/** A day's microjournal entry: what was written, and the sections it drew on, by name, as they were then. */
@Serializable
data class JournalEntry(val day: Long, val text: String, val aims: List<String> = emptyList(), val editedAt: Long = 0L)

/**
 * The microjournal: an entry a day, and sections of prompts to write from. The suggested sections
 * follow the journalling system by Struthless (vent, obligations, mindset, ideation, trajectory), with
 * gratitude as one of its own; each is only a start, to rename, rewrite, delete or add to.
 */
object Journal {
    val SUGGESTED: List<JournalSection> = listOf(
        JournalSection(
            "vent", "Vent",
            "Write what's bothering you, unedited, without holding back. A few sentences is enough: your head is for solving problems, not storing them.",
            listOf("What's annoying me right now?", "What am I carrying that I'd rather put down?", "What would I write if no one could ever read it?"),
        ),
        JournalSection(
            "obligations", "Obligations",
            "Get every task out of your head, then sort it.",
            listOf(
                "Everything on my mind to do, big or small, in any order",
                "Sorted into a few areas: health, work, home, money…",
                "Which one, if done, would make the rest easier?",
                "Today's bare minimum, and the extra if it goes well",
            ),
        ),
        JournalSection(
            "mindset", "Mindset",
            "Specific questions train the mind better than vague intentions.",
            listOf(
                "How could this turn out to be good for me?",
                "I am becoming ___ because…",
                "How could I make sure I miss my goal?",
                "What would a friend who wants me to succeed tell me?",
                "What would change my mind about something I believe?",
                "Out of 10, how do I feel about it, and why not one lower?",
            ),
        ),
        JournalSection(
            "gratitude", "Gratitude",
            "Noticing what went right, however small.",
            listOf(
                "Three things I'm grateful for: one ordinary, one by chance, one I made happen",
                "Who made today easier, and have I told them?",
                "What small thing went right today?",
            ),
        ),
        JournalSection(
            "ideation", "Ideation",
            "Quantity first: good ideas come out of many okay ones.",
            listOf(
                "As many answers as I can in five minutes to…",
                "How would someone else solve this: a child, an inventor, a company?",
                "A question to sleep on, and my answer in the morning",
            ),
        ),
        JournalSection(
            "trajectory", "Trajectory",
            "Small days add up; check which way they point.",
            listOf("My goal, and did today move me towards it or away?", "What gave me energy today?", "What drained me today?", "What did I learn today?"),
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

    fun editedPrompt(sections: List<JournalSection>, id: String, index: Int, prompt: String) =
        sections.changed(id) { s -> s.copy(prompts = s.prompts.mapIndexed { i, p -> if (i == index) prompt else p }) }

    fun withoutPrompt(sections: List<JournalSection>, id: String, index: Int) =
        sections.changed(id) { s -> s.copy(prompts = s.prompts.filterIndexed { i, _ -> i != index }) }

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
