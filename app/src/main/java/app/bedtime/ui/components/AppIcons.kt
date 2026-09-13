package app.bedtime.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import app.bedtime.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bedtime.apps.AppCatalog
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun interface AppIconLoader {
    fun load(packageName: String): ImageBitmap?
}

/** Defaults to "no icons" so screens render in previews and screenshot tests; activities provide the real loader. */
val LocalAppIconLoader = staticCompositionLocalOf { AppIconLoader { null } }

@Composable
fun ProvideAppIcons(content: @Composable () -> Unit) {
    val context = LocalContext.current.applicationContext
    val loader = remember(context) { AppIconLoader { AppCatalog.icon(context, it) } }
    CompositionLocalProvider(LocalAppIconLoader provides loader, content = content)
}

@Composable
fun AppIcon(packageName: String, label: String, size: Dp = 40.dp) {
    val loader = LocalAppIconLoader.current
    var icon by remember(packageName, loader) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(packageName, loader) {
        icon = withContext(Dispatchers.IO) { loader.load(packageName) }
    }
    val bitmap = icon
    if (bitmap != null) {
        Image(bitmap, contentDescription = null, modifier = Modifier.size(size))
    } else {
        LetterAvatar(label, size)
    }
}

@Composable
fun LetterAvatar(label: String, size: Dp) {
    val c = Obsidian.colors
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(c.accent.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label.firstOrNull()?.uppercase() ?: "?",
            color = c.accentText,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.42f).sp,
        )
    }
}

/** A few overlapping app icons plus "+N". */
@Composable
fun AppIconStack(packages: List<String>, label: (String) -> String, max: Int = 3, size: Dp = 26.dp) {
    if (packages.isEmpty()) return
    val c = Obsidian.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
            packages.take(max).forEach { pkg ->
                Box(
                    Modifier
                        .clip(RoundedCornerShape(size * 0.35f))
                        .background(c.bgSecondary)
                        .padding(2.dp),
                ) { AppIcon(pkg, label(pkg), size) }
            }
        }
        if (packages.size > max) {
            Text(
                "+${packages.size - max}",
                style = MaterialTheme.typography.labelMedium,
                color = c.textMuted,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}
