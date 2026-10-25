package app.bedtime.ui.lock

import app.bedtime.ui.components.rememberTodayHours
import app.bedtime.data.HourlyUse
import app.bedtime.ui.components.HourDots
import app.bedtime.data.HourState
import app.bedtime.data.Weather
import app.bedtime.data.WeatherView
import app.bedtime.ui.minimal.rememberWeather
import app.bedtime.service.BlockingState
import app.bedtime.data.NoteAction
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.compose.runtime.rememberCoroutineScope
import android.widget.Toast
import android.app.RemoteInput
import android.accessibilityservice.AccessibilityService
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
                        onAct = ::act,
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

    /**
     * One of a notification's buttons, or a reply. Those that need it ask for the unlock first and lock
     * the phone again straight after, so no app opens and the phone isn't left unlocked.
     */
    private fun act(note: LockNotification, action: NoteAction, reply: String?) {
        val send = {
            val sent = LockNotifications.action(note.key, action.index)?.let { a ->
                val fill = Intent()
                val inputs = a.remoteInputs
                if (reply != null && inputs != null) {
                    RemoteInput.addResultsToIntent(inputs, fill, Bundle().apply { inputs.forEach { putCharSequence(it.resultKey, reply) } })
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) RemoteInput.setResultsSource(fill, RemoteInput.SOURCE_FREE_FORM_INPUT)
                }
                runCatching { a.actionIntent.send(this, 0, fill, null, null, null, sendOptions()) }.isSuccess
            } == true
            Toast.makeText(this, if (!sent) "that didn't go through" else if (reply != null) "sent" else "done", Toast.LENGTH_SHORT).show()
        }
        if (action.needsUnlock) unlockActLock(send) else send()
    }

    /** Asks for the usual unlock, does [action], then locks the phone again; the lock screen returns with the screen. */
    private fun unlockActLock(action: () -> Unit) {
        val keyguard = getSystemService(KeyguardManager::class.java)
        if (!keyguard.isKeyguardLocked) {
            action()
            return
        }
        keyguard.requestDismissKeyguard(
            this,
            object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    action()
                    val locked = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                        BlockingState.service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN) == true
                    if (!locked) finish()
                }
            },
        )
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
    onAct: (LockNotification, NoteAction, String?) -> Unit,
    onFinish: () -> Unit,
    onLightBackground: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val state by Engine.state(context).collectAsStateWithLifecycle()
    val loadedSettings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val settings = loadedSettings ?: AppSettings()
    val notifications by LockNotifications.current.collectAsStateWithLifecycle()
    val quiet by repo.lockQuiet.collectAsStateWithLifecycle(initialValue = emptyMap())
    val scope = rememberCoroutineScope()
    val breezy = remember { Weather.APPS.firstOrNull { AppCatalog.isLaunchable(context, it) } }
    val todayHours = rememberTodayHours()
    // The list folds back to its summary line whenever the screen goes off.
    var listOpen by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { listOpen = false }
    // With several sessions at once, show the one that lasts longest.
    val occurrence = state?.active?.maxByOrNull { it.end }
    val style = settings.homeStyle
    val light = style.palette().isLight
    // With digital refuge as your launcher, it can be your lock screen outside sessions too.
    val always = settings.homeScreen && style.lockAlways

    LaunchedEffect(state, loadedSettings) {
        if (state != null && loadedSettings != null && occurrence == null && !always) onFinish()
    }
    LaunchedEffect(light) { onLightBackground(light) }
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000)
        }
    }

    if (loadedSettings == null || (occurrence == null && !always)) {
        Box(Modifier.fillMaxSize().background(style.palette().background))
        return
    }
    // "Later" and "quiet" last until the session ends; outside one, an hour, and the morning.
    val laterUntil = occurrence?.end ?: (System.currentTimeMillis() + 60 * 60_000L)
    val quietUntil = occurrence?.end ?: nextMorning(now)
    LockScreenContent(
        now = now,
        scheduleName = occurrence?.schedule?.name,
        until = occurrence?.let { formatTime(context, it.end) },
        style = style,
        onOpen = onOpen,
        // A swipe from the middle opens its app, if one is set; every other swipe just unlocks.
        onSwipe = { swipe -> style.lockGestures[swipe]?.let(onOpenApp) ?: onOpen() },
        quote = Quotes.forPeriod(now, style.quoteRefresh, prefs = settings.quotes),
        calendar = rememberCalendarLine(style, now, onLockScreen = true),
        weather = rememberWeather(style, now),
        onWeather = breezy?.let { app -> { onOpenApp(app) } },
        hours = todayHours.takeIf { style.hoursOnLock && settings.distractingApps.isNotEmpty() }
            ?.let { HourlyUse.states(it, now.hour, settings.distractionMinutes) },
        notifications = if (style.lockNotifications) {
            val nowMs = System.currentTimeMillis()
            notifications.filter { it.packageName in style.lockNotificationApps && !LockNotifications.isQuiet(it, quiet, nowMs) }
        } else {
            emptyList()
        },
        listOpen = listOpen,
        onListOpen = { listOpen = it },
        quickReplies = style.quickReplies,
        handlers = NoteHandlers(
            open = onOpenNotification,
            dismiss = LockNotifications::dismiss,
            later = { LockNotifications.later(it, System.currentTimeMillis(), laterUntil) },
            quiet = { note ->
                val key = LockNotifications.quietKey(note)
                scope.launch { repo.quiet(key, quietUntil) }
                // What's already here comes back when the quiet ends, like "later".
                notifications.filter { LockNotifications.quietKey(it) == key }
                    .forEach { LockNotifications.later(it, System.currentTimeMillis(), quietUntil) }
            },
            act = onAct,
        ),
    )
}

