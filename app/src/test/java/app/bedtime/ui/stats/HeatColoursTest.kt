package app.bedtime.ui.stats

import androidx.compose.ui.graphics.Color
import app.bedtime.data.HeatScale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatColoursTest {
    private fun close(a: Color, b: Color) = listOf(a.red - b.red, a.green - b.green, a.blue - b.blue).all { kotlin.math.abs(it) < 0.01f }

    @Test
    fun calmThroughAmberAtTheBudgetToRed() {
        assertTrue(close(Color(0xFF5FA87E), HeatColours.at(0f, HeatScale.GREEN)))
        assertTrue(close(Color(0xFFF2E6C9), HeatColours.at(0f, HeatScale.CREAM)))
        assertTrue(close(Color(0xFFE0B243), HeatColours.at(1f, HeatScale.GREEN)))
        assertTrue(close(Color(0xFFD7263D), HeatColours.at(2f, HeatScale.CREAM)))
        // Past twice the budget, as red as it gets.
        assertEquals(HeatColours.at(2f, HeatScale.GREEN), HeatColours.at(5f, HeatScale.GREEN))
    }
}
