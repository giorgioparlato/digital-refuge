package app.bedtime.data

import java.time.Instant
import java.time.ZoneId

/**
 * One day on the phone: seconds by hour in any app and in your distracting ones, seconds and opens by
 * app, and unlocks. From Android's usage records [complete] is true and every app is there; from
 * refuge's own count only distracting apps are named, opens aren't known, and unlocks only from when it
 * began counting them.
 */
data class UseDay(
    val day: Long,
    val all: Map<Int, Long> = emptyMap(),
    val distracting: Map<Int, Long> = emptyMap(),
    val apps: Map<String, Long> = emptyMap(),
    val opens: Map<String, Int> = emptyMap(),
    val unlocks: Int? = null,
    val complete: Boolean = false,
    /** Unlocks by hour of the day, where known. */
    val unlockHours: Map<Int, Int> = emptyMap(),
) {
    val total: Long get() = all.values.sum()
    val distractingTotal: Long get() = distracting.values.sum()

    /** The same day as the dots read it, naming only [distractingApps] among the apps. */
    fun hours(distractingApps: Set<String>): DayHours = DayHours(distracting, all, apps.filterKeys { it in distractingApps })
}

/** Something Android noted: an app coming to the front or leaving it, the screen going off, or an unlock. */
data class UseEvent(val at: Long, val kind: Kind, val pkg: String = "") {
    enum class Kind { RESUMED, PAUSED, SCREEN_OFF, UNLOCK }
}

/** An app over a stretch of days: time, opens, and whether it's one of your distracting ones. */
data class AppUse(val pkg: String, val seconds: Long, val opens: Int, val distracting: Boolean)

/** This week against the one before: each figure now and then. */
data class Trend(val total: Pair<Long, Long>, val distracting: Pair<Long, Long>, val unlocks: Pair<Int, Int>?)

object PhoneUsage {
    /**
     * Days of use from Android's events between [from] and [to]: an app's time runs from coming to the
     * front until it leaves, another app comes, or the screen goes off. [excluded] apps (digital refuge,
     * home screens) count for nothing but end the app before them. An open is an app coming to the front
     * after a different one.
     */
    fun fromEvents(events: List<UseEvent>, from: Long, to: Long, zone: ZoneId, excluded: Set<String>, distractingApps: Set<String>): Map<Long, UseDay> {
        val all = mutableMapOf<Long, MutableMap<Int, Long>>()
        val distracting = mutableMapOf<Long, MutableMap<Int, Long>>()
        val apps = mutableMapOf<Long, MutableMap<String, Long>>()
        val opens = mutableMapOf<Long, MutableMap<String, Int>>()
        val unlocks = mutableMapOf<Long, Int>()
        val unlockHours = mutableMapOf<Long, MutableMap<Int, Int>>()
        fun dayOf(at: Long) = Instant.ofEpochMilli(at).atZone(zone).toLocalDate().toEpochDay()
        fun add(pkg: String, start: Long, end: Long) {
            HourlyUse.split(maxOf(start, from), minOf(end, to), zone).forEach { (day, hours) ->
                val dayAll = all.getOrPut(day) { mutableMapOf() }
                hours.forEach { (h, s) -> dayAll[h] = (dayAll[h] ?: 0L) + s }
                if (pkg in distractingApps) {
                    val dayDistracting = distracting.getOrPut(day) { mutableMapOf() }
                    hours.forEach { (h, s) -> dayDistracting[h] = (dayDistracting[h] ?: 0L) + s }
                }
                val dayApps = apps.getOrPut(day) { mutableMapOf() }
                dayApps[pkg] = (dayApps[pkg] ?: 0L) + hours.values.sum()
            }
        }
        var current: String? = null
        var since = 0L
        var last: String? = null
        fun close(at: Long) {
            current?.let { if (at > since) add(it, since, at) }
            current = null
        }
        events.sortedBy { it.at }.forEach { e ->
            when (e.kind) {
                UseEvent.Kind.RESUMED -> if (e.pkg != current) {
                    close(e.at)
                    if (e.pkg !in excluded) {
                        current = e.pkg
                        since = e.at
                        if (e.pkg != last && e.at in from..to) {
                            val day = opens.getOrPut(dayOf(e.at)) { mutableMapOf() }
                            day[e.pkg] = (day[e.pkg] ?: 0) + 1
                        }
                    }
                    last = e.pkg
                }
                UseEvent.Kind.PAUSED -> if (e.pkg == current) close(e.at)
                UseEvent.Kind.SCREEN_OFF -> {
                    close(e.at)
                    last = null
                }
                UseEvent.Kind.UNLOCK -> if (e.at in from..to) dayOf(e.at).let { day ->
                    unlocks[day] = (unlocks[day] ?: 0) + 1
                    val hour = Instant.ofEpochMilli(e.at).atZone(zone).hour
                    val byHour = unlockHours.getOrPut(day) { mutableMapOf() }
                    byHour[hour] = (byHour[hour] ?: 0) + 1
                }
            }
        }
        close(to)
        val days = all.keys + opens.keys + unlocks.keys
        return days.associateWith { day ->
            UseDay(day, all[day].orEmpty(), distracting[day].orEmpty(), apps[day].orEmpty(), opens[day].orEmpty(), unlocks[day] ?: 0, complete = true, unlockHours = unlockHours[day].orEmpty())
        }
    }

