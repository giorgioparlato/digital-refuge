package app.bedtime.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bedtime.R

/**
 * Tokens based on the "Primary" Obsidian theme (primary-theme/obsidian, classic palette), with green
 * as the accent. Dark mode swaps Primary's brown grays for neutral ones around #282828; text keeps
 * Primary's warm cream. Light mode is Primary's cream "magazine paper" palette.
 */
@Immutable
data class ObsidianColors(
    val isDark: Boolean,
    val bgPrimary: Color,
    /** Recessed surfaces: text fields, code, time tiles. */
    val bgPrimaryAlt: Color,
    /** Raised surfaces: cards. */
    val bgSecondary: Color,
    val border: Color,
    val interactive: Color,
    val interactiveHover: Color,
    val textNormal: Color,
    val textMuted: Color,
    val textFaint: Color,
    /** Bright green for toggles, progress, icons, tints and focus rings. */
    val accent: Color,
    /** Deep green behind text: main buttons, selected days, active steps. */
    val accentFill: Color,
    val textOnAccent: Color,
    /** Green for text: links, tags, labels. */
    val accentText: Color,
    val red: Color,
    val orange: Color,
    val green: Color,
    val blue: Color,
)

val DarkObsidian = ObsidianColors(
    isDark = true,
    bgPrimary = Color(0xFF282828),
    bgPrimaryAlt = Color(0xFF222222),
    bgSecondary = Color(0xFF2F2F2F),
    border = Color(0xFF3A3A3A),
    interactive = Color(0xFF363636),
    interactiveHover = Color(0xFF424242),
    textNormal = Color(0xFFD7C0A3), // d-gray-20
    textMuted = Color(0xFFAB916D), // d-gray-40
    textFaint = Color(0xFF917959), // d-gray-50
    accent = Color(0xFF2EA873), // d-green-20
    accentFill = Color(0xFF117449), // d-green-40
    textOnAccent = Color(0xFFEBD8C6), // d-gray-10
    accentText = Color(0xFF4EC68E), // d-green-10
    red = Color(0xFFF7685E), // d-red-20
    orange = Color(0xFFEF8839), // d-orange-10
    green = Color(0xFF4EC68E), // d-green-10
    blue = Color(0xFF4DB2D1), // d-blue-20
)

val LightObsidian = ObsidianColors(
    isDark = false,
    bgPrimary = Color(0xFFF8F5F1), // l-gray-20
    bgPrimaryAlt = Color(0xFFF2ECE3), // l-gray-30
    bgSecondary = Color(0xFFFCFAF8), // l-gray-10
    border = Color(0xFFE4D8C3), // l-gray-60
    interactive = Color(0xFFEEE7DD), // l-gray-40
    interactiveHover = Color(0xFFE4D8C3), // l-gray-60
    textNormal = Color(0xFF593E22), // l-gray-130
    textMuted = Color(0xFF836B49), // l-gray-100
    textFaint = Color(0xFFB79D7B), // l-gray-80
    accent = Color(0xFF3EB174), // l-green-20
    accentFill = Color(0xFF1A7A4F), // l-green-40
    textOnAccent = Color(0xFFFCFAF8), // l-gray-10
    accentText = Color(0xFF1A7A4F), // l-green-40
    red = Color(0xFFBF3F36), // l-red-30
    orange = Color(0xFFD58534),
    green = Color(0xFF329562), // l-green-30
    blue = Color(0xFF3079B0),
)

val Inter = FontFamily(
    Font(R.font.inter_400, FontWeight.Normal),
    Font(R.font.inter_500, FontWeight.Medium),
    Font(R.font.inter_600, FontWeight.SemiBold),
    Font(R.font.inter_700, FontWeight.Bold),
)

private fun TextStyle.inter(weight: FontWeight? = null) =
    copy(fontFamily = Inter, letterSpacing = 0.sp, fontWeight = weight ?: fontWeight)

private val ObsidianTypography = Typography().let { t ->
    Typography(
        displayLarge = t.displayLarge.inter(),
        displayMedium = t.displayMedium.inter(),
        displaySmall = t.displaySmall.inter(),
        headlineLarge = t.headlineLarge.inter(FontWeight.Bold),
        headlineMedium = t.headlineMedium.inter(FontWeight.Bold),
        headlineSmall = t.headlineSmall.inter(FontWeight.SemiBold),
        titleLarge = t.titleLarge.inter(FontWeight.SemiBold),
        titleMedium = t.titleMedium.inter(FontWeight.SemiBold),
        titleSmall = t.titleSmall.inter(FontWeight.SemiBold),
        bodyLarge = t.bodyLarge.inter(),
        bodyMedium = t.bodyMedium.inter(),
        bodySmall = t.bodySmall.inter(),
        labelLarge = t.labelLarge.inter(),
        labelMedium = t.labelMedium.inter(),
        labelSmall = t.labelSmall.inter(),
    )
}

private val ObsidianShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(6.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(10.dp),
    extraLarge = RoundedCornerShape(14.dp),
)

private fun ObsidianColors.toColorScheme(): ColorScheme =
    (if (isDark) darkColorScheme() else lightColorScheme()).copy(
        primary = accentFill,
        onPrimary = textOnAccent,
        primaryContainer = accentFill,
        onPrimaryContainer = textOnAccent,
        secondary = accent,
        onSecondary = textOnAccent,
        secondaryContainer = interactive,
        onSecondaryContainer = textNormal,
        tertiary = accent,
        tertiaryContainer = accentFill,
        onTertiaryContainer = textOnAccent,
        background = bgPrimary,
        onBackground = textNormal,
        surface = bgPrimary,
        onSurface = textNormal,
        surfaceVariant = bgSecondary,
        onSurfaceVariant = textMuted,
        surfaceTint = bgPrimary,
        surfaceContainerLowest = bgPrimaryAlt,
        surfaceContainerLow = bgSecondary,
        surfaceContainer = bgSecondary,
        surfaceContainerHigh = bgSecondary,
        surfaceContainerHighest = interactive,
        outline = textFaint,
        outlineVariant = border,
        error = red,
        onError = textOnAccent,
    )

private val LocalObsidianColors = staticCompositionLocalOf { DarkObsidian }

object Obsidian {
    val colors: ObsidianColors
        @Composable @ReadOnlyComposable get() = LocalObsidianColors.current
}

@Composable
fun BedtimeTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkObsidian else LightObsidian
    CompositionLocalProvider(LocalObsidianColors provides colors) {
        MaterialTheme(
            colorScheme = colors.toColorScheme(),
            shapes = ObsidianShapes,
            typography = ObsidianTypography,
            content = content,
        )
    }
}
