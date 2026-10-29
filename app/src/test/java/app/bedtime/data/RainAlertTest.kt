package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class RainAlertTest {
    private val zone = ZoneOffset.UTC
    private val day = LocalDate.of(2026, 10, 30)
    private fun at(hour: Int) = day.atTime(hour, 0).toInstant(zone).toEpochMilli()
    private val morning = at(7) + 30 * 60_000L
    private fun weather(vararg chances: Pair<Int, Int>, daily: Int? = null) =
        WeatherNow(285.0, at = at(6), rainChance = daily, rainHours = chances.map { (h, c) -> RainHour(at(h), c) })

    @Test
    fun thePeakAndTheHoursAroundIt() {
        val w = weather(9 to 5, 13 to 20, 14 to 50, 15 to 60, 16 to 45, 17 to 10)
        val outlook = RainAlert.outlook(w, morning, zone, threshold = 10)
        assertEquals(RainOutlook(60, 14, 17), outlook)
        assertEquals("60% chance of rain today, mostly between 14:00 and 17:00", RainAlert.message(outlook!!) { "%d:00".format(it) })
    }

    @Test
    fun oneHourSaysAround() {
        val outlook = RainAlert.outlook(weather(10 to 5, 11 to 40, 12 to 5), morning, zone, threshold = 10)!!
        assertEquals("40% chance of rain today, mostly around 11:00", RainAlert.message(outlook) { "%d:00".format(it) })
    }

    @Test
    fun quietBelowTheThresholdOrWhenStale() {
        assertNull(RainAlert.outlook(weather(14 to 8), morning, zone, threshold = 10))
        assertNull(RainAlert.outlook(weather(14 to 80).copy(at = morning - RainAlert.STALE_MS - 1), morning, zone, threshold = 10))
        // Tomorrow's rain isn't today's.
        assertNull(RainAlert.outlook(weather(14 to 80).copy(rainHours = listOf(RainHour(at(14) + 86_400_000L, 80))), morning, zone, threshold = 10))
    }

    @Test
    fun withoutHoursTheDaysOwnChance() {
        assertEquals(RainOutlook(60), RainAlert.outlook(weather(daily = 60), morning, zone, threshold = 10))
        assertEquals("60% chance of rain today", RainAlert.message(RainOutlook(60)) { "$it" })
        assertNull(RainAlert.outlook(weather(daily = 5), morning, zone, threshold = 10))
    }

    @Test
    fun breezysChancesAreRead() {
        val json = """{"timestamp":1761800000,"currentTemp":285.4,"precipProbability":40.0,"hourly":[{"timestamp":1761800400,"precipProbability":70},{"timestamp":1761804000,"precipProbability":150}]}"""
        val w = Weather.parse(json, receivedAt = 1_761_800_100_000L)!!
        assertEquals(40, w.rainChance)
        // An impossible chance is left out, not the whole forecast.
        assertEquals(listOf(RainHour(1_761_800_400_000L, 70)), w.rainHours)
    }
}
