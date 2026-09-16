package app.refugeprobe

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Deliberately does nothing. The point of the probe is only that an accessibility service is
 * enabled, so that a banking app's reaction can be compared with its reaction to digital refuge.
 */
class ProbeService : AccessibilityService() {
    override fun onAccessibilityEvent(event: AccessibilityEvent) = Unit

    override fun onInterrupt() = Unit
}
