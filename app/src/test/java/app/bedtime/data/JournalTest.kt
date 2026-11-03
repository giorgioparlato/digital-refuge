package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class JournalTest {
    private val today = LocalDate.of(2026, 10, 31)

    @Test
    fun theSuggestionsUntilYouChangeThem() {
        assertEquals(
            listOf("clarity", "pattern interrupt", "daily reflections", "when bad things happen", "anxieties", "to-do list and direction", "making a decision", "direction in life", "self compassion"),
            Journal.sections(null).map { it.name },
        )
        // A question's example is there only where one was written.
        val bad = Journal.SUGGESTED.first { it.id == "bad" }
        assertEquals(bad.prompts.toSet(), bad.hints.keys)
        assertEquals(emptyMap<String, String>(), Journal.SUGGESTED.first { it.id == "reflection" }.hints)
        assertEquals(emptyList<String>(), Journal.SUGGESTED.first { it.id == "clarity" }.prompts)
        assertEquals(8, Journal.sections(Journal.withoutSection(Journal.SUGGESTED, "clarity")).size)
    }

    @Test
    fun sectionsAndPromptsAreYoursToChange() {
        var s = Journal.SUGGESTED
        val first = s.first { it.id == "bad" }.prompts.first()
        s = Journal.renamed(s, "bad", "Hard days")
        s = Journal.withGuide(s, "bad", "One at a time.")
        s = Journal.withPrompt(s, "bad", "What's loud in my head?")
        s = Journal.editedPrompt(s, "bad", 0, "What happened?")
        s = Journal.withoutPrompt(s, "bad", 1)
        val hard = s.first { it.id == "bad" }
        assertEquals("Hard days", hard.name)
        assertEquals("One at a time.", hard.guide)
        assertEquals(listOf("What happened?", "how would i comfort a friend i loved if this happened to them?", "how is this the best thing that has ever happened to me?", "What's loud in my head?"), hard.prompts)
        // A reworded question keeps its example; a deleted one takes its own with it.
        assertEquals(Journal.SUGGESTED.first { it.id == "bad" }.hints[first], hard.hints["What happened?"])
        assertEquals(null, hard.hints["what did i make it mean?"])
        s = Journal.withHint(s, "bad", "What's loud in my head?", "  the meeting tomorrow ")
        assertEquals("the meeting tomorrow", s.first { it.id == "bad" }.hints["What's loud in my head?"])
        s = Journal.withHint(s, "bad", "What's loud in my head?", "")
        assertEquals(null, s.first { it.id == "bad" }.hints["What's loud in my head?"])
        s = Journal.withSection(s, "Dreams")
        assertEquals("Dreams", s.last().name)
        // Other sections are left as they were.
        assertEquals(Journal.SUGGESTED.first { it.id == "decision" }, s.first { it.id == "decision" })
    }

    @Test
    fun aTitleIsTheDayAndTheSectionsWritten() {
        assertEquals("saturday 31 october", Journal.title(JournalEntry(today.toEpochDay(), "x"), today))
        assertEquals("saturday 31 october · vent, mindset", Journal.title(JournalEntry(today.toEpochDay(), "x", listOf("Vent", "Mindset")), today))
        // A section since deleted keeps its name in the entries written with it.
        assertEquals("wednesday 31 december 2025 · dreams", Journal.title(JournalEntry(LocalDate.of(2025, 12, 31).toEpochDay(), "x", listOf("Dreams")), today))
    }

    @Test
    fun aPromptGoesOnTheEmptyLineOrANewOne() {
        assertEquals("- 14:32 · **what drained me?** " to 31, Journal.prompted("- 14:32 · ", 10, "What drained me?", "14:40"))
        val (text, cursor) = Journal.prompted("- 14:32 · long day\n- 15:00 · tea", 5, "Q", "15:10")
        assertEquals("- 14:32 · long day\n- 15:10 · **q** \n- 15:00 · tea", text)
        assertEquals(35, cursor)
    }

    @Test
    fun aLineAtATimeEachWithItsTime() {
        // Opened, a fresh line at the foot; left empty, it isn't kept.
        assertEquals("- 14:32 · " to 10, Journal.opened("", "14:32"))
        assertEquals("- 09:00 · coffee\n- 14:32 · " to 27, Journal.opened("- 09:00 · coffee\n", "14:32"))
        assertEquals("- 09:00 · coffee", Journal.tidy("- 09:00 · coffee\n- 14:32 · "))
        // A new line after a timed one is timed now; a new line on an empty one ends them.
        assertEquals("- 09:00 · coffee\n- 09:05 · " to 27, Journal.typed("- 09:00 · coffee", "- 09:00 · coffee\n", 17, "09:05"))
        assertEquals("- 09:00 · coffee\n" to 17, Journal.typed("- 09:00 · coffee\n- 09:05 · ", "- 09:00 · coffee\n- 09:05 · \n", 28, "09:06"))
        // The first words on a line take the time they're written; after that, it's plain typing.
        assertEquals("- 09:20 · t" to 11, Journal.typed("- 09:05 · ", "- 09:05 · t", 11, "09:20"))
        assertNull(Journal.typed("- 09:20 · t", "- 09:20 · te", 12, "09:21"))
        assertNull(Journal.typed("plain", "plain\n", 6, "09:21"))
    }

    @Test
    fun aMoodGoesOnTheLineYoureOn() {
        val moods = listOf("tired", "bored")
        val (once, at, on) = Journal.withMood("- 09:00 · slept badly", 21, "tired", moods, "09:10")
        assertEquals("- 09:00 · tired · slept badly", once)
        assertEquals(29, at)
        assertEquals(true, on)
        assertEquals("- 09:00 · tired, bored · slept badly", Journal.withMood(once, at, "bored", moods, "09:10").first)
        assertEquals(listOf("tired"), Journal.moodsAt(once, 3, moods))
        // Tapped again, it comes off.
        val (off, _, wasOn) = Journal.withMood(once, at, "tired", moods, "09:10")
        assertEquals("- 09:00 · slept badly" to false, off to wasOn)
        // On an empty line it waits for the words; off a timed line, it begins one.
        assertEquals("- 09:00 · tired · ", Journal.withMood("- 09:00 · ", 10, "tired", moods, "09:10").first)
        assertEquals("notes\n- 09:10 · tired · ", Journal.withMood("notes", 2, "tired", moods, "09:10").first)
    }

    @Test
    fun todayAtAGlance() {
        assertEquals(0 to null, Journal.glance(null))
        assertEquals(2 to "14:32", Journal.glance(JournalEntry(1, "- 09:00 · coffee\nmore\n- 14:32 · walk\n- 15:00 · ")))
        assertEquals("- 21:40 · tired · slept badly woke early", Journal.line("21:40", "slept badly\nwoke early", listOf("tired")))
    }

    @Test
    fun entriesAreKeptADayEach() {
        val day = today.toEpochDay()
        var all = Journal.saved(emptyList(), day, "first", listOf("Clarity"), 1)
        all = Journal.saved(all, day - 1, "yesterday", emptyList(), 3)
        assertEquals(listOf(day, day - 1), Journal.entries(all).map { it.day })
        // Emptied, an entry goes.
        assertNull(Journal.saved(all, day, " ", emptyList(), 4).firstOrNull { it.day == day })
    }

    @Test
    fun answersToWhyGoAtTheFootNewestLast() {
        val day = today.toEpochDay()
        var all = Journal.answered(emptyList(), day, "- 09:00 · unlocking · went on", listOf("bored"), 1)
        assertEquals("- 09:00 · unlocking · went on", all.single().text)
        all = Journal.saved(all, day, all.single().text + "\n- 09:30 · slept badly", listOf("Clarity"), 2)
        all = Journal.answered(all, day, "- 10:00 · instagram · put it down", listOf("tired"), 3)
        assertEquals("- 09:00 · unlocking · went on\n- 09:30 · slept badly\n- 10:00 · instagram · put it down", all.single().text)
        // What was written stays, and the moods with it.
        assertEquals(listOf("bored", "tired"), all.single().moods.map { it.mood })
        assertEquals(listOf("Clarity"), all.single().aims)
        assertEquals("saturday 31 october · clarity · bored, tired", Journal.title(all.single(), today))
    }

    @Test
    fun eachMoodTapIsKeptWithItsTime() {
        val day = today.toEpochDay()
        var all = Journal.moodAdded(emptyList(), day, "happy", 5)
        all = Journal.moodAdded(all, day, "happy", 9)
        assertEquals(listOf(MoodMark(5, "happy"), MoodMark(9, "happy")), all.single().moods)
        // Saving the text keeps them; one taken back leaves the other.
        all = Journal.saved(all, day, "", emptyList(), 6)
        all = Journal.moodRemoved(all, day, MoodMark(5, "happy"))
        assertEquals(listOf(MoodMark(9, "happy")), all.single().moods)
        // With nothing written and no moods left, the entry goes.
        assertEquals(emptyList<JournalEntry>(), Journal.moodRemoved(all, day, MoodMark(9, "happy")))
        // A mood renamed is renamed on the days it was tapped.
        assertEquals("glad", Journal.moodRenamed(all, "happy", "glad").single().moods.single().mood)
    }
}
