package app.bedtime.data

import java.time.LocalDateTime
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.bedtime.engine.Limits
import app.bedtime.engine.Occurrence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import java.util.UUID

private val Context.store: DataStore<Preferences> by preferencesDataStore(name = "bedtime")

/** Schedules, runtime state, settings and history, each stored as JSON under its own DataStore key. */
class Repository private constructor(context: Context) {
    private val store = context.store

    val schedules: Flow<List<Schedule>> = observe(SCHEDULES, schedulesSerializer, emptyList())
    val runtime: Flow<RuntimeState> = observe(RUNTIME, RuntimeState.serializer(), RuntimeState())
    val settings: Flow<AppSettings> = observe(SETTINGS, AppSettings.serializer(), AppSettings())
    val history: Flow<List<SessionLog>> = observe(HISTORY, historySerializer, emptyList())

    /** Time in limited apps, a day per entry, newest last. Kept apart so counting never re-runs the engine. */
    val usage: Flow<List<UsageDay>> = observe(USAGE, usageSerializer, emptyList())

    /** Your notes, newest first. Kept apart from the settings, which change far more often. */
    val notes: Flow<List<Note>> = observe(NOTES, notesSerializer, emptyList())

    suspend fun addNote(text: String, at: Long = System.currentTimeMillis()) =
        update(NOTES, notesSerializer, emptyList()) { listOf(Note(text = text, createdAt = at)) + it }

    suspend fun editNote(id: String, text: String) = update(NOTES, notesSerializer, emptyList()) { notes ->
        notes.map { if (it.id == id) it.copy(text = text, editedAt = System.currentTimeMillis()) else it }
    }

    /** Adds [words] to the to-do note as an unticked item, starting the note if there isn't one yet. */
    suspend fun addTodo(words: String, at: Long = System.currentTimeMillis()) = update(NOTES, notesSerializer, emptyList()) { notes ->
        val item = Notes.line(words, LocalDateTime.now(), NoteStyle.TASK, withDate = false)
        if (notes.none { it.id == Notes.TODO_ID }) {
            listOf(Note(Notes.TODO_ID, "# To-do\n$item\n", at)) + notes
        } else {
            notes.map { if (it.id == Notes.TODO_ID) it.copy(text = Notes.appended(it.text, item), editedAt = at) else it }
        }
    }

    /** What's been marked for each habit, by day. */
    val habitMarks: Flow<List<HabitMark>> = observe(HABIT_MARKS, habitMarksSerializer, emptyList())

    /** [habit]'s mark on [day] set to [value]; 0 takes it away. */
    suspend fun markHabit(habit: String, day: Long, value: Int) = update(HABIT_MARKS, habitMarksSerializer, emptyList()) { marks ->
        marks.filterNot { it.habit == habit && it.day == day } + listOfNotNull(HabitMark(habit, day, value).takeIf { value > 0 })
    }

    /** A habit gone, and its marks with it. */
    suspend fun deleteHabit(id: String) {
        updateSettings { it.copy(habits = it.habits.filterNot { h -> h.id == id }) }
        update(HABIT_MARKS, habitMarksSerializer, emptyList()) { marks -> marks.filterNot { it.habit == id } }
    }

    suspend fun deleteNote(id: String) = update(NOTES, notesSerializer, emptyList()) { notes -> notes.filterNot { it.id == id } }

    /** The latest weather Breezy Weather sent, if any. */
    val weather: Flow<WeatherNow?> = observe(WEATHER, WeatherNow.serializer().nullable, null)

    suspend fun saveWeather(weather: WeatherNow) = update(WEATHER, WeatherNow.serializer().nullable, null) { weather }

    /** Apps and conversations quieted on the lock screen, each until its session ends. */
    val lockQuiet: Flow<Map<String, Long>> = observe(LOCK_QUIET, quietSerializer, emptyMap())

    suspend fun upsert(schedule: Schedule) = updateSchedules { list ->
        if (list.any { it.id == schedule.id }) list.map { if (it.id == schedule.id) schedule else it }
        else list + schedule
    }

    /**
     * Copies the schedule or block [id] under a fresh name, and returns the copy's id.
     *
     * The copy arrives switched off and with no hours restricting when it can be changed. Both are
     * there to stop a copy being born out of reach: a duplicate of a schedule that is running right
     * now would be running too, and a duplicate of one that can only be changed in the morning
     * couldn't be touched until then. Everything else carries over, which is the point of it.
     */
    suspend fun duplicate(id: String): String? {
        var copyId: String? = null
        updateSchedules { list ->
            val index = list.indexOfFirst { it.id == id }
            if (index < 0) return@updateSchedules list
            val copy = list[index].copy(
                id = UUID.randomUUID().toString(),
                name = copyName(list[index].name, list.mapTo(mutableSetOf()) { it.name }),
                enabled = false,
                editWindow = null,
            )
            copyId = copy.id
            list.toMutableList().also { it.add(index + 1, copy) }
        }
        return copyId
    }

