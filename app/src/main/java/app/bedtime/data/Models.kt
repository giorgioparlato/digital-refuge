package app.bedtime.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.UUID

val AppJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

@Serializable
enum class ScheduleKind { RECURRING, BLOCK }

/** Ordered weakest → strongest, so the strongest active mode wins. */
@Serializable
enum class DndMode { OFF, PRIORITY, SILENCE }

/**
 * A set of restrictions plus when they apply: on repeating days and times ([ScheduleKind.RECURRING]),
 * or on demand for a set length ([ScheduleKind.BLOCK]).
 */
@Serializable
data class Schedule(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Bedtime",
    /** Its mark on the session pill and in the lists, one of [app.bedtime.ui.components.ScheduleIcons]; the moon if null. */
    val icon: String? = null,
    val kind: ScheduleKind = ScheduleKind.RECURRING,
    val enabled: Boolean = true,
    /** ISO day-of-week numbers (1 = Monday … 7 = Sunday) on which a session *starts*. */
    val days: Set<Int> = (1..7).toSet(),
    val startMinute: Int = 22 * 60,
    /** Minute of day the session ends; if <= [startMinute] it ends the following day. */
    val endMinute: Int = 7 * 60,
    /** Default length of a block, in minutes. */
    val durationMinutes: Int = 60,
    /** What it shuts, by name ("these apps"); not used with [allowOnly]. */
    val blockedApps: Set<String> = emptySet(),
    val greyscale: Boolean = true,
    /** Refuge's quiet home screen while it runs, showing [homeApps]. */
    val minimalMode: Boolean = false,
    /** What "all but these" lets open ([allowsOnly]). */
    val allowedApps: Set<String> = emptySet(),
    /**
     * "All but these": everything shut but [allowedApps]. Null on schedules saved before it was a choice
     * of its own, when the quiet home screen ([minimalMode]) meant it.
     */
    val allowOnly: Boolean? = null,
    /** The apps on its quiet home screen; null for the ones it lets open ([quietHomeDefault]). */
    val homeApps: Set<String>? = null,
    val dnd: DndMode = DndMode.OFF,
    /** Hide filtered notifications until the session ends (uses Do Not Disturb's visual suppression). */
    val hideNotifications: Boolean = false,
    /** What refuge's lock screen shows while this runs: your usual choice, its own apps and people, or none. */
    val lockNotes: LockNotes = LockNotes.USUAL,
    val lockNoteApps: Set<String> = emptySet(),
    val lockNotePeople: Set<String> = emptySet(),
    /** With [LockNotes.OWN], the circles ([Circle.id]) whose notifications show too. */
    val lockNoteCircles: Set<String> = emptySet(),
    /** Its own widgets on the minimal and lock screens while it runs, in order; null keeps your usual ones. */
    val homeModules: List<ModuleChoice>? = null,
    val lockModules: List<ModuleChoice>? = null,
    val unlock: UnlockConfig = UnlockConfig(),
    val unlockAction: UnlockAction = UnlockAction(),
    /** Offer a short break for apps that won't run while blocking is on. Off means no way out but unlocking. */
    val pauseEnabled: Boolean = true,
    /** Allow editing right after an early unlock. Off keeps settings shut until the session would have ended. */
    val editAfterUnlock: Boolean = false,
    /** While this runs, "why am i unlocking my phone right now?" after each unlock; with [askWhyWords], at least that many words. */
    val askWhy: Boolean = false,
    val askWhyWords: Int = 0,
    /** "How do you feel now?" once an unlock's break is over. */
    val moodAfterUnlock: Boolean = false,
    /** How long each of those breaks lasts, in minutes (at least 1). */
    val breakMinutes: Int = 1,
    /** Hours outside which this can't be changed at all; null means it can be changed any time. */
    val editWindow: EditWindow? = null,
    /** Cover the Settings screens that switch blocking off or uninstall the app, while this runs. */
    val lockSettings: Boolean = true,
    /** Take over the screen if blocking is switched off while this runs, until it's back on. */
    val fullScreenAlert: Boolean = true,
    /** The words on the cover when you open an app this blocks. */
    val message: CoverMessage = CoverMessage(),
) {
    val isBlock: Boolean get() = kind == ScheduleKind.BLOCK

    /** Shuts everything but [allowed], rather than [blockedApps]. */
    val allowsOnly: Boolean get() = allowOnly ?: minimalMode

    /** The apps it shuts by name: none with "all but these", where whatever isn't [allowed] is shut. */
    val shut: Set<String> get() = if (allowsOnly) emptySet() else blockedApps

    /** What "all but these" lets open. Saved before it was a choice, an app on both lists was shut, so it's left out. */
    val allowed: Set<String> get() = if (allowOnly == null) allowedApps - blockedApps else allowedApps

    /**
     * As the editor shows it, doing just what it did: "all but these" stated, the allowed apps without
     * any that were shut too, and notifications kept out of sight while everyone could ring (which
     * held them under priority) read as priority.
     */
    fun settled(): Schedule = copy(
        allowOnly = allowsOnly,
        allowedApps = allowed,
        dnd = if (dnd == DndMode.OFF && hideNotifications) DndMode.PRIORITY else dnd,
    )
}

/**
 * The only hours of the day in which a schedule can be edited, switched off or deleted — so a
 * bedtime can be put beyond reach of the evening that would talk you out of it. Equal times mean
 * the whole day, as they do for a session.
 */
@Serializable
data class EditWindow(val startMinute: Int = 9 * 60, val endMinute: Int = 17 * 60)

@Serializable
data class UnlockConfig(
    val waitEnabled: Boolean = false,
    /** Legacy wait length in whole minutes; kept so older saves still load. Use [waitDurationSeconds]. */
    val waitMinutes: Int = 10,
    /** Wait length in seconds; null on old data, where [waitMinutes] is used instead. */
    val waitSeconds: Int? = null,
    val textEnabled: Boolean = true,
    val textLength: Int = 200,
    /** Random letters, plain words, or a passage worth reading while you copy it. */
    val textSource: TextSource = TextSource.LETTERS,
    /** Writing at least [writeWords] words, from a prompt of the sections named in [writeSections] (none: the "why" question). */
    val writeEnabled: Boolean = false,
    val writeWords: Int = 30,
    val writeSections: List<String> = emptyList(),
    val passwordEnabled: Boolean = false,
    val passwordHash: String? = null,
    val passwordSalt: String? = null,
    /** Each early unlock of this schedule within a day makes the wait and text harder. */
    val escalate: Boolean = true,
    /** How much harder per early unlock in a day, e.g. 1.5× or 2×. */
    val escalateFactor: Float = 1.5f,
) {
    val hasPassword: Boolean get() = passwordHash != null && passwordSalt != null

    /** The wait timer's length in seconds, from the new field or the legacy minutes. */
    val waitDurationSeconds: Int get() = waitSeconds ?: (waitMinutes * 60)
}

