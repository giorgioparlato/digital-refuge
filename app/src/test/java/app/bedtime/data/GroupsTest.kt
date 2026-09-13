package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Test

class GroupsTest {
    @Test
    fun tickingAGroupAddsAllItsApps() {
        assertEquals(setOf("a", "b", "c"), toggleGroup(setOf("a"), setOf("b", "c")))
    }

    @Test
    fun partlyTickedGroupGetsCompleted() {
        assertEquals(setOf("a", "b", "c"), toggleGroup(setOf("a", "b"), setOf("b", "c")))
    }

    @Test
    fun fullyTickedGroupGetsUnticked() {
        assertEquals(setOf("a"), toggleGroup(setOf("a", "b", "c"), setOf("b", "c")))
    }

    @Test
    fun emptyGroupChangesNothing() {
        assertEquals(setOf("a"), toggleGroup(setOf("a"), emptySet()))
    }

    @Test
    fun templatesStartWithEssentialsAllowed() {
        val essentials = setOf("settings", "phone", "messages")
        val bedtime = Templates.instantiate("bedtime", { false }, essentials)!!
        assertEquals(essentials, bedtime.allowedApps)
        val scratch = Templates.instantiate(Templates.SCRATCH_BLOCK, { false }, essentials)!!
        assertEquals(essentials, scratch.allowedApps)
    }
}
