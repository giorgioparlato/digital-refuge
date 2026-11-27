package app.bedtime.ui.blocked

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import app.bedtime.apps.AppCatalog
import app.bedtime.data.Repository
import app.bedtime.data.WorthAnswer
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.BedtimeTheme
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * "Was it worth it?", once you've left a distracting app after a while in it: worth it, or not really,
 * kept with the app and the stretch's length; or skip, with a tap beside it. Off until asked for.
 */
class WorthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pkg = intent.getStringExtra(EXTRA_APP)
        val seconds = intent.getLongExtra(EXTRA_SECONDS, 0L)
        val label = pkg?.let { AppCatalog.label(this, it).lowercase() } ?: "the phone"
        val repo = Repository.get(this)
        val answer = { worth: Boolean ->
            lifecycleScope.launch {
                withContext(NonCancellable) {
                    repo.addWorth(WorthAnswer(LocalDate.now().toEpochDay(), pkg, seconds, worth, System.currentTimeMillis()))
                }
                finish()
            }
            Unit
        }
        setContent {
            BedtimeTheme {
                val c = Obsidian.colors
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
                    ) {
                        Text("${seconds / 60} min on $label", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
                        Text("Was it worth it?", fontSize = 22.sp, fontWeight = FontWeight.Light, color = c.textNormal)
                        WorthChoice(picked = null, onPick = answer)
                    }
                }
            }
        }
    }

    companion object {
        private const val EXTRA_APP = "app"
        private const val EXTRA_SECONDS = "seconds"

        /** About [pkg], left after [seconds] in it. */
        fun intent(context: Context, pkg: String, seconds: Long): Intent =
            Intent(context, WorthActivity::class.java)
                .putExtra(EXTRA_APP, pkg)
                .putExtra(EXTRA_SECONDS, seconds)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

/** "Worth it" and "not really", side by side and alike, so neither is the one you're meant to tap; [picked] outlined. */
@Composable
internal fun WorthChoice(picked: Boolean?, onPick: (Boolean) -> Unit) {
    val c = Obsidian.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(true to "Worth it", false to "Not really").forEach { (worth, label) ->
            val on = picked == worth
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (on) c.interactive else c.bgSecondary)
                    .border(1.dp, if (on) c.textNormal else Color.Transparent, RoundedCornerShape(50))
                    .clickable(role = Role.RadioButton) { onPick(worth) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, fontSize = 16.sp, color = c.textNormal)
            }
        }
    }
}
