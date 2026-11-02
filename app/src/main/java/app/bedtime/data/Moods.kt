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
}
