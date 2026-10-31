# insights — the third tab

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
insights
├── your week: in refuge · sessions · on the phone · distracting · most of that · habits
│   └── a note on sunday evening (switch) ── the notification
├── phone: day / week / month · against before, usual or a year back · by hour · top apps · unlocks
│   └── no usage access yet → allow it
├── sessions: the last 14 days · kept in a row · each block and schedule
└── habits: today first · each habit → its month · new habit (round +) · hold to reorder
```

## your week

<sub>[LookBackCard.kt](../../app/src/main/java/app/bedtime/ui/stats/LookBackCard.kt)</sub>

- Your week
- In refuge
- Sessions
- … of … kept
-  · … … out
- On the phone
- In distracting apps
- Most of that
- Habits
- … of … days kept
- … in refuge
- phone …
- , as before
- , {change}% more
- , …% less

<sub>[LookBackNotifier.kt](../../app/src/main/java/app/bedtime/service/LookBackNotifier.kt)</sub>

- your week
- a look back on the week, on sunday evening, if you asked for it

## phone and sessions

<sub>[StatsScreen.kt](../../app/src/main/java/app/bedtime/ui/stats/StatsScreen.kt)</sub>

- Phone
- Sessions
- Habits
- add a habit
- in refuge in the last seven days
- kept in a row
- best run
- … of …
- kept this week
- no ways out taken this week
- early unlock
- early unlocks
- emergency break
- emergency breaks
- time blocking was switched off
- times blocking was switched off
- Two weeks
- Time in refuge each day, by the day a session began. Tap a day for its figures.
- Each schedule and block
- The last four weeks
- … of … kept
- Time in limited apps
- The last seven days
- unnamed limit
-  · more time {extras}×
- a way out taken
- {hours} h
-  · nothing on
- nothing on

<sub>[PhoneUseSections.kt](../../app/src/main/java/app/bedtime/ui/stats/PhoneUseSections.kt)</sub>

- Allow usage access
- For every app's time and the days from before refuge, from Android's own record. It stays on the phone. The hour dots work without it
- Day
- Week
- Month
- the one before
- the one after
- on the phone
- against {against}
-  · nothing to compare with
- , so far
- a day you pick…
- By the hour
- By the day
- All of it, and the part in distracting apps
- ; outlined, {against}
- . The dots are unlocks
- . Tap a day for its hours
- The last eight weeks
- The last six months, a day on average
- Apps
- Time and opens, and the change against {against}; distracting ones marked
- Only your distracting apps are counted without usage access
- Nothing here.
- No distracting apps chosen yet. Choose them
- Which apps are distracting
- Fewer
- All …
- Each day
- Against a day's budget, the same in every month. Tap a day for it
- On the phone
- Distracting
- in distracting apps
- Budget: … a day {what}
- No budget chosen yet, so … a day {what} is suggested. Choose yours
- Nothing kept for these days yet.
- twice or more
- {text} ›
- A day on the phone
- A day in distracting apps
- Save
- Cancel
- this week
- last week
- the day before
- the week before
- last month
- the month before
- your usual 
- your usual week
- your usual month
- a year back
- your 7-day average
- your 30-day average
- your average day this year
- Compare
- ↑ {change}%
- the same

<sub>[PhoneUse.kt](../../app/src/main/java/app/bedtime/ui/components/PhoneUse.kt)</sub>

- (no text of its own)

## habits

<sub>[HabitsScreen.kt](../../app/src/main/java/app/bedtime/ui/habits/HabitsScreen.kt)</sub>

- Habits
- Kept on this phone
- add a habit
- Habits you want to keep, ticked off each day: daily, a few times a week, or a count like pages or reps. Add one with the + below; your home and lock screen can show them too.
- Today
- Hold one and drag it to change the order
- … of …… today
- … in a row
- {kept} of {possible}
- kept in the last seven days
- A new habit
- Change it
- Walk, read, stretch…
- Yes or no
- A count
- How often
- days apart
- every {it} days
- {it} a week
- {it} a month
- A day's aim
- reps, pages, glasses…
- In the home screen's habits widget
- In the lock screen's habits widget
- Its colour
- Accent
- Delete this habit
- Save
- Cancel
- Ember
- Coral
- Gold

<sub>[HabitViews.kt](../../app/src/main/java/app/bedtime/ui/habits/HabitViews.kt)</sub>

- days in a row
- best run
- Today
- {value} of ……
- Tap a day to tick or untick it.
- All your habits
- Change it
