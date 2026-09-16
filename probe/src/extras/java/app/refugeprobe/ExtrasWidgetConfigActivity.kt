package app.refugeprobe

import android.app.Activity
import android.os.Bundle

/** The widget's configuration screen, as digital refuge has. Closes immediately. */
class ExtrasWidgetConfigActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        finish()
    }
}
