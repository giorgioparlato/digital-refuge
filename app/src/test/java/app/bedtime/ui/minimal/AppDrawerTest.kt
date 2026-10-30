package app.bedtime.ui.minimal

import app.bedtime.apps.AppEntry
import app.bedtime.data.NoteTarget
import app.bedtime.data.ParsedEvent
import app.bedtime.data.SearchTarget
import app.bedtime.data.SearchChoice
import app.bedtime.data.SearchOrder
import app.bedtime.data.SearchTargets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class AppDrawerTest {
    private val apps = listOf("Signal", "maps", "Calendar", "1Password", "Clock", "Camera").map { AppEntry("pkg.$it", it) }

    @Test
    fun sectionsAreAlphabeticalWithNumbersLast() {
        val sections = AppDrawer.sections(apps)
        assertEquals(listOf("C", "M", "S", "#"), sections.map { it.first })
        assertEquals(listOf("Calendar", "Camera", "Clock"), sections[0].second.map { it.label })
    }

    @Test
    fun resultsComeInOrder() {
        val web = SearchTarget(SearchTarget.Kind.WEB, "pkg.browser", "Browser")
        val scratch = NoteTarget("scratchpad", "content://x")
        val event = ParsedEvent("dentist", LocalDateTime.of(2026, 10, 30, 9, 0), LocalDateTime.of(2026, 10, 30, 10, 0), false)
        val results = AppDrawer.results("ca", apps, listOf(web), listOf(scratch), event)
        // Apps starting with the words first, then those only containing them.
        assertEquals(
            listOf("Calendar", "Camera"),
            results.filterIsInstance<DrawerResult.App>().map { it.app.label },
        )
        assertEquals(
            listOf(DrawerResult.Search(web), DrawerResult.AddNote(scratch), DrawerResult.AddNote(null), DrawerResult.AddEvent(event)),
            results.filterNot { it is DrawerResult.App },
        )
        assertTrue(AppDrawer.results("  ", apps, listOf(web), emptyList(), null).isEmpty())
        assertTrue(DrawerResult.OpenNotes in AppDrawer.results("note", apps, emptyList(), emptyList(), null))
    }

    @Test
    fun searchOnlyThroughAppsYouCouldOpen() {
        val found = listOf(
            SearchTarget(SearchTarget.Kind.WEB, "pkg.blocked", "Blocked browser"),
            SearchTarget(SearchTarget.Kind.WEB, "pkg.browser", "Browser"),
            SearchTarget(SearchTarget.Kind.WIKIPEDIA, "pkg.browser", "Browser"),
            SearchTarget(SearchTarget.Kind.MAPS, "pkg.maps", "Maps"),
            SearchTarget(SearchTarget.Kind.LOOKUP, "pkg.dict", "Dictionary", "pkg.dict.Lookup"),
            SearchTarget(SearchTarget.Kind.LOOKUP, "pkg.translate", "Translate", "pkg.translate.Do"),
        )
        val offered = SearchTargets.offered(found, setOf("pkg.browser", "pkg.dict", "pkg.translate"))
        // Every one whose app could open now; which show, and in what order, is the choices' to say.
        assertEquals(
            listOf("pkg.browser" to SearchTarget.Kind.WEB, "pkg.browser" to SearchTarget.Kind.WIKIPEDIA, "pkg.dict" to SearchTarget.Kind.LOOKUP, "pkg.translate" to SearchTarget.Kind.LOOKUP),
            offered.map { it.packageName to it.kind },
        )
    }

    @Test
    fun withNothingChosenOneOfEachThenNotesAndCalendar() {
        val targets = listOf(
            SearchTarget(SearchTarget.Kind.WEB, "pkg.chrome", "Chrome"),
            SearchTarget(SearchTarget.Kind.WEB, "pkg.firefox", "Firefox"),
            SearchTarget(SearchTarget.Kind.LOOKUP, "pkg.dict", "Dictionary", "x"),
            SearchTarget(SearchTarget.Kind.WIKIPEDIA, "pkg.wiki", "Wikipedia"),
        )
        val scratch = NoteTarget("scratchpad", "content://s")
        // For someone new, only one web search, notes and the calendar are on; the rest wait below, off.
        assertEquals(
            listOf(
                SearchChoice("web:pkg.chrome", true),
                SearchChoice("note:content://s", true),
                SearchChoice(SearchOrder.REFUGE_NOTES, true),
                SearchChoice(SearchOrder.CALENDAR, true),
                SearchChoice(SearchOrder.ALARM, true),
                SearchChoice(SearchOrder.TIMER, true),
                SearchChoice(SearchOrder.SETTINGS, true),
                SearchChoice(SearchOrder.CALCULATOR, true),
                SearchChoice(SearchOrder.BLOCKS, true),
                SearchChoice(SearchOrder.SHORTCUTS, true),
                SearchChoice(SearchOrder.DICTIONARY, true),
                SearchChoice("web:pkg.firefox", false),
                SearchChoice("wikipedia:pkg.wiki", false),
                SearchChoice("lookup:pkg.dict", false),
                SearchChoice(SearchOrder.CONTACTS, false),
                SearchChoice(SearchOrder.FILES, false),
            ),
            SearchOrder.defaults(targets, listOf(scratch)),
        )
        // Obsidian's notes are offered only with Obsidian on the phone, off until you choose your vault.
        assertTrue(SearchChoice(SearchOrder.OBSIDIAN, false) in SearchOrder.defaults(targets, emptyList(), obsidian = true))
        assertTrue(SearchOrder.defaults(targets, emptyList()).none { it.key == SearchOrder.OBSIDIAN })
    }

    @Test
    fun yourOrderAndSwitchesShapeTheResults() {
        val web = SearchTarget(SearchTarget.Kind.WEB, "pkg.browser", "Browser")
        val wiki = SearchTarget(SearchTarget.Kind.WIKIPEDIA, "pkg.wiki", "Wikipedia")
        val defaults = SearchOrder.defaults(listOf(web, wiki), emptyList())
        // Refuge notes first, Wikipedia off; the calendar wasn't in your list yet, so it joins the ones on,
        // and those switched off sit below them all.
        val saved = listOf(SearchChoice(SearchOrder.REFUGE_NOTES, true), SearchChoice("wikipedia:pkg.wiki", false), SearchChoice("web:pkg.browser", true))
        val choices = SearchOrder.arrange(defaults, saved)
        assertEquals(
            listOf(
                SearchChoice(SearchOrder.REFUGE_NOTES, true),
                SearchChoice("web:pkg.browser", true),
                SearchChoice(SearchOrder.CALENDAR, true),
                SearchChoice(SearchOrder.ALARM, true),
                SearchChoice(SearchOrder.TIMER, true),
                SearchChoice(SearchOrder.SETTINGS, true),
                SearchChoice(SearchOrder.CALCULATOR, true),
                SearchChoice(SearchOrder.BLOCKS, true),
                SearchChoice(SearchOrder.SHORTCUTS, true),
                SearchChoice(SearchOrder.DICTIONARY, true),
                SearchChoice("wikipedia:pkg.wiki", false),
                SearchChoice(SearchOrder.CONTACTS, false),
                SearchChoice(SearchOrder.FILES, false),
            ),
            choices,
        )
        val results = AppDrawer.results("tea", emptyList(), listOf(web, wiki), emptyList(), null, choices)
        assertEquals(listOf(DrawerResult.AddNote(null), DrawerResult.Search(web)), results)
        // Moving one up.
        assertEquals("web:pkg.browser", SearchOrder.moved(choices, "web:pkg.browser", up = true)[0].key)
    }

    @Test
    fun switchingMovesItBelowTheOnesStillOn() {
        val a = SearchChoice("a", true); val b = SearchChoice("b", true); val c = SearchChoice("c", true); val d = SearchChoice("d", false)
        // Off: just past the last one on.
        assertEquals(listOf(a, c, b.copy(on = false), d), SearchOrder.switched(listOf(a, b, c, d), "b", on = false))
        // On again: at the end of those on.
        assertEquals(listOf(a, b, c, d.copy(on = true)), SearchOrder.switched(listOf(a, b, c, d), "d", on = true))
        // Saved in a mixed order, they still show on first.
        assertEquals(listOf(a, c, d), SearchOrder.onFirst(listOf(d, a, c)))
        // Dragging the first to the third place.
        assertEquals(listOf(b, c, a, d), SearchOrder.move(listOf(a, b, c, d), 0, 2))
    }

    @Test
    fun contactsShowWhereYouPutThem() {
        val anna = app.bedtime.data.ContactHit("Anna", "+46 70 123", "+4670123")
        val choices = listOf(SearchChoice(SearchOrder.CONTACTS, true), SearchChoice(SearchOrder.REFUGE_NOTES, true))
        assertEquals(
            listOf(DrawerResult.Contact(anna), DrawerResult.AddNote(null)),
            AppDrawer.results("ann", emptyList(), emptyList(), emptyList(), null, choices, contacts = listOf(anna)),
        )
        // Switched off, none show even if found.
        assertEquals(
            listOf(DrawerResult.AddNote(null)),
            AppDrawer.results("ann", emptyList(), emptyList(), emptyList(), null, choices.map { if (it.key == SearchOrder.CONTACTS) it.copy(on = false) else it }, contacts = listOf(anna)),
        )
    }

    @Test
    fun whatElseTheWordsFoundShowsWhereYouPutIt() {
        val note = app.bedtime.data.IndexedFile("Trip.md", "travel/Trip.md", "content://v/trip", "text/markdown")
        val pdf = app.bedtime.data.IndexedFile("trip tickets.pdf", "trip tickets.pdf", "content://f/t", "application/pdf")
        val found = mapOf(SearchOrder.OBSIDIAN to listOf(DrawerResult.VaultNote(note)), SearchOrder.FILES to listOf(DrawerResult.File(pdf)))
        val choices = listOf(SearchChoice(SearchOrder.FILES, true), SearchChoice(SearchOrder.REFUGE_NOTES, true), SearchChoice(SearchOrder.OBSIDIAN, false))
        assertEquals(
            listOf(DrawerResult.File(pdf), DrawerResult.AddNote(null)),
            AppDrawer.results("trip", emptyList(), emptyList(), emptyList(), null, choices, found = found),
        )
        // What's found comes before what would be done, whatever the order of the choices, with a line between.
        val actionFirst = listOf(SearchChoice(SearchOrder.REFUGE_NOTES, true), SearchChoice(SearchOrder.FILES, true))
        val mixed = AppDrawer.results("trip", apps, emptyList(), emptyList(), null, actionFirst, found = found)
        assertEquals(listOf(DrawerResult.File(pdf), DrawerResult.AddNote(null)), mixed)
        assertEquals(1, AppDrawer.dividerAt(mixed))
        assertEquals(null, AppDrawer.dividerAt(listOf(DrawerResult.AddNote(null))))
        // A keyword reaches it even switched off.
        assertEquals(
            listOf(DrawerResult.VaultNote(note)),
            AppDrawer.results("o trip", emptyList(), emptyList(), emptyList(), null, choices, keyword = app.bedtime.data.Keywords.Hit(SearchOrder.OBSIDIAN, "trip"), found = found),
        )
    }
}
