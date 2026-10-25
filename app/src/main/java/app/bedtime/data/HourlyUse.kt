package app.bedtime.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** How an hour of today reads in the row of 24. */
enum class HourState { CALM, DISTRACTED, NOW, NOW_DISTRACTED, AHEAD }

/** Time in your distracting apps, by the hour: counted, and read as a row of 24 for the day. */
object HourlyUse {
    /** The thresholds offered, in minutes. */
    val THRESHOLDS = listOf(2, 5, 10, 15, 30)

    /**
     * The time from [from] to [to] (epoch millis), split by day and hour in [zone], as seconds: an hour
     * crossed in the middle of a visit counts its own share.
     */
    fun split(from: Long, to: Long, zone: ZoneId): Map<Long, Map<Int, Long>> {
        if (to <= from) return emptyMap()
        val out = mutableMapOf<Long, MutableMap<Int, Long>>()
        var at = from
        while (at < to) {
            val time = Instant.ofEpochMilli(at).atZone(zone)
            val nextHour = time.withMinute(0).withSecond(0).withNano(0).plusHours(1).toInstant().toEpochMilli()
            val until = minOf(nextHour, to)
            val day = time.toLocalDate().toEpochDay()
            val hours = out.getOrPut(day) { mutableMapOf() }
            hours[time.hour] = (hours[time.hour] ?: 0L) + (until - at)
            at = until
        }
        // Milliseconds to whole seconds, dropping hours that round to nothing.
        return out.mapValues { (_, hours) -> hours.mapValues { it.value / 1000 }.filterValues { it > 0 } }.filterValues { it.isNotEmpty() }
    }

    /** Today's 24 hours: distracted ones over [thresholdMinutes], this one marked, those to come ahead. */
    fun states(hourly: Map<Int, Long>, nowHour: Int, thresholdMinutes: Int): List<HourState> = (0 until 24).map { hour ->
        val distracted = (hourly[hour] ?: 0L) > thresholdMinutes * 60L
        when {
            hour > nowHour -> HourState.AHEAD
            hour == nowHour -> if (distracted) HourState.NOW_DISTRACTED else HourState.NOW
            distracted -> HourState.DISTRACTED
            else -> HourState.CALM
        }
    }

    /** "3 hours over 5 min today", or a quieter line when there are none. */
    fun summary(states: List<HourState>, thresholdMinutes: Int): String {
        val n = states.count { it == HourState.DISTRACTED || it == HourState.NOW_DISTRACTED }
        return when (n) {
            0 -> "no hour over $thresholdMinutes min in distracting apps today"
            1 -> "1 hour over $thresholdMinutes min in distracting apps today"
            else -> "$n hours over $thresholdMinutes min in distracting apps today"
        }
    }

    fun today(zone: ZoneId = ZoneId.systemDefault()): Long = LocalDate.now(zone).toEpochDay()
}
