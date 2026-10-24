package app.bedtime.ui.lock

import androidx.compose.runtime.key
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.ExperimentalMaterial3Api
import app.bedtime.ui.components.ProvideAppIcons
import app.bedtime.ui.components.AppIcon
import app.bedtime.data.NoteDetail
import app.bedtime.data.LockNotifications
import app.bedtime.data.LockNotification
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import android.app.ActivityOptions
import app.bedtime.ui.minimal.rememberCalendarLine
import app.bedtime.data.Swipe
import app.bedtime.data.GESTURE_CAMERA
import app.bedtime.apps.AppCatalog
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.gestures.detectDragGestures
import android.provider.MediaStore
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.bedtime.ui.theme.WithFontFamily
import app.bedtime.ui.theme.AppFonts
import app.bedtime.data.AppSettings
import app.bedtime.data.HomeStyle
import app.bedtime.data.Quote
import app.bedtime.data.Quotes
import app.bedtime.data.Repository
import app.bedtime.engine.Engine
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.Text
import app.bedtime.ui.formatTime
import app.bedtime.ui.homestyle.HomePalette
import app.bedtime.ui.homestyle.palette
import app.bedtime.ui.minimal.QuoteBlock
import app.bedtime.ui.minimal.SessionHeader
import app.bedtime.ui.theme.BedtimeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/**
 * Shown over the keyguard while a session runs: the minimal home's clock and session, without the apps.
 * It doesn't replace the system lock. A tap asks Android to unlock as usual (PIN, fingerprint…).
 */
class LockScreenActivity : ComponentActivity() {
    private val unlocked = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
        }
        enableEdgeToEdge()
        onBackPressedDispatcher.addCallback(this) { /* Stay; unlocking is the way out. */ }
        // Android's lock-screen privacy may have changed since the notifications were last read.
        LockNotifications.refresh()
        ContextCompat.registerReceiver(this, unlocked, IntentFilter(Intent.ACTION_USER_PRESENT), ContextCompat.RECEIVER_NOT_EXPORTED)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    leaveIfUnlocked()
                    delay(300)
                }
            }
        }
        setContent {
            BedtimeTheme {
                ProvideAppIcons {
                    LockScreen(
                        onOpen = ::openPhone,
                        onOpenApp = ::openApp,
                        onOpenNotification = ::openNotification,
                        onFinish = ::finish,
                        onLightBackground = ::useLightSystemBars,
                    )
                }
            }
        }
    }

    /**
     * Leaves as soon as the phone is unlocked (fingerprint, PIN, no lock set, or still inside the lock
     * delay). Checked continuously, not only on the "unlocked" broadcast: that can arrive before this
     * screen exists and leave it stranded over an unlocked phone.
     */
    private fun leaveIfUnlocked() {
        // Screen off: this screen is waiting to be seen at the next wake.
        if (!getSystemService(PowerManager::class.java).isInteractive) return
        if (!getSystemService(KeyguardManager::class.java).isKeyguardLocked) finish()
    }

    override fun onDestroy() {
        unregisterReceiver(unlocked)
        super.onDestroy()
    }

    private fun openPhone() {
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (!keyguard.isKeyguardLocked) {
            finish()
            return
        }
        keyguard.requestDismissKeyguard(
            this,
            object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() = finish()

                // Nothing left to dismiss: never leave this screen stuck in front.
                override fun onDismissError() = finish()
            },
        )
    }

    /**
     * A swipe's app. The camera opens over the lock screen as it is; anything else asks for the usual
     * unlock first and opens once it succeeds. A session still blocks what it blocks once it's open.
     */
    private fun openApp(target: String) {
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (target == GESTURE_CAMERA) {
            val action = if (keyguard.isKeyguardLocked) MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE else MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA
            runCatching { startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            finish()
            return
        }
        unlockThen { AppCatalog.launch(this, target) }
    }

    /**
     * A notification's own destination, after the usual unlock. A session still blocks what it blocks
     * once it's open.
     */
    private fun openNotification(key: String) {
        val note = LockNotifications.current.value.firstOrNull { it.key == key } ?: return openPhone()
        unlockThen {
            val sent = note.contentIntent?.let { intent -> runCatching { intent.send(this, 0, null, null, null, null, sendOptions()) }.isSuccess }
            if (sent != true) AppCatalog.launch(this, note.packageName)
            if (note.autoCancel) LockNotifications.cancel(key)
        }
    }

    /** Lets a notification open its app from here, which Android 14 otherwise counts as starting from the background. */
    private fun sendOptions(): Bundle? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ActivityOptions.makeBasic()
                .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                .toBundle()
        } else {
            null
        }

    /** Asks for the usual unlock (PIN, fingerprint…), then does [action] and leaves. */
    private fun unlockThen(action: () -> Unit) {
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (!keyguard.isKeyguardLocked) {
            action()
            finish()
            return
        }
        keyguard.requestDismissKeyguard(
            this,
            object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    action()
                    finish()
                }

                override fun onDismissError() = finish()
            },
        )
    }

    private fun useLightSystemBars(light: Boolean) {
        val transparent = android.graphics.Color.TRANSPARENT
        val style = if (light) SystemBarStyle.light(transparent, transparent) else SystemBarStyle.dark(transparent)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }

    companion object {
        fun intent(context: Context): Intent =
            Intent(context, LockScreenActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
    }
}

