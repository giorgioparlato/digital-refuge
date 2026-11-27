package app.bedtime.unlock

import app.bedtime.ui.components.LineIcons
import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.semantics
import app.bedtime.ui.components.ProgressRing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import app.bedtime.ui.components.SoftPillButton
import app.bedtime.ui.components.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.Passages
import app.bedtime.data.Repository
import app.bedtime.data.Schedule
import app.bedtime.data.SessionLog
import app.bedtime.data.TextSource
import app.bedtime.data.UnlockConfig
import app.bedtime.data.UnlockMode
import app.bedtime.engine.Occurrence
import app.bedtime.ui.components.Breathing
import app.bedtime.ui.components.ObsidianTextField
import app.bedtime.ui.formatCountdown
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private enum class Challenge(val label: String) { WAIT("Wait"), TEXT("Type"), WRITE("Write"), PASSWORD("Password") }

/**
 * Runs every enabled challenge of the occurrence's schedule in order (wait → text → password),
 * shows a short success moment, then applies the schedule's unlock action.
 */
@Composable
fun UnlockFlow(occurrence: Occurrence, onUnlocked: () -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val schedule = occurrence.schedule
    val history: List<SessionLog>? by repo.history.collectAsStateWithLifecycle(initialValue = null)
    // Counted once when the flow opens, so the challenge can't change length half-way through.
    val recentUnlocks = remember(history == null) {
        history?.let { Escalation.recentUnlocks(it, schedule.id, System.currentTimeMillis()) }
    }
    // With "choose then", the length comes first: NOT_CHOSEN until it's picked, REST for the rest of it.
    val choosing = schedule.unlockAction.mode == UnlockMode.CHOOSE
    var chosen by rememberSaveable(occurrence.start) { mutableIntStateOf(NOT_CHOSEN) }
    if (choosing && chosen == NOT_CHOSEN) {
        UnlockLengthChoice(
            name = schedule.name,
            maxMinutes = ((occurrence.end - System.currentTimeMillis()) / 60_000).toInt(),
            restLabel = if (schedule.isBlock) "the rest of the block" else "the rest of the session",
            onChoose = { chosen = it ?: REST },
            onCancel = onCancel,
            modifier = modifier,
        )
        return
    }
    val minutes = if (choosing) chosen.takeIf { it != REST } else UnlockManager.setLength(schedule)
    UnlockSteps(
        key = occurrence.start,
        name = schedule.name,
        config = schedule.unlock,
        recentUnlocks = recentUnlocks,
        outcome = outcomeText(schedule, minutes),
        success = successText(schedule, minutes),
        onComplete = { UnlockManager.complete(context, occurrence, minutes) },
        onUnlocked = onUnlocked,
        // Backing out of the steps goes back to choosing, not all the way out.
        onCancel = if (choosing) ({ chosen = NOT_CHOSEN }) else onCancel,
        modifier = modifier,
    )
}

/**
 * The steps themselves, for anything that can be unlocked: a session, or a limit's extra time.
 * [key] ties the step you're on and the wait's progress to one thing being unlocked; [recentUnlocks]
 * (null while loading) makes the steps longer when "harder each time" is on; [onComplete] does the
 * unlocking once the last step is passed.
 */
