package app.refugeprobe

import android.app.Activity
import android.os.Build
import android.os.Bundle

/** As above, but also asks to be shown over the keyguard, like digital refuge's lock screen. */
class CoverLockActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) setShowWhenLocked(true)
    }
}
