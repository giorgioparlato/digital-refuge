package app.bedtime.data

import app.bedtime.ui.notes.Markdown
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class NotesTest {
    @Test
    fun aQuickNoteIsOneMarkdownListItem() {
        assertEquals("- 2026-10-25 22:41 watch Perfect Days", Notes.line("  watch Perfect Days \n", LocalDateTime.of(2026, 10, 25, 22, 41)))
        assertEquals("- 2026-10-25 22:41 one two", Notes.line("one\n  two", LocalDateTime.of(2026, 10, 25, 22, 41)))
    }

    @Test
    fun appendingAlwaysStartsOnItsOwnLine() {
        assertEquals("- a\n", Notes.appended("", "- a"))
        assertEquals("# Inbox\n- a\n", Notes.appended("# Inbox\n", "- a"))
        assertEquals("# Inbox\n- a\n", Notes.appended("# Inbox", "- a"))
    }

    @Test
    fun markdownReadsAsText() {
        val shown = Markdown.render("# Films\n- [ ] Perfect Days\n- [x] **Paterson**\n* a `note` with [a link](https://x)\n> a quote").text
        assertEquals("Films\n☐ Perfect Days\n☑ Paterson\n• a note with a link\na quote", shown)
    }

    @Test
    fun aBackupWithoutNotesStillLoads() {
        val backup = Backup.decode("""{"version":1,"exportedAt":0}""")!!
        assertEquals(emptyList<Note>(), backup.notes)
        val settings = AppJson.decodeFromString(AppSettings.serializer(), """{"onboarded":true}""")
        assertEquals(emptyList<NoteTarget>(), settings.noteTargets)
        assertEquals(null, settings.eventCalendarId)
    }
}
