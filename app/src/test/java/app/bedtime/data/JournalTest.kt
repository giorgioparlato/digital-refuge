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
            listOf("Clarity", "Pattern interrupt", "Daily reflections", "When bad things happen", "Anxieties", "To-do list and direction", "Making a decision", "Direction in life", "Self compassion"),
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
        assertEquals(listOf("What happened?", "How would I comfort a friend I loved if this happened to them?", "How is this the best thing that has ever happened to me?", "What's loud in my head?"), hard.prompts)
        // A reworded question keeps its example; a deleted one takes its own with it.
        assertEquals(Journal.SUGGESTED.first { it.id == "bad" }.hints[first], hard.hints["What happened?"])
        assertEquals(null, hard.hints["What did I make it mean?"])
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
    fun aPromptGoesInAtTheCursorOnItsOwnLine() {
        assertEquals("**What drained me?**\n" to 21, Journal.inserted("", 0, "What drained me?"))
        val (text, cursor) = Journal.inserted("Long day", 8, "What drained me?")
        assertEquals("Long day\n**What drained me?**\n", text)
        assertEquals(text.length, cursor)
        // Between two lines, it takes a line of its own, with an empty one under it for the answer, the cursor there.
        assertEquals("a\n**Q**\n\nb" to 8, Journal.inserted("a\nb", 2, "Q"))
        assertEquals("a\n**Q**\n\nb" to 8, Journal.inserted("a\n\nb", 2, "Q"))
    }

    @Test
    fun entriesAreKeptADayEach() {
        val day = today.toEpochDay()
        var all = Journal.saved(emptyList(), day, "first", listOf("Vent"), 1)
        all = Journal.added(all, day, "  more  ", listOf("Gratitude", "Vent"), 2)
        assertEquals("first\n\nmore", all.single().text)
        assertEquals(listOf("Vent", "Gratitude"), all.single().aims)
        all = Journal.added(all, day - 1, "yesterday", emptyList(), 3)
        assertEquals(listOf(day, day - 1), Journal.entries(all).map { it.day })
        // Emptied, an entry goes; nothing added, nothing changes.
        assertNull(Journal.saved(all, day, " ", emptyList(), 4).firstOrNull { it.day == day })
        assertEquals(all, Journal.added(all, day, "", emptyList(), 5))
    }
}
