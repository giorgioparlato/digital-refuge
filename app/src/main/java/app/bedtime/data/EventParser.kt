package app.bedtime.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

/** An event read from a line of text: what, and when. An all-day one runs from midnight to midnight. */
data class ParsedEvent(val title: String, val start: LocalDateTime, val end: LocalDateTime, val allDay: Boolean)

/**
 * Reads an event out of plain English: "dentist next friday 9", "dinner with sam tomorrow 7-9pm",
 * "trip 25 oct", "call mum in 3 days at 18:30 for 30 min". The words about when are taken out and the
 * rest is the title. Null when nothing in it says when, so an ordinary search never turns into one.
 */
object EventParser {
    private val opts = setOf(RegexOption.IGNORE_CASE)

    private const val MONTHS = "jan(?:uary)?|feb(?:ruary)?|mar(?:ch)?|apr(?:il)?|may|june?|july?|aug(?:ust)?|sep(?:t(?:ember)?)?|oct(?:ober)?|nov(?:ember)?|dec(?:ember)?"
    private const val WEEKDAYS = "monday|mon|tuesday|tues|tue|wednesday|wed|thursday|thurs|thur|thu|friday|fri|saturday|sat|sunday|sun"

    private val dayAfterTomorrow = Regex("""\b(?:the\s+)?day\s+after\s+tomorrow\b""", opts)
    private val tomorrow = Regex("""\b(?:tomorrow|tmrw)\b""", opts)
    private val today = Regex("""\btoday\b""", opts)
    private val tonight = Regex("""\btonight\b""", opts)
    private val inDays = Regex("""\bin\s+(\d{1,3})\s+days?\b""", opts)
    private val nextWeek = Regex("""\b(?:next\s+week|in\s+a\s+week)\b""", opts)
    private val weekday = Regex("""\b(?:(this|next|on)\s+)?($WEEKDAYS)\b""", opts)
    private val isoDate = Regex("""\b(\d{4})-(\d{1,2})-(\d{1,2})\b""", opts)
    private val slashDate = Regex("""\b(?:on\s+)?(\d{1,2})/(\d{1,2})(?:/(\d{2,4}))?\b""", opts)
    private val dayMonth = Regex("""\b(?:on\s+)?(\d{1,2})(?:st|nd|rd|th)?\s+(?:of\s+)?($MONTHS)\b""", opts)
    private val monthDay = Regex("""\b(?:on\s+)?($MONTHS)\s+(\d{1,2})(?:st|nd|rd|th)?\b""", opts)

    private const val CLOCK = """(\d{1,2})(?:[:.](\d{2}))?\s*(am|pm)?"""
    private val range = Regex("""\b(?:from\s+)?$CLOCK\s*(?:-|–|to|until|till)\s*$CLOCK\b""", opts)
    private val withColon = Regex("""\b(?:at\s+)?(\d{1,2})[:.](\d{2})\s*(am|pm)?\b""", opts)
    private val withMeridiem = Regex("""\b(?:at\s+)?(\d{1,2})\s*(am|pm)\b""", opts)
    private val withAt = Regex("""\bat\s+(\d{1,2})\b""", opts)
    private val bare = Regex("""(?<![\d:/.-])\b(\d{1,2})\b(?![\d:/.-])""", opts)
    private val partOfDay = Regex("""\b(?:at\s+|in\s+the\s+)?(noon|midday|midnight|morning|afternoon|evening)\b""", opts)
    private val duration = Regex("""\bfor\s+(?:(an?|half\s+an?)\s+(hour)|(\d+(?:\.\d+)?)\s*(h|hrs?|hours?|m|mins?|minutes?))\b""", opts)

