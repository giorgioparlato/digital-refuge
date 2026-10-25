package app.bedtime.ui.minimal

import app.bedtime.apps.AppEntry
import app.bedtime.data.NoteTarget
import app.bedtime.data.ParsedEvent
import app.bedtime.data.SearchTarget
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
        assertEquals(
            listOf("pkg.browser" to SearchTarget.Kind.WEB, "pkg.browser" to SearchTarget.Kind.WIKIPEDIA, "pkg.dict" to SearchTarget.Kind.LOOKUP, "pkg.translate" to SearchTarget.Kind.LOOKUP),
            offered.map { it.packageName to it.kind },
        )
    }
}
