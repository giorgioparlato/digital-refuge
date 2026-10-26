package app.bedtime.data

import kotlinx.serialization.builtins.ListSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Data saved by v1 of the app (before blocks, Do Not Disturb, etc.) must still load. */
class ModelsCompatTest {
    @Test
    fun v1ScheduleGetsSensibleDefaults() {
        val json = """
            [{"id":"x","name":"Old","enabled":true,"days":[1,2],"startMinute":1320,"endMinute":420,
              "blockedApps":["a"],"greyscale":true,"minimalMode":false,"allowedApps":[],
              "unlock":{"waitEnabled":false,"waitMinutes":10,"textEnabled":true,"textLength":200,
                        "passwordEnabled":false,"passwordHash":null,"passwordSalt":null},
              "unlockAction":{"mode":"END_SESSION","pauseMinutes":15}}]
        """.trimIndent()
        val schedule = AppJson.decodeFromString(ListSerializer(Schedule.serializer()), json).single()
        assertEquals("Old", schedule.name)
        assertEquals(ScheduleKind.RECURRING, schedule.kind)
        assertEquals(DndMode.OFF, schedule.dnd)
        assertEquals(false, schedule.hideNotifications)
        assertTrue(schedule.unlock.escalate)
        // Old data had waitMinutes but no waitSeconds / escalateFactor.
        assertEquals(600, schedule.unlock.waitDurationSeconds)
        assertEquals(1.5f, schedule.unlock.escalateFactor)
    }

    @Test
    fun historyWithoutPausedAtStillLoads() {
        val log = AppJson.decodeFromString(
            ListSerializer(SessionLog.serializer()),
            """[{"scheduleId":"x","name":"Bedtime","start":1,"end":2,"unlockTimes":[3],"endedEarlyAt":3}]""",
        ).single()
        assertTrue(log.pausedAt.isEmpty())
        assertEquals(1, log.escapes)
    }

    @Test
    fun v1RuntimeStateLoads() {
        // pendingWaits and savedDaltonizer are gone from the model; old saves holding them still load.
        val runtime = AppJson.decodeFromString(
            RuntimeState.serializer(),
            """{"overrides":{},"pendingWaits":{"x":{"occurrenceStart":1,"readyAt":2}},"savedDaltonizer":{"enabled":0,"mode":12},
               "activeRuns":{"b":{"start":1,"end":2}}}""",
        )
        assertEquals(Run(1, 2), runtime.activeRuns["b"])
        assertNull(runtime.savedZen)
    }

    @Test
    fun theOneFolderOfFilesFrom098StillCounts() {
        val settings = AppJson.decodeFromString(AppSettings.serializer(), """{"filesFolder":"content://a"}""")
        assertEquals(listOf("content://a"), settings.fileFolders)
        assertEquals(listOf("content://a", "content://b"), settings.copy(filesFolders = listOf("content://a", "content://b")).fileFolders)
    }

    @Test
    fun settingsWithoutLastSeenVersionLoad() {
        val settings = AppJson.decodeFromString(AppSettings.serializer(), """{"onboarded":true}""")
        assertEquals(0, settings.lastSeenVersion)
    }

    @Test
    fun theOldDefaultGreenReadsAsTheSage() {
        assertEquals(DEFAULT_HOME_ACCENT, HomeStyle(accent = 0xFF4EC68E).withCurrentGreen().accent)
        // Any other colour, chosen on purpose, stays.
        assertEquals(0xFF123456, HomeStyle(accent = 0xFF123456).withCurrentGreen().accent)
    }

    @Test
    fun homeStyleWithoutFontsLoadsAsInter() {
        val style = AppJson.decodeFromString(HomeStyle.serializer(), """{"showQuote":true}""")
        assertEquals(AppFont.INTER, style.font)
        assertEquals(AppFont.INTER, style.quoteFont)
        assertEquals(false, style.lockNotifications)
        assertEquals(emptySet<String>(), style.lockNotificationApps)
        assertEquals(DEFAULT_QUICK_REPLIES, style.quickReplies)
    }

    @Test
    fun settingsWithoutQuoteChoicesLoad() {
        val settings = AppJson.decodeFromString(AppSettings.serializer(), """{"onboarded":true}""")
        assertEquals(QuotePrefs(), settings.quotes)
        assertEquals(QuoteSource.ALL, settings.quotes.source)
    }
}
