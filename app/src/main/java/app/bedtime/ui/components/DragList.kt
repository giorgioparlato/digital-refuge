package app.bedtime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.bedtime.ui.theme.Obsidian

/**
 * Rows you put in your own order by dragging their handle, or with [wholeRow] by holding a row itself
 * and dragging it, with no handle. While one is dragged the order lives here; [onMoved] gets the new
 * order when it's let go.
 */
@Composable
fun <T> DragList(items: List<T>, key: (T) -> String, onMoved: (List<T>) -> Unit, wholeRow: Boolean = false, row: @Composable RowScope.(T) -> Unit) {
    val c = Obsidian.colors
    var local by remember(items) { mutableStateOf(items) }
    var dragging by remember { mutableStateOf<String?>(null) }
    var offset by remember { mutableFloatStateOf(0f) }
    var rowHeight by remember { mutableIntStateOf(1) }
    local.forEach { item ->
        val id = key(item)
        key(id) {
            val lifted = dragging == id
            val start = { dragging = id; offset = 0f }
            val end = { dragging = null; offset = 0f; onMoved(local) }
            val cancel = { dragging = null; offset = 0f; local = items }
            val move = { dy: Float ->
                offset += dy
                // Past half a row, it trades places with its neighbour.
                val at = local.indexOfFirst { key(it) == id }
                if (offset > rowHeight / 2f && at < local.lastIndex) {
                    local = local.toMutableList().apply { add(at + 1, removeAt(at)) }
                    offset -= rowHeight
                } else if (offset < -rowHeight / 2f && at > 0) {
                    local = local.toMutableList().apply { add(at - 1, removeAt(at)) }
                    offset += rowHeight
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged { rowHeight = it.height.coerceAtLeast(1) }
                    .zIndex(if (lifted) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (lifted) offset else 0f
                        if (lifted && wholeRow) { scaleX = 1.02f; scaleY = 1.02f }
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (lifted && !wholeRow) c.bgPrimaryAlt else Color.Transparent)
                    .then(
                        if (wholeRow) {
                            Modifier.pointerInput(id) {
                                detectDragGesturesAfterLongPress(onDragStart = { start() }, onDragEnd = { end() }, onDragCancel = { cancel() }) { change, amount ->
                                    change.consume()
                                    move(amount.y)
                                }
                            }
                        } else {
                            Modifier
                        },
                    )
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!wholeRow) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .pointerInput(id) {
                                detectDragGestures(onDragStart = { start() }, onDragEnd = { end() }, onDragCancel = { cancel() }) { change, amount ->
                                    change.consume()
                                    move(amount.y)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Default.Menu, contentDescription = "drag to move", tint = c.textFaint, modifier = Modifier.size(20.dp)) }
                }
                row(item)
            }
        }
    }
}
