package app.bedtime.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import app.bedtime.data.WeatherIcon

/**
 * The sky as soft filled shapes, in the family of the lotus and the moon: a full sun with round rays,
 * plump clouds made of overlapping circles, rounded drops. 24×24, tinted by Icon.
 */
object WeatherIcons {
    private fun circle(cx: Float, cy: Float, r: Float) = "M${cx - r},$cy A$r,$r 0 1,0 ${cx + r},$cy A$r,$r 0 1,0 ${cx - r},$cy Z"

    private fun rect(x: Float, y: Float, w: Float, h: Float) = "M$x,$y H${x + w} V${y + h} H$x Z"

    private fun bar(x: Float, y: Float, w: Float, h: Float): String {
        val r = h / 2
        return "M${x + r},$y H${x + w - r} A$r,$r 0 0,1 ${x + w - r},${y + h} H${x + r} A$r,$r 0 0,1 ${x + r},$y Z"
    }

    /** A drop, pointed at the top, round at the bottom, its centre at ([cx], [cy]). */
    private fun drop(cx: Float, cy: Float, r: Float = 1.35f) =
        "M$cx,${cy - 2.6f * r} L${cx + 0.95f * r},${cy - 0.3f * r} A$r,$r 0 1,1 ${cx - 0.95f * r},${cy - 0.3f * r} Z"

    /** A plump cloud: three circles over a filled base, the base [dy] below the full-size one's. */
    private fun cloud(scale: Float = 1f, dx: Float = 0f, dy: Float = 0f): List<String> {
        fun x(v: Float) = dx + 12 + (v - 12) * scale
        fun y(v: Float) = dy + 12 + (v - 12) * scale
        return listOf(
            circle(x(7.6f), y(14f), 4.2f * scale),
            circle(x(12.6f), y(10.6f), 5.6f * scale),
            circle(x(17.2f), y(14.2f), 4f * scale),
            rect(x(7.6f), y(12.5f), (17.2f - 7.6f) * scale, 5.7f * scale),
        )
    }

    private fun rays(cx: Float, cy: Float, distance: Float, r: Float, angles: List<Int>) = angles.map { deg ->
        val rad = Math.toRadians(deg.toDouble())
        circle(cx + (distance * kotlin.math.cos(rad)).toFloat(), cy + (distance * kotlin.math.sin(rad)).toFloat(), r)
    }

    private val shapes: Map<WeatherIcon, List<String>> = mapOf(
        WeatherIcon.CLEAR to listOf(circle(12f, 12f, 5f)) + rays(12f, 12f, 9f, 1.35f, (0 until 360 step 45).toList()),
        WeatherIcon.PARTLY to listOf(circle(8.6f, 8.8f, 3.9f)) + rays(8.6f, 8.8f, 6.9f, 1.05f, listOf(135, 180, 225, 270, 315)) +
            cloud(scale = 0.78f, dx = 2.2f, dy = 3.4f),
        WeatherIcon.CLOUDY to cloud(dy = 0.5f),
        WeatherIcon.RAIN to cloud(scale = 0.92f, dy = -3.4f) + listOf(drop(8.4f, 20.2f), drop(12.6f, 21.6f), drop(16.8f, 20.2f)),
        WeatherIcon.STORM to cloud(scale = 0.92f, dy = -3.4f) + "M13.2,14.6 L9.6,19.6 H12.2 L11.1,23.4 L15.6,17.6 H13 L14.4,14.6 Z",
        WeatherIcon.SNOW to cloud(scale = 0.92f, dy = -3.4f) +
            listOf(circle(8.2f, 18.6f, 1.25f), circle(12.6f, 20f, 1.25f), circle(17f, 18.6f, 1.25f), circle(10.4f, 22.2f, 1.1f), circle(14.8f, 22.2f, 1.1f)),
        WeatherIcon.FOG to listOf(bar(3f, 6.5f, 18f, 2.6f), bar(5.5f, 11f, 13f, 2.6f), bar(3f, 15.5f, 18f, 2.6f)),
    )

    private val built = mutableMapOf<WeatherIcon, ImageVector>()

    fun of(icon: WeatherIcon): ImageVector = built.getOrPut(icon) {
        ImageVector.Builder(icon.name, 24.dp, 24.dp, 24f, 24f).apply {
            shapes.getValue(icon).forEach { addPath(pathData = addPathNodes(it), fill = SolidColor(Color.Black)) }
        }.build()
    }
}
