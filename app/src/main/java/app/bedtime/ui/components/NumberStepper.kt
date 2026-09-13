package app.bedtime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * A number with −/+ buttons. Tap a button for one step, hold it to keep going (faster and faster),
 * or tap the number itself to type an exact value or pick a preset.
 */
@Composable
fun NumberStepper(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    step: Int = 1,
    suffix: String = "",
    enabled: Boolean = true,
    presets: List<Int> = emptyList(),
    title: String = "",
    /** Past the end, start over (for hours and minutes). */
    wrap: Boolean = false,
    format: (Int) -> String = { it.toString() },
) {
    val c = Obsidian.colors
    var typing by remember { mutableStateOf(false) }

    fun shifted(delta: Int): Int {
        val next = value + delta
        return when {
            !wrap -> next.coerceIn(range)
            next > range.last -> range.first
            next < range.first -> range.last
            else -> next
        }
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton("−", "decrease", enabled && (wrap || value > range.first)) { onValueChange(shifted(-step)) }
        Box(
            Modifier
                .padding(horizontal = 6.dp)
                .widthIn(min = 72.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(c.bgPrimaryAlt)
                .border(1.dp, c.border, RoundedCornerShape(10.dp))
                .clickable(enabled = enabled, onClickLabel = "type a value") { typing = true }
                .padding(horizontal = 10.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "${format(value)}$suffix",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = if (enabled) c.textNormal else c.textFaint,
                textAlign = TextAlign.Center,
            )
        }
        StepButton("+", "increase", enabled && (wrap || value < range.last)) { onValueChange(shifted(step)) }
    }

    if (typing) {
        NumberDialog(
            title = title,
            value = value,
            range = range,
            suffix = suffix,
            presets = presets,
            format = format,
            onDismiss = { typing = false },
            onPick = {
                onValueChange(it.coerceIn(range))
                typing = false
            },
        )
    }
}

@Composable
private fun StepButton(label: String, description: String, enabled: Boolean, onStep: () -> Unit) {
    val c = Obsidian.colors
    val currentOnStep by rememberUpdatedState(onStep)
    val currentEnabled by rememberUpdatedState(enabled)
    var pressed by remember { mutableStateOf(false) }
    Box(
        Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (pressed && enabled) c.interactiveHover else c.interactive)
            .semantics {
                role = Role.Button
                contentDescription = description
                if (!enabled) disabled()
                onClick(label = description) {
                    if (currentEnabled) currentOnStep()
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(onPress = {
                    if (currentEnabled) {
                        pressed = true
                        currentOnStep()
                        coroutineScope {
                            // Holding repeats the step, faster and faster.
                            val repeat = launch {
                                delay(400)
                                var interval = 150L
                                while (isActive && currentEnabled) {
                                    currentOnStep()
                                    delay(interval)
                                    interval = (interval * 0.85).toLong().coerceAtLeast(40L)
                                }
                            }
                            tryAwaitRelease()
                            repeat.cancel()
                        }
                        pressed = false
                    }
                })
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (enabled) c.textNormal else c.textFaint, fontSize = 20.sp)
    }
}

/** Type an exact value, or tap a preset. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NumberDialog(
    title: String,
    value: Int,
    range: IntRange,
    suffix: String,
    presets: List<Int>,
    format: (Int) -> String,
    onDismiss: () -> Unit,
    onPick: (Int) -> Unit,
) {
    val c = Obsidian.colors
    var text by remember { mutableStateOf(value.toString()) }
    val parsed = text.toIntOrNull()
    val valid = parsed != null && parsed in range
    val usablePresets = presets.filter { it in range }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.bgSecondary,
        title = { Text(title.ifBlank { "choose a value" }) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ObsidianTextField(
                    value = text,
                    onValueChange = { typed -> text = typed.filter(Char::isDigit).take(4) },
                    label = "${format(range.first)}–${format(range.last)}$suffix",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = text.isNotEmpty() && !valid,
                    supportingText = if (text.isNotEmpty() && !valid) "pick something between ${range.first} and ${range.last}" else null,
                )
                if (usablePresets.isNotEmpty()) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        usablePresets.forEach { preset ->
                            QuickChip("${format(preset)}$suffix", selected = parsed == preset) { onPick(preset) }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let(onPick) }, enabled = valid) {
                Text("done", color = c.accentText, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("cancel", color = c.textMuted) } },
    )
}
