package app.bedtime.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp
import app.bedtime.data.WeatherIcon

/** The sky in a few quiet shapes, drawn like the app's other icons (24×24, tinted by Icon). */
object WeatherIcons {
    private fun circle(cx: Float, cy: Float, r: Float) = "M${cx - r},$cy A$r,$r 0 1,0 ${cx + r},$cy A$r,$r 0 1,0 ${cx - r},$cy Z"

    private const val CLOUD = "M6.5,18 A4.5,4.5 0 0,1 6,9.03 A6,6 0 0,1 17.3,8.5 A4.75,4.75 0 0,1 17.5,18 Z"
    /** The same cloud, higher, leaving room under it for rain, a bolt or snow. */
    private const val CLOUD_HIGH = "M6.5,14 A4.5,4.5 0 0,1 6,5.03 A6,6 0 0,1 17.3,4.5 A4.75,4.75 0 0,1 17.5,14 Z"

    private val sun = buildList {
        add(circle(12f, 12f, 4.5f))
        // Eight dots for rays: calmer than spikes.
        listOf(0f to -8.5f, 6f to -6f, 8.5f to 0f, 6f to 6f, 0f to 8.5f, -6f to 6f, -8.5f to 0f, -6f to -6f)
            .forEach { (dx, dy) -> add(circle(12 + dx, 12 + dy, 1.3f)) }
    }

    private val shapes: Map<WeatherIcon, List<String>> = mapOf(
        WeatherIcon.CLEAR to sun,
        WeatherIcon.PARTLY to listOf(circle(8.5f, 8.5f, 3.6f), "M9,21 A4,4 0 0,1 8.6,13.03 A5,5 0 0,1 17.8,12.4 A4.2,4.2 0 0,1 18,21 Z"),
        WeatherIcon.CLOUDY to listOf(CLOUD),
        WeatherIcon.RAIN to listOf(CLOUD_HIGH, "M8.2,16 h1.5 l-1.2,5 h-1.5 Z", "M12.2,16 h1.5 l-1.2,5 h-1.5 Z", "M16.2,16 h1.5 l-1.2,5 h-1.5 Z"),
        WeatherIcon.STORM to listOf(CLOUD_HIGH, "M12.8,14.5 L9.5,19.5 H12 L11,23 L15.3,17.3 H12.8 L14.2,14.5 Z"),
        WeatherIcon.SNOW to listOf(CLOUD_HIGH, circle(8f, 17.5f, 1.1f), circle(12f, 19f, 1.1f), circle(16f, 17.5f, 1.1f), circle(10f, 21.8f, 1.1f), circle(14f, 21.8f, 1.1f)),
        WeatherIcon.FOG to listOf(
            "M3,7 H21 A1,1 0 0,1 21,9 H3 A1,1 0 0,1 3,7 Z",
            "M5,11 H19 A1,1 0 0,1 19,13 H5 A1,1 0 0,1 5,11 Z",
            "M3,15 H21 A1,1 0 0,1 21,17 H3 A1,1 0 0,1 3,15 Z",
        ),
    )

    private val built = mutableMapOf<WeatherIcon, ImageVector>()

    fun of(icon: WeatherIcon): ImageVector = built.getOrPut(icon) {
        ImageVector.Builder(icon.name, 24.dp, 24.dp, 24f, 24f).apply {
            shapes.getValue(icon).forEach { addPath(pathData = addPathNodes(it), fill = SolidColor(Color.Black)) }
        }.build()
    }
}
