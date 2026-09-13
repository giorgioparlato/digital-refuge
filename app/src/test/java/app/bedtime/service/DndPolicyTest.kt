package app.bedtime.service

import android.app.NotificationManager
import app.bedtime.data.DndMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Sessions must never mute media: music, videos and alarms keep playing whatever the DND level. */
class DndPolicyTest {
    @Test
    fun silenceUsesAlarmsOnlyModeWhichKeepsMediaPlaying() {
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALARMS, DndController.sessionFilter(DndMode.SILENCE))
    }

    @Test
    fun noModeUsesTotalSilence() {
        for (mode in DndMode.entries) {
            assertNotEquals(NotificationManager.INTERRUPTION_FILTER_NONE, DndController.sessionFilter(mode))
        }
    }

    @Test
    fun priorityPolicyAlwaysAllowsMediaAndAlarms() {
        val categories = DndController.keepMediaAndAlarms(0)
        assertTrue((categories and NotificationManager.Policy.PRIORITY_CATEGORY_MEDIA) != 0)
        assertTrue((categories and NotificationManager.Policy.PRIORITY_CATEGORY_ALARMS) != 0)
    }

    @Test
    fun userCategoriesAreKept() {
        val calls = NotificationManager.Policy.PRIORITY_CATEGORY_CALLS
        assertTrue((DndController.keepMediaAndAlarms(calls) and calls) != 0)
    }
}
