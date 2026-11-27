package app.bedtime.ui.blocked

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.Repository
import app.bedtime.data.Why
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** What the full-screen wall says: a session's, while blocking is off during it, or the one outside any session. */
sealed interface Takeover {
    /** [afterBreak]: an emergency break has just run out; with [askMood], how do you feel now as well. */
    data class Session(
        val name: String,
        val until: String?,
        val progress: Float,
        val breakMinutes: Int,
        val afterBreak: Boolean,
        val askMood: Boolean,
        val onEnable: () -> Unit,
        val onSnooze: (() -> Unit)?,
    ) : Takeover

    /** Blocking switched off for [minutes] outside a session, [since] then; "later" asks again at [later]. */
    data class Off(
        val minutes: Int,
        val since: String,
        val later: String,
        val onEnable: () -> Unit,
        val onLater: () -> Unit,
    ) : Takeover
}

/** The wall, in the covers' frame. After a break, the moods to tap are on it too, kept as you leave it. */
@Composable
internal fun TakeoverScreen(takeover: Takeover) {
    val c = Obsidian.colors
    when (takeover) {
        is Takeover.Off -> CoverLayout(
            where = CoverWhere("Blocking is off", "since ${takeover.since}"),
            emblem = { CoverRing(BedtimeIcons.Refuge, 0f, c.textMuted) },
            title = "Blocking has been off for ${takeover.minutes} ${if (takeover.minutes == 1) "minute" else "minutes"}",
            body = "Your limits and blocks only work while it's on.",
            primary = CoverAction("Switch blocking back on", takeover.onEnable),
            secondary = listOf(CoverAction("Later · asks again at ${takeover.later}", takeover.onLater)),
        )
        is Takeover.Session -> {
            val context = LocalContext.current
            val repo = remember { Repository.get(context) }
            val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
            val moods = settings?.takeIf { takeover.afterBreak && takeover.askMood }?.let { Why.moods(it) }.orEmpty()
            var picked by remember { mutableStateOf(emptyList<String>()) }
            // The moods tapped, in today's microjournal, whichever way you leave.
            val keep = {
                if (picked.isNotEmpty()) {
                    val moodsNow = picked
                    CoroutineScope(Dispatchers.IO).launch { repo.addJournalLine("after an emergency break", moodsNow, after = true) }
                }
            }
            CoverLayout(
                where = CoverWhere("${takeover.name} is on", takeover.until?.let { "until $it" }, takeover.progress, session = true),
                emblem = { CoverRing(BedtimeIcons.Moon, takeover.progress, c.accentText) },
                title = if (takeover.afterBreak) "Your break is over" else "Blocking is off",
                body = "${takeover.name} is still running. Switch blocking back on to carry on.",
                extra = if (moods.isEmpty()) null else {
                    {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.Start) {
                            Text("How do you feel now?", style = MaterialTheme.typography.bodyMedium, color = c.textNormal)
                            MoodChips(moods, picked, toneOf = { mood -> settings?.let { Why.toneOf(it, mood) } ?: "cream" }, centered = false) { mood ->
                                picked = if (mood in picked) picked - mood else picked + mood
                            }
                        }
                    }
                },
                primary = CoverAction("Switch blocking back on", { keep(); takeover.onEnable() }),
                secondary = listOfNotNull(
                    takeover.onSnooze?.let { snooze ->
                        CoverAction("Take ${takeover.breakMinutes} more ${if (takeover.breakMinutes == 1) "minute" else "minutes"}", { keep(); snooze() })
                    },
                ),
            )
        }
    }
}
