package app.bedtime.ui.lock

import androidx.compose.ui.draw.drawBehind

import androidx.compose.ui.graphics.Color

import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import kotlin.math.abs
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.bedtime.data.HomeStyle
import app.bedtime.data.LockNotification
import app.bedtime.data.LockNotifications
import app.bedtime.data.NoteAction
import app.bedtime.data.NoteDetail
import app.bedtime.ui.components.AppIcon
import app.bedtime.ui.components.Text
import app.bedtime.ui.formatTime
import app.bedtime.ui.homestyle.HomePalette
import app.bedtime.ui.homestyle.palette

/** What the lock screen can do with a notification. */
class NoteHandlers(
    /** Unlock, then open it. */
    val open: (String) -> Unit = {},
    /** Clear it, as Android's own shade does. */
    val dismiss: (LockNotification) -> Unit = {},
    /** Out of the way until the session ends. */
    val later: (LockNotification) -> Unit = {},
    /** Its app or conversation off the lock screen until the time chosen ([quietChoices]). */
    val quiet: (LockNotification, Long) -> Unit = { _, _ -> },
    /** How long "quiet until…" can last, each with when it ends. */
    val quietChoices: () -> List<Pair<String, Long>> = { emptyList() },
    /** One of its buttons; a reply carries its text. */
    val act: (LockNotification, NoteAction, String?) -> Unit = { _, _, _ -> },
)

/** How many notifications the open list shows before "and N more". */
private const val MAX_NOTIFICATIONS = 4

