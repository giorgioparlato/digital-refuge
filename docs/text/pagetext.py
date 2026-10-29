# Run from the repository root: python3 docs/text/pagetext.py .
"""Writes docs/text/{focus,limits,insights,settings}.md: each page's branches, and the text in each place."""
import re, pathlib, sys

ROOT = pathlib.Path(sys.argv[1])
SRC = ROOT / "app/src/main/java/app/bedtime"
SKIP_LINE = ("import ", "package ", "//", "*", "/*")
SKIP_IN = ("contentDescription", "onClickLabel", "putExtra", "getStringExtra", "const val", "Regex(", "Log.",
           "Routes.", "route", "SuppressLint", "ofPattern", "Suppress(", "MIME", "application/", "font/", "text/", "Intent(", "Uri.parse", "\"%")

def tidy(t):
    t = re.sub(r"\$\{[^}]*\}", "…", t)
    t = re.sub(r"\$([a-zA-Z_][a-zA-Z0-9_]*)", r"{\1}", t)
    return t.replace("\\n", " ").replace('\\"', '"').replace("\\u2014", "—").replace("\\u201c", "“").replace("\\u201d", "”")

def collapse_templates(src):
    """Each ${...} as one …, its braces counted, so strings inside it don't break the quotes around it."""
    out, i = [], 0
    while i < len(src):
        if src.startswith("${", i):
            depth, j = 1, i + 2
            while j < len(src) and depth:
                depth += {"{": 1, "}": -1}.get(src[j], 0)
                j += 1
            out.append("…")
            i = j
        else:
            out.append(src[i])
            i += 1
    return "".join(out)

def strings(rel):
    src = collapse_templates((SRC / rel).read_text())
    # A sentence split over lines with + is one sentence.
    src = re.sub(r'"\s*\+\s*\n\s*"', "", src)
    src = re.sub(r'"\s*\+\s*"', "", src)
    out = []
    for line in src.split("\n"):
        s = line.strip()
        if s.startswith(SKIP_LINE) or any(k in line for k in SKIP_IN):
            continue
        for m in re.finditer(r'"((?:[^"\\]|\\.)*)"', line):
            t = m.group(1)
            if not re.search(r"[A-Za-z]{2,}", t) or re.search(r" AND |[!<>]?= |\bIN \(", t):
                continue
            words = t.strip().split()
            if len(words) == 1:
                # Single words in code are mostly keys; the ones shown start with a capital.
                if not t[:1].isupper() or re.search(r"[._:/#@=]", t) or t.isupper():
                    continue
            elif re.fullmatch(r"[\w./:-]+", t):
                continue
            t = tidy(t)
            if t not in out:
                out.append(t)
    return out

def block(title, files, note=None, level=2):
    lines = [f"{'#' * level} {title}", ""]
    if note:
        lines += [note, ""]
    for f in files:
        texts = strings(f)
        lines += [f"<sub>[{f.split('/')[-1]}](../../app/src/main/java/app/bedtime/{f})</sub>", ""]
        lines += [f"- {t}" for t in texts] or ["- (no text of its own)"]
        lines.append("")
    return lines

HEADER = """Every screen this tab leads to, in the order you reach them. The tree comes first; below it, each place
lists the words on it, in the order they appear in the code.

- **Placeholders:** `{name}` is filled in by the app (a block's name, a number…), and `…` is a value worked out
  there and then.
- **Case:** the app shows everything lowercase, whatever is written here.
- **Variants:** some lines are alternatives (on / off, one / many); they're listed one after another.
- **Editing:** edit any line in place and tell me when you're done. I'll carry the changes into the app. This file
  is written from the code, so after that I'll write it again.
"""

def doc(name, title, tree, sections):
    lines = [f"# {title}", "", HEADER, "## the tree", "", "```", tree.strip("\n"), "```", ""]
    for s in sections:
        lines += block(*s)
    (ROOT / f"docs/text/{name}.md").write_text("\n".join(lines).rstrip() + "\n")
    print(name, sum(1 for l in lines if l.startswith("- ")), "lines of text")

