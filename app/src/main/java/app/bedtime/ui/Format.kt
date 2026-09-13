package app.bedtime.ui

import android.content.Context
import android.text.format.DateFormat
import app.bedtime.data.Schedule
import app.bedtime.data.UnlockConfig
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

fun is24Hour(context: Context): Boolean = runCatching { DateFormat.is24HourFormat(context) }.getOrDefault(true)

fun timeFormatter(context: Context): DateTimeFormatter =
    DateTimeFormatter.ofPattern(if (is24Hour(context)) "HH:mm" else "h:mm a", Locale.getDefault())

fun formatMinuteOfDay(context: Context, minuteOfDay: Int): String =
    timeFormatter(context).format(LocalTime.of(minuteOfDay / 60, minuteOfDay % 60))

fun formatTime(context: Context, epochMillis: Long): String =
    timeFormatter(context).format(Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()))

/** "today 22:00", "tomorrow 22:00" or "Mon 22:00". */
fun formatRelative(context: Context, epochMillis: Long): String {
    val dateTime = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
    val today = LocalDate.now()
    val time = timeFormatter(context).format(dateTime)
    return when (dateTime.toLocalDate()) {
        today -> "today at $time"
        today.plusDays(1) -> "tomorrow at $time"
        else -> "${dateTime.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())} at $time"
    }
}

fun formatDays(days: Set<Int>): String = when {
    days.size == 7 -> "Every day"
    days == (1..5).toSet() -> "Weekdays"
    days == setOf(6, 7) -> "Weekends"
    days.isEmpty() -> "No days picked"
    else -> days.sorted().joinToString(", ") { DayOfWeek.of(it).getDisplayName(TextStyle.SHORT, Locale.getDefault()) }
}

/** Length of one session in minutes; equal start and end means a full day. */
fun scheduleMinutes(schedule: Schedule): Int {
    val diff = schedule.endMinute - schedule.startMinute
    return if (diff > 0) diff else diff + 24 * 60
}

/** "45 min", "9 h", "8 h 30 min". */
fun formatMinutes(minutes: Long): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0L -> "$m min"
        m == 0L -> "$h h"
        else -> "$h h $m min"
    }
}

/** Rounds up to whole minutes, so "1 min" is shown until the very end. */
fun formatDuration(millis: Long): String = formatMinutes(((millis.coerceAtLeast(0) + 59_999) / 60_000))

fun formatCountdown(millis: Long): String {
    val total = (millis + 999) / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

fun pluralApps(count: Int): String = if (count == 1) "1 app" else "$count apps"

fun greeting(hour: Int): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..17 -> "Good afternoon"
    in 18..22 -> "Good evening"
    else -> "Good night"
}

fun unlockSummary(config: UnlockConfig): String = listOfNotNull(
    if (config.waitEnabled) "wait ${config.waitMinutes} min" else null,
    if (config.textEnabled) "type ${config.textLength}" else null,
    if (config.passwordEnabled && config.hasPassword) "password" else null,
).joinToString(" + ").let { if (it.isEmpty()) "instant unlock" else "unlock: $it" }
