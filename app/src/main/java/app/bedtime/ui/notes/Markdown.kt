package app.bedtime.ui.notes

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.sp

/**
 * Enough Markdown to read a note comfortably: headings, lists and checkboxes, quotes, **bold**,
 * *italic*, `code` and [links](…) (shown as their words). The note itself stays plain Markdown.
 */
object Markdown {
    private val inline = Regex("""\*\*(.+?)\*\*|__(.+?)__|\*(.+?)\*|_(.+?)_|`(.+?)`|\[(.+?)]\((.+?)\)""")

    fun render(text: String, faint: Color = Color.Unspecified): AnnotatedString = buildAnnotatedString {
        text.lines().forEachIndexed { i, raw ->
            if (i > 0) append("\n")
            val heading = Regex("""^(#{1,6})\s+(.*)$""").find(raw)
            when {
                heading != null -> {
                    val size = when (heading.groupValues[1].length) { 1 -> 22.sp; 2 -> 19.sp; else -> 17.sp }
                    pushStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = size))
                    inline(heading.groupValues[2])
                    pop()
                }
                raw.matches(Regex("""^\s*[-*]\s+\[[ ]]\s+.*""")) -> { append("☐ "); inline(raw.substringAfter("] ")) }
                raw.matches(Regex("""^\s*[-*]\s+\[[xX]]\s+.*""")) -> {
                    append("☑ ")
                    pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = faint))
                    inline(raw.substringAfter("] "))
                    pop()
                }
                raw.matches(Regex("""^\s*[-*+]\s+.*""")) -> {
                    append(raw.takeWhile { it == ' ' } + "• ")
                    inline(raw.trimStart().drop(1).trimStart())
                }
                raw.startsWith(">") -> {
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic, color = faint))
                    inline(raw.removePrefix(">").trimStart())
                    pop()
                }
                else -> inline(raw)
            }
        }
    }

    private fun AnnotatedString.Builder.inline(line: String) {
        var at = 0
        inline.findAll(line).forEach { m ->
            append(line.substring(at, m.range.first))
            val g = m.groupValues
            when {
                g[1].isNotEmpty() || g[2].isNotEmpty() -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { append(g[1] + g[2]) }
                g[3].isNotEmpty() || g[4].isNotEmpty() -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(g[3] + g[4]) }
                g[5].isNotEmpty() -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(g[5]) }
                else -> withStyle(SpanStyle(textDecoration = TextDecoration.Underline)) { append(g[6]) }
            }
            at = m.range.last + 1
        }
        append(line.substring(at))
    }

    /**
     * The note exactly as typed, styled as you write: headings large, **bold** bold, *italic* slanted,
     * to-dos and quotes set off. The Markdown marks stay, faint, so what you see is what's saved.
     */
    fun styled(text: String, faint: Color, accent: Color): AnnotatedString = buildAnnotatedString {
        append(text)
        val mark = SpanStyle(color = faint)
        var start = 0
        text.split("\n").forEach { line ->
            val end = start + line.length
            Regex("""^(#{1,6})\s""").find(line)?.let { m ->
                val size = when (m.groupValues[1].length) { 1 -> 22.sp; 2 -> 19.sp; else -> 17.sp }
                addStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = size), start, end)
                addStyle(mark, start, start + m.groupValues[1].length)
            }
            Regex("""^\s*[-*]\s+\[([ xX])]\s""").find(line)?.let { m ->
                addStyle(SpanStyle(color = accent), start + m.range.first, start + m.range.last)
                if (m.groupValues[1].isNotBlank()) addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = faint), start + m.range.last, end)
            } ?: Regex("""^\s*([-*+])\s""").find(line)?.let { m -> addStyle(SpanStyle(color = accent), start + m.groups[1]!!.range.first, start + m.groups[1]!!.range.last + 1) }
            if (line.startsWith(">")) addStyle(SpanStyle(fontStyle = FontStyle.Italic, color = faint), start, end)
            inline.findAll(line).forEach { m ->
                val g = m.groupValues
                val from = start + m.range.first
                val to = start + m.range.last + 1
                when {
                    g[1].isNotEmpty() || g[2].isNotEmpty() -> {
                        addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), from, to)
                        addStyle(mark, from, from + 2)
                        addStyle(mark, to - 2, to)
                    }
                    g[3].isNotEmpty() || g[4].isNotEmpty() -> {
                        addStyle(SpanStyle(fontStyle = FontStyle.Italic), from, to)
                        addStyle(mark, from, from + 1)
                        addStyle(mark, to - 1, to)
                    }
                    g[5].isNotEmpty() -> {
                        addStyle(SpanStyle(fontFamily = FontFamily.Monospace), from, to)
                        addStyle(mark, from, from + 1)
                        addStyle(mark, to - 1, to)
                    }
                    else -> {
                        addStyle(SpanStyle(textDecoration = TextDecoration.Underline), from + 1, from + 1 + g[6].length)
                        addStyle(mark, from, from + 1)
                        addStyle(mark, from + 1 + g[6].length, to)
                    }
                }
            }
            start = end + 1
        }
    }

    /** Wraps the selection in [marker] ("**" for bold), or puts the cursor between a pair of them. */
    fun wrap(value: TextFieldValue, marker: String): TextFieldValue {
        val sel = value.selection
        val chosen = value.text.substring(sel.min, sel.max)
        val text = value.text.replaceRange(sel.min, sel.max, marker + chosen + marker)
        val cursor = if (sel.collapsed) sel.min + marker.length else sel.min + marker.length + chosen.length + marker.length
        return TextFieldValue(text, TextRange(cursor))
    }

    /** Types [insert] in place of the selection. */
    fun insert(value: TextFieldValue, insert: String): TextFieldValue {
        val sel = value.selection
        return TextFieldValue(value.text.replaceRange(sel.min, sel.max, insert), TextRange(sel.min + insert.length))
    }

    /** Starts the cursor's line with [prefix] ("# ", "- ", "- [ ] "), or takes it away if it's there. */
    fun toggleLine(value: TextFieldValue, prefix: String): TextFieldValue {
        val at = value.selection.min
        val lineStart = value.text.lastIndexOf('\n', at - 1) + 1
        val line = value.text.substring(lineStart)
        return if (line.startsWith(prefix)) {
            TextFieldValue(value.text.removeRange(lineStart, lineStart + prefix.length), TextRange((at - prefix.length).coerceAtLeast(lineStart)))
        } else {
            TextFieldValue(value.text.replaceRange(lineStart, lineStart, prefix), TextRange(at + prefix.length))
        }
    }

    private val listItem = Regex("""^(\s*)(- \[[ xX]] |[-*+] |(\d+)([.)]) )(.*)$""")

    /**
     * [new] as typed after [old], except that Enter at the end of a list item carries the list on: the
     * same mark, the next number, a fresh box for a to-do. Enter on an item with nothing in it ends
     * the list instead, taking its mark away.
     */
    fun continueList(old: TextFieldValue, new: TextFieldValue): TextFieldValue {
        val at = new.selection.start - 1
        if (!new.selection.collapsed || at < 0 || new.text.length != old.text.length + 1 || new.text[at] != '\n') return new
        if (new.text.removeRange(at, at + 1) != old.text) return new
        val lineStart = new.text.lastIndexOf('\n', at - 1) + 1
        val m = listItem.matchEntire(new.text.substring(lineStart, at)) ?: return new
        if (m.groupValues[5].isBlank()) return TextFieldValue(new.text.removeRange(lineStart, at + 1), TextRange(lineStart))
        val mark = when {
            m.groupValues[3].isNotEmpty() -> "${m.groupValues[3].toInt() + 1}${m.groupValues[4]} "
            m.groupValues[2].startsWith("- [") -> "- [ ] "
            else -> m.groupValues[2]
        }
        val carried = m.groupValues[1] + mark
        return TextFieldValue(new.text.replaceRange(at + 1, at + 1, carried), TextRange(at + 1 + carried.length))
    }

    /** The cursor's line changed by [change], the cursor kept where it was in the words. */
    private fun changeLine(value: TextFieldValue, change: (String) -> String): TextFieldValue {
        val at = value.selection.min
        val lineStart = value.text.lastIndexOf('\n', at - 1) + 1
        val lineEnd = value.text.indexOf('\n', at).let { if (it < 0) value.text.length else it }
        val line = value.text.substring(lineStart, lineEnd)
        val changed = change(line)
        return TextFieldValue(value.text.replaceRange(lineStart, lineEnd, changed), TextRange((at + changed.length - line.length).coerceAtLeast(lineStart)))
    }

    /** The line two spaces further in (a sub-item), or out again. */
    fun indent(value: TextFieldValue, inward: Boolean): TextFieldValue =
        changeLine(value) { line -> if (inward) "  $line" else line.removePrefix(line.take(2).takeWhile { it == ' ' }) }

    /** The line's heading one level smaller each time, # to ## to ###, then plain again. */
    fun cycleHeading(value: TextFieldValue): TextFieldValue = changeLine(value) { line ->
        when {
            line.startsWith("### ") -> line.removePrefix("### ")
            line.startsWith("## ") -> "#$line"
            line.startsWith("# ") -> "#$line"
            else -> "# $line"
        }
    }
}

/** Shows a note styled as Markdown while it's typed; nothing is added or taken away, so the cursor maps one to one. */
class MarkdownTransformation(private val faint: Color, private val accent: Color) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText =
        TransformedText(Markdown.styled(text.text, faint, accent), OffsetMapping.Identity)

    override fun equals(other: Any?) = other is MarkdownTransformation && other.faint == faint && other.accent == accent

    override fun hashCode() = faint.hashCode() * 31 + accent.hashCode()
}
