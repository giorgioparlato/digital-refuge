# focus — the first tab

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
focus
├── the session header: what's on now, "leave early", undo; or what starts next
├── what's new (after an update)
├── setup checklist (until blocking is allowed)
├── notes (top right) ─────────────── refuge notes → one note
├── blocks: start pill · tap → the editor
├── schedules: switch · swipe left to delete · tap → the editor
├── new ───────────────────────────── templates → the editor
│
├── the editor (a block or schedule)
│   ├── what it does: block apps · what the cover says · greyscale · minimal mode · notifications
│   ├── on the screens: its own widgets
│   ├── how hard to leave: the steps · what unlocking gets you
│   └── the ways around it: emergency break · cover android's off switches · full-screen reminder ·
│       keep this shut after leaving early · changeable only at certain hours
│
├── leave early ──────────────────── how long (if "you choose") → wait → typing → password → done
│
└── while a session runs (outside the app)
    ├── the cover over a blocked app · the cover over android's off switches
    ├── the minimal home screen: header · emergency · all apps and search · hold an app · hold a widget
    ├── refuge's lock screen: notifications · replies · gestures
    └── the status-bar notification · the full-screen reminder · the quick-settings tile · the widget
```

## focus

<sub>[HomeScreen.kt](../../app/src/main/java/app/bedtime/ui/home/HomeScreen.kt)</sub>

- No blocks yet · add one
- No schedules yet · add one
- new block or schedule
- digital refuge
- … is on
- … sessions are on
- for another …, until …
- undo …
- leave early
- … blocked
- minimal mode
- do not disturb
- taking a break
- … picks up again at …
- all clear for now
- … begins …
- all clear
- start a block whenever you need one
- none of your schedules are switched on
- until …
- on now
- How long is …?
- This is the length it starts with from now on. You can still change it in the block's settings.
- Cancel
- Save
- new in …
- got it
- Finish setting up
- A few one-time steps and digital refuge is ready.
- Let digital refuge see which app is open
- Needed for blocking and minimal mode
- Allow greyscale
- Optional · a one-time step
- Allow Do Not Disturb
- Lets sessions quiet your phone and hold notifications
- Continue setup
- Set quiet hours or start a focus block. Distracting apps step aside, your screen can fade to grey and notifications wait for you.
- Create your own
- Or start from a template

<sub>[ScheduleItem.kt](../../app/src/main/java/app/bedtime/ui/schedules/ScheduleItem.kt)</sub>

- on now
- minimal mode
- … blocked
- priority only
- can be changed from …
- locked until …
- starts …

## what's new

Each update's lines are in [Changelog.kt](../../app/src/main/java/app/bedtime/data/Changelog.kt), newest first.

## notes

<sub>[NotesScreen.kt](../../app/src/main/java/app/bedtime/ui/notes/NotesScreen.kt)</sub>

- Notes
- Written in Markdown, kept on this phone
- new note
- Nothing yet. Type in the minimal home's search and pick “save in refuge notes”, or start one with +.
- New note
- Note
- # A title  - a list - [ ] a to-do
- closing bracket
- heading: tap again for a smaller one

<sub>[Markdown.kt](../../app/src/main/java/app/bedtime/ui/notes/Markdown.kt)</sub>

- # {line}

## new: templates

<sub>[CreateScreen.kt](../../app/src/main/java/app/bedtime/ui/create/CreateScreen.kt)</sub>

- New
- Start from scratch
- Schedule
- Repeats on days and times
- Block
- Start any time, for a set length
- Schedules
- Switch on by themselves at set times
- Focus blocks
- Start one whenever you need it

<sub>[Templates.kt](../../app/src/main/java/app/bedtime/data/Templates.kt)</sub>

- Bedtime
- greyscale · calm home screen · quiet phone
- Work focus
- no social apps · notifications held back
- Slow morning
- greyscale · no feeds before breakfast
- Weekend detox
- minimal home screen · greyscale
- Focus
- social apps blocked · quiet phone
- Deep work
- minimal home screen · silent phone, music still plays
- Pomodoro
- one task, then a break
- Study session
- greyscale · notifications held back

## the editor

<sub>[ScheduleEditScreen.kt](../../app/src/main/java/app/bedtime/ui/edit/ScheduleEditScreen.kt)</sub>

- Pick at least one day for this schedule.
- Every day needs at least … minutes in a row when this can be changed, so you can never be shut out of it. 
- Widen the hours it can be changed, or move them clear of the hours it runs.
- Leave a longer gap between when it ends and when it starts again.
- Choose a password, or switch off the password step.
- Apps to block
- Apps to allow
- Notifications from
- Starts at
- Ends at
- Can be changed from
- Can be changed until
- Focus block
- Schedule
- Delete “…”?
- This can't be undone.
- Delete
- Keep it
- Discard changes?
- Your edits won't be saved.
- Discard
- Keep editing
- nothing blocked yet
- … blocked
- minimal home
- priority calls only
- notifications held
- a single tap — no steps picked
- ends the block
- off until its next start
- a …-minute break
- as long as you say
- … · {then}
- emergency break … min
- no emergency break
- off switches covered
- reminder on
- changeable {changeable}
- Name this block
- Name this schedule
- e.g. Deep work
- e.g. Bedtime
- Can be changed from …
- Locked until …
- Unlock to make changes
- Saving…
- Create block
- Create schedule
- Save changes
- Outside the hours you set
- You left this one early
- This block is running
- This schedule is on right now
- You chose to let this one be changed only between … and …. Until then it can't be edited, switched off or deleted.
- Its settings stay shut until …, when it would have ended — otherwise unlocking would be a way to soften it and start again. You can change that under "the ways around it".
- To keep you honest, it can't be changed or deleted until you unlock it or it ends.
- Almost there
- What it does
- Block apps
- None yet · tap to choose
- What the cover says
- Greyscale
- Fade apps to black and white
- Needs a one-time step (see Setup)
- Keep home screen in colour
- Only apps turn grey; digital refuge's own home screen keeps its colours
- Minimal mode
- Swap your home screen for a calm list of essential apps
- Allowed apps
- Just the phone app for now · tap to add
- …, plus the phone app
- Notifications
- Off
- Priority
- Silence
- Calls and notifications work as usual.
- Only your priority contacts can ring. Music, videos and alarms still play.
- No calls or notification sounds. Music, videos and alarms still play.
- Hide notifications
- Held back while it's on, then they all come back
- On refuge's lock screen
- As usual
- Only these
- None
- From these apps
- None yet
- And these people
- Nobody yet
- And these circles
- The apps and circles you chose for the lock screen.
- Only these while it runs, whatever the lock screen usually shows.
- Nothing on refuge's lock screen while it runs.
- Needs Do Not Disturb access
- Grant it once in Setup so digital refuge can quiet your phone.
- Open setup
- On the screens
- Your usual widgets
- Its own widgets on home and the lock screen
- Its own widgets on home
- Its own widgets on the lock screen
- Home screen
- Lock screen
- Its own
- While it runs, the … shows your usual widgets.
- While it runs, these instead, in this order. If another session with its own starts later, its own win.
- How hard to leave
- The steps, in this order
- What unlocking gets you
- End it
- End today
- Set break
- You choose
- The block stops there.
- The schedule stays off until its next start.
- It switches itself back on after your break.
- When you unlock, you first say how long: a few minutes, or the rest of 
- the block.
- the session.
-  The steps are the same either way.
- How long the break lasts
- break length
- The ways around it
- Every way around a session has a switch of its own.
- Emergency break
- A short step out for whatever genuinely can't wait. It counts as an escape.
- How long you can step out
- step out
- Cover Android's off switches
- While it runs, covers the Settings screens that switch blocking off or uninstall the app
- Full-screen reminder
- If blocking is switched off while this runs, take over the screen until it's back on
- Keep this shut after leaving early
- Its settings stay as they are until it would have ended, so leaving early can't soften it
- Changeable only at certain hours
- Outside them it can't be edited, switched off or deleted.
- From
- Until
- next day
- Outside these hours this page is read-only, and the schedule can't be switched off or deleted from the list either.
- Duplicate block
- Duplicate schedule
- Delete block
- Delete schedule
- name this block
- name this schedule
- you can change it each time you start the block
- default length
- … · … each time
-  · ends next day
- Every day
- Weekdays
- Weekends
- Done
- Cancel

<sub>[UnlockStepsEditor.kt](../../app/src/main/java/app/bedtime/ui/edit/UnlockStepsEditor.kt)</sub>

- wait …
- copy out a passage
- type … characters
- a password
- Password
- Tip: let someone else choose it
- New password (leave empty to keep)
- Choose a password
- Each early unlock in a day makes the wait and text longer
- No steps picked
- Unlocking will take a single tap.
- Start a timer, come back when it's done
- Wait it out
- Timer
- Type random text
- No pasting, typos don't count
- Characters
- What to copy
- Letters
- Words
- Passages
- Random letters and digits. Nothing to read, only to copy.
- Plain words, easier on the eyes at midnight.
- Quotes and longer passages, worth reading while you type them.
- Harder each time
- How much
- each time

<sub>[MessageChoice.kt](../../app/src/main/java/app/bedtime/ui/components/MessageChoice.kt)</sub>

- What the cover says
- A different one each time
- From the app's and yours that are in the mix
- Or always one of these
- Yours
- In the mix · tap the dot to take it out
- Not in the mix · tap the dot to add it
- Write your own
- A few words to read
- Add and use it
- Done

<sub>[CoverMessages.kt](../../app/src/main/java/app/bedtime/data/CoverMessages.kt)</sub>

- Is this what you came to the phone for?
- Maybe put the phone down for a bit.
- What were you about to do before this?
- This will still be here later.
- Take a breath. Then decide.
- Look up for a moment.
- A different one each time

## leave early

<sub>[UnlockFlow.kt](../../app/src/main/java/app/bedtime/unlock/UnlockFlow.kt)</sub>

- Wait
- Type
- Password
- the rest of the block
- the rest of the session
- … unlock today \u00b7 the steps are longer
- Unlock
- Continue
- Waiting\u2026
- {outcome} The timer only runs while this screen is open, and starts over if you back out.
- {outcome} Spaces and punctuation fill themselves in; pasting and typos are rejected.
- This ends the block.
- This ends the current session.
- This gives you a {minutes}-minute break.
- … is over. Start it again whenever you like.
- … is off until its next scheduled start.
- Enjoy your {minutes}-minute break. … switches back on afterwards.
- Unlocking {scheduleName}
- Never mind, let's keep still
- step … of …
- Unlocked
- Time's up.
- Take a breath.
- breathe in
- breathe out
- {rejected} … caught. No worries, just keep going.
- Start typing here…
- Enter the password for this schedule.
- That's not it. Try again.
- Checking…

<sub>[UnlockChoice.kt](../../app/src/main/java/app/bedtime/unlock/UnlockChoice.kt)</sub>

- The steps come next, and they're the same whichever length you choose.
- Continue · {restLabel}
- Continue · …
- How long do you need?
- Decide first, then earn it.
- Or exactly
- how long

## while a session runs: the covers

<sub>[BlockedActivity.kt](../../app/src/main/java/app/bedtime/ui/blocked/BlockedActivity.kt)</sub>

- {scheduleName} is on
- {appLabel} can wait
- You'll have it back at {until}. Until then, maybe put the phone down for a bit.
- You'll have it back at {until}.
- Go to home screen
- I really need it
- {breakMinutes} …
- End {scheduleName} early
- This stays locked until {until}
- Switching blocking off and uninstalling are locked while {scheduleName} runs. If something truly can't wait, take an emergency break; otherwise end the session with the unlock steps.
- Switching blocking off and uninstalling are locked while {scheduleName} runs. To do either, end the session first — the unlock steps still work.
- Pause blocking for {minutes}
- Go back

<sub>[CoverLayout.kt](../../app/src/main/java/app/bedtime/ui/blocked/CoverLayout.kt)</sub>

- (no text of its own)

## while a session runs: the minimal home screen

<sub>[MinimalHomeActivity.kt](../../app/src/main/java/app/bedtime/ui/minimal/MinimalHomeActivity.kt)</sub>

- … until …
- added to your calendar
- couldn't add it to a calendar
- calendar access wasn't allowed
- digital refuge
- couldn't open that
- … needs their number with its country code
- message copied: paste it in …
- copied …
- alarm set
- timer started
- no clock app took it
- couldn't run that
- Hold an app in the full list to add it here.
- no calendar app opened it
- more for …
- nothing to open right now. enjoy the quiet.
- favourite or hide this quote
- web search
- Wikipedia
- look up
- Spotify search
- refuge notes
- a note file
- the calendar
- Obsidian notes

<sub>[SessionHeader.kt](../../app/src/main/java/app/bedtime/ui/minimal/SessionHeader.kt)</sub>

- see the week ahead
- {scheduleName} · until {until}
- open the weather
- — {credit}

<sub>[EmergencyContent.kt](../../app/src/main/java/app/bedtime/ui/minimal/EmergencyContent.kt)</sub>

- {breakMinutes} …
- for when something really can't wait. your session stays in place.
- always works
- digital refuge
- emergency break
- steps out for {minutes}. a full screen brings you back when it ends.
- pause blocking for {minutes}

<sub>[AppDrawer.kt](../../app/src/main/java/app/bedtime/ui/minimal/AppDrawer.kt)</sub>

- {day}, all day
- {day}, …–…
- add to calendar: … · {wh}
- Apps, the web, a note, an event
- add “{query}” to …
- save “{query}” in refuge notes
- open refuge notes
- start … · …
- settings: …
- … · obsidian

<sub>[AppActions.kt](../../app/src/main/java/app/bedtime/ui/minimal/AppActions.kt)</sub>

- Type it in the search, then Enter, to open ….
- “{typed}” is already the keyword for {owner}. Pick another.
- Save
- Take off the home screen
- Add to the home screen
- Move up
- Move down
- Rename
- Give it a keyword
- Keyword: {keyword}
- Show in the lists again
- Hide from the lists
- App info
- Uninstall

<sub>[BlockSheet.kt](../../app/src/main/java/app/bedtime/ui/minimal/BlockSheet.kt)</sub>

- Start a block
- No blocks yet. Make one in digital refuge, under schedules.
- A block
- on now

<sub>[WidgetSheet.kt](../../app/src/main/java/app/bedtime/ui/minimal/WidgetSheet.kt)</sub>

- Other…
- Custom
- Show on the home screen
- Show on the lock screen
- Hide from this screen
- This session has its own widgets; hiding takes it off its list. 
- Bring it back from Widgets in digital refuge's settings.
- Done
- Cancel

<sub>[ScreenSheet.kt](../../app/src/main/java/app/bedtime/ui/minimal/ScreenSheet.kt)</sub>

- This screen
- The lock screen
- Its widgets while this session runs. Hold one on the screen for its own options.
- Its widgets. Hold one on the screen for its own options.

<sub>[CalendarLine.kt](../../app/src/main/java/app/bedtime/ui/minimal/CalendarLine.kt)</sub>

- … ASC
- now · … until …
- an event until …
- tomorrow · … at …
- tomorrow, first event at …
- next · … at …
- next event at …
- Today
- Tomorrow

<sub>[CalendarWeek.kt](../../app/src/main/java/app/bedtime/ui/minimal/CalendarWeek.kt)</sub>

- The week ahead
- Nothing in the next seven days.
- All day
- Busy
- , all day
- Open in calendar

<sub>[WeatherLine.kt](../../app/src/main/java/app/bedtime/ui/minimal/WeatherLine.kt)</sub>

- (no text of its own)

<sub>[LookUp.kt](../../app/src/main/java/app/bedtime/ui/minimal/LookUp.kt)</sub>

-  · opens once unlocked
- A note
- refuge notes{later}
- All
- the whole entry ›
- Notes
- ‹ back

<sub>[NoteWidgetView.kt](../../app/src/main/java/app/bedtime/ui/notes/NoteWidgetView.kt)</sub>

- hold here to choose a note to show
- Note
- tap to write

<sub>[QuoteMenu.kt](../../app/src/main/java/app/bedtime/ui/quotes/QuoteMenu.kt)</sub>

- hidden · undo it on the quotes page
- No longer a favourite
- Favourite
- Don't show again
- Another one
- The quote widget…

## while a session runs: the lock screen

<sub>[LockScreenActivity.kt](../../app/src/main/java/app/bedtime/ui/lock/LockScreenActivity.kt)</sub>

- that didn't go through
- unlock the phone
- tap to open
- timer started
- no clock app took it
- allow calendar access under settings › search first
- added to your calendar
- couldn't add it to a calendar
- … until …
- copied …

<sub>[LockNotificationViews.kt](../../app/src/main/java/app/bedtime/ui/lock/LockNotificationViews.kt)</sub>

- and … more
- later for all
- New notification
- Reply
- Later
- Quiet until morning
- Write a reply
- Sending asks for your unlock, then locks again
- Cancel
- Send

## while a session runs: notification, reminder, tile and widget

<sub>[SessionNotifier.kt](../../app/src/main/java/app/bedtime/service/SessionNotifier.kt)</sub>

- the full-screen reminder is off
- tap to allow display over other apps again
- refuge is on
- {name} · until …
- on a break
- switch blocking back on when you're done
- blocking is off
- tap to switch it back on
- {name} is still running · tap to switch blocking back on
- no session running
- +{others} more
- minimal home
- … apps blocked
- do not disturb
- session running
- a lotus in the status bar while a schedule or block is on, and a reminder if blocking goes off
- taken back, as if it never started
- too late to undo · leave early instead

<sub>[TakeoverOverlay.kt](../../app/src/main/java/app/bedtime/service/TakeoverOverlay.kt)</sub>

- drawing the full-screen reminder
- {scheduleName} is still running. switch blocking back on to carry on.
- take {breakMinutes} more …

<sub>[FocusTileService.kt](../../app/src/main/java/app/bedtime/service/FocusTileService.kt)</sub>

- until …
- add a block

<sub>[BlockWidget.kt](../../app/src/main/java/app/bedtime/ui/widget/BlockWidget.kt)</sub>

- add a block
- on · until …

<sub>[BlockWidgetConfigActivity.kt](../../app/src/main/java/app/bedtime/ui/widget/BlockWidgetConfigActivity.kt)</sub>

- Widget
- You don't have any focus blocks yet. Create one, and this widget will start it with a tap.
- Which block should this widget start? While it runs, the widget shows when it ends.
- Create a block
