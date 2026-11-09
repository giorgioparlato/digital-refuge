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
            91,
            "0.9.45",
            listOf(
                "The week in insights shows each day's time over its bar, on the phone or in distracting apps, as you switch.",
                "Tap a day of the week to see it against the same day of what you compare with: its time, unlocks and top apps, and how each moved.",
                "A day's unlocks have a small chart of their own under its hours, with how many in each.",
                "The home screen's foot: choose up to three buttons besides unlock and emergency (the refuge mark, all apps, notes, habits, insights) and drag all five into your order; settings › home screen › look.",
                "Unlock and emergency can be icons alone, and the foot's buttons can be spread evenly across the width.",
            ),
        ),
        ChangelogEntry(
            90,
            "0.9.44",
            listOf(
                "Setup is clearer: needed, recommended and optional, with how many are done; each step says what it's for in a line, and done ones keep only a quiet link.",
                "Greyscale is part of the do not disturb step, as it comes with that access. Android's warning for the accessibility switch is explained a tap away.",
                "Info boxes are a calm grey instead of blue.",
            ),
        ),
        ChangelogEntry(
            89,
            "0.9.43",
            listOf(
                "Backups can be sealed with a passphrase: settings › about & backup › backup passphrase. Exports and the weekly backup then can't be read without it, wherever they end up.",
                "The microjournal, its moods and why answers stay out of backups unless you switch on \"microjournal in backups\".",
                "Choosing the weekly backup's folder first says what the file holds, and that a folder which syncs puts a copy there too.",
                "The microjournal's mood chips start folded each time it opens.",
            ),
        ),
        ChangelogEntry(
            88,
            "0.9.42",
            listOf(
                "The habits widget can show what today needs to keep each run going, rather than the habits you chose; on each screen apart, with the done ones kept or gone.",
                "The habits dots can sit under the date or beside the clock, with or without their words.",
                "Habits can be sets of steps, like a workout's exercises: ticked one by one on the habits page, or from a pop-up when you tap the set in the widget.",
                "A credits page in settings › about, with thanks and the open source refuge is built with.",
            ),
        ),
        ChangelogEntry(
            87,
            "0.9.41",
            listOf(
                "The + on focus makes limits too: \"limit\" sits beside schedule and block on the new page.",
                "Tap blocks, limits or schedules on focus to fold them to their count, for a quicker way about the page.",
            ),
        ),
        ChangelogEntry(
            86,
            "0.9.40",
            listOf(
                "Limits live on focus now, after your blocks, with what's left today; schedules come last. The limits tab is gone.",
                "Check-ins have a page of their own in settings: ask why, moods, and the nudge after time in a row; a link at the foot of focus too.",
                "Habits can have a tab of their own, like notes: switch it on at the foot of the habits page.",
                "Hold the notes button on the home screen for a new note, the microjournal, a to-do or a voice note.",
                "The everyday launcher is a choice in settings › home screen, not a step of setup. The greeting is gone.",
            ),
        ),
        ChangelogEntry(
            85,
            "0.9.39",
            listOf(
                "The note beside the clock shows the lines you choose again, and its own background colour.",
                "Setup explains Android's warning for the accessibility switch: what refuge is actually told, what it's for, and that it has no internet access at all.",
                "Small fixes from a check for bugs.",
            ),
        ),
        ChangelogEntry(
            84,
            "0.9.38",
            listOf(
                "Sessions in insights, by what they did: phone-free nights, phone time spared, kept week by week, when and why you break, the hour after, the apps used in them, and blocks' focus. Hours in refuge are one quiet line now.",
                "Notes are saved the moment you leave them, whatever takes you away, so nothing written is lost.",
                "Numbered lists renumber themselves as you add or take out a line.",
                "The note beside the clock is no taller than the clock, as wide as there's room, or the width you choose.",
                "Count habits can be changed for past days: tap the day, then − or +.",
                "Sessions can be switched off in insights; holding the note widget offers the note in full first.",
            ),
        ),
        ChangelogEntry(
            83,
            "0.9.37",
            listOf(
                "Voice notes: where it goes is offered as soon as you start, and choosing never stops it; only \"stop\" does. Not chosen by then, it asks.",
                "The month's colours: green, cream at your budget, and the month's worst day the reddest.",
                "The habits widget can be a row of dots, today's for each; a tap opens them all.",
                "Hold the notes button on focus: a new note, today's microjournal, a to-do or a voice note.",
            ),
        ),
        ChangelogEntry(
            82,
            "0.9.36",
            listOf(
                "Export your notes as Markdown files to a folder (your Obsidian vault, say): a file a note, the microjournal a file a day, recordings as embeds. Nothing comes back from the folder.",
                "Voice notes: stop first, then choose where it goes. A recording is kept safe from the moment you stop; if refuge is closed on the way, it carries on next time and asks again.",
                "The month's colour scale shows times: none, your budget, twice it.",
            ),
        ),
        ChangelogEntry(
            81,
            "0.9.35",
            listOf(
                "Voice notes, if you switch them on: recorded and heard on the phone by Whisper, kept in a folder of yours, the words in the microjournal or a new note. A tap plays it.",
                "Record from the microjournal or a note (it goes there), or from the quick note, the notes tab, a gesture or the app's icon (you choose where when you stop). It keeps recording with the screen off.",
                "A notes tab at refuge's foot if you want one; held, it starts a note, today's microjournal, a to-do or a voice note.",
                "Insights shows habits and moods only if you want them (the ⋮ at the top); switched off, nothing is lost.",
                "The month's heatmap is one smooth scale, green or cream to red, with amber at your budget.",
                "The mood chips fold away to one line while you write.",
            ),
        ),
        ChangelogEntry(
            80,
            "0.9.34",
            listOf(
                "The microjournal is a line at a time, each with its time, the newest at the foot; a mood tapped goes on the line you're on. Prompts wait until you ask, or can go for good.",
                "The microjournal is one more place for a quick note, with how you feel if you like; and a gesture, a widget, and \"add to microjournal\" in the share menu.",
                "Moods: rename yours, add one while writing; in insights, each mood's lines, its week, what came after it, and patterns over eight weeks.",
                "\"How do you feel now?\", where you choose: after a while on the phone, after a schedule's break, when a limit's time open is over.",
                "Limits: \"back within\" is now \"after the pause, open for\": a time to come and go as you like, or until you leave.",
                "Notifications: each swipe does what you choose (clear, later, mark as read, quiet, reply); the summary line swipes all of them; replied ones go.",
                "A day on the month's heatmap shows its time and top apps at a glance.",
            ),
        ),
        ChangelogEntry(
            79,
            "0.9.33",
            listOf(
                "Your answers to \"why\" go into that day's microjournal entry, the newest on top.",
                "Moods are back, yours to change: tap how you feel when answering why, or in the microjournal. Insights shows them on a calendar.",
                "Ask why can need a few words at least, in limits and in schedules; whether a session asks is now in each schedule's settings.",
                "Tap the weather for today's rain hour by hour; hold it for its options and the weather app.",
                "The player shows where it's playing, a tap to switch, and the app's own like button, such as Spotify's.",
                "Your most distracted hours, in your week and under a day's hours, for the day and for what you compare it with.",
                "Count habits line up with the others, and lock-screen notifications swipe away without opening.",
            ),
        ),
        ChangelogEntry(
            78,
            "0.9.32",
            listOf(
                "Ask why now shows after every unlock, however you unlock, with a different question each time and room to write.",
                "Before a limit's apps, \"ask why\" is a pause each limit can choose, with its own questions.",
                "Ask why's questions, and cover messages, are all yours to reword, delete or add to; cover messages have new suggestions.",
                "The quick note's microjournal opens today's entry in your notes.",
                "Dragging to reorder (habits, widgets, search) stays under your finger.",
                "No capitals, anywhere: what refuge writes into your notes too.",
            ),
        ),
        ChangelogEntry(
            77,
            "0.9.31",
            listOf(
                "Ask why: after unlocking, and before distracting apps, a moment to ask yourself why, with tags for how you feel. Off until you switch it on, in settings or from the focus page.",
                "Writing can be an unlock step for schedules, blocks and limits: a few lines, from the microjournal's prompts or \"why?\", typed rather than pasted.",
                "The microjournal's suggested sections are new, each question with an example where it helps.",
                "Notes: search, a trash that keeps deleted notes a month, undo after deleting, undo and redo while writing, pins, labels, moving and merging notes, and reminders.",
                "The to-do: ticked items gather at the end with their date, folded by when they were done.",
                "Share text from any app into refuge; share or copy a note out.",
                "A note added on the lock screen syncs once you unlock; leaving Obsidian can sync too.",
                "Murakami's quotes are gone, and Shelley has joined.",
            ),
        ),
        ChangelogEntry(
            76,
            "0.9.30",
            listOf(
                "A microjournal, if you want one: switch it on in settings › notes. An entry a day in your notes, and from the quick note.",
                "Prompts while you write: pick sections (vent, obligations, mindset, gratitude, ideation, trajectory, after the journalling system by Struthless) and tap a prompt to write from it.",
                "The sections and prompts are only a start: change, delete or add your own.",
                "Insights opens on the month's heatmap, with today outlined: tap it for today.",
                "Turning the screen off from the lock screen goes black at once, Android's bar at the foot too.",
            ),
        ),
        ChangelogEntry(
            75,
            "0.9.29",
            listOf(
                "Insights › phone › month opens with a heatmap: each day against your daily budget, green within it, rose over, red at twice it, the same scale every month.",
                "The whole phone first, distracting apps a tap away; tap a day to open it, Back returns to the month.",
                "No budget chosen yet? The heatmap suggests 1 h on the phone, 30 min distracting, and lets you choose yours right there.",
                "Lock-screen notifications: \"Quiet until…\" offers an hour, three, this evening, tomorrow morning, or the session's end.",
                "Each day's total is now kept with its month, so older months fill in the heatmap as time goes on.",
            ),
        ),
        ChangelogEntry(
            74,
            "0.9.28",
            listOf(
                "The quick note has an × at the top to close it.",
                "On the lock screen, Android's swipe up from the very bottom with the quick note open brings back refuge's lock screen, not Android's.",
                "Turning the screen off from the lock screen notes under recent problems when Android's own lock didn't do it, to see why.",
            ),
        ),
        ChangelogEntry(
            73,
            "0.9.27",
            listOf(
                "The quick note and look-up are one box: type a single word and your dictionaries show above where the note goes; a second word takes them away, and the note works as before.",
                "Entries show three lines at first; \"the whole entry\" opens the rest, and any word in one can still be tapped.",
                "Above where the note goes, when your words say so: a timer, an event for your calendar, a block to start, or a sum to copy. They follow your search settings.",
                "A gesture set to look up opens the same box.",
            ),
        ),
        ChangelogEntry(
            72,
            "0.9.26",
            listOf(
                "A double tap on the clock can do something of its own, on the home and lock screens; until you set it, it's a double tap like anywhere else.",
                "Look up opens at the top of the screen, above the keyboard. Swipe it up or down to put it away.",
                "Type \"timer 10\" in the look-up to start a timer, on the lock screen too.",
                "The search finds a word's meaning in every dictionary, the one you picked first, and with its keyword any word or phrase.",
                "Dictionaries that list word forms find \"running\" under run; add yours again to pick that up.",
                "Search settings show only what you use; the rest waits behind \"Add more\", with icons and a search.",
                "An Obsidian note found on the lock screen now opens that note, not Obsidian's start page.",
            ),
        ),
        ChangelogEntry(
            71,
            "0.9.25",
            listOf(
                "Dictionary entries read better: parts of speech in italics, in the accent colour, examples fainter.",
                "Tap any word in an entry to look it up; synonyms and other references are links. \"‹ back\" takes you back.",
                "With more than one dictionary, choose which to look in, or all.",
                "Dictionaries are kept by their own title, so two can't overwrite each other.",
                "A keyword left behind by an uninstalled app's look-up or search is cleared, and free again.",
                "The calendar the search adds to is chosen from a short menu.",
            ),
        ),
        ChangelogEntry(
            70,
            "0.9.24",
            listOf(
                "Look up: put it on a gesture to find a word in your dictionaries, right on the lock screen. Add StarDict dictionaries under settings › search; bilingual ones translate word by word.",
                "Look up finds your notes too. On the lock screen only by their titles, opening once you unlock.",
                "While a navigation app gives directions, refuge's lock screen steps aside for it.",
                "Every settings page has a search at the top right; on the search page it searches that page, and finds apps to give a keyword.",
                "A day's allowance can be any amount. Over it, the bar shows how far beyond.",
                "Days compare with a year back last, after the averages.",
                "An uninstalled app's keyword and name go with it, and voice messages no longer show in the player.",
            ),
        ),
        ChangelogEntry(
            69,
            "0.9.23",
            listOf(
                "Apps' own shortcuts in the search: type \"sync\" for a sync app's \"Sync now\".",
                "After a quick note goes into one of your files, refuge can run a shortcut too, under notes, e.g. a sync app's \"Sync now\".",
                "A day can be set against your 7-day or 30-day average, this year's average day, or a day you pick.",
                "Your week shows from Sunday evening to the end of Monday, with a × to put it away. Its Sunday note is under lock-screen notifications.",
            ),
        ),
        ChangelogEntry(
            68,
            "0.9.22",
            listOf(
                "Fixed: the lock screen closed the app as it opened, on newer Android versions, which also sent Home to your other launcher for a moment.",
                "The rain alert can come at any time you choose.",
                "A long quick note no longer pushes where it goes off the screen.",
            ),
        ),
        ChangelogEntry(
            67,
            "0.9.21",
            listOf(
                "Circles on the lock screen: up to three, like close and known, each with its people, its group chats and a colour. Theirs come first, from any app, and a schedule can let just one circle through.",
                "A rain alert in the morning, under lock-screen notifications: how likely rain is today, and when it's likeliest, from Breezy Weather.",
                "Backups now keep your phone use too.",
                "Recent problems, under about & backup: what went wrong lately, to copy into a message.",
                "Easier on the battery: the home and lock screens redraw once a minute, the lock screen notices unlocking without checking all the time, and day-to-day data is kept apart from your settings.",
                "An emergency break carries on if the app is restarted.",
            ),
        ),
        ChangelogEntry(
            66,
            "0.9.20",
            listOf(
                "Settings, one row per place: home screen, lock screen, search, apps, notes, cover messages, shortcuts, and about & backup. Every setting lives in one of them, and the search finds any.",
                "Each widget has one set of options, in the same order everywhere: what it shows, its size, its colours, what a tap does. Holding it on your screen shows the same ones.",
                "The app list: favourites in your order, hidden apps, your own names and keywords, in one place.",
                "Cover messages have a page of their own. The nudge after time in a row is on the limits tab, and the calendar the search adds to is on the search page.",
                "Limits can count opens: so many a day, shared or each.",
                "Your week, at the top of insights, and if you like a note about it on Sunday evening.",
                "A weekly backup to a folder you choose, the newest four kept.",
                "Fixed: resetting the look no longer resets your widgets and gestures; leaving early is never lost if the screen closes; the session's notification no longer refreshes every second.",
            ),
        ),
        ChangelogEntry(
            65,
            "0.9.19",
            listOf(
                "New tabs: focus (your blocks and schedules, and what's on now), limits, insights and settings. Your notes are a tap away at the top of focus.",
                "Insights: your phone by day, week or month, against the period before, your usual, or a year back; unlocks by the hour; how your sessions held; and your habits.",
                "Habits: hold one and drag it to reorder, add one with the round button, and see the week as plain figures.",
                "Settings are sorted by screen: home screen, lock screen, widgets, search, apps and notes.",
                "The lock screen has its own look and widget options: colours, fonts, sizes, the clock, the note, the quote.",
                "Widgets open their options in place; the ones not shown sit apart, a tap from showing.",
                "Search: each action is in every search, only with its keyword, or off, sorted into find, do and look up, each of which you can switch off. Try it right on the page.",
                "The budget bar opens today by app, and the whole phone's budget can start at 30 minutes.",
                "The quote stays on the lock screen; notifications slide over it. A running block no longer shows as a timer.",
                "The nudge after time in a row should now start reliably, however you unlock.",
            ),
        ),
        ChangelogEntry(
            64,
            "0.9.18",
            listOf(
                "Replying from the lock screen, the widgets step aside: you see the conversation and what you type.",
                "The distraction tracker can be a line, a daily budget (for distracting apps or the whole phone), or your top apps, and can show your unlocks. refuge counts them itself.",
                "A running timer counts down on the lock screen, from the search or the Clock app; swipe it away.",
                "Statistics go month by month too.",
                "An optional nudge after so many minutes in a row on the phone.",
                "The note widget: text size, as few as 1 line, fewer card colours, dark or light text, and \"Open this note in full\" from a long press.",
                "The next event can show tomorrow's first one once today's are done.",
                "Quick notes show where they go: refuge, Obsidian or a file.",
                "Emergency is on your everyday home too while a session runs.",
            ),
        ),
        ChangelogEntry(
            63,
            "0.9.17",
            listOf(
                "Swipe the player away, or close it; it comes back with the next track.",
                "To-do boxes look like boxes: soft squares, filled with a tick when done, as you write and on your screens.",
                "Hold an empty spot on your home or lock screen to switch its widgets on or off, or go to its settings.",
                "The note beside the clock has a column of its own on the right.",
                "The note widget can have a background colour of its own.",
                "The weather's style, and which habits the widget shows, can differ between the home and lock screen.",
                "The home tab's line shows your time in distracting apps this week.",
            ),
        ),
        ChangelogEntry(
            62,
            "0.9.16",
            listOf(
                "Tap a note on your home or lock screen to write in it, title included; the note beside the clock too.",
                "\"Beside the clock\" and the calendar icon are set for the home and lock screen apart.",
                "A habit's popup leads to all your habits.",
                "The quote and the other widgets get their space above them again.",
            ),
        ),
        ChangelogEntry(
            61,
            "0.9.15",
            listOf(
                "The note widget is quieter and saves as you type: tap its lines to write right there, Enter for a new line; close the keyboard or go back to stop. Choose 3, 6 or 10 lines.",
                "With a small clock, the note can sit beside it, top right.",
                "Notes save as you type in the notes editor too.",
                "An optional notes button at the bottom right of the home screen.",
                "Tap the player to open its app; tap the distraction tracker for today's detail, on the lock screen too, with a way to your statistics.",
                "On the lock screen, tap the next event for the week ahead; with nothing coming, a calendar icon does the same.",
                "Notifications on the lock screen get the room the quote took at the foot.",
                "Space at the top can differ between the home and lock screen; widgets have a little more room between them.",
                "The status bar, app icons, the mark, the arrow and the notes button are now under Look.",
            ),
        ),
        ChangelogEntry(
            60,
            "0.9.14",
            listOf(
                "Settings, arranged by what they're for, with a search at the top: type \"keyword\", \"weather\" or \"colour\" to go straight there.",
                "Settings now say plainly that refuge's home screen takes over in minimal sessions and can be your everyday launcher, and that its lock screen is up to you.",
                "Widgets: pick the home screen or the lock screen at the top of the page.",
                "A note widget: one of your refuge notes on the home or lock screen, with boxes to tick and a line to add. Hold it to choose which note.",
                "Hold a widget on your home or lock screen to hide it, or change it there: the weather's style, the clock's size, the quote, your habits.",
                "A search keyword that's already taken now says what has it, and isn't set twice.",
                "\"Today by the hour\" is now the distraction tracker.",
            ),
        ),
        ChangelogEntry(
            59,
            "0.9.13",
            listOf(
                "Schedules and blocks can bring their own widgets to your home and lock screen while they run (\"On the screens\" in each).",
                "Start a block from the minimal screen: hold the refuge mark, use a gesture, or type its name in the search (\"b\" for short).",
                "Your blocks also appear on digital refuge's icon: long-press it in any launcher and tap one to start it.",
            ),
        ),
        ChangelogEntry(
            58,
            "0.9.12",
            listOf(
                "Habits: daily, every few days, a few times a week or month, or a count like pages or reps, each in its own colour. Tick them from the home tab, Settings → Habits, or a widget on your home and lock screen.",
                "A to-do note, kept at the top of your notes; add to it from the search (\"td\") or a quick note.",
                "Notes show boxes, bullets and indents as you write; tap a box to tick it.",
                "A \"now playing\" widget with play, pause and skip.",
                "Each schedule can choose what refuge's lock screen shows: as usual, only some apps and people, or nothing.",
                "Tap an event in the week ahead for its details, and open it in your calendar.",
                "Simpler gesture choices, with a search for apps; the screen-off gesture now works on phones that ignored it.",
                "Your own colour for the hour dots, and for each habit.",
            ),
        ),
        ChangelogEntry(
            57,
            "0.9.11",
            listOf(
                "Statistics are now about your days on the phone: today, two weeks day by day, apps by time and opens, the hours you use it most, and this week against last. Time in refuge sits lower down.",
                "With usage access (asked on the statistics page), every app, your unlocks and the days before refuge are counted, from Android's own record. It stays on the phone.",
                "The hour dots still need nothing extra; with usage access they read the same record, so the numbers always agree.",
            ),
        ),
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
