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
        // Indented items keep their indent, a wide space for every two; numbers stay.
        assertEquals("☐ a\n\u2003☐ b\n\u2003\u2003• c\n1. d", Markdown.render("- [ ] a\n  - [ ] b\n    - c\n1. d").text)
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
        val text = "# Films\n**Perfect** *Days* `x` [a](b)"
        assertEquals(text, Markdown.styled(text, androidx.compose.ui.graphics.Color.Gray, androidx.compose.ui.graphics.Color.Green).text)
        // Boxes, bullets and indents are drawn as such, one character for one, so the cursor still maps.
        val list = "- [ ] milk\n  - [x] eggs\n- bread"
        val drawn = Markdown.drawn(list)
        assertEquals(list.length, drawn.length)
        assertEquals("milk\neggs\nbread", drawn.filter { it.isLetter() || it == '\n' })
        assertEquals(listOf('☐', '☑', '•'), drawn.filter { it in "☐☑•" }.toList())
    }

    @Test
    fun aTapOnABoxTicksIt() {
        fun v(text: String, at: Int) = androidx.compose.ui.text.input.TextFieldValue(text, androidx.compose.ui.text.TextRange(at))
        val text = "- [ ] milk\n  - [x] eggs"
        // The cursor moving onto the box ticks it and goes to the line's end.
        assertEquals(v("- [x] milk\n  - [x] eggs", 10), Markdown.tickOnTap(v(text, 20), v(text, 2)))
        assertEquals(v("- [ ] milk\n  - [ ] eggs", 23), Markdown.tickOnTap(v(text, 0), v(text, 14)))
        // Onto the words, or typing, is just that.
        assertEquals(null, Markdown.tickOnTap(v(text, 0), v(text, 8)))
        assertEquals(null, Markdown.tickOnTap(v(text, 0), v(text + "x", 24)))
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

    @Test
    fun theNoteWidgetReadsTicksAndAdds() {
        val text = "# Groceries\n- [ ] oat milk\n  - [x] bread\n\n- lemons\nplain words"
        assertEquals("Groceries", NoteWidget.title(text))
        val lines = NoteWidget.lines(text, 3)
        assertEquals(listOf("oat milk", "bread", "lemons"), lines.map { it.text })
        assertEquals(listOf(false, true, null), lines.map { it.box })
        assertEquals(1, lines[1].indent)
        assertEquals(true, lines[2].bullet)
        // Ticking the first box, then unticking the second.
        assertEquals("# Groceries\n- [x] oat milk\n  - [x] bread\n\n- lemons\nplain words", NoteWidget.toggled(text, 1))
        assertEquals("# Groceries\n- [ ] oat milk\n  - [ ] bread\n\n- lemons\nplain words", NoteWidget.toggled(text, 2))
        // A line added as the note already writes its lines.
        assertEquals("# Groceries\n- [ ] eggs", NoteWidget.withLine("# Groceries\n- [x] bread", "eggs").replace("- [x] bread\n", ""))
        assertEquals("Ideas\n- tea\n- coffee", NoteWidget.withLine("Ideas\n- tea", "coffee"))
        assertEquals("Thoughts\nsomething", NoteWidget.withLine("Thoughts", "something"))
    }
}