@Composable
internal fun UnlockSteps(
    key: Any,
    name: String,
    config: UnlockConfig,
    recentUnlocks: Int?,
    outcome: String,
    success: String,
    onComplete: suspend () -> Unit,
    onUnlocked: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val challenges = remember(config) {
        buildList {
            if (config.waitEnabled) add(Challenge.WAIT)
            if (config.textEnabled) add(Challenge.TEXT)
            if (config.writeEnabled) add(Challenge.WRITE)
            if (config.passwordEnabled && config.hasPassword) add(Challenge.PASSWORD)
        }
    }
    var index by rememberSaveable(key) { mutableIntStateOf(0) }
    var succeeded by remember { mutableStateOf(false) }

    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    fun passCurrent() {
        // Two light ticks: a step done.
        haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
        if (index + 1 < challenges.size) {
            index++
        } else if (!succeeded) {
            succeeded = true
            scope.launch {
                // Let the success state register before the screen closes. The unlock is applied even if
                // the screen goes first (Back, or the screen switching off): the steps were passed.
                try {
                    delay(1_400)
                } finally {
                    withContext(NonCancellable) { onComplete() }
                }
                onUnlocked()
            }
        }
    }

    if (recentUnlocks == null) return
    // What's written to unlock goes with the other answers to "why?", in their note; its prompts are the sections chosen.
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val sections = app.bedtime.data.Journal.sections(settings?.journalSections).filter { it.name in config.writeSections }
    // A section with no questions of its own (clarity) is written from its guide.
    val prompts = sections.flatMap { s -> s.prompts.map { it to s.hints[it] }.ifEmpty { listOf(s.guide to null) } }
        .ifEmpty { listOf("why do i want to unlock ${name.lowercase()} right now?" to null) }
    val writeWords = if (config.escalate) Escalation.textLength(config.writeWords, recentUnlocks, config.escalateFactor) else config.writeWords
    val escalating = config.escalate && recentUnlocks > 0
    val textLength = if (config.escalate) Escalation.textLength(config.textLength, recentUnlocks, config.escalateFactor) else config.textLength
    val waitMs = (if (config.escalate) Escalation.waitSeconds(config.waitDurationSeconds, recentUnlocks, config.escalateFactor) else config.waitDurationSeconds) * 1000L
    val note = if (escalating && (config.waitEnabled || config.textEnabled || config.writeEnabled)) {
        "${ordinal(recentUnlocks + 1)} unlock today \u00b7 the steps are longer"
    } else {
        null
    }

    val step = if (succeeded) null else challenges.getOrNull(index)
    // The timer is hoisted out of the page so the ring and the button, which sit at opposite ends of
    // it, can both read the same countdown.
    val remaining = if (step == Challenge.WAIT) waitCountdown(key, waitMs) else null

    UnlockLayout(
        scheduleName = name,
        footnote = if (succeeded) null else footnoteFor(step, outcome),
        stepLabels = challenges.map { it.label },
        current = if (succeeded) challenges.size else index,
        showCancel = !succeeded,
        onCancel = onCancel,
        note = if (succeeded) null else note,
        bloom = step == Challenge.WAIT,
        modifier = modifier,
        action = when {
            succeeded -> null
            challenges.isEmpty() -> {
                { SoftPillButton("Unlock", onClick = ::passCurrent, modifier = Modifier.fillMaxWidth()) }
            }
            step == Challenge.WAIT -> {
                {
                    val done = remaining == 0L
                    SoftPillButton(if (done) "Continue" else "Waiting\u2026", onClick = ::passCurrent, enabled = done, modifier = Modifier.fillMaxWidth())
                }
            }
            else -> null
        },
    ) {
        if (succeeded) {
            UnlockSuccess(success)
        } else {
            when (step) {
                null -> Unit
                Challenge.WAIT -> WaitChallengeContent(remaining = remaining, total = waitMs)
                Challenge.TEXT -> TextChallengeInput(textLength, config.textSource, onPassed = ::passCurrent)
                Challenge.WRITE -> WriteChallenge(writeWords, prompts) { prompt, text ->
                    val line = app.bedtime.data.Why.line(java.time.LocalDateTime.now(), "unlocking ${name.lowercase()}", prompt, text, wentOn = true)
                    scope.launch { withContext(NonCancellable) { repo.addWhy(line) } }
                    passCurrent()
                }
                Challenge.PASSWORD -> PasswordChallenge(config, onPassed = ::passCurrent)
            }
        }
    }
}

/** "1st", "2nd", "3rd", "4th" \u2014 enough for a day's worth of unlocks. */
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

/** The small print at the foot: what unlocking does, plus whatever this step needs you to know. */
private fun footnoteFor(step: Challenge?, outcome: String): String = when (step) {
    Challenge.WAIT -> "$outcome The timer keeps going with the screen off or the phone locked, pauses while you're in another app, and starts over if you back out."
    Challenge.TEXT -> "$outcome Spaces and punctuation fill themselves in; pasting and typos are rejected."
    else -> outcome
}

/** Before a length has been chosen. */
private const val NOT_CHOSEN = -1

/** What the unlock will do: a break of [minutes], or with null the end of the session. */
internal fun outcomeText(schedule: Schedule, minutes: Int?): String = when (minutes) {
    null -> if (schedule.isBlock) "This ends the block." else "This ends the current session."
    else -> "This gives you a $minutes-minute break."
}

internal fun successText(schedule: Schedule, minutes: Int?): String = when (minutes) {
    null -> if (schedule.isBlock) "${schedule.name} is over. Start it again whenever you like." else "${schedule.name} is off until its next scheduled start."
    else -> "Enjoy your $minutes-minute break. ${schedule.name} switches back on afterwards."
}

