package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class HourlyUseTest {
    private val zone = ZoneId.of("Europe/Stockholm")
    private val day = LocalDate.of(2026, 10, 25)
    private fun at(h: Int, m: Int, d: LocalDate = day) = d.atTime(h, m).atZone(zone).toInstant().toEpochMilli()

    @Test
    fun aVisitIsSplitAtEachHour() {
        val split = HourlyUse.split(at(9, 50), at(11, 5), zone)
        assertEquals(mapOf(day.toEpochDay() to mapOf(9 to 600L, 10 to 3600L, 11 to 300L)), split)
    }

    @Test
    fun andAtMidnight() {
        val split = HourlyUse.split(at(23, 55), at(0, 10, day.plusDays(1)), zone)
        assertEquals(mapOf(day.toEpochDay() to mapOf(23 to 300L), day.plusDays(1).toEpochDay() to mapOf(0 to 600L)), split)
        assertEquals(emptyMap<Long, Map<Int, Long>>(), HourlyUse.split(at(10, 0), at(10, 0), zone))
    }

    @Test
    fun hoursReadAsCalmDistractedNowOrAhead() {
        val states = HourlyUse.states(DayHours(mapOf(8 to 200L, 9 to 400L, 14 to 900L)), nowHour = 14, rule = HourRule(5))
        assertEquals(HourState.CALM, states[8])
        assertEquals(HourState.DISTRACTED, states[9])
        assertEquals(HourState.NOW_DISTRACTED, states[14])
        assertEquals(HourState.AHEAD, states[15])
        assertEquals(24, states.size)
        assertEquals("2 hours over 5 min in distracting apps today", HourlyUse.summary(states, HourRule(5)))
    }

    @Test
    fun orOnThePhoneAtAllIfYouSaySo() {
        // Only 3 min in distracting apps, but 40 in all: distracted with the any-app rule at 30, not without it.
        val hours = DayHours(apps = mapOf(10 to 180L), all = mapOf(10 to 2400L))
        assertEquals(false, HourlyUse.distracted(hours, 10, HourRule(5)))
        assertEquals(true, HourlyUse.distracted(hours, 10, HourRule(5, totalMinutes = 30)))
        assertEquals(false, HourlyUse.distracted(hours, 10, HourRule(5, totalMinutes = 45)))
        val states = HourlyUse.states(hours, nowHour = 12, rule = HourRule(5, 30))
        assertEquals("1 distracted hour today", HourlyUse.summary(states, HourRule(5, 30)))
    }

    @Test
    fun oldSavesHaveNoDistractingAppsYet() {
        val s = AppJson.decodeFromString(AppSettings.serializer(), """{"onboarded":true}""")
        assertEquals(emptySet<String>(), s.distractingApps)
        assertEquals(5, s.distractionMinutes)
        assertEquals(false, s.homeStyle.hoursOnHome)
        assertEquals(TopSpace.MEDIUM, s.homeStyle.topSpace)
        assertEquals(false, s.homeStyle.lockAlways)
        assertEquals(null, s.distractionTotalMinutes)
        assertEquals(false, HourlyUse.counting(s))
    }

    @Test
    fun theDayToldAgainstYesterday() {
        val today = DayHours(
            apps = mapOf(9 to 600L, 21 to 2040L),
            all = mapOf(9 to 1800L, 12 to 900L, 21 to 2400L),
            byApp = mapOf("insta" to 1800L, "tube" to 840L, "chess" to 30L),
        )
        val yesterday = DayHours(apps = mapOf(20 to 4200L), all = mapOf(20 to 6000L))
        val r = HourlyUse.report(today, yesterday, HourRule(5), nowHour = 22)
        assertEquals(2640L, r.inApps)
        assertEquals(5100L, r.overall)
        assertEquals(-1560L, r.inAppsVsYesterday)
        assertEquals(-900L, r.overallVsYesterday)
        assertEquals(2, r.distracted)
        assertEquals(23, r.hoursSoFar)
        assertEquals(21, r.peakHour)
        // Under a minute isn't worth a line.
        assertEquals(listOf("insta" to 1800L, "tube" to 840L), r.topApps)
        // No yesterday: nothing to compare.
        assertEquals(null, HourlyUse.report(today, DayHours(), HourRule(5), 22).inAppsVsYesterday)
    }

    @Test
    fun comparedInWords() {
        val f = { m: Long -> "$m min" }
        assertEquals("26 min less than yesterday", HourlyUse.versus(-1560L, f))
        assertEquals("15 min more than yesterday", HourlyUse.versus(900L, f))
        assertEquals("the same as yesterday", HourlyUse.versus(30L, f))
    }
}
