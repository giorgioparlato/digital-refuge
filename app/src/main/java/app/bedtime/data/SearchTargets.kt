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
    enum class Kind { WEB, WIKIPEDIA, MAPS, LOOKUP, SPOTIFY }

    /** How the result reads, in the app's lowercase, with [query] in it as typed. */
    fun label(query: String): String = when (kind) {
        Kind.WEB -> "search the web for “$query”"
        Kind.WIKIPEDIA -> "wikipedia: “$query”"
        Kind.MAPS -> "maps: “$query”"
        Kind.LOOKUP -> "look up “$query” in ${appLabel.lowercase()}"
        Kind.SPOTIFY -> "search spotify for “$query”"
    }
}

/**
 * The search's places to send words, found among the apps on the phone; offered only when their app
 * is one you could open right now anyway.
 */
object SearchTargets {
    private const val WIKIPEDIA_APP = "org.wikipedia"
    private const val SPOTIFY_APP = "com.spotify.music"

    /** The ones whose app is in [openable] right now; which of them show, and in what order, is [SearchOrder]'s. */
    fun offered(found: List<SearchTarget>, openable: Set<String>): List<SearchTarget> =
        found.filter { it.packageName in openable }.distinctBy { it.kind to it.packageName }

    /** Every app that could take a search, of each kind. Wikipedia only through its own app, never a browser. */
    fun find(context: Context): List<SearchTarget> {
        val pm = context.packageManager
        fun handlers(intent: Intent) = runCatching { pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY) }.getOrDefault(emptyList())
        fun label(pkg: String) = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
        val browsers = handlers(Intent(Intent.ACTION_VIEW, Uri.parse("https://wikipedia.org"))).map { it.activityInfo.packageName }
        fun installed(pkg: String) = runCatching { pm.getApplicationInfo(pkg, 0) }.isSuccess
        val wikipedia = listOfNotNull(WIKIPEDIA_APP.takeIf(::installed))
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
            if (installed(SPOTIFY_APP)) add(SearchTarget(SearchTarget.Kind.SPOTIFY, SPOTIFY_APP, label(SPOTIFY_APP)))
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
            SearchTarget.Kind.SPOTIFY -> Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:$q"))
        }
        return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).also {
            if (target.activity != null) it.setClassName(target.packageName, target.activity) else it.setPackage(target.packageName)
        }
    }
}

/** The target's place in your search choices: its kind and its app. */
val SearchTarget.key: String get() = "${kind.name.lowercase()}:$packageName"

/**
 * What the search offers after apps, and in what order. With nothing chosen, only one web search (the
 * first found), your files, refuge notes, the calendar, alarms and timers, and settings pages are on;
 * Wikipedia, maps, dictionaries, Spotify, contacts, Obsidian notes and a folder of files are listed, off
 * until you switch them on. Your choices, once made, keep their order; anything new joins at the end.
 */
object SearchOrder {
    const val REFUGE_NOTES = "note:refuge"
    const val CALENDAR = "calendar"
    const val CONTACTS = "contacts"
    const val ALARM = "alarm"
    const val TIMER = "timer"
    const val SETTINGS = "settings"
    const val OBSIDIAN = "obsidian"
    const val FILES = "files"

    fun noteKey(target: NoteTarget) = "note:${target.uri}"

    /** [obsidian]: whether Obsidian is on the phone, so its notes can be offered. */
    fun defaults(targets: List<SearchTarget>, notes: List<NoteTarget>, obsidian: Boolean = false): List<SearchChoice> {
        val ordered = SearchTarget.Kind.entries.flatMap { kind -> targets.filter { it.kind == kind }.distinctBy { it.packageName } }
        val firsts = ordered.groupBy { it.kind }.mapValues { (_, same) -> same.first().key }
        val on = { t: SearchTarget -> t.kind == SearchTarget.Kind.WEB && firsts[t.kind] == t.key }
        return onFirst(
            ordered.map { SearchChoice(it.key, on(it)) } +
                notes.map { SearchChoice(noteKey(it), true) } +
                SearchChoice(REFUGE_NOTES, true) +
                SearchChoice(CALENDAR, true) +
                SearchChoice(CONTACTS, false) +
                SearchChoice(ALARM, true) +
                SearchChoice(TIMER, true) +
                SearchChoice(SETTINGS, true) +
                listOfNotNull(SearchChoice(OBSIDIAN, false).takeIf { obsidian }) +
                SearchChoice(FILES, false),
        )
    }

    fun arrange(defaults: List<SearchChoice>, saved: List<SearchChoice>): List<SearchChoice> {
        val present = defaults.map { it.key }.toSet()
        val known = saved.map { it.key }.toSet()
        return onFirst(saved.filter { it.key in present } + defaults.filter { it.key !in known })
    }

    /** Those switched on first, in their order, then those switched off. */
    fun onFirst(choices: List<SearchChoice>): List<SearchChoice> = choices.filter { it.on } + choices.filterNot { it.on }

    /**
     * [key] switched [on], and moved to where that belongs: switched on, to the end of those on;
     * switched off, to the top of those off. Both are the same place, just past the last one on.
     */
    fun switched(choices: List<SearchChoice>, key: String, on: Boolean): List<SearchChoice> {
        val item = choices.firstOrNull { it.key == key }?.copy(on = on) ?: return choices
        val rest = onFirst(choices.filterNot { it.key == key })
        val at = rest.count { it.on }
        return rest.take(at) + item + rest.drop(at)
    }

    /** The one at [from] put at [to], the rest closing up around it. */
    fun move(choices: List<SearchChoice>, from: Int, to: Int): List<SearchChoice> {
        if (from !in choices.indices || to !in choices.indices) return choices
        return choices.toMutableList().apply { add(to, removeAt(from)) }
    }

    /** [choices] with [key] moved one place up (or down), staying put at either end. */
    fun moved(choices: List<SearchChoice>, key: String, up: Boolean): List<SearchChoice> {
        val i = choices.indexOfFirst { it.key == key }
        val j = if (up) i - 1 else i + 1
        if (i < 0 || j !in choices.indices) return choices
        return choices.toMutableList().also { val a = it[i]; it[i] = it[j]; it[j] = a }
    }
}
