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
            56,
            "0.9.10",
            listOf(
                "Widgets: pick and order what's on your home screen and lock screen, each its own (greeting, clock, date, weather, next event, hours, quote). Hiding the clock is now a switch.",
                "Weather can show just the five days, without \"now\".",
                "Search settings list only what you've added; \"Add to the search\" offers the rest.",
                "Up to three messaging apps beside contacts: message, WhatsApp, Signal, Telegram. \"@anna running late\" opens the chat with it typed (copied for Signal and Telegram).",
                "A calculator in the search: \"12 * 3\" shows the answer; a tap copies it.",
                "Limits' pause uses the same rows as a block's steps, with \"harder each time\" by how much you choose.",
                "Schedules and blocks can carry their own mark on the session pill.",
            ),
        ),
        ChangelogEntry(
            55,
            "0.9.9",
            listOf(
                "Alarms and timers each have their own keyword in the search: \"a 7:30\", \"t 10 min\".",
                "The search shows what it found first, then a line, then what it can do with your words, so a tap meant for one can't land on the other.",
                "File search can look in several folders.",
                "Notes: Enter carries a list, to-do or numbered list on, new keys indent and outdent, and H steps through three heading sizes.",
                "The refuge mark can sit at the bottom left of the minimal screen, and the arrow to all apps can be hidden.",
                "A lock-screen gesture can turn the screen off.",
                "The emergency screen no longer lists always-available apps; they're in the full list.",
            ),
        ),
        ChangelogEntry(
            54,
            "0.9.8",
            listOf(
                "Keywords in the search: \"m cafe\" goes straight to maps, \"@anna\" to contacts, and an app's keyword opens it on its own. Change them in Search settings.",
                "The search can set alarms and timers (\"alarm 7:30\", \"timer 10 min\"), open settings pages, search in Spotify, and find Obsidian notes and files by name in a folder you choose.",
                "Limits: the pause before opening can be a typing challenge, and it can grow with each open today.",
                "Lock-screen notifications can come from people you choose, not only apps.",
                "Pulling down from the very top of the lock screen opens the notification shade.",
            ),
        ),
        ChangelogEntry(
            53,
            "0.9.7",
            listOf(
                "Tap the hour dots for your day so far: time in distracting apps and on the phone at all, against yesterday; the heaviest hour; the distracting apps you used most; and every hour as a bar.",
            ),
        ),
        ChangelogEntry(
            52,
            "0.9.6",
            listOf(
                "A double tap on the lock screen, and every lock-screen gesture can now write a quick note or show notifications too.",
                "On the minimal screen, the right side beside your apps is free for a double tap.",
                "Search suggestions are as large as the apps in the full list.",
            ),
        ),
        ChangelogEntry(
            51,
            "0.9.5",
            listOf(
                "Drag the search's choices into your order; switched off, one moves below the rest.",
                "Contacts in the search, if you switch them on: call, message or WhatsApp.",
                "An hour can also count as distracted after a while on the phone at all, and the dots can take your colours.",
                "Hide Android's status bar on the minimal and lock screens, under what's shown.",
            ),
        ),
        ChangelogEntry(
            50,
            "0.9.4",
            listOf(
                "Today by the hour: 24 dots on home, lit when you spent more than a few minutes in apps you call distracting. Choose them in settings.",
                "Gestures on the minimal screen: swipe down, left, right or double tap to open an app, write a quick note, see notifications, lock the phone, and more.",
                "Choose what the search offers, and in what order. Wikipedia only through its own app.",
                "Your notes from the home tab; more or less space above the clock; the lock screen outside sessions too, if digital refuge is your launcher.",
                "The launcher choice now lives in setup, and uninstalling from a long press works.",
            ),
        ),
        ChangelogEntry(
            49,
            "0.9.3",
            listOf(
                "digital refuge can be your home screen, if you want: settings › use as your home screen. The same calm screen all day, with your favourite apps, narrowed during sessions.",
                "Hold an app for its shortcuts, to add it to your home screen, rename it, hide it, or see its info.",
                "Settings are grouped by place: your home screen, your lock screen, your sessions.",
                "Covers now show a different message each time by default.",
            ),
        ),
        ChangelogEntry(
            48,
            "0.9.2",
            listOf(
                "The weather sits beside the clock, with new drawings of the sky; its words, today's range or the next days end the date line. A tap opens Breezy Weather.",
                "Weather for five days, on a line under the date, and a choice of °C or °F.",
                "Drag a notification down to read it, and the summary line down to see them all, without the arrows.",
                "While you write a reply on the lock screen, the keyboard has the room to itself.",
                "Search results are as large as your app names.",
            ),
        ),
        ChangelogEntry(
            47,
            "0.9.1",
            listOf(
                "Quick notes can go to today's daily note in Obsidian, made if it isn't there yet. Each file chooses how notes land: a list item, a to-do or a plain line, dated or not.",
                "Notes show their Markdown as you type, with brackets, bold, italic, headings, lists and to-dos a tap away.",
                "Weather can show today's range, or three days with their skies.",
                "On the lock screen, the arrow or a drag down each open a notification fully.",
                "The full app list's text follows your minimal home's size.",
            ),
        ),
        ChangelogEntry(
            46,
            "0.9.0",
            listOf(
                "On the lock screen, notifications start as one calm line. Drag one down to read it, reply, mark it as read, keep it for later or quiet it until morning; replies unlock, send and lock again.",
                "Swipe up on the minimal home for all your apps, A–Z, and a search that also looks things up, saves quick notes (to the app or your own files, like Obsidian) and adds events to your calendar.",
                "Tap the calendar line for the week ahead.",
                "Weather after the date, sent by Breezy Weather; this app still never goes online.",
            ),
        ),
        ChangelogEntry(
            45,
            "0.8.8.1",
            listOf(
                "Swipe a notification on the lock screen aside to clear it, as in Android's own shade.",
                "The lock screen's app swipes now start at the top, around the clock. The middle is the notifications', and the bottom still unlocks.",
            ),
        ),
        ChangelogEntry(
            44,
            "0.8.8",
            listOf(
                "Notifications can show on the session lock screen, from the apps you choose: only those the session lets through, and only as much as Android's own lock screen would show.",
                "Tap one to unlock and open it. Switch it on in the minimal home's settings; it asks for notification access.",
            ),
        ),
        ChangelogEntry(
            43,
            "0.8.7",
            listOf(
                "Hold the quote on either home screen to make it a favourite, never see it again, or move on to another. Favourites come round three times as often.",
                "A quotes page, under the minimal home's settings: add your own, or paste many at once, and choose to see the app's and yours, only yours, or only favourites.",
                "Look back at the last ten quotes shown, and keep the one you liked.",
                "The typing challenge draws on the same quotes: your own in, hidden ones out.",
            ),
        ),
        ChangelogEntry(
            41,
            "0.8.6",
            listOf(
                "Minimal mode now lets the recent-apps view through, where your phone shows it as a screen of its own: close stuck or unused apps there. Anything you open from it follows the usual rules.",
                "The calendar line only appears when something is still to come today.",
            ),
        ),
        ChangelogEntry(
            40,
            "0.8.5",
            listOf(
                "Choose the words a cover shows, for each schedule, block and limit: one in particular, a different one each time, or your own.",
                "Your own words can join the mix, or wait to be picked.",
            ),
        ),
        ChangelogEntry(
            39,
            "0.8.4",
            listOf(
                "The minimal home's text is the app's cream again, and you can choose its colour.",
                "The clock has a size of its own now, and can go much smaller.",
                "The bright green is back among the accents, if you miss it. Sage stays the default.",
                "On the lock screen, a swipe from the middle can open an app, or the camera without unlocking.",
                "Today's next event can sit under the date, read from the calendar already on your phone.",
            ),
        ),
        ChangelogEntry(
            38,
            "0.8.3",
            listOf(
                "A calmer sage green throughout, and the app now stays dark whatever your phone is set to.",
                "Gentler buttons where you're asked to pause, and limits that fill like a tide as the day's minutes go.",
                "Choose the minimal home's fonts: one for the clock and apps, one for the quote — or add your own.",
                "A limit can now be changeable only at the hours you pick, like a schedule.",
            ),
        ),
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
