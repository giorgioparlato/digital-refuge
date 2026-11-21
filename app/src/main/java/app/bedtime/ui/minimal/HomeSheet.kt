package app.bedtime.ui.minimal

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.bedtime.data.HomeStyle
import app.bedtime.ui.homestyle.palette

/** A sheet from the bottom of the screen, for what a widget's tap opens. A tap beside it or Back puts it away. */
@Composable
internal fun HomeSheetDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(
            Modifier.fillMaxSize().pointerInput(onDismiss) { detectTapGestures { onDismiss() } },
            contentAlignment = Alignment.BottomCenter,
        ) { content() }
    }
}

private val SheetShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)

/** The sheet itself, a shade lifted from the home's background, with rounded top corners. */
@Composable
internal fun HomeSheet(style: HomeStyle, content: @Composable ColumnScope.() -> Unit) {
    val p = style.palette()
    HomeSheet(p.background, p.faint, content)
}

/** The same, from the screen's [background] and [faint] colours. */
@Composable
internal fun HomeSheet(background: Color, faint: Color, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .sheetSurface(background, faint)
            .padding(horizontal = 22.dp)
            .padding(top = 10.dp, bottom = 18.dp),
    ) {
        SheetHandle(faint)
        content()
    }
}

/**
 * What every sheet from the bottom stands on: a shade lifted from the screen's [background], rounded
 * at the top, clear of the gesture bar, and keeping taps to itself rather than letting them reach
 * the space behind, which puts it away.
 */
internal fun Modifier.sheetSurface(background: Color, faint: Color): Modifier {
    val lift = if (background.luminance() > 0.5f) Color(0x14000000) else Color(0x1AFFFFFF)
    return this
        .clip(SheetShape)
        .background(lift.compositeOver(background))
        .border(1.dp, faint.copy(alpha = 0.2f), SheetShape)
        .pointerInput(Unit) { detectTapGestures { } }
        .navigationBarsPadding()
}

/** The small bar at the top of a sheet, saying it came up from below and can go back down. */
@Composable
internal fun SheetHandle(faint: Color) {
    Box(Modifier.fillMaxWidth().padding(bottom = 12.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(50)).background(faint.copy(alpha = 0.4f)))
    }
}
