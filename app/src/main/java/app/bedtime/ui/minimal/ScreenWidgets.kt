package app.bedtime.ui.minimal

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    val toggle = { i: Int -> note?.let { onWrite(it, NoteWidget.toggled(it.text, i)) } ?: Unit }
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
                    maxLines = style.noteLines.coerceAtMost(3),
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
): (@Composable () -> Unit)? {
    val shown = habits.filter { it.shownOn(screen) }
    if (shown.isEmpty() || !Modules.isOn(style, screen, Module.HABITS)) return null
    return {
        val p = style.palette()
        val today = HourlyUse.today()
        HabitsWidget(
            shown, marks, today, p.text, p.faint, p.accent,
            Modifier.padding(top = 22.dp),
            onTap = { h -> onMark(h, today, Habits.tapped(h, marks[h.id]?.get(today) ?: 0)) },
            onOpen = onOpen,
            onHold = onHold,
        )
    }
}