    /** Days as refuge counted them itself, from the window changes blocking sees. */
    fun fromRecords(usage: List<UsageDay>): Map<Long, UseDay> =
        usage.associate { it.day to UseDay(it.day, it.hourlyAll, it.hourly, it.distractingSeconds, unlocks = it.unlocks, unlockHours = it.unlockHours) }

    /**
     * Android's days where it has them whole (after the first day its records reach back to, which is
     * cut off), refuge's own for the rest.
     */
    fun merge(fromAndroid: Map<Long, UseDay>, own: Map<Long, UseDay>, firstAndroidDay: Long?): Map<Long, UseDay> {
        if (firstAndroidDay == null) return own
        return own.filterKeys { it <= firstAndroidDay } + fromAndroid.filterKeys { it > firstAndroidDay }
    }

    /** [days] from [first] to [last], an empty day for any missing. */
    fun range(days: Map<Long, UseDay>, first: Long, last: Long): List<UseDay> = (first..last).map { days[it] ?: UseDay(it) }

    /** Average seconds a day, over [days] that had any use at all. */
    fun dailyAverage(days: List<UseDay>, of: (UseDay) -> Long): Long? {
        val used = days.filter { it.total > 0 }
        return if (used.isEmpty()) null else used.sumOf(of) / used.size
    }

    /** Each hour's average seconds over [days] that had any use: on the phone at all, and in distracting apps. */
    fun averageHours(days: List<UseDay>): List<Pair<Long, Long>> {
        val used = days.filter { it.total > 0 }.ifEmpty { return List(24) { 0L to 0L } }
        return (0 until 24).map { h -> used.sumOf { it.all[h] ?: 0L } / used.size to used.sumOf { it.distracting[h] ?: 0L } / used.size }
    }

    /** Apps by time over [days], most first; those under a minute left out. */
    fun ranked(days: List<UseDay>, distractingApps: Set<String>): List<AppUse> {
        val seconds = mutableMapOf<String, Long>()
        val opens = mutableMapOf<String, Int>()
        days.forEach { d ->
            d.apps.forEach { (pkg, s) -> seconds[pkg] = (seconds[pkg] ?: 0L) + s }
            d.opens.forEach { (pkg, n) -> opens[pkg] = (opens[pkg] ?: 0) + n }
        }
        return seconds.filterValues { it >= 60 }
            .map { (pkg, s) -> AppUse(pkg, s, opens[pkg] ?: 0, pkg in distractingApps) }
            .sortedByDescending { it.seconds }
    }

