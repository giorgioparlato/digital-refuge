package app.bedtime.engine

import app.bedtime.data.MoodMark
import app.bedtime.data.Moods
import app.bedtime.data.SessionLog
import app.bedtime.data.UseDay
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

/**
 * What sessions did, rather than how long they ran: the phone time they spare you, the nights they
 * leave free, when and why they break, what follows them, and how often they hold. All from what's
 * kept already: the sessions, the phone's use by the hour, and the moods tapped. Each says something
 * only once there's enough to say it, and only "with" against "without", never why.
 */
object SessionInsights {
    /** When a session really ended: early if it was ended early. */
    fun ended(s: SessionLog): Long = s.endedEarlyAt?.let { minOf(it, s.end) } ?: s.end

    private fun hour(millis: Long, zone: ZoneId) = Instant.ofEpochMilli(millis).atZone(zone)

    /** Seconds on the phone between [from] and [to], from its use by the hour (shared out within an hour); null if a day isn't known. */
    fun phoneIn(days: Map<Long, UseDay>, from: Long, to: Long, zone: ZoneId): Long? {
        var at = from
        var seconds = 0.0
        while (at < to) {
            val t = hour(at, zone)
            val day = days[t.toLocalDate().toEpochDay()] ?: return null
            val end = minOf(to, t.truncatedTo(ChronoUnit.HOURS).plusHours(1).toInstant().toEpochMilli())
            seconds += (day.all[t.hour] ?: 0L) * (end - at) / 3_600_000.0
            at = end
        }
        return seconds.toLong()
    }

    /** Average phone seconds in a session, against the same hours on known days without one: with, without. */
    data class Compared(val with: Long, val without: Long)

    private fun compared(days: Map<Long, UseDay>, sessions: List<SessionLog>, zone: ZoneId, window: (SessionLog) -> Pair<Long, Long>): Compared? {
        val known = days.values.filter { it.total > 0 }.map { it.day }.toSet()
        val with = sessions.mapNotNull { s -> window(s).let { (a, b) -> phoneIn(days, a, b, zone) } }
        if (with.size < 2) return null
        // The same clock hours, from the latest session, on days none began.
        val latest = sessions.maxBy { it.start }
        val (from, to) = window(latest)
        val startDay = hour(latest.start, zone).toLocalDate()
        val began = sessions.map { hour(it.start, zone).toLocalDate().toEpochDay() }.toSet()
        val without = known.filter { it !in began }.mapNotNull { d ->
            val shift = (d - startDay.toEpochDay()) * 86_400_000L
            phoneIn(days, from + shift, to + shift, zone)
        }
        if (without.size < 2) return null
        return Compared(with.average().toLong(), without.average().toLong())
    }

    /** The phone during [sessions] (of one schedule), against the same hours without it. */
    fun saved(sessions: List<SessionLog>, days: Map<Long, UseDay>, zone: ZoneId): Compared? =
        compared(days, sessions, zone) { it.start to ended(it) }

    /** The phone in the hour after [sessions] end, against that hour without one. */
    fun rebound(sessions: List<SessionLog>, days: Map<Long, UseDay>, zone: ZoneId): Compared? =
        compared(days, sessions, zone) { ended(it) to ended(it) + 3_600_000L }

    /** Whether [sessions] are mostly at night: begun between 19:00 and 2:59. */
    fun atNight(sessions: List<SessionLog>, zone: ZoneId): Boolean =
        sessions.isNotEmpty() && sessions.count { hour(it.start, zone).hour.let { h -> h >= 19 || h <= 2 } } * 2 > sessions.size

    /** When in sessions the ways out came: how many in its first 15 minutes, 15–30, 30–60 and after; and on which days. */
    data class Breaks(val byMinutes: List<Int>, val byDay: Map<DayOfWeek, Int>) {
        val total: Int get() = byMinutes.sum()
    }

    fun breaks(sessions: List<SessionLog>, zone: ZoneId): Breaks {
        val minutes = IntArray(4)
        val days = mutableMapOf<DayOfWeek, Int>()
        sessions.forEach { s ->
            s.escapeTimes.forEach { at ->
                val m = (at - s.start) / 60_000
                minutes[when { m < 15 -> 0; m < 30 -> 1; m < 60 -> 2; else -> 3 }]++
                hour(at, zone).dayOfWeek.let { days[it] = (days[it] ?: 0) + 1 }
            }
        }
        return Breaks(minutes.toList(), days)
    }

    /** The moods "why?" heard inside [sessions], most first; and how often the phone was put down, of how many answers. */
    data class Why(val moods: List<Pair<String, Int>>, val putDown: Int, val answers: Int)

