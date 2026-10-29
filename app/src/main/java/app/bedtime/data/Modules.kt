package app.bedtime.data

import kotlinx.serialization.Serializable

/** The pieces around the clock on the home and lock screens, each switched on or off, in your order. */
@Serializable
enum class Module(val label: String) {
    GREETING("Greeting"),
    CLOCK("Clock"),
    DATE("Date"),
    WEATHER("Weather"),
    EVENT("Today's next event"),
    HOURS("Distraction tracker"),
    QUOTE("Quote"),
    HABITS("Habits"),
    NOW_PLAYING("Now playing"),
    NOTE("Note"),
}

@Serializable
data class ModuleChoice(val module: Module, val on: Boolean = true)

/** Which of the two screens a list of [Module]s is for. */
enum class Screen { HOME, LOCK }

/**
 * Each screen's widgets. Until you change them they follow the separate switches there used to be
 * (greeting, date, weather, calendar, hours on each screen, quote), so nobody's screen changes on update.
 */
object Modules {
    fun of(style: HomeStyle, screen: Screen): List<ModuleChoice> =
        arrange((if (screen == Screen.HOME) style.homeModules else style.lockModules) ?: fromSwitches(style, screen))

    /** Those switched on, in order. */
    fun shown(style: HomeStyle, screen: Screen): List<Module> = of(style, screen).filter { it.on }.map { it.module }

    fun isOn(style: HomeStyle, screen: Screen, module: Module): Boolean = module in shown(style, screen)

    /** The clock and what sits with it; the session pill comes after the last of these. */
    val CLOCK_GROUP = setOf(Module.GREETING, Module.CLOCK, Module.DATE, Module.WEATHER, Module.EVENT)

    fun onAnywhere(style: HomeStyle, module: Module): Boolean = Screen.entries.any { isOn(style, it, module) }

    /**
     * [style] as the screens show it while [sessions] (start, schedule) run: each screen's widgets from
     * the session that started last and has its own for it, otherwise your usual ones.
     */
    fun during(style: HomeStyle, sessions: List<Pair<Long, Schedule>>): HomeStyle {
        val latest = sessions.sortedByDescending { it.first }.map { it.second }
        return style.copy(
            homeModules = latest.firstNotNullOfOrNull { it.homeModules } ?: style.homeModules,
            lockModules = latest.firstNotNullOfOrNull { it.lockModules } ?: style.lockModules,
        )
    }

    /** The running session whose own widgets [screen] shows now, if any: the one that started last with its own. */
    fun owner(sessions: List<Pair<Long, Schedule>>, screen: Screen): Schedule? =
        sessions.sortedByDescending { it.first }.map { it.second }
            .firstOrNull { (if (screen == Screen.HOME) it.homeModules else it.lockModules) != null }

    /** [style] with [screen]'s widgets set to [list]. */
    fun with(style: HomeStyle, screen: Screen, list: List<ModuleChoice>): HomeStyle =
        if (screen == Screen.HOME) style.copy(homeModules = list) else style.copy(lockModules = list)

    /** [style] with [module] switched [on] on [screen], keeping its place. */
    fun switched(style: HomeStyle, screen: Screen, module: Module, on: Boolean): HomeStyle =
        with(style, screen, of(style, screen).map { if (it.module == module) it.copy(on = on) else it })

    /** Each module once, in the order given, and any not in it (new ones) at the end, off. */
    fun arrange(list: List<ModuleChoice>): List<ModuleChoice> {
        val known = list.distinctBy { it.module }
        return known + Module.entries.filter { m -> known.none { it.module == m } }.map { ModuleChoice(it, on = false) }
    }

    private fun fromSwitches(style: HomeStyle, screen: Screen): List<ModuleChoice> = Module.entries.map { m ->
        ModuleChoice(
            m,
            when (m) {
                Module.GREETING -> style.showGreeting
                Module.CLOCK -> true
                Module.DATE -> style.showDate
                Module.WEATHER -> style.showWeather
                Module.EVENT -> style.showCalendar
                Module.HOURS -> if (screen == Screen.HOME) style.hoursOnHome else style.hoursOnLock
                Module.QUOTE -> style.showQuote
                Module.HABITS, Module.NOW_PLAYING, Module.NOTE -> false
            },
        )
    }
}
