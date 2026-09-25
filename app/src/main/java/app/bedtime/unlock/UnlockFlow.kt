package app.bedtime.unlock

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import app.bedtime.ui.components.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.Repository
import app.bedtime.data.Schedule
import app.bedtime.data.SessionLog
import app.bedtime.data.TextSource
import app.bedtime.data.UnlockConfig
import app.bedtime.data.UnlockMode
import app.bedtime.engine.Occurrence
import app.bedtime.ui.components.Callout
import app.bedtime.ui.components.CalloutKind
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.ObsidianTextField
import app.bedtime.ui.components.StepChips
import app.bedtime.ui.formatCountdown
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private enum class Challenge(val label: String) { WAIT("Wait"), TEXT("Type"), PASSWORD("Password") }

/**
 * Runs every enabled challenge of the occurrence's schedule in order (wait → text → password),
 * shows a short success moment, then applies the schedule's unlock action.
 */
@Composable
fun UnlockFlow(occurrence: Occurrence, onUnlocked: () -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { Repository.get(context) }
    val schedule = occurrence.schedule
    val config = schedule.unlock
    val challenges = remember(config) {
        buildList {
            if (config.waitEnabled) add(Challenge.WAIT)
            if (config.textEnabled) add(Challenge.TEXT)
            if (config.passwordEnabled && config.hasPassword) add(Challenge.PASSWORD)
        }
    }
    var index by rememberSaveable(occurrence.start) { mutableIntStateOf(0) }
    var succeeded by remember { mutableStateOf(false) }
    val history: List<SessionLog>? by repo.history.collectAsStateWithLifecycle(initialValue = null)
    // Counted once when the flow opens, so the challenge can't change length half-way through.
    val recentUnlocks = remember(history == null) {
        history?.let { Escalation.recentUnlocks(it, schedule.id, System.currentTimeMillis()) }
    }

    fun passCurrent() {
        if (index + 1 < challenges.size) {
            index++
        } else if (!succeeded) {
            succeeded = true
            scope.launch {
                delay(1_400) // Let the success state register before the screen closes.
                UnlockManager.complete(context, occurrence)
                onUnlocked()
            }
        }
    }

    if (recentUnlocks == null) return
    val escalating = config.escalate && recentUnlocks > 0
    val textLength = if (config.escalate) Escalation.textLength(config.textLength, recentUnlocks, config.escalateFactor) else config.textLength
    val waitMs = (if (config.escalate) Escalation.waitSeconds(config.waitDurationSeconds, recentUnlocks, config.escalateFactor) else config.waitDurationSeconds) * 1000L
    val note = if (escalating && (config.waitEnabled || config.textEnabled)) {
        "This is early unlock #${recentUnlocks + 1} today, so the steps are longer."
    } else {
        null
    }

    UnlockLayout(
        scheduleName = schedule.name,
        outcome = outcomeText(schedule),
        stepLabels = challenges.map { it.label },
        current = if (succeeded) challenges.size else index,
        showCancel = !succeeded,
        onCancel = onCancel,
        note = note,
        modifier = modifier,
    ) {
        if (succeeded) {
            UnlockSuccess(successText(schedule))
        } else {
            when (challenges.getOrNull(index)) {
                null -> CtaButton("Unlock", onClick = ::passCurrent, modifier = Modifier.fillMaxWidth())
                Challenge.WAIT -> WaitChallenge(occurrence, waitMs, onPassed = ::passCurrent)
                Challenge.TEXT -> TextChallengeInput(textLength, config.textSource, onPassed = ::passCurrent)
                Challenge.PASSWORD -> PasswordChallenge(config, onPassed = ::passCurrent)
            }
        }
    }
}

internal fun outcomeText(schedule: Schedule): String = when (schedule.unlockAction.mode) {
    UnlockMode.END_SESSION -> if (schedule.isBlock) "This ends the block." else "This ends the current session."
    UnlockMode.PAUSE -> "This gives you a ${schedule.unlockAction.pauseMinutes}-minute break."
}

internal fun successText(schedule: Schedule): String = when (schedule.unlockAction.mode) {
    UnlockMode.END_SESSION ->
        if (schedule.isBlock) "${schedule.name} is over. Start it again whenever you like." else "${schedule.name} is off until its next scheduled start."
    UnlockMode.PAUSE -> "Enjoy your ${schedule.unlockAction.pauseMinutes}-minute break. ${schedule.name} switches back on afterwards."
}