/** What a typing challenge asks you to copy out. */
@Serializable
enum class TextSource { LETTERS, WORDS, PASSAGES }

/** Notifications on refuge's lock screen during a session: as usual, from its own apps and people, or none. */
@Serializable
enum class LockNotes { USUAL, OWN, NONE }

/** When "why?" is asked: never, only while a session runs, or always. */
@Serializable
enum class AskWhen { OFF, SESSIONS, ALWAYS }

/** What a limit's pause before opening asks: a wait, typing a little, or "why am I opening this?". */
@Serializable
enum class PauseKind { WAIT, TYPING, ASK }

/** What a completed unlock gives: the rest of the session, a set break, or a length chosen there and then. */
@Serializable
enum class UnlockMode { END_SESSION, PAUSE, CHOOSE }

@Serializable
data class UnlockAction(
    val mode: UnlockMode = UnlockMode.END_SESSION,
    val pauseMinutes: Int = 15,
)

/** Result of a completed unlock: either the specific occurrence was ended, or the schedule is paused. */
@Serializable
data class ScheduleOverride(
    val endedOccurrenceStart: Long? = null,
    val pausedUntil: Long? = null,
)

/** A running block, as epoch-millis [start, end). */
@Serializable
data class Run(val start: Long, val end: Long)

/** Do Not Disturb state from before a session changed it, restored afterwards. */
@Serializable
data class SavedZen(
    val filter: Int,
    val categories: Int,
    val callSenders: Int,
    val messageSenders: Int,
    val suppressedEffects: Int,
    val conversationSenders: Int = -1,
)

// Older saves also carry "pendingWaits" and "savedDaltonizer", from a wait timer that used to be
// persisted and a greyscale route that needed a permission the app no longer asks for. The decoder
// ignores unknown keys, so they are simply dropped.
@Serializable
data class RuntimeState(
    val overrides: Map<String, ScheduleOverride> = emptyMap(),
    val activeRuns: Map<String, Run> = emptyMap(),
    /** Non-null means Do Not Disturb is currently managed by us. */
    val savedZen: SavedZen? = null,
    /** Events hidden from the event widget until they're over: their key ([app.bedtime.ui.minimal.CalendarEvent.key]) and end. */
    val hiddenEvents: Map<String, Long> = emptyMap(),
)

/** Size of app names in the minimal home's list, on top of [TextSize]; small sizes fit many apps and a quote. */
@Serializable
enum class AppNameSize(val scale: Float) { VERY_SMALL(0.55f), SMALL(0.75f), MEDIUM(1f), LARGE(1.2f) }

@Serializable
enum class TextSize(val scale: Float) { SMALL(0.85f), MEDIUM(1f), LARGE(1.2f) }

/** The clock's own size, apart from the rest of the text, so it can go well below it. */
@Serializable
enum class ClockSize(val scale: Float) { VERY_SMALL(0.5f), SMALL(0.7f), MEDIUM(1f), LARGE(1.2f) }

/** Room above the greeting and clock, in dp, under the phone's own status bar. Medium is how it always was. */
enum class TopSpace(val dp: Int) { NONE(0), SMALL(20), MEDIUM(56), LARGE(96) }

/** How long a quote stays before the next one comes round. */
@Serializable
enum class QuoteRefresh(val hours: Int) { HOURLY(1), SIX_HOURS(6), DAILY(24), WEEKLY(168) }

/** The minimal home's green: the app's sage text green. */
const val DEFAULT_HOME_ACCENT = 0xFF86BC9F

/** Its green before the sage palette (v0.8.3). Still on offer as "bright green" once a style has moved on. */
const val OLD_HOME_ACCENT = 0xFF4EC68E

fun HomeStyle.withCurrentGreen(): HomeStyle = if (accent == OLD_HOME_ACCENT) copy(accent = DEFAULT_HOME_ACCENT) else this

/**
 * The lock screen's own choices where it differs from the home screen: its look, and the options of
 * widgets both screens share. Each null follows the home screen's.
 */
@Serializable
data class LockLook(
    val background: Long? = null,
    val accent: Long? = null,
    val textColor: Long? = null,
    val textSize: TextSize? = null,
    val font: AppFont? = null,
    val quoteFont: AppFont? = null,
    val clockSize: ClockSize? = null,
    val quoteRefresh: QuoteRefresh? = null,
    val noteLines: Int? = null,
    val noteTextSize: TextSize? = null,
    val noteBackground: Long? = null,
    val noteDarkText: Boolean? = null,
    val calendarTomorrow: Boolean? = null,
    val habitsDots: Boolean? = null,
    val noteBesideShare: Int? = null,
    val habitsDue: Boolean? = null,
    val habitsKeepDone: Boolean? = null,
    val habitsLabel: Boolean? = null,
    val habitsPlace: HabitsPlace? = null,
)

/** A button at the home screen's foot. Unlock and emergency are always there in sessions; the rest are chosen. */
@Serializable
enum class FootButton {
    MARK, UNLOCK, ARROW, NOTES, HABITS, INSIGHTS, EMERGENCY;

    val fixed: Boolean get() = this == UNLOCK || this == EMERGENCY
}

object FootButtons {
    /** Chosen ones at most, so with unlock and emergency there are never more than five. */
    const val MAX_CHOSEN = 3

    /** [list] with [button] added at the end, if there's room for it. */
    fun with(list: List<FootButton>, button: FootButton): List<FootButton> =
        if (button in list || list.count { !it.fixed } >= MAX_CHOSEN) list else list + button

    fun without(list: List<FootButton>, button: FootButton): List<FootButton> = if (button.fixed) list else list - button

    /**
     * The buttons gathered at the ends: half at the left, half at the right, an odd one out in the middle
     * (as the arrow always sat between unlock and emergency).
     */
    fun gathered(count: Int): Triple<Int, Int, Int> = Triple(count / 2, count % 2, count / 2)
}

/** Where the habits widget's dots sit: a row of their own, under the date, or beside the clock. */
@Serializable
enum class HabitsPlace { ROW, UNDER_DATE, BESIDE_CLOCK }

