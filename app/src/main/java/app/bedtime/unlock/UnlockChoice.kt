package app.bedtime.unlock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.NumberStepper
import app.bedtime.ui.components.QuickChip
import app.bedtime.ui.components.Text
import app.bedtime.ui.formatMinutes
import app.bedtime.ui.theme.Obsidian

/** Lengths offered at a glance; any other can be set with the stepper. */
private val PRESETS = listOf(5, 10, 15, 30, 60, 120)

/** Stands for "the rest of it" while choosing, since a length is always at least a minute. */
internal const val REST = 0

/**
 * "How long do you need?", asked before the unlock steps when the length is chosen there and then.
 * Deciding first keeps it honest: the steps earn what was asked for, not more. [maxMinutes] is what's
 * left of the session or the day; anything longer is simply "the rest of it" ([restLabel]).
 * [onChoose] gets the minutes, or null for the rest.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun UnlockLengthChoice(
    name: String,
    maxMinutes: Int,
    restLabel: String,
    onChoose: (Int?) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Obsidian.colors
    val longest = maxMinutes.coerceAtLeast(1)
    var picked by rememberSaveable { mutableIntStateOf(15.coerceAtMost(longest)) }
    UnlockLayout(
        scheduleName = name,
        stepLabels = emptyList(),
        current = 0,
        showCancel = true,
        onCancel = onCancel,
        modifier = modifier,
        footnote = "The steps come next, and they're the same whichever length you choose.",
        action = {
            CtaButton(
                if (picked == REST) "Continue · $restLabel" else "Continue · ${formatMinutes(picked.toLong())}",
                onClick = { onChoose(picked.takeIf { it != REST }) },
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("How long do you need?", fontSize = 28.sp, fontWeight = FontWeight.Light, color = c.textNormal)
            Spacer(Modifier.height(8.dp))
            Text(
                "Decide first, then earn it.",
                style = MaterialTheme.typography.bodyLarge,
                color = c.textMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(26.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                PRESETS.filter { it < longest }.forEach { minutes ->
                    QuickChip(formatMinutes(minutes.toLong()), picked == minutes) { picked = minutes }
                }
                QuickChip(restLabel, picked == REST) { picked = REST }
            }
            if (picked != REST) {
                Spacer(Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Or exactly", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                    Spacer(Modifier.width(14.dp))
                    NumberStepper(
                        picked,
                        { picked = it },
                        1..longest,
                        suffix = " min",
                        title = "how long",
                    )
                }
            }
        }
    }
}
