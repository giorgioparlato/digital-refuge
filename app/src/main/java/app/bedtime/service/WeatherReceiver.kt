package app.bedtime.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.bedtime.data.Repository
import app.bedtime.data.Weather
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.zip.GZIPInputStream

/**
 * Takes the weather Breezy Weather sends (its "Send Gadgetbridge data", with digital refuge ticked)
 * and keeps the latest for the minimal home and lock screen. Nothing is fetched.
 */
class WeatherReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Weather.ACTION) return
        val json = intent.getByteArrayExtra("WeatherGz")?.let { gz -> runCatching { GZIPInputStream(gz.inputStream()).use { it.readBytes().decodeToString() } }.getOrNull() }
            ?: intent.getStringExtra("WeatherJson")
            ?: return
        val weather = Weather.parse(json, System.currentTimeMillis()) ?: return
        val done = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Repository.get(context).saveWeather(weather)
            } finally {
                done.finish()
            }
        }
    }
}