/** A lock-screen notification swiped aside: cleared, kept for later, marked read, quieted for a while, or answered. */
@Serializable
enum class NoteSwipe { CLEAR, LATER, MARK_READ, QUIET, REPLY }

/** Look of the minimal home screen. Colours are ARGB. */
@Serializable
data class HomeStyle(
    val background: Long = 0xFF282828,
    val accent: Long = DEFAULT_HOME_ACCENT,
    val textSize: TextSize = TextSize.MEDIUM,
    val appNameSize: AppNameSize = AppNameSize.MEDIUM,
    /** The full app list behind a swipe up: its app names, and the search's suggestions at the same size. */
    val listSize: AppNameSize = AppNameSize.MEDIUM,
    val showGreeting: Boolean = true,
    val showDate: Boolean = true,
    val showIcons: Boolean = false,
    /** The refuge mark at the bottom left of the minimal screen, a tap into the app. */
    val showLogo: Boolean = false,
    /** The ⌃ at the foot that opens all apps; swiping up works either way. */
    val showSwipeArrow: Boolean = true,
    /** A quote under the session, on the minimal home and the lock screen. */
    val showQuote: Boolean = true,
    /** How often that quote changes. */
    val quoteRefresh: QuoteRefresh = QuoteRefresh.DAILY,
    /** During sessions, show the clock and session over the lock screen. */
    val lockScreen: Boolean = true,
    /** Outside sessions too, when digital refuge is your launcher. */
    val lockAlways: Boolean = false,
    /** Android's status bar hidden on the minimal and lock screens; a swipe from the top edge still shows it. */
    val hideStatusBar: Boolean = false,
    /** Notifications on that lock screen: those the session lets through, as private as Android's own. Needs notification access. */
    val lockNotifications: Boolean = false,
    /** The apps whose notifications that lock screen may show; none until you pick them. */
    val lockNotificationApps: Set<String> = emptySet(),
    /** People whose notifications show too, from any app; from 0.9.21 only the start of the first circle. */
    val lockNotificationPeople: Set<String> = emptySet(),
    /** The people and group chats that matter most, first circle first; null until changed (see [Circles]). */
    val circles: List<Circle>? = null,
    /** Ready-made replies to a message from the lock screen; typing your own works too. */
    val quickReplies: List<String> = DEFAULT_QUICK_REPLIES,
    /** What swiping a notification aside does, one way and the other. */
    val lockSwipeLeft: NoteSwipe = NoteSwipe.CLEAR,
    val lockSwipeRight: NoteSwipe = NoteSwipe.CLEAR,
    /** The weather after the date, as Breezy Weather sends it. */
    val showWeather: Boolean = false,
    /** The row of today's 24 hours on the minimal screen, and on the lock screen. */
    val hoursOnHome: Boolean = false,
    val hoursOnLock: Boolean = false,
    /** The dots' colours: calm hours, and distracted ones; null follows the text and the accent. */
    val hourCalmColor: Long? = null,
    val hourDistractedColor: Long? = null,
    /** How much of it: now, today's range too, or three days with their skies. */
    val weatherStyle: WeatherStyle = WeatherStyle.TODAY,
    /** The lock screen's own weather style; null takes the home screen's [weatherStyle]. */
    val lockWeatherStyle: WeatherStyle? = null,
    /** Celsius, Fahrenheit, or the usual one where the phone is set. */
    val weatherUnit: TempUnit = TempUnit.AUTO,
    /** A notification at [rainAlertMinute] when today's chance of rain reaches [rainAlertFrom] percent. */
    val rainAlert: Boolean = false,
    val rainAlertMinute: Int = 7 * 60 + 30,
    val rainAlertFrom: Int = 10,
    /** The clock, date and app names. */
    val font: AppFont = AppFont.INTER,
    /** The quote, chosen apart from the rest: Inter as it always was, or a serif in italics. */
    val quoteFont: AppFont = AppFont.INTER,
    /** The name of the font file you added, if any; the file itself lives in the app's own storage. */
    val customFontName: String? = null,
    val clockSize: ClockSize = ClockSize.MEDIUM,
    val topSpace: TopSpace = TopSpace.MEDIUM,
    /** The text colour; null picks for you: cream on a dark background, ink on a light one. */
    val textColor: Long? = null,
    /**
     * What a lock-screen gesture does: a swipe from around the clock, or a double tap anywhere. A package
     * name (opened after unlocking), [GESTURE_CAMERA], or [HomeActions.NOTE] / [HomeActions.NOTIFICATIONS].
     */
    val lockGestures: Map<Swipe, String> = emptyMap(),
    /** What a gesture on the home screen does: one of [HomeActions], or a package name to open. */
    val homeGestures: Map<HomeGesture, String> = mapOf(HomeGesture.SWIPE_DOWN to HomeActions.NOTIFICATIONS),
    /** Today's next event under the date, read from the calendar already on the phone. */
    val showCalendar: Boolean = false,
    /** Whether the lock screen names the event, or only says when it is. */
    val calendarNamesOnLock: Boolean = false,
    /**
     * The widgets on the minimal screen and the lock screen, in order; null until changed, following the
     * older switches above until then. Read through [Modules].
     */
    val homeModules: List<ModuleChoice>? = null,
    val lockModules: List<ModuleChoice>? = null,
    /** The refuge note the note widget shows; null shows the to-do note. */
    val noteWidgetId: String? = null,
    /** How many of the note's lines the note widget shows. */
    val noteLines: Int = 6,
    /** The note widget's own card colour, ARGB; null leaves it on the screen's background. */
    val noteBackground: Long? = null,
    /** On that card, dark words or light ones; null suits them to the card. */
    val noteDarkText: Boolean? = null,
    /** The size of the note widget's words; beside the clock, a step smaller still. */
    val noteTextSize: TextSize = TextSize.MEDIUM,
    /** The note beside the clock rather than in the list, on the home screen and on the lock screen; only with a small clock and no weather beside it. */
    val noteBesideClock: Boolean = false,
    val lockNoteBesideClock: Boolean = false,
    /** A notes button at the bottom right of the minimal screen, opening refuge notes. */
    val showNotesButton: Boolean = false,
    /** The buttons at the foot, in their order; null until changed, following the switches above until then. Read through [foot]. */
    val footButtons: List<FootButton>? = null,
    /** "unlock" and "emergency" written beside their icons at the foot; off, the icons alone. */
    val footWords: Boolean = true,
    /** The foot's buttons spread evenly across the width rather than gathered at its ends. */
    val footSpread: Boolean = false,
    /** With nothing coming today, a small calendar icon where the next event would be, opening the week ahead; on each screen. */
    val calendarIconWhenEmpty: Boolean = true,
    val lockCalendarIconWhenEmpty: Boolean = true,
    /** Once today's events are done, tomorrow's first one in their place, to get ready for the morning. */
    val calendarTomorrow: Boolean = false,
    /** The habits widget as a row of dots, today's for each, rather than a line a habit. */
    val habitsDots: Boolean = false,
    /** The note beside the clock: its share of the width, in percent; 0 leaves the clock what it needs and the note the rest. */
    val noteBesideShare: Int = 0,
    /** The habits widget shows what today asks for to keep each run going, rather than the habits chosen for it. */
    val habitsDue: Boolean = false,
    /** Showing what's due: those done today stay, filled in, rather than going. */
    val habitsKeepDone: Boolean = true,
    /** The dots' words, "habits · 2 of 5"; off, only the dots. */
    val habitsLabel: Boolean = true,
    /** Where the dots sit; under the date and beside the clock only where those are shown (beside, as the note can). */
    val habitsPlace: HabitsPlace = HabitsPlace.ROW,
    /** Room above the lock screen's clock; null takes the home screen's [topSpace]. */
    val lockTopSpace: TopSpace? = null,
    /** How the distraction tracker looks on the home screen, and on the lock screen (null: as on home). */
    val useWidget: UseWidget = UseWidget(),
    val lockUseWidget: UseWidget? = null,
    /** The lock screen's own look and widget options, over these. */
    val lock: LockLook = LockLook(),
) {
    /** This style as [screen] shows it: on the lock screen, its own choices over the home screen's. */
    fun on(screen: Screen): HomeStyle = if (screen == Screen.HOME) this else copy(
        background = lock.background ?: background,
        accent = lock.accent ?: accent,
        textColor = lock.textColor ?: textColor,
        textSize = lock.textSize ?: textSize,
        font = lock.font ?: font,
        quoteFont = lock.quoteFont ?: quoteFont,
        clockSize = lock.clockSize ?: clockSize,
        quoteRefresh = lock.quoteRefresh ?: quoteRefresh,
        noteLines = lock.noteLines ?: noteLines,
        noteTextSize = lock.noteTextSize ?: noteTextSize,
        noteBackground = lock.noteBackground ?: noteBackground,
        noteDarkText = lock.noteDarkText ?: noteDarkText,
        calendarTomorrow = lock.calendarTomorrow ?: calendarTomorrow,
        habitsDots = lock.habitsDots ?: habitsDots,
        noteBesideShare = lock.noteBesideShare ?: noteBesideShare,
        habitsDue = lock.habitsDue ?: habitsDue,
        habitsKeepDone = lock.habitsKeepDone ?: habitsKeepDone,
        habitsLabel = lock.habitsLabel ?: habitsLabel,
        habitsPlace = lock.habitsPlace ?: habitsPlace,
    )

    /**
     * [change], made to the style as [screen] shows it. The lock screen's look and widget options are
     * always its own: changed there, they leave the home screen as it was; changed on the home screen,
     * the lock screen keeps what it had. Anything else changes for both, as it always has.
     */
    fun edit(screen: Screen, change: (HomeStyle) -> HomeStyle): HomeStyle {
        if (screen == Screen.HOME) {
            val after = change(this)
            val lockNow = on(Screen.LOCK)
            fun <T> kept(get: (HomeStyle) -> T, own: T?): T? = own ?: get(lockNow).takeIf { get(after) != get(this) }
            return after.copy(
                lock = LockLook(
                    background = kept({ it.background }, lock.background),
                    accent = kept({ it.accent }, lock.accent),
                    textColor = kept({ it.textColor }, lock.textColor),
                    textSize = kept({ it.textSize }, lock.textSize),
                    font = kept({ it.font }, lock.font),
                    quoteFont = kept({ it.quoteFont }, lock.quoteFont),
                    clockSize = kept({ it.clockSize }, lock.clockSize),
                    quoteRefresh = kept({ it.quoteRefresh }, lock.quoteRefresh),
                    noteLines = kept({ it.noteLines }, lock.noteLines),
                    noteTextSize = kept({ it.noteTextSize }, lock.noteTextSize),
                    noteBackground = kept({ it.noteBackground }, lock.noteBackground),
                    noteDarkText = kept({ it.noteDarkText }, lock.noteDarkText),
                    calendarTomorrow = kept({ it.calendarTomorrow }, lock.calendarTomorrow),
                    habitsDots = kept({ it.habitsDots }, lock.habitsDots),
                    noteBesideShare = kept({ it.noteBesideShare }, lock.noteBesideShare),
                    habitsDue = kept({ it.habitsDue }, lock.habitsDue),
                    habitsKeepDone = kept({ it.habitsKeepDone }, lock.habitsKeepDone),
                    habitsLabel = kept({ it.habitsLabel }, lock.habitsLabel),
                    habitsPlace = kept({ it.habitsPlace }, lock.habitsPlace),
                ),
                lockWeatherStyle = lockWeatherStyle ?: weatherStyle.takeIf { after.weatherStyle != weatherStyle },
                lockTopSpace = lockTopSpace ?: topSpace.takeIf { after.topSpace != topSpace },
                lockUseWidget = lockUseWidget ?: useWidget.takeIf { after.useWidget != useWidget },
            )
        }
        val before = on(Screen.LOCK)
        val after = change(before)
        fun <T> own(get: (HomeStyle) -> T, kept: T?): T? = if (get(after) != get(before)) get(after) else kept
        val own = LockLook(
            background = own({ it.background }, lock.background),
            accent = own({ it.accent }, lock.accent),
            textColor = own({ it.textColor }, lock.textColor),
            textSize = own({ it.textSize }, lock.textSize),
            font = own({ it.font }, lock.font),
            quoteFont = own({ it.quoteFont }, lock.quoteFont),
            clockSize = own({ it.clockSize }, lock.clockSize),
            quoteRefresh = own({ it.quoteRefresh }, lock.quoteRefresh),
            noteLines = own({ it.noteLines }, lock.noteLines),
            noteTextSize = own({ it.noteTextSize }, lock.noteTextSize),
            noteBackground = own({ it.noteBackground }, lock.noteBackground),
            noteDarkText = own({ it.noteDarkText }, lock.noteDarkText),
            calendarTomorrow = own({ it.calendarTomorrow }, lock.calendarTomorrow),
            habitsDots = own({ it.habitsDots }, lock.habitsDots),
            noteBesideShare = own({ it.noteBesideShare }, lock.noteBesideShare),
            habitsDue = own({ it.habitsDue }, lock.habitsDue),
            habitsKeepDone = own({ it.habitsKeepDone }, lock.habitsKeepDone),
            habitsLabel = own({ it.habitsLabel }, lock.habitsLabel),
            habitsPlace = own({ it.habitsPlace }, lock.habitsPlace),
        )
        return after.copy(
            background = background, accent = accent, textColor = textColor, textSize = textSize, font = font, quoteFont = quoteFont,
            clockSize = clockSize, quoteRefresh = quoteRefresh, noteLines = noteLines, noteTextSize = noteTextSize,
            noteBackground = noteBackground, noteDarkText = noteDarkText, calendarTomorrow = calendarTomorrow, habitsDots = habitsDots,
            noteBesideShare = noteBesideShare, habitsDue = habitsDue, habitsKeepDone = habitsKeepDone, habitsLabel = habitsLabel,
            habitsPlace = habitsPlace, lock = own,
        )
    }

    /** What a widget's long-press sheet can change, taken from [from]; the rest stays as it is here. */
    fun sheetFields(from: HomeStyle): HomeStyle = copy(
        weatherStyle = from.weatherStyle, lockWeatherStyle = from.lockWeatherStyle, clockSize = from.clockSize, quoteRefresh = from.quoteRefresh,
        noteWidgetId = from.noteWidgetId, noteLines = from.noteLines, noteTextSize = from.noteTextSize, noteBackground = from.noteBackground,
        noteDarkText = from.noteDarkText, noteBesideClock = from.noteBesideClock, lockNoteBesideClock = from.lockNoteBesideClock,
        calendarIconWhenEmpty = from.calendarIconWhenEmpty, lockCalendarIconWhenEmpty = from.lockCalendarIconWhenEmpty,
        calendarTomorrow = from.calendarTomorrow, habitsDots = from.habitsDots, noteBesideShare = from.noteBesideShare,
        habitsDue = from.habitsDue, habitsKeepDone = from.habitsKeepDone, habitsLabel = from.habitsLabel, habitsPlace = from.habitsPlace, useWidget = from.useWidget, lockUseWidget = from.lockUseWidget,
        weatherUnit = from.weatherUnit, calendarNamesOnLock = from.calendarNamesOnLock,
        hourCalmColor = from.hourCalmColor, hourDistractedColor = from.hourDistractedColor,
    )

    /** The foot's buttons in order: as chosen, or as the older switches had them (mark, unlock, arrow, notes, emergency). */
    fun foot(): List<FootButton> = (footButtons ?: listOfNotNull(
        FootButton.MARK.takeIf { showLogo },
        FootButton.UNLOCK,
        FootButton.ARROW.takeIf { showSwipeArrow },
        FootButton.NOTES.takeIf { showNotesButton },
        FootButton.EMERGENCY,
    )).let { list -> list + listOf(FootButton.UNLOCK, FootButton.EMERGENCY).filter { it !in list } }

    fun useWidgetFor(screen: Screen): UseWidget = if (screen == Screen.HOME) useWidget else lockUseWidget ?: useWidget

    fun withUseWidget(screen: Screen, widget: UseWidget): HomeStyle = if (screen == Screen.HOME) copy(useWidget = widget) else copy(lockUseWidget = widget)

    fun weatherStyleFor(screen: Screen): WeatherStyle = if (screen == Screen.HOME) weatherStyle else lockWeatherStyle ?: weatherStyle

    fun withWeatherStyle(screen: Screen, ws: WeatherStyle): HomeStyle = if (screen == Screen.HOME) copy(weatherStyle = ws) else copy(lockWeatherStyle = ws)

    fun noteBeside(screen: Screen): Boolean = if (screen == Screen.HOME) noteBesideClock else lockNoteBesideClock

    fun withNoteBeside(screen: Screen, on: Boolean): HomeStyle = if (screen == Screen.HOME) copy(noteBesideClock = on) else copy(lockNoteBesideClock = on)

    fun calendarIcon(screen: Screen): Boolean = if (screen == Screen.HOME) calendarIconWhenEmpty else lockCalendarIconWhenEmpty

    fun withCalendarIcon(screen: Screen, on: Boolean): HomeStyle = if (screen == Screen.HOME) copy(calendarIconWhenEmpty = on) else copy(lockCalendarIconWhenEmpty = on)

    /** Whether the note can sit beside the clock on [screen]: a small clock, and no weather reading beside it. */
    fun noteFitsBesideClock(screen: Screen): Boolean =
        clockSize in setOf(ClockSize.VERY_SMALL, ClockSize.SMALL) &&
            !(Modules.isOn(this, screen, Module.WEATHER) && weatherStyleFor(screen) != WeatherStyle.FIVE_DAYS)

    /**
     * The Look page's colours, sizes and fonts as they start, for [screen] only, made to the style as that
     * screen shows it (save it through [edit]). Widgets and their options (the clock's size among them),
     * gestures, notifications, the app list and your own font stay as they are.
     */
    fun withDefaultLook(screen: Screen): HomeStyle {
        val d = HomeStyle()
        val look = copy(background = d.background, accent = d.accent, textColor = d.textColor, textSize = d.textSize, font = d.font, quoteFont = d.quoteFont)
        return if (screen == Screen.HOME) look.copy(topSpace = d.topSpace) else look.copy(lockTopSpace = d.topSpace)
    }
}

