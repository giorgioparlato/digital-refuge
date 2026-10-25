package app.bedtime.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** How an hour of today reads in the row of 24. */
enum class HourState { CALM, DISTRACTED, NOW, NOW_DISTRACTED, AHEAD }

/** A day's seconds by hour: in your distracting apps, and in any app. */
data class DayHours(val apps: Map<Int, Long> = emptyMap(), val all: Map<Int, Long> = emptyMap())

/** What makes an hour a distracted one: over [appMinutes] in distracting apps, or over [totalMinutes] in any, if set. */
data class HourRule(val appMinutes: Int, val totalMinutes: Int? = null)

/** Time on the phone by the hour: counted, and read as a row of 24 for the day. */
object HourlyUse {
    /** The thresholds offered for distracting apps, in minutes. */
    val THRESHOLDS = listOf(2, 5, 10, 15, 30)

    /** The thresholds offered for any app, in minutes. */
    val TOTAL_THRESHOLDS = listOf(15, 20, 30, 45)

    fun rule(settings: AppSettings) = HourRule(settings.distractionMinutes, settings.distractionTotalMinutes)

    /** Whether there's anything to count by: distracting apps chosen, or the any-app rule on. */
    fun counting(settings: AppSettings) = settings.distractingApps.isNotEmpty() || settings.distractionTotalMinutes != null

    fun distracted(hours: DayHours, hour: Int, rule: HourRule): Boolean =
        (hours.apps[hour] ?: 0L) > rule.appMinutes * 60L ||
            (rule.totalMinutes != null && (hours.all[hour] ?: 0L) > rule.totalMinutes * 60L)

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

    /** Today's 24 hours: distracted ones by [rule], this one marked, those to come ahead. */
    fun states(hours: DayHours, nowHour: Int, rule: HourRule): List<HourState> = (0 until 24).map { hour ->
        val distracted = distracted(hours, hour, rule)
        when {
            hour > nowHour -> HourState.AHEAD
            hour == nowHour -> if (distracted) HourState.NOW_DISTRACTED else HourState.NOW
            distracted -> HourState.DISTRACTED
            else -> HourState.CALM
        }
    }

    /** "3 hours over 5 min in distracting apps today", or, with both rules, "3 distracted hours today". */
    fun summary(states: List<HourState>, rule: HourRule): String {
        val n = states.count { it == HourState.DISTRACTED || it == HourState.NOW_DISTRACTED }
        if (rule.totalMinutes != null) {
            return when (n) {
                0 -> "no distracted hour today"
                1 -> "1 distracted hour today"
                else -> "$n distracted hours today"
            }
        }
        return when (n) {
            0 -> "no hour over ${rule.appMinutes} min in distracting apps today"
            1 -> "1 hour over ${rule.appMinutes} min in distracting apps today"
            else -> "$n hours over ${rule.appMinutes} min in distracting apps today"
        }
    }

    fun today(zone: ZoneId = ZoneId.systemDefault()): Long = LocalDate.now(zone).toEpochDay()
}
