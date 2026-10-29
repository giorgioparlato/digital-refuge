package app.bedtime.data

import java.time.Instant
import java.time.ZoneId

/** Rain today as the alert says it: how likely, and the hours it's likeliest, [from] until [until], when known. */
data class RainOutlook(val chance: Int, val from: Int? = null, val until: Int? = null)

/**
 * The morning's rain alert: worth a word only when today's chance of rain reaches your threshold,
 * from the weather Breezy last sent, if that's recent enough to trust.
 */
object RainAlert {
    /** Older than this, the forecast is too stale to send. */
    const val STALE_MS = 6 * 60 * 60 * 1000L

    val THRESHOLDS = listOf(10, 20, 30, 50)

    /** The times it can come, as minutes of the day. */
    val TIMES = listOf(6 * 60, 6 * 60 + 30, 7 * 60, 7 * 60 + 30, 8 * 60, 9 * 60)

    /**
     * Today's outlook at [now], or null when there's nothing to say: no fresh forecast, or no chance
     * of rain up to [threshold]. With hours, the chance is the highest of the hours left today, and
     * its stretch the hours around it at least two thirds as likely; without, the day's own chance.
     */
    fun outlook(weather: WeatherNow?, now: Long, zone: ZoneId, threshold: Int): RainOutlook? {
        if (weather == null || now - weather.at > STALE_MS) return null
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val hours = weather.rainHours
            .filter { it.at + 3_600_000L > now }
            .filter { Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate() == today }
            .map { Instant.ofEpochMilli(it.at).atZone(zone).hour to it.chance }
            .sortedBy { it.first }
        if (hours.isEmpty()) return weather.rainChance?.takeIf { it >= threshold }?.let { RainOutlook(it) }
        val peak = hours.maxBy { it.second }
        if (peak.second < threshold) return null
        val enough = maxOf(threshold, peak.second * 2 / 3)
        val at = hours.indexOf(peak)
        var first = at
        while (first > 0 && hours[first - 1].second >= enough && hours[first - 1].first == hours[first].first - 1) first--
        var last = at
        while (last < hours.lastIndex && hours[last + 1].second >= enough && hours[last + 1].first == hours[last].first + 1) last++
        return RainOutlook(peak.second, hours[first].first, hours[last].first + 1)
    }

    /** "60% chance of rain today, mostly between 14:00 and 17:00", with [hour] saying an hour as the phone does. */
    fun message(outlook: RainOutlook, hour: (Int) -> String): String {
        val chance = "${outlook.chance}% chance of rain today"
        val from = outlook.from ?: return chance
        val until = outlook.until ?: return chance
        return if (until - from <= 1) "$chance, mostly around ${hour(from)}" else "$chance, mostly between ${hour(from)} and ${hour(until % 24)}"
    }
}
