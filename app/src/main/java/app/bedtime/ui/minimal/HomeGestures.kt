package app.bedtime.ui.minimal

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.runtime.produceState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.compose.foundation.verticalScroll

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.alpha
import app.bedtime.data.FolderIndex
import app.bedtime.data.Notes
import app.bedtime.ui.components.AppIcon
import app.bedtime.ui.components.BedtimeIcons
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.bedtime.data.HomeGesture
import app.bedtime.data.HomeStyle
import app.bedtime.data.NoteTarget
import app.bedtime.ui.components.Text
import app.bedtime.ui.homestyle.palette
import kotlin.math.abs

/** What a drag on the home screen meant: all apps (up), one of the gestures, or nothing at all. */
sealed interface HomeDrag {
    data object None : HomeDrag
    data object AllApps : HomeDrag
    data class Gesture(val gesture: HomeGesture) : HomeDrag
}

/** Reads a drag that travelled ([dx], [dy]): the longer way wins, and anything shorter than [minDistance] is nothing. */
fun readHomeDrag(dx: Float, dy: Float, minDistance: Float): HomeDrag = when {
    abs(dx) < minDistance && abs(dy) < minDistance -> HomeDrag.None
    abs(dy) >= abs(dx) -> if (dy < 0) HomeDrag.AllApps else HomeDrag.Gesture(HomeGesture.SWIPE_DOWN)
    dx < 0 -> HomeDrag.Gesture(HomeGesture.SWIPE_LEFT)
    else -> HomeDrag.Gesture(HomeGesture.SWIPE_RIGHT)
}

/**
 * A note written straight from the home screen or the lock screen: the words, then where they go, as
 * from the search: the app's own notes, or one of your files. With [lookUp], a single word also
 * brings your dictionaries' entries above where it goes; a second word takes them away and it's a
 * note again. What the words say to do ([offers]: a timer, an event, a block, a sum) sits first.
 */
@Composable
internal fun QuickNoteDialog(
    style: HomeStyle,
    targets: List<NoteTarget>,
    onSave: (String, NoteTarget?) -> Unit,
    onDismiss: () -> Unit,
    vault: String? = null,
    lookUp: LookUpWith? = null,
    offers: (String) -> List<DrawerResult> = { emptyList() },
    onOffer: (DrawerResult) -> Unit = {},
    initial: String = "",
    whole: Boolean = false,
    journal: JournalWith? = null,
) {
    var words by remember { mutableStateOf(initial) }
    // A swipe puts it away only while there's no more than a word in it, so a note isn't lost to one.
    TopPanel(onDismiss, swipeAway = { lookUpWord(words) != null || words.isBlank() }) {
        QuickNoteContent(
            style, targets,
            onSave = { text, target -> onSave(text, target); onDismiss() },
            vault = vault,
            lookUp = lookUp,
            offers = offers,
            onOffer = { onOffer(it); onDismiss() },
            onClose = onDismiss,
            whole = whole,
            initial = initial,
            onWords = { words = it },
            journal = journal?.let { j -> JournalWith(j.sections) { text, aims -> j.onSave(text, aims); onDismiss() } },
        )
    }
}

