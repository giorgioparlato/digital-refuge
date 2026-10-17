package app.bedtime.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecentsScreensTest {
    @Test
    fun recognisesTheRecentsScreensByName() {
        // As reported on the user's phone, with CClauncher as home screen.
        assertTrue(RecentsScreens.matches("com.android.quickstep.RecentsActivity"))
        assertTrue(RecentsScreens.matches("com.android.systemui.recents.RecentsActivity"))
        assertTrue(RecentsScreens.matches("com.example.launcher.RecentsActivity"))
    }

    @Test
    fun theHomeScreenItselfIsNotRecents() {
        assertFalse(RecentsScreens.matches("app.cclauncher.MainActivity"))
        assertFalse(RecentsScreens.matches("com.google.android.apps.nexuslauncher.NexusLauncherActivity"))
        assertFalse(RecentsScreens.matches(null))
    }
}
