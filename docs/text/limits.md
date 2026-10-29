# limits — the second tab

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
limits
├── each limit: what's left today · tap → the editor
├── new limit ──────────────────── the editor
├── the whole phone: a nudge after time in a row
│
├── the editor
│   ├── the apps
│   ├── pause before opening: a wait or typing · grows each open · "open anyway" lasts
│   ├── daily budget: minutes a day · opens a day · shared or each · when it runs out · the steps for more
│   ├── what it says (the cover's words)
│   └── when it can be changed
│
└── outside the app
    ├── the pause before opening (breathe, or type)
    ├── the cover when the minutes or opens are gone → more: how long → the steps
    └── the nudge after time in a row
```

## limits

<sub>[LimitsScreen.kt](../../app/src/main/java/app/bedtime/ui/limits/LimitsScreen.kt)</sub>

- every day, not only in a session
- new limit
- No limits yet. A limit can ask for a few breaths before an app opens, give it a daily budget, or both — for the apps that pull hardest.
- Create a limit
- the whole phone
- A nudge after time in a row
- After so long on the phone without the screen going off, in any app, a full screen asks if it's still what you meant to do. Never during a call
- Off
- Minutes
- … pause
- … a day
- no opens left
- 1 open left
- {opensLeft} opens left
- lifted today
- none left
- … left
- {extra} extra today
- ends at midnight
- looser from tomorrow
- unnamed limit

## the editor

The steps for more use the same rows as a block's steps: see the editor in focus.md.

<sub>[LimitEditScreen.kt](../../app/src/main/java/app/bedtime/ui/limits/LimitEditScreen.kt)</sub>

- Pick at least one app for this limit.
- Turn on a pause, minutes a day, or opens a day.
- Choose a password, or switch that step off.
- Apps to limit
- Limit
- Can be changed from
- Can be changed until
- Delete “…”?
- It goes straight away.
- It keeps holding until midnight, so deleting it can't get you round today.
- Delete
- Keep it
- Discard changes?
- Your edits won't be saved.
- Discard
- Keep editing
- Name this limit
- e.g. Social
- Can be changed from …
- Create limit
- Save, starting tomorrow
- Save changes
- name this limit
- … a day
- … characters to type
- … pause
- not set yet
- no apps yet · choose them below
- Outside the hours you set
- You chose to let this one be changed only between … and …. Until then it can't be edited or deleted.
- Almost there
- This loosens it, so it starts tomorrow
- Today keeps the limit as it was. Making a limit stricter always takes effect at once.
- Once it's saved, making it stricter takes effect at once, while loosening it or deleting it waits until the next day. Until tonight, while it's new, you can still change it freely.
- Making it stricter takes effect at once. Loosening it — fewer apps, a shorter pause, more minutes, an easier way to get more — or deleting it waits until the next day.
- Apps
- None yet · tap to choose
- Pause before opening
- Before one of these apps opens, a moment to decide
- Each open in a day makes the wait or the typing longer, up to 8× the wait or 3× the text. A new day starts over
- Back within
- every time
- {it} min
- Opening one anyway lets you into
- Just that app
- All of them
- Getting through the pause once opens all of these apps, until you leave them for something else.
- Getting through the pause once opens the way to all of these apps. Leaving them and coming back within … min doesn't ask again.
- The pause comes every time you open one of them.
- After you open one anyway, leaving and coming back within … min doesn't ask again.
- Daily budget
- Minutes a day
- Once they're used, the apps step aside until tomorrow
- Each day
- minutes a day
- Opens a day
- How many times they can be opened; coming back within a minute isn't another
- opens a day
- Shared or each
- Shared
- Each app
-  a day
- {allowance} across all of them, so it doesn't just move from one to another.
- {allowance} in each of them.
- When it runs out
- Wait for tomorrow
- Set extra time
- You choose
- Rest of the day
- No way to get more. The apps come back at midnight.
- The steps below give you a set amount more, and get longer each time if you choose.
- When you need more, you first say how much — some minutes, or the rest of today — then do the steps below.
- The steps below lift the limit until midnight.
-  Out of opens, each time gives one more.
- Each time
- more time
- The steps for more, in this order
- What it says
- What the cover says
- When it can be changed
- Changeable only at certain hours
- Outside them it can't be edited or deleted
- From
- Until
- next day
- Outside these hours this page is read-only, and the limit can't be deleted. Loosening it still waits for midnight either way.
- Delete limit
- 1 open a day
- {opens} opens a day

## outside the app: the pause and the cover

<sub>[LimitCovers.kt](../../app/src/main/java/app/bedtime/ui/blocked/LimitCovers.kt)</sub>

- About to open {label}
- type this first · the … time today
- type this first
- Never mind
- breathe in
- breathe out
- the … time today
- About to open {appLabel}
- Is this what you came to the phone for?
- Open {appLabel}
- Open {appLabel} in a moment
- this limit
- the rest of today
- This lifts {name} for the rest of today.
- This gives you one more open today.
- This gives you {minutes} more minutes today.
- {name} is lifted until tomorrow.
- One more open. Make it count.
- {minutes} more minutes. Spend them well.
- 1 open
- {dailyOpens} opens
- {limitName} · {allowance} a day
- {appLabel} is done for today
- That was today's {allowance}. It'll be back tomorrow.
- That was today's {allowance}. If it really can't wait, you can lift the limit for the rest of today.
- That was today's {allowance}. If it really can't wait, you can take one more.
- Go to home screen
- Lift it for today
- Open it once more
- {limitName} · {budget} a day
- That was today's {budget}. It'll be back tomorrow.
- That was today's {budget}. If it really can't wait, you can take {extraMinutes} more minutes.
- That was today's {budget}. If it really can't wait, you can lift the limit for the rest of today.
- That was today's {budget}. If it really can't wait, you can say how much more you need.
- Take {extraMinutes} more minutes
- Take more time

## outside the app: the nudge

<sub>[NudgeActivity.kt](../../app/src/main/java/app/bedtime/ui/blocked/NudgeActivity.kt)</sub>

- {minutes} minutes in a row
- A moment to look up
- You've been on your phone for {minutes} minutes without a break. Is this still what you meant to do?
- Lock the phone
- Keep going