@Composable
internal fun UnlockLayout(
    scheduleName: String,
    stepLabels: List<String>,
    current: Int,
    showCancel: Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    footnote: String? = null,
    note: String? = null,
    /** The waiting page's light comes from behind its ring rather than from the top of the screen. */
    bloom: Boolean = false,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val c = Obsidian.colors
    // The faint sage light of a session's covers, seen this time: the old fade was too close to the page to show.
    val deep = c.accent.copy(alpha = 0.11f).compositeOver(c.bgPrimary)
    Column(
        modifier
            .fillMaxSize()
            .background(
                if (bloom) {
                    Brush.radialGradient(listOf(deep, c.bgPrimary))
                } else {
                    Brush.verticalGradient(0f to deep, 0.55f to c.bgPrimary)
                },
            )
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Where you are, always at the top: what you're unlocking, and the steps by name.
        Text(
            "Unlocking $scheduleName",
            style = MaterialTheme.typography.bodyMedium,
            color = c.textMuted,
            modifier = Modifier.fillMaxWidth(),
        )
        if (stepLabels.size > 1) {
            Spacer(Modifier.height(14.dp))
            StepTrack(stepLabels, current)
        }
        if (note != null) {
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth()) { app.bedtime.ui.blocked.CoverCount(note) }
        }
        // The middle scrolls on its own, so the button and the small print stay at the foot of the
        // page instead of being pushed off it by a long passage or the keyboard.
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            content()
        }
        if (action != null) {
            Spacer(Modifier.height(16.dp))
            action()
        }
        if (showCancel) {
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(50)).clickable(onClick = onCancel),
                contentAlignment = Alignment.Center,
            ) {
                Text("Never mind, let's keep still", style = MaterialTheme.typography.bodyLarge, color = c.textMuted)
            }
        }
        if (footnote != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                footnote,
                style = MaterialTheme.typography.bodySmall,
                color = c.textMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/**
 * The steps as a track across the top: one segment each with its name under it, the ones done in a
 * soft green, this one lit, the ones to come grey.
 */
@Composable
private fun StepTrack(labels: List<String>, current: Int) {
    val c = Obsidian.colors
    Row(Modifier.fillMaxWidth().semantics(mergeDescendants = true) {}, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        labels.forEachIndexed { index, label ->
            val done = index < current
            val here = index == current
            Column(Modifier.weight(1f)) {
                Box(
                    Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(50))
                        .background(
                            when {
                                here -> c.accentText
                                done -> c.accent.copy(alpha = 0.5f)
                                else -> c.interactive
                            },
                        ),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (here) c.textNormal else c.textMuted,
                    maxLines = 1,
                )
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
            Icon(LineIcons.Check, contentDescription = null, tint = c.green, modifier = Modifier.size(44.dp))
        }
        Text("Unlocked", fontSize = 28.sp, fontWeight = FontWeight.Light, color = c.textNormal)
        Text(message, style = MaterialTheme.typography.bodyLarge, color = c.textMuted, textAlign = TextAlign.Center)
    }
}

/**
 * The countdown, in milliseconds left. Counts only while this screen is open and in front: leaving
 * the app pauses it, and backing out drops it, so the next attempt starts from scratch.
 */
@Composable
private fun waitCountdown(key: Any, total: Long): Long {
    val context = LocalContext.current
    var elapsed by rememberSaveable(key) { mutableLongStateOf(0L) }
    var inFront by remember { mutableStateOf(false) }
    // Left with the screen going off: when, and when you were back in the phone (unlocked) since.
    var awayAt by remember { mutableStateOf<Long?>(null) }
    var backAt by remember { mutableStateOf<Long?>(null) }
    DisposableEffect(Unit) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(c: android.content.Context, intent: android.content.Intent) {
                if (awayAt != null && backAt == null) backAt = SystemClock.elapsedRealtime()
            }
        }
        androidx.core.content.ContextCompat.registerReceiver(
            context,
            receiver,
            android.content.IntentFilter(android.content.Intent.ACTION_USER_PRESENT),
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    LifecycleResumeEffect(Unit) {
        val left = awayAt
        if (left != null) elapsed = (elapsed + WaitAway.counted(left, backAt, SystemClock.elapsedRealtime())).coerceAtMost(total)
        awayAt = null
        backAt = null
        inFront = true
        onPauseOrDispose {
            inFront = false
            // The screen going off (or the phone locking) keeps the wait going; leaving for another app doesn't.
            val interactive = context.getSystemService(android.os.PowerManager::class.java)?.isInteractive != false
            if (!interactive) awayAt = SystemClock.elapsedRealtime()
        }
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
    return total - elapsed
}

/**
 * Three words and a ring. Everything else about waiting lives in the small print at the foot.
 *
 * The ring sits inside a hairline it can fill, and the page's light blooms from behind it rather
 * than falling from the top of the screen, so what you are waiting on is also what you are looking at.
 */
@Composable
internal fun WaitChallengeContent(remaining: Long?, total: Long) {
    val c = Obsidian.colors
    val done = remaining == 0L
    val ring = if (done) c.green else c.accent
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            if (done) "Time's up." else "Take a breath.",
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            color = c.textNormal,
        )
        Spacer(Modifier.height(30.dp))
        // The ring is the time left; the disc inside it breathes. The words for the breath go in the
        // disc, the minutes under the ring, so the two never compete inside one circle.
        Box(Modifier.size(256.dp), contentAlignment = Alignment.Center) {
            ProgressRing(
                progress = if (remaining == null || total <= 0) 0f else 1f - remaining.toFloat() / total,
                modifier = Modifier.fillMaxSize(),
                stroke = 2.dp,
                color = if (done) c.green else c.accentText,
                track = c.interactive,
            )
            // Something to do with the wait besides watching it: breathe with the disc. The time left
            // sits small under the breath, so the breath is what you watch, not the clock.
            Breathing(running = remaining != null && !done, color = ring, modifier = Modifier.size(214.dp)) { inhaling ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (done) "" else if (inhaling) "breathe in" else "breathe out",
                        fontSize = 18.sp,
                        color = c.textNormal,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        remaining?.let { if (done) "" else formatCountdown(it) + " left" } ?: "\u2026",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.textMuted,
                    )
                }
            }
        }
    }
}

