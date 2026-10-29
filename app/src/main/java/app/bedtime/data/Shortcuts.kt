package app.bedtime.data

import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import kotlinx.serialization.Serializable

/** One of an app's own shortcuts, as kept and shown: whose, which, and what it's called. */
@Serializable
data class AppShortcut(val packageName: String, val id: String, val label: String, val app: String)

/**
 * Apps' shortcuts — "Sync now", "New message" — which Android shares only with the phone's default
 * home screen: found by name in the search, and one of them run after a quick note goes into a file.
 */
object Shortcuts {
    /** Every app's shortcuts; none unless refuge is the home screen. */
    fun all(context: Context): List<AppShortcut> {
        val launcher = context.getSystemService(LauncherApps::class.java) ?: return emptyList()
        if (!runCatching { launcher.hasShortcutHostPermission() }.getOrDefault(false)) return emptyList()
        val query = LauncherApps.ShortcutQuery().setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST)
        val labels = mutableMapOf<String, String>()
        return runCatching { launcher.getShortcuts(query, Process.myUserHandle()) }.getOrNull().orEmpty()
            .filter { it.isEnabled }
            .mapNotNull { s ->
                val label = (s.shortLabel ?: s.longLabel)?.toString()?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val app = labels.getOrPut(s.`package`) {
                    runCatching { context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(s.`package`, 0)).toString() }.getOrDefault(s.`package`)
                }
                AppShortcut(s.`package`, s.id, label, app)
            }
            .sortedWith(compareBy({ it.app.lowercase() }, { it.label.lowercase() }))
    }

    /** Those whose name, or whose app's name with it, has [words] in it; from three letters, at most [limit]. */
    fun match(shortcuts: List<AppShortcut>, words: String, limit: Int = 3): List<AppShortcut> {
        val q = words.trim()
        if (q.length < 3) return emptyList()
        return shortcuts.filter { it.label.contains(q, ignoreCase = true) || "${it.app} ${it.label}".contains(q, ignoreCase = true) }
            .sortedBy { if (it.label.startsWith(q, ignoreCase = true)) 0 else 1 }
            .take(limit)
    }

    /** Runs [shortcut]; false if it couldn't be (refuge isn't the home screen, or the app took it away). */
    fun start(context: Context, shortcut: AppShortcut): Boolean =
        Problems.attempt("running ${shortcut.app}'s “${shortcut.label}”") {
            context.getSystemService(LauncherApps::class.java)?.startShortcut(shortcut.packageName, shortcut.id, null, null, Process.myUserHandle())
        } != null
}
