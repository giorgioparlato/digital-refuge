package app.bedtime.ui.minimal

import app.bedtime.data.Notes

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import app.bedtime.data.Habit
import app.bedtime.data.Habits
import app.bedtime.data.HomeStyle
import app.bedtime.data.HourlyUse
import app.bedtime.data.Module
import app.bedtime.data.Modules
import app.bedtime.data.Note
import app.bedtime.data.NoteWidget
import app.bedtime.data.Screen
import app.bedtime.ui.habits.HabitsWidget
import app.bedtime.ui.habits.HabitsDots
import app.bedtime.ui.homestyle.palette
import app.bedtime.ui.notes.NoteWidgetView

/** The note widget's two places: in the list, and small beside the clock where it's asked for and fits. */
internal class NoteSlots(val inList: (@Composable () -> Unit)?, val beside: (@Composable () -> Unit)?)

/**
 * The note widget as the home and lock screens show it on [screen], or nothing where it's off.
 * Ticking a box or writing in it saves through [onWrite]; holding it opens its sheet.
 */
internal fun noteSlots(style: HomeStyle, screen: Screen, note: Note?, onWrite: (Note, String) -> Unit, onHold: () -> Unit): NoteSlots {
    if (!Modules.isOn(style, screen, Module.NOTE)) return NoteSlots(null, null)
    // The to-do note's ticked items go down below the rest, dated, as in the note itself.
    val toggle = { i: Int ->
        note?.let { n -> onWrite(n, if (n.id == Notes.TODO_ID) app.bedtime.data.Todo.toggled(n.text, i, java.time.LocalDate.now()) else NoteWidget.toggled(n.text, i)) } ?: Unit
    }
    val save = { written: String -> note?.let { onWrite(it, written) } ?: Unit }
    return NoteSlots(
        inList = {
            val p = style.palette()
            NoteWidgetView(
                note, p.text, p.faint, p.accent,
                Modifier.padding(top = 22.dp),
                maxLines = style.noteLines,
                card = style.noteBackground?.let { Color(it) },
                darkText = style.noteDarkText,
                textScale = style.noteTextSize.scale,
                onToggle = toggle,
                onSave = save,
                onHold = onHold,
            )
        },
        beside = if (style.noteBeside(screen) && style.noteFitsBesideClock(screen)) {
            {
                val p = style.palette()
                NoteWidgetView(
                    note, p.text, p.faint, p.accent,
                    // As many lines as chosen, a row each, in the room beside the clock.
                    maxLines = style.noteLines.coerceAtMost(6),
                    // Its own colours beside the clock too, as in the list.
                    card = style.noteBackground?.let { Color(it) },
                    darkText = style.noteDarkText,
                    textScale = style.noteTextSize.scale,
                    compact = true,
                    onToggle = toggle,
                    onSave = save,
                    onHold = onHold,
                )
            }
        } else {
            null
        },
    )
}

/**
 * The habits widget on [screen]: those shown there, or nothing where it's off or none are. A tap
 * hands [onMark] the habit and the value it's tapped to for today.
 */
internal fun habitsSlot(
    style: HomeStyle,
    screen: Screen,
    habits: List<Habit>,
    marks: Map<String, Map<Long, Int>>,
    onMark: (Habit, Long, Int) -> Unit,
    onOpen: (Habit) -> Unit,
    onHold: () -> Unit,
    /** All your habits, from a tap on the dots. */
    onOpenAll: () -> Unit = {},
): (@Composable () -> Unit)? {
    val shown = habits.filter { it.shownOn(screen) }
    if (shown.isEmpty() || !Modules.isOn(style, screen, Module.HABITS)) return null
    return ret@{
        val p = style.palette()
        val today = HourlyUse.today()
        if (style.habitsDots) {
            HabitsDots(shown, marks, today, p.text, p.faint, p.accent, Modifier.padding(top = 22.dp), onOpenAll, onHold)
            return@ret
        }
        HabitsWidget(
            shown, marks, today, p.text, p.faint, p.accent,
            Modifier.padding(top = 22.dp),
            onTap = { h -> onMark(h, today, Habits.tapped(h, marks[h.id]?.get(today) ?: 0)) },
            onOpen = onOpen,
            onHold = onHold,
        )
    }
}

/**
 * Today's microjournal as one quiet line: how many lines and the latest's time, never what they say,
 * so it can sit on the lock screen too. A tap to write; held, its sheet.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun JournalWidget(style: HomeStyle, entry: app.bedtime.data.JournalEntry?, onTap: () -> Unit, onHold: (() -> Unit)?) {
    val p = style.palette()
    val (count, last) = app.bedtime.data.Journal.glance(entry)
    app.bedtime.ui.components.Text(
        when (count) {
            0 -> "microjournal · a line for today ›"
            1 -> "microjournal · 1 line today, at $last ›"
            else -> "microjournal · $count lines today · last $last ›"
        },
        style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
        color = p.muted,
        modifier = Modifier
            .padding(top = 12.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
            .combinedClickable(onClickLabel = "write in the microjournal", onLongClick = onHold, onClick = onTap)
            .padding(vertical = 6.dp),
    )
}