/**
 * People and group chats whose notifications come first on the lock screen, in its colour: say, "close"
 * and "known". [people] are names as your contacts have them; [groups] are group chats by their name.
 */
@Serializable
data class Circle(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    /** ARGB; null takes the screen's accent. */
    val colour: Long? = null,
    val people: Set<String> = emptySet(),
    val groups: Set<String> = emptySet(),
)

object Circles {
    const val MAX = 3
    const val CLOSE = "close"
    const val KNOWN = "known"

    /**
     * Your circles: as saved, or until then "close" (with the people you'd chosen before circles) and
     * "known", empty.
     */
    fun of(style: HomeStyle): List<Circle> = style.circles ?: listOf(
        Circle(CLOSE, "Close", people = style.lockNotificationPeople),
        Circle(KNOWN, "Known"),
    )
}

/**
 * The lock screen's gestures: four swipes starting around the clock, a double tap anywhere, and one on
 * the clock, which does what a double tap does until it's given something of its own.
 */
@Serializable
enum class Swipe { UP, DOWN, LEFT, RIGHT, DOUBLE_TAP, CLOCK_DOUBLE_TAP }

/** The camera, which unlike any other app can open over the lock screen without unlocking. */
const val GESTURE_CAMERA = "camera"

/** One thing the search can offer, by [SearchOrder] key, and whether it does. */
@Serializable
data class SearchChoice(
    val key: String,
    val on: Boolean,
    /** Kept out of ordinary results: only its keyword reaches it. */
    val keywordOnly: Boolean = false,
) {
    val mode: SearchMode get() = when {
        !on -> SearchMode.OFF
        keywordOnly -> SearchMode.KEYWORD
        else -> SearchMode.EVERY
    }
}

