package app.bedtime.data

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import java.util.Locale

/** Somewhere the minimal home's search can take what you typed. */
data class SearchTarget(
    val kind: Kind,
    val packageName: String,
    /** The app's own name, for "look up in …". */
    val appLabel: String,
    /** The exact screen, where an app offers several (dictionaries' "look up"). */
    val activity: String? = null,
) {
    enum class Kind { WEB, WIKIPEDIA, MAPS, LOOKUP }

    /** How the result reads, in the app's lowercase, with [query] in it as typed. */
    fun label(query: String): String = when (kind) {
        Kind.WEB -> "search the web for “$query”"
        Kind.WIKIPEDIA -> "wikipedia: “$query”"
        Kind.MAPS -> "maps: “$query”"
        Kind.LOOKUP -> "look up “$query” in ${appLabel.lowercase()}"
    }
}

/**
 * The search's places to send words, found among the apps on the phone; offered only when their app
 * is one you could open right now anyway.
 */
object SearchTargets {
    private const val WIKIPEDIA_APP = "org.wikipedia"

    /** The ones whose app is in [openable] right now; which of them show, and in what order, is [SearchOrder]'s. */
    fun offered(found: List<SearchTarget>, openable: Set<String>): List<SearchTarget> =
        found.filter { it.packageName in openable }.distinctBy { it.kind to it.packageName }

    /** Every app that could take a search, of each kind. Wikipedia only through its own app, never a browser. */
    fun find(context: Context): List<SearchTarget> {
        val pm = context.packageManager
        fun handlers(intent: Intent) = runCatching { pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY) }.getOrDefault(emptyList())
        fun label(pkg: String) = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
        val browsers = handlers(Intent(Intent.ACTION_VIEW, Uri.parse("https://wikipedia.org"))).map { it.activityInfo.packageName }
        val wikipedia = listOfNotNull(WIKIPEDIA_APP.takeIf { runCatching { pm.getApplicationInfo(it, 0) }.isSuccess })
        return buildList {
            handlers(Intent(Intent.ACTION_WEB_SEARCH)).forEach {
                add(SearchTarget(SearchTarget.Kind.WEB, it.activityInfo.packageName, label(it.activityInfo.packageName), it.activityInfo.name))
            }
            // Without a web-search screen, a browser can still search through its address bar.
            browsers.forEach { add(SearchTarget(SearchTarget.Kind.WEB, it, label(it))) }
            wikipedia.forEach { add(SearchTarget(SearchTarget.Kind.WIKIPEDIA, it, label(it))) }
            handlers(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=x"))).forEach { add(SearchTarget(SearchTarget.Kind.MAPS, it.activityInfo.packageName, label(it.activityInfo.packageName))) }
            handlers(Intent(Intent.ACTION_PROCESS_TEXT).setType("text/plain")).forEach {
                add(SearchTarget(SearchTarget.Kind.LOOKUP, it.activityInfo.packageName, it.loadLabel(pm).toString(), it.activityInfo.name))
            }
        }
    }

    /** The intent that takes [query] to [target]. */
    fun intent(target: SearchTarget, query: String): Intent {
        val q = Uri.encode(query)
        val intent = when (target.kind) {
            // An app with a web-search screen gets the words; a plain browser gets them as an address.
            SearchTarget.Kind.WEB -> if (target.activity != null) {
                Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse("https://duckduckgo.com/?q=$q"))
            }
            // The Wikipedia app takes its own links; no browser is ever asked.
            SearchTarget.Kind.WIKIPEDIA ->
                Intent(Intent.ACTION_VIEW, Uri.parse("https://${Locale.getDefault().language.ifBlank { "en" }}.wikipedia.org/wiki/Special:Search?search=$q"))
            SearchTarget.Kind.MAPS -> Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=$q"))
            SearchTarget.Kind.LOOKUP -> Intent(Intent.ACTION_PROCESS_TEXT)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_PROCESS_TEXT, query)
                .putExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
        }
        return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).also {
            if (target.activity != null) it.setClassName(target.packageName, target.activity) else it.setPackage(target.packageName)
        }
    }
}

/** The target's place in your search choices: its kind and its app. */
val SearchTarget.key: String get() = "${kind.name.lowercase()}:$packageName"

/**
 * What the search offers after apps, and in what order. With nothing chosen: one web search and one
 * maps (the first found of each), every dictionary, your files, refuge notes and the calendar, in that
 * order; Wikipedia is listed, but off until you switch it on. Your choices, once made, keep their order; anything new joins at the end.
 */
object SearchOrder {
    const val REFUGE_NOTES = "note:refuge"
    const val CALENDAR = "calendar"

    fun noteKey(target: NoteTarget) = "note:${target.uri}"

    fun defaults(targets: List<SearchTarget>, notes: List<NoteTarget>): List<SearchChoice> {
        val ordered = SearchTarget.Kind.entries.flatMap { kind -> targets.filter { it.kind == kind }.distinctBy { it.packageName } }
        val firsts = ordered.groupBy { it.kind }.mapValues { (_, same) -> same.first().key }
        val on = { t: SearchTarget -> t.kind == SearchTarget.Kind.LOOKUP || (t.kind != SearchTarget.Kind.WIKIPEDIA && firsts[t.kind] == t.key) }
        return ordered.map { SearchChoice(it.key, on(it)) } +
            notes.map { SearchChoice(noteKey(it), true) } +
            SearchChoice(REFUGE_NOTES, true) +
            SearchChoice(CALENDAR, true)
    }

    fun arrange(defaults: List<SearchChoice>, saved: List<SearchChoice>): List<SearchChoice> {
        val present = defaults.map { it.key }.toSet()
        val known = saved.map { it.key }.toSet()
        return saved.filter { it.key in present } + defaults.filter { it.key !in known }
    }

    /** [choices] with [key] moved one place up (or down), staying put at either end. */
    fun moved(choices: List<SearchChoice>, key: String, up: Boolean): List<SearchChoice> {
        val i = choices.indexOfFirst { it.key == key }
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j !in choices.indices) return choices
        return choices.toMutableList().also { val a = it[i]; it[i] = it[j]; it[j] = a }
    }
}
