package app.bedtime.ui.edit

import androidx.compose.runtime.getValue

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.material.icons.filled.Create

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
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
    if (unlock.writeEnabled) add("write ${unlock.writeWords} words")
    if (unlock.passwordEnabled && (unlock.hasPassword || hasNewPassword)) add("a password")
}

/** A step an unlock can take, in the order they run. */
private enum class StepKind(val title: String, val description: String) {
    WAIT("Wait it out", "Start a timer, come back when it's done"),
    TYPE("Type random text", "No pasting, typos don't count"),
    WRITE("Write a few lines", "Why you want to unlock, or from prompts you choose"),
    PASSWORD("A password", "Tip: let someone else choose it"),
    ;

    fun isOn(u: UnlockConfig): Boolean = when (this) {
        WAIT -> u.waitEnabled
        TYPE -> u.textEnabled
        WRITE -> u.writeEnabled
        PASSWORD -> u.passwordEnabled
    }

    fun set(u: UnlockConfig, on: Boolean): UnlockConfig = when (this) {
        WAIT -> u.copy(waitEnabled = on)
        TYPE -> u.copy(textEnabled = on)
        WRITE -> u.copy(writeEnabled = on)
        PASSWORD -> u.copy(passwordEnabled = on)
    }
}

/**
 * The steps an unlock takes: only the ones added, each with its own settings and a × to take it away,
 * then "+ add a step" for more friction. Shared by schedules, blocks and limits, so the same words mean
 * the same thing everywhere. "Harder each time" shows once there's a step it can make longer.
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
    val added = StepKind.entries.filter { it.isOn(unlock) }
    Column {
        if (added.isEmpty()) {
            Text("No steps yet: unlocking takes a single tap.", style = MaterialTheme.typography.bodySmall, color = c.textFaint, modifier = Modifier.padding(vertical = 6.dp))
        }
        added.forEach { kind ->
            StepHeader(kind, editable) { onChange(kind.set(unlock, false)) }
            when (kind) {
                StepKind.WAIT -> SubOptionRow("Timer") {
                    NumberStepper(
                        unlock.waitDurationSeconds, { v -> setUnlock { it.copy(waitSeconds = v) } }, 5..3600, step = 5, enabled = editable,
                        presets = listOf(10, 30, 60, 120, 300, 600), title = "timer", format = { formatWaitSeconds(it) },
                    )
                }
                StepKind.TYPE -> TypingSettings(
                    unlock.textLength, { v -> setUnlock { it.copy(textLength = v) } },
                    unlock.textSource, { v -> setUnlock { it.copy(textSource = v) } },
                    editable,
                )
                StepKind.WRITE -> WritingSettings(unlock, { change -> setUnlock(change) }, editable)
                StepKind.PASSWORD -> if (editable) {
                    ObsidianTextField(
                        value = newPassword,
                        onValueChange = onNewPasswordChange,
                        label = if (unlock.hasPassword) "New password (leave empty to keep)" else "Choose a password",
                        password = true,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                    )
                }
            }
        }
        if (unlock.waitEnabled || unlock.textEnabled || unlock.writeEnabled) {
            HarderStep(
                unlock.escalate,
                { on -> setUnlock { it.copy(escalate = on) } },
                (unlock.escalateFactor * 10).roundToInt(),
                { v -> setUnlock { it.copy(escalateFactor = v / 10f) } },
                editable,
                description = "Each time in a day makes the wait, the typing and the writing longer",
            )
        }
        val left = StepKind.entries - added.toSet()
        if (editable && left.isNotEmpty()) {
            Box {
                var choosing by remember { mutableStateOf(false) }
                Text(
                    if (added.isEmpty()) "+ add a step" else "+ add another step",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.accentText,
                    modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(8.dp)).clickable { choosing = true }.padding(vertical = 8.dp, horizontal = 4.dp),
                )
                DropdownMenu(expanded = choosing, onDismissRequest = { choosing = false }, containerColor = c.bgSecondary) {
                    left.forEach { kind ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(kind.title, color = c.textNormal)
                                    Text(kind.description, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                                }
                            },
                            onClick = {
                                choosing = false
                                onChange(kind.set(unlock, true))
                            },
                        )
                    }
                }
            }
        }
    }
}

/** An added step's name and what it asks, with a × to take it away again. */
@Composable
private fun StepHeader(kind: StepKind, editable: Boolean, onRemove: () -> Unit) {
    val icon = when (kind) {
        StepKind.WAIT -> BedtimeIcons.Hourglass
        StepKind.TYPE -> Icons.Default.Edit
        StepKind.WRITE -> Icons.Default.Create
        StepKind.PASSWORD -> Icons.Default.Lock
    }
    OptionRow(icon, kind.title, description = kind.description, enabled = editable) {
        if (editable) {
            IconButton(onClick = onRemove) { Icon(Icons.Default.Close, contentDescription = "remove ${kind.title}", tint = Obsidian.colors.textFaint) }
        }
    }
}

/**
 * "Write a few lines", once added: at least so many words, from a prompt of the microjournal's sections you pick,
 * or "why do I want to unlock this?" with none picked. What's written goes with your answers to "why?".
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun WritingSettings(unlock: UnlockConfig, onChange: ((UnlockConfig) -> UnlockConfig) -> Unit, editable: Boolean) {
    val c = Obsidian.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    val repo = androidx.compose.runtime.remember { app.bedtime.data.Repository.get(context) }
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val sections = app.bedtime.data.Journal.sections(settings?.journalSections)
    SubOptionRow("Words, at least") {
        NumberStepper(unlock.writeWords, { v -> onChange { it.copy(writeWords = v) } }, 5..500, step = 5, enabled = editable, presets = listOf(10, 20, 30, 50, 100, 200), title = "words")
    }
    Text("Prompts from", style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp),
    ) {
        app.bedtime.ui.components.QuickChip("Just “why?”", unlock.writeSections.isEmpty()) { if (editable) onChange { it.copy(writeSections = emptyList()) } }
        sections.forEach { section ->
            val on = section.name in unlock.writeSections
            app.bedtime.ui.components.QuickChip(section.name, on) {
                if (editable) onChange { it.copy(writeSections = if (on) it.writeSections - section.name else it.writeSections + section.name) }
            }
        }
    }
    Text("Kept with your answers to “why?”. The sections are the microjournal's, as written in settings › notes.", style = MaterialTheme.typography.bodySmall, color = c.textFaint, modifier = Modifier.padding(top = 8.dp))
}

/** "Type random text", once added: how much, and from what. */
@Composable
internal fun TypingSettings(length: Int, onLength: (Int) -> Unit, source: TextSource, onSource: (TextSource) -> Unit, editable: Boolean) {
    val c = Obsidian.colors
    SubOptionRow("Characters") {
        NumberStepper(length, onLength, 25..2000, step = 25, enabled = editable, presets = listOf(50, 100, 150, 200, 300, 500, 1000), title = "characters")
    }
    Text("What to copy", style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.padding(top = 12.dp, bottom = 8.dp))
    SegmentedChoice(options = listOf("Letters", "Words", "Passages"), selected = source.ordinal, onSelect = { onSource(TextSource.entries[it]) }, enabled = editable)
    Text(
        when (source) {
            TextSource.LETTERS -> "Random letters and digits. Nothing to read, only to copy."
            TextSource.WORDS -> "Plain words, easier on the eyes at midnight."
            TextSource.PASSAGES -> "Quotes and longer passages, worth reading while you type them."
        },
        style = MaterialTheme.typography.bodySmall,
        color = c.textFaint,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
    )
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
