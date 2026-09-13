package app.bedtime.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Small custom icons that aren't in material-icons-core (24×24 viewport, tinted by Icon). */
object BedtimeIcons {
    /** The app's mark: a shelter with an arched doorway (same shape as the launcher icon). */
    val Refuge: ImageVector by lazy {
        icon("Refuge") {
            path("M12,2.5 L22,11 L19.5,11 L19.5,21 L4.5,21 L4.5,11 L2,11 Z M12,6.2 L17.5,10.9 L17.5,19 L6.5,19 L6.5,10.9 Z", evenOdd = true)
            path("M9.5,19 L9.5,15 A2.5,2.5 0 0,1 14.5,15 L14.5,19 Z")
        }
    }

    val Moon: ImageVector by lazy {
        icon("Moon") { path("M11.21,3.04 A9,9 0 1,0 20.38,15.27 A7.65,7.65 0 1,1 11.21,3.04 Z") }
    }

    /** Half-filled circle, the usual "contrast / greyscale" symbol. */
    val Contrast: ImageVector by lazy {
        icon("Contrast") {
            path(RING, evenOdd = true)
            path("M12,4.5 A7.5,7.5 0 0,0 12,19.5 Z")
        }
    }

    /** Circle with a slash. */
    val Block: ImageVector by lazy {
        icon("Block") {
            path(RING, evenOdd = true)
            path("M5.99,7.4 L7.4,5.99 L18.01,16.6 L16.6,18.01 Z")
        }
    }

    val Hourglass: ImageVector by lazy {
        icon("Hourglass") {
            path("M6,2 H18 V6 C18,8.5 16,10.5 13.5,12 C16,13.5 18,15.5 18,18 V22 H6 V18 C6,15.5 8,13.5 10.5,12 C8,10.5 6,8.5 6,6 Z")
        }
    }

    val Grid: ImageVector by lazy {
        icon("Grid") { path("M4,4 H10 V10 H4 Z M14,4 H20 V10 H14 Z M4,14 H10 V20 H4 Z M14,14 H20 V20 H14 Z") }
    }

    /** Concentric rings, for focus. */
    val Target: ImageVector by lazy {
        icon("Target") {
            path(
                "M12,2 A10,10 0 1,1 12,22 A10,10 0 1,1 12,2 Z M12,4 A8,8 0 1,0 12,20 A8,8 0 1,0 12,4 Z " +
                    "M12,7 A5,5 0 1,1 12,17 A5,5 0 1,1 12,7 Z M12,9 A3,3 0 1,0 12,15 A3,3 0 1,0 12,9 Z " +
                    "M12,10.5 A1.5,1.5 0 1,1 12,13.5 A1.5,1.5 0 1,1 12,10.5 Z",
                evenOdd = true,
            )
        }
    }

    val Sun: ImageVector by lazy {
        icon("Sun") {
            path("M12,7 A5,5 0 1,1 12,17 A5,5 0 1,1 12,7 Z")
            path(
                "M11,1 H13 V4 H11 Z M11,20 H13 V23 H11 Z M1,11 H4 V13 H1 Z M20,11 H23 V13 H20 Z " +
                    "M4.22,5.64 L5.64,4.22 L7.76,6.34 L6.34,7.76 Z M16.24,17.66 L17.66,16.24 L19.78,18.36 L18.36,19.78 Z " +
                    "M16.24,6.34 L18.36,4.22 L19.78,5.64 L17.66,7.76 Z M4.22,18.36 L6.34,16.24 L7.76,17.66 L5.64,19.78 Z",
            )
        }
    }

    val Leaf: ImageVector by lazy {
        icon("Leaf") { path("M5,20 C4,11 10,4 20,4 C20,14 13,20 5,20 Z") }
    }

    val Headphones: ImageVector by lazy {
        icon("Headphones") {
            path("M12,3 A9,9 0 0,0 3,12 V19 A2,2 0 0,0 5,21 H8 V14 H5 V12 A7,7 0 0,1 19,12 V14 H16 V21 H19 A2,2 0 0,0 21,19 V12 A9,9 0 0,0 12,3 Z")
        }
    }

    val Book: ImageVector by lazy {
        icon("Book") {
            path("M3,5 H9 A3,3 0 0,1 11.2,6 V20 A2.5,2.5 0 0,0 9,19 H3 Z M21,5 H15 A3,3 0 0,0 12.8,6 V20 A2.5,2.5 0 0,1 15,19 H21 Z")
        }
    }

    private const val RING = "M12,2 A10,10 0 1,1 12,22 A10,10 0 1,1 12,2 Z M12,4.5 A7.5,7.5 0 1,0 12,19.5 A7.5,7.5 0 1,0 12,4.5 Z"

    private fun icon(name: String, block: ImageVector.Builder.() -> Unit): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply(block).build()

    private fun ImageVector.Builder.path(pathData: String, evenOdd: Boolean = false) {
        addPath(
            pathData = addPathNodes(pathData),
            pathFillType = if (evenOdd) PathFillType.EvenOdd else PathFillType.NonZero,
            fill = SolidColor(Color.Black),
        )
    }
}