/** How the search reaches an action: in every search, only with its keyword, or not at all. */
enum class SearchMode { EVERY, KEYWORD, OFF }

/** The search's three kinds of action, in the order results show them; each can be switched off whole. */
@Serializable
enum class SearchCategory(val title: String, val hint: String) {
    FIND("Find", "Things on the phone, by their name"),
    DO("Do", "Set, save, start or work something out"),
    LOOK_UP("Look up", "Your words sent to an app or the web"),
}

/** The home screen's gestures; a swipe up always opens every app. A double tap on the clock is a double tap until it's set. */
@Serializable
enum class HomeGesture { SWIPE_DOWN, SWIPE_LEFT, SWIPE_RIGHT, DOUBLE_TAP, CLOCK_DOUBLE_TAP }

/** What a home-screen gesture can do besides opening an app. None has a dot in it, so none is a package. */
object HomeActions {
    const val NOTIFICATIONS = "notifications"
    const val LOCK = "lock"
    const val NOTE = "note"
    const val SEARCH = "search"
    const val NOTES = "notes"
    const val WEEK = "week"
    const val BLOCKS = "blocks"
    const val LOOKUP = "lookup"
    const val JOURNAL = "journal"
    const val VOICE = "voice"
    const val CAMERA = GESTURE_CAMERA

