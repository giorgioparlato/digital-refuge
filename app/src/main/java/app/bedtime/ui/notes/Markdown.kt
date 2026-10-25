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
}
