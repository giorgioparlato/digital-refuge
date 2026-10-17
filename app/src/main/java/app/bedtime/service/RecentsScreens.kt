package app.bedtime.service

/**
 * The recent-apps view, recognised by its screen's name alone. On a phone whose home screen is a
 * third-party launcher, Android still draws recents itself, as a screen of its own: there, minimal
 * mode lets it through, so stuck or unused apps can be closed. Anything opened from it follows the
 * usual rules. Where recents is part of the home screen itself (the Pixel launcher, for one) there's
 * no separate screen to recognise, and it stays as it was.
 */
object RecentsScreens {
    private val known = setOf(
        "com.android.quickstep.RecentsActivity",
        "com.android.systemui.recents.RecentsActivity",
        "com.android.launcher3.uioverrides.QuickstepLauncher\$RecentsActivity",
    )

    fun matches(className: String?): Boolean =
        className != null && (className in known || className.endsWith(".RecentsActivity"))
}
