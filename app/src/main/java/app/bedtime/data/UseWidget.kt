package app.bedtime.data

import kotlinx.serialization.Serializable

/** How the distraction tracker looks: the hour dots, a line, a budget bar, or the apps that took the most. */
@Serializable
enum class UseLook { DOTS, LINE, BUDGET, APPS, APPS_LINE }

/** The distraction tracker on one screen. */
@Serializable
data class UseWidget(
    val look: UseLook = UseLook.DOTS,
    /** In the line, and under the dots: time on the phone at all, and time in distracting apps. */
    val total: Boolean = false,
    val distracting: Boolean = false,
    /** Today's unlocks, in any look. */
    val unlocks: Boolean = false,
    /** The budget counts the whole phone, not only distracting apps. */
    val budgetWholePhone: Boolean = false,
    /** The day's allowance in distracting apps, and on the whole phone, in minutes. */
    val budgetMinutes: Int = 60,
    val phoneBudgetMinutes: Int = 180,
)

/** Today so far, as the tracker tells it: seconds on the phone and in distracting apps, unlocks if known, and each distracting app's seconds. */
data class UseToday(
    val total: Long,
    val distracting: Long,
    val unlocks: Int?,
    val apps: Map<String, Long> = emptyMap(),
    /** Every app's seconds with usage access; only the distracting ones without it. */
    val allApps: Map<String, Long> = apps,
)

/** The budget bar's words, its note at the right ("20 min left", "25m over"), and how full it is. */
data class UseBudget(val line: String, val note: String, val filled: Float, val over: Boolean)

object UseWords {
    /** "38 min", "1h", "3h 40m". */
    fun short(seconds: Long): String {
        val minutes = seconds / 60
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0L -> "$m min"
            m == 0L -> "${h}h"
            else -> "${h}h ${m}m"
        }
    }

    /** The allowances to choose from: for distracting apps, and for the whole phone. */
    val DISTRACTING_BUDGETS = listOf(15, 30, 45, 60, 90, 120)
    val PHONE_BUDGETS = listOf(30, 60, 90, 120, 180, 240, 300)

    /** An allowance on a narrow chip: "45m", "1h", "1½h". */
    fun compact(minutes: Int): String = when {
        minutes < 60 -> "${minutes}m"
        minutes % 60 == 0 -> "${minutes / 60}h"
        minutes % 60 == 30 -> "${minutes / 60}½h"
        else -> "${minutes / 60}h ${minutes % 60}m"
    }

    fun unlocks(count: Int): String = if (count == 1) "1 unlock" else "$count unlocks"

    private fun figures(widget: UseWidget, today: UseToday): List<String> = buildList {
        if (widget.total) add("${short(today.total)} on the phone")
        if (widget.distracting) add("${short(today.distracting)} distracting")
        if (widget.unlocks && today.unlocks != null) add(unlocks(today.unlocks))
    }

    /** The line: the figures chosen, or the distracting time with none; a single one says "today". */
    fun line(widget: UseWidget, today: UseToday): String {
        val parts = figures(widget, today).ifEmpty { listOf("${short(today.distracting)} distracting") }
        return if (parts.size == 1) "${parts.single()} today" else parts.joinToString(" · ")
    }

    /** The small line under the dots: the figures chosen, or null with none. */
    fun underDots(widget: UseWidget, today: UseToday): String? = figures(widget, today).takeIf { it.isNotEmpty() }?.joinToString(" · ")

    /** The budget: an allowance up to an hour is told in minutes ("40 of 60 min"), a longer one in hours. */
    fun budget(widget: UseWidget, today: UseToday): UseBudget {
        val used = if (widget.budgetWholePhone) today.total else today.distracting
        val allowed = (if (widget.budgetWholePhone) widget.phoneBudgetMinutes else widget.budgetMinutes).coerceAtLeast(1) * 60L
        val amount = if (allowed <= 3_600L) "${used / 60} of ${allowed / 60} min" else "${short(used)} of ${short(allowed)}"
        val over = used > allowed
        return UseBudget(
            line = amount + if (widget.budgetWholePhone) " on the phone" else " distracting",
            note = if (over) "${short(used - allowed)} over" else "${short(allowed - used)} left",
            filled = (used.toFloat() / allowed).coerceIn(0f, 1f),
            over = over,
        )
    }

    /** Every app today, most first, a minute or more each. */
    fun ranked(today: UseToday): List<Pair<String, Long>> = today.allApps.filterValues { it >= 60 }.toList().sortedByDescending { it.second }

    /** The distracting apps that took the most today, a minute or more each. */
    fun top(today: UseToday, count: Int = 3): List<Pair<String, Long>> =
        today.apps.filterValues { it >= 60 }.toList().sortedByDescending { it.second }.take(count)

    /** All of it on one line: the total, unlocks if chosen, then the top apps by [label]. */
    fun appsLine(widget: UseWidget, today: UseToday, label: (String) -> String): String =
        (listOf(short(today.total)) + listOfNotNull(today.unlocks?.takeIf { widget.unlocks }?.let(::unlocks)) + top(today).map { (pkg, s) -> "${label(pkg)} ${short(s)}" })
            .joinToString(" · ")
}
