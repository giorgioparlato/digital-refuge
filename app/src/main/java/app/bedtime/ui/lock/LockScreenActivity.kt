package app.bedtime.ui.lock

import app.bedtime.ui.minimal.habitsSlot
import app.bedtime.ui.minimal.noteSlots

import app.bedtime.ui.components.rememberNow

import app.bedtime.ui.components.HourDetailDialog
import app.bedtime.ui.minimal.loadCalendarWeek
import app.bedtime.data.NoteWidget
import app.bedtime.ui.notes.NoteWidgetView
import app.bedtime.ui.minimal.WidgetHandlers
import app.bedtime.ui.minimal.WidgetSheet
import app.bedtime.ui.components.rememberNowPlaying
import app.bedtime.ui.components.NowPlayingWidget
import app.bedtime.data.Habits
import app.bedtime.ui.habits.rememberHabits
import app.bedtime.ui.habits.HabitsWidget
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.onClick
import androidx.compose.foundation.gestures.detectTapGestures
import app.bedtime.ui.minimal.QuickNoteDialog
import app.bedtime.data.Notes
import app.bedtime.data.HomeActions
import app.bedtime.ui.components.StatusBarVisibility
import app.bedtime.data.HourlyUse
import app.bedtime.data.Timers
import app.bedtime.ui.components.rememberUseToday
import app.bedtime.ui.components.useWidgetSlot
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
import app.bedtime.data.LockNotifications
import app.bedtime.data.LockNotification
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import app.bedtime.data.Problems
import app.bedtime.engine.Engine
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.Text
import app.bedtime.ui.formatTime
import app.bedtime.ui.homestyle.palette
import app.bedtime.ui.minimal.QuoteBlock
import app.bedtime.ui.minimal.SessionHeader
import app.bedtime.data.Module
import app.bedtime.data.Modules
import app.bedtime.data.Screen
import app.bedtime.ui.theme.BedtimeTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.time.LocalDateTime

/**
 * Shown over the keyguard while a session runs: the minimal home's clock and session, without the apps.
 * It doesn't replace the system lock. A tap asks Android to unlock as usual (PIN, fingerprint…).
 */
