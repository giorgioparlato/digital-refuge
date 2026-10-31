package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class JournalTest {
    private val today = LocalDate.of(2026, 10, 31)

    @Test
    fun theSuggestionsUntilYouChangeThem() {
        assertEquals(listOf("Vent", "Obligations", "Mindset", "Gratitude", "Ideation", "Trajectory"), Journal.sections(null).map { it.name })
        val yours = Journal.withoutSection(Journal.SUGGESTED, "vent")
        assertEquals(5, Journal.sections(yours).size)
        // Gratitude is a section of its own, and its prompt isn't under mindset any more.
        assertEquals(true, Journal.SUGGESTED.first { it.id == "mindset" }.prompts.none { "grateful" in it })
    }

    @Test
    fun sectionsAndPromptsAreYoursToChange() {
        var s = Journal.SUGGESTED
        s = Journal.renamed(s, "vent", "Let it out")
        s = Journal.withGuide(s, "vent", "No editing.")
        s = Journal.withPrompt(s, "vent", "What's loud in my head?")
        s = Journal.editedPrompt(s, "vent", 0, "What's annoying me?")
        s = Journal.withoutPrompt(s, "vent", 1)
        val vent = s.first { it.id == "vent" }
        assertEquals("Let it out", vent.name)
        assertEquals("No editing.", vent.guide)
        assertEquals(listOf("What's annoying me?", "What would I write if no one could ever read it?", "What's loud in my head?"), vent.prompts)
        s = Journal.withSection(s, "Dreams")
        assertEquals("Dreams", s.last().name)
        assertEquals(emptyList<String>(), s.last().prompts)
        // Other sections are left as they were.
        assertEquals(Journal.SUGGESTED.first { it.id == "mindset" }, s.first { it.id == "mindset" })
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
