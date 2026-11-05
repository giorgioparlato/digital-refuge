package app.bedtime.ui.stats

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HeatColoursTest {
    private fun close(a: Color, b: Color) = listOf(a.red - b.red, a.green - b.green, a.blue - b.blue).all { kotlin.math.abs(it) < 0.01f }
    private val hour = 3_600L

    @Test
    fun greenThroughCreamAtTheBudgetToRedAtTheMonthsWorst() {
        assertTrue(close(Color(0xFF5FA87E), HeatColours.at(0, hour, 3 * hour)))
        assertTrue(close(Color(0xFFF2E6C9), HeatColours.at(hour, hour, 3 * hour)))
        // The worst day of the month is as red as it gets, whatever it was.
        assertTrue(close(Color(0xFFD7263D), HeatColours.at(3 * hour, hour, 3 * hour)))
        assertTrue(close(Color(0xFFD7263D), HeatColours.at(5 * hour, hour, 5 * hour)))
        // A month all within the budget has no red at all.
        assertTrue(close(Color(0xFFF2E6C9), HeatColours.at(hour, hour, hour)))
        assertEquals(HeatColours.at(hour, hour, 2 * hour), HeatColours.at(hour, hour, 4 * hour))
    }
}
