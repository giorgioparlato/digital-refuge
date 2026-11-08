package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BackupTest {
    private val backup = Backup(
        exportedAt = 1_700_000_000_000,
        schedules = listOf(
            Schedule(id = "bedtime", name = "Bedtime", blockedApps = setOf("com.instagram.android"), minimalMode = true),
            Schedule(id = "focus", name = "Focus", kind = ScheduleKind.BLOCK, durationMinutes = 90, dnd = DndMode.PRIORITY),
        ),
        settings = AppSettings(
            homeStyle = HomeStyle(background = 0xFF101010, appNameSize = AppNameSize.VERY_SMALL),
            groups = listOf(AppGroup(id = "g1", name = "Social", packages = setOf("com.zhiliaoapp.musically"))),
            alwaysAvailable = setOf("com.google.android.apps.maps"),
            widgetBlocks = mapOf("7" to "focus"),
            journalOn = true,
            journalSections = Journal.withSection(Journal.SUGGESTED, "Dreams"),
        ),
        journal = listOf(JournalEntry(20_000L, "**What drained me?**\nmeetings", listOf("Trajectory"), 5L)),
        history = listOf(SessionLog("bedtime", "Bedtime", 1L, 2L, unlockTimes = listOf(3L))),
        usage = listOf(UsageDay(20_000L, hourlyAll = mapOf(9 to 600L), unlocks = 12)),
        usageMonths = listOf(UsageMonth("2026-08", total = 36_000L, days = 31)),
    )

    @Test
    fun survivesARoundTrip() {
        assertEquals(backup, Backup.decode(Backup.encode(backup)))
    }

    @Test
    fun fileFromAnotherVersionStillLoadsIfOlder() {
        val withExtras = """{"version":1,"exportedAt":5,"schedules":[],"settings":{},"history":[],"somethingNew":true}"""
        val loaded = Backup.decode(withExtras)
        assertNotNull(loaded)
        assertEquals(5L, loaded!!.exportedAt)
    }

    @Test
    fun fileFromANewerVersionIsRefused() {
        assertNull(Backup.decode("""{"version":99,"exportedAt":5,"schedules":[],"settings":{},"history":[]}"""))
    }

    @Test
    fun brokenFileIsRefused() {
        assertNull(Backup.decode("this is not a backup"))
    }

    @Test
    fun aSealedBackupOpensWithItsPassphrase() {
        val key = BackupSeal.key("correct horse", rounds = 1_000)
        val sealed = BackupSeal.seal(Backup.encode(backup), key)
        assertTrue(BackupSeal.isSealed(sealed))
        assertFalse("meetings" in sealed)
        assertEquals(backup, Backup.decode(BackupSeal.open(sealed, "correct horse")!!))
    }

    @Test
    fun aSealedBackupStaysShutToAnyOtherPassphrase() {
        val sealed = BackupSeal.seal(Backup.encode(backup), BackupSeal.key("correct horse", rounds = 1_000))
        assertNull(BackupSeal.open(sealed, "Correct horse"))
        assertNull(BackupSeal.open(sealed, ""))
    }

    @Test
    fun aSealedFileIsNeverReadAsAnEmptyBackup() {
        val sealed = BackupSeal.seal(Backup.encode(backup), BackupSeal.key("correct horse", rounds = 1_000))
        assertNull(Backup.decode(sealed))
    }

    @Test
    fun aPlainBackupIsNotSealed() {
        assertFalse(BackupSeal.isSealed(Backup.encode(backup)))
        assertNull(BackupSeal.open(Backup.encode(backup), "correct horse"))
    }

    @Test
    fun eachFileIsSealedAfresh() {
        val key = BackupSeal.key("correct horse", rounds = 1_000)
        val json = Backup.encode(backup)
        assertNotEquals(BackupSeal.seal(json, key), BackupSeal.seal(json, key))
    }

    @Test
    fun theJournalStaysOutOfBackupsUnlessAskedFor() {
        assertFalse(AppSettings().backupJournal)
    }

    @Test
    fun aRestoredDayGivesWayToTheOneCountedHere() {
        val file = listOf(UsageDay(1L, unlocks = 3), UsageDay(2L, unlocks = 5))
        val here = listOf(UsageDay(2L, unlocks = 9), UsageDay(3L, unlocks = 1))
        assertEquals(listOf(3, 9, 1), Repository.merged(file, here).map { it.unlocks })
    }
}
