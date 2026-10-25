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

    /** The ones whose app is in [openable]: one each for web, Wikipedia and maps, and every dictionary. */
    fun offered(found: List<SearchTarget>, openable: Set<String>): List<SearchTarget> {
        val usable = found.filter { it.packageName in openable }
        val firsts = listOf(SearchTarget.Kind.WEB, SearchTarget.Kind.WIKIPEDIA, SearchTarget.Kind.MAPS)
            .mapNotNull { kind -> usable.firstOrNull { it.kind == kind } }
        return firsts + usable.filter { it.kind == SearchTarget.Kind.LOOKUP }.distinctBy { it.packageName }
    }

    /** Every app that could take a search, of each kind; the Wikipedia app before browsers. */
    fun find(context: Context): List<SearchTarget> {
        val pm = context.packageManager
        fun handlers(intent: Intent) = runCatching { pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY) }.getOrDefault(emptyList())
        fun label(pkg: String) = runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault(pkg)
        val browsers = handlers(Intent(Intent.ACTION_VIEW, Uri.parse("https://wikipedia.org"))).map { it.activityInfo.packageName }
        val wikipedia = listOfNotNull(WIKIPEDIA_APP.takeIf { runCatching { pm.getApplicationInfo(it, 0) }.isSuccess }) + browsers
        return buildList {
            handlers(Intent(Intent.ACTION_WEB_SEARCH)).forEach {
                add(SearchTarget(SearchTarget.Kind.WEB, it.activityInfo.packageName, label(it.activityInfo.packageName), it.activityInfo.name))
            }
            // Without a web-search screen, a browser can still search through its address bar.
            browsers.forEach { add(SearchTarget(SearchTarget.Kind.WEB, it, label(it))) }
            wikipedia.distinct().forEach { add(SearchTarget(SearchTarget.Kind.WIKIPEDIA, it, label(it))) }
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
