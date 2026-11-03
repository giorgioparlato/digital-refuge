package app.bedtime.data

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** The moods tapped, over time: how often each came up in a month, when in the day, and each day's most. */
object Moods {
    /** Every mood tapped in [month], from the microjournal's entries. */
    fun inMonth(entries: List<JournalEntry>, month: YearMonth, zone: ZoneId): List<MoodMark> =
        entries.flatMap { it.moods }.filter { YearMonth.from(Instant.ofEpochMilli(it.at).atZone(zone)) == month }

    /** How often each mood came up, most first. */
    fun counts(marks: List<MoodMark>): List<Pair<String, Int>> =
        marks.groupingBy { it.mood }.eachCount().toList().sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })

    /** Each day's mood, the one tapped most that day; on a tie, the latest of them. */
    fun byDay(marks: List<MoodMark>, zone: ZoneId): Map<LocalDate, String> =
        marks.groupBy { Instant.ofEpochMilli(it.at).atZone(zone).toLocalDate() }.mapValues { (_, day) ->
            val counts = day.groupingBy { it.mood }.eachCount()
            val most = counts.values.max()
            day.filter { counts[it.mood] == most }.maxBy { it.at }.mood
        }

    /** "mornings", "afternoons", "evenings" or "nights": when in the day [hour] falls. */
    fun partOfDay(hour: Int): String = when (hour) {
        in 5..11 -> "mornings"
        in 12..16 -> "afternoons"
        in 17..21 -> "evenings"
        else -> "nights"
    }

    /** When in the day [mood] came up most, if it came up at least twice. */
    fun mostly(marks: List<MoodMark>, mood: String, zone: ZoneId): String? =
        marks.filter { it.mood == mood }.takeIf { it.size >= 2 }
            ?.groupingBy { partOfDay(Instant.ofEpochMilli(it.at).atZone(zone).hour) }?.eachCount()
            ?.maxByOrNull { it.value }?.key

    private fun dayOf(at: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(at).atZone(zone).toLocalDate()

    /** Every mood tapped from [from] to [to], both days included. */
    fun between(entries: List<JournalEntry>, from: LocalDate, to: LocalDate, zone: ZoneId): List<MoodMark> =
        entries.flatMap { it.moods }.filter { dayOf(it.at, zone) in from..to }

    /** Whether [line] names [mood] among its parts ("- 09:10 · tired, bored · …"). */
    private fun names(line: String, mood: String): Boolean =
        line.startsWith("- ") && line.split(" · ").drop(1).any { part -> part.split(", ").any { it.trim() == mood } }

    /** The lines that name [mood] in [month], by day, the newest day first, each without its "- ". */
    fun linesWith(entries: List<JournalEntry>, month: YearMonth, mood: String): List<Pair<LocalDate, String>> =
        entries.filter { YearMonth.from(LocalDate.ofEpochDay(it.day)) == month }.sortedByDescending { it.day }
            .flatMap { e -> e.text.lines().filter { names(it, mood) }.map { LocalDate.ofEpochDay(e.day) to it.removePrefix("- ") } }

    /** How often [mood] came up on each weekday at each part of the day ([partOfDay]). */
    fun grid(marks: List<MoodMark>, mood: String, zone: ZoneId): Map<Pair<java.time.DayOfWeek, String>, Int> =
        marks.filter { it.mood == mood }.groupingBy { Instant.ofEpochMilli(it.at).atZone(zone).let { t -> t.dayOfWeek to partOfDay(t.hour) } }.eachCount()

    /** What came after [mood] when "why?" asked: put down how often, of how many, and the app gone on to most. */
    data class Next(val putDown: Int, val of: Int, val app: String?)

    fun next(marks: List<MoodMark>, mood: String): Next? {
        val asked = marks.filter { it.mood == mood && it.wentOn != null }.takeIf { it.size >= 2 } ?: return null
        val app = asked.filter { it.wentOn == true && it.from != null && it.from != UNLOCKING }
            .groupingBy { it.from!! }.eachCount().maxByOrNull { it.value }?.key
        return Next(asked.count { it.wentOn == false }, asked.size, app)
    }

    /** What "why?" after an unlock is asked about. */
    const val UNLOCKING = "unlocking"

    /**
     * Seconds in distracting apps on an average day with [mood], and on the others with any use, over
     * [days]; null until each side has at least two days.
     */
    fun useOnDays(marks: List<MoodMark>, days: Map<Long, UseDay>, mood: String, zone: ZoneId): Pair<Long, Long>? {
        val moodDays = marks.filter { it.mood == mood }.map { dayOf(it.at, zone).toEpochDay() }.toSet()
        val used = days.values.filter { it.total > 0 }
        val with = used.filter { it.day in moodDays }
        val without = used.filter { it.day !in moodDays }
        if (with.size < 2 || without.size < 2) return null
        return with.sumOf { it.distractingTotal } / with.size to without.sumOf { it.distractingTotal } / without.size
    }

    /** The moods "why?" after an unlock heard most, and those of what you wrote yourself, most first, three of each. */
    fun whereFrom(marks: List<MoodMark>): Pair<List<String>, List<String>> {
        fun top(list: List<MoodMark>) = counts(list).filter { it.second >= 2 }.take(3).map { it.first }
        return top(marks.filter { it.from == UNLOCKING }) to top(marks.filter { it.from == null && !it.after })
    }

    /** How a mood turned up on some days against the rest: [with] of [days], against [otherWith] of [others]. */
    data class Odds(val mood: String, val with: Int, val days: Int, val otherWith: Int, val others: Int)

    /** Those of [odds] clearly more likely on the days in question, from at least three days on each side. */
    private fun clear(odds: List<Odds>): List<Odds> = odds.filter {
        it.days >= 3 && it.others >= 3 && it.with >= 2 && it.with.toFloat() / it.days - it.otherWith.toFloat() / it.others >= 0.25f
    }.sortedByDescending { it.with.toFloat() / it.days }

    private fun odds(marks: List<MoodMark>, these: Set<LocalDate>, rest: Set<LocalDate>, zone: ZoneId, keep: (MoodMark) -> Boolean = { true }): List<Odds> {
        val byDay = marks.filter(keep).groupBy({ dayOf(it.at, zone) }, { it.mood })
        return marks.map { it.mood }.distinct().map { mood ->
            Odds(mood, these.count { mood in byDay[it].orEmpty() }, these.size, rest.count { mood in byDay[it].orEmpty() }, rest.size)
        }
    }

    /** At least this long on the phone between 23:00 and 5:00 makes a late night. */
    const val LATE_SECONDS = 5 * 60L

    /**
     * The morning moods (before noon) after a late night on the phone against after the others, of the
     * days you tapped any mood at all. A night is the evening before's last hour and the small hours.
     */
    fun afterLateNights(marks: List<MoodMark>, days: Map<Long, UseDay>, zone: ZoneId): List<Odds> {
        val tapped = marks.map { dayOf(it.at, zone) }.toSet()
        val known = tapped.filter { days.containsKey(it.toEpochDay()) && days.containsKey(it.toEpochDay() - 1) }
        val late = known.filter { d ->
            val night = (days[d.toEpochDay() - 1]?.all?.get(23) ?: 0L) + (0..4).sumOf { h -> days[d.toEpochDay()]?.all?.get(h) ?: 0L }
            night >= LATE_SECONDS
        }.toSet()
        val morning = { m: MoodMark -> Instant.ofEpochMilli(m.at).atZone(zone).hour < 12 }
        return clear(odds(marks, late, known.toSet() - late, zone, morning))
    }

    /** For each habit, the moods of the days you kept it against those you didn't, of the days you tapped any. */
    fun withHabits(marks: List<MoodMark>, habits: List<Habit>, habitMarks: Map<String, Map<Long, Int>>, zone: ZoneId): List<Pair<Habit, Odds>> {
        val tapped = marks.map { dayOf(it.at, zone) }.toSet()
        return habits.flatMap { habit ->
            val kept = tapped.filter { Habits.day(habit, habitMarks[habit.id].orEmpty(), it.toEpochDay()).let { s -> s == HabitDay.DONE || s == HabitDay.MET } }.toSet()
            clear(odds(marks, kept, tapped - kept, zone)).map { habit to it }
        }
    }

    /** How you felt once a stretch on the phone was over, most first. */
    fun afterwards(marks: List<MoodMark>): List<Pair<String, Int>> = counts(marks.filter { it.after })
}