@Composable
internal fun UnlockLayout(
    scheduleName: String,
    outcome: String,
    stepLabels: List<String>,
    current: Int,
    showCancel: Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    note: String? = null,
    content: @Composable () -> Unit,
) {
    val c = Obsidian.colors
    val intro = when (stepLabels.size) {
        0 -> "Unlock $scheduleName?"
        1 -> "Finish this step to unlock $scheduleName."
        else -> "Finish these steps to unlock $scheduleName."
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Leaving early?", style = MaterialTheme.typography.headlineSmall, color = c.textNormal)
            Text("$intro $outcome", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
        }
        if (note != null) Callout(title = "A little harder this time", kind = CalloutKind.WARNING, body = note)
        if (stepLabels.size > 1) StepChips(stepLabels, current)
        content()
        if (showCancel) {
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                Text("Never mind, I'll stay focused", color = c.textMuted)
            }
        }
    }
}

@Composable
internal fun UnlockSuccess(message: String) {
    val c = Obsidian.colors
    Column(
        Modifier.fillMaxWidth().padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(88.dp).clip(CircleShape).background(c.green.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Check, contentDescription = null, tint = c.green, modifier = Modifier.size(44.dp))
        }
        Text("Unlocked", style = MaterialTheme.typography.headlineSmall, color = c.textNormal)
        Text(message, style = MaterialTheme.typography.bodyLarge, color = c.textMuted, textAlign = TextAlign.Center)
    }
}

/**
 * Counts only while this screen is open and in front: leaving the app pauses it, and backing out
 * ("I'll stay focused") drops it, so the next attempt starts from scratch.
 */
@Composable
private fun WaitChallenge(occurrence: Occurrence, total: Long, onPassed: () -> Unit) {
    var elapsed by rememberSaveable(occurrence.start) { mutableLongStateOf(0L) }
    var inFront by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        inFront = true
        onPauseOrDispose { inFront = false }
    }
    LaunchedEffect(inFront) {
        if (!inFront) return@LaunchedEffect
        var last = SystemClock.elapsedRealtime()
        while (elapsed < total) {
            delay(250)
            val tick = SystemClock.elapsedRealtime()
            elapsed = (elapsed + tick - last).coerceAtMost(total)
            last = tick
        }
    }
    WaitChallengeContent(remaining = total - elapsed, total = total, onContinue = onPassed)
}

@Composable
internal fun WaitChallengeContent(remaining: Long?, total: Long, onContinue: () -> Unit) {
    val c = Obsidian.colors
    val done = remaining == 0L
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            if (done) "Time's up. Still want to unlock?" else "Take a breath. The timer only runs while this screen is open, and starts over if you back out.",
            style = MaterialTheme.typography.bodyLarge,
            color = c.textMuted,
            textAlign = TextAlign.Center,
        )
        Box(Modifier.size(208.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { if (remaining == null || total <= 0) 0f else 1f - remaining.toFloat() / total },
                modifier = Modifier.fillMaxSize(),
                color = if (done) c.green else c.accent,
                strokeWidth = 8.dp,
                trackColor = c.interactive,
                strokeCap = StrokeCap.Round,
            )
            Text(
                remaining?.let(::formatCountdown) ?: "…",
                style = MaterialTheme.typography.displaySmall.copy(fontFeatureSettings = "tnum"),
                fontWeight = FontWeight.Medium,
                color = if (done) c.green else c.textNormal,
            )
        }
        CtaButton(if (done) "Continue" else "Waiting…", onClick = onContinue, enabled = done, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun TextChallengeInput(length: Int, source: TextSource, onPassed: () -> Unit) {
    val scope = rememberCoroutineScope()
    val target = rememberSaveable { TextChallenge.generate(length, source) }
    var typed by rememberSaveable { mutableStateOf("") }
    var rejected by rememberSaveable { mutableIntStateOf(0) }
    val shake = remember { Animatable(0f) }

    TextChallengeContent(
        target = target,
        typed = typed,
        rejected = rejected,
        shakeOffset = { shake.value },
        onValueChange = { value ->
            val step = TextChallenge.advance(target, typed, value)
            // Always take the progress back: a burst that ends in a typo still got the earlier letters right.
            typed = step.typed
            when (step.outcome) {
                TextChallenge.Outcome.ACCEPTED -> if (typed.length == target.length) onPassed()
                TextChallenge.Outcome.REJECTED -> {
                    rejected++
                    scope.launch { for (x in listOf(-12f, 12f, -8f, 8f, 0f)) shake.animateTo(x, tween(40)) }
                }
                TextChallenge.Outcome.IGNORED -> Unit
            }
        },
    )
}

@Composable
internal fun TextChallengeContent(
    target: String,
    typed: String,
    rejected: Int,
    onValueChange: (String) -> Unit,
    shakeOffset: () -> Float = { 0f },
) {
    val c = Obsidian.colors
    // Words and passages read as themselves; random letters get grouped into fives.
    val natural = remember(target) { target.any { it == ' ' } }
    val display = remember(target, natural) { DisplayText.of(target, natural) }
    // Only three stretches ever differ: what's done, the character to type next, and the rest. Styling
    // each character separately meant rebuilding hundreds of spans per keystroke, which typing felt.
    val annotated = remember(display, typed.length, c) {
        val cursor = display.offsetOf(typed.length)
        val end = display.text.length
        buildAnnotatedString {
            append(display.text)
            if (cursor > 0) addStyle(SpanStyle(color = c.textFaint), 0, cursor)
            if (cursor < end) {
                addStyle(SpanStyle(color = c.textNormal, background = c.accent.copy(alpha = 0.35f)), cursor, cursor + 1)
                if (cursor + 1 < end) addStyle(SpanStyle(color = c.textNormal), cursor + 1, end)
            }
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "Copy the text below, one character at a time. Spaces and punctuation fill themselves in; " +
                "pasting and typos are rejected.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.textMuted,
        )
        Text(
            annotated,
            fontFamily = if (natural) FontFamily.Default else FontFamily.Monospace,
            fontSize = 17.sp,
            lineHeight = if (natural) 26.sp else 28.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(c.bgPrimaryAlt)
                .border(1.dp, c.border, RoundedCornerShape(12.dp))
                .padding(16.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            LinearProgressIndicator(
                progress = { typed.length / target.length.toFloat() },
                modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50)),
                color = c.accent,
                trackColor = c.interactive,
                strokeCap = StrokeCap.Round,
            )
            Spacer(Modifier.width(12.dp))
            Text("${typed.length}/${target.length}", style = MaterialTheme.typography.labelLarge, color = c.textMuted)
        }
        if (rejected > 0) {
            Text(
                "$rejected ${if (rejected == 1) "typo" else "typos"} caught. No worries, just keep going.",
                style = MaterialTheme.typography.labelMedium,
                color = c.textFaint,
            )
        }
        ObsidianTextField(
            value = typed,
            onValueChange = onValueChange,
            modifier = Modifier.offset { IntOffset(shakeOffset().roundToInt(), 0) },
            placeholder = "Start typing here…",
            singleLine = false,
            maxLines = 3,
            // Password keyboards turn off suggestions and autocorrect, which would otherwise insert whole words.
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, keyboardType = KeyboardType.Password),
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 16.sp),
        )
    }
}

