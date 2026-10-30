package app.bedtime.ui.components

import android.media.session.PlaybackState
import org.junit.Assert.assertFalse
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
}
