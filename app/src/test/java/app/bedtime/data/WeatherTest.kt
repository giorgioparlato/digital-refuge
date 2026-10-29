package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class WeatherTest {
    private val spec = """{"timestamp":1761420000,"location":"Stockholm","currentTemp":285.4,"currentCondition":"light rain","currentConditionCode":500,"forecasts":[]}"""

    @Test
    fun readsGadgetbridgesWeatherSpec() {
        val w = Weather.parse(spec, receivedAt = 1_761_420_060_000L)!!
        assertEquals(285.4, w.kelvin, 0.001)
        assertEquals("light rain", w.condition)
        assertEquals(1_761_420_000_000L, w.at)
        // Sent as if from the future, it counts from when it came, so it can't stay fresh for good.
        assertEquals(1_000L, Weather.parse(spec, receivedAt = 1_000L)!!.at)
        // A list of locations: the first is yours.
        assertEquals("Stockholm", Weather.parse("[$spec, ${spec.replace("Stockholm", "Rome")}]", 0L)!!.location)
    }

    @Test
    fun ignoresWhatIsntWeather() {
        assertNull(Weather.parse("not json", 0L))
        assertNull(Weather.parse("""{"location":"x"}""", 0L))
        assertNull(Weather.parse("""{"currentTemp":12}""", 0L))
    }

    @Test
    fun theLineInTheUsualUnitAndOnlyWhileFresh() {
        val w = WeatherNow(285.4, "light rain", at = 1_000L)
        assertEquals("12° · light rain", Weather.line(w, now = 2_000L, fahrenheit = false))
        assertEquals("54° · light rain", Weather.line(w, now = 2_000L, fahrenheit = true))
        assertEquals("12°", Weather.line(w.copy(condition = null), now = 2_000L, fahrenheit = false))
        assertNull(Weather.line(w, now = 1_000L + Weather.FRESH_MS + 1, fahrenheit = false))
        assertNull(Weather.line(null, now = 0L, fahrenheit = false))
    }

    @Test
    fun fahrenheitWhereItsEveryday() {
        assertTrue(Weather.usesFahrenheit(Locale.US))
        assertFalse(Weather.usesFahrenheit(Locale("sv", "SE")))
        assertFalse(Weather.usesFahrenheit(Locale.UK))
    }

    @Test
    fun weatherIsOffUntilSwitchedOn() {
        val style = AppJson.decodeFromString(HomeStyle.serializer(), """{"showQuote":true}""")
        assertFalse(style.showWeather)
        assertEquals(WeatherStyle.TODAY, style.weatherStyle)
        assertEquals(TempUnit.AUTO, style.weatherUnit)
        // A saved "now" still loads.
        assertEquals(WeatherStyle.NOW, AppJson.decodeFromString(HomeStyle.serializer(), """{"weatherStyle":"NOW"}""").weatherStyle)
    }

    @Test
    fun theUnitYouChoose() {
        assertTrue(Weather.fahrenheit(TempUnit.AUTO, Locale.US))
        assertFalse(Weather.fahrenheit(TempUnit.CELSIUS, Locale.US))
        assertTrue(Weather.fahrenheit(TempUnit.FAHRENHEIT, Locale("sv", "SE")))
        assertFalse(Weather.fahrenheit(TempUnit.AUTO, Locale("sv", "SE")))
    }

    @Test
    fun readsTodayAndTheNextDays() {
        val json = """{"timestamp":1761420000,"currentTemp":285.4,"currentCondition":"light rain","currentConditionCode":500,
            "todayMinTemp":281.4,"todayMaxTemp":287.6,
            "forecasts":[{"conditionCode":802,"minTemp":280.2,"maxTemp":289.1},{"conditionCode":800,"minTemp":279.5,"maxTemp":290.8},{"conditionCode":600}]}"""
        val w = Weather.parse(json, 0L)!!
        assertEquals(500, w.code)
        // Up to four coming days are kept; this one sends three.
        assertEquals(3, w.days.size)
        val now = w.at + 1
        assertEquals(WeatherView(WeatherIcon.RAIN, "12°", range = "8°–14°"), Weather.view(w, now, false, WeatherStyle.TODAY))
        // "Now" is no longer offered; saved, it shows as today.
        assertEquals(Weather.view(w, now, false, WeatherStyle.TODAY), Weather.view(w, now, false, WeatherStyle.NOW))
        assertEquals(
            listOf(WeatherDayView(1, WeatherIcon.PARTLY, 16, 7), WeatherDayView(2, WeatherIcon.CLEAR, 18, 6)),
            Weather.view(w, now, false, WeatherStyle.DAYS)!!.days,
        )
        assertNull(Weather.view(w, w.at + Weather.FRESH_MS + 1, false, WeatherStyle.TODAY))
        // Five days: today, then the forecasts sent (here three).
        assertEquals(listOf(0, 1, 2, 3), Weather.view(w, now, false, WeatherStyle.WEEK)!!.week.map { it.offset })
        assertEquals(WeatherDayView(0, WeatherIcon.RAIN, 14, 8), Weather.view(w, now, false, WeatherStyle.WEEK)!!.week.first())
        // The five days alone: the same days, and nothing for beside the clock or after the date.
        val five = Weather.view(w, now, false, WeatherStyle.FIVE_DAYS)!!
        assertEquals(Weather.view(w, now, false, WeatherStyle.WEEK)!!.week, five.week)
        assertEquals(WeatherView(null, "", week = five.week), five)
    }

    @Test
    fun skiesFromConditionCodes() {
        assertEquals(WeatherIcon.STORM, Weather.icon(211))
        assertEquals(WeatherIcon.RAIN, Weather.icon(301))
        assertEquals(WeatherIcon.SNOW, Weather.icon(511))
        assertEquals(WeatherIcon.SNOW, Weather.icon(601))
        assertEquals(WeatherIcon.FOG, Weather.icon(741))
        assertEquals(WeatherIcon.CLEAR, Weather.icon(800))
        assertEquals(WeatherIcon.PARTLY, Weather.icon(802))
        assertEquals(WeatherIcon.CLOUDY, Weather.icon(804))
        assertNull(Weather.icon(null))
        // Without a code, the sky's own words.
        assertEquals(WeatherIcon.RAIN, Weather.icon(null, "Light rain"))
        assertEquals(WeatherIcon.PARTLY, Weather.icon(null, "Partly cloudy"))
        assertEquals(WeatherIcon.CLOUDY, Weather.icon(null, "Overcast"))
        assertEquals(WeatherIcon.CLEAR, Weather.icon(null, "Sunny"))
        assertNull(Weather.icon(null, "Windy"))
    }
}
