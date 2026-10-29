package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class AutoBackupTest {
    @Test
    fun onlyItsOwnOlderFilesGo() {
        val names = listOf(
            "digital-refuge-auto-2026-10-01.json",
            "digital-refuge-auto-2026-10-08.json",
            "digital-refuge-auto-2026-10-15.json",
            "digital-refuge-auto-2026-10-22.json",
            "digital-refuge-auto-2026-10-29.json",
            // Never touched: an export you made yourself, and anything else.
            "digital-refuge-2026-09-01.json",
            "notes.md",
        )
        assertEquals(listOf("digital-refuge-auto-2026-10-01.json"), AutoBackup.toDelete(names))
    }

    @Test
    fun nothingGoesWhileThereAreFewEnough() {
        assertEquals(emptyList<String>(), AutoBackup.toDelete(listOf(AutoBackup.fileName(LocalDate.of(2026, 10, 29)))))
    }

    @Test
    fun dueOnceAWeekAndOnlyWithAFolder() {
        val week = AutoBackup.EVERY_MS
        assertFalse(AutoBackup.due(AppSettings(), now = 10 * week))
        val set = AppSettings(backupFolder = "content://x", lastBackupAt = 10 * week)
        assertFalse(AutoBackup.due(set, now = 10 * week + week - 1))
        assertTrue(AutoBackup.due(set, now = 11 * week))
    }
}
