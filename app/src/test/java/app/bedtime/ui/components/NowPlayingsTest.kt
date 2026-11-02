package app.bedtime.ui.components

import android.media.session.PlaybackState
import android.media.AudioDeviceInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NowPlayingsTest {
    @Test
    fun musicShowsButNotAVoiceMessageOrAnIdleSession() {
        assertTrue(NowPlayings.isPlayer("com.spotify.music", PlaybackState.STATE_PLAYING))
        assertTrue(NowPlayings.isPlayer("com.spotify.music", PlaybackState.STATE_PAUSED))
        // Signal's voice messages are messages, playing or not.
        assertFalse(NowPlayings.isPlayer("org.thoughtcrime.securesms", PlaybackState.STATE_PLAYING))
        // A session left open, or stopped, isn't something playing.
        assertFalse(NowPlayings.isPlayer("com.spotify.music", PlaybackState.STATE_NONE))
        assertFalse(NowPlayings.isPlayer("com.spotify.music", PlaybackState.STATE_STOPPED))
        assertFalse(NowPlayings.isPlayer("com.spotify.music", null))
    }

    @Test
    fun whereItsHeard() {
        val speaker = AudioDeviceInfo.TYPE_BUILTIN_SPEAKER to "Pixel 8"
        assertEquals(NowPlayings.Output(NowPlayings.Where.PHONE, "this phone"), NowPlayings.output(false, listOf(speaker)))
        assertEquals(NowPlayings.Output(NowPlayings.Where.HEADPHONES, "Pixel Buds"), NowPlayings.output(false, listOf(speaker, AudioDeviceInfo.TYPE_BLUETOOTH_A2DP to "Pixel Buds")))
        // A wire's name is the phone's own, so it's just headphones.
        assertEquals(NowPlayings.Output(NowPlayings.Where.HEADPHONES, "headphones"), NowPlayings.output(false, listOf(AudioDeviceInfo.TYPE_WIRED_HEADPHONES to "Pixel 8")))
        assertEquals(NowPlayings.Where.ELSEWHERE, NowPlayings.output(true, listOf(speaker)).where)
    }

    @Test
    fun theAppsOwnLikeButton() {
        val spotify = listOf("TOGGLE_SHUFFLE" to "Shuffle", "ADD_TO_COLLECTION" to "Add to Liked Songs", "START_RADIO" to "Radio")
        assertEquals("ADD_TO_COLLECTION" to false, NowPlayings.like(spotify))
        assertEquals("REMOVE_FROM_COLLECTION" to true, NowPlayings.like(listOf("REMOVE_FROM_COLLECTION" to "Remove from Liked Songs")))
        assertEquals("thumbs_up" to false, NowPlayings.like(listOf("thumbs_down" to "Dislike", "thumbs_up" to "Like")))
        assertNull(NowPlayings.like(listOf("TOGGLE_SHUFFLE" to "Shuffle")))
    }
}
