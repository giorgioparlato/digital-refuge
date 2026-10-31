package app.bedtime.ui.blocked

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import app.bedtime.data.AppSettings
import app.bedtime.data.Repository
import app.bedtime.data.Why
import app.bedtime.service.BlockingState
import app.bedtime.ui.components.SoftPillButton
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.BedtimeTheme
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/**
 * "Why am I unlocking my phone right now?", or another question in turn, after an unlock. Write it
 * down if you like, kept in its own note; then put it down, or go on. Back goes on too: it's a
 * pause, not a wall.
 */
class WhyActivity : ComponentActivity() {
    private var settings = AppSettings()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val pkg = intent.getStringExtra(EXTRA_APP)
        onBackPressedDispatcher.addCallback(this) { finish() }
        lifecycleScope.launch {
            settings = Repository.get(this@WhyActivity).settings.first()
            setContent {
                BedtimeTheme {
                    val question = remember { Why.ask(Why.questions(settings)) }
                    WhyScreen(
                        question = question,
                        onPutDown = { words ->
                            keep(question, words, wentOn = false)
                            putDown(pkg)
                        },
                        onGoOn = { words ->
                            keep(question, words, wentOn = true)
                            finish()
                        },
                    )
                }
            }
        }
    }

    /** The answer, in its note, when there's anything to it; written even as the screen goes. */
    private fun keep(question: String, words: String, wentOn: Boolean) {
        if (words.isBlank()) return
        val repo = Repository.get(applicationContext)
        val line = Why.line(LocalDateTime.now(), "unlocking", question, words, wentOn)
        lifecycleScope.launch { withContext(NonCancellable) { repo.addWhy(line) } }
    }

    /** Not now: after an unlock the phone locks again; before an app, home instead. */
    private fun putDown(pkg: String?) {
        if (pkg == null) {
            BlockingState.globalAction(this, AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
        } else {
            runCatching { startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
        finish()
    }

    companion object {
        private const val EXTRA_APP = "app"

        /** The question after an unlock, or before [app] opens. */
        fun intent(context: Context, app: String? = null): Intent =
            Intent(context, WhyActivity::class.java)
                .apply { app?.let { putExtra(EXTRA_APP, it) } }
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}

/**
 * The question and a box to write in: then put it down, or go on. [about] sits above the question
 * ("about to open Instagram"), when there's an app to name.
 */
@Composable
internal fun WhyScreen(
    question: String,
    onPutDown: (String) -> Unit,
    onGoOn: (String) -> Unit,
    about: String? = null,
) {
    val c = Obsidian.colors
    var words by rememberSaveable { mutableStateOf("") }
    Column(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(c.fade, c.bgPrimary)))
            .safeDrawingPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        about?.let {
            Text(it, style = MaterialTheme.typography.labelLarge, color = c.textMuted)
            Spacer(Modifier.height(10.dp))
        }
        Text(question, fontSize = 26.sp, color = c.textNormal, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        // Room to write it out: as much as you like, or nothing.
        BasicTextField(
            value = words,
            onValueChange = { words = it },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.textNormal),
            cursorBrush = SolidColor(c.accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(c.bgSecondary)
                .border(1.dp, c.border, RoundedCornerShape(14.dp))
                .padding(16.dp),
            decorationBox = { field ->
                androidx.compose.foundation.layout.Box {
                    if (words.isEmpty()) Text("Write it down, if you want…", style = MaterialTheme.typography.bodyLarge, color = c.textFaint)
                    field()
                }
            },
        )
        Spacer(Modifier.height(32.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Not now",
                style = MaterialTheme.typography.labelLarge,
                color = c.textMuted,
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onPutDown(words) }.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            Spacer(Modifier.weight(1f))
            SoftPillButton("Yes, go on", onClick = { onGoOn(words) })
        }
    }
}
