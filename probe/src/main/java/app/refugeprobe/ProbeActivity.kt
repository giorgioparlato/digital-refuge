package app.refugeprobe

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

/** Says what to do with this probe; the overlay variant also shows and hides a real overlay. */
class ProbeActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(56, 112, 56, 56)
        }
        column.addView(
            TextView(this).apply {
                text = if (canOverlay()) overlaySteps() else serviceSteps()
                textSize = 18f
            },
        )
        if (canOverlay()) {
            column.addView(button("1 · allow display over other apps") {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            })
            column.addView(button("2 · show the overlay") { showOverlay() })
            column.addView(button("remove the overlay") { removeOverlay() })
        }
        setContentView(ScrollView(this).apply { addView(column) })
    }

    private fun serviceSteps() = buildString {
        appendLine("refuge probe")
        appendLine(packageName)
        appendLine()
        appendLine("1. settings → accessibility → turn ON this probe's service.")
        appendLine("2. turn OFF \"digital refuge blocker\".")
        appendLine("3. open the banking app and see whether it names the probe.")
        appendLine()
        append("this app does nothing else. uninstall it when the test is over.")
    }

    private fun overlaySteps() = buildString {
        appendLine("probe overlay")
        appendLine(packageName)
        appendLine()
        appendLine("this probe has no accessibility service. it only draws over other apps.")
        appendLine()
        appendLine("first switch OFF \"digital refuge blocker\" in settings → accessibility, so only this probe is in play.")
        appendLine("1. allow display over other apps, below.")
        appendLine("   open BankID and Chase: do they complain?")
        appendLine("2. show the overlay (a small pill at the top).")
        appendLine("   open BankID and Chase again: do they complain now?")
        appendLine()
        append("also note whether Play Protect warns about this probe at any point.")
    }

    private fun button(label: String, onClick: () -> Unit) = Button(this).apply {
        text = label
        setOnClickListener { onClick() }
    }

    private fun canOverlay(): Boolean =
        runCatching {
            packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
                .requestedPermissions?.contains(Manifest.permission.SYSTEM_ALERT_WINDOW) == true
        }.getOrDefault(false)

    private fun showOverlay() {
        if (!Settings.canDrawOverlays(this) || overlay != null) return
        val pill = TextView(applicationContext).apply {
            text = "probe overlay"
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.argb(200, 20, 110, 70))
            setPadding(24, 8, 24, 8)
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT,
        ).apply { gravity = Gravity.TOP or Gravity.END }
        applicationContext.getSystemService(WindowManager::class.java).addView(pill, params)
        overlay = pill
    }

    private fun removeOverlay() {
        val pill = overlay ?: return
        runCatching { applicationContext.getSystemService(WindowManager::class.java).removeView(pill) }
        overlay = null
    }

    companion object {
        /** Kept outside the activity so the overlay stays up while the banking apps are opened. */
        private var overlay: TextView? = null
    }
}
