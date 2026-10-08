# the intro — all the text, for editing

The four-page walk-through shown on first launch, and again from **settings → the intro again**.
Lives in [WalkthroughScreen.kt](../app/src/main/java/app/bedtime/ui/onboarding/WalkthroughScreen.kt).

Edit freely in here and tell me when you're done — I'll copy it into the app.

**Each page has three parts:**

- **title** — one line, large and light. Keep it short; it wraps badly past about 30 characters.
- **body** — two or three sentences, the idea itself.
- **note** — one sentence against a green rule, for the caveat or reassurance. Shortest of the three.

Everything renders lowercase whatever you type here, so don't worry about capitals. An em dash is
fine; the app uses `—`.

---

## page 1 of 4 · the lotus

**title**

> hours that ask less

**body**

> some hours are better spent not reaching for your phone. you choose which ones.

**note**

> no data leaves your phone. no account, no sync — the app has no internet permission at all.

---

## page 2 of 4 · the moon

**title**

> what a session is like

**body**

> the apps you named stay shut. the home screen thins out to what you chose. the color can drain
> away.

**note**

> notifications wait. music and alarms are never silenced.

---

## page 3 of 4 · the hourglass

**title**

> the way out is slow on purpose

**body**

> a wait, a passage to copy, a password. long enough for the instinct to pass.

**note**

> each unlock in a day can make the next one longer. the count starts again each morning.

---

## page 4 of 4 · the target

**title**

> as firm as you want

**body**

> every way around a session has a switch of its own. turn them all on and there is no way out but
> the steps you set yourself.

**note**

> the switches sit in each schedule and block, under “the ways around it”.

---

## the buttons

| where | now |
|---|---|
| top right, pages 1–3 | skip |
| bottom, pages 1–3 | next |
| bottom, page 4 | get started |

---

## not in here

The longer **"how it works"** page is a different screen
([OnboardingScreen.kt](../app/src/main/java/app/bedtime/ui/onboarding/OnboardingScreen.kt)) with a
lot more text. Say the word and I'll pull that into a file the same way.

If you want to add or remove a page, just add or delete a `## page` section — the dots at the foot
count themselves, and I'll pick an icon to match.
