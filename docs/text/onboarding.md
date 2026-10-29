# onboarding — all the text, for editing

The two places that introduce the app:

1. **The walk-through:** six pages, shown on first launch and again from **settings → about & backup → the intro again**.
   It lives in [WalkthroughScreen.kt](../../app/src/main/java/app/bedtime/ui/onboarding/WalkthroughScreen.kt).
2. **How it works:** one longer page, reached from **settings → about & backup → how it works** and from the ⓘ at
   the top of any block or schedule. It lives in [OnboardingScreen.kt](../../app/src/main/java/app/bedtime/ui/onboarding/OnboardingScreen.kt).

Edit freely in here and tell me when you're done. I'll copy it into the app.

Everything renders lowercase whatever you type here, so don't worry about capitals. An em dash is fine; the app
uses `—`.

---

# part 1 · the walk-through

**Each page has three parts:**

- **title:** one line, large and light. Keep it short; it wraps badly past about 30 characters.
- **body:** two or three sentences, the idea itself.
- **note:** one sentence against a green rule, for the caveat or reassurance. The shortest of the three.

## page 1 of 6 · the lotus

**title**

> hours that ask less

**body**

> some hours are better spent not reaching for your phone. you choose which ones.

**note**

> no data leaves your phone. no account, no sync — the app has no internet permission at all.

---

## page 2 of 6 · the moon

**title**

> what a session is like

**body**

> the apps you named stay shut. the home screen thins out to what you chose. the color can drain away.

**note**

> notifications wait. music and alarms are never silenced.

---

## page 3 of 6 · the leaf

**title**

> limits for every day

**body**

> outside any session, the apps that pull hardest can ask for a few breaths before they open, or keep to so many
> minutes, or so many opens, a day.

**note**

> making a limit stricter takes effect at once. loosening it, or deleting it, waits until the next day.

---

## page 4 of 6 · the grid · *new*

**title**

> a quieter phone

**body**

> a home screen and a lock screen of their own: the time, the few things you want to see, your apps as words.

**note**

> they can stay when no session runs. you'll find them, and everything else, in settings.

---

## page 5 of 6 · the hourglass

**title**

> the way out is slow on purpose

**body**

> a wait, a passage to copy, a password. long enough for the instinct to pass.

**note**

> each unlock in a day can make the next one longer. the count starts again each morning.

---

## page 6 of 6 · the target

**title**

> as firm as you want

**body**

> every way around a session has a switch of its own. turn them all on and there is no way out but the steps you
> set yourself.

**note**

> the switches sit in each schedule and block, under “the ways around it”.

---

## the buttons

| where | text |
|---|---|
| top right, pages 1–5 | skip |
| bottom, pages 1–5 | next |
| bottom, page 6 | get started |

---

# part 2 · how it works

**Its layout, from top to bottom:**

1. An opening paragraph.
2. Five points about a session. Each point is an icon, a title and a body.
3. **around the sessions:** a heading, a paragraph, and four points about the rest of the app.
4. **how firm to make it:** a heading, a paragraph, and five numbered switches. Each switch is a name, a body,
   and a small tag saying where to find it.
5. A closing line and the **got it** button.

**top bar:** how it works

## opening

> digital refuge sets aside hours when your phone asks less of you. here is what happens in one of them, and how to
> leave if you need to.

## the session, in five points

**the moon · a session begins**

> at the times you set, or whenever you start a block. the apps you named stop opening. your home screen can
> become a short list of what you chose, the screen can fade to grey, and notifications wait. music and alarms are
> never silenced.

**the hourglass · leaving early takes the steps you chose**

> a wait, a passage to type, a password — as many as you want, in that order. enough of a pause for an urge to
> pass. they can grow with each unlock on the same day, and start fresh each morning.

**the lotus · it stays put while it runs**

> a session's own settings can't be changed while it is running. that one isn't a choice: without it, the
> easiest way out would simply be to edit the block.

**the half circle · an emergency break**

> if a block allows one, you can step out for a few minutes without ending the session — for whatever genuinely
> can't wait. it counts as an escape, and a full screen brings you back when it is over.

**the leaf · limits hold every day**

> outside any session too, a limit can ask for a few breaths before an app opens, give it so many minutes a day,
> or so many opens. making one stricter takes effect at once; loosening it, or deleting it, waits until the next
> day, so it can't be used to get round today.

## around the sessions · *new*

**heading**

> around the sessions

**paragraph**

> the rest of the app is there to make the phone itself quieter, in a session or not. all of it is optional, and
> set in settings.

**the house · a quieter home screen**

> the clock, the few things you want to see, and your apps as words. during a session with minimal mode it takes
> over; it can also be your everyday home screen, with a search that finds apps, people and a few things to do.

**the padlock · a lock screen of its own**

> over android's own: the time, what's next today, a quote, and only the notifications from the apps and people
> you pick. a tap, or a swipe from the bottom, unlocks.

**the pencil · notes and habits**

> a thought typed into the search lands in refuge's notes, or in a file of yours. habits are ticked off here or on
> your screens.

**the target · insights**

> what the phone took, how your sessions held and which habits you kept, with a look back at each week. only which
> app was in front is ever counted, never what was in it.

## how firm to make it

**heading**

> how firm to make it

**paragraph**

> two things are always true: while a session runs its settings can't be changed, and leaving early takes the
> steps you chose. everything below is one switch on top of that, in each schedule and block. turn them all on for
> the firmest it gets, or pick only the ones you want.

**1 · harder each time** · tag: *how hard to leave*

> each unlock on the same day makes the wait and the typing longer, by a multiplier you set. it starts fresh each
> morning.

**2 · keep this shut after leaving early** · tag: *the ways around it*

> leave a session early and it stays as it is for what would have been the rest of it, so leaving can't be used to
> soften it.

**3 · no emergency break** · tag: *the ways around it*

> with the break switched off there is no short way out, and the unlock steps are the only way through.

**4 · cover android's off switches** · tag: *the ways around it*

> covers the settings screens that would switch blocking off, and the prompt to uninstall the app.

**5 · full-screen reminder** · tag: *the ways around it*

> if blocking is switched off at all, a full screen stays with you until you switch it back on.

## closing line

> open a block or schedule to set each one, under the heading on its tag. the ⓘ at the top of it brings you back
> here. everything stays on this phone: there's no account, and the app has no internet permission at all.

**button:** got it

---

## adding or removing

- **A walk-through page:** add or delete a `## page` section. The dots at the foot count themselves, and I'll pick
  an icon to match.
- **A point or a switch:** the same; I'll keep the numbering.
