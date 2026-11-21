# Wording

Every line rewritten in the new voice, as the app shows it now. To change one, edit the text here and ask Claude to apply it: the file in each section says where it lives. (The app shows everything in lowercase, whatever the case here.)

## The voice

- Plain and direct, talking to "you", in everyday words ("stuff", "a bunch more things").
- Name the thing and what it's for ("habit tracker", "special folder in notes just for journaling"); say how to use it first.
- One idea per sentence, few details, no metaphors, no chains of colons and semicolons.
- Reassure, and say what's needed, where someone could worry or get stuck.
- Sentences have verbs; short intros and labels can be fragments.
- Left as they are: covers and unlock screens, the passages, quotes and journal prompts, the changelog's history.

## Setup — `ui/onboarding/SetupFlow.kt`

| Where | Text |
|---|---|
| distracting apps | Pick distracting apps, useful for other features, including tracking screen time |
| a day's budget | Set how much phone time a day feels right. Only for tracking screen time |
| a bedtime | Choose when your distracting apps get blocked at night. To open one early, you go through a few steps first. |
| bedtime › greyscale | Colours drain away; on Android 15 and newer, with do not disturb access |
| bedtime › quiet home | The time and your few apps as words |
| bedtime › silence | Held back until morning; alarms and music are never silenced |
| limits | Set a daily time limit, or a short pause before opening, for the apps you use most. Limits work all day, not only at bedtime. |
| limits › several apps | Here each app gets its own limit. One limit can also cover several apps together, like all your social apps: set that up in focus › limits. |
| limits › none chosen | No distracting apps chosen yet. You can add limits any time in focus. |
| always available | These apps are never blocked, like maps, rides, your bank or your authenticator. |
| permission | Refuge needs this to notice when a blocked app opens. It only sees which app is open, never what's in it. |
| permission › the rest | The other permissions (notifications, battery, full-screen reminder) are in settings › get started, whenever you want them. |
| usage access | Lets refuge see your past screen time, from Android's own records. It stays on the phone. Without it, refuge starts counting from now. |
| home screen | Use refuge's calm home screen: the time, a few widgets and your apps as words. Use it all the time, or only at bedtime. |
| done | You can change all of this later in settings. |
| done › discover | See what else refuge does when you want: the home and lock screens, widgets, search, notes, voice notes, Obsidian and more. |

## How it works — `ui/onboarding/OnboardingScreen.kt`

| Where | Text |
|---|---|
| intro | digital refuge blocks your distracting apps when you want a break from them. here's what happens during a session, and how to leave early if you need to. |
| a session begins | at the times you set, or when you start a block, the apps you chose stop opening. your home screen can switch to a short list of apps, the screen can turn grey, and notifications can wait. music and alarms always work. |
| leaving early takes the steps you chose | you choose what leaving early asks for: a wait, a passage to copy, a few words of why, or a password. the steps can get longer each time that day, and reset every morning. |
| it stays put while it runs | you can't change a session's settings while it runs, otherwise the easiest way out would be to edit it. |
| an emergency break | for something that can't wait, take a minute with no steps, without ending the session. when it's over, refuge brings you back. |
| limits hold every day | limits work outside sessions too: a short pause before an app opens, or a daily time budget. making one stricter works straight away; loosening or deleting it starts the next day. |
| around the sessions | the rest of refuge helps make your phone quieter, in a session or not. all of it is optional, and you set it up in settings. |
| a quieter home screen | use refuge as your home screen: the time, a few widgets and your apps as words. use it only during sessions, or as your everyday launcher, with search for apps, people and more. |
| a lock screen of its own | use refuge's lockscreen: the time, what's next today, and only the notifications from the apps and people you choose. tap or swipe up to unlock. |
| notes and habits | jot a quick note from anywhere into refuge's notes or your own files, and tick off habits here or from your screens. |
| insights | see your screen time, how your sessions went and which habits you kept. refuge only counts which app is open, never what's in it. |
| how firm to make it | two things always apply: you can't change a session while it runs, and leaving early takes the steps you chose. everything below is an extra switch in each schedule and block: turn on the ones you want. |
| harder each time | each time you leave early on the same day, the wait and the typing get longer. it resets every morning. |
| keep this shut after leaving early | if you leave a session early, its apps stay blocked until it would have ended. |
| no emergency break | turn the emergency break off, and the unlock steps are the only way out. |
| cover android's off switches | refuge covers the android settings that would turn blocking off or uninstall the app. |
| full-screen reminder | if blocking gets turned off, a full screen stays up until you turn it back on. |
| footer | open a block or schedule to set the first three. the last two are in settings › focus. everything stays on your phone: there's no account, and the app can't use the internet. |

## Settings — `ui/settings/SettingsScreen.kt`

