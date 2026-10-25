package app.bedtime.data

import android.content.Intent
import android.provider.AlarmClock

/** An alarm or a timer, read from the search: "alarm 7:30", "wake me at 6:45", "timer 10 min". */
sealed interface ClockAction {
    data class Alarm(val hour: Int, val minute: Int, val label: String) : ClockAction
    data class Timer(val seconds: Int, val label: String) : ClockAction
}

object ClockActions {
    private val alarm = Regex("""^(?:alarm|wake(?:\s+me)?(?:\s+up)?)(?:\s+(?:at|for))?\s+(\d{1,2})(?:[:.](\d{2}))?\s*(am|pm)?(?:\s+(.*))?$""", RegexOption.IGNORE_CASE)
    private val timer = Regex("""^timer(?:\s+for)?\s+(.+)$""", RegexOption.IGNORE_CASE)
    private val part = Regex("""(\d+(?:\.\d+)?)\s*(?:(hours?|hrs?|h|minutes?|mins?|m|seconds?|secs?|s)(?![a-z]))?\s*""", RegexOption.IGNORE_CASE)

    /** What [text] asks of the clock, or null if it isn't an alarm or a timer. */
    fun parse(text: String): ClockAction? {
        val t = text.trim()
        alarm.matchEntire(t)?.let { m ->
            var hour = m.groupValues[1].toInt()
            val minute = m.groupValues[2].ifEmpty { "0" }.toInt()
            when (m.groupValues[3].lowercase()) {
                "am" -> if (hour == 12) hour = 0
                "pm" -> if (hour < 12) hour += 12
            }
            if (hour !in 0..23 || minute !in 0..59) return null
            return ClockAction.Alarm(hour, minute, m.groupValues[4].trim())
        }
        timer.matchEntire(t)?.let { m ->
            val rest = m.groupValues[1]
            // "10", "10 min", "1h 30", "90s": a bare number is minutes, or seconds right after minutes.
            var seconds = 0.0
            var end = 0
            var unit = ""
            while (true) {
                val p = part.find(rest, end)?.takeIf { it.range.first == end && it.value.isNotEmpty() } ?: break
                val next = p.groupValues[2].lowercase()
                seconds += p.groupValues[1].toDouble() * when {
                    next.startsWith("h") -> 3600
                    next.startsWith("s") || (next.isEmpty() && unit.startsWith("m")) -> 1
                    else -> 60
                }
                unit = next
                end = p.range.last + 1
            }
            if (seconds < 1) return null
            return ClockAction.Timer(seconds.toInt(), rest.substring(end).trim())
        }
        return null
    }

    /** "set an alarm for 07:30", "start a 10 min timer". */
    fun label(action: ClockAction, time: (Int, Int) -> String): String = when (action) {
        is ClockAction.Alarm -> "set an alarm for ${time(action.hour, action.minute)}" + if (action.label.isNotEmpty()) " · ${action.label}" else ""
        is ClockAction.Timer -> "start a ${duration(action.seconds)} timer" + if (action.label.isNotEmpty()) " · ${action.label}" else ""
    }

    private fun duration(seconds: Int): String {
        val h = seconds / 3600
        val m = seconds % 3600 / 60
        val s = seconds % 60
        return listOfNotNull(h.takeIf { it > 0 }?.let { "$it h" }, m.takeIf { it > 0 }?.let { "$it min" }, s.takeIf { it > 0 }?.let { "$it s" }).joinToString(" ")
    }

    /** Your clock app sets it without opening, where it allows that. */
    fun intent(action: ClockAction): Intent = when (action) {
        is ClockAction.Alarm -> Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_HOUR, action.hour)
            .putExtra(AlarmClock.EXTRA_MINUTES, action.minute)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            .apply { if (action.label.isNotEmpty()) putExtra(AlarmClock.EXTRA_MESSAGE, action.label) }
        is ClockAction.Timer -> Intent(AlarmClock.ACTION_SET_TIMER)
            .putExtra(AlarmClock.EXTRA_LENGTH, action.seconds)
            .putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            .apply { if (action.label.isNotEmpty()) putExtra(AlarmClock.EXTRA_MESSAGE, action.label) }
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
