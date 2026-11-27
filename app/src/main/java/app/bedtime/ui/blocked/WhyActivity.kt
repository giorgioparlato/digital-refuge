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
import androidx.compose.foundation.layout.width
import androidx.compose.ui.text.font.FontWeight
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
                        toneOf = { Why.toneOf(settings, it) },
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
 * on. [about] sits at the top ("about to open Instagram"), when there's an app to name. With
 * [minWords], going on waits for that many words, typed here: pasting doesn't count. Laid out like the
 * covers: where you are at the top, the words, then "not now" as the main way on and going on quieter.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WhyScreen(
    question: String,
    onPutDown: (String, List<String>) -> Unit,
    onGoOn: (String, List<String>) -> Unit,
    about: String? = null,
    moods: List<String> = emptyList(),
    /** Each mood's colour, one of [Why.TONES]. */
    toneOf: (String) -> String = { "cream" },
    minWords: Int = 0,
    /** A line for the microjournal rather than an answer before going on: keep it, or never mind. */
    keeping: Boolean = false,
) {
    val c = Obsidian.colors
    val context = androidx.compose.ui.platform.LocalContext.current
    var words by rememberSaveable { mutableStateOf("") }
    var picked by rememberSaveable { mutableStateOf(emptyList<String>()) }
    // Moods of your own made here, with their colours, until settings carry them.
    var added by remember { mutableStateOf(emptyMap<String, String>()) }
    val written = Why.words(words)
    // With the keyboard up, the question moves up and gets smaller, so the box and its count stay in view.
    val typing = WindowInsets.isImeVisible
    val now = remember { app.bedtime.ui.formatTime(context, System.currentTimeMillis()) }
    Column(
        Modifier
            .fillMaxSize()
            .background(c.bgPrimary)
            .safeDrawingPadding()
            .imePadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(about ?: "Journal", style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.weight(1f))
            Text(now, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
        Column(
            Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            // Writing, everything sits at the top, so more of what's written shows above the keyboard.
            verticalArrangement = if (typing) Arrangement.Top else Arrangement.Center,
        ) {
            Spacer(Modifier.height(if (typing) 8.dp else 24.dp))
            Text(question, fontSize = if (typing) 19.sp else 28.sp, fontWeight = FontWeight.Light, lineHeight = if (typing) 25.sp else 35.sp, color = c.textNormal)
            Spacer(Modifier.height(if (typing) 12.dp else 20.dp))
            // Room to write it out between two hairlines: as much as you like, or nothing; with a minimum, typed rather than pasted.
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalTextToolbar provides if (minWords > 0) app.bedtime.unlock.NoPasting else androidx.compose.ui.platform.LocalTextToolbar.current,
            ) {
                BasicTextField(
                    value = words,
                    onValueChange = { if (minWords <= 0 || Why.typed(words, it)) words = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = c.textNormal, fontSize = 17.sp, lineHeight = 26.sp),
                    cursorBrush = SolidColor(c.accent),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { field ->
                        Column {
                            androidx.compose.material3.HorizontalDivider(color = c.border)
                            Column(Modifier.fillMaxWidth().heightIn(min = if (typing) 96.dp else 132.dp).padding(vertical = 14.dp)) {
                                androidx.compose.foundation.layout.Box {
                                    if (words.isEmpty()) Text(if (minWords > 0) "At least $minWords words…" else "Write it down, if you want…", style = MaterialTheme.typography.bodyLarge, color = c.textFaint)
                                    field()
                                }
                            }
                            if (minWords > 0) WordCount(written, minWords, Modifier.align(Alignment.End).padding(bottom = 8.dp))
                            androidx.compose.material3.HorizontalDivider(color = c.border)
                        }
                    },
                )
            }
            if (moods.isNotEmpty()) {
                Spacer(Modifier.height(14.dp))
                // Open to start with; folded into a line saying how you feel once the keyboard comes up, a tap to open again.
                var moodsOpen by remember { mutableStateOf(true) }
                androidx.compose.runtime.LaunchedEffect(typing) { if (typing) moodsOpen = false }
                Text(
                    (if (picked.isEmpty()) "how do you feel?" else "feeling " + picked.joinToString(", ")) + if (moodsOpen) " ‹" else " ›",
                    style = MaterialTheme.typography.labelLarge,
                    color = if (picked.isEmpty()) c.textMuted else c.textNormal,
                    modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClickLabel = if (moodsOpen) "fold the moods" else "show the moods") { moodsOpen = !moodsOpen }.padding(vertical = 6.dp),
                )
                if (moodsOpen) {
                    Spacer(Modifier.height(8.dp))
                    val scope = androidx.compose.runtime.rememberCoroutineScope()
                    // A mood of your own joins the others for next time, in the colour picked for it, and is picked now.
                    MoodChips(
                        (moods + picked).distinct(),
                        picked,
                        toneOf = { added[it] ?: toneOf(it) },
                        centered = false,
                        onAdd = { mood, tone ->
                            added = added + (mood to tone)
                            if (mood !in picked) picked = picked + mood
                            scope.launch { Repository.get(context).addMoodTag(mood, tone) }
                        },
                    ) { mood -> picked = if (mood in picked) picked - mood else picked + mood }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        // With the keyboard up, the two ways on share a line, leaving the room to the words.
        if (typing) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (keeping) {
                    QuietButton("Never mind", Modifier.weight(1f)) { onPutDown(words, picked) }
                    SoftPillButton("Keep it", onClick = { onGoOn(words, picked) }, enabled = words.isNotBlank() || picked.isNotEmpty(), modifier = Modifier.weight(1f))
                } else {
                    SoftPillButton("Not now", onClick = { onPutDown(words, picked) }, modifier = Modifier.weight(1f))
                    QuietButton("Yes, go on", Modifier.weight(1f), enabled = written >= minWords) { onGoOn(words, picked) }
                }
            }
        } else if (keeping) {
            SoftPillButton("Keep it", onClick = { onGoOn(words, picked) }, enabled = words.isNotBlank() || picked.isNotEmpty(), modifier = Modifier.fillMaxWidth())
            QuietButton("Never mind") { onPutDown(words, picked) }
        } else {
            SoftPillButton("Not now", onClick = { onPutDown(words, picked) }, modifier = Modifier.fillMaxWidth())
            QuietButton("Yes, go on", enabled = written >= minWords) { onGoOn(words, picked) }
        }
    }
}

