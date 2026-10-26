package app.bedtime.ui.notes

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownEditingTest {
    /** [before] with Enter typed where "|" is. */
    private fun enter(before: String): TextFieldValue {
        val at = before.indexOf('|')
        val old = TextFieldValue(before.replace("|", ""), TextRange(at))
        val new = TextFieldValue(old.text.replaceRange(at, at, "\n"), TextRange(at + 1))
        return Markdown.continueList(old, new)
    }

    private fun TextFieldValue.shown() = text.replaceRange(selection.start, selection.start, "|")

    @Test
    fun enterCarriesAListOn() {
        assertEquals("- milk\n- |", enter("- milk|").shown())
        assertEquals("- [x] milk\n- [ ] |", enter("- [x] milk|").shown())
        assertEquals("1. one\n2. |", enter("1. one|").shown())
        assertEquals("  * sub\n  * |", enter("  * sub|").shown())
        // Splitting an item takes the rest down as a new item.
        assertEquals("- mi\n- |lk", enter("- mi|lk").shown())
        // Plain lines stay plain.
        assertEquals("milk\n|", enter("milk|").shown())
    }

    @Test
    fun enterOnAnEmptyItemEndsTheList() {
        assertEquals("- milk\n|", enter("- milk\n- |").shown())
        assertEquals("- milk\n|", enter("- milk\n- [ ] |").shown())
    }

    @Test
    fun indentAndHeadings() {
        val line = TextFieldValue("a\n- milk", TextRange(5))
        val inward = Markdown.indent(line, inward = true)
        assertEquals("a\n  - milk", inward.text)
        assertEquals(7, inward.selection.start)
        assertEquals("a\n- milk", Markdown.indent(inward, inward = false).text)
        assertEquals("a\n- milk", Markdown.indent(line, inward = false).text)
        var title = TextFieldValue("Plan", TextRange(4))
        val seen = (1..4).map { title = Markdown.cycleHeading(title); title.text }
        assertEquals(listOf("# Plan", "## Plan", "### Plan", "Plan"), seen)
    }
}