    /** In the order they're offered, with how each reads. The look-up is in the quick note now, so it isn't offered of its own. */
    val all: List<Pair<String, String>> = listOf(
        NOTIFICATIONS to "Show notifications",
        LOCK to "Turn the screen off",
        NOTE to "Write a note or look up a word",
        SEARCH to "Search",
        NOTES to "Your notes",
        JOURNAL to "Microjournal: a line for today",
        VOICE to "Record a voice note",
        WEEK to "The week ahead",
        BLOCKS to "Your blocks, to start one",
        CAMERA to "Camera",
    )

    /** How [action] reads; a gesture still set to the look-up opens the quick note, and reads as it. */
    fun label(action: String?): String? = all.firstOrNull { it.first == (if (action == LOOKUP) NOTE else action) }?.second
}

/** The fonts the minimal home can use: four that ship with the app, and one you add. */
@Serializable
enum class AppFont { INTER, NUNITO, LITERATA, FRAUNCES, SPECTRAL, CUSTOM }

@Serializable
data class AppSettings(
    val homeStyle: HomeStyle = HomeStyle(),
    /** Block the Quick Settings tile starts; null means the first block. */
    val tileBlockId: String? = null,
    val groups: List<AppGroup> = emptyList(),
    /** True once the starter groups have been created, so deleting them sticks. */
    val groupsSeeded: Boolean = false,
    /** Apps no session blocks, reached from the Emergency screen (maps, rides, an authenticator…). */
    val alwaysAvailable: Set<String> = emptySet(),
    val alwaysAvailableSeeded: Boolean = false,
    /** digital refuge is your home screen, not only the minimal one during sessions. */
    val homeScreen: Boolean = false,
    /** The apps on the home screen outside sessions, in your order. */
    val homeFavourites: List<String> = emptyList(),
    /** Apps left out of the home screen's lists and search (they still open, and still follow sessions). */
    val hiddenApps: Set<String> = emptySet(),
    /** Your own names for apps, by package, used on the home screen and in its lists. */
    val appNames: Map<String, String> = emptyMap(),
    /** Apps whose time counts toward the row of 24 hours. */
    val distractingApps: Set<String> = emptySet(),
    /** Minutes in those apps that make an hour a distracted one. */
    val distractionMinutes: Int = 5,
    /** Minutes in any app that make an hour a distracted one too; null leaves it to the distracting apps. */
    val distractionTotalMinutes: Int? = null,
    /** What the statistics compare a period with, as last chosen. */
    val statsVersus: Versus = Versus.LAST,
    /** A nudge after this many minutes in a row on the phone, in any app; null for none. */
    val runNudgeMinutes: Int? = null,
    /** Keywords for the search's choices and for apps ("app:" + package); the suggested ones until [keywordsCustom]. */
    val keywords: Map<String, String> = emptyMap(),
    val keywordsCustom: Boolean = false,
    /** Your habits, in your order. */
    val habits: List<Habit> = emptyList(),
    /** The messaging apps beside a contact in the search, by [ChatApp.key], in order, at most three. */
    val contactApps: List<String> = ChatApp.DEFAULT,
    /** Your Obsidian vault's folder (a tree URI), whose notes' titles the search finds; null until chosen. */
    val obsidianVault: String? = null,
    /** The one folder of files from 0.9.8; kept so it still loads. Use [fileFolders]. */
    val filesFolder: String? = null,
    /** Folders (tree URIs) whose files the search finds by name. */
    val filesFolders: List<String> = emptyList(),
    /** What the search offers after apps, in your order, each on or off; empty until you change it. */
    val searchChoices: List<SearchChoice> = emptyList(),
    /** Whether [searchChoices] have been saved since "only with its keyword" became a state of its own. */
    val searchModesSet: Boolean = false,
    /** Kinds of action switched off whole; each action keeps its own state for when it's back. */
    val searchOff: Set<SearchCategory> = emptySet(),
    /** Which block each home-screen widget starts, by widget id; missing means the tile's block, then the first. */
    val widgetBlocks: Map<String, String> = emptyMap(),
    // Both of these used to live here and now belong to each schedule, since how firmly a session
    // holds is a property of that session and not of the phone. Old saves still carry them; the
    // decoder ignores unknown keys, so they are simply dropped.
    /** True once the one-time explainer has been shown. */
    val onboarded: Boolean = false,
    /** The newest version whose "what's new" has been seen (or skipped by a fresh install). */
    val lastSeenVersion: Int = 0,
    /** Pauses before opening and daily budgets, which hold every day rather than in a session. */
    val limits: List<AppLimit> = emptyList(),
    /** True once a style still on the old default green has moved to the sage — once, so it can be chosen again. */
    val homeGreenMigrated: Boolean = false,
    /**
     * Messages for covers, shared by every schedule, block and limit: until you change the list
     * ([coverMessagesEdited]), the suggested ones and then these, those you wrote; after, all of them.
     */
    val customMessages: List<CustomMessage> = emptyList(),
    val coverMessagesEdited: Boolean = false,
    /** Your own quotes, favourites, hidden ones, and which of them rotate. */
    val quotes: QuotePrefs = QuotePrefs(),
    /** Files quick notes can be added to, besides the app's own notes; at most [Notes.MAX_TARGETS]. */
    val noteTargets: List<NoteTarget> = emptyList(),
    /** Which calendar the search adds events to; null for the first one that can take them. */
    val eventCalendarId: Long? = null,
    /** The folder (a tree URI) a backup is written to each week; null for none. */
    val backupFolder: String? = null,
    /** When the last of those was written. */
    val lastBackupAt: Long = 0L,
    /** The microjournal (its lines, moods and "why?" answers) in backups and exports: only once asked for. */
    val backupJournal: Boolean = false,
    /** A notification looking back on the week, on Sunday evening. */
    val weeklyNote: Boolean = false,
    /** The week (its Monday, as an epoch day) last looked back on in that notification. */
    val weeklyNotedWeek: Long = 0L,
    /** The week whose look back was put away in insights, before its time was up. */
    val lookBackPutAway: Long = 0L,
    /** The dictionary looked in when you have several, by name; null for all of them. */
    val lookUpDictionary: String? = null,
    /** The microjournal in refuge notes, and in the quick note: only once you switch it on. */
    val journalOn: Boolean = false,
    /** Your journalling sections; null until you change one, so the suggested ones ([Journal.SUGGESTED]). */
    val journalSections: List<JournalSection>? = null,
    /** "How do you feel now?" after this many minutes on the phone in one go; 0 never. Schedules and limits have their own. */
    val moodAfterMinutes: Int = 0,
    /** Voice notes, off until switched on; they show anywhere only once a folder and a model are chosen too ([Voice.ready]). */
    val voiceOn: Boolean = false,
    /** Voice notes: the folder their files go in (a document tree); none until chosen, and none means no recording. */
    val voiceFolder: String? = null,
    /** The Whisper model's file name, as chosen; the file itself is copied into refuge's own storage. */
    val voiceModel: String? = null,
    /** What Whisper listens for: "auto", or a language's code. */
    val voiceLanguage: String = "auto",
    /** Where your notes were last exported as Markdown files (a document tree), to export to again. */
    val notesExportFolder: String? = null,
    /** Your habits as a tab of their own at refuge's foot, like notes; off until asked for. */
    val habitsTab: Boolean = false,
    /** Your notes as a tab of their own at refuge's foot, beside insights. */
    val notesTab: Boolean = false,
    /** Insights' habits and moods sections, for those who use them. */
    val insightsSessions: Boolean = true,
    val insightsHabits: Boolean = true,
    val insightsMoods: Boolean = true,
    /** No longer used: the mood chips start folded each time the microjournal opens. Kept so saved settings still read. */
    val journalMoodsShown: Boolean = true,
    /** The prompts a button away while writing (hidden until asked for); off, no button at all. */
    val journalPrompts: Boolean = true,
    /** Whether looking up on the lock screen finds your notes too, to read there without unlocking. */
    val lookUpNotes: Boolean = false,
    /** An app's shortcut run after a quick note goes into one of your files, e.g. a sync app's "Sync now". */
    val afterNoteShortcut: AppShortcut? = null,
    /** That shortcut also when you leave Obsidian, so what you wrote there is sent on too. */
    val syncAfterObsidian: Boolean = false,
    /** "Why am I unlocking my phone right now?" after an unlock; before a limit's apps, it's that limit's pause. */
    val askOnUnlock: AskWhen = AskWhen.OFF,
    /** The questions asked in turn after unlocking, and before a limit's apps; null keeps the suggested ones ([Why]). */
    val whyQuestions: List<String>? = null,
    val whyAppQuestions: List<String>? = null,
    /** Moods to tap when answering and in the microjournal, for the mood tracker; null keeps the suggested ones. */
    val moodsOn: Boolean = true,
    val moodTags: List<String>? = null,
    /**
     * Every session covers Android's screens that switch blocking off, and takes over the screen if
     * blocking is switched off anyway. Null until set here: before, each schedule had its own.
     */
    val coverOffSwitches: Boolean? = null,
    val fullScreenReminder: Boolean? = null,
) {
    /** Whether sessions cover Android's off switches: as set, or as any of [schedules] did before it was one switch. */
    fun coversOffSwitches(schedules: List<Schedule>): Boolean = coverOffSwitches ?: (schedules.isEmpty() || schedules.any { it.lockSettings })

    /** Whether sessions take over the screen when blocking is switched off: on unless switched off here. */
    val remindsFullScreen: Boolean get() = fullScreenReminder ?: true

    /** Where quick notes and the search can add: the to-do note first, then your files. */
    val quickTargets: List<NoteTarget> get() = listOf(Notes.TODO) + noteTargets

    /** These settings without keywords, names or favourites of apps [installed] says are gone. */
    fun withoutGoneApps(installed: (String) -> Boolean): AppSettings = copy(
        keywords = keywords.filterKeys { key -> !key.startsWith(Keywords.APP) || installed(key.removePrefix(Keywords.APP)) },
        appNames = appNames.filterKeys(installed),
        homeFavourites = homeFavourites.filter(installed),
    )

    /** The folders the search finds files in, including the single one chosen before there could be several. */
    val fileFolders: List<String> get() = (listOfNotNull(filesFolder) + filesFolders).distinct()
}

