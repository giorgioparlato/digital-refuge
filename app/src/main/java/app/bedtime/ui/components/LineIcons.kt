package app.bedtime.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The app's line icons: one family, a 1.6 stroke with round ends on a 24×24 viewport, tinted by Icon.
 * They stand in for material-icons-core's filled ones, named the same, so `LineIcons.Edit`
 * becomes `LineIcons.Edit`. Filled icons that carry a state (a filled heart or star, play) stay
 * material, because the fill is the meaning.
 */
object LineIcons {
    /** The pen stays filled (your choice, 2026-11-21): it's refuge's mark for notes, everywhere they appear. */
    val Edit by lazy { filled("Edit", "M3,17.25 V21 H6.75 L17.81,9.94 L14.06,6.19 L3,17.25 Z M20.71,7.04 C21.1,6.65 21.1,6.02 20.71,5.63 L18.37,3.29 C17.98,2.9 17.35,2.9 16.96,3.29 L15.13,5.12 L18.88,8.87 L20.71,7.04 Z") }
    val Create: ImageVector get() = Edit
    val Close by lazy { line("Close", "M6,6 L18,18 M18,6 L6,18") }
    val Clear: ImageVector get() = Close
    val Lock by lazy { line("Lock", "M7,11 H17 A2,2 0 0,1 19,13 V18 A2,2 0 0,1 17,20 H7 A2,2 0 0,1 5,18 V13 A2,2 0 0,1 7,11 Z M8,11 V8 A4,4 0 0,1 16,8 V11") }
    val DateRange by lazy { line("DateRange", "M6,5 H18 A2,2 0 0,1 20,7 V18 A2,2 0 0,1 18,20 H6 A2,2 0 0,1 4,18 V7 A2,2 0 0,1 6,5 Z M4,10 H20 M9,3 V7 M15,3 V7") }
    val Check by lazy { line("Check", "M5,12.5 L9.5,17 L19,7.5") }
    val Add by lazy { line("Add", "M12,5 V19 M5,12 H19") }
    val Info by lazy { line("Info", "M12,3.5 A8.5,8.5 0 1,1 12,20.5 A8.5,8.5 0 1,1 12,3.5 Z M12,11 V16 M12,8 V8.2") }
    val Notifications by lazy { line("Notifications", "M6,16 V11 A6,6 0 0,1 18,11 V16 L19.5,18 H4.5 Z M10,20.5 A2,2 0 0,0 14,20.5") }
    val Refresh by lazy { line("Refresh", "M19,12 A7,7 0 1,1 16.95,7.05 M19,4 V8 H15") }
    val KeyboardArrowUp by lazy { line("KeyboardArrowUp", "M6,15 L12,9 L18,15") }
    val KeyboardArrowDown by lazy { line("KeyboardArrowDown", "M6,9 L12,15 L18,9") }
    val KeyboardArrowRight by lazy { line("KeyboardArrowRight", "M9,6 L15,12 L9,18", mirrored = true) }
    val KeyboardArrowLeft by lazy { line("KeyboardArrowLeft", "M15,6 L9,12 L15,18", mirrored = true) }
    val ArrowBack by lazy { line("ArrowBack", "M19,12 H5 M11,6 L5,12 L11,18", mirrored = true) }
    val Search by lazy { line("Search", "M11,5 A6,6 0 1,1 11,17 A6,6 0 1,1 11,5 Z M20,20 L15.5,15.5") }
    val Home by lazy { line("Home", "M4,11 L12,4 L20,11 V20 H4 Z M10,20 V15 H14 V20") }
    val CheckCircle by lazy { line("CheckCircle", "M12,4 A8,8 0 1,1 12,20 A8,8 0 1,1 12,4 Z M8.5,12.5 L11,15 L15.5,10") }
    val Warning by lazy { line("Warning", "M12,4 L21,20 H3 Z M12,10 V14 M12,17 V17.2") }
    /** Sliders rather than a cog: the app's settings are choices you set, not machinery. */
    val Settings by lazy { line("Settings", "M4,7 H14 M18,7 H20 M4,17 H8 M12,17 H20 M16,5 A2,2 0 1,1 16,9 A2,2 0 1,1 16,5 Z M10,15 A2,2 0 1,1 10,19 A2,2 0 1,1 10,15 Z") }
    val Share by lazy { line("Share", "M18,2.5 A2.5,2.5 0 1,1 18,7.5 A2.5,2.5 0 1,1 18,2.5 Z M6,9.5 A2.5,2.5 0 1,1 6,14.5 A2.5,2.5 0 1,1 6,9.5 Z M18,16.5 A2.5,2.5 0 1,1 18,21.5 A2.5,2.5 0 1,1 18,16.5 Z M8.2,10.8 L15.8,6.2 M8.2,13.2 L15.8,17.8") }
    val Delete by lazy { line("Delete", "M4,7 H20 M10,11 V17 M14,11 V17 M6,7 L7,20 H17 L18,7 M9,7 V4 H15 V7") }
    val Person by lazy { line("Person", "M12,4 A4,4 0 1,1 12,12 A4,4 0 1,1 12,4 Z M4.5,20 A7.5,6.5 0 0,1 19.5,20") }
    val Call by lazy { line("Call", "M5,4 H9 L11,9 L8.5,10.5 A11,11 0 0,0 13.5,15.5 L15,13 L20,15 V19 A1,1 0 0,1 19,20 A16,16 0 0,1 4,5 A1,1 0 0,1 5,4 Z") }
    val Phone: ImageVector get() = Call
    val Build by lazy { line("Build", "M14.5,4 A4.5,4.5 0 0,0 10.3,10.2 L4,16.5 L7.5,20 L13.8,13.7 A4.5,4.5 0 0,0 20,9.5 L17,12.5 L13.5,11 L12.5,7.5 L15.5,4.5") }
    val Undo by lazy { line("Undo", "M9,14 L4,9 L9,4 M4,9 H15 A5,5 0 0,1 15,19 H12", mirrored = true) }
    val Redo by lazy { line("Redo", "M15,14 L20,9 L15,4 M20,9 H9 A5,5 0 0,0 9,19 H12", mirrored = true) }
    /** A ticked box, for a to-do line. */
    val Todo by lazy { line("Todo", "M8,4 H16 A4,4 0 0,1 20,8 V16 A4,4 0 0,1 16,20 H8 A4,4 0 0,1 4,16 V8 A4,4 0 0,1 8,4 Z M8.5,12.5 L11,15 L15.5,10") }
    val Indent by lazy { line("Indent", "M4,6 H20 M11,12 H20 M4,18 H20 M4,9.5 L7,12 L4,14.5", mirrored = true) }
    val Outdent by lazy { line("Outdent", "M4,6 H20 M11,12 H20 M4,18 H20 M7,9.5 L4,12 L7,14.5", mirrored = true) }
    /** Four bars of different heights, for insights. */
    val Insights by lazy { line("Insights", "M5,20 V12 M10,20 V6 M15,20 V14 M20,20 V9") }
    val Menu by lazy { line("Menu", "M4,7 H20 M4,12 H20 M4,17 H20") }
    val LocationOn by lazy { line("LocationOn", "M12,21 C12,21 5,14.5 5,9.5 A7,7 0 0,1 19,9.5 C19,14.5 12,21 12,21 Z M12,7 A2.5,2.5 0 1,1 12,12 A2.5,2.5 0 1,1 12,7 Z") }
    val List by lazy { line("List", "M9,7 H20 M9,12 H20 M9,17 H20 M4.5,7 V7.1 M4.5,12 V12.1 M4.5,17 V17.1", mirrored = true) }

    private fun filled(name: String, pathData: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
            addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black))
        }.build()

    private fun line(name: String, pathData: String, mirrored: Boolean = false): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f, autoMirror = mirrored).apply {
            addPath(
                pathData = addPathNodes(pathData),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.6f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }.build()
}
