package app.bedtime.ui.minimal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.HomeStyle
import app.bedtime.data.Repository
import app.bedtime.data.Weather
import app.bedtime.data.WeatherView
import java.time.LocalDateTime
import java.time.ZoneId

/** The weather as [style] asks for it, or null when it's off or Breezy hasn't sent anything lately. */
@Composable
fun rememberWeather(style: HomeStyle, now: LocalDateTime): WeatherView? {
    val context = LocalContext.current
    val repo = remember { Repository.get(context.applicationContext) }
    val weather by repo.weather.collectAsStateWithLifecycle(initialValue = null)
    if (!style.showWeather) return null
    return Weather.view(weather, now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), Weather.usesFahrenheit(), style.weatherStyle)
}
