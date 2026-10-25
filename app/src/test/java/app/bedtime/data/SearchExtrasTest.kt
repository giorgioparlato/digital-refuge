package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchExtrasTest {
    @Test
    fun alarmsReadTheTimeAndWhatFollows() {
        assertEquals(ClockAction.Alarm(7, 30, ""), ClockActions.parse("alarm 7:30"))
        assertEquals(ClockAction.Alarm(6, 0, ""), ClockActions.parse("wake me up at 6"))
        assertEquals(ClockAction.Alarm(19, 15, "gym"), ClockActions.parse("Alarm 7.15pm gym"))
        assertEquals(ClockAction.Alarm(0, 0, ""), ClockActions.parse("alarm 12am"))
        assertNull(ClockActions.parse("alarm 25:00"))
        assertNull(ClockActions.parse("alarm clock"))
        assertNull(ClockActions.parse("alarmed 7"))
    }

    @Test
    fun timersAddUpTheirParts() {
        assertEquals(ClockAction.Timer(600, ""), ClockActions.parse("timer 10"))
        assertEquals(ClockAction.Timer(600, "tea"), ClockActions.parse("timer 10 min tea"))
        assertEquals(ClockAction.Timer(5400, ""), ClockActions.parse("timer for 1h 30"))
        assertEquals(ClockAction.Timer(330, ""), ClockActions.parse("timer 5 min 30"))
        assertEquals(ClockAction.Timer(90, "eggs"), ClockActions.parse("timer 90s eggs"))
        assertNull(ClockActions.parse("timer tea"))
        assertEquals("start a 1 h 30 min timer", ClockActions.label(ClockAction.Timer(5400, "")) { _, _ -> "" })
    }

    @Test
    fun settingsPagesByNameOrOtherWords() {
        assertEquals("Wi-Fi", SettingsPages.match("wifi").first().name)
        assertEquals("Battery", SettingsPages.match("bat").first().name)
        assertTrue(SettingsPages.match("brightness").any { it.name == "Display" })
        // Two letters aren't enough, and an app's name isn't a setting.
        assertTrue(SettingsPages.match("wi").isEmpty())
        assertTrue(SettingsPages.match("pinterest").isEmpty())
    }

    @Test
    fun filesByNameStartingFirst() {
        val files = listOf("Groceries.md", "Weekly review.md", "Review notes.md", "Ideas.md").map { IndexedFile(it, it, "content://$it", "text/markdown") }
        assertEquals(listOf("Review notes", "Weekly review"), FolderIndex.match(files, "review", name = FolderIndex::title).map(FolderIndex::title))
        assertTrue(FolderIndex.match(files, "r").isEmpty())
    }
}
