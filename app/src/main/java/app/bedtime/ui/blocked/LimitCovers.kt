package app.bedtime.ui.blocked

import app.bedtime.unlock.TypingChallenge
import app.bedtime.data.TextSource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.CoverMessages
import app.bedtime.apps.AppCatalog
import app.bedtime.data.AppLimit
import app.bedtime.data.OutOfTime
import app.bedtime.data.Repository
import app.bedtime.data.UsageDay
import app.bedtime.engine.Limits
import app.bedtime.service.LimitPasses
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.Breathing
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
internal fun PauseScreen(
    pkg: String,
    seconds: Int,
    opens: Int,
    passKey: String,
    onHome: () -> Unit,
    onFinish: () -> Unit,
    typing: Int = 0,
    source: TextSource = TextSource.WORDS,
) {
    val context = LocalContext.current
    val label = remember(pkg) { AppCatalog.label(context, pkg) }
    val settings by Repository.get(context).settings.collectAsStateWithLifecycle(initialValue = null)
    // The words of the limit whose pause this is: the one asking the longest, as the pause itself is.
    val message = remember(pkg, opens, settings == null) {
        val loaded = settings ?: return@remember null
        Limits.covering(loaded.limits, pkg, LocalDate.now().toEpochDay())
            .filter { (it.rules.pauseSeconds ?: 0) > 0 }
            .maxByOrNull { it.rules.pauseSeconds ?: 0 }
            ?.let { CoverMessages.pick(it.message, loaded.customMessages) }
    }
    var left by rememberSaveable(pkg, opens) { mutableIntStateOf(if (typing > 0) 0 else seconds) }
    var typed by rememberSaveable(pkg, opens) { mutableStateOf(typing <= 0) }
    LaunchedEffect(pkg, opens) {
        while (left > 0) {
            delay(1_000)
            left--
        }
    }
    // Typing first, when the limit asks for it; the way to open appears once it's done.
    if (!typed) {
        Column(
            Modifier.fillMaxSize().background(Obsidian.colors.bgPrimary).safeDrawingPadding().imePadding().padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text("About to open $label", style = MaterialTheme.typography.titleLarge, color = Obsidian.colors.textNormal)
            Text(
                if (opens > 1) "type this first · the ${ordinal(opens)} time today" else "type this first",
                style = MaterialTheme.typography.bodyMedium,
                color = Obsidian.colors.textMuted,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )
            TypingChallenge(length = typing, source = source, onPassed = { typed = true })
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onHome, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Never mind", color = Obsidian.colors.textMuted) }
        }
        return
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
        message = message,
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
internal fun PauseContent(appLabel: String, secondsLeft: Int, opens: Int, onOpen: () -> Unit, onHome: () -> Unit, message: String? = null) {
    val c = Obsidian.colors
    val done = secondsLeft <= 0
    CoverLayout(
        emblem = {
            Breathing(running = !done, color = c.accent, modifier = Modifier.size(180.dp)) { inhaling ->
                // Once the breaths are done the disc rests empty: the choice is yours, nothing to read.
                if (!done) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(if (inhaling) "breathe in" else "breathe out", style = MaterialTheme.typography.titleMedium, color = c.textNormal)
                        Text("$secondsLeft", style = MaterialTheme.typography.labelLarge, color = c.textFaint)
                    }
                }
            }
        },
        pill = if (opens > 1) "the ${ordinal(opens)} time today" else null,
        pillColor = c.orange,
        title = "About to open $appLabel",
        body = message ?: "Is this what you came to the phone for?",
        primary = CoverAction("Never mind", onHome),
        secondary = listOf(CoverAction(if (done) "Open $appLabel" else "Open $appLabel in a moment", onOpen, enabled = done)),
    )
}

/**
 * A limit's minutes are gone for today, or with [opens] its opens: what's left is home, or — if the
 * limit allows — more. Out of opens, "more" is one more open, or the rest of the day.
 */
