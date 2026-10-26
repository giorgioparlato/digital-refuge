package app.bedtime.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.PhoneUsage
import app.bedtime.data.Repository
import app.bedtime.data.UseDay
import app.bedtime.service.UsageReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Recent days on the phone, and whether they're Android's record (usage access granted) or refuge's own count. */
data class UseDays(val days: Map<Long, UseDay>, val fromAndroid: Boolean, val distractingApps: Set<String>)

/**
 * The last [count] days of phone use, today included: Android's record when usage access is granted,
 * refuge's own count otherwise, so nothing here needs the permission. With [live], read again each minute.
 */
@Composable
fun rememberUseDays(count: Int, live: Boolean): UseDays {
    val context = LocalContext.current
    val repo = remember { Repository.get(context.applicationContext) }
    val usage by repo.usage.collectAsStateWithLifecycle(initialValue = emptyList())
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val distracting = settings?.distractingApps.orEmpty()
    var access by remember { mutableStateOf(UsageReader.hasAccess(context)) }
    LifecycleResumeEffect(Unit) {
        access = UsageReader.hasAccess(context)
        onPauseOrDispose { }
    }
    val android by produceState<Pair<Map<Long, UseDay>, Long>?>(null, access, distracting, count) {
        if (!access) {
            value = null
            return@produceState
        }
        val excluded = withContext(Dispatchers.IO) { UsageReader.excluded(context) }
        while (true) {
            value = withContext(Dispatchers.IO) { readAndroid(context, count, excluded, distracting) }
            if (!live) break
            delay(60_000)
        }
    }
    val own = PhoneUsage.fromRecords(usage)
    return android?.let { (days, first) -> UseDays(PhoneUsage.merge(days, own, first), true, distracting) } ?: UseDays(own, false, distracting)
}

/** Android's days, and the first of them its records reach back to (cut off, so not whole). Null without any. */
private fun readAndroid(context: android.content.Context, count: Int, excluded: Set<String>, distracting: Set<String>): Pair<Map<Long, UseDay>, Long>? {
    val zone = ZoneId.systemDefault()
    val firstDay = LocalDate.now(zone).toEpochDay() - count + 1
    val from = LocalDate.ofEpochDay(firstDay).atStartOfDay(zone).toInstant().toEpochMilli()
    val now = System.currentTimeMillis()
    // A little before the first day, to know what was already in front at midnight.
    val events = UsageReader.events(context, from - 2 * 3_600_000L, now)
    val earliest = events.minOfOrNull { it.at } ?: return null
    val earliestDay = Instant.ofEpochMilli(earliest).atZone(zone).toLocalDate().toEpochDay()
    return PhoneUsage.fromEvents(events, from, now, zone, excluded, distracting) to minOf(earliestDay, firstDay - 1)
}
