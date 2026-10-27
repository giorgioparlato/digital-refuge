package app.bedtime.ui.components

import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.core.Animatable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import app.bedtime.service.LockNotificationListener

/** The track you swiped the player away on: it stays away until something else plays. */
object NowPlayingDismissal {
    val track = mutableStateOf<String?>(null)
}

/** What's playing: its title and artist, whether it's playing, and the buttons that steer it. */
data class NowPlaying(
    val title: String,
    val artist: String,
    val playing: Boolean,
    val previous: () -> Unit = {},
    val toggle: () -> Unit = {},
    val next: () -> Unit = {},
    /** The app playing it, to open with a tap on the title. */
    val app: String = "",
) {
    val key: String get() = "$app|$title"
}

/**
 * The music or podcast playing now, from whichever app plays it, kept current; null when nothing does
 * or notification access isn't given (Android shares what's playing only with notification listeners).
 */
@Composable
fun rememberNowPlaying(): NowPlaying? {
    val context = LocalContext.current
    var now by remember { mutableStateOf<NowPlaying?>(null) }
    DisposableEffect(Unit) {
        val sessions = context.getSystemService(MediaSessionManager::class.java)
        val component = LockNotificationListener.component(context)
        val handler = Handler(Looper.getMainLooper())
        var controller: MediaController? = null
        lateinit var callback: MediaController.Callback
        fun read() {
            val c = controller
            val meta = c?.metadata
            val title = meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
            now = if (c == null || title.isNullOrBlank()) {
                null
            } else {
                val controls = c.transportControls
                val playing = c.playbackState?.state == PlaybackState.STATE_PLAYING
                NowPlaying(
                    title = title,
                    artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE).orEmpty(),
                    playing = playing,
                    previous = { controls.skipToPrevious() },
                    toggle = { if (playing) controls.pause() else controls.play() },
                    next = { controls.skipToNext() },
                    app = c.packageName.orEmpty(),
                )
            }
        }
        fun pick() {
            val active = runCatching { sessions.getActiveSessions(component) }.getOrDefault(emptyList())
            val chosen = active.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING } ?: active.firstOrNull()
            if (chosen?.sessionToken != controller?.sessionToken) {
                controller?.unregisterCallback(callback)
                controller = chosen
                chosen?.registerCallback(callback, handler)
            }
            read()
        }
        callback = object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) = read()
            override fun onPlaybackStateChanged(state: PlaybackState?) = pick()
            override fun onSessionDestroyed() = pick()
        }
        val listener = MediaSessionManager.OnActiveSessionsChangedListener { pick() }
        runCatching { sessions.addOnActiveSessionsChangedListener(listener, component, handler) }
        pick()
        onDispose {
            runCatching { sessions.removeOnActiveSessionsChangedListener(listener) }
            controller?.unregisterCallback(callback)
        }
    }
    return now?.takeIf { it.key != NowPlayingDismissal.track.value }
}

/** A quiet line: the title over its artist, and previous, play or pause, next. */
@Composable
fun NowPlayingWidget(now: NowPlaying, text: Color, faint: Color, modifier: Modifier = Modifier, onOpen: ((String) -> Unit)? = null) {
    val scope = rememberCoroutineScope()
    val offset = remember(now.key) { Animatable(0f) }
    val dismiss = { NowPlayingDismissal.track.value = now.key }
    // Swiped far enough either way, it goes until the next track; otherwise it springs back.
    Row(
        modifier
            .fillMaxWidth()
            .offset { IntOffset(offset.value.roundToInt(), 0) }
            .alpha((1f - kotlin.math.abs(offset.value) / 600f).coerceIn(0.2f, 1f))
            .pointerInput(now.key) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (kotlin.math.abs(offset.value) > size.width * 0.35f) {
                                offset.animateTo(if (offset.value > 0) size.width.toFloat() else -size.width.toFloat())
                                dismiss()
                            } else {
                                offset.animateTo(0f)
                            }
                        }
                    },
                    onDragCancel = { scope.launch { offset.animateTo(0f) } },
                ) { change, dx ->
                    change.consume()
                    scope.launch { offset.snapTo(offset.value + dx) }
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.weight(1f).clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                .then(if (onOpen != null && now.app.isNotEmpty()) Modifier.clickable(onClickLabel = "open the player") { onOpen(now.app) } else Modifier),
        ) {
            VerbatimText(AnnotatedString(now.title), color = text, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (now.artist.isNotBlank()) {
                VerbatimText(AnnotatedString(now.artist), color = faint, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        MediaButton(BedtimeIcons.Previous, "previous", faint, now.previous)
        MediaButton(if (now.playing) BedtimeIcons.Pause else Icons.Default.PlayArrow, if (now.playing) "pause" else "play", text, now.toggle)
        MediaButton(BedtimeIcons.Next, "next", faint, now.next)
        Box(Modifier.size(32.dp).clip(CircleShape).clickable(onClickLabel = "close the player", onClick = dismiss), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Close, contentDescription = "close the player", tint = faint.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun MediaButton(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClickLabel = label, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(22.dp))
    }
}