@Composable
internal fun LimitScreen(pkg: String, limitId: String, onHome: () -> Unit, onFinish: () -> Unit, opens: Boolean = false) {
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
    val message = remember(limitId, limit == null) {
        val loaded = settings ?: return@remember null
        limit?.let { CoverMessages.pick(it.message, loaded.customMessages) }
    }
    // A minute's slack: the last seconds counted may still be on their way to storage.
    val stillOut = limit != null && if (opens) {
        (Limits.opensLeft(limit, pkg, day) ?: Int.MAX_VALUE) <= 0
    } else {
        (Limits.secondsLeft(limit, pkg, day) ?: Long.MAX_VALUE) <= 60L
    }

    // Deleted, changed, lifted or given more time since: nothing left to cover.
    LaunchedEffect(settings, usage) {
        if (settings != null && usage != null && !stillOut && !granted) onFinish()
    }
    Surface(Modifier.fillMaxSize(), color = c.bgPrimary) {
        if (limit == null) return@Surface
        val rules = limit.rules
        val name = limit.name.ifBlank { "this limit" }
        // Opens have no length to choose: more is one more open.
        val choosing = rules.outOfTime == OutOfTime.CHOOSE && !opens
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
            val minutes: Int? = when {
                rules.outOfTime == OutOfTime.REST_OF_DAY -> null
                opens -> 0
                rules.outOfTime == OutOfTime.CHOOSE -> chosen.takeIf { it != REST }
                else -> rules.extraMinutes
            }
            UnlockSteps(
                key = "$limitId-$today",
                name = name,
                config = rules.unlock,
                recentUnlocks = day.extras[limitId]?.size ?: 0,
                outcome = when {
                    minutes == null -> "This lifts $name for the rest of today."
                    opens -> "This gives you one more open today."
                    else -> "This gives you $minutes more minutes today."
                },
                success = when {
                    minutes == null -> "$name is lifted until tomorrow."
                    opens -> "One more open. Make it count."
                    else -> "$minutes more minutes. Spend them well."
                },
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
                dailyOpens = rules.dailyOpens.takeIf { opens },
                outOfTime = rules.outOfTime,
                extraMinutes = rules.extraMinutes,
                onHome = onHome,
                onMore = { unlocking = true },
                message = message,
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
    message: String? = null,
    /** The day's opens, when it's they that are gone rather than the minutes. */
    dailyOpens: Int? = null,
) {
    if (dailyOpens != null) {
        val allowance = if (dailyOpens == 1) "1 open" else "$dailyOpens opens"
        CoverLayout(
            message = message,
            emblem = { CoverEmblem(BedtimeIcons.Hourglass) },
            pill = "$limitName · $allowance a day",
            title = "$appLabel is done for today",
            body = when (outOfTime) {
                OutOfTime.TOMORROW -> "That was today's $allowance. It'll be back tomorrow."
                OutOfTime.REST_OF_DAY -> "That was today's $allowance. If it really can't wait, you can lift the limit for the rest of today."
                else -> "That was today's $allowance. If it really can't wait, you can take one more."
            },
            primary = CoverAction("Go to home screen", onHome),
            secondary = when (outOfTime) {
                OutOfTime.TOMORROW -> emptyList()
                OutOfTime.REST_OF_DAY -> listOf(CoverAction("Lift it for today", onMore))
                else -> listOf(CoverAction("Open it once more", onMore))
            },
        )
        return
    }
    val budget = formatMinutes(dailyMinutes.toLong())
    CoverLayout(
        message = message,
        emblem = { CoverEmblem(BedtimeIcons.Hourglass) },
        pill = "$limitName · $budget a day",
        title = "$appLabel is done for today",
        body = when (outOfTime) {
            OutOfTime.TOMORROW -> "That was today's $budget. It'll be back tomorrow."
            OutOfTime.MORE_TIME -> "That was today's $budget. If it really can't wait, you can take $extraMinutes more minutes."
            OutOfTime.REST_OF_DAY -> "That was today's $budget. If it really can't wait, you can lift the limit for the rest of today."
            OutOfTime.CHOOSE -> "That was today's $budget. If it really can't wait, you can say how much more you need."
        },
        primary = CoverAction("Go to home screen", onHome),
        secondary = when (outOfTime) {
            OutOfTime.TOMORROW -> emptyList()
            OutOfTime.MORE_TIME -> listOf(CoverAction("Take $extraMinutes more minutes", onMore))
            OutOfTime.CHOOSE -> listOf(CoverAction("Take more time", onMore))
            OutOfTime.REST_OF_DAY -> listOf(CoverAction("Lift it for today", onMore))
        },
    )
}
