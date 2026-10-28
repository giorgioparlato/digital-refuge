package app.bedtime.ui.homestyle

import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import app.bedtime.data.HomeStyle

internal val BackgroundPresets: List<Pair<String, Long>> = listOf(
    "Graphite" to 0xFF282828,
    "Black" to 0xFF000000,
    "Forest" to 0xFF1D2621,
    "Night" to 0xFF1A2130,
    "Plum" to 0xFF2A1F2B,
    "Paper" to 0xFFF8F5F1,
    "Sand" to 0xFFEFE6D8,
)

internal val TextPresets: List<Pair<String, Long>> = listOf(
    "Cream" to 0xFFD7C0A3,
    "Warm white" to 0xFFE8E0D4,
    "White" to 0xFFFFFFFF,
    "Sage" to 0xFFB7D3C2,
    "Ink" to 0xFF2B2620,
    "Brown" to 0xFF593E22,
)

/** A few calm cards for the note widget, dark and light; anything else is a custom colour. */
internal val NotePresets: List<Pair<String, Long>> = listOf(
    "Paper" to 0xFFF8F5F1,
    "Sand" to 0xFFEFE6D8,
    "Graphite" to 0xFF363636,
    "Forest" to 0xFF2C3A32,
    "Night" to 0xFF22304A,
)

internal val AccentPresets: List<Pair<String, Long>> = listOf(
    "Sage" to 0xFF86BC9F,
    "Bright green" to 0xFF4EC68E,
    "Cream" to 0xFFD7C0A3,
    "Amber" to 0xFFE0B243,
    "Sky" to 0xFF4DB2D1,
    "Rose" to 0xFFE47B8A,
    "Lavender" to 0xFFA99BEA,
)

/** "#1D2621" or "1d2621" → opaque ARGB, or null if it isn't a 6-digit hex colour. */
fun parseHexColor(text: String): Long? {
    val hex = text.trim().removePrefix("#")
    if (hex.length != 6 || !hex.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) return null
    return 0xFF000000L or hex.toLong(16)
}

fun Long.toHexColor(): String = "#%06X".format(this and 0xFFFFFFL)

/** Colours for the minimal home screen; text shades follow the background's brightness so they stay readable. */
data class HomePalette(
    val background: Color,
    val accent: Color,
    val text: Color,
    val muted: Color,
    val faint: Color,
    val chip: Color,
    val isLight: Boolean,
)

fun HomeStyle.palette(): HomePalette {
    val bg = Color(background)
    val light = bg.luminance() > 0.5f
    // The app's own cream on a dark background, ink on a light one, unless a colour was chosen.
    val text = textColor?.let { Color(it) } ?: if (light) Color(0xFF2B2620) else Color(0xFFD7C0A3)
    return HomePalette(
        background = bg,
        accent = Color(accent),
        text = text,
        // The quieter shades are the text eased toward the background, so any text colour keeps its family.
        muted = lerp(bg, text, 0.75f),
        faint = lerp(bg, text, 0.5f),
        chip = if (light) Color(0x14000000) else Color(0x1AFFFFFF),
        isLight = light,
    )
}
