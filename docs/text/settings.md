# settings — the fourth tab

Every screen this tab leads to, in the order you reach them. The tree comes first; below it, each place
lists the words on it, in the order they appear in the code.

- **Placeholders:** `{name}` is filled in by the app (a block's name, a number…), and `…` is a value worked out
  there and then.
- **Case:** the app shows everything lowercase, whatever is written here.
- **Variants:** some lines are alternatives (on / off, one / many); they're listed one after another.
- **Editing:** edit any line in place and tell me when you're done. I'll carry the changes into the app. This file
  is written from the code, so after that I'll write it again.

## the tree

```
settings
├── find a setting (searches every page below)
├── permissions & setup ── 8 steps: blocking · greyscale · do not disturb · the lotus · full-screen reminder ·
│                          always-available apps · battery · your everyday launcher
├── home screen
│   ├── everyday launcher ──── (set in permissions & setup)
│   ├── look: colours · sizes · fonts · show
│   ├── widgets: each widget's options · not shown
│   ├── gestures
│   └── the app list: favourites · hidden apps · your own names · keywords · sizes
├── lock screen
│   ├── in sessions · outside sessions too
│   ├── look · widgets (as on the home screen, its own)
│   ├── notifications: from these apps · quick replies · circles ── a circle: name · colour · people · groups
│   └── gestures
├── search: what it offers · keywords · beside each contact · adding to your calendar · folders · try it
├── apps: app groups · always-available apps · distracting apps
├── notes: refuge notes · your files · today's daily note
├── cover messages: yours · the app's own
├── shortcuts: home-screen widget · quick-settings tile
└── about & backup: how it works · the intro again · export · import · recent problems · weekly backup ·
                    back up now
```

## settings, and the pages it opens

Home screen, lock screen, apps, shortcuts and about & backup are lists of rows from this one file.

<sub>[SettingsScreen.kt](../../app/src/main/java/app/bedtime/ui/settings/SettingsScreen.kt)</sub>

- settings saved to the file
- couldn't write that file
- backed up
- couldn't write to that folder; choose it again
- that folder can't be written to from here
- Import settings?
- This replaces your schedules, blocks and settings with the ones in the file. Running sessions aren't affected.
- that file isn't a digital refuge backup
- settings restored
- Replace
- Cancel
- set up once, then left alone
- Nothing by that name.
- digital refuge {version} · everything stays on this phone
- Permissions & setup
- All set
- 1 step left
- {stepsLeft} steps left
- permissions setup accessibility greyscale dnd do not disturb battery overlay notifications launcher
- Home screen
- Your launcher
- In minimal sessions
-  · look, widgets, gestures, the app list
- home screen minimal launcher
- Lock screen
- All the time
- In sessions
- Off
-  · look, widgets, notifications, gestures
- lock screen
- Search
- What it offers, keywords, contacts, folders, the calendar
- search keyword keywords web maps contacts messaging whatsapp signal telegram alarm timer calculator settings pages files folder obsidian spotify blocks calendar events
- Cover messages
- What a blocked app's cover says
- The app's and … of yours
- cover message messages words blocked
- Everyday launcher
- Off · only in minimal sessions. Switched on in setup
- On · refuge's home screen all the time. Changed in setup
- On, but not your home app yet · choose it in setup
- launcher home screen default home app everyday
- The app list
- Favourites, hidden apps, your own names, keywords, sizes
- favourites favorites hidden hide apps rename names keyword icons app names size list
- While a schedule or block runs
- lock screen sessions
- Outside sessions too
- All the time, as your launcher's
- Only when refuge is your everyday launcher
- lock screen always launcher
- Notifications
- From the apps and people you choose
- notifications messages reply replies people contacts circles groups lock screen rain alert weather
- App groups
- Tick many apps at once when choosing
- 1 group
- … groups
- groups apps
- Always-available apps
- None yet · maps, rides, authenticators…
- 1 app, never blocked, whichever session is on
- … apps, never blocked, whichever session is on
- always available emergency maps authenticator
- Distracting apps
- None yet
- … apps
-  · what counts as a distracted hour
- distraction distracting tracker hours dots
- Export settings
- Everything you set, with your history, notes, habits and phone use, in one file
- export backup save file
- Import settings
- Replace everything with a saved file
- import restore backup file
- Weekly backup
- Off · the same file as export, once a week, in a folder you choose; the newest … kept
- Can't write to … any more · tap to choose it again
- To … · none written yet
- To … · last on …
- backup automatic weekly folder save
- Back up now
- Write this week's file straight away
- backup now
- widget widgets weather calendar event date greeting clock clock size hours distraction tracker budget unlocks habits quote quotes now playing music note colours
- home-screen widget
- A one-tap button on your home screen that starts a block. Add one for:
- Long-press your home screen → widgets → digital refuge, then pick a block.
- Create a focus block first.
- Add
- quick settings tile
- Edit your quick settings and add the “refuge” tile. Tapping it starts:
- Find a setting: keyword, weather, colour…

## permissions & setup

<sub>[SetupScreen.kt](../../app/src/main/java/app/bedtime/ui/setup/SetupScreen.kt)</sub>

- Setup
- A few one-time steps so digital refuge can do its job. Nothing ever leaves your phone.
- Let digital refuge see which app is open
- Required
- This is what notices when a blocked app opens. It only sees which app is in front: it can't read what's on your screen or what you type.
- Android hides this switch for apps installed outside the Play Store, so there are two parts. Do them in this order:
- Tap “Allow restricted settings” below. In App info, tap ⋮ in the top-right corner, then “Allow restricted settings”. If that menu item isn't there, skip to step 2 — your phone doesn't need it.
- Tap “Open accessibility settings” below, find digital refuge blocker (under “Downloaded apps” on most phones), open it and switch it on. Confirm the permission Android asks about.
- 1 · Allow restricted settings
- 2 · Open accessibility settings
- Switch still greyed out?
- Go back to step 1 and check the ⋮ menu again: the option only appears once, and it disappears after you've used it. After a reinstall you have to do it again.
- Open accessibility settings
- Allow greyscale
- Optional
- Greyscale works through Do Not Disturb access (step 3): allow that, and sessions can fade apps to black and white.
- Greyscale needs Android 15 or newer, where it works through Do Not Disturb access. On older versions Android doesn't let apps switch it on.
- Previewing…
- Try it for 5 seconds
- Allow Do Not Disturb
- Lets schedules and blocks silence your phone and hold notifications back until they end. Find digital refuge in the list and switch it on.
- Open Do Not Disturb access
- Show a lotus while a session runs
- A quiet notification puts a small lotus in the status bar during schedules and blocks, with the time left. It never makes a sound.
- Allow notifications
- Allow the full-screen reminder
- If blocking is ever switched off while a session is running, digital refuge fills the screen until you switch it back on. That needs permission to display over other apps.
- Allow display over other apps
- Choose always-available apps
- Recommended
- Apps that stay usable during every session, reached from the emergency button: maps, rides, your authenticator. Keep the list short. You can change it later in settings → apps.
- Choose apps
- Edit · …
- Keep digital refuge running
- Some phones (Samsung, Xiaomi, OnePlus…) put background apps to sleep. If blocking ever stops working, set digital refuge's battery usage to Unrestricted.
- Open battery settings
- Use as your default launcher
- The minimal screen all the time, with your favourite apps; otherwise it's only there during the refuge sessions you choose. Switch it off any time and your previous launcher is back.
- Use as default launcher
- Not your home app yet.
- Choose it as your home app
- Stop using it
- Pressing Home brings you to the minimal screen.
- Stop using it as launcher
- Done

## look, widgets, gestures and lock-screen notifications

One file serves the look, widgets, gestures and notifications pages, for both screens.

<sub>[HomeStyleScreen.kt](../../app/src/main/java/app/bedtime/ui/homestyle/HomeStyleScreen.kt)</sub>

- calendar access wasn't allowed
- that file isn't a font this phone can read
- Notifications from
- allow notification access for digital refuge
- Circle …
- Home screen
- Lock screen
- Bedtime
- Background
- Accent
- Text
- Automatic is cream on a dark background, ink on a light one
- Automatic
- Sizes
- Space at the top
- None
- Small
- Medium
- Large
- The clock's size is in its widget's options; app names are with the app list.
- The clock's size is in its widget's options.
- Fonts
- Two choices: one for the clock, date and apps, one for the quote
- Clock, date and apps
- Quote
- Serifs set the quote in italics. Your own font is a .ttf or .otf file; it stays on this phone and isn't part of a backup.
- Reset to default
- Show
- Android's status bar
- On both screens. Off hides it; a swipe down from the top edge shows it
- The refuge mark
- Bottom left; a tap opens digital refuge
- The arrow to all apps
- At the foot; swiping up works either way
- A notes button
- Bottom right; a tap opens refuge notes
- Last heard …; hidden when over 3 hours old.
- On the home screen
- On the lock screen
- Tap one for its options; drag by the handle to change the order
- Notifications
- Notification access is off, so none can show. Switch this on again to allow it
- Drag one down to read it; tap to unlock and open it, or swipe it aside to clear it
- From these apps
- None yet. Pick the apps you want to hear from
-  and … more
- Quick replies
- … ready to send with a tap. Typing your own works too
- Rain today
- From Breezy Weather's forecast (see the weather widget's options to set it up)
- A rain alert
- In the morning, when rain is likely today: how likely, and when
- At
- When the chance is at least
- Circles
- People and group chats whose notifications show from any app, first, in their colour
- … people
- 1 group
- {it} groups
- Nobody yet
- Add a circle
- Swipe up
- All apps and search
- Open {it}
- Nothing
- Just unlocks
- Camera
- Write a quick note
- Show notifications
- Turn the screen off
- Look
- Colours, sizes and fonts, each screen its own. Changes save as you go.
- Widgets
- What your home screen and lock screen show, each its own, in the order you like. Pick the screen here first. Changes save as you go.
- Lock-screen gestures
- Swipes start around the clock; a double tap is anywhere. Each can open an app once you've unlocked, or straight away the camera, a quick note or your notifications. The middle is for swiping notifications away; from the bottom, any swipe just unlocks.
- Gestures
- On your home screen, and the minimal one during sessions. Swipes start anywhere but the very top edge, which stays Android's; a double tap is on an empty spot.
- Lock-screen notifications
- Those the session lets through, from the apps you choose, as private as Android's own lock screen.
- Swipe down
- Swipe left
- Swipe right
- Double tap
- … does
- Open an app
- Search your apps
- Back
- Cancel
- Swipe …
- Nothing, it just unlocks
- Camera, without unlocking
- Write a quick note, without unlocking
- Open an app, once unlocked
- + your own font
- + another
- Custom
- Custom colour (#RRGGBB)
- just now
- {minutes} min ago
- … h ago
- … days ago
- Nothing shown yet; add one below.
- Not shown
- Dots
- Line
- Budget
- Apps
- Apps line
-  in a small line under the dots
- Time on the phone
- All of today, in any app{under}
- Time in distracting apps
- Today so far{under}
- Unlocks
- How many times today; refuge counts them itself, so nothing more is asked
- The budget counts
- Distracting apps
- The whole phone
- A day's allowance

<sub>[WidgetOptions.kt](../../app/src/main/java/app/bedtime/ui/homestyle/WidgetOptions.kt)</sub>

- Size
- Very small
- Small
- Medium
- Large
- From Breezy Weather, a free app that does the fetching; this one never goes online. 
- Nothing from it yet.
- Show
- Today
- 3 days
- Now + 5
- 5 days
- In
- Automatic
- Install Breezy Weather (it's on F-Droid). In its settings, under External modules, open “Send Gadgetbridge data” and tick digital refuge. It sends the weather each time it updates.
- Read from the calendar already on this phone. Nothing is fetched.
- Tomorrow's first event, once today's are done
- In the evening, what the morning starts with
- Event names
- Off, the lock screen only says when the next one is
- A calendar icon when nothing's coming
- A tap on it, or on the event, shows the week ahead
- A new one every
- Hour
- 6 hours
- Day
- Week
- Tap it for another; hold it to favourite or hide it.
- A note
- No notes yet. Write one in refuge notes.
- Lines shown
- Beside the clock
- Small, at the top right: its title and first lines
- Only with a small or very small clock, and the weather not beside it (off, or set to 5 days)
- Text size
- Its background
- The screen's own
- Its text
- To suit it
- Dark
- Light
- Tap its lines to write right there.
- Look
- Dots
- Line
- Budget
- Apps
- Apps in a line
- Time on the phone
- All of today, in any app
- Time in distracting apps
- Today so far
- Unlocks
- How many times today; refuge counts them itself
- The budget counts
- Distracting apps
- The whole phone
- A day's allowance
- Calm hours
- Distracted hours
- Accent
- Tap it for today's detail.
- No habits yet.

<sub>[CircleScreen.kt](../../app/src/main/java/app/bedtime/ui/settings/CircleScreen.kt)</sub>

- contacts access wasn't allowed
- A circle
- Their notifications show on the lock screen whatever app they come from, above the rest, marked in the circle's colour.
- Its name
- Its colour
- Accent
- People
- From your contacts, in any app: messages, calls, mail
- Choose people
- Tap to change
- Group chats
- By the group's name, as the app shows it. Someone in this circle writing in another group doesn't bring that group in
- On your lock screen now
- + {it}
- A group's name
- Add
- Delete this circle

<sub>[PeoplePickerScreen.kt](../../app/src/main/java/app/bedtime/ui/settings/PeoplePickerScreen.kt)</sub>

- Done
- Search your contacts
- No contacts to choose from, or contacts access wasn't allowed.

## quotes (from the quote widget)

<sub>[QuotesScreen.kt](../../app/src/main/java/app/bedtime/ui/quotes/QuotesScreen.kt)</sub>

- Quotes
- Changes save as you go
- Which quotes
- App's and yours
- Only yours
- Only favourites
- Recently shown
- don't show again
- Yours
- None yet. Add one below, or paste many at once.
- Add one
- Edit
- Paste many
- One a line, each can end in — author, where it's from
- Add
- Add 1 quote
- Add … quotes
- Quote
- Author (optional)
- Where it's from (optional)
- Save
- Cancel
- The app's quotes
- All
- Favourites
- Hidden
- No favourites among these yet.
- Nothing hidden.
- You have no quotes of your own showing yet, so the app's and yours are rotating.
- You have no favourites yet, so the app's and yours are rotating. Hold a quote or tap ♡ to add one.
- — {it}
- no longer a favourite
- Show again

## the app list

<sub>[AppListScreen.kt](../../app/src/main/java/app/bedtime/ui/settings/AppListScreen.kt)</sub>

- Favourites
- Hidden from the lists
- The app list
- On your home screen and in its search. Holding an app there changes these too: it's where you rename one or give it a keyword.
- On the home screen outside sessions; drag by the handle to change the order
- take … off
- None yet.
- Choose favourites
- Hidden apps
- None · they still open, and still follow sessions
- … left out of the lists and search
- Your own names
- Used on the home screen and in its lists; × goes back to the app's own
- None yet. Hold an app on the home screen to rename it.
- Keywords for apps
- Typed alone in the search, they open the app
- None yet. Hold an app on the home screen to give it one.
- Sizes
- App names
- Very small
- Small
- Medium
- Large
- Full app list and search
- App icons
- Small icons next to app names
- remove {title}

<sub>[AppPickerScreen.kt](../../app/src/main/java/app/bedtime/ui/apps/AppPickerScreen.kt)</sub>

- Done
- Done · …
- Search apps
- No apps match “…”.
- Keep a way out
- Settings, Phone and Messages start out allowed so you can always call, text and reach Settings. Phone works no matter what, and the minimal home screen has an Emergency button too.
- … … allowed
- That's fine if it's on purpose. To get out early you'd use the unlock steps or the Emergency button.
- What belongs here
- Apps you could need in the middle of anything: maps and public transport, taxis, your bank or payment app, an authenticator, tickets and boarding passes, a health or care app. These are never blocked, and the Emergency button reaches them during any session.
- Keep the list short
- Everything here is a way around every session, so add what you would genuinely need in a hurry — not what you would like to have.
- … is an easy way out
- From Settings, digital refuge can be switched off in a moment, which makes every session easy to skip.
- Groups
- + Save selection as group
- Save as group
- Name it, and next time you can tick all these apps in one tap.
- Group name
- e.g. Social
- My group
- Save
- Cancel

## search

<sub>[SearchSettingsScreen.kt](../../app/src/main/java/app/bedtime/ui/settings/SearchSettingsScreen.kt)</sub>

- Add to …
- Save in refuge notes
- Add to calendar, when the words say when
- Contacts: call and message
- Calculator: "12 * 3"
- Start a block, by its name
- Alarms: "alarm 7:30"
- Timers: "timer 10 min"
- Settings pages
- Obsidian notes, by title
- Files in your folders, by name
- Obsidian vault
- Folder of files
- Search the web · {appLabel}
- Wikipedia · {appLabel}
- Maps · {appLabel}
- Look up in {appLabel}
- Search in {appLabel}
- “{keyword}” is taken
- It's already the keyword for {owner}. Pick another, or change that one first.
- Search
- Matching apps always come first; then what's found, what you can do, and where to look your words up, in that order. Each action is in every search, only with its keyword (type it, a space, then your words: "m cafe"), or off. Only apps a session allows are offered during it.
- Switched off: none of these show, not even by keyword. Each keeps its own setting for when it's back.
- Every search
- Keyword only
- Off
- Give it a keyword, or nothing reaches it
- Beside each contact
- Up to … ways to message them, in the order you pick them
- A message typed after their name is copied, to paste
- Adding to your calendar
- “dentist next friday 9” offers to add it, to this calendar
- Allow calendar access
- Asked anyway the first time you add one
- No calendar on this phone can take new events.
- in use
- Folders
- Only names are read, to match what you type; nothing inside the files.
- None chosen yet
- Add a folder of files
- Try it
- anna, t 10, m cafe…
- By its keyword, only: …, with “…”
- Apps whose name matches “…”, then, where they have something for it:
- · nothing else

## apps

<sub>[GroupsScreen.kt](../../app/src/main/java/app/bedtime/ui/groups/GroupsScreen.kt)</sub>

- Apps in this group
- My group
- App groups
- New group
- Groups let you tick many apps in one tap when choosing apps to block or allow. Editing a group later doesn't change schedules you've already set up.
- No groups yet.
- Edit group
- Create group
- Save changes
- Name
- e.g. Social, Work tools, Games
- Apps
- None yet · tap to choose
- Delete group

<sub>[AlwaysAvailableScreen.kt](../../app/src/main/java/app/bedtime/ui/settings/AlwaysAvailableScreen.kt)</sub>

- A session is running
- You left a session early
- Apps here are never blocked, so adding one now would be a way out of the session you are in — and of every session after it. The list opens again at ….
- The list stays shut until …, when that session would have ended, so unlocking early isn't a way to make the next one easier.
- Locked until …
- Always available

<sub>[DistractionScreen.kt](../../app/src/main/java/app/bedtime/ui/settings/DistractionScreen.kt)</sub>

- Distracting apps
- Your day as 24 hours: an hour counts as distracted when you spent more than its minutes in these apps, or, if you like, on the phone at all. Only which app was open is counted, never what was in it.
- The apps
- None yet
- An hour counts after
- In these apps
- {it} min
- Or on the phone at all, in any app
- {m} min
- Off
- To see it on your home or lock screen, switch on the distraction tracker in Widgets; its look and colours are there too.

## notes

<sub>[NoteTargetsScreen.kt](../../app/src/main/java/app/bedtime/ui/notes/NoteTargetsScreen.kt)</sub>

- that file can't be written to from here
- that folder can't be written to from here
- Quick notes
- From the minimal home's search
- Type in the minimal home's search, then pick where it goes: the app's own notes, one of your files, or your calendar if it says when.
- Refuge notes
- Your notes
- Kept on this phone, written in Markdown
- Your files
- Up to …. Each note is added at the end; tap one to change how
- your words
- Today's note, e.g. … · adds “{example}”
- Adds “{example}”
- Add a file
- Pick a note, then name it, e.g. scratchpad or movies list
- Add today's daily note
- Pick your daily-notes folder; today's note is made if it isn't there
- For Obsidian, pick the note inside your vault. Vaults in Documents work; one kept in Obsidian's own app folder can't be reached by other apps.
- How it reads in the search: “add to …”
- e.g. today
- e.g. scratchpad
- The daily note's name
- As in Obsidian's daily notes settings; folders work too, like YYYY/MM/YYYY-MM-DD
- Each note is written as
- A list item
- A to-do
- Plain
- Date and time first
- Watch Paterson
- Add
- Save
- Cancel

## cover messages

<sub>[CoverMessagesScreen.kt](../../app/src/main/java/app/bedtime/ui/settings/CoverMessagesScreen.kt)</sub>

- Cover messages
- What a cover says when you open an app a session or a limit holds back. Each schedule, block and limit picks one, or a different one each time.
- Yours
- Switched on, it's in the mix of a different one each time
- None yet.
- Write your own
- Add
- The app's own
- Always in the mix

## recent problems

<sub>[ProblemsScreen.kt](../../app/src/main/java/app/bedtime/ui/settings/ProblemsScreen.kt)</sub>

- digital refuge problems
- Recent problems
- What failed lately and where, newest first: the last …. Only what went wrong is noted, never what was on the screen. Copy it into a message if something isn't working.
- Nothing has gone wrong.
- Copy all
- Clear

## how it works and the intro

Their text is in [onboarding.md](onboarding.md).
