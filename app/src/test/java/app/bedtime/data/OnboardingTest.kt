package app.bedtime.data

import android.content.pm.ApplicationInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingTest {
    @Test
    fun feedsAreSuggestedAndTheMostUsedListed() {
        val apps = listOf(
            SetupApp("com.instagram.android", "Instagram", 7 * 3600, pulling = false),
            SetupApp("some.game", "A game", 0, pulling = Onboarding.pulling(ApplicationInfo.CATEGORY_GAME)),
            SetupApp("a.mail", "Mail", 3 * 3600, pulling = false),
            SetupApp("a.calc", "Calculator", 0, pulling = false),
        )
        assertEquals(setOf("com.instagram.android", "some.game"), Onboarding.suggestedDistracting(apps))
        // Used this week or suggested; most used first, the calculator nowhere.
        assertEquals(listOf("com.instagram.android", "a.mail", "some.game"), Onboarding.candidates(apps, emptySet()).map { it.pkg })
        assertEquals(listOf("com.instagram.android", "a.mail"), Onboarding.worst(apps, setOf("a.mail", "com.instagram.android")).map { it.pkg })
    }

    @Test
    fun aBudgetALittleUnderNow() {
        assertEquals(90, Onboarding.suggestedBudget(120, 30))
        assertEquals(15, Onboarding.suggestedBudget(10, 30))
        assertEquals(30, Onboarding.suggestedBudget(0, 30))
    }

    @Test
    fun limitsAndBedtimeAsChosen() {
        val app = SetupApp("com.instagram.android", "Instagram", 0, false)
        assertNull(Onboarding.limit(app, LimitChoice.NONE, 1))
        assertEquals(10, Onboarding.limit(app, LimitChoice.PAUSE, 1)!!.rules.pauseSeconds)
        val thirty = Onboarding.limit(app, LimitChoice.THIRTY, 1)!!.rules
        assertEquals(30, thirty.dailyMinutes)
        assertNull(thirty.pauseSeconds)
        val bed = Onboarding.bedtime(null, 23 * 60, 6 * 60 + 30, setOf("x"), greyscale = false, minimal = true, quiet = false)
        // It shuts the apps chosen; the quiet home screen is only the look, not "all but these".
        assertEquals(setOf("x"), bed.shut)
        assertTrue(!bed.allowsOnly)
        assertEquals(23 * 60 to 6 * 60 + 30, bed.startMinute to bed.endMinute)
        assertEquals(DndMode.OFF, bed.dnd)
        assertTrue(bed.minimalMode && !bed.greyscale)
        // Gone through again, the same bedtime changes rather than a second one.
        assertEquals(bed.id, Onboarding.bedtime(Onboarding.existingBedtime(listOf(bed)), 22 * 60, 7 * 60, emptySet(), false, false, true).id)
    }
}
