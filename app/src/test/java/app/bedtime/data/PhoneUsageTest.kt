package app.bedtime.data

import app.bedtime.data.UseEvent.Kind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class PhoneUsageTest {
    private val zone = ZoneId.of("Europe/Stockholm")
    private val day = LocalDate.of(2026, 10, 26)
    private fun at(h: Int, m: Int = 0, d: LocalDate = day) = d.atTime(h, m).atZone(zone).toInstant().toEpochMilli()
    private val from = at(0)
    private val to = at(23, 59)
    private val today = day.toEpochDay()

    private fun read(vararg events: UseEvent, distracting: Set<String> = setOf("insta")) =
        PhoneUsage.fromEvents(events.toList(), from, to, zone, excluded = setOf("home", "refuge"), distractingApps = distracting)

    @Test
    fun anAppRunsUntilAnotherComesOrTheScreenGoesOff() {
        val days = read(
            UseEvent(at(9), Kind.RESUMED, "insta"),
            UseEvent(at(9, 20), Kind.RESUMED, "maps"),
            UseEvent(at(9, 30), Kind.SCREEN_OFF),
            UseEvent(at(10), Kind.UNLOCK),
            UseEvent(at(10), Kind.RESUMED, "maps"),
            UseEvent(at(10, 10), Kind.PAUSED, "maps"),
        )
        val d = days.getValue(today)
        assertEquals(mapOf("insta" to 20 * 60L, "maps" to 20 * 60L), d.apps)
        assertEquals(mapOf(9 to 30 * 60L, 10 to 10 * 60L), d.all)
        assertEquals(mapOf(9 to 20 * 60L), d.distracting)
        // Maps again after the screen was off is a second open.
        assertEquals(mapOf("insta" to 1, "maps" to 2), d.opens)
        assertEquals(1, d.unlocks)
        assertEquals(mapOf(10 to 1), d.unlockHours)
    }

    @Test
    fun homeScreensAndRefugeCountForNothingButEndTheAppBefore() {
        val d = read(
            UseEvent(at(8), Kind.RESUMED, "insta"),
            UseEvent(at(8, 5), Kind.RESUMED, "home"),
            UseEvent(at(8, 30), Kind.RESUMED, "insta"),
            UseEvent(at(8, 35), Kind.RESUMED, "refuge"),
        ).getValue(today)
        assertEquals(mapOf("insta" to 10 * 60L), d.apps)
        assertEquals(2, d.opens["insta"])
    }

    @Test
    fun overMidnightEachDayGetsItsShare() {
        val yesterday = day.minusDays(1)
        val days = PhoneUsage.fromEvents(
            listOf(UseEvent(at(23, 50, yesterday), Kind.RESUMED, "maps"), UseEvent(at(0, 10), Kind.SCREEN_OFF)),
            at(0, 0, yesterday), to, zone, emptySet(), emptySet(),
        )
        assertEquals(mapOf(23 to 600L), days.getValue(today - 1).all)
        assertEquals(mapOf(0 to 600L), days.getValue(today).all)
    }

    @Test
    fun androidsWholeDaysOverRefugesOwn() {
        val android = mapOf(today - 1 to UseDay(today - 1, all = mapOf(9 to 99L)), today to UseDay(today, all = mapOf(9 to 100L)))
        val own = mapOf(today - 2 to UseDay(today - 2, all = mapOf(9 to 1L)), today - 1 to UseDay(today - 1, all = mapOf(9 to 2L)))
        // Android's first day is cut off where its record begins, so refuge's own count stands for it.
        val merged = PhoneUsage.merge(android, own, firstAndroidDay = today - 1)
        assertEquals(listOf(1L, 2L, 100L), (today - 2..today).map { merged.getValue(it).total })
        assertEquals(own, PhoneUsage.merge(android, own, null))
    }

    @Test
    fun appsAveragesAndTheWeek() {
        val a = UseDay(1, all = mapOf(9 to 3600L), distracting = mapOf(9 to 1800L), apps = mapOf("insta" to 1800L, "maps" to 1800L, "clock" to 30L), opens = mapOf("insta" to 4), unlocks = 10)
        val b = UseDay(2, all = mapOf(10 to 1200L), apps = mapOf("maps" to 1200L), unlocks = 6)
        assertEquals(
            listOf(AppUse("maps", 3000, 0, false), AppUse("insta", 1800, 4, true)),
            PhoneUsage.ranked(listOf(a, b), setOf("insta")),
        )
        val avg = PhoneUsage.averageHours(listOf(a, b, UseDay(3)))
        assertEquals(1800L to 900L, avg[9])
        assertEquals(600L to 0L, avg[10])
        assertEquals(2400L, PhoneUsage.dailyAverage(listOf(a, b, UseDay(3))) { it.total })
        val trend = PhoneUsage.trend(listOf(a), listOf(b))
        assertEquals(3600L to 1200L, trend.total)
        assertEquals(10 to 6, trend.unlocks)
        // Without Android's record a week's unlocks aren't known.
        assertNull(PhoneUsage.trend(listOf(a), listOf(b.copy(unlocks = null))).unlocks)
    }
}
