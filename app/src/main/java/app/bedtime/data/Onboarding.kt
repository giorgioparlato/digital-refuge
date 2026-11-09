package app.bedtime.data

import android.content.pm.ApplicationInfo

/** Setup's steps, in their order; everyone goes through all of them. The [optional] ones can be left for now. */
enum class SetupStep(val optional: Boolean) {
    APPS(true), BUDGET(true), BEDTIME(true), LIMITS(true), ALWAYS(true), PERMISSION(false), HOME(false), DONE(false),
}

/**
 * A day's choice for one of your worst apps: nothing, a pause before it opens, or minutes a day. The
 * same ones are a limit's starting points: one fills the rules in, which stay yours to change.
 */
enum class LimitChoice(val label: String, val minutes: Int?) {
    NONE("None", null),
    PAUSE("A pause", null),
    FIFTEEN("15 min a day", 15),
    THIRTY("30 min a day", 30),
    HOUR("1 h a day", 60),
    ;

    /** [rules] with this choice's pause or minutes, and nothing else daily; the apps and the rest kept. */
    fun applied(rules: LimitRules): LimitRules = when (this) {
        NONE -> rules.copy(pauseSeconds = null, dailyMinutes = null, dailyOpens = null)
        PAUSE -> rules.copy(pauseSeconds = 10, pauseKind = PauseKind.WAIT, pauseGrowth = 0, dailyMinutes = null, dailyOpens = null)
        else -> rules.copy(pauseSeconds = null, dailyMinutes = minutes, dailyOpens = null)
    }

    companion object {
        /** The starting points a limit's page offers. */
        val starts: List<LimitChoice> get() = entries - NONE

        /** The one [rules] is; null when it's your own. */
        fun of(rules: LimitRules): LimitChoice? = starts.firstOrNull { it.applied(rules) == rules }
    }
}

/** An app setup can offer: its name, how long it took this week, and whether Android files it as a feed or a game. */
data class SetupApp(val pkg: String, val label: String, val weekSeconds: Long, val pulling: Boolean)

object Onboarding {
    /** How many of your worst apps setup offers limits for. */
    const val LIMIT_APPS = 3

    /** How many of this week's most used apps the distracting step lists before "all apps". */
    const val TOP_APPS = 12

    /** Android's own categories for the apps that pull you in: social, video, games and news. */
    fun pulling(category: Int): Boolean = category in setOf(
        ApplicationInfo.CATEGORY_SOCIAL,
        ApplicationInfo.CATEGORY_VIDEO,
        ApplicationInfo.CATEGORY_GAME,
        ApplicationInfo.CATEGORY_NEWS,
    )

    /** The ones ticked to start with: the well-known feeds, and those Android files as pulling. */
    fun suggestedDistracting(apps: List<SetupApp>): Set<String> =
        apps.filter { it.pulling || it.pkg in Templates.DISTRACTIONS }.map { it.pkg }.toSet()

    /** The apps the distracting step lists: this week's most used, and any suggested or already chosen; most used first. */
    fun candidates(apps: List<SetupApp>, chosen: Set<String>): List<SetupApp> {
        val top = apps.filter { it.weekSeconds >= 60 }.sortedByDescending { it.weekSeconds }.take(TOP_APPS).map { it.pkg }.toSet()
        val suggested = suggestedDistracting(apps)
        return apps.filter { it.pkg in top || it.pkg in suggested || it.pkg in chosen }
            .sortedWith(compareByDescending<SetupApp> { it.weekSeconds }.thenBy { it.label.lowercase() })
    }

    /** A budget a little under what you do now: four fifths of it, down to a quarter hour, at least 15 minutes. */
    fun suggestedBudget(averageMinutes: Int, fallback: Int): Int =
        if (averageMinutes <= 0) fallback else ((averageMinutes * 4 / 5) / 15 * 15).coerceAtLeast(15)

    /** Your worst chosen apps, for limits: by this week's time, or as listed without it. */
    fun worst(apps: List<SetupApp>, chosen: Set<String>): List<SetupApp> =
        apps.filter { it.pkg in chosen }.sortedByDescending { it.weekSeconds }.take(LIMIT_APPS)

    /** A limit of its own for [app], as chosen; null for none. */
    fun limit(app: SetupApp, choice: LimitChoice, today: Long): AppLimit? =
        if (choice == LimitChoice.NONE) null else AppLimit(name = app.label, rules = choice.applied(LimitRules(apps = setOf(app.pkg))), createdDay = today)

    /**
     * The bedtime: [existing] changed, or the template's, from [start] to [end], shutting [apps], with
     * greyscale, the quiet home screen and silence as chosen. It shuts those apps by name, whatever
     * the template does; the quiet home screen is only how the phone looks.
     */
    fun bedtime(existing: Schedule?, start: Int, end: Int, apps: Set<String>, greyscale: Boolean, minimal: Boolean, quiet: Boolean): Schedule =
        (existing ?: Templates.all.first { it.key == "bedtime" }.build { false }).copy(
            startMinute = start,
            endMinute = end,
            blockedApps = apps,
            allowOnly = false,
            greyscale = greyscale,
            minimalMode = minimal,
            dnd = if (quiet) DndMode.PRIORITY else DndMode.OFF,
            hideNotifications = quiet,
            enabled = true,
        )

    /** A bedtime made before, by setup or by hand, to change rather than add another. */
    fun existingBedtime(schedules: List<Schedule>): Schedule? =
        schedules.firstOrNull { it.kind == ScheduleKind.RECURRING && it.name.equals("bedtime", ignoreCase = true) }
}
