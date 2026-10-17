package app.bedtime.ui.minimal

import android.Manifest
import android.content.ContentUris
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
import app.bedtime.ui.formatTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId

/** One timed event from the phone's calendar, as epoch millis. */
data class CalendarEvent(val title: String, val begin: Long, val end: Long)

/**
 * The calendar already on the phone, as your calendar apps have synced it. Nothing is fetched: the
 * app has no internet permission, so this reads what's there and nothing more.
 */
object CalendarSource {
    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    /** Timed events still to come or under way between [now] and [until], soonest first; declined ones left out. */
    fun upcoming(context: Context, now: Long, until: Long): List<CalendarEvent> {
        if (!hasPermission(context)) return emptyList()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also {
            ContentUris.appendId(it, now)
            ContentUris.appendId(it, until)
        }.build()
        val projection = arrayOf(CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.END)
        val selection = "${CalendarContract.Instances.ALL_DAY} = 0 AND ${CalendarContract.Instances.VISIBLE} = 1 AND " +
            "${CalendarContract.Instances.SELF_ATTENDEE_STATUS} != ${CalendarContract.Attendees.ATTENDEE_STATUS_DECLINED}"
        return runCatching {
            context.contentResolver.query(uri, projection, selection, null, "${CalendarContract.Instances.BEGIN} ASC")?.use { c ->
                buildList {
                    while (c.moveToNext()) add(CalendarEvent(c.getString(0).orEmpty(), c.getLong(1), c.getLong(2)))
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }
}

/**
 * The line under the date: what's on now, or what's next, today. With [showNames] off — the lock
 * screen, unless you said otherwise — it says when, not what.
 */
fun calendarLine(events: List<CalendarEvent>, now: Long, showNames: Boolean, time: (Long) -> String): String {
    val current = events.firstOrNull { it.begin <= now && it.end > now }
    if (current != null) {
        return if (showNames && current.title.isNotBlank()) "now · ${current.title} until ${time(current.end)}" else "an event until ${time(current.end)}"
    }
    val next = events.firstOrNull { it.begin > now } ?: return "nothing else today"
    return if (showNames && next.title.isNotBlank()) "next · ${next.title} at ${time(next.begin)}" else "next event at ${time(next.begin)}"
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
