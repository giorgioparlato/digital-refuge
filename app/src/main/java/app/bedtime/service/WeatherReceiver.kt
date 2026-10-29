package app.bedtime.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.bedtime.data.Repository
import app.bedtime.data.Weather
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.GZIPInputStream

/**
 * Takes the weather Breezy Weather sends (its "Send Gadgetbridge data", with digital refuge ticked)
 * and keeps the latest for the minimal home and lock screen. Nothing is fetched.
 */
class WeatherReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Weather.ACTION) return
        val gz = intent.getByteArrayExtra("WeatherGz")
        val text = intent.getStringExtra("WeatherJson")
        if (gz == null && text == null) return
        val done = goAsync()
        // Read off the main thread, and only so much of it: any app can send this.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = gz?.let { bytes ->
                    runCatching { GZIPInputStream(bytes.inputStream()).use { it.readAtMost(Weather.MAX_BYTES) } }.getOrNull()?.decodeToString()
                } ?: text?.takeIf { it.length <= Weather.MAX_BYTES }
                val weather = json?.let { Weather.parse(it, System.currentTimeMillis()) }
                if (weather != null) Repository.get(context).saveWeather(weather)
            } finally {
                done.finish()
            }
        }
    }
}

/** All of it, or null if there's more than [max] bytes. */
private fun InputStream.readAtMost(max: Int): ByteArray? {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val n = read(buffer)
        if (n < 0) return out.toByteArray()
        if (out.size() + n > max) return null
        out.write(buffer, 0, n)
    }
}
