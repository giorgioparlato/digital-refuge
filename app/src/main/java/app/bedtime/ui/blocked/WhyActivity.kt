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
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Clear
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
 * down and say how you feel if you like, kept in today's microjournal entry; then put it down, or go on. Back goes on too: it's a
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
                        moods = Why.moods(settings),
                        minWords = intent.getIntExtra(EXTRA_WORDS, 0),
                        onPutDown = { words, moods ->
                            keep(question, words, moods, wentOn = false)
                            putDown(pkg)
                        },
                        onGoOn = { words, moods ->
                            keep(question, words, moods, wentOn = true)
                            finish()
                        },
                    )
                }
            }
        }
    }

    /** The answer, in today's microjournal entry, when there's anything to it; written even as the screen goes. */
    private fun keep(question: String, words: String, moods: List<String>, wentOn: Boolean) {
        if (words.isBlank() && moods.isEmpty()) return
        val repo = Repository.get(applicationContext)
        val line = Why.line(LocalDateTime.now(), app.bedtime.data.Moods.UNLOCKING, question, words, wentOn, moods)
        lifecycleScope.launch { withContext(NonCancellable) { repo.addWhy(line, moods, app.bedtime.data.Moods.UNLOCKING, wentOn) } }
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
        private const val EXTRA_WORDS = "words"

        /** The question after an unlock, or before [app] opens; with [words], at least that many before going on. */
        fun intent(context: Context, app: String? = null, words: Int = 0): Intent =
            Intent(context, WhyActivity::class.java)
                .apply { app?.let { putExtra(EXTRA_APP, it) } }
                .putExtra(EXTRA_WORDS, words)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}

/**
 * The question and a box to write in, and how you feel ([moods]), if you like: then put it down, or go
 * on. [about] sits above the question ("about to open Instagram"), when there's an app to name. With
 * [minWords], going on waits for that many words, typed here: pasting doesn't count.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WhyScreen(
    question: String,
    onPutDown: (String, List<String>) -> Unit,
    onGoOn: (String, List<String>) -> Unit,
    about: String? = null,
    moods: List<String> = emptyList(),
    minWords: Int = 0,
) {
    val c = Obsidian.colors
    var words by rememberSaveable { mutableStateOf("") }
    var picked by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val written = Why.words(words)
    // With the keyboard up, the question moves up and gets smaller, so the box and its count stay in view.
    val typing = WindowInsets.isImeVisible
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
        Spacer(Modifier.height(if (typing) 0.dp else 40.dp))
        about?.let {
            Text(it, style = MaterialTheme.typography.labelLarge, color = c.textMuted)
            Spacer(Modifier.height(10.dp))
        }
        Text(question, fontSize = if (typing) 19.sp else 26.sp, color = c.textNormal, textAlign = TextAlign.Center)
        if (moods.isNotEmpty()) {
            Spacer(Modifier.height(if (typing) 10.dp else 18.dp))
            // Folded each time: a line saying how you feel, the chips a tap away.
            var moodsOpen by remember { mutableStateOf(false) }
            Text(
                (if (picked.isEmpty()) "how do you feel?" else "feeling " + picked.joinToString(", ")) + if (moodsOpen) " ‹" else " ›",
                style = MaterialTheme.typography.labelLarge,
                color = if (picked.isEmpty()) c.textMuted else c.accentText,
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable { moodsOpen = !moodsOpen }.padding(horizontal = 12.dp, vertical = 6.dp),
            )
            if (moodsOpen) {
                Spacer(Modifier.height(8.dp))
                // A mood of your own joins the others for next time, and is picked now.
                val context = androidx.compose.ui.platform.LocalContext.current
                val scope = androidx.compose.runtime.rememberCoroutineScope()
                MoodChips(
                    (moods + picked).distinct(),
                    picked,
                    onAdd = { mood ->
                        if (mood !in picked) picked = picked + mood
                        scope.launch { Repository.get(context).addMoodTag(mood) }
                    },
                ) { mood -> picked = if (mood in picked) picked - mood else picked + mood }
            }
        }
        Spacer(Modifier.height(if (typing) 12.dp else 20.dp))
        // Room to write it out: as much as you like, or nothing; with a minimum, typed rather than pasted.
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.ui.platform.LocalTextToolbar provides if (minWords > 0) app.bedtime.unlock.NoPasting else androidx.compose.ui.platform.LocalTextToolbar.current,
        ) {
            BasicTextField(
                value = words,
                onValueChange = { if (minWords <= 0 || Why.typed(words, it)) words = it },
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.textNormal),
                cursorBrush = SolidColor(c.accent),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (typing) 120.dp else 180.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.bgSecondary)
                    .border(1.dp, c.border, RoundedCornerShape(14.dp))
                    .padding(16.dp),
                decorationBox = { field ->
                    Column {
                        // The count at the top of the box, where it stays in view above the keyboard.
                        if (minWords > 0) WordCount(written, minWords, Modifier.align(Alignment.End).padding(bottom = 6.dp))
                        androidx.compose.foundation.layout.Box {
                            if (words.isEmpty()) Text(if (minWords > 0) "At least $minWords words…" else "Write it down, if you want…", style = MaterialTheme.typography.bodyLarge, color = c.textFaint)
                            field()
                        }
                    }
                },
            )
        }
        Spacer(Modifier.height(32.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Not now",
                style = MaterialTheme.typography.labelLarge,
                color = c.textMuted,
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable { onPutDown(words, picked) }.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            Spacer(Modifier.weight(1f))
            SoftPillButton("Yes, go on", onClick = { onGoOn(words, picked) }, enabled = written >= minWords)
        }
    }
}

/**
 * Moods to tap, one or several: the ones picked filled. With [onAdd], a "+" to type a mood of your
 * own, which stays among them from then on.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MoodChips(moods: List<String>, picked: List<String>, onAdd: ((String) -> Unit)? = null, onTap: (String) -> Unit) {
    val c = Obsidian.colors
    var adding by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            moods.forEach { mood -> MoodChip(mood, mood in picked) { onTap(mood) } }
            if (onAdd != null && !adding) MoodChip("+", false) { adding = true }
        }
        if (onAdd != null && adding) {
            var name by remember { mutableStateOf("") }
            val done = {
                name.trim().lowercase().takeIf { it.isNotEmpty() }?.let(onAdd)
                adding = false
            }
            Spacer(Modifier.height(10.dp))
            app.bedtime.ui.components.ObsidianTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = "a mood of your own",
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { done() }),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                Text("Cancel", style = MaterialTheme.typography.labelLarge, color = c.textMuted, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { adding = false }.padding(10.dp))
                Text("Add", style = MaterialTheme.typography.labelLarge, color = c.accentText, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = done).padding(10.dp))
            }
        }
    }
}

@Composable
private fun MoodChip(label: String, on: Boolean, onClick: () -> Unit) {
    val c = Obsidian.colors
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (on) c.bgPrimary else c.textMuted,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (on) c.accent else androidx.compose.ui.graphics.Color.Transparent)
            .border(1.dp, if (on) c.accent else c.border, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    )
}

/** "12 of 30 words", or "31 words · enough" once there are; shared by every box that asks for a few. */
@Composable
internal fun WordCount(written: Int, needed: Int, modifier: Modifier = Modifier) {
    val c = Obsidian.colors
    Text(
        if (written >= needed) "$written words · enough" else "$written of $needed words",
        style = MaterialTheme.typography.labelMedium,
        color = if (written >= needed) c.accentText else c.textFaint,
        modifier = modifier,
    )
}
