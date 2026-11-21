package app.bedtime.ui.minimal

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
    val lift = if (background.luminance() > 0.5f) Color(0x14000000) else Color(0x1AFFFFFF)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(SheetShape)
            .background(lift.compositeOver(background))
            .border(1.dp, faint.copy(alpha = 0.3f), SheetShape)
            // Taps on the sheet stay on it, rather than reaching the space behind that puts it away.
            .pointerInput(Unit) { detectTapGestures { } }
            .navigationBarsPadding()
            .padding(horizontal = 22.dp, vertical = 18.dp),
        content = content,
    )
}
