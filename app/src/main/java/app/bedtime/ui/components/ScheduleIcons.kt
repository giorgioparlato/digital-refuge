package app.bedtime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.bedtime.ui.theme.Obsidian

/** The small marks a schedule or block can carry on its pill and in the lists; the moon when none is chosen. */
object ScheduleIcons {
    val all: List<Pair<String, ImageVector>> by lazy {
        listOf(
            "moon" to BedtimeIcons.Moon,
            "sun" to BedtimeIcons.Sun,
            "lotus" to BedtimeIcons.Refuge,
            "leaf" to BedtimeIcons.Leaf,
            "book" to BedtimeIcons.Book,
            "headphones" to BedtimeIcons.Headphones,
            "target" to BedtimeIcons.Target,
            "hourglass" to BedtimeIcons.Hourglass,
            "work" to Icons.Default.Build,
            "home" to Icons.Default.Home,
            "heart" to Icons.Default.Favorite,
            "star" to Icons.Default.Star,
        )
    }

    fun of(key: String?): ImageVector = all.firstOrNull { it.first == key }?.second ?: BedtimeIcons.Moon
}

/** A grid of [ScheduleIcons] to pick from, the current one marked. */
@Composable
fun ScheduleIconDialog(current: String?, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val c = Obsidian.colors
    val chosen = current ?: "moon"
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.bgSecondary,
        title = { Text("Its mark", color = c.textNormal) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ScheduleIcons.all.chunked(4).forEach { row ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        row.forEach { (key, icon) ->
                            Box(
                                Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (key == chosen) c.accent.copy(alpha = 0.18f) else Color.Transparent)
                                    .clickable(onClickLabel = key) { onPick(key) },
                                contentAlignment = Alignment.Center,
                            ) { Icon(icon, contentDescription = key, tint = if (key == chosen) c.accent else c.textMuted, modifier = Modifier.size(24.dp)) }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = c.textMuted) } },
    )
}
