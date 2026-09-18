package app.bedtime.service

/**
 * During a session, the few screens that switch blocking off get covered: our page in Settings →
 * Accessibility and its "stop?" dialog, our App info page, and the uninstall prompt.
 *
 * Matching uses our own names ("digital refuge blocker", "digital refuge"), which the phone's language
 * doesn't change, and only inside Settings or the package installer, so no other Settings screen is
 * ever covered.
 */
object SettingsGuard {
    /** Screens that can uninstall the app live here, besides Settings itself. */
    val INSTALLER_PACKAGES = setOf("com.android.packageinstaller", "com.google.android.packageinstaller")

    /** How long the guard stays off after blocking is switched back on, so finishing that isn't covered. */
    const val GRACE_MS = 15_000L

    @Volatile
    var graceUntil = 0L

    /** Whether a window from [pkg] showing [texts] is one of the screens that switch blocking off. */
    fun matches(pkg: String, texts: List<CharSequence>, guardedPackages: Set<String>, names: Set<String>): Boolean {
        if (pkg !in guardedPackages) return false
        return texts.any { text -> names.any { name -> text.toString().contains(name, ignoreCase = true) } }
    }
}
