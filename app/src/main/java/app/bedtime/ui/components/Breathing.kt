package app.bedtime.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer

private const val INHALE_MS = 4_000
private const val EXHALE_MS = 6_000
private const val SMALLEST = 0.55f

/**
 * A soft disc that swells for four seconds and settles for six: a longer breath out than in, which
 * is what slows a body down. [content] sits on top and is told which half of the breath it is in;
 * while not [running], the disc rests small and [content] is told "in".
 */
@Composable
fun Breathing(
    running: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable (inhaling: Boolean) -> Unit,
) {
    val scale = remember { Animatable(SMALLEST) }
    var inhaling by remember { mutableStateOf(true) }
    LaunchedEffect(running) {
        if (!running) {
            inhaling = true
            scale.animateTo(SMALLEST, tween(800))
            return@LaunchedEffect
        }
        while (true) {
            inhaling = true
            scale.animateTo(1f, tween(INHALE_MS, easing = FastOutSlowInEasing))
            inhaling = false
            scale.animateTo(SMALLEST, tween(EXHALE_MS, easing = FastOutSlowInEasing))
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier.matchParentSize()
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
        )
        content(inhaling)
    }
}