@Composable
internal fun QuickNoteContent(
    style: HomeStyle,
    targets: List<NoteTarget>,
    onSave: (String, NoteTarget?) -> Unit,
    initial: String = "",
    focus: Boolean = true,
    vault: String? = null,
    lookUp: LookUpWith? = null,
    offers: (String) -> List<DrawerResult> = { emptyList() },
    onOffer: (DrawerResult) -> Unit = {},
    whole: Boolean = false,
    /** Hears the words as they change: the dialog, to know whether a swipe may close it. */
    onWords: ((String) -> Unit)? = null,
    /** An × at the top, to put it away. */
    onClose: (() -> Unit)? = null,
    /** The microjournal, when it's on: a chip turns the box into today's entry, with its prompts. */
    journal: JournalWith? = null,
) {
    val context = LocalContext.current
    val p = style.palette()
    // The words with the cursor, so a prompt goes in where you're writing.
    var field by remember { mutableStateOf(TextFieldValue(initial, TextRange(initial.length))) }
    val text = field.text
    /** The words looked up before this one by tapping, to go back through. */
    var back by remember { mutableStateOf(emptyList<String>()) }
    val setField: (TextFieldValue) -> Unit = {
        field = it
        onWords?.invoke(it.text)
    }
    val setText: (String) -> Unit = { setField(TextFieldValue(it, TextRange(it.length))) }
    // Writing in the microjournal instead: today's entry, and the sections picked for it.
    var journaling by remember { mutableStateOf(false) }
    var aims by remember { mutableStateOf(emptyList<String>()) }
    var reading by remember { mutableStateOf<FoundNote?>(null) }
    val focusRequester = remember { FocusRequester() }
    if (focus) LaunchedEffect(Unit) { runCatching { focusRequester.requestFocus() } }
    val hasDictionaries by produceState(false, lookUp) {
        if (lookUp != null) value = withContext(Dispatchers.IO) { app.bedtime.data.StarDicts.names(context).isNotEmpty() }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(p.background)
            .border(1.dp, p.faint.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
            // A tap on the panel stays on it; only one beside it puts it away.
            .pointerInput(Unit) { detectTapGestures { } }
            // However long the words, where they go stays within reach.
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        reading?.let { note ->
            NoteReader(note, style) { reading = null }
            return@Column
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (journaling) "Microjournal · today" else "A quick note", style = MaterialTheme.typography.titleMedium, color = p.text, modifier = Modifier.weight(1f))
            if (journal != null) {
                Text(
                    if (journaling) "‹ note" else "microjournal ›",
                    style = MaterialTheme.typography.labelLarge,
                    color = p.accent,
                    modifier = Modifier.clip(RoundedCornerShape(50)).clickable { journaling = !journaling }.padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
            if (onClose != null) {
                Box(Modifier.size(36.dp).clip(RoundedCornerShape(50)).clickable(onClickLabel = "close", onClick = onClose), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = p.faint, modifier = Modifier.size(20.dp))
                }
            }
        }
        if (journaling && journal != null) {
            app.bedtime.ui.notes.JournalPrompts(
                journal.sections, aims, onPick = { aims = it },
                onPrompt = { prompt ->
                    val (written, cursor) = app.bedtime.data.Journal.inserted(field.text, field.selection.start, prompt)
                    setField(TextFieldValue(written, TextRange(cursor)))
                },
                text = p.text, faint = p.faint, accent = p.accent, background = p.background,
            )
        }
        BasicTextField(
            value = field,
            onValueChange = {
                setField(it)
                back = emptyList()
            },
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = p.text),
            cursorBrush = SolidColor(p.accent),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            modifier = Modifier
                .fillMaxWidth()
                // A long paste scrolls inside the box rather than pushing the choices off the screen.
                .heightIn(min = 96.dp, max = 240.dp)
                .focusRequester(focusRequester)
                .border(1.dp, p.faint.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            decorationBox = { field ->
                Box {
                    if (text.isEmpty()) {
                        Text(
                            when {
                                journaling -> "A few lines for today"
                                hasDictionaries -> "What's on your mind, or a word to look up"
                                else -> "What's on your mind"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = p.faint,
                        )
                    }
                    field()
                }
            },
        )
        back.lastOrNull()?.let { previous ->
            Text(
                "‹ back to $previous",
                style = MaterialTheme.typography.labelLarge,
                color = p.accent,
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable {
                    setText(previous)
                    back = back.dropLast(1)
                }.padding(vertical = 2.dp),
            )
        }
        // In the microjournal, it only ever goes to today's entry: no look-ups, no offers, no other places.
        if (journaling && journal != null) {
            val ready = text.isNotBlank()
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable(enabled = ready) { journal.onSave(text.trim(), aims) }.padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = p.accent.copy(alpha = if (ready) 1f else 0.5f), modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(14.dp))
                Text("Add to today's microjournal", style = MaterialTheme.typography.bodyLarge, color = if (ready) p.text else p.faint)
            }
            return@Column
        }
        // What the words say to do, first: a timer, an event, a block, a sum.
        remember(text) { offers(text) }.forEach { offer -> OfferRow(offer, style) { onOffer(offer) } }
        val word = lookUpWord(text)
        if (lookUp != null && word != null) {
            WordResults(
                style, word, lookUp, vault, whole,
                onWord = { tapped ->
                    // Another word from an entry: looked up in its place, with a way back.
                    if (!tapped.equals(word, ignoreCase = true)) {
                        back = back + word
                        setText(tapped)
                    }
                },
                onRead = { reading = it },
            )
            Box(Modifier.fillMaxWidth().padding(vertical = 2.dp).height(1.dp).background(p.faint.copy(alpha = 0.25f)))
        }
        val ready = text.isNotBlank()
        (listOf<NoteTarget?>(null) + targets).forEach { target ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(enabled = ready) { onSave(text.trim(), target) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Where it goes, at a glance: refuge's mark, Obsidian's own icon for your vault, or a file.
                Box(Modifier.size(22.dp).alpha(if (ready) 1f else 0.5f), contentAlignment = Alignment.Center) {
                    when {
                        target == null || target.uri.startsWith("refuge:") ->
                            Icon(BedtimeIcons.Refuge, contentDescription = "refuge notes", tint = p.accent, modifier = Modifier.size(22.dp))
                        vault != null && Notes.inFolder(target.uri, vault) -> AppIcon(FolderIndex.OBSIDIAN, "Obsidian", 22.dp)
                        else -> Icon(if (target.daily) Icons.Default.DateRange else Icons.Default.Edit, contentDescription = null, tint = p.accent, modifier = Modifier.size(20.dp))
                    }
                }
                Spacer(Modifier.width(14.dp))
                Text(
                    target?.let { if (it.daily) "Add to today's note in ${it.name}" else "Add to ${it.name}" } ?: "Save in refuge notes",
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (ready) p.text else p.faint,
                )
            }
        }
    }
}

/** Something the words say to do, above where the note goes: start a timer, add an event, start a block, copy a sum. */
@Composable
private fun OfferRow(offer: DrawerResult, style: HomeStyle, onClick: () -> Unit) {
    val context = LocalContext.current
    val p = style.palette()
    val (icon, label) = when (offer) {
        is DrawerResult.Clock -> Icons.Default.PlayArrow to app.bedtime.data.ClockActions.label(offer.action) { h, m ->
            app.bedtime.ui.formatTime(context, java.time.LocalDate.now().atTime(h, m).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()).lowercase()
        }
        is DrawerResult.AddEvent -> Icons.Default.DateRange to AppDrawer.eventLabel(offer.event) {
            app.bedtime.ui.formatTime(context, it.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli())
        }
        is DrawerResult.StartBlock -> app.bedtime.ui.components.ScheduleIcons.of(offer.block.icon) to
            "start ${offer.block.name.lowercase()} · ${app.bedtime.ui.formatMinutes(offer.block.durationMinutes.toLong())}"
        is DrawerResult.Calculation -> Icons.Default.Add to "= ${offer.answer} · copy"
        else -> return
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, p.accent.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = p.accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = p.accent)
    }
}

/** The microjournal as the quick note reaches it: your sections, and where today's words go, with the sections picked. */
internal class JournalWith(val sections: List<app.bedtime.data.JournalSection>, val onSave: (String, List<String>) -> Unit)
