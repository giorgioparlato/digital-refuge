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

/** How long a quote stays before the next one comes round. */
@Serializable
enum class QuoteRefresh(val hours: Int) { HOURLY(1), SIX_HOURS(6), DAILY(24), WEEKLY(168) }

/** The minimal home's green: the app's sage text green. */
const val DEFAULT_HOME_ACCENT = 0xFF86BC9F

/** Its green before the sage palette (v0.8.3), which a saved style still holding it reads as the new one. */
private const val OLD_HOME_ACCENT = 0xFF4EC68E

fun HomeStyle.withCurrentGreen(): HomeStyle = if (accent == OLD_HOME_ACCENT) copy(accent = DEFAULT_HOME_ACCENT) else this

/** Look of the minimal home screen. Colours are ARGB. */
@Serializable
data class HomeStyle(
    val background: Long = 0xFF282828,
    val accent: Long = DEFAULT_HOME_ACCENT,
    val textSize: TextSize = TextSize.MEDIUM,
    val appNameSize: AppNameSize = AppNameSize.MEDIUM,
    val showGreeting: Boolean = true,
    val showDate: Boolean = true,
    val showIcons: Boolean = false,
    /** A quote under the session, on the minimal home and the lock screen. */
    val showQuote: Boolean = true,
    /** How often that quote changes. */
    val quoteRefresh: QuoteRefresh = QuoteRefresh.DAILY,
    /** During sessions, show the clock and session over the lock screen. */
    val lockScreen: Boolean = true,
    /** The clock, date and app names. */
    val font: AppFont = AppFont.INTER,
    /** The quote, chosen apart from the rest: Inter as it always was, or a serif in italics. */
    val quoteFont: AppFont = AppFont.INTER,
    /** The name of the font file you added, if any; the file itself lives in the app's own storage. */
    val customFontName: String? = null,
)

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
)

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
)

/** One day of time spent in limited apps, and what was done about it. Days are local epoch days. */
@Serializable
data class UsageDay(
    val day: Long,
    /** Seconds in front, by package. Only apps that belong to a limit are counted. */
    val seconds: Map<String, Long> = emptyMap(),
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
