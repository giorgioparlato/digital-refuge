package app.bedtime.service

import android.Manifest
import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.ContextCompat
import app.bedtime.data.Repository
import app.bedtime.data.SavedDaltonizer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * System-wide greyscale. Android 15+ does it through an Android mode ([ModeGreyscale]), which only
 * needs Do Not Disturb access; below that there is no way for an app to switch greyscale on.
 *
 * The colour-correction ("daltonizer") route below needs WRITE_SECURE_SETTINGS, which the app no
 * longer declares: banking and ID apps such as BankID refuse to run beside an app that asks for it.
 * The code stays because it works if that permission is ever declared and granted again.
 */
object GreyscaleController {
    private const val KEY_ENABLED = "accessibility_display_daltonizer_enabled"
    private const val KEY_MODE = "accessibility_display_daltonizer"
    private const val MODE_MONOCHROMACY = 0
    private const val MODE_SYSTEM_DEFAULT = 12 // deuteranomaly, AOSP's default when nothing was set

    private val mutex = Mutex()

    /** Settings that change when colour correction is toggled, watched to re-assert greyscale mid-session. */
    val observedUris: List<Uri> get() = listOf(Settings.Secure.getUriFor(KEY_ENABLED), Settings.Secure.getUriFor(KEY_MODE))

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    /** Whether greyscale can be switched on at all (Android 15+ with Do Not Disturb access). */
    fun isAvailable(context: Context): Boolean = hasPermission(context) || ModeGreyscale.isSupported(context)

    /**
     * Turns greyscale on or off. Only ever turns off greyscale that we turned on ourselves.
     * [pausedForHome] shows the user's normal colours on the minimal home screen while a session
     * still wants greyscale everywhere else. Returns true if greyscale had been switched off during a
     * session and was turned back on.
     */
    suspend fun apply(context: Context, wanted: Boolean, pausedForHome: Boolean = false): Boolean {
        if (!hasPermission(context)) {
            ModeGreyscale.apply(context, on = wanted && !pausedForHome)
            return false
        }
        return mutex.withLock { applyLocked(context, wanted, pausedForHome) }
    }

    private suspend fun applyLocked(context: Context, wanted: Boolean, pausedForHome: Boolean): Boolean {
        val resolver = context.contentResolver
        val repo = Repository.get(context)
        val saved = repo.runtime.first().savedDaltonizer
        try {
            if (wanted && pausedForHome) {
                // Keep ownership (saved values) so leaving the home screen turns greyscale straight back on.
                if (saved != null && isOn(resolver)) write(resolver, saved.enabled, saved.mode)
                return false
            }
            if (wanted && !isOn(resolver)) {
                if (saved == null) {
                    val previous = SavedDaltonizer(
                        enabled = Settings.Secure.getInt(resolver, KEY_ENABLED, 0),
                        mode = Settings.Secure.getInt(resolver, KEY_MODE, MODE_SYSTEM_DEFAULT),
                    )
                    repo.updateRuntime { it.copy(savedDaltonizer = previous) }
                }
                write(resolver, enabled = 1, mode = MODE_MONOCHROMACY)
                // We already owned greyscale, so someone switched it off mid-session.
                return saved != null
            }
            if (!wanted && saved != null) {
                write(resolver, saved.enabled, saved.mode)
                repo.updateRuntime { it.copy(savedDaltonizer = null) }
            }
        } catch (_: SecurityException) {
            // Permission was revoked between the check and the write; nothing to do.
        }
        return false
    }

    private fun isOn(resolver: ContentResolver): Boolean =
        Settings.Secure.getInt(resolver, KEY_ENABLED, 0) == 1 &&
            Settings.Secure.getInt(resolver, KEY_MODE, -1) == MODE_MONOCHROMACY

    private fun write(resolver: ContentResolver, enabled: Int, mode: Int) {
        Settings.Secure.putInt(resolver, KEY_MODE, mode)
        Settings.Secure.putInt(resolver, KEY_ENABLED, enabled)
    }
}
