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
        val noClock = Modules.switchedEverywhere(lock, Module.CLOCK, false)
        assertFalse(Modules.onAnywhere(noClock, Module.CLOCK))
    }
}
