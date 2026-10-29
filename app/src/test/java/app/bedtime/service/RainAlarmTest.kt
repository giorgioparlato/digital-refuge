package app.bedtime.service

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneOffset
import java.time.ZonedDateTime

class RainAlarmTest {
    @Test
    fun laterTodayOrElseTomorrow() {
        val morning = ZonedDateTime.of(2026, 10, 30, 6, 0, 0, 0, ZoneOffset.UTC)
        assertEquals(morning.withHour(7).withMinute(30), RainAlarm.next(morning, 7 * 60 + 30))
        val after = morning.withHour(8)
        assertEquals(after.withHour(7).withMinute(30).plusDays(1), RainAlarm.next(after, 7 * 60 + 30))
    }
}