| Where | Text |
|---|---|
| get started (intro) | Give refuge what it needs, set it up again, see how sessions work, and discover what else it does. |
| home screen (intro) | Use refuge as your launcher, or only during sessions. Choose its look, widgets and gestures. |
| lock screen (intro) | Use refuge's lockscreen with only what you choose: the time, widgets and the notifications you want. |
| focus (intro) | How sessions work: what blocked apps show, and when refuge asks you to pause or journal. |
| apps (intro) | Choose your distracting apps, the ones that never get blocked, and groups to pick many apps at once. |
| shortcuts (intro) | Start a block without opening the app. |
| about & backup (intro) | Back up your settings and history, see credits, and check anything that went wrong. |
| get started | (steps left) · set up again · how it works · discover features |
| set up again | Go through setup again, starting from what you have now |
| how it works | What a session blocks, and how to leave early |
| discover features | See what refuge does, each with a short look at how it works |
| habits | Your habits, habit sets, and a tab for them |
| about & backup | Export and import · credits · recent problems |
| everyday launcher, off / on | Off · only during quiet sessions / On · refuge is your home screen |
| home › widgets, lock › widgets | Choose what it shows |
| home › gestures | Open an app, write a note, start a block… |
| lock › in sessions | Show it while a schedule or block runs |
| lock › outside sessions, off | Only when refuge is your launcher |
| lock › look | Colours, sizes and fonts, separate from home |
| focus › cover android's off switches | During a session, blocks the Android settings that would turn refuge off or uninstall it |
| focus › full-screen reminder | If blocking is turned off during a session, a full screen stays up until you turn it back on |
| focus › cover messages | The messages you see when opening a blocked app · (count) |
| focus › nudge | A reminder after (n) min on the phone without a break |
| focus › nudge dialog | After a long stretch on the phone, a full screen asks if you still want to be on it. Never during a call. |
| apps › app groups, none | Pick many apps at once with one tap |
| apps › always available, none | None yet · like maps, rides or your authenticator |
| shortcuts › quick-settings tile | Choose which block the "refuge" tile starts |
| about › export settings | Saves your settings, history, notes, habits and screen time in one file (+ microjournal, + sealed with your passphrase, when on) |
| about › import settings | Replaces everything with a saved file |
| about › credits | The people and open-source projects refuge is built on |
| about › recent problems | Errors from the last days, to copy and send |
| about › weekly backup, off | Off · saves a backup every week to a folder you choose, keeping the last (n) |
| about › back up now | Save this week's backup now |
| about › microjournal in backups, on / off | Journal lines, moods and why answers are included / Off · they stay on this phone |
| settings search, nothing found | No setting with that name. |

## Schedule editor — `ui/edit/ScheduleEditScreen.kt`

| Where | Text |
|---|---|
| greyscale | Turns the whole screen black and white, refuge included |
| quiet home screen | Minimal homescreen, shows the apps and widgets you choose |
| on its home screen, allow-only / usual | The apps that stay open · tap to choose / Your usual home apps, minus the blocked ones · tap to choose |
| notifications › priority | Only your priority contacts and apps make a sound. Music, videos and alarms still play. |
| notifications › out of sight | No banners or icons. They all come back when it ends. |
| refuge's lock screen › only these | Show only these while it runs. |
| leaving early (heading) | How leaving early works |
| what unlocking gets you › you choose | When you leave early, choose for how long: a few minutes, or the rest of the session (block). The steps are the same either way. |
| emergency break | A minute with no steps, for what can't wait |
| journal after each unlock | While it runs, asks why you unlocked and how you feel. One tap to skip |
| keep this shut after leaving early | If you leave early, it stays blocked until it would have ended |
| changeable hours | Outside these hours you can't edit, turn off or delete this schedule. |
| while it runs | You can't change or delete it until you leave early or it ends. |

## Leaving-early steps — `ui/edit/UnlockStepsEditor.kt`

| Where | Text |
|---|---|
| wait it out | Wait until a timer runs out |
| write a few lines | Write why you want to unlock, or answer a prompt |
| harder each time | Each time in a day, the wait, typing and writing get longer |
| no steps | No steps yet: leaving early takes one tap. |
| text: random letters | Random letters and numbers to copy. |
| text: words | Simple words, easier to read late at night. |
| text: passages | Quotes and longer passages, worth reading as you type. |

## Limit editor — `ui/limits/LimitEditScreen.kt` (and `LimitsScreen.kt`)

| Where | Text |
|---|---|
| journal pause | Asks why you're opening it. One tap to skip. What you write goes to its note. |
| harder each time | Each open makes it longer, up to 8× the wait or 3× the text. Resets every day |
| open for, all apps / each app | One pause opens all these apps for (n) min. Then the pause comes back. / After the pause, each app stays open for (n) min. Then the pause comes back. |
| minutes a day | Once used up, the apps are blocked until tomorrow |
| opens a day | How many times a day they can open. Coming back within a minute doesn't count |
| runs out › wait for tomorrow | There's no way to get more. The apps are back at midnight. |
| runs out › you choose | To get more time, choose how much, then do the steps below. |
| stricter or looser, new / saved | Once it's saved, making it stricter works straight away. Loosening or deleting it starts tomorrow. Until tonight, while it's new, you can change it freely. / Making it stricter works straight away. Loosening or deleting it starts tomorrow. |
| deleting (kept) | It keeps holding until midnight, so deleting it can't get you round today. |
| no limits yet (`LimitsScreen.kt`) | No limits yet. Add one to set a daily time budget or a pause before an app opens. |

