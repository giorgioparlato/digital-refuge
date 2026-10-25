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

    @Test
    fun eachFileChoosesItsFormat() {
        val at = LocalDateTime.of(2026, 10, 25, 22, 41)
        assertEquals("- [ ] buy bread", Notes.line("buy bread", at, NoteStyle.TASK, withDate = false))
        assertEquals("- [ ] 2026-10-25 22:41 buy bread", Notes.line("buy bread", at, NoteStyle.TASK))
        assertEquals("buy bread", Notes.line("buy bread", at, NoteStyle.PLAIN, withDate = false))
        assertEquals("- buy bread", Notes.line("buy bread", at, NoteStyle.BULLET, withDate = false))
        // Files added before keep the dated list item.
        val old = AppJson.decodeFromString(NoteTarget.serializer(), """{"name":"inbox","uri":"x"}""")
        assertEquals(NoteStyle.BULLET, old.style)
        assertEquals(true, old.withDate)
    }

    @Test
    fun liveMarkdownKeepsEveryCharacter() {
        val text = "# Films\n- [ ] **Perfect** *Days* `x` [a](b)"
        assertEquals(text, Markdown.styled(text, androidx.compose.ui.graphics.Color.Gray, androidx.compose.ui.graphics.Color.Green).text)
    }

    @Test
    fun theToolbarsMarks() {
        fun v(text: String, from: Int, to: Int = from) = androidx.compose.ui.text.input.TextFieldValue(text, androidx.compose.ui.text.TextRange(from, to))
        // Bold around a selection, or a pair with the cursor between.
        Markdown.wrap(v("so good", 3, 7), "**").let {
            assertEquals("so **good**", it.text)
            assertEquals(11, it.selection.start)
        }
        Markdown.wrap(v("ab", 1), "*").let {
            assertEquals("a**b", it.text)
            assertEquals(2, it.selection.start)
        }
        assertEquals("a[b", Markdown.insert(v("ab", 1), "[").text)
        // A heading or to-do starts the cursor's line, and comes off again.
        assertEquals("one\n# two", Markdown.toggleLine(v("one\ntwo", 6), "# ").text)
        assertEquals("one\ntwo", Markdown.toggleLine(v("one\n# two", 8), "# ").text)
        assertEquals("- [ ] milk", Markdown.toggleLine(v("milk", 2), "- [ ] ").text)
    }

    @Test
    fun todaysNoteNamedAsObsidianWould() {
        val day = java.time.LocalDate.of(2026, 10, 5)
        val english = java.util.Locale.getDefault()
        java.util.Locale.setDefault(java.util.Locale.UK)
        try {
            assertEquals("2026-10-05.md", Notes.dailyPath("YYYY-MM-DD", day))
            assertEquals("2026/10/2026-10-05.md", Notes.dailyPath("YYYY/MM/YYYY-MM-DD", day))
            assertEquals("Journal/5 Oct 26.md", Notes.dailyPath("[Journal]/D MMM YY", day))
            assertEquals("Monday 05 October 2026.md", Notes.dailyPath("dddd DD MMMM YYYY", day))
            // Left empty, Obsidian's own default.
            assertEquals("2026-10-05.md", Notes.dailyPath("", day))
        } finally {
            java.util.Locale.setDefault(english)
        }
        val daily = AppJson.decodeFromString(NoteTarget.serializer(), """{"name":"inbox","uri":"x"}""")
        assertEquals(false, daily.daily)
        assertEquals("YYYY-MM-DD", daily.dailyFormat)
    }
}