    fun parse(text: String, now: LocalDateTime): ParsedEvent? {
        var rest = text
        fun take(match: MatchResult) {
            rest = rest.replaceRange(match.range, " ".repeat(match.value.length))
        }

        // When: the day first, then the time, each taken out of the text as it's found.
        val todayDate = now.toLocalDate()
        var date: LocalDate? = null
        var evening = false
        dayAfterTomorrow.find(rest)?.let { take(it); date = todayDate.plusDays(2) }
        if (date == null) tomorrow.find(rest)?.let { take(it); date = todayDate.plusDays(1) }
        if (date == null) tonight.find(rest)?.let { take(it); date = todayDate; evening = true }
        if (date == null) today.find(rest)?.let { take(it); date = todayDate }
        if (date == null) inDays.find(rest)?.let { take(it); date = todayDate.plusDays(it.groupValues[1].toLong()) }
        if (date == null) nextWeek.find(rest)?.let { take(it); date = todayDate.with(TemporalAdjusters.next(DayOfWeek.MONDAY)) }
        if (date == null) isoDate.find(rest)?.let { m ->
            dateOf(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())?.let { take(m); date = it }
        }
        if (date == null) slashDate.find(rest)?.let { m ->
            val year = m.groupValues[3].takeIf { it.isNotEmpty() }?.toInt()?.let { if (it < 100) 2000 + it else it }
            dated(m.groupValues[1].toInt(), m.groupValues[2].toInt(), year, todayDate)?.let { take(m); date = it }
        }
        if (date == null) dayMonth.find(rest)?.let { m ->
            dated(m.groupValues[1].toInt(), month(m.groupValues[2]), null, todayDate)?.let { take(m); date = it }
        }
        if (date == null) monthDay.find(rest)?.let { m ->
            dated(m.groupValues[2].toInt(), month(m.groupValues[1]), null, todayDate)?.let { take(m); date = it }
        }
        if (date == null) weekday.find(rest)?.let { m ->
            take(m)
            val day = weekdayOf(m.groupValues[2])
            date = if (m.groupValues[1].equals("next", ignoreCase = true)) {
                todayDate.with(TemporalAdjusters.next(DayOfWeek.MONDAY)).with(TemporalAdjusters.nextOrSame(day))
            } else {
                todayDate.with(TemporalAdjusters.nextOrSame(day))
            }
        }

        var start: LocalTime? = null
        var end: LocalTime? = null
        range.find(rest)?.let { m ->
            val g = m.groupValues
            // Like a bare number, a bare "2-3" is only a time next to a day ("buy 2-3 apples" isn't).
            val clearlyTime = date != null || m.value.trimStart().startsWith("from", ignoreCase = true) ||
                listOf(g[2], g[3], g[5], g[6]).any { it.isNotEmpty() }
            if (!clearlyTime) return@let
            val from = clock(g[1].toInt(), g[2], g[3].ifEmpty { g[6] }) ?: return@let
            var to = clock(g[4].toInt(), g[5], g[6]) ?: return@let
            if (!to.isAfter(from) && g[6].isEmpty() && to.hour < 12) to = to.plusHours(12)
            take(m)
            start = from
            end = to
        }
        if (start == null) withColon.find(rest)?.let { m -> clock(m.groupValues[1].toInt(), m.groupValues[2], m.groupValues[3])?.let { take(m); start = it } }
        if (start == null) withMeridiem.find(rest)?.let { m -> clock(m.groupValues[1].toInt(), "", m.groupValues[2])?.let { take(m); start = it } }
        if (start == null) withAt.find(rest)?.let { m -> clock(m.groupValues[1].toInt(), "", "")?.let { take(m); start = it } }
        if (start == null) partOfDay.find(rest)?.let { m ->
            take(m)
            start = when (m.groupValues[1].lowercase()) {
                "noon", "midday" -> LocalTime.NOON
                "midnight" -> LocalTime.MIDNIGHT
                "morning" -> LocalTime.of(9, 0)
                "afternoon" -> LocalTime.of(15, 0)
                else -> LocalTime.of(19, 0)
            }
        }
        // A number on its own is a time only next to a day ("dentist friday 9"), never "buy 2 apples".
        if (start == null && date != null) bare.find(rest)?.let { m -> clock(m.groupValues[1].toInt(), "", "")?.let { take(m); start = it } }
        if (start == null && evening) start = LocalTime.of(20, 0)

        var length: java.time.Duration? = null
        duration.find(rest)?.let { m ->
            take(m)
            length = when {
                m.groupValues[2].isNotEmpty() -> if (m.groupValues[1].startsWith("half", ignoreCase = true)) java.time.Duration.ofMinutes(30) else java.time.Duration.ofHours(1)
                m.groupValues[4].startsWith("h", ignoreCase = true) -> java.time.Duration.ofMinutes((m.groupValues[3].toDouble() * 60).toLong())
                else -> java.time.Duration.ofMinutes(m.groupValues[3].toDouble().toLong())
            }
        }

        if (date == null && start == null) return null
        val title = tidy(rest).ifEmpty { return null }
        val time = start
        if (time == null) {
            val day = date ?: todayDate
            return ParsedEvent(title, day.atStartOfDay(), day.plusDays(1).atStartOfDay(), allDay = true)
        }
        var day = date ?: todayDate
        var at = day.atTime(time)
        // A time already gone today means tomorrow, unless a day was named.
        if (date == null && at.isBefore(now)) {
            day = day.plusDays(1)
            at = day.atTime(time)
        }
        val finish = end?.let { day.atTime(it).let { e -> if (e.isAfter(at)) e else e.plusDays(1) } } ?: at.plus(length ?: java.time.Duration.ofHours(1))
        return ParsedEvent(title, at, finish, allDay = false)
    }

