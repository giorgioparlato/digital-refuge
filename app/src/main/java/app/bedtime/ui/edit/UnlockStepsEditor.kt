package app.bedtime.ui.edit

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.bedtime.data.TextSource
import app.bedtime.data.UnlockConfig
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.Callout
import app.bedtime.ui.components.CalloutKind
import app.bedtime.ui.components.NumberStepper
import app.bedtime.ui.components.ObsidianTextField
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.ui.components.OptionRow
import app.bedtime.ui.components.SegmentedChoice
import app.bedtime.ui.components.SubOptionRow
import app.bedtime.ui.components.Text
import app.bedtime.ui.formatWaitSeconds
import app.bedtime.ui.theme.Obsidian
import kotlin.math.roundToInt

/** The steps an unlock takes, in order, as short phrases ("wait 30 s", "a password"). */
internal fun unlockSteps(unlock: UnlockConfig, hasNewPassword: Boolean): List<String> = buildList {
    if (unlock.waitEnabled) add("wait ${formatWaitSeconds(unlock.waitDurationSeconds)}")
    if (unlock.textEnabled) {
        add(
            when (unlock.textSource) {
                TextSource.PASSAGES -> "copy out a passage"
                TextSource.WORDS -> "type ${unlock.textLength} characters"
                TextSource.LETTERS -> "type ${unlock.textLength} characters"
            },
        )
    }
    if (unlock.passwordEnabled && (unlock.hasPassword || hasNewPassword)) add("a password")
}

/**
 * Wait, type, password and "harder each time": the steps an unlock takes. Shared by schedules,
 * blocks and limits, so the same words mean the same thing everywhere.
 */
@Composable
internal fun UnlockStepsEditor(
    unlock: UnlockConfig,
    onChange: (UnlockConfig) -> Unit,
    editable: Boolean,
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
) {
    val c = Obsidian.colors
    fun setUnlock(transform: (UnlockConfig) -> UnlockConfig) = onChange(transform(unlock))
    Column {
        WaitStep(unlock.waitEnabled, { on -> setUnlock { it.copy(waitEnabled = on) } }, unlock.waitDurationSeconds, { v -> setUnlock { it.copy(waitSeconds = v) } }, editable)
        TypingStep(
            unlock.textEnabled,
            { on -> setUnlock { it.copy(textEnabled = on) } },
            unlock.textLength,
            { v -> setUnlock { it.copy(textLength = v) } },
            unlock.textSource,
            { v -> setUnlock { it.copy(textSource = v) } },
            editable,
        )
        OptionRow(Icons.Default.Lock, "Password", description = "Tip: let someone else choose it", enabled = editable) {
            ObsidianToggle(unlock.passwordEnabled, { on -> setUnlock { it.copy(passwordEnabled = on) } }, enabled = editable)
        }
        AnimatedVisibility(unlock.passwordEnabled && editable) {
            ObsidianTextField(
                value = newPassword,
                onValueChange = onNewPasswordChange,
                label = if (unlock.hasPassword) "New password (leave empty to keep)" else "Choose a password",
                password = true,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
        }
        AnimatedVisibility(unlock.waitEnabled || unlock.textEnabled) {
            Column {
                HarderStep(
                    unlock.escalate,
                    { on -> setUnlock { it.copy(escalate = on) } },
                    (unlock.escalateFactor * 10).roundToInt(),
                    { v -> setUnlock { it.copy(escalateFactor = v / 10f) } },
                    editable,
                    description = "Each early unlock in a day makes the wait and text longer",
                )
            }
        }
        val anyStep = unlock.waitEnabled || unlock.textEnabled ||
            (unlock.passwordEnabled && (unlock.hasPassword || newPassword.isNotBlank()))
        AnimatedVisibility(!anyStep) {
            Callout(
                title = "No steps picked",
                kind = CalloutKind.WARNING,
                body = "Unlocking will take a single tap.",
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** "Wait it out": a timer to sit through, and how long. The same row wherever a wait is asked for. */
@Composable
internal fun WaitStep(
    on: Boolean,
    onToggle: (Boolean) -> Unit,
    seconds: Int,
    onSeconds: (Int) -> Unit,
    editable: Boolean,
    description: String = "Start a timer, come back when it's done",
    range: IntRange = 5..3600,
    step: Int = 5,
    presets: List<Int> = listOf(10, 30, 60, 120, 300, 600),
) {
    OptionRow(BedtimeIcons.Hourglass, "Wait it out", description = description, enabled = editable) {
        ObsidianToggle(on, onToggle, enabled = editable)
    }
    AnimatedVisibility(on) {
        SubOptionRow("Timer") {
            NumberStepper(seconds, onSeconds, range, step = step, enabled = editable, presets = presets, title = "timer", format = { formatWaitSeconds(it) })
        }
    }
}

/** "Type random text": how much, and from what. The same row wherever typing is asked for. */
@Composable
internal fun TypingStep(
    on: Boolean,
    onToggle: (Boolean) -> Unit,
    length: Int,
    onLength: (Int) -> Unit,
    source: TextSource,
    onSource: (TextSource) -> Unit,
    editable: Boolean,
    range: IntRange = 25..2000,
    step: Int = 25,
    presets: List<Int> = listOf(50, 100, 150, 200, 300, 500, 1000),
) {
    val c = Obsidian.colors
    OptionRow(Icons.Default.Edit, "Type random text", description = "No pasting, typos don't count", enabled = editable) {
        ObsidianToggle(on, onToggle, enabled = editable)
    }
    AnimatedVisibility(on) {
        Column {
            SubOptionRow("Characters") {
                NumberStepper(length, onLength, range, step = step, enabled = editable, presets = presets, title = "characters")
            }
            Text(
                "What to copy",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
            )
            SegmentedChoice(
                options = listOf("Letters", "Words", "Passages"),
                selected = source.ordinal,
                onSelect = { onSource(TextSource.entries[it]) },
                enabled = editable,
            )
            Text(
                when (source) {
                    TextSource.LETTERS -> "Random letters and digits. Nothing to read, only to copy."
                    TextSource.WORDS -> "Plain words, easier on the eyes at midnight."
                    TextSource.PASSAGES -> "Quotes and longer passages, worth reading while you type them."
                },
                style = MaterialTheme.typography.bodySmall,
                color = c.textFaint,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** "Harder each time", and by how much, in tenths: 15 is 1.5× the time before. */
@Composable
internal fun HarderStep(on: Boolean, onToggle: (Boolean) -> Unit, tenths: Int, onTenths: (Int) -> Unit, editable: Boolean, description: String) {
    OptionRow(Icons.Default.Refresh, "Harder each time", description = description, enabled = editable) {
        ObsidianToggle(on, onToggle, enabled = editable)
    }
    AnimatedVisibility(on) {
        SubOptionRow("How much") {
            NumberStepper(
                tenths,
                onTenths,
                11..40,
                enabled = editable,
                presets = listOf(15, 20, 25, 30, 40),
                title = "each time",
                format = { "%.1f×".format(it / 10f) },
            )
        }
    }
}
