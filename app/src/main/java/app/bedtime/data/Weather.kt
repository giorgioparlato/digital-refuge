package app.bedtime.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import java.util.Locale
import kotlin.math.roundToInt

/** The weather now, as Breezy Weather last sent it: temperature in kelvin, its words for the sky, and when. */
@Serializable
data class WeatherNow(val kelvin: Double, val condition: String? = null, val location: String? = null, val at: Long = 0L)

/** The fields of Gadgetbridge's WeatherSpec that the line needs; the rest are ignored. */
@Serializable
private data class WeatherSpec(
    val timestamp: Long? = null,
    val location: String? = null,
    val currentTemp: Double? = null,
    val currentCondition: String? = null,
)

/**
 * Weather from Breezy Weather, which sends it to apps you tick under "Send Gadgetbridge data". The app
 * has no internet permission: this is only ever what Breezy hands over.
 */
object Weather {
    /** Gadgetbridge's broadcast, which Breezy sends to the apps you choose. */
    const val ACTION = "nodomain.freeyourgadget.gadgetbridge.ACTION_GENERIC_WEATHER"

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
        return WeatherNow(kelvin, spec.currentCondition?.takeIf { it.isNotBlank() }, spec.location, at)
    }

    /** "12° · light rain" in the phone's usual unit, or null once it's more than [FRESH_MS] old. */
    fun line(weather: WeatherNow?, now: Long, fahrenheit: Boolean): String? {
        if (weather == null || now - weather.at > FRESH_MS) return null
        val celsius = weather.kelvin - 273.15
        val degrees = (if (fahrenheit) celsius * 9 / 5 + 32 else celsius).roundToInt()
        return listOfNotNull("$degrees°", weather.condition).joinToString(" · ")
    }

    /** Fahrenheit where it's the everyday unit; Celsius everywhere else. */
    fun usesFahrenheit(locale: Locale = Locale.getDefault()): Boolean = locale.country in setOf("US", "LR", "MM", "BS", "BZ", "KY", "PW", "FM", "MH")
}