## Widgets and home screen — `ui/homestyle/WidgetOptions.kt`, `ui/homestyle/HomeStyleScreen.kt`

| Where | Text |
|---|---|
| weather | The weather comes from Breezy Weather, a free app. Refuge itself never goes online. |
| calendar | Uses the calendar already on your phone. Nothing goes online. |
| habits › what today needs | The habits still to do today to keep their streak going. |
| habits › beside the clock | Shows beside the clock only with a small clock and no weather beside it. Otherwise, in their own row. |
| look (intro) | Colours, sizes and fonts for each screen. Changes save as you go. |
| widgets (intro) | Choose what your home and lock screen show, and in what order. Changes save as you go. |
| lock-screen gestures (intro) | Swipe from around the clock, or double tap anywhere. Each can open the camera, a quick note or an app once you unlock. |
| gestures (intro) | Swipe anywhere except the very top edge, or double tap an empty spot. Each can open an app or do an action. |
| lock-screen notifications (intro) | The notifications from the apps you choose, as private as Android's own lock screen. |
| lock-screen notifications › switch | Drag one down to read it, tap to open it, or swipe it away. Choose what each does below |
| fonts | Serif fonts show the quote in italics. You can add your own .ttf or .otf font. It stays on the phone and isn't backed up. |
| a note on sunday evening | Shows once the week is over, and in insights until Monday ends |
| a rain alert | A morning heads-up when rain is likely, with the chance and the time |

## Messages and empty screens

| Where | Text |
|---|---|
| voice notes not set up (`voice/RecordActivity.kt`) | set up voice notes first in settings › notes |
| no voice notes folder (`voice/VoiceFiling.kt`) | choose a folder for voice notes first, in settings › notes |
| voice notes folder unwritable (`voice/VoiceFiling.kt`) | refuge can't save to the voice notes folder. choose it again in settings › notes |
| no notes (`ui/notes/NotesScreen.kt`) | No notes yet. Tap + to write one, or double tap the home or lock screen for a quick note. |
| no journal sections (`ui/notes/JournalScreen.kt`) | No sections yet. Add some in settings › microjournal. |
| no moods (`ui/stats/MoodSections.kt`) | No moods yet this month. Add how you feel when you journal. |

## Discover features — `ui/onboarding/DiscoverScreen.kt`

The list:

| Feature | Line |
|---|---|
| when a session runs | see what blocked apps show, and how to leave early |
| your home screen | use refuge as your launcher |
| the lock screen | use refuge's lockscreen, with the widgets you choose |
| some widgets | see what each widget does when you tap it |
| insights | see your screen time against your budget |
| search | search anything, and do things, as you type |
| notes | notes in markdown, synced with obsidian if you like |
| quick notes | jot a note with a double tap, even locked |
| voice notes | speak your notes instead of typing |
| obsidian | find, add to and save notes in your vault |
| habits | a simple habit tracker |
| the microjournal | a journal, one line at a time |
| the dictionary | look up a word without unlocking |

Each page's intro, as you wrote it:

| Page | Intro |
|---|---|
| when a session runs | while a block, a schedule or a limit runs, the apps it shuts open to a cover instead. |
| your home screen | use refuge as your launcher: a calm home screen with only what you put on it. |
| the lock screen | you can use refuge's lockscreen, only with what you choose, and the option of cool widgets |
| some widgets | on the home screen or lock screen, each does a little more than it shows. |
| insights | see your phone usage, overall and in distracting apps in relation to the budget you set for yourself |
| search | swipe up to search anything, add stuff to your calendar, or do a bunch more things |
| notes | notes written in markdown. can be synced with obsidian |
| quick notes | jot a thought down in a second, even from your lockscreen, before the phone pulls you elsewhere. |
| voice notes | speak your notes or journaling instead of typing. an offline model writes out what you said. to use it you need to download a local voice transcription model. |
| obsidian | if you keep notes in obsidian, refuge works with them, all on the phone. |
| habits | simple, but effective and accessible habit tracker |
| the microjournal | special folder in notes just for journaling. every day you can easily add a line at any time. inspired from struthless' microjournaling concept: using our phones for something that turns our attention inwards rather than squandering it away |
| the dictionary | look up a word right from the lock screen, without unlocking the phone. the dictionaries are kept on the phone, so nothing else opens. |

The demos' captions and cards follow the canvas at https://claude.ai/artifact/29iXqZMy8SLioxghTDDTUL.
