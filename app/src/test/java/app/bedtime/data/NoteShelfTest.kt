package app.bedtime.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteShelfTest {
    private val todo = Note(Notes.TODO_ID, "# To-do\n- [ ] post\n\n- [x] bread ✅ 2026-10-30\n", 1)
    private val books = Note("books", "# Books\n- Stoner\n", 2, labels = listOf("reading"))
    private val quick = Note("q", "Paterson", 3, labels = listOf("films"))
    private val long = Note("long", "# Trip\nTrains first.\n\nThen the coast.", 4)

    @Test
    fun theToDoThenThosePinnedThenTheRest() {
        val notes = listOf(quick, books.copy(pinned = true), long, todo)
        assertEquals(listOf(Notes.TODO_ID, "books", "q", "long"), NoteShelf.ordered(notes).map { it.id })
    }

    @Test
    fun aOneLineNoteMovesInAsAListItem() {
        val moved = NoteShelf.movedInto(listOf(books, quick), "q", "books", 9)
        assertEquals(listOf("books"), moved.map { it.id })
        assertEquals("# Books\n- Stoner\n- Paterson\n", moved.single().text)
        assertEquals(listOf("reading", "films"), moved.single().labels)
        assertEquals(9L, moved.single().editedAt)
    }

    @Test
    fun intoTheToDoItsATaskAboveThoseDone() {
        val moved = NoteShelf.movedInto(listOf(todo, quick), "q", Notes.TODO_ID, 9)
        assertEquals("# To-do\n- [ ] post\n- [ ] Paterson\n\n- [x] bread ✅ 2026-10-30\n", moved.single().text)
    }

    @Test
    fun aLongerOneMovesInAfterABlankLine() {
        val moved = NoteShelf.movedInto(listOf(books, long), "long", "books", 9)
        assertEquals("# Books\n- Stoner\n\n# Trip\nTrains first.\n\nThen the coast.\n", moved.single().text)
        // Into itself, or into nothing, nothing changes.
        assertEquals(listOf(books), NoteShelf.movedInto(listOf(books), "books", "books", 9))
    }

    @Test
    fun mergedInTheOrderPicked() {
        val merged = NoteShelf.merged(listOf(books, quick, long.copy(pinned = true)), listOf("long", "q"), 9)
        assertEquals(listOf("books", "long"), merged.map { it.id })
        val one = merged.first { it.id == "long" }
        assertEquals("# Trip\nTrains first.\n\nThen the coast.\n\nPaterson\n", one.text)
        assertEquals(true, one.pinned)
        assertEquals(listOf("films"), one.labels)
        // One alone isn't a merge.
        assertEquals(listOf(books, quick), NoteShelf.merged(listOf(books, quick), listOf("q"), 9))
    }

    @Test
    fun labelsTidiedAndFilteredBy() {
        val notes = NoteShelf.withLabels(listOf(books, quick), "q", listOf(" films ", "Films", "", "ideas"))
        assertEquals(listOf("films", "ideas"), notes.first { it.id == "q" }.labels)
        assertEquals(listOf("films", "ideas", "reading"), NoteShelf.labels(notes))
        assertEquals(listOf("q"), NoteShelf.labelled(notes, "ideas").map { it.id })
        assertEquals(2, NoteShelf.labelled(notes, null).size)
        assertEquals(true, NoteShelf.pinned(notes, "q", true).first { it.id == "q" }.pinned)
    }

    @Test
    fun theTrashKeepsAMonth() {
        val day = 86_400_000L
        val now = 100 * day
        val trash = listOf(TrashedNote(books, now - 2 * day), TrashedNote(quick, now - 31 * day))
        assertEquals(listOf("books"), NoteShelf.keptTrash(trash, now).map { it.note.id })
        assertEquals(28L, NoteShelf.daysLeft(trash.first(), now))
        assertEquals(1L, NoteShelf.daysLeft(TrashedNote(books, now - 30 * day + 1), now))
    }

    @Test
    fun sortingByDateLeavesYourOwnOrderAsItWas() {
        // Stored in your order: long, books, quick. Created 4, 2, 3; edited 5, 9, 7.
        val notes = listOf(long.copy(editedAt = 5), books.copy(editedAt = 9), quick.copy(editedAt = 7), todo)
        assertEquals(listOf(Notes.TODO_ID, "long", "books", "q"), NoteShelf.sorted(notes, NotesSort.CUSTOM).map { it.id })
        assertEquals(listOf(Notes.TODO_ID, "books", "q", "long"), NoteShelf.sorted(notes, NotesSort.EDITED).map { it.id })
        assertEquals(listOf(Notes.TODO_ID, "long", "q", "books"), NoteShelf.sorted(notes, NotesSort.CREATED).map { it.id })
        // Pinned ones still come first, however they're sorted.
        assertEquals("books", NoteShelf.sorted(notes.map { if (it.id == "books") it.copy(pinned = true) else it }, NotesSort.CREATED)[1].id)
        // A card dragged to a new place saves that order; one left out of it stays at the end.
        assertEquals(listOf("q", "long", "books", Notes.TODO_ID), NoteShelf.reordered(notes, listOf("q", "long", "books")).map { it.id })
    }
}