class LockScreenActivity : ComponentActivity() {
    private val unlocked = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = finish()
    }

    /** Black and at the lowest brightness, for phones that won't turn the screen off while locked. */
    private val blackout = mutableStateOf(false)

    /** Whether the quick note (and its look-up) is open, so leaving it by Android's swipe can bring this screen back. */
    private val boxOpen = mutableStateOf(false)

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
        // Looked at often while it's in front: Android's own "unlocked" listener needs a permission only
        // system apps get (asking for it anyway crashed the app), and USER_PRESENT alone isn't reliable.
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
                        onScreenOff = ::screenOff,
                        onConfirmed = ::unlockActLock,
                        onOpenRoute = { route -> unlockThen { startActivity(app.bedtime.MainActivity.intent(this, route)) } },
                        onOpenIntent = { intent -> unlockThen { runCatching { startActivity(intent) } } },
                        boxOpen = boxOpen,
                    )
                    if (blackout.value) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(androidx.compose.ui.graphics.Color.Black)
                                .pointerInput(Unit) { detectTapGestures { wake() } },
                        )
                    }
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

    /**
     * The screen off, as Android's own lock does. Some phones ignore that while already locked, so if the
     * screen is still on a moment later it goes black at the lowest brightness until it sleeps as usual;
     * a touch brings it back.
     */
    private fun screenOff() {
        // Android's own lock, as the power button: through the accessibility service, no other permission.
        val asked = when {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.P -> "not on this Android"
            BlockingState.service == null -> "blocking is off"
            BlockingState.service?.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN) == true -> "taken"
            else -> "refused"
        }
        lifecycleScope.launch {
            delay(600)
            if (getSystemService(PowerManager::class.java).isInteractive) {
                // Noted, to see on the phone whether Android's lock ever turns the screen off from here.
                Problems.note("screen off from the lock screen: Android's lock was $asked, the screen stayed on, so it went black instead")
                blackout.value = true
                window.attributes = window.attributes.apply { screenBrightness = 0f }
            }
        }
    }

    private fun wake() {
        blackout.value = false
        window.attributes = window.attributes.apply { screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE }
    }

    override fun onStop() {
        // Asleep now, so the next wake shows the lock screen as it was.
        if (blackout.value) wake()
        // Android's swipe up from the foot, with the quick note open, shows Android's own lock screen over
        // this one: still locked, the screen still on. The box closes and this screen comes back. Not
        // during a call, whose screen this mustn't cover.
        val audio = getSystemService(android.media.AudioManager::class.java)
        if (boxOpen.value && !isFinishing && getSystemService(KeyguardManager::class.java).isKeyguardLocked &&
            getSystemService(PowerManager::class.java).isInteractive && audio.mode == android.media.AudioManager.MODE_NORMAL
        ) {
            boxOpen.value = false
            Problems.attempt("coming back over Android's lock screen") { startActivity(intent(this)) }
        }
        super.onStop()
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
    onScreenOff: () -> Unit = {},
    /** Asks for the usual unlock, does it, and locks the phone again: for ticking a habit from here. */
    onConfirmed: (() -> Unit) -> Unit = { it() },
    /** Digital refuge on one of its pages, after the usual unlock. */
    onOpenRoute: (String) -> Unit = {},
    /** Any other app's screen (an event in the calendar), after the usual unlock. */
    onOpenIntent: (Intent) -> Unit = {},
    /** Whether the quick note is open, kept by the screen. */
    boxOpen: androidx.compose.runtime.MutableState<Boolean> = remember { mutableStateOf(false) },
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
    val (todayHours, useToday) = rememberUseToday()
    val (habitList, habitMarks) = rememberHabits()
    // A session can bring its own widgets.
    // The lock screen's own look and widget options, over the home screen's.
    val lockStyle = settings.homeStyle.on(Screen.LOCK)
    val circles = app.bedtime.data.Circles.of(settings.homeStyle)
    val sessionStyle = Modules.during(lockStyle, state?.active.orEmpty().map { it.start to it.schedule })
    val nowPlaying = if (Modules.isOn(sessionStyle, Screen.LOCK, Module.NOW_PLAYING)) rememberNowPlaying() else null
    var quickNote by boxOpen
    // A gesture's action: a quick note or the notification shade right here, otherwise an app (the camera
    // straight away, any other once unlocked).
    val runAction: (String) -> Unit = { action ->
        when (action) {
            // A quick note and the look-up are one box now.
            HomeActions.NOTE, HomeActions.LOOKUP -> quickNote = true
            HomeActions.NOTIFICATIONS -> BlockingState.globalAction(context, AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            HomeActions.LOCK -> onScreenOff()
            else -> onOpenApp(action)
        }
    }
    var holding by remember { mutableStateOf<Module?>(null) }
    var screenSheet by remember { mutableStateOf(false) }
    if (screenSheet) {
        // Switching a widget from here asks for the usual unlock first, then locks again.
        val sessions = state?.active.orEmpty().map { it.start to it.schedule }
        val owner = Modules.owner(sessions, Screen.LOCK)
        val shown = Modules.during(lockStyle, sessions)
        app.bedtime.ui.minimal.ScreenSheet(
            Screen.LOCK,
            shown,
            Modules.of(shown, Screen.LOCK),
            sessionOwned = owner != null,
            onSwitch = { module, on ->
                onConfirmed {
                    scope.launch {
                        if (owner != null) {
                            repo.upsert(owner.copy(lockModules = Modules.arrange(owner.lockModules.orEmpty()).map { if (it.module == module) it.copy(on = on) else it }))
                        } else {
                            repo.updateSettings { s -> s.copy(homeStyle = Modules.switched(s.homeStyle, Screen.LOCK, module, on)) }
                        }
                    }
                }
            },
            onOpen = onOpenRoute,
            onDismiss = { screenSheet = false },
        )
    }
    var hoursDetail by remember { mutableStateOf(false) }
    var appsDetail by remember { mutableStateOf(false) }
    if (appsDetail) {
        app.bedtime.ui.components.UseAppsDialog(useToday, lockStyle, onDismiss = { appsDetail = false }, onStats = { onOpenRoute(app.bedtime.Routes.STATS) })
    }
    if (hoursDetail) {
        val p = lockStyle.palette()
        HourDetailDialog(todayHours, HourlyUse.rule(settings), p.background, p.text, p.faint, p.accent, onDismiss = { hoursDetail = false }, onStats = {
            onOpenRoute(app.bedtime.Routes.STATS)
        })
    }
    // The week ahead, as on the home screen; names only if you show them on the lock screen.
    var week by remember { mutableStateOf<List<app.bedtime.ui.minimal.CalendarDay>?>(null) }
    week?.let { days ->
        val shown = if (lockStyle.calendarNamesOnLock) days else days.map { d -> d.copy(events = d.events.map { it.copy(title = "", location = "", description = "") }) }
        app.bedtime.ui.minimal.CalendarWeekDialog(shown, lockStyle, onDismiss = { week = null }, onOpenEvent = { event ->
            onOpenIntent(app.bedtime.ui.minimal.CalendarSource.openIntent(event))
            week = null
        })
    }
    val allNotes by repo.notes.collectAsStateWithLifecycle(initialValue = emptyList())
    val widgetNote = allNotes.firstOrNull { it.id == (lockStyle.noteWidgetId ?: Notes.TODO_ID) }
    holding?.let { module ->
        // Changes from here ask for the usual unlock first, then lock again, as ticking a habit does.
        val sessions = state?.active.orEmpty().map { it.start to it.schedule }
        val owner = Modules.owner(sessions, Screen.LOCK)
        WidgetSheet(
            module,
            Modules.during(lockStyle, sessions),
            sessionOwned = owner != null,
            handlers = WidgetHandlers(
                hide = {
                    onConfirmed {
                        scope.launch {
                            if (owner != null) {
                                repo.upsert(owner.copy(lockModules = owner.lockModules.orEmpty().map { if (it.module == module) it.copy(on = false) else it }))
                            } else {
                                repo.updateSettings { s -> s.copy(homeStyle = Modules.switched(s.homeStyle, Screen.LOCK, module, false)) }
                            }
                        }
                    }
                },
                change = { changed ->
                    onConfirmed {
                        scope.launch { repo.updateSettings { s -> s.copy(homeStyle = s.homeStyle.edit(Screen.LOCK) { it.sheetFields(changed) }) } }
                    }
                },
                open = onOpenRoute,
                habitShown = { id, shown -> onConfirmed { scope.launch { repo.updateSettings { s -> s.copy(habits = s.habits.map { if (it.id == id) it.withShownOn(Screen.LOCK, shown) else it }) } } } },
            ),
            onDismiss = { holding = null },
            notes = allNotes,
            screen = Screen.LOCK,
            habits = habitList,
        )
    }
    // A quick note, and with a single word your dictionaries, read right here: nothing to unlock for a word.
    // Notes only if you've said so: here, anyone holding the phone could read them.
    // Notes by title only: one opens, in refuge or Obsidian, once you've unlocked, never here.
    if (quickNote) {
        val schedules by repo.schedules.collectAsStateWithLifecycle(initialValue = emptyList())
        val blocks = schedules.filter { it.isBlock && it.enabled }
        QuickNoteDialog(
            lockStyle,
            settings.quickTargets,
            onSave = { words, target -> scope.launch { Toast.makeText(context, Notes.save(context, repo, words, target), Toast.LENGTH_SHORT).show() } },
            onDismiss = { quickNote = false },
            vault = settings.obsidianVault,
            lookUp = app.bedtime.ui.minimal.LookUpWith(
                notes = settings.lookUpNotes,
                elsewhere = app.bedtime.ui.minimal.NotesOpenElsewhere(
                    refuge = { id ->
                        quickNote = false
                        onOpenRoute(app.bedtime.Routes.note(id))
                    },
                    vault = { file ->
                        quickNote = false
                        // Obsidian knows the vault by its folder's name, not by the address Android gave refuge for it.
                        settings.obsidianVault?.let { tree ->
                            scope.launch {
                                val name = withContext(Dispatchers.IO) { app.bedtime.data.FolderIndex.folderName(context, android.net.Uri.parse(tree)) }
                                onOpenIntent(app.bedtime.data.FolderIndex.obsidianIntent(name, file))
                            }
                        }
                    },
                ),
            ),
            offers = { words -> app.bedtime.ui.minimal.AppDrawer.noteOffers(words, settings, blocks, LocalDateTime.now()) },
            onOffer = { offer -> runOffer(context, repo, scope, settings, offer) },
        )
    }
    // The list folds back to its summary line whenever the screen goes off.
    var listOpen by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { listOpen = false }
    // With several sessions at once, show the one that lasts longest.
    val occurrence = state?.active?.maxByOrNull { it.end }
    val style = sessionStyle
    val light = style.palette().isLight
    // With digital refuge as your launcher, it can be your lock screen outside sessions too.
    val always = settings.homeScreen && style.lockAlways

    LaunchedEffect(state, loadedSettings) {
        if (state != null && loadedSettings != null && occurrence == null && !always) onFinish()
    }
    LaunchedEffect(light) { onLightBackground(light) }
    StatusBarVisibility(hidden = style.hideStatusBar)
    // Timers counting down: those set from the search, and the clock app's own.
    val refugeTimers by repo.timers.collectAsStateWithLifecycle(initialValue = emptyList())
    val clockTimers by app.bedtime.data.Timers.clock.collectAsStateWithLifecycle()
    // Each second only while a timer counts down; otherwise each minute.
    val now = rememberNow(everySecond = refugeTimers.isNotEmpty() || clockTimers.isNotEmpty())
    val hiddenTimers by app.bedtime.data.Timers.hidden.collectAsStateWithLifecycle()

    if (loadedSettings == null || (occurrence == null && !always)) {
        Box(Modifier.fillMaxSize().background(style.palette().background))
        return
    }
    // "Later" and "quiet" last until the session ends; outside one, an hour, and the morning.
    val laterUntil = occurrence?.end ?: (System.currentTimeMillis() + 60 * 60_000L)
    val quietUntil = occurrence?.end ?: nextMorning(now)
    // Like a quick note, writing in it here saves as you go.
    val lockNote = noteSlots(style, Screen.LOCK, widgetNote, onWrite = { n, text -> scope.launch { repo.editNote(n.id, text) } }, onHold = { holding = Module.NOTE })
    LockScreenContent(
        now = now,
        timers = app.bedtime.data.Timers.running(refugeTimers, clockTimers, System.currentTimeMillis(), hiddenTimers),
        onHideTimer = app.bedtime.data.Timers::hide,
        scheduleName = occurrence?.schedule?.name,
        scheduleIcon = occurrence?.schedule?.icon,
        nowPlaying = nowPlaying?.let { playing -> { NowPlayingWidget(playing, style.palette().text, style.palette().faint, Modifier.padding(top = 20.dp), onOpen = onOpenApp) } },
        onWidgetHold = { holding = it },
        onScreenHold = { screenSheet = true },
        // Ticking, or saving what was typed, asks for the usual unlock, then locks again; a tap on the title opens it after unlocking.
        // Like a quick note, writing in it here saves as you go.
        note = lockNote.inList,
        noteBeside = lockNote.beside,
        // Ticking from here asks for the usual unlock first, then locks again, as a reply does.
        habits = habitsSlot(
            style, Screen.LOCK, habitList, habitMarks,
            onMark = { h, day, value -> onConfirmed { scope.launch { repo.markHabit(h.id, day, value) } } },
            onOpen = {},
            onHold = { holding = Module.HABITS },
        ),
        until = occurrence?.let { formatTime(context, it.end) },
        style = style,
        onOpen = onOpen,
        // A swipe from around the clock does what you set; every other swipe just unlocks.
        onSwipe = { swipe -> style.lockGestures[swipe]?.let(runAction) ?: onOpen() },
        onDoubleTap = style.lockGestures[Swipe.DOUBLE_TAP]?.let { action -> { runAction(action) } },
        onClockDoubleTap = (style.lockGestures[Swipe.CLOCK_DOUBLE_TAP] ?: style.lockGestures[Swipe.DOUBLE_TAP])?.let { action -> { runAction(action) } },
        onShade = { BlockingState.globalAction(context, AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS) },
        quote = Quotes.forPeriod(now, style.quoteRefresh, prefs = settings.quotes),
        calendar = rememberCalendarLine(style, now, onLockScreen = true),
        onCalendar = { scope.launch { week = loadCalendarWeek(context, LocalDateTime.now()) } },
        // The distraction tracker's tap: today's detail right here, as at home; only the statistics ask for the unlock.
        // The dots open today by the hour; the other looks, today by app.
        onHours = { if (style.useWidgetFor(Screen.LOCK).look == app.bedtime.data.UseLook.DOTS) hoursDetail = true else appsDetail = true },
        weather = rememberWeather(style, now, Screen.LOCK),
        onWeather = breezy?.let { app -> { onOpenApp(app) } },
        hours = todayHours.takeIf { Modules.isOn(style, Screen.LOCK, Module.HOURS) && HourlyUse.counting(settings) }
            ?.let { HourlyUse.states(it, now.hour, HourlyUse.rule(settings)) },
        useToday = useToday.takeIf { Modules.isOn(style, Screen.LOCK, Module.HOURS) },
        // A session can show its own apps, people and circles here, or none at all. Your circles come first.
        notifications = LockNotifications.chosen(style.lockNotificationApps, circles, state?.active?.map { it.schedule }.orEmpty())
            ?.takeIf { style.lockNotifications }
            ?.let { chosen ->
                val nowMs = System.currentTimeMillis()
                LockNotifications.sorted(notifications.filter { LockNotifications.wanted(it, chosen) && !LockNotifications.isQuiet(it, quiet, nowMs) }, circles)
            }
            .orEmpty(),
        circles = circles,
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
    /** Running timers' ends, soonest first: each a line counting down, swiped away with [onHideTimer]. */
    timers: List<Long> = emptyList(),
    onHideTimer: (Long) -> Unit = {},
    /** The session in the pill; null outside sessions, and there's no pill. */
    scheduleName: String?,
    until: String?,
    style: HomeStyle,
    onOpen: () -> Unit = {},
    onSwipe: (Swipe) -> Unit = {},
    quote: Quote? = null,
    calendar: String? = null,
    /** The week ahead, from a tap on the next event (or its icon). */
    onCalendar: (() -> Unit)? = null,
    weather: WeatherView? = null,
    /** Opens the weather app after the usual unlock. */
    onWeather: (() -> Unit)? = null,
    /** Today by the hour, when it's shown here. */
    hours: List<HourState>? = null,
    /** Today's figures, for the tracker's looks other than the dots and the line under them. */
    useToday: app.bedtime.data.UseToday? = null,
    /** A double tap anywhere, when you've set one; a single tap then waits a moment to be sure. */
    onDoubleTap: (() -> Unit)? = null,
    /** A double tap on the clock: its own, or else the one anywhere. */
    onClockDoubleTap: (() -> Unit)? = null,
    /** A pull down from the very top: the notification shade. */
    onShade: () -> Unit = {},
    onHours: (() -> Unit)? = null,
    preview: Boolean = false,
    notifications: List<LockNotification> = emptyList(),
    /** Your circles, for each notification's colour. */
    circles: List<app.bedtime.data.Circle> = emptyList(),
    listOpen: Boolean = false,
    onListOpen: (Boolean) -> Unit = {},
    quickReplies: List<String> = emptyList(),
    handlers: NoteHandlers = NoteHandlers(),
    initialExpanded: String? = null,
    initialReplying: String? = null,
    /** The session's mark on its pill. */
    scheduleIcon: String? = null,
    /** The habits and now-playing widgets, drawn by the screen, when they're on. */
    habits: (@Composable () -> Unit)? = null,
    nowPlaying: (@Composable () -> Unit)? = null,
    /** A long press on a widget: its own sheet, to hide or change it. */
    onWidgetHold: ((Module) -> Unit)? = null,
    /** A long press on an empty spot: this screen's widgets, and the way to their settings. */
    onScreenHold: (() -> Unit)? = null,
    note: (@Composable () -> Unit)? = null,
    noteBeside: (@Composable () -> Unit)? = null,
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
                .semantics { onClick(label = "unlock the phone") { onOpen(); true } }
                .pointerInput(onOpen, onDoubleTap) {
                    detectTapGestures(onTap = { onOpen() }, onDoubleTap = onDoubleTap?.let { { _: Offset -> it() } }, onLongPress = onScreenHold?.let { { _: Offset -> it() } })
                }
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
                                LockSwipe.Shade -> onShade()
                                is LockSwipe.From -> onSwipe(read.swipe)
                            }
                        },
                    )
                }
                .then(if (preview) Modifier else Modifier.safeDrawingPadding())
                .padding(horizontal = 28.dp)
                .padding(bottom = 24.dp),
        ) {
            // The quote keeps its place at the foot when it's the last widget, as it always has; anywhere else it's in line.
            val modules = Modules.shown(style, Screen.LOCK)
            val quoteAtFoot = modules.lastOrNull() == Module.QUOTE
            // While a reply is being written, the widgets step aside: the conversation and the box get the
            // room above the keyboard, with only the time left at the top.
            if (replying != null) {
                Spacer(Modifier.height(12.dp))
                Text(app.bedtime.ui.timeFormatter(LocalContext.current).format(now), style = MaterialTheme.typography.titleMedium, color = p.faint)
            } else {
                Spacer(Modifier.height(if (preview) 12.dp else (style.lockTopSpace ?: style.topSpace).dp.dp))
                SessionHeader(
                    now, scheduleName, until, style, calendar, onCalendar, weather = weather, onWeather = onWeather,
                    modules = modules,
                    scheduleIcon = scheduleIcon,
                    habits = habits,
                    nowPlaying = nowPlaying,
                    note = note,
                    noteBeside = noteBeside,
                    onHold = onWidgetHold,
                    screen = Screen.LOCK,
                    onClockTap = onOpen,
                    onClockDoubleTap = onClockDoubleTap,
                    hours = useWidgetSlot(style.useWidgetFor(Screen.LOCK), hours, useToday, style, onHours, onWidgetHold?.let { { it(Module.HOURS) } }),
                    quote = quote?.takeIf { !quoteAtFoot }?.let { q ->
                        {
                            Spacer(Modifier.height(24.dp))
                            QuoteBlock(q, style)
                        }
                    },
                )
                val nowMillis = now.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                timers.forEach { end -> key(end) { TimerLine(end, nowMillis, style) { onHideTimer(end) } } }
            }
            // The free space below the widgets: the quote keeps its place at the foot, and the notifications,
            // from the top, take as much as they need, going over the quote when there are many.
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (quote != null && quoteAtFoot && replying == null) {
                    Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()) {
                        QuoteBlock(quote, style)
                        Spacer(Modifier.height(40.dp))
                    }
                }
                if (notifications.isNotEmpty()) {
                    NotificationArea(
                        notes = notifications,
                        style = style,
                        circles = circles,
                        listOpen = listOpen,
                        onListOpen = onListOpen,
                        handlers = handlers,
                        quickReplies = quickReplies,
                        expanded = expanded,
                        onExpand = { expanded = it },
                        replying = replying,
                        onReply = { replying = it },
                        modifier = Modifier
                            .padding(top = if (replying != null) 16.dp else 28.dp, bottom = if (replying != null) 8.dp else 24.dp)
                            .then(if (listOpen || replying != null) Modifier.fillMaxHeight() else Modifier),
                    )
                }
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

