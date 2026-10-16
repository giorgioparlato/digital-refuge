package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoAndChangelogTest {
    @Test
    fun undoOnlyInTheFirstSeconds() {
        val start = 1_000_000L
        assertTrue(Repository.canUndo(start, start))
        assertTrue(Repository.canUndo(start, start + Repository.UNDO_MS - 1))
        assertFalse(Repository.canUndo(start, start + Repository.UNDO_MS))
        // A clock set back must not open a window that never closes.
        assertFalse(Repository.canUndo(start, start - 1))
    }

    @Test
    fun changelogShowsTheNewestUnseenEntryUpToTheInstalledVersion() {
        val newest = Changelog.entries.maxBy { it.versionCode }
        assertEquals(newest, Changelog.unseen(lastSeen = 0, installed = newest.versionCode))
        assertNull(Changelog.unseen(lastSeen = newest.versionCode, installed = newest.versionCode))
        // An entry written for a version not yet installed stays hidden.
        assertNull(Changelog.unseen(lastSeen = 0, installed = newest.versionCode - 1).takeIf { it == newest })
    }

    @Test
    fun changelogEntriesAreDistinctAndHaveLines() {
        assertEquals(Changelog.entries.size, Changelog.entries.map { it.versionCode }.toSet().size)
        assertTrue(Changelog.entries.all { it.lines.isNotEmpty() })
    }
}
