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
    val minimalMode: Boolean = false,
    val allowedApps: Set<String> = emptySet(),
    val dnd: DndMode = DndMode.OFF,
    /** Hide filtered notifications until the session ends (uses Do Not Disturb's visual suppression). */
    val hideNotifications: Boolean = false,
    val unlock: UnlockConfig = UnlockConfig(),
    val unlockAction: UnlockAction = UnlockAction(),
) {
    val isBlock: Boolean get() = kind == ScheduleKind.BLOCK
}

@Serializable
data class UnlockConfig(
    val waitEnabled: Boolean = false,
    val waitMinutes: Int = 10,
    val textEnabled: Boolean = true,
    val textLength: Int = 200,
    val passwordEnabled: Boolean = false,
    val passwordHash: String? = null,
    val passwordSalt: String? = null,
    /** Each early unlock of this schedule within a day makes the text challenge longer. */
    val escalate: Boolean = true,
) {
    val hasPassword: Boolean get() = passwordHash != null && passwordSalt != null
}

@Serializable
enum class UnlockMode { END_SESSION, PAUSE }

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

/** A started wait challenge; tied to one occurrence so a stale timer never carries over to the next night. */
@Serializable
data class PendingWait(val occurrenceStart: Long, val readyAt: Long)

/** A running block, as epoch-millis [start, end). */
@Serializable
data class Run(val start: Long, val end: Long)

/** Colour-correction settings from before we switched greyscale on, restored when we switch it off. */
@Serializable
data class SavedDaltonizer(val enabled: Int, val mode: Int)

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

@Serializable
data class RuntimeState(
    val overrides: Map<String, ScheduleOverride> = emptyMap(),
    val pendingWaits: Map<String, PendingWait> = emptyMap(),
    val activeRuns: Map<String, Run> = emptyMap(),
    /** Non-null means greyscale is currently switched on by us. */
    val savedDaltonizer: SavedDaltonizer? = null,
    /** Non-null means Do Not Disturb is currently managed by us. */
    val savedZen: SavedZen? = null,
)

/** Size of app names in the minimal home's list, on top of [TextSize]; small sizes fit many apps and a quote. */
@Serializable
enum class AppNameSize(val scale: Float) { VERY_SMALL(0.55f), SMALL(0.75f), MEDIUM(1f), LARGE(1.2f) }

@Serializable
enum class TextSize(val scale: Float) { SMALL(0.85f), MEDIUM(1f), LARGE(1.2f) }

/** Look of the minimal home screen. Colours are ARGB. */
@Serializable
data class HomeStyle(
    val background: Long = 0xFF282828,
    val accent: Long = 0xFF4EC68E,
    val textSize: TextSize = TextSize.MEDIUM,
    val appNameSize: AppNameSize = AppNameSize.MEDIUM,
    val showGreeting: Boolean = true,
    val showDate: Boolean = true,
    val showIcons: Boolean = false,
    /** A quote of the day under the session, on the minimal home and the lock screen. */
    val showQuote: Boolean = true,
    /** When greyscale is on, keep the home screen itself in colour. */
    val keepInColour: Boolean = true,
    /** During sessions, show the clock and session over the lock screen. */
    val lockScreen: Boolean = true,
)

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
    /** During sessions, cover the Settings screens that switch blocking off (see SettingsGuard). */
    val lockSettingsDuringSessions: Boolean = true,
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
    /** Times blocking was switched off during this session (the banking pause, or Settings). */
    val pausedAt: List<Long> = emptyList(),
) {
    val unlocks: Int get() = unlockTimes.size

    /** Early unlocks and pauses together: every way out of a session. */
    val escapes: Int get() = unlockTimes.size + pausedAt.size
}