/**
 * The challenge text as it is shown, and where each character of the target sits in it. Random letters
 * are grouped into fives for legibility, which shifts every index along; words and passages read as
 * themselves. Worked out once per challenge so that moving the cursor on costs nothing.
 */
private class DisplayText(val text: String, private val offsets: IntArray?) {
    /** Where the target's [index]th character sits in [text] — the end, once past the last one. */
    fun offsetOf(index: Int): Int = when {
        offsets == null -> index.coerceIn(0, text.length)
        index < offsets.size -> offsets[index]
        else -> text.length
    }

    companion object {
        fun of(target: String, natural: Boolean): DisplayText {
            if (natural) return DisplayText(target, null)
            val text = StringBuilder(target.length + target.length / 5)
            val offsets = IntArray(target.length)
            target.forEachIndexed { index, char ->
                if (index > 0 && index % 5 == 0) text.append(' ')
                offsets[index] = text.length
                text.append(char)
            }
            return DisplayText(text.toString(), offsets)
        }
    }
}

@Composable
private fun PasswordChallenge(config: UnlockConfig, onPassed: () -> Unit) {
    val scope = rememberCoroutineScope()
    var input by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var checking by remember { mutableStateOf(false) }

    PasswordChallengeContent(
        input = input,
        wrong = wrong,
        checking = checking,
        onInputChange = {
            input = it
            wrong = false
        },
        onSubmit = {
            checking = true
            scope.launch {
                val ok = withContext(Dispatchers.Default) {
                    PasswordHasher.verify(input, config.passwordHash.orEmpty(), config.passwordSalt.orEmpty())
                }
                checking = false
                if (ok) {
                    onPassed()
                } else {
                    wrong = true
                    input = ""
                }
            }
        },
    )
}

@Composable
internal fun PasswordChallengeContent(
    input: String,
    wrong: Boolean,
    checking: Boolean,
    onInputChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Enter the password for this schedule.", style = MaterialTheme.typography.bodyMedium, color = Obsidian.colors.textMuted)
        ObsidianTextField(
            value = input,
            onValueChange = onInputChange,
            label = "Password",
            password = true,
            isError = wrong,
            supportingText = if (wrong) "That's not it. Try again." else null,
        )
        CtaButton(
            if (checking) "Checking…" else "Continue",
            onClick = onSubmit,
            enabled = input.isNotEmpty() && !checking,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
