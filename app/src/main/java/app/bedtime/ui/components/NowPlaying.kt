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
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaMetadata
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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
    /** Where it's heard, and a tap to send it somewhere else. */
    val output: NowPlayings.Output? = null,
    val pickOutput: () -> Unit = {},
    /** The app's own like button, such as Spotify's, when it has one: whether it's liked, and the tap. */
    val liked: Boolean? = null,
    val like: () -> Unit = {},
) {
    val key: String get() = "$app|$title"
}

/**
 * The music or podcast playing now, from whichever app plays it, kept current; null when nothing does
 * or notification access isn't given (Android shares what's playing only with notification listeners).
 */
/** Which media sessions the player shows. */
object NowPlayings {
    /** Messaging apps, whose sessions are voice messages: a message isn't music, and it's private. */
    val MESSAGING = setOf(
        "org.thoughtcrime.securesms", "com.whatsapp", "com.whatsapp.w4b", "org.telegram.messenger", "org.telegram.messenger.web",
        "com.facebook.orca", "com.google.android.apps.messaging", "com.samsung.android.messaging", "im.vector.app", "com.discord",
    )

    /**
     * Worth showing: not a messaging app's, and playing or paused part-way (a session merely left
     * open, stopped or never started, isn't).
     */
    fun isPlayer(packageName: String, state: Int?): Boolean =
        packageName !in MESSAGING && state in setOf(PlaybackState.STATE_PLAYING, PlaybackState.STATE_PAUSED, PlaybackState.STATE_BUFFERING)

    enum class Where { PHONE, HEADPHONES, ELSEWHERE }

    /** Where it's heard: [Where], and a name to show ("this phone", "pixel buds", "another device"). */
    data class Output(val where: Where, val name: String)

    /** Headphones, earbuds and the like, wired or not ([AudioDeviceInfo] types). */
    private val WORN = setOf(
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_WIRED_HEADSET, AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
        AudioDeviceInfo.TYPE_USB_HEADSET, AudioDeviceInfo.TYPE_HEARING_AID, 26 /* BLE headset */, 27 /* BLE speaker */,
    )
    private val WIRELESS = setOf(AudioDeviceInfo.TYPE_BLUETOOTH_A2DP, AudioDeviceInfo.TYPE_HEARING_AID, 26, 27)

    /**
     * Where it's heard: played on another device (a cast, or Spotify Connect) when [remote]; otherwise the
     * first headphones among [outputs] (type to name), by their name if wireless; otherwise this phone.
     */
    fun output(remote: Boolean, outputs: List<Pair<Int, String>>): Output {
        if (remote) return Output(Where.ELSEWHERE, "another device")
        val worn = outputs.firstOrNull { it.first in WORN } ?: return Output(Where.PHONE, "this phone")
        return Output(Where.HEADPHONES, worn.second.takeIf { worn.first in WIRELESS && it.isNotBlank() } ?: "headphones")
    }

    /**
     * The app's like button among its own [actions] (id to name), and whether the track is liked already:
     * a button to take it off ("remove from liked songs") means it is. Dislikes and thumbs down aren't one.
     */
    fun like(actions: List<Pair<String, String>>): Pair<String, Boolean>? {
        val words = listOf("like", "heart", "favorite", "favourite", "collection", "thumb_up", "thumbs up")
        val found = actions.firstOrNull { (id, name) ->
            val both = "$id $name".lowercase()
            words.any { it in both } && listOf("dislike", "thumb_down", "thumbs down").none { it in both }
        } ?: return null
        val both = "${found.first} ${found.second}".lowercase()
        return found.first to listOf("remove", "unlike", "unsave", "unfavo", "unheart").any { it in both }
    }
}