    /**
     * An hour as people say it: "18" or "6pm" are clear; on its own, 7 to 11 is the morning and 1 to 6
     * the afternoon or evening, as nobody books the dentist for 3am.
     */
    private fun clock(hour: Int, minutes: String, meridiem: String): LocalTime? {
        val minute = minutes.ifEmpty { "0" }.toInt()
        if (minute > 59) return null
        val h = when (meridiem.lowercase()) {
            "am" -> if (hour == 12) 0 else hour
            "pm" -> if (hour == 12) 12 else hour + 12
            else -> if (hour in 1..6) hour + 12 else hour
        }
        if (h !in 0..23 || (meridiem.isNotEmpty() && hour !in 1..12)) return null
        return LocalTime.of(h, minute)
    }

    /** [day]/[month] of [year], or of this year, or next year if that's already gone. */
    private fun dated(day: Int, month: Int, year: Int?, today: LocalDate): LocalDate? {
        if (year != null) return dateOf(year, month, day)
        val thisYear = dateOf(today.year, month, day) ?: return null
        return if (thisYear.isBefore(today)) dateOf(today.year + 1, month, day) else thisYear
    }

    private fun dateOf(year: Int, month: Int, day: Int): LocalDate? = runCatching { LocalDate.of(year, month, day) }.getOrNull()

    private fun month(name: String): Int = when (name.lowercase().take(3)) {
        "jan" -> 1; "feb" -> 2; "mar" -> 3; "apr" -> 4; "may" -> 5; "jun" -> 6
        "jul" -> 7; "aug" -> 8; "sep" -> 9; "oct" -> 10; "nov" -> 11; else -> 12
    }

    private fun weekdayOf(name: String): DayOfWeek = when (name.lowercase().take(3)) {
        "mon" -> DayOfWeek.MONDAY; "tue" -> DayOfWeek.TUESDAY; "wed" -> DayOfWeek.WEDNESDAY; "thu" -> DayOfWeek.THURSDAY
        "fri" -> DayOfWeek.FRIDAY; "sat" -> DayOfWeek.SATURDAY; else -> DayOfWeek.SUNDAY
    }

    /** What's left once the when is gone: no doubled spaces, no stray "at", "on" or dashes at the ends. */
    private fun tidy(rest: String): String {
        val words = rest.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.toMutableList()
        val loose = setOf("at", "on", "from", "for", "by", "in", "-", "–", ",", "the")
        while (words.isNotEmpty() && words.last().lowercase() in loose) words.removeAt(words.lastIndex)
        while (words.isNotEmpty() && words.first().lowercase() in loose) words.removeAt(0)
        return words.joinToString(" ").trim(' ', ',', '-', '–')
    }
}