/**
 * What the quick note offered, done without unlocking: a timer in your clock app (counted down here),
 * an event in your calendar, a block started, a sum copied. Each only makes the phone stricter or
 * adds something; nothing here opens a way into it.
 */
private fun runOffer(context: Context, repo: Repository, scope: kotlinx.coroutines.CoroutineScope, settings: AppSettings, offer: app.bedtime.ui.minimal.DrawerResult) {
    val say = { text: String -> Toast.makeText(context, text, Toast.LENGTH_SHORT).show() }
    when (offer) {
        is app.bedtime.ui.minimal.DrawerResult.Clock -> {
            val timer = offer.action as? app.bedtime.data.ClockAction.Timer ?: return
            runCatching { context.startActivity(app.bedtime.data.ClockActions.intent(timer)) }
                .onSuccess {
                    scope.launch { repo.addTimer(System.currentTimeMillis() + timer.seconds * 1000L) }
                    say("timer started")
                }
                .onFailure { say("no clock app took it") }
        }
        // Calendar access can't be asked for here: it's given once, from the search or its settings.
        is app.bedtime.ui.minimal.DrawerResult.AddEvent -> if (!app.bedtime.ui.minimal.CalendarSource.hasPermission(context) || !app.bedtime.ui.minimal.CalendarSource.canWrite(context)) {
            say("allow calendar access under settings › search first")
        } else {
            scope.launch {
                val saved = withContext(Dispatchers.IO) { app.bedtime.ui.minimal.CalendarSource.insert(context, offer.event, settings.eventCalendarId) }
                say(if (saved) "added to your calendar" else "couldn't add it to a calendar")
            }
        }
        is app.bedtime.ui.minimal.DrawerResult.StartBlock -> scope.launch {
            repo.startBlock(offer.block, offer.block.durationMinutes)
            say("${offer.block.name} until ${formatTime(context, System.currentTimeMillis() + offer.block.durationMinutes * 60_000L)}".lowercase())
        }
        is app.bedtime.ui.minimal.DrawerResult.Calculation -> {
            context.getSystemService(android.content.ClipboardManager::class.java)
                ?.setPrimaryClip(android.content.ClipData.newPlainText("answer", offer.answer))
            say("copied ${offer.answer}")
        }
        else -> Unit
    }
}

