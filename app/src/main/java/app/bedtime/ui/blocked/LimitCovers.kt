package app.bedtime.ui.blocked

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.apps.AppCatalog
import app.bedtime.data.AppLimit
import app.bedtime.data.OutOfTime
import app.bedtime.data.Repository
import app.bedtime.data.UsageDay
import app.bedtime.engine.Limits
import app.bedtime.service.LimitPasses
import app.bedtime.ui.components.Breathing
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.StatusPill
import app.bedtime.ui.components.Text
import app.bedtime.ui.formatMinutes
import app.bedtime.ui.theme.Obsidian
import app.bedtime.unlock.REST
import app.bedtime.unlock.UnlockLengthChoice
import app.bedtime.unlock.UnlockSteps
import java.time.Duration
import java.time.LocalDateTime
import kotlinx.coroutines.delay
import java.time.LocalDate

/** The pause before opening an app with a limit: a few breaths, then open it or let it be. */
@Composable
internal fun PauseScreen(pkg: String, seconds: Int, opens: Int, passKey: String, onHome: () -> Unit, onFinish: () -> Unit) {
    val context = LocalContext.current
    val label = remember(pkg) { AppCatalog.label(context, pkg) }
    var left by rememberSaveable(pkg, opens) { mutableIntStateOf(seconds) }
    LaunchedEffect(pkg, opens) {
        while (left > 0) {
            delay(1_000)
            left--
        }
    }
    PauseContent(
        appLabel = label,
        secondsLeft = left,
        opens = opens,
        onOpen = {
            LimitPasses.grant(passKey, System.currentTimeMillis())
            AppCatalog.launch(context, pkg)
            onFinish()
        },
        onHome = onHome,
    )
}

/** "1st", "2nd", "3rd"… for how many times today. */
private fun ordinal(n: Int): String {
    val suffix = when {
        n % 100 in 11..13 -> "th"
        n % 10 == 1 -> "st"
        n % 10 == 2 -> "nd"
        n % 10 == 3 -> "rd"
        else -> "th"
    }
    return "$n$suffix"
}

@Composable
internal fun PauseContent(appLabel: String, secondsLeft: Int, opens: Int, onOpen: () -> Unit, onHome: () -> Unit) {
    val c = Obsidian.colors
    val done = secondsLeft <= 0
    Column(
        Modifier.fillMaxSize().background(c.bgPrimary).safeDrawingPadding().padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Text("about to open $appLabel", style = MaterialTheme.typography.titleMedium, color = c.textMuted)
        if (opens > 1) {
            Spacer(Modifier.height(10.dp))
            StatusPill("the ${ordinal(opens)} time today", c.orange)
        }
        Spacer(Modifier.height(36.dp))
        Breathing(running = !done, color = c.accent, modifier = Modifier.size(220.dp)) { inhaling ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    if (done) "and now?" else if (inhaling) "breathe in" else "breathe out",
                    style = MaterialTheme.typography.headlineSmall,
                    color = c.textNormal,
                )
                if (!done) Text("$secondsLeft", style = MaterialTheme.typography.labelLarge, color = c.textFaint)
            }
        }
        Spacer(Modifier.height(36.dp))
        Text(
            "Is this what you came to the phone for?",
            style = MaterialTheme.typography.bodyLarge,
            color = c.textMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1.3f))
        CtaButton("Never mind", onClick = onHome, modifier = Modifier.fillMaxWidth())
        TextButton(onClick = onOpen, enabled = done, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            Text(if (done) "Open $appLabel" else "Open $appLabel in a moment", color = if (done) c.textMuted else c.textFaint)
        }
    }
}

