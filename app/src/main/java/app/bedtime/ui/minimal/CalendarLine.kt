package app.bedtime.ui.minimal

import android.Manifest
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import app.bedtime.data.HomeStyle
import app.bedtime.data.ParsedEvent
import app.bedtime.ui.formatTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.ZoneId

/** One event from the phone's calendar, as epoch millis. An all-day one's times are UTC midnights, as Android keeps them. */
data class CalendarEvent(val title: String, val begin: Long, val end: Long, val allDay: Boolean = false)

/**
 * The calendar already on the phone, as your calendar apps have synced it. Nothing is fetched: the
 * app has no internet permission, so this reads what's there and nothing more.
 */
object CalendarSource {
    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun canWrite(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** Calendars that can take a new event, as (id, name), the phone's primary ones first. */
    fun writableCalendars(context: Context): List<Pair<Long, String>> {
        if (!hasPermission(context)) return emptyList()
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.IS_PRIMARY)
        val selection = "${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ${CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR} AND " +
            "${CalendarContract.Calendars.VISIBLE} = 1"
        return runCatching {
            context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, selection, null, null)?.use { c ->
                buildList { while (c.moveToNext()) add(Triple(c.getLong(0), c.getString(1).orEmpty(), c.getInt(2) == 1)) }
            }.orEmpty().sortedByDescending { it.third }.map { it.first to it.second }
        }.getOrDefault(emptyList())
    }

    /** Saves [event] into calendar [calendarId], or the first that can take it. False if it couldn't. */
    fun insert(context: Context, event: ParsedEvent, calendarId: Long?): Boolean {
        if (!canWrite(context)) return false
        val id = calendarId?.takeIf { want -> writableCalendars(context).any { it.first == want } } ?: writableCalendars(context).firstOrNull()?.first ?: return false
        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, id)
            put(CalendarContract.Events.TITLE, event.title)
            if (event.allDay) {
                // All-day events are kept as UTC midnights.
                put(CalendarContract.Events.DTSTART, event.start.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                put(CalendarContract.Events.DTEND, event.end.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
                put(CalendarContract.Events.ALL_DAY, 1)
                put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
            } else {
                val zone = ZoneId.systemDefault()
                put(CalendarContract.Events.DTSTART, event.start.atZone(zone).toInstant().toEpochMilli())
                put(CalendarContract.Events.DTEND, event.end.atZone(zone).toInstant().toEpochMilli())
                put(CalendarContract.Events.EVENT_TIMEZONE, zone.id)
            }
        }
        return runCatching { context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values) != null }.getOrDefault(false)
    }

    /**
     * Events still to come or under way between [now] and [until], soonest first; declined ones left
     * out, and all-day ones too unless [allDay].
     */
    fun upcoming(context: Context, now: Long, until: Long, allDay: Boolean = false): List<CalendarEvent> {
        if (!hasPermission(context)) return emptyList()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, now)
            ContentUris.appendId(it, until)
        }.build()
        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
        )
        val timedOnly = if (allDay) "" else "${CalendarContract.Instances.ALL_DAY} = 0 AND "
        val selection = timedOnly + "${CalendarContract.Instances.VISIBLE} = 1 AND " +
            "${CalendarContract.Instances.SELF_ATTENDEE_STATUS} != ${CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED}"
        return runCatching {
            context.contentResolver.query(uri, projection, selection, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
                buildList {
                    while (c.moveToNext()) add(CalendarEvent(c.getString(0).orEmpty(), c.getLong(1), c.getLong(2), c.getInt(3) == 1))
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }
}

/**
 * The line under the date: what's on now, or what's next, today — or null with nothing to come, so the
 * line simply isn't there. With [showNames] off (the lock screen, unless you said otherwise) it says
 * when, not what.
 */
fun calendarLine(events: List<CalendarEvent>, now: Long, showNames: Boolean, time: (Long) -> String): String? {
    val current = events.firstOrNull { it.begin <= now && it.end > now }
    if (current != null) {
        return if (showNames && current.title.isNotBlank()) "now · ${current.title} until ${time(current.end)}" else "an event until ${time(current.end)}"
    }
    val next = events.firstOrNull { it.begin > now } ?: return null
    return if (showNames && next.title.isNotBlank()) "next · ${next.title} at ${time(next.begin)}" else "next event at ${time(next.begin)}"
}

/** A day of the week ahead: its label ("today", "tomorrow", "wed 28 oct") and its events, all-day ones first. */
data class CalendarDay(val label: String, val events: List<CalendarEvent>)

/**
 * The events from [today] for [days] days, a group per day that has any. An all-day event belongs to
 * every day it covers; a timed one to the day it starts in [zone].
 */
fun calendarWeek(events: List<CalendarEvent>, today: LocalDate, zone: ZoneId, days: Int = 7): List<CalendarDay> =
    (0 until days).mapNotNull { offset ->
        val day = today.plusDays(offset.toLong())
        val start = day.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val (allDay, timed) = events.partition { it.allDay }
        val onDay = allDay.filter { it.begin < start + DAY_MS && it.end > start } +
            timed.filter { Instant.ofEpochMilli(it.begin).atZone(zone).toLocalDate() == day }.sortedBy { it.begin }
        if (onDay.isEmpty()) {
            null
        } else {
            val label = when (offset) {
                0 -> "Today"
                1 -> "Tomorrow"
                else -> DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()).format(day)
            }
            CalendarDay(label, onDay)
        }
    }

private const val DAY_MS = 86_400_000L

/** The next seven days of events, read when asked for; empty without calendar access. */
suspend fun loadCalendarWeek(context: Context, now: LocalDateTime): List<CalendarDay> {
    if (!CalendarSource.hasPermission(context)) return emptyList()
    val zone = ZoneId.systemDefault()
    val start = now.atZone(zone).toInstant().toEpochMilli()
    // A day's margin each side, so all-day events (kept in UTC) are caught whatever the time zone.
    val end = now.toLocalDate().plusDays(8).atStartOfDay(zone).toInstant().toEpochMilli()
    val events = withContext(Dispatchers.IO) { CalendarSource.upcoming(context, start - DAY_MS, end, allDay = true) }
        .filter { it.end > start }
    return calendarWeek(events, now.toLocalDate(), zone)
}

/** The calendar line for [style], refreshed each minute; null when it's switched off or not allowed. */
@Composable
fun rememberCalendarLine(style: HomeStyle, now: LocalDateTime, onLockScreen: Boolean): String? {
    val context = LocalContext.current
    var line by remember { mutableStateOf<String?>(null) }
    val minute = now.withSecond(0).withNano(0)
    LaunchedEffect(style.showCalendar, style.calendarNamesOnLock, onLockScreen, minute) {
        if (!style.showCalendar || !CalendarSource.hasPermission(context)) {
            line = null
            return@LaunchedEffect
        }
        val zone = ZoneId.systemDefault()
        val start = now.atZone(zone).toInstant().toEpochMilli()
        val midnight = now.toLocalDate().plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val events = withContext(Dispatchers.IO) { CalendarSource.upcoming(context, start, midnight) }
        line = calendarLine(events, start, showNames = !onLockScreen || style.calendarNamesOnLock) { formatTime(context, it) }
    }
    return if (style.showCalendar) line else null
}