doc("focus", "focus — the first tab", """
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
""", [
    ("focus", ["ui/home/HomeScreen.kt", "ui/schedules/ScheduleItem.kt"]),
    ("what's new", [], "Each update's lines are in [Changelog.kt](../../app/src/main/java/app/bedtime/data/Changelog.kt), newest first."),
    ("notes", ["ui/notes/NotesScreen.kt", "ui/notes/Markdown.kt"]),
    ("new: templates", ["ui/create/CreateScreen.kt", "data/Templates.kt"]),
    ("the editor", ["ui/edit/ScheduleEditScreen.kt", "ui/edit/UnlockStepsEditor.kt", "ui/components/MessageChoice.kt", "data/CoverMessages.kt"]),
    ("leave early", ["unlock/UnlockFlow.kt", "unlock/UnlockChoice.kt"]),
    ("while a session runs: the covers", ["ui/blocked/BlockedActivity.kt", "ui/blocked/CoverLayout.kt"]),
    ("while a session runs: the minimal home screen", ["ui/minimal/MinimalHomeActivity.kt", "ui/minimal/SessionHeader.kt", "ui/minimal/EmergencyContent.kt", "ui/minimal/AppDrawer.kt", "ui/minimal/AppActions.kt", "ui/minimal/BlockSheet.kt", "ui/minimal/WidgetSheet.kt", "ui/minimal/ScreenSheet.kt", "ui/minimal/CalendarLine.kt", "ui/minimal/CalendarWeek.kt", "ui/minimal/WeatherLine.kt", "ui/notes/NoteWidgetView.kt", "ui/quotes/QuoteMenu.kt"]),
    ("while a session runs: the lock screen", ["ui/lock/LockScreenActivity.kt", "ui/lock/LockNotificationViews.kt"]),
    ("while a session runs: notification, reminder, tile and widget", ["service/SessionNotifier.kt", "service/TakeoverOverlay.kt", "service/FocusTileService.kt", "ui/widget/BlockWidget.kt", "ui/widget/BlockWidgetConfigActivity.kt"]),
])

doc("limits", "limits — the second tab", """
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
""", [
    ("limits", ["ui/limits/LimitsScreen.kt"]),
    ("the editor", ["ui/limits/LimitEditScreen.kt"], "The steps for more use the same rows as a block's steps: see the editor in focus.md."),
    ("outside the app: the pause and the cover", ["ui/blocked/LimitCovers.kt"]),
    ("outside the app: the nudge", ["ui/blocked/NudgeActivity.kt"]),
])

doc("insights", "insights — the third tab", """
insights
├── your week: in refuge · sessions · on the phone · distracting · most of that · habits
│   └── a note on sunday evening (switch) ── the notification
├── phone: day / week / month · against before, usual or a year back · by hour · top apps · unlocks
│   └── no usage access yet → allow it
├── sessions: the last 14 days · kept in a row · each block and schedule
└── habits: today first · each habit → its month · new habit (round +) · hold to reorder
""", [
    ("your week", ["ui/stats/LookBackCard.kt", "service/LookBackNotifier.kt"]),
    ("phone and sessions", ["ui/stats/StatsScreen.kt", "ui/stats/PhoneUseSections.kt", "ui/components/PhoneUse.kt"]),
    ("habits", ["ui/habits/HabitsScreen.kt", "ui/habits/HabitViews.kt"]),
])

doc("settings", "settings — the fourth tab", """
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
""", [
    ("settings, and the pages it opens", ["ui/settings/SettingsScreen.kt"], "Home screen, lock screen, apps, shortcuts and about & backup are lists of rows from this one file."),
    ("permissions & setup", ["ui/setup/SetupScreen.kt"]),
    ("look, widgets, gestures and lock-screen notifications", ["ui/homestyle/HomeStyleScreen.kt", "ui/homestyle/WidgetOptions.kt", "ui/settings/CircleScreen.kt", "ui/settings/PeoplePickerScreen.kt"], "One file serves the look, widgets, gestures and notifications pages, for both screens."),
    ("quotes (from the quote widget)", ["ui/quotes/QuotesScreen.kt"]),
    ("the app list", ["ui/settings/AppListScreen.kt", "ui/apps/AppPickerScreen.kt"]),
    ("search", ["ui/settings/SearchSettingsScreen.kt"]),
    ("apps", ["ui/groups/GroupsScreen.kt", "ui/settings/AlwaysAvailableScreen.kt", "ui/settings/DistractionScreen.kt"]),
    ("notes", ["ui/notes/NoteTargetsScreen.kt"]),
    ("cover messages", ["ui/settings/CoverMessagesScreen.kt"]),
    ("recent problems", ["ui/settings/ProblemsScreen.kt"]),
    ("how it works and the intro", [], "Their text is in [onboarding.md](onboarding.md)."),
])
