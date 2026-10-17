package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CoverMessagesTest {
    private val mine = CustomMessage("You said you'd read tonight.")
    private val notInMix = CustomMessage("Only when I choose it.", inRandom = false)

    @Test
    fun theUsualWordsMeanNoMessage() {
        assertNull(CoverMessages.pick(CoverMessage(), listOf(mine)))
    }

    @Test
    fun aChosenMessageIsUsedWhileItStillExists() {
        assertEquals(mine.text, CoverMessages.pick(CoverMessage(MessageMode.CHOSEN, mine.text), listOf(mine)))
        assertEquals(CoverMessages.presets[2], CoverMessages.pick(CoverMessage(MessageMode.CHOSEN, CoverMessages.presets[2]), emptyList()))
        // Deleted since: back to the usual words, never a blank cover.
        assertNull(CoverMessages.pick(CoverMessage(MessageMode.CHOSEN, mine.text), emptyList()))
    }

    @Test
    fun theMixIsTheAppsAndYoursThatAreInIt() {
        val pool = CoverMessages.pool(listOf(mine, notInMix))
        assertTrue(mine.text in pool)
        assertTrue(notInMix.text !in pool)
        assertEquals(CoverMessages.presets.size + 1, pool.size)
        val picked = (0 until 50).map { CoverMessages.pick(CoverMessage(MessageMode.RANDOM), listOf(mine, notInMix), Random(it)) }
        assertTrue(picked.all { it in pool })
        // Over enough draws, more than one comes round.
        assertTrue(picked.toSet().size > 1)
    }

    @Test
    fun oldSavesWithoutMessagesLoad() {
        val schedule = AppJson.decodeFromString(Schedule.serializer(), """{"id":"x","name":"Old"}""")
        assertEquals(CoverMessage(), schedule.message)
        val settings = AppJson.decodeFromString(AppSettings.serializer(), """{"onboarded":true}""")
        assertTrue(settings.customMessages.isEmpty())
    }
}