/** Which quotes rotate: the app's and yours, only yours, or only favourites. */
@Serializable
enum class QuoteSource { ALL, MINE, FAVOURITES }

/**
 * How the quotes rotate for you. Favourites and hidden ones are kept by their text, which works for
 * the app's quotes (fixed) and for yours (moved along if you edit one).
 */
@Serializable
data class QuotePrefs(
    val source: QuoteSource = QuoteSource.ALL,
    val mine: List<Quote> = emptyList(),
    val favourites: Set<String> = emptySet(),
    val hidden: Set<String> = emptySet(),
)

/**
 * How a cover picks its words: a different one each time, or one you chose. [USUAL] is no longer
 * offered; saved, it reads as a different one each time.
 */
@Serializable
enum class MessageMode { USUAL, CHOSEN, RANDOM }

/** A cover's words. [text] is the chosen message when [mode] is [MessageMode.CHOSEN]. */
@Serializable
data class CoverMessage(val mode: MessageMode = MessageMode.RANDOM, val text: String? = null)

/** A message you wrote. [inRandom] puts it in the mix for covers set to a different one each time. */
@Serializable
data class CustomMessage(val text: String, val inRandom: Boolean = true)

/**
 * What running out of a limit's daily minutes leads to. Ordered strictest first: choosing there and
 * then can reach the rest of the day too, so it counts as the loosest.
 */
