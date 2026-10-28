package app.bedtime.ui.notes

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteCardTextTest {
    @Test
    fun leftToSuitItDarkWordsGoOnALightCard() {
        assertTrue(inkOnCard(Color(0xFFF8F5F1), null))
        assertFalse(inkOnCard(Color(0xFF22304A), null))
    }

    @Test
    fun aChoiceWinsOverTheCard() {
        assertFalse(inkOnCard(Color(0xFFF8F5F1), dark = false))
        assertTrue(inkOnCard(Color(0xFF22304A), dark = true))
    }
}
