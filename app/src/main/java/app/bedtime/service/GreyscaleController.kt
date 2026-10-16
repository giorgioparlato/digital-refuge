package app.bedtime.service

import android.content.Context

/**
 * System-wide greyscale. Android 15+ does it through an Android mode ([ModeGreyscale]), which only
 * needs Do Not Disturb access; below that there is no way for an app to switch greyscale on.
 *
 * There used to be a colour-correction route as well, but it needed WRITE_SECURE_SETTINGS granted
 * over adb, which the app stopped declaring in v1.4. It lives in git history (before v0.7.9).
 */
object GreyscaleController {
    /** Whether greyscale can be switched on at all (Android 15+ with Do Not Disturb access). */
    fun isAvailable(context: Context): Boolean = ModeGreyscale.isSupported(context)

    /**
     * Turns greyscale on or off. [pausedForHome] shows the user's normal colours on the minimal home
     * screen while a session still wants greyscale everywhere else.
     */
    fun apply(context: Context, wanted: Boolean, pausedForHome: Boolean = false) {
        ModeGreyscale.apply(context, on = wanted && !pausedForHome)
    }
}
