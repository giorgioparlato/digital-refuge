package app.bedtime.ui.homestyle

import app.bedtime.data.HomeStyle
import app.bedtime.data.Module
import app.bedtime.data.Note
import app.bedtime.data.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class WidgetOptionsTest {
    private val notes = listOf(Note(id = "a", text = "# Groceries", createdAt = 0), Note(id = "b", text = "Ideas", createdAt = 0))

    /** Each option's change, made once with a value other than the current one. */
    private fun changes(module: Module, style: HomeStyle, screen: Screen): List<Pair<String, HomeStyle>> {
        val out = mutableListOf<Pair<String, HomeStyle>>()
        WidgetOptions.of(module, style, screen, change = { out += "" to it }, notes = notes)
            .forEach { option ->
                val before = out.size
                when (option) {
                    is WidgetOption.Choice<*> -> {
                        @Suppress("UNCHECKED_CAST")
                        val choice = option as WidgetOption.Choice<Any?>
                        choice.options.firstOrNull { it.second != choice.selected }?.let { choice.pick(it.second) }
                    }
                    is WidgetOption.Switch -> option.set(!option.on)
                    is WidgetOption.Colour -> option.pick(option.presets.first().second)
                    is WidgetOption.Pick -> option.items.firstOrNull { it.first != option.selected }?.let { option.pick(it.first) }
                    else -> Unit
                }
                if (out.size > before) out[out.lastIndex] = option.label to out.last().second
            }
        return out
    }

    @Test
    fun everyChangeFromTheSheetIsKept() {
        // The sheet saves through sheetFields: whatever an option changes must be among them.
        val style = HomeStyle(noteBackground = 0xFF223344)
        Module.entries.forEach { module ->
            Screen.entries.forEach { screen ->
                changes(module, style, screen).forEach { (label, changed) ->
                    assertEquals("$module on $screen: $label", changed, style.sheetFields(changed))
                }
            }
        }
    }

    @Test
    fun everyWidgetReadsInTheSameOrder() {
        // What it shows, then its size, then its colours, then what a tap does.
        val labels = WidgetOptions.of(Module.NOTE, HomeStyle(noteBackground = 0xFF223344, noteWidgetId = "a"), Screen.HOME, {}, notes).map { it.label }
        assertEquals(
            listOf("Show", "Lines shown", "Beside the clock", "Text size", "Its background", "Its text", "Open this note in full", "Tap its lines to write right there."),
            labels,
        )
        val tracker = WidgetOptions.of(Module.HOURS, HomeStyle(), Screen.HOME, {}).map { it.label }
        assertEquals("Look", tracker.first())
        assert(tracker.indexOf("Calm hours") > tracker.indexOf("Unlocks"))
        assert(tracker.indexOf("Open your statistics") > tracker.indexOf("Distracted hours"))
    }

    @Test
    fun eventNamesAreTheLockScreensOnly() {
        val home = WidgetOptions.of(Module.EVENT, HomeStyle(), Screen.HOME, {}).map { it.label }
        val lock = WidgetOptions.of(Module.EVENT, HomeStyle(), Screen.LOCK, {}).map { it.label }
        assert("Event names" !in home)
        assertNotNull(lock.firstOrNull { it == "Event names" })
    }
}
