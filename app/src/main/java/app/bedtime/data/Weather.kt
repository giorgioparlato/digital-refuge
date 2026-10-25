package app.bedtime.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.util.Locale
import kotlin.math.roundToInt

/** One coming day's forecast: its sky (an OpenWeatherMap code) and its low and high, in kelvin. */
@Serializable
data class WeatherDay(val code: Int? = null, val low: Double? = null, val high: Double? = null)

/**
 * The weather as Breezy Weather last sent it: now (kelvin, its words for the sky and their code),
 * today's low and high, the next days, and when.
 */
@Serializable
data class WeatherNow(
    val kelvin: Double,
    val condition: String? = null,
    val location: String? = null,
    val at: Long = 0L,
    val code: Int? = null,
    val low: Double? = null,
    val high: Double? = null,
    /** Tomorrow first. */
    val days: List<WeatherDay> = emptyList(),
)

/**
 * How much weather to show: today's low and high; three days with their skies; or five days, on a
 * line of their own under the date. [NOW] is no longer offered; saved, it reads as [TODAY].
 */
@Serializable
enum class WeatherStyle { NOW, TODAY, DAYS, WEEK }

/** Celsius or Fahrenheit, or whichever is usual where the phone is set. */
@Serializable
enum class TempUnit { AUTO, CELSIUS, FAHRENHEIT }

/** The sky, as a small drawing. */
enum class WeatherIcon { CLEAR, PARTLY, CLOUDY, RAIN, STORM, SNOW, FOG }

/** A day as shown: 0 today, 1 tomorrow, and so on; its sky and its high and low in your unit. */
data class WeatherDayView(val offset: Int, val icon: WeatherIcon?, val high: Int?, val low: Int?)

/**
 * What sits beside the clock: the sky now, the temperature ("12°"), and under it the sky's words,
 * today's range ("8°–14°"), or the next two days, as chosen.
 */
data class WeatherView(
    val icon: WeatherIcon?,
    val temperature: String,
    val condition: String? = null,
    val range: String? = null,
    /** The next two days, at the end of the date line. */
    val days: List<WeatherDayView> = emptyList(),
    /** Five days from today, on a line of their own under the date. */
    val week: List<WeatherDayView> = emptyList(),
)

/** The fields of Gadgetbridge's WeatherSpec that are shown; the rest are ignored. */
@Serializable
private data class WeatherSpec(
    val timestamp: Long? = null,
    val location: String? = null,
    val currentTemp: Double? = null,
    val currentCondition: String? = null,
    val currentConditionCode: Int? = null,
    val todayMinTemp: Double? = null,
    val todayMaxTemp: Double? = null,
    val forecasts: List<ForecastSpec> = emptyList(),
)

@Serializable
private data class ForecastSpec(val conditionCode: Int? = null, val minTemp: Double? = null, val maxTemp: Double? = null)

/**
 * Weather from Breezy Weather, which sends it to apps you tick under "Send Gadgetbridge data". The app
 * has no internet permission: this is only ever what Breezy hands over.
 */
object Weather {
    /** Breezy Weather, and its debug build, to open from a tap on the weather. */
    val APPS = listOf("org.breezyweather", "org.breezyweather.debug")

    /** Gadgetbridge's broadcast, which Breezy sends to the apps you choose. */
    const val ACTION = "nodomain.freeyourgadget.gadgetbridge.ACTION_GENERIC_WEATHER"

    /** An example sky for the style page's preview, before Breezy has sent anything. */
    val SAMPLE = WeatherNow(
        kelvin = 285.4,
        condition = "light rain",
        at = 1L,
        code = 500,
        low = 281.4,
        high = 287.6,
        days = listOf(WeatherDay(802, 280.2, 289.1), WeatherDay(800, 279.5, 290.8), WeatherDay(804, 281.0, 286.3), WeatherDay(211, 282.4, 288.0)),
    )

    /** After this long without news, the line goes rather than show a stale sky. */
    const val FRESH_MS = 3 * 60 * 60 * 1000L

    /** The first location in [json], a WeatherSpec or a list of them; null if it isn't one, or is absurd. */
    fun parse(json: String, receivedAt: Long): WeatherNow? {
        val spec = runCatching { AppJson.decodeFromString(WeatherSpec.serializer(), json) }.getOrNull()
            ?: runCatching { AppJson.decodeFromString(ListSerializer(WeatherSpec.serializer()), json).firstOrNull() }.getOrNull()
            ?: return null
        val kelvin = spec.currentTemp ?: return null
        // Between about -90 °C and 60 °C: anything else isn't weather.
        if (kelvin !in 180.0..335.0) return null
        val at = spec.timestamp?.takeIf { it > 0 }?.let { it * 1000 } ?: receivedAt
        return WeatherNow(
            kelvin = kelvin,
            condition = spec.currentCondition?.takeIf { it.isNotBlank() },
            location = spec.location,
            at = at,
            code = spec.currentConditionCode,
            low = spec.todayMinTemp?.takeIf { it in 180.0..335.0 },
            high = spec.todayMaxTemp?.takeIf { it in 180.0..335.0 },
            days = spec.forecasts.take(4).map { WeatherDay(it.conditionCode, it.minTemp, it.maxTemp) },
        )
    }