    fun why(sessions: List<SessionLog>, marks: List<MoodMark>): Why? {
        val inside = marks.filter { m -> sessions.any { m.at >= it.start && m.at <= ended(it) } }
        if (inside.isEmpty()) return null
        val answers = inside.filter { it.wentOn != null }.groupBy { it.at }.map { it.value.first() }
        return Why(Moods.counts(inside).take(3), answers.count { it.wentOn == false }, answers.size)
    }

    /** A night without the phone: the morning it ends, roughly when it was last used and first used again, and how long between. */
    data class Night(val morning: LocalDate, val lastHour: Int, val firstHour: Int, val hours: Int)

    /** At least this long in an hour counts as using the phone then. */
    private const val USED = 60L

    /**
     * The nights before the last [count] mornings up to [today]: the last hour the phone was used from
     * 18:00 to 3:59, and the first from 4:00 to noon. By the hour, as Android keeps it.
     */
    fun nights(days: Map<Long, UseDay>, today: LocalDate, count: Int = 14): List<Night> =
        (0 until count).mapNotNull { back ->
            val morning = today.minusDays(back.toLong())
            val day = days[morning.toEpochDay()] ?: return@mapNotNull null
            val eve = days[morning.toEpochDay() - 1] ?: return@mapNotNull null
            val evening = (18..23).filter { (eve.all[it] ?: 0) >= USED }.map { it - 24 } + (0..3).filter { (day.all[it] ?: 0) >= USED }
            val last = evening.maxOrNull() ?: return@mapNotNull null
            val first = (4..12).firstOrNull { (day.all[it] ?: 0) >= USED } ?: return@mapNotNull null
            Night(morning, (last + 24) % 24, first, first - (last + 1))
        }

    /** Of [sessions], the share kept (no way out taken) each of the last [weeks] weeks, oldest first; null for a week with none. */
    fun keptByWeek(sessions: List<SessionLog>, today: LocalDate, zone: ZoneId, weeks: Int = 8): List<Float?> {
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        return (weeks - 1 downTo 0).map { back ->
            val from = monday.minusWeeks(back.toLong())
            val those = sessions.filter { hour(it.start, zone).toLocalDate().let { d -> !d.isBefore(from) && d.isBefore(from.plusWeeks(1)) } }
            if (those.isEmpty()) null else those.count { it.escapes == 0 }.toFloat() / those.size
        }
    }

    /**
     * The apps used inside [sessions] (the ones [events] still reach), most first, with their seconds:
     * let through by the session, or used once a way out was taken. [excluded] apps (refuge, the home
     * screens) aren't counted.
     */
    fun apps(sessions: List<SessionLog>, events: List<app.bedtime.data.UseEvent>, zone: ZoneId, excluded: Set<String>): List<Pair<String, Long>> {
        if (events.isEmpty()) return emptyList()
        val reach = events.minOf { it.at }
        val totals = mutableMapOf<String, Long>()
        sessions.filter { ended(it) > reach }.forEach { s ->
            app.bedtime.data.PhoneUsage.fromEvents(events, maxOf(s.start, reach), ended(s), zone, excluded, emptySet()).values.forEach { day ->
                day.apps.forEach { (pkg, seconds) -> totals[pkg] = (totals[pkg] ?: 0L) + seconds }
            }
        }
        return totals.filter { it.value >= 60 }.toList().sortedByDescending { it.second }
    }

    /** Blocks: minutes focused this week and last, the longest in [sessions], and the part of the day they hold best (with how many kept of how many). */
    data class Blocks(val thisWeek: Long, val lastWeek: Long, val longest: Long, val best: Triple<String, Int, Int>?)

    fun blocks(sessions: List<SessionLog>, today: LocalDate, zone: ZoneId): Blocks? {
        if (sessions.isEmpty()) return null
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        fun minutesIn(from: LocalDate) = sessions.filter { hour(it.start, zone).toLocalDate().let { d -> !d.isBefore(from) && d.isBefore(from.plusWeeks(1)) } }
            .sumOf { (ended(it) - it.start).coerceAtLeast(0) } / 60_000
        val best = sessions.groupBy { Moods.partOfDay(hour(it.start, zone).hour) }
            .filter { it.value.size >= 3 }
            .map { (part, list) -> Triple(part, list.count { it.escapes == 0 }, list.size) }
            .maxByOrNull { it.second.toFloat() / it.third }
        return Blocks(minutesIn(monday), minutesIn(monday.minusWeeks(1)), sessions.maxOf { (ended(it) - it.start) / 60_000 }, best)
    }
}
