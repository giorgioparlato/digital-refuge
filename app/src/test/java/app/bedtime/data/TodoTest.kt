package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class TodoTest {
    private val today = LocalDate.of(2026, 10, 31)

    @Test
    fun aTickedItemGoesToTheDoneOnesDatedNewestFirst() {
        val note = "# To-do\n- [ ] milk\n- [ ] post\n\n- [x] bread ✅ 2026-10-30\n"
        assertEquals("# To-do\n- [ ] post\n\n- [x] milk ✅ 2026-10-31\n- [x] bread ✅ 2026-10-30\n", Todo.toggled(note, 1, today))
    }

    @Test
    fun untickedItComesBackToTheListWithoutItsDate() {
        val note = "# To-do\n- [ ] post\n\n- [x] milk ✅ 2026-10-31\n- [x] bread ✅ 2026-10-30\n"
        assertEquals("# To-do\n- [ ] post\n- [ ] milk\n\n- [x] bread ✅ 2026-10-30\n", Todo.toggled(note, 3, today))
    }

    @Test
    fun itemsUnderAnItemMoveWithIt() {
        val note = "- [ ] trip\n  - [ ] tickets\n  - [ ] bags\n- [ ] post"
        assertEquals("- [ ] post\n\n- [x] trip ✅ 2026-10-31\n  - [ ] tickets\n  - [ ] bags\n", Todo.toggled(note, 0, today))
        // One under another stays among its own, done after those to do.
        assertEquals("- [ ] trip\n  - [ ] bags\n  - [x] tickets ✅ 2026-10-31\n- [ ] post", Todo.toggled(note, 1, today))
    }

    @Test
    fun doneOnesAnywhereGatherAtTheEnd() {
        // As notes were before: done ones left among the rest, without dates.
        val (open, done) = Todo.split("# To-do\n- [x] old\n- [ ] post\n- [x] older")
        assertEquals("# To-do\n- [ ] post", open)
        assertEquals(listOf("old", "older"), done.map { it.text })
        assertEquals(listOf<LocalDate?>(null, null), done.map { it.date })
        assertEquals("# To-do\n- [ ] post\n\n- [x] old\n- [x] older\n", Todo.join(open, done))
        // Anything but a to-do is left alone.
        assertEquals("# To-do\n- [ ] post", Todo.toggled("# To-do\n- [ ] post", 0, today))
    }

    @Test
    fun aNewOneGoesAboveThoseDone() {
        assertEquals("# To-do\n- [ ] post\n- [ ] milk\n\n- [x] bread ✅ 2026-10-30\n", Todo.added("# To-do\n- [ ] post\n\n- [x] bread ✅ 2026-10-30\n", "- [ ] milk"))
        assertEquals("# To-do\n- [ ] post\n- [ ] milk\n", Todo.added("# To-do\n- [ ] post\n", "- [ ] milk"))
        assertEquals("# To-do\n- [ ] milk\n", Todo.added("# To-do\n", "- [ ] milk"))
        assertEquals("# To-do\n- [ ] post\n", Todo.withoutDone("# To-do\n- [ ] post\n\n- [x] bread ✅ 2026-10-30\n"))
    }

    @Test
    fun doneOnesFoldedByWhen() {
        fun d(text: String, date: LocalDate?) = DoneTodo(listOf("- [x] $text"), text, date)
        val done = listOf(
            d("a", today), d("b", today.minusDays(6)), d("c", today.minusDays(14)),
            d("d", LocalDate.of(2026, 9, 3)), d("e", LocalDate.of(2026, 2, 10)), d("f", LocalDate.of(2025, 7, 1)), d("g", null),
        )
        // Saturday 31 October: the week began on Monday the 26th.
        assertEquals(
            listOf("This week" to "a", "Last week" to "b", "Earlier this month" to "c", "Last month" to "d", "February" to "e", "2025" to "f", "Undated" to "g"),
            Todo.groups(done, today).map { (name, items) -> name to items.joinToString("") { it.text } },
        )
    }

    @Test
    fun remindersOnOffer() {
        val zone = java.time.ZoneId.systemDefault()
        val afternoon = java.time.LocalDateTime.of(2026, 10, 31, 14, 0)
        val choices = NoteShelf.reminderChoices(afternoon)
        assertEquals(listOf("In an hour", "In 3 hours", "This evening", "Tomorrow morning", "Next week"), choices.map { it.first })
        assertEquals(java.time.LocalDateTime.of(2026, 11, 2, 9, 0).atZone(zone).toInstant().toEpochMilli(), choices.last().second)
        assertEquals(4, NoteShelf.reminderChoices(afternoon.withHour(17).withMinute(30)).size)
        val notes = listOf(Note("a", "x", 1, remindAt = 500), Note("b", "y", 1, remindAt = 200), Note("c", "z", 1))
        assertEquals(500L, NoteShelf.nextReminder(notes, 300))
        assertEquals(listOf("b"), NoteShelf.due(notes, 300).map { it.id })
    }
}