    /** The sky for an OpenWeatherMap condition code, as Gadgetbridge passes them on; else read from its words. */
    fun icon(code: Int?, words: String? = null): WeatherIcon? = when (code) {
        in 200..299 -> WeatherIcon.STORM
        in 300..399, in 500..510, in 512..599 -> WeatherIcon.RAIN
        511, in 600..699 -> WeatherIcon.SNOW
        in 700..799 -> WeatherIcon.FOG
        800 -> WeatherIcon.CLEAR
        801, 802 -> WeatherIcon.PARTLY
        in 803..899 -> WeatherIcon.CLOUDY
        else -> words?.lowercase()?.let { w ->
            when {
                "thunder" in w || "storm" in w -> WeatherIcon.STORM
                "snow" in w || "sleet" in w || "hail" in w -> WeatherIcon.SNOW
                "rain" in w || "drizzle" in w || "shower" in w -> WeatherIcon.RAIN
                "fog" in w || "mist" in w || "haze" in w -> WeatherIcon.FOG
                "partly" in w || "few clouds" in w || "scattered" in w -> WeatherIcon.PARTLY
                "cloud" in w || "overcast" in w -> WeatherIcon.CLOUDY
                "clear" in w || "sun" in w || "fair" in w -> WeatherIcon.CLEAR
                else -> null
            }
        }
    }

    private fun degrees(kelvin: Double, fahrenheit: Boolean): Int {
        val celsius = kelvin - 273.15
        return (if (fahrenheit) celsius * 9 / 5 + 32 else celsius).roundToInt()
    }

    /**
     * What to show for [style], or null when there's nothing fresh (over [FRESH_MS] old): the sky and
     * temperature, and under them its words, today's range, or the next two days.
     */
    fun view(weather: WeatherNow?, now: Long, fahrenheit: Boolean, style: WeatherStyle): WeatherView? {
        if (weather == null || now - weather.at > FRESH_MS) return null
        val sky = icon(weather.code, weather.condition)
        val temperature = "${degrees(weather.kelvin, fahrenheit)}°"
        return when (style) {
            WeatherStyle.NOW, WeatherStyle.TODAY -> WeatherView(
                sky,
                temperature,
                range = if (weather.low != null && weather.high != null) "${degrees(weather.low, fahrenheit)}°–${degrees(weather.high, fahrenheit)}°" else null,
            )
            WeatherStyle.DAYS -> WeatherView(sky, temperature, days = coming(weather, fahrenheit).take(2))
            WeatherStyle.WEEK -> WeatherView(
                sky,
                temperature,
                condition = weather.condition,
                week = listOf(WeatherDayView(0, sky, weather.high?.let { degrees(it, fahrenheit) }, weather.low?.let { degrees(it, fahrenheit) })) +
                    coming(weather, fahrenheit).take(4),
            )
        }
    }

    /** The days after today, as sent, tomorrow first. */
    private fun coming(weather: WeatherNow, fahrenheit: Boolean): List<WeatherDayView> = weather.days.mapIndexed { i, d ->
        WeatherDayView(i + 1, icon(d.code), d.high?.let { degrees(it, fahrenheit) }, d.low?.let { degrees(it, fahrenheit) })
    }

    /** Whether to show Fahrenheit, as chosen, or as usual where the phone is set. */
    fun fahrenheit(unit: TempUnit, locale: Locale = Locale.getDefault()): Boolean = when (unit) {
        TempUnit.AUTO -> usesFahrenheit(locale)
        TempUnit.CELSIUS -> false
        TempUnit.FAHRENHEIT -> true
    }

    /** "12° · light rain" in the phone's usual unit, or null once it's more than [FRESH_MS] old. */
    fun line(weather: WeatherNow?, now: Long, fahrenheit: Boolean): String? {
        if (weather == null || now - weather.at > FRESH_MS) return null
        return listOfNotNull("${degrees(weather.kelvin, fahrenheit)}°", weather.condition).joinToString(" · ")
    }

    /** Fahrenheit where it's the everyday unit; Celsius everywhere else. */
    fun usesFahrenheit(locale: Locale = Locale.getDefault()): Boolean = locale.country in setOf("US", "LR", "MM", "BS", "BZ", "KY", "PW", "FM", "MH")
}