/** The next 7:00, as epoch millis: when "quiet until morning" ends outside a session. */
private fun nextMorning(now: LocalDateTime): Long {
    val today = now.toLocalDate().atTime(7, 0)
    val morning = if (now.isBefore(today)) today else today.plusDays(1)
    return morning.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
}

/** A timer counting down, under the widgets; a swipe either way hides it until it ends. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimerLine(end: Long, now: Long, style: HomeStyle, onHide: () -> Unit) {
    val p = style.palette()
    val swipe = rememberSwipeToDismissBoxState(confirmValueChange = { value ->
        if (value != SwipeToDismissBoxValue.Settled) onHide()
        true
    })
    SwipeToDismissBox(swipe, backgroundContent = {}, modifier = Modifier.padding(top = 20.dp)) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.text.copy(alpha = 0.06f)).padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("timer", style = MaterialTheme.typography.labelLarge, color = p.faint, modifier = Modifier.weight(1f))
            Text(Timers.left(end, now), style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"), color = p.text)
            Spacer(Modifier.width(6.dp))
            Text("left", style = MaterialTheme.typography.labelLarge, color = p.faint)
            Spacer(Modifier.width(4.dp))
            Box(Modifier.size(32.dp).clip(CircleShape).clickable(onClickLabel = "hide this timer", onClick = onHide), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Close, contentDescription = null, tint = p.faint, modifier = Modifier.size(16.dp))
            }
        }
    }
}
