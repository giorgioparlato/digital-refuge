package app.bedtime.ui.notes

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextLayoutResult

/**
 * The to-do boxes of [text], drawn over where its (invisible) box marks are laid out: a soft rounded
 * square when open, filled in [accent] with a [tick] when done. [layout] is the text field's latest layout.
 */
fun Modifier.todoBoxes(text: String, layout: () -> TextLayoutResult?, open: Color, accent: Color, tick: Color = Color.White): Modifier =
    drawWithContent {
        drawContent()
        val laid = layout() ?: return@drawWithContent
        Markdown.boxes(text).forEach { (at, done) ->
            if (at >= laid.layoutInput.text.length) return@forEach
            val glyph = laid.getBoundingBox(at)
            val side = glyph.height * 0.66f
            val topLeft = Offset(glyph.center.x - side / 2, glyph.center.y - side / 2)
            drawRoundRect(if (done) accent else open, topLeft, Size(side, side), CornerRadius(side * 0.24f))
            if (done) {
                val check = Path().apply {
                    moveTo(topLeft.x + side * 0.26f, topLeft.y + side * 0.53f)
                    lineTo(topLeft.x + side * 0.44f, topLeft.y + side * 0.70f)
                    lineTo(topLeft.x + side * 0.76f, topLeft.y + side * 0.34f)
                }
                drawPath(check, tick, style = Stroke(width = side * 0.13f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }
