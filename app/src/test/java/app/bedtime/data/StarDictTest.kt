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
}
