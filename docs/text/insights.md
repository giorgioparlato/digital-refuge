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
- Most distracted
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
- Habits
- Moods
- Sessions
- add a habit
- Time in limited apps
- The last seven days
- unnamed limit
-  · more time {extras}×

<sub>[PhoneUseSections.kt](../../app/src/main/java/app/bedtime/ui/stats/PhoneUseSections.kt)</sub>

- Allow usage access
- For every app's time and the days from before refuge, from Android's own record. It stays on the phone. The hour dots work without it
- Day
- Week
- Month
- the one before
- the one after
- on the phone
- of … unlocks
- against {against}
-  · nothing to compare with
- , so far
- a day you pick…
- The last six months
- A day on average in each month. Tap one for more
- By the hour
- By the day
- Each day's time on the phone, the part in distracting apps in orange
- Each day's time in distracting apps
- All of it, and the part in distracting apps
- ; outlined, {against}
- . Unlocks by the hour under it
- . Tap a day to see it against …
- On the phone
- Distracting
- Most distracted
- this day
- The last eight weeks
- Apps
- Time and opens, and the change against {against}; distracting ones marked
- Only your distracting apps are counted without usage access
- Nothing here.
- No distracting apps chosen yet. Choose them
- Which apps are distracting
- Fewer
- All …
- {budget} unlocks
- Unlocks in a day
- A day on the phone
- A day in distracting apps
- Each day
- Against a day's budget, the same in every month. Tap a day for its time and top apps
- Unlocks
-  on the phone
-  in distracting apps
- Budget: {budgetWords} a day{what}
- , just to keep track
- No budget chosen yet, so {budgetWords} a day{what} is suggested. Choose yours
- … budget
- Today so far: 
- … of {budgetWords}
- No unlocks kept for these days.
- Nothing kept for these days yet.
- {text} ›
- … of it distracting
- Only the day's total is kept this far back.
- No app took a minute.
- The whole day ›
- Close
- your usual one of that weekday
- the same day a year back
- the same day a week before
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
-  · {it}, the most … in an hour
- in distracting apps
- the same as the month before
- ↓ …% on the month before
- ↑ {it}% on the month before
- … a day on the phone
- … a day in distracting apps
- , …% of it
- … in all, over … days with any use
- The month ›
- the same

<sub>[PhoneUse.kt](../../app/src/main/java/app/bedtime/ui/components/PhoneUse.kt)</sub>

- (no text of its own)

## habits

<sub>[HabitsScreen.kt](../../app/src/main/java/app/bedtime/ui/habits/HabitsScreen.kt)</sub>

- Habits
- Kept on this phone
- add a habit
- A habits tab
- Your habits at refuge's foot, beside focus and insights
- Habits you want to keep, ticked off each day: daily, a few times a week, or a count like pages or reps. Add one with the + below; your home and lock screen can show them too.
- Today
- Hold one and drag it to change the order
- fold its steps
- show its steps
- one more
- tick …
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
- Steps
- Optional: what it's made of, like a workout's exercises. Each is ticked on its own on the habits page, and it's done once they all are; the widgets show it as one habit.
- Add a step: squats, lunges…
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

- habits · {done} of …
- … of … today · …
- Open the habit
- days in a row
- best run
- {value} of ……
- Tap a day to tick or untick it.
- Tap a day to change its count above.
- All your habits
- Change it
