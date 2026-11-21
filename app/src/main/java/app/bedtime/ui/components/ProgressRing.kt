package app.bedtime.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.bedtime.ui.theme.Obsidian

/**
 * A thin ring filled clockwise from the top by [progress] (0–1): how much of a session or a wait has
 * passed. The track is the raised grey, the fill the accent unless [color] says otherwise.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    stroke: Dp = 4.dp,
    color: Color = Obsidian.colors.accent,
    track: Color = Obsidian.colors.bgSecondary,
) {
    Canvas(modifier) {
        val width = stroke.toPx()
        val inset = width / 2
        val arcSize = androidx.compose.ui.geometry.Size(size.width - width, size.height - width)
        val topLeft = androidx.compose.ui.geometry.Offset(inset, inset)
        drawArc(track, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width))
        val sweep = 360f * progress.coerceIn(0f, 1f)
        if (sweep > 0f) {
            drawArc(color, -90f, sweep, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(width, cap = StrokeCap.Round))
        }
    }
}
