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

    @Test
    fun onEachChangeOnlyWhatChangedIsWrittenAndARenamedNoteLeavesNoOldFile() {
        val films = Note("a", "# films", at)
        val ideas = Note("b", "# ideas", at + 1)
        val before = NotesExport.Written.of(listOf(films, ideas), emptyList())
        // Nothing written yet: all of them.
        assertEquals(2, NotesExport.changes(NotesExport.named(listOf(films, ideas)), null).first.size)
        // One changed in text: just that one.
        val more = ideas.copy(text = "# ideas\nmore")
        assertEquals(listOf("ideas.md" to more) to emptyList<String>(), NotesExport.changes(NotesExport.named(listOf(films, more)), before))
        // One retitled: written under its new name, the old file gone.
        val retitled = films.copy(text = "# films to see")
        assertEquals(listOf("films to see.md" to retitled) to listOf("films.md"), NotesExport.changes(NotesExport.named(listOf(retitled, ideas)), before))
        // One deleted in refuge: its file goes too.
        assertEquals(emptyList<Pair<String, Note>>() to listOf("films.md"), NotesExport.changes(NotesExport.named(listOf(ideas)), before))
    }
}
