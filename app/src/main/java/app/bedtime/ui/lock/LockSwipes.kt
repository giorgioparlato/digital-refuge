package app.bedtime.ui.lock

import app.bedtime.data.Swipe
import kotlin.math.abs

/** What a swipe on the lock screen asks for. */
sealed interface LockSwipe {
    /** Not a swipe at all, or too short to mean anything. */
    data object None : LockSwipe

    /** Any swipe from the bottom, or from the sides, top or a direction with nothing set: just unlock. */
    data object Unlock : LockSwipe

    /** A swipe from the middle, in one of the four directions. */
    data class From(val swipe: Swipe) : LockSwipe
}

/**
 * Reads a swipe that started at ([startX], [startY]) on a screen [width] by [height] and travelled
 * ([dx], [dy]). Only the middle of the screen opens apps: a swipe that starts near the bottom is the
 * way to unlock whichever way it goes, and the edges belong to Android's own gestures.
 */
fun readLockSwipe(startX: Float, startY: Float, width: Float, height: Float, dx: Float, dy: Float, minDistance: Float): LockSwipe {
    if (abs(dx) < minDistance && abs(dy) < minDistance) return LockSwipe.None
    val inMiddle = startX in width * 0.1f..width * 0.9f && startY in height * 0.15f..height * 0.8f
    if (!inMiddle) return LockSwipe.Unlock
    return LockSwipe.From(
        if (abs(dx) > abs(dy)) {
            if (dx > 0) Swipe.RIGHT else Swipe.LEFT
        } else {
            if (dy > 0) Swipe.DOWN else Swipe.UP
        },
    )
}