    /** The last seven days against the seven before; unlocks only if both weeks know them. */
    fun trend(thisWeek: List<UseDay>, lastWeek: List<UseDay>): Trend {
        fun unlocks(week: List<UseDay>) = week.filter { it.total > 0 }.takeIf { w -> w.isNotEmpty() && w.all { it.unlocks != null } }?.sumOf { it.unlocks ?: 0 }
        val now = unlocks(thisWeek)
        val then = unlocks(lastWeek)
        return Trend(
            thisWeek.sumOf { it.total } to lastWeek.sumOf { it.total },
            thisWeek.sumOf { it.distractingTotal } to lastWeek.sumOf { it.distractingTotal },
            if (now != null && then != null) now to then else null,
        )
    }
}

/** A month on the phone: seconds in any app and in distracting ones, over the [days] that had any use. "2026-10". */
@kotlinx.serialization.Serializable
data class UsageMonth(val month: String, val total: Long = 0, val distracting: Long = 0, val days: Int = 0) {
    /** Seconds a day on average, over the days that had any use. */
    val perDay: Long? get() = if (days > 0) total / days else null
    val distractingPerDay: Long? get() = if (days > 0) distracting / days else null
}

object UseMonths {
    fun key(day: Long): String = java.time.LocalDate.ofEpochDay(day).let { "%04d-%02d".format(it.year, it.monthValue) }

    /** [months] with [day] added to its month, if it had any use. */
    fun add(months: List<UsageMonth>, day: UseDay): List<UsageMonth> {
        if (day.total <= 0) return months
        val key = key(day.day)
        val month = months.firstOrNull { it.month == key } ?: UsageMonth(key)
        val added = month.copy(total = month.total + day.total, distracting = month.distracting + day.distractingTotal, days = month.days + 1)
        return (months.filterNot { it.month == key } + added).sortedBy { it.month }
    }

    /** refuge's own months: those [kept] from days gone by, and the [days] it still has. */
    fun own(kept: List<UsageMonth>, days: Collection<UseDay>): Map<String, UsageMonth> =
        days.sortedBy { it.day }.fold(kept) { months, day -> add(months, day) }.associateBy { it.month }

    /** One app's time in one of Android's monthly records, which run from [start] to [end]. */
    data class Bucket(val pkg: String, val start: Long, val end: Long, val seconds: Long)

    /**
     * Months from Android's monthly records, each counted in the month of its middle; [excluded] apps
     * left out, [distracting] ones summed apart. A month's days are those passed of it by [today].
     */
    fun fromBuckets(buckets: List<Bucket>, zone: java.time.ZoneId, excluded: Set<String>, distracting: Set<String>, today: java.time.LocalDate): Map<String, UsageMonth> =
        buckets.filter { it.pkg !in excluded && it.seconds > 0 }
            .groupBy { java.time.Instant.ofEpochMilli(it.start + (it.end - it.start) / 2).atZone(zone).toLocalDate() }
            .entries.groupBy({ java.time.YearMonth.from(it.key) }, { it.value }).mapValues { (_, lists) -> lists.flatten() }
            .entries.associate { (month, list) ->
                val key = "%04d-%02d".format(month.year, month.monthValue)
                val days = if (month == java.time.YearMonth.from(today)) today.dayOfMonth else month.lengthOfMonth()
                key to UsageMonth(key, list.sumOf { it.seconds }, list.filter { it.pkg in distracting }.sumOf { it.seconds }, days)
            }

    /** Android's months where it has them, refuge's own for the rest. */
    fun merge(fromAndroid: Map<String, UsageMonth>, own: Map<String, UsageMonth>): Map<String, UsageMonth> = own + fromAndroid

    /** The [count] months up to [current], oldest first, an empty one for any missing. */
    fun last(months: Map<String, UsageMonth>, current: java.time.YearMonth, count: Int = 6): List<UsageMonth> =
        (count - 1 downTo 0).map { back -> current.minusMonths(back.toLong()).let { "%04d-%02d".format(it.year, it.monthValue) } }
            .map { months[it] ?: UsageMonth(it) }
}