@Composable
internal fun LockScreenContent(
    now: LocalDateTime,
    /** The session in the pill; null outside sessions, and there's no pill. */
    scheduleName: String?,
    until: String?,
    style: HomeStyle,
    onOpen: () -> Unit = {},
    onSwipe: (Swipe) -> Unit = {},
    quote: Quote? = null,
    calendar: String? = null,
    weather: WeatherView? = null,
    /** Opens the weather app after the usual unlock. */
    onWeather: (() -> Unit)? = null,
    /** Today by the hour, when it's shown here. */
    hours: List<HourState>? = null,
    onHours: (() -> Unit)? = null,
    preview: Boolean = false,
    notifications: List<LockNotification> = emptyList(),
    listOpen: Boolean = false,
    onListOpen: (Boolean) -> Unit = {},
    quickReplies: List<String> = emptyList(),
    handlers: NoteHandlers = NoteHandlers(),
    initialExpanded: String? = null,
    initialReplying: String? = null,
) {
    var expanded by remember { mutableStateOf(initialExpanded) }
    var replying by remember { mutableStateOf(initialReplying) }
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
                .padding(horizontal = 28.dp)
                .padding(bottom = 24.dp),
        ) {
            Spacer(Modifier.height(if (preview) 12.dp else style.topSpace.dp.dp))
            SessionHeader(now, scheduleName, until, style, calendar, weather = weather, onWeather = onWeather)
            if (hours != null) {
                HourDots(hours, text = style.palette().text, accent = style.palette().accent, modifier = Modifier.padding(top = 12.dp), onClick = onHours)
            }
            if (notifications.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))
                NotificationArea(
                    notes = notifications,
                    style = style,
                    listOpen = listOpen,
                    onListOpen = onListOpen,
                    handlers = handlers,
                    quickReplies = quickReplies,
                    expanded = expanded,
                    onExpand = { expanded = it },
                    replying = replying,
                    onReply = { replying = it },
                    // Open, the list takes the free space and scrolls in it; the quote and "tap to open" stay at the foot.
                    modifier = if (listOpen) Modifier.weight(1f) else Modifier,
                )
            }
            if (notifications.isEmpty() || !listOpen) Spacer(Modifier.weight(1f)) else Spacer(Modifier.height(24.dp))
            if (quote != null && style.showQuote && replying == null) {
                QuoteBlock(quote, style)
                Spacer(Modifier.height(40.dp))
            }
            // While a reply is being written, the keyboard has the foot of the screen: the mark stays out
            // of the way instead of riding up above it and taking the room the reply needs.
            if (replying == null) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(BedtimeIcons.Refuge, contentDescription = null, tint = p.accent, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("tap to open", style = MaterialTheme.typography.labelLarge, color = p.faint)
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

/** The next 7:00, as epoch millis: when "quiet until morning" ends outside a session. */
private fun nextMorning(now: LocalDateTime): Long {
    val today = now.toLocalDate().atTime(7, 0)
    val morning = if (now.isBefore(today)) today else today.plusDays(1)
    return morning.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
}
