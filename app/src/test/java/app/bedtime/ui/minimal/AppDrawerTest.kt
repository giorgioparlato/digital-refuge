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
        assertEquals(
            listOf(
                SearchChoice("web:pkg.chrome", true),
                SearchChoice("web:pkg.firefox", false),
                // Wikipedia is offered, but off until you want it.
                SearchChoice("wikipedia:pkg.wiki", false),
                SearchChoice("lookup:pkg.dict", true),
                SearchChoice("note:content://s", true),
                SearchChoice(SearchOrder.REFUGE_NOTES, true),
                SearchChoice(SearchOrder.CALENDAR, true),
            ),
            SearchOrder.defaults(targets, listOf(scratch)),
        )
    }

    @Test
    fun yourOrderAndSwitchesShapeTheResults() {
        val web = SearchTarget(SearchTarget.Kind.WEB, "pkg.browser", "Browser")
        val wiki = SearchTarget(SearchTarget.Kind.WIKIPEDIA, "pkg.wiki", "Wikipedia")
        val defaults = SearchOrder.defaults(listOf(web, wiki), emptyList())
        // Refuge notes first, Wikipedia off; the calendar wasn't in your list yet, so it joins at the end.
        val saved = listOf(SearchChoice(SearchOrder.REFUGE_NOTES, true), SearchChoice("wikipedia:pkg.wiki", false), SearchChoice("web:pkg.browser", true))
        val choices = SearchOrder.arrange(defaults, saved)
        assertEquals(saved + SearchChoice(SearchOrder.CALENDAR, true), choices)
        val results = AppDrawer.results("tea", emptyList(), listOf(web, wiki), emptyList(), null, choices)
        assertEquals(listOf(DrawerResult.AddNote(null), DrawerResult.Search(web)), results)
        // Moving one up.
        assertEquals("web:pkg.browser", SearchOrder.moved(choices, "web:pkg.browser", up = true)[1].key)
    }
}