@Composable
private fun LockScreen(
    onOpen: () -> Unit,
    onOpenApp: (String) -> Unit,
    onOpenNotification: (String) -> Unit,
    onFinish: () -> Unit,
    onLightBackground: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val state by Engine.state(context).collectAsStateWithLifecycle()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
    val notifications by LockNotifications.current.collectAsStateWithLifecycle()
    // With several sessions at once, show the one that lasts longest.
    val occurrence = state?.active?.maxByOrNull { it.end }
    val style = settings.homeStyle
    val light = style.palette().isLight

    LaunchedEffect(state) {
        if (state != null && occurrence == null) onFinish()
    }
    LaunchedEffect(light) { onLightBackground(light) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000)
        }
    }

    if (occurrence == null) {
        Box(Modifier.fillMaxSize().background(style.palette().background))
        return
    }
    LockScreenContent(
        now = now,
        scheduleName = occurrence.schedule.name,
        until = formatTime(context, occurrence.end),
        style = style,
        onOpen = onOpen,
        // A swipe from the middle opens its app, if one is set; every other swipe just unlocks.
        onSwipe = { swipe -> style.lockGestures[swipe]?.let(onOpenApp) ?: onOpen() },
        quote = Quotes.forPeriod(now, style.quoteRefresh, prefs = settings.quotes),
        calendar = rememberCalendarLine(style, now, onLockScreen = true),
        notifications = if (style.lockNotifications) notifications.filter { it.packageName in style.lockNotificationApps } else emptyList(),
        onOpenNotification = onOpenNotification,
        onDismissNotification = LockNotifications::dismiss,
    )
}

@Composable
internal fun LockScreenContent(
    now: LocalDateTime,
    scheduleName: String,
    until: String,
    style: HomeStyle,
    onOpen: () -> Unit = {},
    onSwipe: (Swipe) -> Unit = {},
    quote: Quote? = null,
    calendar: String? = null,
    preview: Boolean = false,
    notifications: List<LockNotification> = emptyList(),
    onOpenNotification: (String) -> Unit = {},
    onDismissNotification: (LockNotification) -> Unit = {},
) {
    val p = style.palette()
    val minDistance = with(LocalDensity.current) { 64.dp.toPx() }
    WithFontFamily(AppFonts.family(LocalContext.current, style.font)) {
        Column(
            Modifier
                .fillMaxSize()
                .background(p.background)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = "unlock the phone",
                    onClick = onOpen,
                )
                .pointerInput(onSwipe, onOpen) {
                    var start = Offset.Zero
                    var travel = Offset.Zero
                    detectDragGestures(
                        onDragStart = {
                            start = it
                            travel = Offset.Zero
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            travel += amount
                        },
                        onDragEnd = {
                            val w = size.width.toFloat()
                            val h = size.height.toFloat()
                            when (val read = readLockSwipe(start.x, start.y, w, h, travel.x, travel.y, minDistance)) {
                                LockSwipe.None -> Unit
                                LockSwipe.Unlock -> onOpen()
                                is LockSwipe.From -> onSwipe(read.swipe)
                            }
                        },
                    )
                }
                .then(if (preview) Modifier else Modifier.safeDrawingPadding())
                .padding(horizontal = 28.dp, vertical = 24.dp),
        ) {
            Spacer(Modifier.height(if (preview) 12.dp else 32.dp))
            SessionHeader(now, scheduleName, until, style, calendar)
            if (notifications.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                NotificationList(notifications, style, onOpenNotification, onDismissNotification)
            }
            Spacer(Modifier.weight(1f))
            if (quote != null && style.showQuote) {
                QuoteBlock(quote, style)
                Spacer(Modifier.height(40.dp))
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(BedtimeIcons.Refuge, contentDescription = null, tint = p.accent, modifier = Modifier.size(28.dp))
                Spacer(Modifier.height(10.dp))
                Text("tap to open", style = MaterialTheme.typography.labelLarge, color = p.faint)
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** How many notifications the lock screen lists before "and N more". */
private const val MAX_NOTIFICATIONS = 4

/**
 * The newest notifications, quietly: app, time, and as much as Android's own lock screen would show.
 * Swiped aside, one is cleared as in Android's own shade.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NotificationList(notes: List<LockNotification>, style: HomeStyle, onOpen: (String) -> Unit, onDismiss: (LockNotification) -> Unit) {
    val p = style.palette()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        notes.take(MAX_NOTIFICATIONS).forEach { note ->
            key(note.key) {
                val swipe = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value != SwipeToDismissBoxValue.Settled) onDismiss(note)
                        true
                    },
                )
                SwipeToDismissBox(swipe, backgroundContent = {}) { NotificationRow(note, p, onOpen) }
            }
        }
        if (notes.size > MAX_NOTIFICATIONS) {
            Text(
                "and ${notes.size - MAX_NOTIFICATIONS} more",
                style = MaterialTheme.typography.labelMedium,
                color = p.faint,
                modifier = Modifier.padding(start = 14.dp, top = 2.dp),
            )
        }
    }
}

@Composable
private fun NotificationRow(note: LockNotification, p: HomePalette, onOpen: (String) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(p.text.copy(alpha = 0.06f))
            .clickable(onClickLabel = "unlock and open") { onOpen(note.key) }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.padding(top = 2.dp)) { AppIcon(note.packageName, note.app, 22.dp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(note.app, style = MaterialTheme.typography.labelMedium, color = p.faint, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(formatTime(LocalContext.current, note.postedAt), style = MaterialTheme.typography.labelSmall, color = p.faint)
            }
            val title = note.title ?: if (note.detail == NoteDetail.APP_ONLY) "New notification" else null
            title?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            note.text?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = p.muted, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}
