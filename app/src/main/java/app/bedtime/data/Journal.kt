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

/**
 * A mood tapped, and when; with "why?", what it was asked about ([from]: "unlocking", or the app) and
 * whether you went on; [after] for "how do you feel now?" once a stretch on the phone is over.
 */
@Serializable
data class MoodMark(val at: Long, val mood: String, val from: String? = null, val wentOn: Boolean? = null, val after: Boolean = false)

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

    /** How each line of an entry starts: "- 14:32 · ", the time it was begun. */
    fun bullet(time: String): String = "- $time · "

    private val TIMED = Regex("""^- (\d{1,2}:\d{2}) ·(?: (.*))?$""")
    private val EMPTY = Regex("""^- \d{1,2}:\d{2} · ?$""")

    /** "- 14:32 · tired · slept badly": a line from [text] at [time], with the [moods] picked for it, on one line. */
    fun line(time: String, text: String, moods: List<String> = emptyList()): String =
        bullet(time) + listOf(moods.joinToString(", "), text.trim().replace(Regex("\\s*\\n\\s*"), " ")).filter { it.isNotEmpty() }.joinToString(" · ")

    /** [text] with [line] at its foot: the newest last, as you'd write them. */
    fun withLine(text: String, line: String): String = (text.trimEnd().takeIf { it.isNotEmpty() }?.plus("\n") ?: "") + line

    /** [entries] with [line] at the foot of [day]'s entry, an answer to "why?" or a quick note, and the moods tapped with it. */
    fun answered(
        entries: List<JournalEntry>,
        day: Long,
        line: String,
        moods: List<String>,
        at: Long,
        from: String? = null,
        wentOn: Boolean? = null,
        after: Boolean = false,
    ): List<JournalEntry> {
        val now = entries.firstOrNull { it.day == day } ?: JournalEntry(day, "")
        val updated = now.copy(text = withLine(now.text, line), editedAt = at, moods = now.moods + moods.map { MoodMark(at, it, from, wentOn, after) })
        return entries.filterNot { it.day == day } + updated
    }

    /** [text] opened to write in: a fresh line at its foot, begun at [time], and the cursor on it. */
    fun opened(text: String, time: String): Pair<String, Int> {
        val lines = text.trimEnd().lines()
        if (lines.isNotEmpty() && EMPTY.matches(lines.last())) return text.trimEnd() + " " to text.trimEnd().length + 1
        val out = withLine(text, bullet(time))
        return out to out.length
    }

    /** What's kept of [text]: without lines begun and left empty. */
    fun tidy(text: String): String = text.lines().filterNot { EMPTY.matches(it) }.joinToString("\n").trimEnd()

    /**
     * Writing in an entry, from [old] to [new] with the cursor at [cursor]: a new line after a timed one
     * starts timed at [time], and a new line on an empty one ends the run; the first words on an empty
     * line take the time they're written. Null when it's plain typing.
     */
    fun typed(old: String, new: String, cursor: Int, time: String): Pair<String, Int>? {
        val at = cursor - 1
        if (at >= 0 && new.length == old.length + 1 && new[at] == '\n' && new.removeRange(at, at + 1) == old) {
            val start = new.lastIndexOf('\n', at - 1) + 1
            val line = new.substring(start, at)
            if (EMPTY.matches(line)) return new.removeRange(start, at + 1) to start
            if (!TIMED.matches(line)) return null
            val begun = bullet(time)
            return new.replaceRange(at + 1, at + 1, begun) to at + 1 + begun.length
        }
        if (cursor < 0 || cursor > new.length || new.length <= old.length) return null
        val start = new.lastIndexOf('\n', cursor - 1) + 1
        val end = new.indexOf('\n', start).let { if (it < 0) new.length else it }
        val oldEnd = old.indexOf('\n', start).let { if (it < 0) old.length else it }
        if (start > old.length || !EMPTY.matches(old.substring(start, oldEnd))) return null
        val m = TIMED.matchEntire(new.substring(start, end)) ?: return null
        if (m.groupValues[2].isBlank() || m.groupValues[1] == time) return null
        return new.replaceRange(start + 2, start + 2 + m.groupValues[1].length, time) to cursor + time.length - m.groupValues[1].length
    }

    /** The moods named at the start of the timed line at [cursor] in [text], of [moods]. */
    fun moodsAt(text: String, cursor: Int, moods: List<String>): List<String> {
        val (start, end) = lineAt(text, cursor)
        val rest = TIMED.matchEntire(text.substring(start, end))?.groupValues?.get(2) ?: return emptyList()
        return moodPart(rest, moods)
    }

    private fun lineAt(text: String, cursor: Int): Pair<Int, Int> {
        val at = cursor.coerceIn(0, text.length)
        val start = text.lastIndexOf('\n', at - 1) + 1
        return start to text.indexOf('\n', at).let { if (it < 0) text.length else it }
    }

    private fun moodPart(rest: String, moods: List<String>): List<String> =
        rest.substringBefore(" · ").split(", ").map { it.trim() }.takeIf { part -> part.isNotEmpty() && part.all { it in moods } }.orEmpty()

    /**
     * [mood] put on the timed line at [cursor], or taken off it if it's there: the moods follow its time.
     * Off a timed line, a new one begins at [time] with it. The text, the cursor, and whether it went on.
     */
    fun withMood(text: String, cursor: Int, mood: String, moods: List<String>, time: String): Triple<String, Int, Boolean> {
        val (start, end) = lineAt(text, cursor)
        val m = TIMED.matchEntire(text.substring(start, end))
        if (m == null) {
            val out = withLine(text, bullet(time) + "$mood · ")
            return Triple(out, out.length, true)
        }
        val rest = m.groupValues[2]
        val named = moodPart(rest, moods + mood)
        val words = if (named.isEmpty()) rest else rest.substringAfter(" · ", "")
        val on = mood !in named
        val now = if (on) named + mood else named - mood
        val line = bullet(m.groupValues[1]) + listOf(now.joinToString(", "), words).filter { it.isNotEmpty() }.joinToString(" · ") + if (words.isEmpty() && now.isNotEmpty()) " · " else ""
        val out = text.replaceRange(start, end, line)
        return Triple(out, (cursor + line.length - (end - start)).coerceIn(start, start + line.length), on)
    }

    /** [prompt] in bold on the timed line at [cursor] if it's empty, or on a new one begun at [time]; the cursor after it. */
    fun prompted(text: String, cursor: Int, prompt: String, time: String): Pair<String, Int> {
        val (start, end) = lineAt(text, cursor)
        val asked = "**${prompt.trim().lowercase()}** "
        if (EMPTY.matches(text.substring(start, end))) {
            val line = text.substring(start, end).trimEnd() + " " + asked
            return text.replaceRange(start, end, line) to start + line.length
        }
        val line = bullet(time) + asked
        val out = text.substring(0, end) + "\n" + line + text.substring(end)
        return out to end + 1 + line.length
    }

    /** Today's lines at a glance: how many, and the time of the latest. */
    fun glance(entry: JournalEntry?): Pair<Int, String?> {
        val timed = entry?.text.orEmpty().lines().mapNotNull { TIMED.matchEntire(it)?.takeIf { m -> m.groupValues[2].isNotBlank() } }
        return timed.size to timed.maxOfOrNull { it.groupValues[1].padStart(5, '0') }
    }

    /** [entries] with [mood] tapped on [day] at [at]: each tap is kept, with its time. */
    fun moodAdded(entries: List<JournalEntry>, day: Long, mood: String, at: Long): List<JournalEntry> {
        val now = entries.firstOrNull { it.day == day } ?: JournalEntry(day, "")
        return entries.filterNot { it.day == day } + now.copy(moods = now.moods + MoodMark(at, mood))
    }

    /** [entries] without one tap, [mark], on [day]; an entry left with nothing goes. */
    fun moodRemoved(entries: List<JournalEntry>, day: Long, mark: MoodMark): List<JournalEntry> {
        val now = entries.firstOrNull { it.day == day } ?: return entries
        val updated = now.copy(moods = now.moods - mark)
        return entries.filterNot { it.day == day } + listOfNotNull(updated.takeIf { it.text.isNotBlank() || it.moods.isNotEmpty() })
    }

    /** [entries] with every [old] mood called [new], as when a mood is renamed. */
    fun moodRenamed(entries: List<JournalEntry>, old: String, new: String): List<JournalEntry> =
        entries.map { e -> if (e.moods.none { it.mood == old }) e else e.copy(moods = e.moods.map { if (it.mood == old) it.copy(mood = new) else it }) }

    /** [entries] newest first. */
    fun entries(entries: List<JournalEntry>): List<JournalEntry> = entries.sortedByDescending { it.day }

    /** [entries] with [day]'s set to [text] and [aims], its moods kept; an entry left with nothing goes. */
    fun saved(entries: List<JournalEntry>, day: Long, text: String, aims: List<String>, at: Long): List<JournalEntry> {
        val moods = entries.firstOrNull { it.day == day }?.moods.orEmpty()
        val rest = entries.filterNot { it.day == day }
        return if (text.isBlank() && moods.isEmpty()) rest else rest + JournalEntry(day, text, aims, at, moods)
    }
}
