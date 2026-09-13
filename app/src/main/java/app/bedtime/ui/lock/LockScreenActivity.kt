package app.bedtime.ui.lock

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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.AppSettings
import app.bedtime.data.HomeStyle
import app.bedtime.data.Quote
import app.bedtime.data.Quotes
import app.bedtime.data.Repository
import app.bedtime.engine.Engine
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.Text
import app.bedtime.ui.formatTime
import app.bedtime.ui.homestyle.palette
import app.bedtime.ui.minimal.QuoteBlock
import app.bedtime.ui.minimal.SessionHeader
import app.bedtime.ui.theme.BedtimeTheme
import kotlinx.coroutines.delay
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
        ContextCompat.registerReceiver(this, unlocked, IntentFilter(Intent.ACTION_USER_PRESENT), ContextCompat.RECEIVER_NOT_EXPORTED)
        setContent {
            BedtimeTheme { LockScreen(onOpen = ::openPhone, onFinish = ::finish, onLightBackground = ::useLightSystemBars) }
        }
    }

    override fun onResume() {
        super.onResume()
        // Screen on without a keyguard (no lock set, or still inside the lock delay): nothing to cover.
        val interactive = getSystemService(PowerManager::class.java).isInteractive
        if (interactive && !getSystemService(KeyguardManager::class.java).isKeyguardLocked) finish()
    }

    override fun onDestroy() {
        unregisterReceiver(unlocked)
        super.onDestroy()
    }

    private fun openPhone() {
        getSystemService(KeyguardManager::class.java).requestDismissKeyguard(
            this,
            object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() = finish()
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
private fun LockScreen(onOpen: () -> Unit, onFinish: () -> Unit, onLightBackground: (Boolean) -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val state by Engine.state(context).collectAsStateWithLifecycle()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = AppSettings())
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
        quote = Quotes.forDay(now.toLocalDate()),
    )
}

@Composable
internal fun LockScreenContent(
    now: LocalDateTime,
    scheduleName: String,
    until: String,
    style: HomeStyle,
    onOpen: () -> Unit = {},
    quote: Quote? = null,
    preview: Boolean = false,
) {
    val p = style.palette()
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
            .then(if (preview) Modifier else Modifier.safeDrawingPadding())
            .padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        Spacer(Modifier.height(if (preview) 12.dp else 32.dp))
        SessionHeader(now, scheduleName, until, style)
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
