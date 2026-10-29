package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ModulesTest {
    @Test
    fun untilChangedTheyFollowTheOldSwitches() {
        val style = HomeStyle(showGreeting = false, showWeather = true, hoursOnHome = true, hoursOnLock = false, showQuote = true)
        assertEquals(
            listOf(Module.CLOCK, Module.DATE, Module.WEATHER, Module.HOURS, Module.QUOTE),
            Modules.shown(style, Screen.HOME),
        )
        assertEquals(listOf(Module.CLOCK, Module.DATE, Module.WEATHER, Module.QUOTE), Modules.shown(style, Screen.LOCK))
        // Saved before there were widgets, a style loads the same.
        val old = AppJson.decodeFromString(HomeStyle.serializer(), """{"showDate":false,"showCalendar":true}""")
        assertEquals(listOf(Module.GREETING, Module.CLOCK, Module.EVENT, Module.QUOTE), Modules.shown(old, Screen.HOME))
    }

    @Test
    fun eachScreenItsOwnOrderAndSwitches() {
        val style = HomeStyle()
        val lock = Modules.switched(Modules.with(style, Screen.LOCK, listOf(ModuleChoice(Module.QUOTE), ModuleChoice(Module.CLOCK))), Screen.LOCK, Module.CLOCK, false)
        // The rest join at the end, off; the home screen is untouched.
        assertEquals(listOf(Module.QUOTE), Modules.shown(lock, Screen.LOCK))
        assertEquals(Module.entries.size, Modules.of(lock, Screen.LOCK).size)
        assertEquals(Modules.shown(style, Screen.HOME), Modules.shown(lock, Screen.HOME))
        assertTrue(Modules.onAnywhere(lock, Module.CLOCK))
        val noClock = Modules.switched(lock, Screen.HOME, Module.CLOCK, false)
        assertFalse(Modules.onAnywhere(noClock, Module.CLOCK))
    }

    @Test
    fun aSessionCanBringItsOwn() {
        val usual = HomeStyle()
        val deepWork = Schedule(name = "Deep work", homeModules = listOf(ModuleChoice(Module.CLOCK), ModuleChoice(Module.HABITS)))
        val bed = Schedule(name = "Bed", homeModules = listOf(ModuleChoice(Module.QUOTE)), lockModules = listOf(ModuleChoice(Module.CLOCK)))
        // Nothing of its own: your usual ones.
        assertEquals(Modules.shown(usual, Screen.HOME), Modules.shown(Modules.during(usual, listOf(1L to Schedule())), Screen.HOME))
        assertEquals(listOf(Module.CLOCK, Module.HABITS), Modules.shown(Modules.during(usual, listOf(1L to deepWork)), Screen.HOME))
        // The one that started last wins, screen by screen.
        val both = Modules.during(usual, listOf(5L to bed, 9L to deepWork))
        assertEquals(listOf(Module.CLOCK, Module.HABITS), Modules.shown(both, Screen.HOME))
        assertEquals(listOf(Module.CLOCK), Modules.shown(both, Screen.LOCK))
    }
}
