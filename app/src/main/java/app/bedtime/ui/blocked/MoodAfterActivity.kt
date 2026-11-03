package app.bedtime.ui.blocked

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import app.bedtime.data.Repository
import app.bedtime.data.Why
import app.bedtime.ui.components.SoftPillButton
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.BedtimeTheme
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * "How do you feel now?", once a stretch on the phone is over: a session's unlock time, or a while in
 * an app with a limit. Moods to tap, kept as a line in today's microjournal; or skip. Off until asked for.
 */
class MoodAfterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val what = intent.getStringExtra(EXTRA_WHAT).orEmpty()
        val repo = Repository.get(this)
        lifecycleScope.launch {
            val moods = Why.moods(repo.settings.first())
            if (moods.isEmpty()) return@launch finish()
            setContent {
                BedtimeTheme {
                    val c = Obsidian.colors
                    var picked by remember { mutableStateOf(emptyList<String>()) }
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable(onClickLabel = "skip") { finish() }.safeDrawingPadding(),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(c.bgPrimary)
                                .clickable(enabled = false) {}
                                .padding(22.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("How do you feel now?", style = MaterialTheme.typography.titleMedium, color = c.textNormal)
                            if (what.isNotEmpty()) Text(what, style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                            MoodChips(moods, picked) { mood -> picked = if (mood in picked) picked - mood else picked + mood }
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Skip",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = c.textMuted,
                                    modifier = Modifier.clip(RoundedCornerShape(50)).clickable { finish() }.padding(horizontal = 16.dp, vertical = 12.dp),
                                )
                                Spacer(Modifier.weight(1f))
                                SoftPillButton("Keep it", enabled = picked.isNotEmpty(), onClick = {
                                    lifecycleScope.launch {
                                        withContext(NonCancellable) { repo.addJournalLine(what, picked, after = true) }
                                        finish()
                                    }
                                })
                            }
                        }
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_WHAT = "what"

        /** [what] it's after: "after 12 min on instagram", "the unlock is over". */
        fun intent(context: Context, what: String): Intent =
            Intent(context, MoodAfterActivity::class.java).putExtra(EXTRA_WHAT, what).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