@Composable
fun rememberNowPlaying(): NowPlaying? {
    val context = LocalContext.current
    var now by remember { mutableStateOf<NowPlaying?>(null) }
    DisposableEffect(Unit) {
        val sessions = context.getSystemService(MediaSessionManager::class.java)
        val component = LockNotificationListener.component(context)
        val handler = Handler(Looper.getMainLooper())
        val audio = context.getSystemService(AudioManager::class.java)
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
                val like = NowPlayings.like(c.playbackState?.customActions.orEmpty().map { it.action to it.name.toString() })
                val app = c.packageName.orEmpty()
                NowPlaying(
                    title = title,
                    artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE).orEmpty(),
                    playing = playing,
                    previous = { controls.skipToPrevious() },
                    toggle = { if (playing) controls.pause() else controls.play() },
                    next = { controls.skipToNext() },
                    app = app,
                    output = NowPlayings.output(c.playbackInfo?.playbackType == MediaController.PlaybackInfo.PLAYBACK_TYPE_REMOTE, outputs(audio)),
                    pickOutput = { showOutputSwitcher(context, app) },
                    liked = like?.second,
                    like = { like?.let { controls.sendCustomAction(it.first, null) } },
                )
            }
        }
        fun pick() {
            val active = runCatching { sessions.getActiveSessions(component) }.getOrDefault(emptyList())
                .filter { NowPlayings.isPlayer(it.packageName.orEmpty(), it.playbackState?.state) }
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
        // Headphones put on or taken off change where it's heard.
        val devices = object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>?) = read()
            override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>?) = read()
        }
        audio.registerAudioDeviceCallback(devices, handler)
        pick()
        onDispose {
            audio.unregisterAudioDeviceCallback(devices)
            runCatching { sessions.removeOnActiveSessionsChangedListener(listener) }
            controller?.unregisterCallback(callback)
        }
    }
    return now?.takeIf { it.key != NowPlayingDismissal.track.value }
}

/** The outputs music goes to now, as type to name: the actual route where Android says (13 on), else every output. */
private fun outputs(audio: AudioManager): List<Pair<Int, String>> {
    val media = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build()
    val routed = if (Build.VERSION.SDK_INT >= 33) runCatching { audio.getAudioDevicesForAttributes(media) }.getOrNull() else null
    return (routed ?: audio.getDevices(AudioManager.GET_DEVICES_OUTPUTS).toList()).map { it.type to it.productName.toString() }
}

/** Android's own "play on" switcher for [app]'s music: the system media output dialog, or the settings panel on 11. */
private fun showOutputSwitcher(context: Context, app: String) {
    runCatching {
        when {
            Build.VERSION.SDK_INT >= 31 -> context.sendBroadcast(
                Intent("com.android.systemui.action.LAUNCH_MEDIA_OUTPUT_DIALOG").setPackage("com.android.systemui").putExtra("package_name", app),
            )
            Build.VERSION.SDK_INT == 30 -> context.startActivity(
                Intent("com.android.settings.panel.action.MEDIA_OUTPUT").putExtra("com.android.settings.panel.extra.PACKAGE_NAME", app).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            else -> context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

/** A quiet line: the title over its artist and where it's heard; like, if the app has one, previous, play or pause, next. */
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
            now.output?.let { out ->
                Row(
                    Modifier.padding(top = 2.dp).clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp)).clickable(onClickLabel = "play it somewhere else", onClick = now.pickOutput),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val icon = when (out.where) {
                        NowPlayings.Where.PHONE -> BedtimeIcons.Phone
                        NowPlayings.Where.HEADPHONES -> BedtimeIcons.Headphones
                        NowPlayings.Where.ELSEWHERE -> BedtimeIcons.Speaker
                    }
                    Icon(icon, contentDescription = null, tint = faint, modifier = Modifier.size(12.dp))
                    Text(out.name, color = faint, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 4.dp))
                }
            }
        }
        now.liked?.let { liked ->
            MediaButton(if (liked) Icons.Default.Favorite else Icons.Default.FavoriteBorder, if (liked) "unlike" else "like", if (liked) text else faint, now.like)
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
