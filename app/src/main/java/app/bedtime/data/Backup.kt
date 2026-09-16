package app.bedtime.data

import kotlinx.serialization.Serializable

/**
 * Everything worth keeping across a reinstall, as one JSON file: schedules, settings and history.
 * Running sessions ([RuntimeState]) are left out on purpose; they're momentary.
 */
@Serializable
data class Backup(
    val version: Int = VERSION,
    val exportedAt: Long = 0L,
    val schedules: List<Schedule> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val history: List<SessionLog> = emptyList(),
) {
    companion object {
        /** Raised only if a future file can no longer be read by this code. */
        const val VERSION = 1

        fun encode(backup: Backup): String = AppJson.encodeToString(serializer(), backup)

        /** Null when the file isn't ours, is broken, or was written by a newer version of the app. */
        fun decode(raw: String): Backup? =
            runCatching { AppJson.decodeFromString(serializer(), raw) }.getOrNull()?.takeIf { it.version <= VERSION }
    }
}
