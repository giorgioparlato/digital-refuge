package app.refugeprobe

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

/** Only here so the probe has a launcher entry and says what to do with it. */
class ProbeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(
            TextView(this).apply {
                text = buildString {
                    appendLine("refuge probe")
                    appendLine()
                    appendLine("1. settings → accessibility → turn ON \"refuge probe\".")
                    appendLine("2. turn OFF \"digital refuge blocker\".")
                    appendLine("3. open the banking app and see whether it names the probe.")
                    appendLine()
                    append("this app does nothing else. uninstall it when the test is over.")
                }
                textSize = 18f
                setPadding(56, 112, 56, 56)
            },
        )
    }
}
