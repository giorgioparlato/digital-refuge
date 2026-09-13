> **This app was entirely coded by Claude**, Anthropic's AI, working in [Claude Code](https://claude.com/claude-code): every line of Kotlin, the tests, the icon and this README. The idea, the feature requests and the testing on a real phone came from its owner.

# digital refuge

A calm, Obsidian-styled Android app that gives you a refuge from your phone. On a schedule, or for an on-demand focus block, it can:

- **block distracting apps**: a gentle full-screen cover appears over them;
- **turn apps greyscale**, while your minimal home screen can stay in colour;
- **swap your home screen for a minimal launcher** where only the apps you choose can be opened. Its colours, text size and what it shows are yours to customize;
- **quiet the phone** with Do Not Disturb ("Priority only" or "Silence") and **hold notifications back** until the session ends. Media and alarms are never muted, so music, videos and alarms keep playing.

<p>
  <img src="app/src/test/snapshots/images/app.bedtime_ScreenshotTest_homeActive_dark.png" width="190" alt="Home screen with a focus block running">
  <img src="app/src/test/snapshots/images/app.bedtime_ScreenshotTest_minimalHome_dark.png" width="190" alt="Minimal home screen">
  <img src="app/src/test/snapshots/images/app.bedtime_ScreenshotTest_blocked_dark.png" width="190" alt="A blocked app">
  <img src="app/src/test/snapshots/images/app.bedtime_ScreenshotTest_unlockText_dark.png" width="190" alt="Typing challenge to unlock early">
  <img src="app/src/test/snapshots/images/app.bedtime_ScreenshotTest_minimalHomeCustom_light.png" width="190" alt="Customized minimal home screen">
</p>

## Features

- **Schedules and focus blocks.** Schedules repeat on days and times. Blocks start whenever you like and run for a set length.
- **Templates**, always available from **New**:
  - schedules: Bedtime, Work focus, Slow morning, Weekend detox;
  - blocks: Focus 60, Deep work 90, Pomodoro 25, Study 45.
- **Leaving early takes effort**, Cold Turkey style. Stack any mix of steps, completed in this order:
  1. wait out a timer;
  2. type random text (no pasting, no typos);
  3. enter a password.

  Each schedule decides whether unlocking **ends the session** or **pauses it for N minutes**. Optionally, the text gets 50% longer with each early unlock in a day.
- **No quick escapes mid-session.** While a session is running, you can't edit or delete its schedule. If Do Not Disturb or colour correction is switched off from quick settings, the app switches it straight back on and tells you why.
- **Emergency button** on the minimal home screen, which keeps the session in place. It offers calling someone, and your **always-available apps** (maps, rides, authenticators…), which no session ever blocks.
- **Easy number picking:** tap − or + for one step, hold to keep going faster, or tap the number to type a value or pick a preset.
- **Safer defaults:** Settings, Phone and Messages start out allowed in minimal mode, and the app picker explains why.
- **App groups** ("Social & feeds", "Essentials", or your own) let you tick many apps in one tap.
- **Stats:** a streak, time spent in refuge this week, and early unlocks.
- **Quick Settings tile** that starts your favourite focus block.
- **Design:** based on the [Primary](https://primary-theme.github.io/start-here/) Obsidian theme, with green accents and the Inter font. Dark mode uses neutral grays around `#282828`; light mode uses Primary's cream palette. All text in the app is lowercase, and the icon is a small temple with a warmly lit doorway.

## Install

This is a personal, sideloaded app. Google Play doesn't allow accessibility-based blockers like this one.

1. Install [Android Studio](https://developer.android.com/studio) (or just the Android SDK and a JDK 17).
2. Turn on USB debugging on the phone (Settings → About phone → tap *Build number* 7× → Developer options → USB debugging).
3. Build and install:

   ```sh
   ./gradlew installDebug
   ```

   Or build the APK with `./gradlew assembleDebug` and install `app/build/outputs/apk/debug/app-debug.apk`.

## One-time phone setup

**Settings → Permissions & setup** inside the app shows the live status of each step.

1. **Accessibility service (required):** Settings → Accessibility → *digital refuge blocker* → On. On Android 13+, sideloaded apps are "restricted", so if the toggle is greyed out:
   1. Go to App info → digital refuge → ⋮.
   2. Tap **Allow restricted settings**.
   3. Try the toggle again.
2. **Greyscale (optional):** Android has no public greyscale API, so the app uses the system colour-correction filter in monochrome mode. That needs a permission only ADB can grant:

   ```sh
   adb shell pm grant app.bedtime android.permission.WRITE_SECURE_SETTINGS
   ```

   The permission survives reboots and updates. If you use colour correction yourself, your setting is restored after each session.
3. **Do Not Disturb access (optional):** needed for silencing the phone and holding notifications. Grant it from the setup screen.
4. **Always-available apps (recommended):** choose the few apps that stay usable during every session, like maps, rides and your authenticator. You reach them from the emergency button, and can change them later in Settings → Emergency.
5. **Battery:** if blocking stops after a while, set the app's battery usage to *Unrestricted*. Some phone makers aggressively stop background apps.

**Last resort,** if you're ever truly stuck: restart the phone in safe mode (power menu → press and hold *Power off*). Downloaded apps don't run there, so you can uninstall the app like any other.

## Development

```sh
./gradlew testDebugUnitTest      # logic tests: schedules, stats, challenges, DND policy…
./gradlew recordPaparazziDebug   # renders every screen, dark and light, to app/src/test/snapshots/images/
./gradlew verifyPaparazziDebug   # fails if a screen changed unexpectedly
./gradlew lintDebug
```

The screens are checked with [Paparazzi](https://github.com/cashapp/paparazzi) screenshots rather than an emulator. Every screen has a stateless `…Content` composable, which is what the screenshot tests render.

| Piece | Where |
|---|---|
| Models and storage (JSON in DataStore) | `data/Models.kt`, `data/Repository.kt` |
| Templates, app groups | `data/Templates.kt`, `data/Groups.kt` |
| Pure "what's active now" logic (midnight crossing, blocks, overrides) | `engine/ScheduleEvaluator.kt` |
| Live state, re-evaluated at each boundary | `engine/Engine.kt` |
| Stats and streaks | `engine/Stats.kt` |
| Foreground-app watcher: blocking, minimal mode, greyscale, Do Not Disturb | `service/BlockerService.kt` |
| Greyscale and Do Not Disturb | `service/GreyscaleController.kt`, `service/DndController.kt` |
| Quick Settings tile | `service/FocusTileService.kt` |
| Unlock challenges and escalation | `unlock/` |
| Screens and the Primary-based theme | `ui/` |

The internal package name is still `app.bedtime`, from the app's first name, so updates install over existing copies.

**Strictness is deliberately moderate.** There's no Device Admin, and Android doesn't let any app lock you out of Settings. A determined person can always switch the accessibility service off. The app is designed to add friction to impulses, not to be a prison.
