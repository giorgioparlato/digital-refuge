package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ShortcutsTest {
    private val sync = AppShortcut("com.ttxapps.dropsync", "sync", "Sync now", "Dropsync")
    private val message = AppShortcut("org.thoughtcrime.securesms", "new", "New message", "Signal")
    private val all = listOf(sync, message)

    @Test
    fun foundByTheirNameOrTheirApps() {
        assertEquals(listOf(sync), Shortcuts.match(all, "sync"))
        assertEquals(listOf(sync), Shortcuts.match(all, "dropsync sync"))
        assertEquals(listOf(message), Shortcuts.match(all, "message"))
        // Not from a letter or two, which would match half of them.
        assertEquals(emptyList<AppShortcut>(), Shortcuts.match(all, "sy"))
    }
}