/**
 * Moods to tap, one or several, each with a dot in its colour ([toneOf]): the ones picked outlined in
 * it. With [onAdd], a "+" to type a mood of your own and pick its colour; it stays among them from then on.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MoodChips(
    moods: List<String>,
    picked: List<String>,
    toneOf: (String) -> String = { "cream" },
    onAdd: ((String, String) -> Unit)? = null,
    centered: Boolean = true,
    onTap: (String) -> Unit,
) {
    val c = Obsidian.colors
    var adding by remember { mutableStateOf(false) }
    Column(horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp, if (centered) Alignment.CenterHorizontally else Alignment.Start),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            moods.forEach { mood -> MoodChip(mood, app.bedtime.ui.components.toneColour(toneOf(mood)), mood in picked) { onTap(mood) } }
            if (onAdd != null && !adding) MoodChip("+", null, false) { adding = true }
        }
        if (onAdd != null && adding) {
            var name by remember { mutableStateOf("") }
            var tone by remember { mutableStateOf("cream") }
            val done = {
                name.trim().lowercase().takeIf { it.isNotEmpty() }?.let { onAdd(it, tone) }
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
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                // Its colour, from the app's four, picked as it's made.
                app.bedtime.ui.components.ToneSwatches(tone, onPick = { tone = it }, modifier = Modifier.weight(1f))
                Text("Cancel", style = MaterialTheme.typography.labelLarge, color = c.textMuted, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { adding = false }.padding(10.dp))
                Text("Add", style = MaterialTheme.typography.labelLarge, color = c.accentText, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = done).padding(10.dp))
            }
        }
    }
}

@Composable
private fun MoodChip(label: String, colour: androidx.compose.ui.graphics.Color?, on: Boolean, onClick: () -> Unit) {
    val c = Obsidian.colors
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (on) c.interactive else androidx.compose.ui.graphics.Color.Transparent)
            .border(1.dp, if (on && colour != null) colour else c.border, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .heightIn(min = 40.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (colour != null) {
            androidx.compose.foundation.layout.Box(Modifier.size(9.dp).clip(androidx.compose.foundation.shape.CircleShape).background(colour))
            Spacer(Modifier.width(8.dp))
        }
        Text(label, style = MaterialTheme.typography.labelLarge, color = if (on) c.textNormal else c.textMuted)
    }
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
