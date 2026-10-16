package app.bedtime.data

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat

/** One update, in a few lines written for someone using the app rather than reading its code. */
data class ChangelogEntry(val versionCode: Int, val versionName: String, val lines: List<String>)

/**
 * What changed, shown once on home after an update. Add an entry with each version worth telling
 * people about; versions without one simply show nothing.
 */
object Changelog {
    val entries: List<ChangelogEntry> = listOf(
        ChangelogEntry(
            37,
            "0.8.2",
            listOf(
                "Every page now starts the same way: a large, light title on the same soft green fade.",
                "In light mode that fade is a pale green, instead of the dark band some pages had.",
                "The full screens in front of an app — blocked, paused, out of time — share one calm layout.",
                "A limit's page looks like a schedule's: tap its name to rename it.",
            ),
        ),
        ChangelogEntry(
            36,
            "0.8.1",
            listOf(
                "Your week is back on home, as one quiet line at the foot. Tap it for the full picture.",
                "The days are now bars: how long you spent in refuge each day, green when it held. Tap one for its figures.",
                "Unlocking can ask how long you need: pick a few minutes, or the rest of it, then do the steps. Set it under \"how hard to leave\", or \"when it runs out\" for a limit.",
                "A limit's pause can let you into all its apps at once, or only the one you opened.",
            ),
        ),
        ChangelogEntry(
            35,
            "0.8.0",
            listOf(
                "A new limits tab: a few breaths before the apps that pull hardest, and a daily budget for them if you like.",
                "Your refuge, on the same tab: the week in figures, four weeks of days, and emergency breaks on a line of their own.",
                "Started a block by mistake? For ten seconds there's an undo, on home and in the notification. It doesn't count against you.",
                "The wait before unlocking now breathes with you: in for four, out for six.",
            ),
        ),
    )

    /** The newest entry you haven't seen yet, if any. */
    fun unseen(lastSeen: Int, installed: Int): ChangelogEntry? =
        entries.filter { it.versionCode in (lastSeen + 1)..installed }.maxByOrNull { it.versionCode }

    /** The installed app's versionCode. */
    fun installedVersion(context: Context): Int =
        runCatching {
            PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0)).toInt()
        }.getOrDefault(0)
}
