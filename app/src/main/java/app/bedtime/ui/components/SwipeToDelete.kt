package app.bedtime.ui.components

import androidx.compose.runtime.mutableStateOf

import androidx.compose.runtime.setValue

import androidx.compose.runtime.getValue

import androidx.compose.ui.layout.onSizeChanged

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Swipe a row left to reveal a red "delete" button behind it; deleting takes a second tap on that
 * button, so it never happens by accident. Swiping right closes it again. With [swipeAway], where a
 * deletion can be undone (notes go to a trash), a swipe most of the way across deletes it at once.
 *
 * [panel] should match the screen behind the row, so the red button only shows once revealed.
 */
@Composable
fun SwipeToDelete(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    panel: Color = Obsidian.colors.bgPrimary,
    swipeAway: Boolean = false,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        // Locked: no reveal, so deleting can't be a way around a session.
        Box(modifier.fillMaxWidth().background(panel)) { content() }
        return
    }
    val c = Obsidian.colors
    val scope = rememberCoroutineScope()
    val revealPx = with(LocalDensity.current) { 96.dp.toPx() }
    val offset = remember { Animatable(0f) }

    var width by remember { mutableStateOf(0f) }
    Box(modifier.fillMaxWidth().onSizeChanged { width = it.width.toFloat() }) {
        // The delete button, revealed as the row slides left.
        Box(Modifier.matchParentSize(), contentAlignment = Alignment.CenterEnd) {
            Box(
                Modifier
                    .width(88.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(CardRadius))
                    .background(c.red)
                    .clickable(onClickLabel = "delete") {
                        scope.launch { offset.animateTo(0f) }
                        onDelete()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("delete", color = Color.White, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge)
            }
        }
        // The row itself, draggable and drawn over the button.
        Box(
            Modifier
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .fillMaxWidth()
                .background(panel)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dx ->
                            val furthest = if (swipeAway) -width else -revealPx
                            scope.launch { offset.snapTo((offset.value + dx).coerceIn(furthest, 0f)) }
                        },
                        onDragEnd = {
                            scope.launch {
                                if (swipeAway && offset.value < -width * 0.45f) {
                                    // Most of the way across: gone, with the undo the list offers.
                                    offset.animateTo(-width)
                                    onDelete()
                                    offset.snapTo(0f)
                                } else {
                                    offset.animateTo(if (offset.value < -revealPx / 2) -revealPx else 0f)
                                }
                            }
                        },
                    )
                },
        ) {
            content()
        }
    }
}
