package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class CoverMessagesTest {
    private val mine = CustomMessage("You said you'd read tonight.")
    private val notInMix = CustomMessage("Only when I choose it.", inRandom = false)

    private val suggested = CoverMessages.all(AppSettings())

    @Test
    fun byDefaultADifferentOneEachTime() {
        assertEquals(MessageMode.RANDOM, CoverMessage().mode)
        val pool = CoverMessages.pool(suggested)
        assertTrue(CoverMessages.pick(CoverMessage(), suggested) in pool)
        // "The usual words", no longer offered, reads as a different one each time.
        assertTrue(CoverMessages.pick(CoverMessage(MessageMode.USUAL), suggested) in pool)
        assertEquals("A different one each time", CoverMessages.summary(CoverMessage(MessageMode.USUAL)))
    }

    @Test
    fun theSuggestedOnesComeFirstThenYoursUntilYouChangeThem() {
        val settings = AppSettings(customMessages = listOf(mine))
        assertEquals(CoverMessages.presets + mine.text, CoverMessages.all(settings).map { it.text })
        // Rewording a suggested one makes the whole list yours, the rest kept as they were.
        val changed = CoverMessages.edited(settings) { list -> list.mapIndexed { i, m -> if (i == 0) m.copy(text = "put it down.") else m } }
        assertEquals(true, changed.coverMessagesEdited)
        assertEquals(listOf("put it down.") + CoverMessages.presets.drop(1) + mine.text, CoverMessages.all(changed).map { it.text })
        // Deleting a suggested one keeps it gone.
        val fewer = CoverMessages.edited(changed) { it.drop(1) }
        assertTrue("put it down." !in CoverMessages.all(fewer).map { it.text })
        assertEquals(suggested, CoverMessages.all(CoverMessages.reset(fewer)))
    }

    @Test
    fun aChosenMessageIsUsedWhileItStillExists() {
        val messages = suggested + mine
        assertEquals(mine.text, CoverMessages.pick(CoverMessage(MessageMode.CHOSEN, mine.text), messages))
        assertEquals(CoverMessages.presets[2], CoverMessages.pick(CoverMessage(MessageMode.CHOSEN, CoverMessages.presets[2]), messages))
        // Deleted since: back to the mix, never a blank cover.
        assertTrue(CoverMessages.pick(CoverMessage(MessageMode.CHOSEN, mine.text), suggested) in CoverMessages.presets)
    }

    @Test
    fun theMixIsThoseInIt() {
        val messages = suggested + mine + notInMix
        val pool = CoverMessages.pool(messages)
        assertTrue(mine.text in pool)
        assertTrue(notInMix.text !in pool)
        assertEquals(CoverMessages.presets.size + 1, pool.size)
        val picked = (0 until 50).map { CoverMessages.pick(CoverMessage(MessageMode.RANDOM), messages, Random(it)) }
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