@Serializable
enum class OutOfTime { TOMORROW, MORE_TIME, REST_OF_DAY, CHOOSE }

/**
 * How one limit behaves, every day and not only in a session. Kept apart from its name so that a
 * looser version can be saved now and wait for midnight (see [AppLimit.next]).
 */
@Serializable
data class LimitRules(
    val apps: Set<String> = emptySet(),
    /** Seconds of quiet before one of these apps opens; null means it opens straight away. */
    val pauseSeconds: Int? = 10,
    /** What that pause asks: a wait of [pauseSeconds], or typing [pauseTextLength] characters of [pauseTextSource]. */
    val pauseKind: PauseKind = PauseKind.WAIT,
    val pauseTextLength: Int = 40,
    val pauseTextSource: TextSource = TextSource.WORDS,
    /** Each open today makes the pause this much longer than the one before, in percent; 0 keeps it the same. */
    val pauseGrowth: Int = 0,
    /** With the pause asking why: at least this many words before the app opens; 0 for none. */
    val askWords: Int = 0,
    /**
     * After the pause, how long its apps stay open, coming and going as you like; then the pause again.
     * Null: until you leave them (back within a minute is the same visit).
     */
    val openMinutes: Int? = null,
    /** Whether "open anyway" on one of the apps lets you into all of them, rather than only that one. */
    val pauseForGroup: Boolean = false,
    /** "How do you feel now?" once the time open after the pause is over, or a minute after leaving them. */
    val moodAfter: Boolean = false,
    /** Minutes a day in these apps; null means no budget. */
    val dailyMinutes: Int? = null,
    /** Times a day these apps can be opened; null means no count. Each "more" gives one more. */
    val dailyOpens: Int? = null,
    /** True: the minutes and opens are shared by all the apps together. False: each app gets them in full. */
    val shared: Boolean = true,
    val outOfTime: OutOfTime = OutOfTime.MORE_TIME,
    /** What "more time" gives each time, in minutes. */
    val extraMinutes: Int = 15,
    /** The steps that getting more time, or the rest of the day, takes. */
    val unlock: UnlockConfig = UnlockConfig(),
)

/** A limit on some apps: a pause before opening them, a daily budget, or both. */
@Serializable
data class AppLimit(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val rules: LimitRules = LimitRules(),
    /** Looser rules saved today, which take over from local day [nextFromDay] (an epoch day). */
    val next: LimitRules? = null,
    val nextFromDay: Long = 0,
    /** Deleting is the loosest change of all, so it waits for midnight too. */
    val deleteFromDay: Long? = null,
    /** The day it was made. Until that day ends it can be changed freely: nothing has been given up yet. */
    val createdDay: Long = 0,
    /** Hours outside which this can't be changed or deleted at all; null means any time. */
    val editWindow: EditWindow? = null,
    /** The words on the pause before opening, and on the cover once the day's minutes are gone. */
    val message: CoverMessage = CoverMessage(),
)

/** One day of time spent in limited apps, and what was done about it. Days are local epoch days. */
@Serializable
data class UsageDay(
    val day: Long,
    /** Seconds in front, by package. Only apps that belong to a limit are counted. */
    val seconds: Map<String, Long> = emptyMap(),
    /** Seconds in your distracting apps, by hour of the day (0–23). */
    val hourly: Map<Int, Long> = emptyMap(),
    /** Seconds in any app but digital refuge itself, by hour of the day. */
    val hourlyAll: Map<Int, Long> = emptyMap(),
    /** Seconds in each of your distracting apps, by package. */
    val distractingSeconds: Map<String, Long> = emptyMap(),
    /** Times the pause before opening was shown, by package. */
    val opens: Map<String, Int> = emptyMap(),
    /** When more time was taken, by limit id; lifting for the rest of the day counts here too. */
    val extras: Map<String, List<Long>> = emptyMap(),
    /**
     * How many minutes each of those gave, by limit id. Entries in [extras] without one here are from
     * before a length could be chosen, and gave the limit's set extra minutes.
     */
    val extraMinutes: Map<String, List<Int>> = emptyMap(),
    /** Limits lifted for the rest of this day. */
    val lifted: Set<String> = emptySet(),
    /** Unlocks counted by refuge itself; null on days from before it counted them. */
    val unlocks: Int? = null,
    /** The same, by hour of the day. */
    val unlockHours: Map<Int, Int> = emptyMap(),
    /** Times each limited app was opened (after a minute or more away), for limits that count opens. */
    val launches: Map<String, Int> = emptyMap(),
)

/** A named set of apps for ticking many at once in the app picker. Copied into schedules, never linked. */
@Serializable
data class AppGroup(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val packages: Set<String>,
)

/** One session (an occurrence of a schedule or a run of a block), for stats and escalation. */
@Serializable
data class SessionLog(
    val scheduleId: String,
    val name: String,
    val start: Long,
    val end: Long,
    val unlockTimes: List<Long> = emptyList(),
    val endedEarlyAt: Long? = null,
    /**
     * Times blocking was switched off during this session some other way than an emergency break:
     * from Settings, safe mode, or by taking away "display over other apps". Before v0.8 this also
     * held emergency breaks, which couldn't be told apart then.
     */
    val pausedAt: List<Long> = emptyList(),
    /** Emergency breaks taken during this session, including "take more minutes" on the reminder. */
    val breakTimes: List<Long> = emptyList(),
) {
    val unlocks: Int get() = unlockTimes.size

    /** Early unlocks, emergency breaks and blocking switched off: every way out of a session. */
    val escapes: Int get() = unlockTimes.size + pausedAt.size + breakTimes.size

    /** Every moment this session was escaped, of whichever kind. */
    val escapeTimes: List<Long> get() = unlockTimes + pausedAt + breakTimes
}

/** The quick replies to start with, all of them editable. */
val DEFAULT_QUICK_REPLIES = listOf(
    "i'm offline until morning, i'll write then",
    "all fine, talk tomorrow",
    "can't talk now",
)