/**
 * The notifications on the lock screen: one calm summary line until tapped, then the list. Drag a
 * row down to read it in place and reach its buttons; swipe it aside to clear it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotificationArea(
    notes: List<LockNotification>,
    style: HomeStyle,
    circles: List<app.bedtime.data.Circle> = emptyList(),
    listOpen: Boolean,
    onListOpen: (Boolean) -> Unit,
    handlers: NoteHandlers,
    quickReplies: List<String>,
    expanded: String?,
    onExpand: (String?) -> Unit,
    replying: String?,
    onReply: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = style.palette()
    val scroll = rememberScrollState()
    // Replying, the list holds only that conversation: its foot, the latest messages and the box, stays in view.
    LaunchedEffect(replying) {
        if (replying != null) {
            delay(120)
            scroll.animateScrollTo(scroll.maxValue)
        }
    }
    Column(modifier.verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!listOpen) {
            SummaryLine(LockNotifications.summary(notes), p) { onListOpen(true) }
            return@Column
        }
        val shown = if (replying != null) notes.filter { it.key == replying } else notes.take(MAX_NOTIFICATIONS)
        shown.forEach { note ->
            key(note.key) {
                val swipe = rememberSwipeToDismissBoxState(
                    confirmValueChange = { value ->
                        if (value != SwipeToDismissBoxValue.Settled) handlers.dismiss(note)
                        true
                    },
                )
                SwipeToDismissBox(swipe, backgroundContent = {}) {
                    NotificationRow(
                        note = note,
                        p = p,
                        // A circle's colour down its left edge; the screen's accent if the circle has none.
                        mark = LockNotifications.circleOf(note, circles)?.let { circle -> circle.colour?.let { Color(it) } ?: p.accent },
                        expanded = expanded == note.key,
                        onExpand = { open -> onExpand(if (open) note.key else null) },
                        handlers = handlers,
                        replying = replying == note.key,
                        onReply = { on -> onReply(if (on) note.key else null) },
                        quickReplies = quickReplies,
                    )
                }
            }
        }
        if (replying == null) {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(p.background).padding(horizontal = 14.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                if (notes.size > MAX_NOTIFICATIONS) {
                    Text("and ${notes.size - MAX_NOTIFICATIONS} more", style = MaterialTheme.typography.labelMedium, color = p.faint)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "later for all",
                    style = MaterialTheme.typography.labelMedium,
                    color = p.accent,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(onClickLabel = "keep all of them until the session ends") { notes.forEach(handlers.later) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun SummaryLine(summary: String, p: HomePalette, onOpen: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(p.background)
            .background(p.text.copy(alpha = 0.06f))
            .clickable(onClickLabel = "show the notifications", onClick = onOpen)
            .pullDown(enabled = true, onPull = onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(summary, style = MaterialTheme.typography.bodyMedium, color = p.text, modifier = Modifier.weight(1f))
        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, tint = p.faint, modifier = Modifier.size(20.dp))
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun NotificationRow(
    note: LockNotification,
    p: HomePalette,
    mark: Color? = null,
    expanded: Boolean,
    onExpand: (Boolean) -> Unit,
    handlers: NoteHandlers,
    replying: Boolean,
    onReply: (Boolean) -> Unit,
    quickReplies: List<String>,
) {
    val more = note.messages.isNotEmpty() || note.body != null
    // Opened, by the arrow or a drag, it scrolls itself fully into view.
    val reveal = remember { BringIntoViewRequester() }
    LaunchedEffect(expanded) {
        if (expanded) {
            delay(80)
            reveal.bringIntoView()
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(reveal)
            .clip(RoundedCornerShape(14.dp))
            // Opaque underneath, so a quote it passes over doesn't show through.
            .background(p.background)
            .background(p.text.copy(alpha = 0.06f))
            .then(if (mark != null) Modifier.drawBehind { drawRect(mark, size = size.copy(width = 4.dp.toPx())) } else Modifier),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClickLabel = "unlock and open") { handlers.open(note.key) }
                // Drag down to read it in place, as in Android's own shade.
                .pullDown(enabled = !expanded) { onExpand(true) }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(Modifier.padding(top = 2.dp)) { AppIcon(note.packageName, note.app, 22.dp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(note.app, style = MaterialTheme.typography.labelMedium, color = p.faint, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(formatTime(LocalContext.current, note.postedAt), style = MaterialTheme.typography.labelSmall, color = p.faint)
                    Box(
                        Modifier.size(32.dp).clip(RoundedCornerShape(50)).clickable(onClickLabel = if (expanded) "fold" else "read it here") { onExpand(!expanded) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = p.faint, modifier = Modifier.size(20.dp))
                    }
                }
                val title = note.title ?: if (note.detail == NoteDetail.APP_ONLY) "New notification" else null
                title?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = p.text, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                if (!(expanded && more)) {
                    note.text?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = p.muted, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                }
            }
        }
        if (expanded) {
            Column(Modifier.padding(start = 48.dp, end = 14.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    note.messages.isNotEmpty() -> note.messages.forEach { line ->
                        Text(
                            listOfNotNull(line.sender, line.text).joinToString(": "),
                            style = MaterialTheme.typography.bodySmall,
                            color = p.muted,
                        )
                    }
                    note.body != null -> Text(note.body, style = MaterialTheme.typography.bodySmall, color = p.muted)
                }
                val reply = note.actions.firstOrNull { it.isReply }
                // "Quiet until…" opens its choices in place of the buttons; "‹" goes back to them.
                var quieting by remember(note.key) { mutableStateOf(false) }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (quieting) {
                        NoteChip("‹", p) { quieting = false }
                        handlers.quietChoices().forEach { (label, until) -> NoteChip(label, p) { handlers.quiet(note, until) } }
                    } else {
                        if (reply != null && !replying) NoteChip("Reply", p) { onReply(true) }
                        note.actions.filterNot { it.isReply }.forEach { action -> NoteChip(action.title, p) { handlers.act(note, action, null) } }
                        NoteChip("Later", p) { handlers.later(note) }
                        NoteChip("Quiet until…", p) { quieting = true }
                    }
                }
                if (replying && reply != null) {
                    ReplyBox(p, quickReplies, onSend = { text ->
                        handlers.act(note, reply, text)
                        onReply(false)
                    }, onCancel = { onReply(false) })
                }
            }
        }
    }
}

/** A reply typed on the lock screen, or one of yours with a tap. Sending asks for the unlock, then locks again. */
@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
private fun ReplyBox(p: HomePalette, quickReplies: List<String>, onSend: (String) -> Unit, onCancel: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    // As the reply grows a line, the box keeps itself above the keyboard.
    val reveal = remember { BringIntoViewRequester() }
    LaunchedEffect(text.count { it == '\n' }, text.length / 40) { reveal.bringIntoView() }
    Column(Modifier.bringIntoViewRequester(reveal), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            quickReplies.forEach { quick -> NoteChip(quick, p) { onSend(quick) } }
        }
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = p.text),
            cursorBrush = SolidColor(p.accent),
            // A message: sentences start with a capital, and the keyboard's own key sends it.
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, keyboardType = KeyboardType.Text, imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { if (text.isNotBlank()) onSend(text.trim()) }),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 44.dp, max = 140.dp)
                .focusRequester(focus)
                .border(1.dp, p.faint.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            decorationBox = { field ->
                Box {
                    if (text.isEmpty()) Text("Write a reply", style = MaterialTheme.typography.bodyMedium, color = p.faint)
                    field()
                }
            },
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Sending asks for your unlock, then locks again",
                style = MaterialTheme.typography.labelSmall,
                color = p.faint,
                modifier = Modifier.weight(1f),
            )
            NoteChip("Cancel", p, onClick = onCancel)
            NoteChip("Send", p, enabled = text.isNotBlank()) { onSend(text.trim()) }
        }
    }
}

@Composable
private fun NoteChip(text: String, p: HomePalette, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = if (enabled) p.text else p.faint,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .clip(shape)
            .border(1.dp, p.faint.copy(alpha = if (enabled) 0.5f else 0.25f), shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

/**
 * Runs [onPull] when a finger drags down over this, as pulling Android's shade does. It reads the
 * finger itself, before the scrolling list around it can take the drag, so it works wherever the
 * list is. A drag up or sideways is left alone: to scroll, or to swipe a notification away.
 */
private fun Modifier.pullDown(enabled: Boolean, onPull: () -> Unit): Modifier =
    if (!enabled) this else pointerInput(onPull) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            var travel = Offset.Zero
            while (true) {
                val change = awaitPointerEvent(PointerEventPass.Initial).changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                // Movement as the finger made it, even if something else has claimed it already.
                travel += change.position - change.previousPosition
                val slop = viewConfiguration.touchSlop
                if (travel.y > slop && travel.y > abs(travel.x)) {
                    change.consume()
                    onPull()
                    // The rest of this drag is spent.
                    do {
                        val rest = awaitPointerEvent(PointerEventPass.Initial)
                        rest.changes.forEach { it.consume() }
                    } while (rest.changes.any { it.pressed })
                    break
                }
                if (abs(travel.x) > slop || travel.y < -slop) break
            }
        }
    }
