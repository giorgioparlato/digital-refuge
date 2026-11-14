package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotesWordsTest {
    @Test
    fun aWordGoesAtTheFootOnceOnly() {
        val text = "# words\n- **ephemeral** — lasting a very short time\n"
        assertEquals(
            "# words\n- **ephemeral** — lasting a very short time\n- **sonder** — the sense that every passer-by has a life\n",
            Notes.withWord(text, "sonder", "the sense that every passer-by has a life"),
        )
        assertNull(Notes.withWord(text, "Ephemeral", "again"))
        assertEquals("- **word**", Notes.wordLine("word", ""))
    }
}
