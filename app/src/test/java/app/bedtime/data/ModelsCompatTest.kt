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
        val runtime = AppJson.decodeFromString(
            RuntimeState.serializer(),
            """{"overrides":{},"pendingWaits":{},"savedDaltonizer":{"enabled":0,"mode":12}}""",
        )
        assertTrue(runtime.activeRuns.isEmpty())
        assertNull(runtime.savedZen)
        assertEquals(SavedDaltonizer(0, 12), runtime.savedDaltonizer)
    }
}
