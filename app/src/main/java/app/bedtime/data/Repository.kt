package app.bedtime.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.bedtime.engine.Occurrence
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer

private val Context.store: DataStore<Preferences> by preferencesDataStore(name = "bedtime")

/** Schedules, runtime state, settings and history, each stored as JSON under its own DataStore key. */
class Repository private constructor(context: Context) {
    private val store = context.store

    val schedules: Flow<List<Schedule>> = observe(SCHEDULES, schedulesSerializer, emptyList())
    val runtime: Flow<RuntimeState> = observe(RUNTIME, RuntimeState.serializer(), RuntimeState())
    val settings: Flow<AppSettings> = observe(SETTINGS, AppSettings.serializer(), AppSettings())
    val history: Flow<List<SessionLog>> = observe(HISTORY, historySerializer, emptyList())

    suspend fun upsert(schedule: Schedule) = updateSchedules { list ->
        if (list.any { it.id == schedule.id }) list.map { if (it.id == schedule.id) schedule else it }
        else list + schedule
    }

    suspend fun delete(id: String) {
        updateSchedules { list -> list.filterNot { it.id == id } }
        updateRuntime {
            it.copy(overrides = it.overrides - id, pendingWaits = it.pendingWaits - id, activeRuns = it.activeRuns - id)
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
                pendingWaits = it.pendingWaits - schedule.id,
            )
        }
    }

    /** Adds history entries for occurrences seen for the first time. */
    suspend fun recordOccurrences(active: List<Occurrence>) = update(HISTORY, historySerializer, emptyList()) { log ->
        val fresh = active.filter { occ -> log.none { it.scheduleId == occ.schedule.id && it.start == occ.start } }
        if (fresh.isEmpty()) log
        else (log + fresh.map { SessionLog(it.schedule.id, it.schedule.name, it.start, it.end) }).takeLast(MAX_HISTORY)
    }

    suspend fun recordUnlock(occurrence: Occurrence, endedEarly: Boolean, at: Long) =
        update(HISTORY, historySerializer, emptyList()) { log ->
            val index = log.indexOfFirst { it.scheduleId == occurrence.schedule.id && it.start == occurrence.start }
            val base = if (index >= 0) log[index] else {
                SessionLog(occurrence.schedule.id, occurrence.schedule.name, occurrence.start, occurrence.end)
            }
            val entry = base.copy(unlockTimes = base.unlockTimes + at, endedEarlyAt = if (endedEarly) at else base.endedEarlyAt)
            if (index >= 0) log.toMutableList().also { it[index] = entry } else (log + entry).takeLast(MAX_HISTORY)
        }

    /** Notes that blocking was switched off during [occurrence], so the stats show it. */
    suspend fun recordPause(occurrence: Occurrence, at: Long) =
        update(HISTORY, historySerializer, emptyList()) { log ->
            val index = log.indexOfFirst { it.scheduleId == occurrence.schedule.id && it.start == occurrence.start }
            val base = if (index >= 0) log[index] else {
                SessionLog(occurrence.schedule.id, occurrence.schedule.name, occurrence.start, occurrence.end)
            }
            val entry = base.copy(pausedAt = base.pausedAt + at)
            if (index >= 0) log.toMutableList().also { it[index] = entry } else (log + entry).takeLast(MAX_HISTORY)
        }

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
    )

    /** Replaces schedules, settings and history with [backup]. Runtime state is left alone. */
    suspend fun restore(backup: Backup) {
        store.edit { prefs ->
            prefs[SCHEDULES] = AppJson.encodeToString(schedulesSerializer, backup.schedules)
            prefs[SETTINGS] = AppJson.encodeToString(AppSettings.serializer(), backup.settings)
            prefs[HISTORY] = AppJson.encodeToString(historySerializer, backup.history)
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
        private const val MAX_HISTORY = 200
        private val schedulesSerializer = ListSerializer(Schedule.serializer())
        private val historySerializer = ListSerializer(SessionLog.serializer())

        @Volatile
        private var instance: Repository? = null

        fun get(context: Context): Repository = instance ?: synchronized(this) {
            instance ?: Repository(context.applicationContext).also { instance = it }
        }
    }
}
