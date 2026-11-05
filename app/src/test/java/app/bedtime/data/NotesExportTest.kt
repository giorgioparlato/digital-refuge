package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class NotesExportTest {
    private val day = LocalDate.of(2026, 11, 4)
    private val at = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    @Test
    fun aFileANoteByItsTitle() {
        assertEquals("films to see.md", NotesExport.fileName(Note("a", "# Films to see\n- [ ] Perfect Days", at)))
        // What a file name can't hold goes; marks of Markdown too.
        assertEquals("plans for monday.md", NotesExport.fileName(Note("b", "**Plans: for / monday?**", at)))
        assertEquals("note 2026-11-04.md", NotesExport.fileName(Note("c", "   ", at)))
    }

    @Test
    fun aSecondNoteOfTheSameNameIsNumberedTheOldestKeepingIt() {
        val older = Note("a", "# Ideas", at)
        val newer = Note("b", "# Ideas\nmore", at + 1)
        assertEquals(listOf("ideas.md" to older, "ideas 2.md" to newer), NotesExport.named(listOf(newer, older)))
    }

    @Test
    fun recordingsAsObsidianEmbedsAndTheMicrojournalADayAFile() {
        assertEquals("- 14:32 · 🎙 0:42 · hi · ![[2026-11-04 1432.m4a]]", NotesExport.withEmbeds("- 14:32 · 🎙 0:42 · hi · 2026-11-04 1432.m4a"))
        // Already an embed: left as it is.
        assertEquals("![[2026-11-04 1432.m4a]]", NotesExport.withEmbeds("![[2026-11-04 1432.m4a]]"))
        val (name, text) = NotesExport.journalFile(JournalEntry(day.toEpochDay(), "- 09:00 · coffee\n"), day)
        assertEquals("2026-11-04.md", name)
        assertEquals("# wednesday 4 november\n\n- 09:00 · coffee\n", text)
    }
}
