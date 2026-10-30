package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream

class StarDictTest {
    @get:Rule
    val folder = TemporaryFolder()

    /** A dictionary as StarDict writes one: its words sorted its way, each pointing into the .dict. */
    private fun make(name: String, entries: List<Pair<String, String>>, types: String? = "m"): StarDict {
        val dir = folder.newFolder(name)
        val sorted = entries.sortedWith { a, b -> StarDicts.compare(a.first, b.first) }
        val dict = ByteArrayOutputStream()
        val idx = ByteArrayOutputStream()
        val out = DataOutputStream(idx)
        sorted.forEach { (word, text) ->
            val bytes = text.toByteArray()
            out.write(word.toByteArray())
            out.writeByte(0)
            out.writeInt(dict.size())
            out.writeInt(bytes.size)
            dict.write(bytes)
        }
        java.io.File(dir, "dict.ifo").writeText("StarDict's dict ifo file\nversion=2.4.2\nbookname=$name\nwordcount=${entries.size}\n" + (types?.let { "sametypesequence=$it\n" } ?: ""))
        java.io.File(dir, "dict.idx").writeBytes(idx.toByteArray())
        java.io.File(dir, "dict.dict").writeBytes(dict.toByteArray())
        return StarDicts.open(dir)!!
    }

    @Test
    fun aWordFoundWhateverItsCase() {
        val d = make("English", listOf("refuge" to "a place of shelter", "Rain" to "water falling in drops", "read" to "to look at words"))
        assertEquals(Definition("refuge", "English", "a place of shelter"), d.define("Refuge"))
        assertEquals("water falling in drops", d.define("rain")!!.text)
        assertNull(d.define("refuges"))
    }

    @Test
    fun wordsWithAccentsAreFoundToo() {
        val d = make("Italiano", listOf("perché" to "per quale ragione", "pera" to "un frutto", "però" to "ma", "perdere" to "smarrire"))
        assertEquals("per quale ragione", d.define("perché")!!.text)
        assertEquals("ma", d.define("però")!!.text)
        assertEquals(listOf("pera", "perché", "perdere", "però"), d.startingWith("per"))
    }

    @Test
    fun markupIsTakenOut() {
        assertEquals("shelter\na safe place & rest", StarDicts.plain("<b>shelter</b><br/>a safe place &amp; rest".toByteArray(), "h"))
        // Without a type sequence, each field starts with its letter and ends with a zero.
        assertEquals("the sense", StarDicts.plain("mthe sense\u0000".toByteArray(), null))
    }

    private fun marked(styled: Styled, kind: MarkKind) = styled.marks.filter { it.kind == kind }.map { styled.text.substring(it.start, it.end) }

    @Test
    fun htmlKeepsItsLabelsBoldAndLines() {
        val s = StarDicts.styled("<b>refuge</b> <i>noun</i><br>a place of safety".toByteArray(), "h")
        assertEquals("refuge noun\na place of safety", s.text)
        assertEquals(listOf("noun"), marked(s, MarkKind.LABEL))
        assertEquals(listOf("refuge"), marked(s, MarkKind.STRONG))
    }

    @Test
    fun plainTextKeepsItsBracketsAndTheirLabels() {
        // FreeDict's plain entries: "<n, c>" was once read as a tag and lost.
        val s = StarDicts.styled("katt /kat/\n<n, c>\ncat".toByteArray(), "m")
        assertEquals("katt /kat/\n<n, c>\ncat", s.text)
        assertEquals(listOf("<n, c>"), marked(s, MarkKind.LABEL))
        // WordNet's: the part of speech opening a line.
        val w = StarDicts.styled("n 1: a shelter\nv 1: to take shelter".toByteArray(), "m")
        assertEquals(listOf("n", "v"), marked(w, MarkKind.LABEL))
        // A word in a sentence isn't a label.
        assertEquals(emptyList<String>(), marked(StarDicts.styled("a verb in the past".toByteArray(), "m"), MarkKind.LABEL))
    }

    @Test
    fun xdxfExamplesAndAbbreviations() {
        val s = StarDicts.styled("<abr>adj.</abr> quiet <ex>a quiet room</ex>".toByteArray(), "x")
        assertEquals("adj. quiet a quiet room", s.text)
        assertEquals(listOf("adj."), marked(s, MarkKind.LABEL))
        assertEquals(listOf("a quiet room"), marked(s, MarkKind.EXAMPLE))
    }

    @Test
    fun crossReferencesBecomeLinks() {
        val s = StarDicts.styled("n 1: a place of safety [syn: {shelter}, {protection}]".toByteArray(), "m")
        assertEquals("n 1: a place of safety [syn: shelter, protection]", s.text)
        assertEquals(listOf("shelter", "protection"), s.marks.filter { it.kind == MarkKind.LINK }.map { it.target })
        assertEquals(listOf("shelter", "protection"), marked(s, MarkKind.LINK))
        // A gender in braces stays a label, braces and all.
        assertEquals(listOf("{f}"), marked(StarDicts.styled("katt {f}".toByteArray(), "m"), MarkKind.LABEL))
        // And xdxf's own references.
        assertEquals(listOf("refuge"), StarDicts.styled("see <kref>refuge</kref>".toByteArray(), "x").marks.map { it.target })
    }

    @Test
    fun aTapLooksUpTheWordUnderIt() {
        val text = "a place of safety [syn: shelter, well-being]"
        val marks = listOf(Mark(24, 31, MarkKind.LINK, "shelter"))
        assertEquals("shelter", StarDicts.wordAt(text, marks, 26))
        assertEquals("safety", StarDicts.wordAt(text, emptyList(), 13))
        assertEquals("well-being", StarDicts.wordAt(text, emptyList(), text.indexOf("being")))
        // Between words, or on a lone letter, nothing.
        assertNull(StarDicts.wordAt(text, emptyList(), 7))
        assertNull(StarDicts.wordAt(text, emptyList(), 0))
    }
}