/** A limit's minutes are gone for today: what's left is home, or — if the limit allows — more time. */
@Composable
internal fun LimitScreen(pkg: String, limitId: String, onHome: () -> Unit, onFinish: () -> Unit) {
    val context = LocalContext.current
    val c = Obsidian.colors
    val repo = remember { Repository.get(context) }
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val usage by repo.usage.collectAsStateWithLifecycle(initialValue = null)
    val label = remember(pkg) { AppCatalog.label(context, pkg) }
    var unlocking by rememberSaveable { mutableStateOf(false) }
    /** With "choose then": minutes picked, [REST] for the rest of today, or -1 before choosing. */
    var chosen by rememberSaveable { mutableIntStateOf(-1) }
    var granted by remember { mutableStateOf(false) }
    val today = LocalDate.now().toEpochDay()
    val day = usage?.lastOrNull { it.day == today } ?: UsageDay(today)
    val limit: AppLimit? = settings?.let { s -> Limits.settle(s.limits, today).firstOrNull { it.id == limitId } }
    // A minute's slack: the last seconds counted may still be on their way to storage.
    val stillOut = limit != null && (Limits.secondsLeft(limit, pkg, day) ?: Long.MAX_VALUE) <= 60L

    // Deleted, changed, lifted or given more time since: nothing left to cover.
    LaunchedEffect(settings, usage) {
        if (settings != null && usage != null && !stillOut && !granted) onFinish()
    }
    Surface(Modifier.fillMaxSize(), color = c.bgPrimary) {
        if (limit == null) return@Surface
        val rules = limit.rules
        val name = limit.name.ifBlank { "this limit" }
        val choosing = rules.outOfTime == OutOfTime.CHOOSE
        if (unlocking && choosing && chosen < 0) {
            val untilMidnight = Duration.between(LocalDateTime.now(), LocalDate.now().plusDays(1).atStartOfDay()).toMinutes().toInt()
            UnlockLengthChoice(
                name = name,
                maxMinutes = untilMidnight,
                restLabel = "the rest of today",
                onChoose = { chosen = it ?: REST },
                onCancel = { unlocking = false },
                modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding(),
            )
        } else if (unlocking) {
            // Minutes given, or null to lift the limit until midnight.
            val minutes: Int? = when (rules.outOfTime) {
                OutOfTime.CHOOSE -> chosen.takeIf { it != REST }
                OutOfTime.REST_OF_DAY -> null
                else -> rules.extraMinutes
            }
            UnlockSteps(
                key = "$limitId-$today",
                name = name,
                config = rules.unlock,
                recentUnlocks = day.extras[limitId]?.size ?: 0,
                outcome = if (minutes == null) "This lifts $name for the rest of today." else "This gives you $minutes more minutes today.",
                success = if (minutes == null) "$name is lifted until tomorrow." else "$minutes more minutes. Spend them well.",
                onComplete = {
                    granted = true
                    repo.grantLimit(today, limitId, System.currentTimeMillis(), minutes)
                },
                onUnlocked = {
                    AppCatalog.launch(context, pkg)
                    onFinish()
                },
                // Backing out of the steps goes back to choosing, when there was a choice.
                onCancel = { if (choosing) chosen = -1 else unlocking = false },
                modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding(),
            )
        } else {
            LimitContent(
                appLabel = label,
                limitName = name,
                dailyMinutes = rules.dailyMinutes ?: 0,
                outOfTime = rules.outOfTime,
                extraMinutes = rules.extraMinutes,
                onHome = onHome,
                onMore = { unlocking = true },
            )
        }
    }
}

@Composable
internal fun LimitContent(
    appLabel: String,
    limitName: String,
    dailyMinutes: Int,
    outOfTime: OutOfTime,
    extraMinutes: Int,
    onHome: () -> Unit,
    onMore: () -> Unit,
) {
    val c = Obsidian.colors
    val budget = formatMinutes(dailyMinutes.toLong())
    Column(
        Modifier.fillMaxSize().background(c.bgPrimary).safeDrawingPadding().padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Breathing(running = false, color = c.accent, modifier = Modifier.size(124.dp)) {
            Text(budget, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Light, color = c.accentText)
        }
        Spacer(Modifier.height(28.dp))
        StatusPill("$limitName · $budget a day", c.accentText)
        Spacer(Modifier.height(16.dp))
        Text(
            "$appLabel is done for today",
            style = MaterialTheme.typography.headlineMedium,
            color = c.textNormal,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            when (outOfTime) {
                OutOfTime.TOMORROW -> "That was today's $budget. It'll be back tomorrow."
                OutOfTime.MORE_TIME -> "That was today's $budget. If it really can't wait, you can take $extraMinutes more minutes."
                OutOfTime.REST_OF_DAY -> "That was today's $budget. If it really can't wait, you can lift the limit for the rest of today."
                OutOfTime.CHOOSE -> "That was today's $budget. If it really can't wait, you can say how much more you need."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = c.textMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.weight(1.3f))
        CtaButton("Go to home screen", onClick = onHome, modifier = Modifier.fillMaxWidth())
        if (outOfTime != OutOfTime.TOMORROW) {
            TextButton(onClick = onMore, modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                Text(
                    when (outOfTime) {
                        OutOfTime.MORE_TIME -> "Take $extraMinutes more minutes"
                        OutOfTime.CHOOSE -> "Take more time"
                        else -> "Lift it for today"
                    },
                    color = c.textMuted,
                )
            }
        }
    }
}