@Composable
private fun TextChallengeInput(length: Int, source: TextSource, onPassed: () -> Unit) = TypingChallenge(length, source, onPassed)

/** Copy a short text to go on: the unlock steps' typing, also asked by a limit's pause before opening. */
@Composable
internal fun TypingChallenge(length: Int, source: TextSource, onPassed: () -> Unit) {
    val scope = rememberCoroutineScope()
    // Passages follow your quote choices, so wait for them before choosing what to copy.
    val settings by Repository.get(LocalContext.current).settings.collectAsStateWithLifecycle(initialValue = null)
    val loaded = settings ?: return
    val target = rememberSaveable { TextChallenge.generate(length, source, passages = Passages.forPrefs(loaded.quotes)) }
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
            if (cursor > 0) addStyle(SpanStyle(color = c.textNormal), 0, cursor)
            if (cursor < end) {
                addStyle(SpanStyle(color = c.textNormal, background = c.accent.copy(alpha = 0.35f)), cursor, cursor + 1)
                if (cursor + 1 < end) addStyle(SpanStyle(color = c.textFaint), cursor + 1, end)
            }
        }
    }

    // One box: you type over the passage itself, and what's done turns from faint to cream. The field
    // that takes the keys lies on top of it, invisible, so a tap anywhere on the passage brings the keyboard.
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset(shakeOffset().roundToInt(), 0) }
                .clip(RoundedCornerShape(18.dp))
                .background(c.bgSecondary)
                .padding(18.dp),
        ) {
            Text(
                annotated,
                fontFamily = if (natural) FontFamily.Default else FontFamily.Monospace,
                fontSize = 18.sp,
                lineHeight = if (natural) 28.sp else 30.sp,
                modifier = Modifier.fillMaxWidth(),
            )
            // The field holds the cursor as well as the text, and the cursor is always put back at the end:
            // accepting a letter can fill a space in with it, and a cursor left in front of that space
            // would drop the next letter into the middle of the word.
            var field by remember { mutableStateOf(TextFieldValue(typed, TextRange(typed.length))) }
            if (field.text != typed) field = TextFieldValue(typed, TextRange(typed.length))
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalTextToolbar provides NoPasting,
                androidx.compose.foundation.text.selection.LocalTextSelectionColors provides
                    androidx.compose.foundation.text.selection.TextSelectionColors(Color.Transparent, Color.Transparent),
            ) {
                BasicTextField(
                    value = field,
                    onValueChange = { value ->
                        field = value.copy(selection = TextRange(value.text.length))
                        onValueChange(value.text)
                    },
                    textStyle = TextStyle(color = Color.Transparent, fontSize = 18.sp),
                    cursorBrush = SolidColor(Color.Transparent),
                    // Password keyboards turn off suggestions and autocorrect, which would otherwise insert whole words.
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, keyboardType = KeyboardType.Password),
                    modifier = Modifier.matchParentSize().focusRequester(focus).semantics { contentDescription = "type the passage" },
                )
            }
        }
        Text(
            "${typed.length} of ${target.length}" + if (rejected > 0) " · $rejected ${if (rejected == 1) "typo" else "typos"} caught. No worries, just keep going." else "",
            style = MaterialTheme.typography.labelMedium,
            color = c.textMuted,
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
        SoftPillButton(
            if (checking) "Checking…" else "Continue",
            onClick = onSubmit,
            enabled = input.isNotEmpty() && !checking,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * How much of a time away from the wait counts. Away with the screen off (and the phone locked, as it
 * then is), it counts until you were back in the phone ([backAt], unlocked), or until the wait was back
 * in front if that came within a few seconds; whatever you did in between in other apps doesn't.
 */
internal object WaitAway {
    private const val BACK_WITHIN_MS = 10_000L

    fun counted(awayAt: Long, backAt: Long?, resumedAt: Long): Long {
        val until = if (backAt == null || resumedAt - backAt <= BACK_WITHIN_MS) resumedAt else backAt
        return (until - awayAt).coerceAtLeast(0L)
    }
}
