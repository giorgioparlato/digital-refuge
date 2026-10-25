package app.bedtime.ui.lock

import app.bedtime.data.Swipe
import kotlin.math.abs

/** What a swipe on the lock screen asks for. */
sealed interface LockSwipe {
    /** Not a swipe at all, too short to mean anything, or in the middle, which belongs to the notifications. */
    data object None : LockSwipe

    /** Any swipe from the bottom, or from the sides, or a direction with nothing set: just unlock. */
    data object Unlock : LockSwipe

    /** A pull down from the very top: Android's notification shade, as on any lock screen. */
    data object Shade : LockSwipe

    /** A swipe from the top, around the clock, in one of the four directions. */
    data class From(val swipe: Swipe) : LockSwipe
}

/** Where the top, which opens apps, ends; the middle, the notifications', runs from here to [BOTTOM_STARTS]. */
internal const val TOP_ENDS = 0.35f
internal const val BOTTOM_STARTS = 0.75f

/**
 * Reads a swipe that started at ([startX], [startY]) on a screen [width] by [height] and travelled
 * ([dx], [dy]). The top, around the clock, opens apps; the middle is for swiping notifications away,
 * so nothing else happens there; a swipe from the bottom is the way to unlock whichever way it goes.
 * A pull down from the very top brings Android's notification shade; the sides unlock too.
 */
fun readLockSwipe(startX: Float, startY: Float, width: Float, height: Float, dx: Float, dy: Float, minDistance: Float): LockSwipe {
    if (abs(dx) < minDistance && abs(dy) < minDistance) return LockSwipe.None
    if (startY < height * 0.05f && dy > abs(dx)) return LockSwipe.Shade
    val onEdge = startX !in width * 0.1f..width * 0.9f || startY < height * 0.05f
    return when {
        onEdge || startY >= height * BOTTOM_STARTS -> LockSwipe.Unlock
        startY >= height * TOP_ENDS -> LockSwipe.None
        else -> LockSwipe.From(
            if (abs(dx) > abs(dy)) {
                if (dx > 0) Swipe.RIGHT else Swipe.LEFT
            } else {
                if (dy > 0) Swipe.DOWN else Swipe.UP
            },
        )
    }
}
