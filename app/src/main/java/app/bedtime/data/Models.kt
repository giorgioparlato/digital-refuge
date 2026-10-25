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
    val kind: ScheduleKind = ScheduleKind.RECURRING,
    val enabled: Boolean = true,
    /** ISO day-of-week numbers (1 = Monday … 7 = Sunday) on which a session *starts*. */
    val days: Set<Int> = (1..7).toSet(),
    val startMinute: Int = 22 * 60,
    /** Minute of day the session ends; if <= [startMinute] it ends the following day. */
    val endMinute: Int = 7 * 60,
    /** Default length of a block, in minutes. */
    val durationMinutes: Int = 60,
    val blockedApps: Set<String> = emptySet(),
    val greyscale: Boolean = true,
    /** While this session greys the screen, leave our own minimal home in colour. */
    val keepHomeInColour: Boolean = true,
    val minimalMode: Boolean = false,
    val allowedApps: Set<String> = emptySet(),
    val dnd: DndMode = DndMode.OFF,
    /** Hide filtered notifications until the session ends (uses Do Not Disturb's visual suppression). */
    val hideNotifications: Boolean = false,
    val unlock: UnlockConfig = UnlockConfig(),
    val unlockAction: UnlockAction = UnlockAction(),
    /** Offer a short break for apps that won't run while blocking is on. Off means no way out but unlocking. */
    val pauseEnabled: Boolean = true,
    /** Allow editing right after an early unlock. Off keeps settings shut until the session would have ended. */
    val editAfterUnlock: Boolean = false,
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
    /** Ready-made replies to a message from the lock screen; typing your own works too. */
    val quickReplies: List<String> = DEFAULT_QUICK_REPLIES,
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
    /** Celsius, Fahrenheit, or the usual one where the phone is set. */
    val weatherUnit: TempUnit = TempUnit.AUTO,
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
)

/** The lock screen's gestures: four swipes starting around the clock, and a double tap anywhere. */
@Serializable
enum class Swipe { UP, DOWN, LEFT, RIGHT, DOUBLE_TAP }

/** The camera, which unlike any other app can open over the lock screen without unlocking. */
const val GESTURE_CAMERA = "camera"

/** One thing the search can offer, by [SearchOrder] key, and whether it does. */
@Serializable
data class SearchChoice(val key: String, val on: Boolean)

/** The home screen's gestures; a swipe up always opens every app. */
@Serializable
enum class HomeGesture { SWIPE_DOWN, SWIPE_LEFT, SWIPE_RIGHT, DOUBLE_TAP }

/** What a home-screen gesture can do besides opening an app. None has a dot in it, so none is a package. */
object HomeActions {
    const val NOTIFICATIONS = "notifications"
    const val LOCK = "lock"
    const val NOTE = "note"
    const val SEARCH = "search"
    const val NOTES = "notes"
    const val WEEK = "week"
    const val CAMERA = GESTURE_CAMERA

    /** In the order they're offered, with how each reads. */
    val all: List<Pair<String, String>> = listOf(
        NOTIFICATIONS to "Show notifications",
        LOCK to "Lock the phone",
        NOTE to "Write a quick note",
        SEARCH to "Search",
        NOTES to "Your notes",
        WEEK to "The week ahead",
        CAMERA to "Camera",
    )

    fun label(action: String?): String? = all.firstOrNull { it.first == action }?.second
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
    /** What the search offers after apps, in your order, each on or off; empty until you change it. */
    val searchChoices: List<SearchChoice> = emptyList(),
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
    /** Messages you wrote for covers, shared by every schedule, block and limit. */
    val customMessages: List<CustomMessage> = emptyList(),
    /** Your own quotes, favourites, hidden ones, and which of them rotate. */
    val quotes: QuotePrefs = QuotePrefs(),
    /** Files quick notes can be added to, besides the app's own notes; at most [Notes.MAX_TARGETS]. */
    val noteTargets: List<NoteTarget> = emptyList(),
    /** Which calendar the search adds events to; null for the first one that can take them. */
    val eventCalendarId: Long? = null,
)

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
    /** After "open anyway", how long you can be away and come back without the pause; 0 = every open. */
    val graceMinutes: Int = 2,
    /** Whether "open anyway" on one of the apps lets you into all of them, rather than only that one. */
    val pauseForGroup: Boolean = false,
    /** Minutes a day in these apps; null means no budget. */
    val dailyMinutes: Int? = null,
    /** True: the minutes are shared by all the apps together. False: each app gets them in full. */
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
