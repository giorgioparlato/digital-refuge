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
        val w = Weather.parse(spec, receivedAt = 0L)!!
        assertEquals(285.4, w.kelvin, 0.001)
        assertEquals("light rain", w.condition)
        assertEquals(1_761_420_000_000L, w.at)
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
        assertFalse(AppJson.decodeFromString(HomeStyle.serializer(), """{"showQuote":true}""").showWeather)
    }

    @Test
    fun readsTodayAndTheNextDays() {
        val json = """{"timestamp":1761420000,"currentTemp":285.4,"currentCondition":"light rain","currentConditionCode":500,
            "todayMinTemp":281.4,"todayMaxTemp":287.6,
            "forecasts":[{"conditionCode":802,"minTemp":280.2,"maxTemp":289.1},{"conditionCode":800,"minTemp":279.5,"maxTemp":290.8},{"conditionCode":600}]}"""
        val w = Weather.parse(json, 0L)!!
        assertEquals(500, w.code)
        assertEquals(2, w.days.size)
        val now = w.at + 1
        assertEquals(WeatherView("12° · light rain", WeatherIcon.RAIN), Weather.view(w, now, false, WeatherStyle.NOW))
        assertEquals("12° · light rain · 8°–14°", Weather.view(w, now, false, WeatherStyle.TODAY)!!.text)
        assertEquals(
            listOf(WeatherDayView(0, WeatherIcon.RAIN, 14, 8), WeatherDayView(1, WeatherIcon.PARTLY, 16, 7), WeatherDayView(2, WeatherIcon.CLEAR, 18, 6)),
            Weather.view(w, now, false, WeatherStyle.DAYS)!!.days,
        )
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
    }
}
