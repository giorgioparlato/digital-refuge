package app.bedtime.ui.theme

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import app.bedtime.R
import app.bedtime.data.AppFont
import java.io.File

/**
 * The minimal home's fonts. Four ship with the app — subset to Latin and cut to the weights used,
 * about 550 KB together (licences in assets/licenses) — and one is whatever font file you add.
 */
object AppFonts {
    private val nunito = FontFamily(Font(R.font.nunito_400), Font(R.font.nunito_600, FontWeight.SemiBold))
    private val literata = FontFamily(
        Font(R.font.literata_400),
        Font(R.font.literata_600, FontWeight.SemiBold),
        Font(R.font.literata_italic, style = FontStyle.Italic),
    )
    private val fraunces = FontFamily(
        Font(R.font.fraunces_400),
        Font(R.font.fraunces_600, FontWeight.SemiBold),
        Font(R.font.fraunces_italic, style = FontStyle.Italic),
    )
    private val spectral = FontFamily(Font(R.font.spectral_400), Font(R.font.spectral_italic, style = FontStyle.Italic))

    /** What each is called where you choose it. */
    fun label(font: AppFont, customName: String?): String = when (font) {
        AppFont.INTER -> "Inter"
        AppFont.NUNITO -> "Nunito"
        AppFont.LITERATA -> "Literata"
        AppFont.FRAUNCES -> "Fraunces"
        AppFont.SPECTRAL -> "Spectral"
        AppFont.CUSTOM -> customName ?: "Your font"
    }

    /** Serifs set a quote in italics; sans-serifs and your own font stay upright, as Inter always has. */
    fun quoteStyle(font: AppFont): FontStyle =
        if (font == AppFont.LITERATA || font == AppFont.FRAUNCES || font == AppFont.SPECTRAL) FontStyle.Italic else FontStyle.Normal

    /** The family for [font]; your own font falls back to Inter if its file is gone or unreadable. */
    fun family(context: Context, font: AppFont): FontFamily = when (font) {
        AppFont.INTER -> Inter
        AppFont.NUNITO -> nunito
        AppFont.LITERATA -> literata
        AppFont.FRAUNCES -> fraunces
        AppFont.SPECTRAL -> spectral
        AppFont.CUSTOM -> customFile(context).takeIf { it.exists() }?.let { FontFamily(Font(it)) } ?: Inter
    }

    private fun customFile(context: Context) = File(File(context.filesDir, "fonts"), "custom")

    /**
     * Copies the font at [uri] into the app's own storage, replacing any earlier one, and returns its
     * name to show — or null if it isn't a font Android can read. Run off the main thread.
     */
    fun importCustom(context: Context, uri: Uri): String? {
        val target = customFile(context)
        val incoming = File(target.parentFile, "incoming")
        return runCatching {
            target.parentFile?.mkdirs()
            context.contentResolver.openInputStream(uri)?.use { input -> incoming.outputStream().use { input.copyTo(it) } }
                ?: return null
            // A file Android can't make a typeface from would only ever show as Inter: refuse it now.
            Typeface.createFromFile(incoming)
            incoming.renameTo(target) || run { incoming.copyTo(target, overwrite = true); incoming.delete() }
            displayName(context, uri)
        }.getOrElse {
            incoming.delete()
            null
        }
    }

    private fun displayName(context: Context, uri: Uri): String {
        val raw = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        }.getOrNull() ?: "Your font"
        return raw.substringBeforeLast('.').replace('-', ' ').replace('_', ' ').take(28)
    }
}

/** [content] with every text style set in [family]; sizes and weights stay as they are. */
@Composable
fun WithFontFamily(family: FontFamily, content: @Composable () -> Unit) {
    val base = MaterialTheme.typography
    val typography = remember(base, family) { base.withFamily(family) }
    MaterialTheme(colorScheme = MaterialTheme.colorScheme, typography = typography, shapes = MaterialTheme.shapes, content = content)
}

private fun Typography.withFamily(family: FontFamily): Typography {
    fun TextStyle.f() = copy(fontFamily = family)
    return Typography(
        displayLarge.f(), displayMedium.f(), displaySmall.f(),
        headlineLarge.f(), headlineMedium.f(), headlineSmall.f(),
        titleLarge.f(), titleMedium.f(), titleSmall.f(),
        bodyLarge.f(), bodyMedium.f(), bodySmall.f(),
        labelLarge.f(), labelMedium.f(), labelSmall.f(),
    )
}