    suspend fun delete(id: String) {
        updateSchedules { list -> list.filterNot { it.id == id } }
        updateRuntime {
            it.copy(overrides = it.overrides - id, activeRuns = it.activeRuns - id)
        }
        updateSettings { if (it.tileBlockId == id) it.copy(tileBlockId = null) else it }
    }

    suspend fun updateSchedules(transform: (List<Schedule>) -> List<Schedule>) =
        update(SCHEDULES, schedulesSerializer, emptyList(), transform)

    suspend fun updateRuntime(transform: (RuntimeState) -> RuntimeState) =
        update(RUNTIME, RuntimeState.serializer(), RuntimeState(), transform)

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) =
        update(SETTINGS, AppSettings.serializer(), AppSettings(), transform)

    /** Starts [schedule] (a block) now for [minutes], clearing leftovers from a previous run. */
    suspend fun startBlock(schedule: Schedule, minutes: Int) {
        val now = System.currentTimeMillis()
        updateRuntime {
            it.copy(
                activeRuns = it.activeRuns + (schedule.id to Run(now, now + minutes * 60_000L)),
                overrides = it.overrides - schedule.id,
            )
        }
    }

    /**
     * Takes back a block started moments ago, as if it had never begun: no history and no escape.
     * Only within [UNDO_MS] of its start, so a mis-tap costs nothing while a block you meant still
     * can't be left this way. Returns false once that moment has passed.
     */
    suspend fun undoStart(id: String): Boolean {
        val now = System.currentTimeMillis()
        var undone = false
        // One edit for both keys, so the session never lingers in the history without its run.
        store.edit { prefs ->
            val runtime = decode(prefs[RUNTIME], RuntimeState.serializer(), RuntimeState())
            val run = runtime.activeRuns[id]
            if (run == null || !canUndo(run.start, now)) return@edit
            prefs[RUNTIME] = AppJson.encodeToString(RuntimeState.serializer(), runtime.copy(activeRuns = runtime.activeRuns - id))
            val history = decode(prefs[HISTORY], historySerializer, emptyList())
            prefs[HISTORY] = AppJson.encodeToString(historySerializer, history.filterNot { it.scheduleId == id && it.start == run.start })
            undone = true
        }
        return undone
    }

    /** Adds history entries for occurrences seen for the first time. */
    suspend fun recordOccurrences(active: List<Occurrence>) = update(HISTORY, historySerializer, emptyList()) { log ->
        val fresh = active.filter { occ -> log.none { it.scheduleId == occ.schedule.id && it.start == occ.start } }
        if (fresh.isEmpty()) log
        else (log + fresh.map { SessionLog(it.schedule.id, it.schedule.name, it.start, it.end) }).takeLast(MAX_HISTORY)
    }

    suspend fun recordUnlock(occurrence: Occurrence, endedEarly: Boolean, at: Long) = recordEscape(occurrence) {
        it.copy(unlockTimes = it.unlockTimes + at, endedEarlyAt = if (endedEarly) at else it.endedEarlyAt)
    }

    /** Notes that blocking was switched off during [occurrence], so the stats show it. */
    suspend fun recordPause(occurrence: Occurrence, at: Long) = recordEscape(occurrence) { it.copy(pausedAt = it.pausedAt + at) }

    /** Notes an emergency break taken during [occurrence]. */
    suspend fun recordBreak(occurrence: Occurrence, at: Long) = recordEscape(occurrence) { it.copy(breakTimes = it.breakTimes + at) }

    private suspend fun recordEscape(occurrence: Occurrence, change: (SessionLog) -> SessionLog) =
        update(HISTORY, historySerializer, emptyList()) { log ->
            val index = log.indexOfFirst { it.scheduleId == occurrence.schedule.id && it.start == occurrence.start }
            val base = if (index >= 0) log[index] else {
                SessionLog(occurrence.schedule.id, occurrence.schedule.name, occurrence.start, occurrence.end)
            }
            val entry = change(base)
            if (index >= 0) log.toMutableList().also { it[index] = entry } else (log + entry).takeLast(MAX_HISTORY)
        }

    /** Saves a new or edited limit; loosening waits for midnight (see [Limits.save]). */
    suspend fun saveLimit(limit: AppLimit, today: Long) = updateSettings { settings ->
        val existing = settings.limits.firstOrNull { it.id == limit.id }
        val saved = Limits.save(existing, limit, today)
        val limits = if (existing == null) settings.limits + saved else settings.limits.map { if (it.id == limit.id) saved else it }
        settings.copy(limits = Limits.settle(limits, today))
    }

    /** Deletes a limit at once if it was made today, otherwise at midnight. */
    suspend fun deleteLimit(id: String, today: Long) = updateSettings { settings ->
        val limits = settings.limits.mapNotNull { if (it.id == id) Limits.delete(it, today) else it }
        settings.copy(limits = Limits.settle(limits, today))
    }

    /** Changes [day]'s usage, creating it if needed, and keeps only the last [USAGE_DAYS] days. */
    suspend fun updateUsage(day: Long, transform: (UsageDay) -> UsageDay) =
        update(USAGE, usageSerializer, emptyList()) { days ->
            val today = days.lastOrNull { it.day == day } ?: UsageDay(day)
            (days.filterNot { it.day == day } + transform(today)).sortedBy { it.day }.takeLast(USAGE_DAYS)
        }

    /** Quiets [key] on the lock screen until [until], dropping entries already over. */
    suspend fun quiet(key: String, until: Long) = update(LOCK_QUIET, quietSerializer, emptyMap()) { quiet ->
        val now = System.currentTimeMillis()
        quiet.filterValues { it > now } + (key to until)
    }

    /** Adds time spent in front, by package. */
    suspend fun addUsage(day: Long, seconds: Map<String, Long>) = updateUsage(day) { usage ->
        usage.copy(seconds = usage.seconds + seconds.map { (pkg, s) -> pkg to (usage.seconds[pkg] ?: 0L) + s })
    }

    /** Adds time to [day], by hour: in distracting apps, and in any app; and to [app] if it's a distracting one. */
    suspend fun addHourly(day: Long, distracting: Map<Int, Long>, all: Map<Int, Long>, app: String? = null) = updateUsage(day) { usage ->
        val spent = distracting.values.sum()
        usage.copy(
            hourly = usage.hourly + distracting.map { (hour, s) -> hour to (usage.hourly[hour] ?: 0L) + s },
            hourlyAll = usage.hourlyAll + all.map { (hour, s) -> hour to (usage.hourlyAll[hour] ?: 0L) + s },
            distractingSeconds = if (app == null || spent == 0L) usage.distractingSeconds else usage.distractingSeconds + (app to (usage.distractingSeconds[app] ?: 0L) + spent),
        )
    }

    /** Counts one more time the pause before opening [pkg] was shown. */
    suspend fun recordOpen(day: Long, pkg: String) = updateUsage(day) { it.copy(opens = it.opens + (pkg to (it.opens[pkg] ?: 0) + 1)) }

    /** Gives [limitId] [minutes] more today, or with null lifts it for the rest of the day. */
    suspend fun grantLimit(day: Long, limitId: String, at: Long, minutes: Int?) = updateUsage(day) {
        val taken = it.copy(extras = it.extras + (limitId to it.extras[limitId].orEmpty() + at))
        if (minutes == null) {
            taken.copy(lifted = taken.lifted + limitId)
        } else {
            taken.copy(extraMinutes = taken.extraMinutes + (limitId to taken.extraMinutes[limitId].orEmpty() + minutes))
        }
    }

    /**
     * Moves a minimal home still on the old default green to the sage, once. Doing it once, and
     * remembering, is what lets the old green be picked again afterwards without being moved back.
     */
    suspend fun migrateHomeGreen() = updateSettings {
        if (it.homeGreenMigrated) it else it.copy(homeStyle = it.homeStyle.withCurrentGreen(), homeGreenMigrated = true)
    }

    /** Changes your quote choices: your own quotes, favourites, hidden ones, and which rotate. */
    suspend fun updateQuotes(transform: (QuotePrefs) -> QuotePrefs) = updateSettings { it.copy(quotes = transform(it.quotes)) }

    /** Fills the always-available list once with suggested apps that are installed. */
    suspend fun seedAlwaysAvailable(suggested: Set<String>) = updateSettings {
        if (it.alwaysAvailableSeeded) it else it.copy(alwaysAvailable = it.alwaysAvailable + suggested, alwaysAvailableSeeded = true)
    }

    /** Creates the starter groups once ("Social & feeds", "Essentials"); empty ones are skipped. */
    suspend fun seedGroups(social: Set<String>, essentials: Set<String>) = updateSettings { settings ->
        if (settings.groupsSeeded) {
            settings
        } else {
            val starters = listOfNotNull(
                AppGroup(name = "Social & feeds", packages = social).takeIf { social.isNotEmpty() },
                AppGroup(name = "Essentials", packages = essentials).takeIf { essentials.isNotEmpty() },
            )
            settings.copy(groups = settings.groups + starters, groupsSeeded = true)
        }
    }

    suspend fun saveGroup(group: AppGroup) = updateSettings { settings ->
        val groups = if (settings.groups.any { it.id == group.id }) {
            settings.groups.map { if (it.id == group.id) group else it }
        } else {
            settings.groups + group
        }
        settings.copy(groups = groups)
    }

    suspend fun deleteGroup(id: String) = updateSettings { it.copy(groups = it.groups.filterNot { group -> group.id == id }) }

    /** Everything worth keeping across a reinstall. Running sessions are left out on purpose. */
    suspend fun snapshot(): Backup = Backup(
        exportedAt = System.currentTimeMillis(),
        schedules = schedules.first(),
        settings = settings.first(),
        history = history.first(),
        notes = notes.first(),
        habitMarks = habitMarks.first(),
    )

    /**
     * Replaces schedules, settings and history with [backup], and the notes too when it has some (an
     * older backup has none, and shouldn't wipe yours). Runtime state is left alone.
     */
    suspend fun restore(backup: Backup) {
        store.edit { prefs ->
            prefs[SCHEDULES] = AppJson.encodeToString(schedulesSerializer, backup.schedules)
            prefs[SETTINGS] = AppJson.encodeToString(AppSettings.serializer(), backup.settings)
            prefs[HISTORY] = AppJson.encodeToString(historySerializer, backup.history)
            if (backup.notes.isNotEmpty()) prefs[NOTES] = AppJson.encodeToString(notesSerializer, backup.notes)
            if (backup.habitMarks.isNotEmpty()) prefs[HABIT_MARKS] = AppJson.encodeToString(habitMarksSerializer, backup.habitMarks)
        }
    }

    private fun <T> observe(key: Preferences.Key<String>, serializer: KSerializer<T>, default: T): Flow<T> =
        store.data.map { decode(it[key], serializer, default) }.distinctUntilChanged()

    private suspend fun <T> update(key: Preferences.Key<String>, serializer: KSerializer<T>, default: T, transform: (T) -> T) {
        store.edit { prefs ->
            prefs[key] = AppJson.encodeToString(serializer, transform(decode(prefs[key], serializer, default)))
        }
    }

    private fun <T> decode(raw: String?, serializer: KSerializer<T>, default: T): T =
        raw?.let { runCatching { AppJson.decodeFromString(serializer, it) }.getOrNull() } ?: default

    companion object {
        private val SCHEDULES = stringPreferencesKey("schedules")
        private val RUNTIME = stringPreferencesKey("runtime")
        private val SETTINGS = stringPreferencesKey("settings")
        private val HISTORY = stringPreferencesKey("history")
        private val USAGE = stringPreferencesKey("usage")
        private val LOCK_QUIET = stringPreferencesKey("lockQuiet")
        private val NOTES = stringPreferencesKey("notes")
        private val WEATHER = stringPreferencesKey("weather")
        private val notesSerializer = ListSerializer(Note.serializer())
        private val HABIT_MARKS = stringPreferencesKey("habit_marks")
        private val habitMarksSerializer = ListSerializer(HabitMark.serializer())
        private val quietSerializer = MapSerializer(String.serializer(), Long.serializer())
        /** About three years of nightly sessions, or several months of a busy mix: still a small file. */
        private const val MAX_HISTORY = 1000
        /** Five weeks, enough for the week's figures and the strip of days on the limits tab. */
        private const val USAGE_DAYS = 35
        private val usageSerializer = ListSerializer(UsageDay.serializer())

        /** How long after starting a block it can still be taken back with "undo". */
        const val UNDO_MS = 10_000L

        /** Whether a block that started at [start] can still be undone at [now]. */
        fun canUndo(start: Long, now: Long): Boolean = now - start in 0 until UNDO_MS
        private val schedulesSerializer = ListSerializer(Schedule.serializer())
        private val historySerializer = ListSerializer(SessionLog.serializer())

        @Volatile
        private var instance: Repository? = null

        fun get(context: Context): Repository = instance ?: synchronized(this) {
            instance ?: Repository(context.applicationContext).also { instance = it }
        }
    }
}

/**
 * "Bedtime" becomes "Bedtime copy", then "Bedtime copy 2" and on, never landing on a name already
 * in use — two schedules with the same name would be indistinguishable everywhere they are listed.
 */
internal fun copyName(name: String, taken: Set<String>): String {
    // Copying a copy counts on from it rather than stacking: "Bedtime copy 2", never "Bedtime copy copy".
    val root = COPY_SUFFIX.find(name)?.groupValues?.get(1)?.takeIf { it.isNotBlank() } ?: name
    val base = "$root copy"
    if (base !in taken) return base
    var n = 2
    while ("$base $n" in taken) n++
    return "$base $n"
}

private val COPY_SUFFIX = Regex("""^(.*?)\s+copy(?:\s+\d+)?$""")
