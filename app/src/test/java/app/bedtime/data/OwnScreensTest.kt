package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Test

class OwnScreensTest {
    private val own = "app.bedtime"

    @Test
    fun refugesHomeAndLockScreensAreMarkedTheRestIsRefuge() {
        assertEquals("app.bedtime#home", OwnScreens.marked(own, "app.bedtime.ui.minimal.MinimalHomeActivity", own))
        assertEquals("app.bedtime#home", OwnScreens.marked(own, "app.bedtime.ui.lock.LockScreenActivity", own))
        assertEquals("app.bedtime#home", OwnScreens.marked(own, "app.bedtime.Home", own))
        assertEquals(own, OwnScreens.marked(own, "app.bedtime.MainActivity", own))
        assertEquals(own, OwnScreens.marked(own, null, own))
        // Another app's class of the same name is that app's.
        assertEquals("com.other", OwnScreens.marked("com.other", "app.bedtime.ui.lock.LockScreenActivity", own))
    }
}
