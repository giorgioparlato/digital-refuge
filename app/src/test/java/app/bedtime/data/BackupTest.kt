package app.bedtime.data

import org.junit.Assert.assertEquals
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
    fun aRestoredDayGivesWayToTheOneCountedHere() {
        val file = listOf(UsageDay(1L, unlocks = 3), UsageDay(2L, unlocks = 5))
        val here = listOf(UsageDay(2L, unlocks = 9), UsageDay(3L, unlocks = 1))
        assertEquals(listOf(3, 9, 1), Repository.merged(file, here).map { it.unlocks })
    }
}
