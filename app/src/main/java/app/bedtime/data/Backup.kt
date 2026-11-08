package app.bedtime.data

import kotlinx.serialization.Serializable
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Everything worth keeping across a reinstall, as one JSON file: schedules, settings, history, notes,
 * habits, and your phone use by day and by month. The microjournal only when you ask for it
 * ([AppSettings.backupJournal]). Running sessions ([RuntimeState]) are left out on purpose; they're
 * momentary.
 */
@Serializable
data class Backup(
    val version: Int = VERSION,
    val exportedAt: Long = 0L,
    val schedules: List<Schedule> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val history: List<SessionLog> = emptyList(),
    val notes: List<Note> = emptyList(),
    /** The microjournal's entries. Empty in older files, and in those written without it. */
    val journal: List<JournalEntry> = emptyList(),
    val habitMarks: List<HabitMark> = emptyList(),
    /** Phone use, a day per entry, and the months the oldest days were folded into. Empty in older files. */
    val usage: List<UsageDay> = emptyList(),
    val usageMonths: List<UsageMonth> = emptyList(),
) {
    companion object {
        /** Raised only if a future file can no longer be read by this code. */
        const val VERSION = 1

        fun encode(backup: Backup): String = AppJson.encodeToString(serializer(), backup)

        /**
         * Null when the file isn't ours, is broken, is sealed ([BackupSeal.open] it first), or was
         * written by a newer version of the app.
         */
        fun decode(raw: String): Backup? {
            // A sealed file is JSON too, and with unknown keys ignored it would read as an empty backup.
            if (BackupSeal.isSealed(raw)) return null
            return runCatching { AppJson.decodeFromString(serializer(), raw) }.getOrNull()?.takeIf { it.version <= VERSION }
        }
    }
}

/**
 * The key made from your backup passphrase, with the salt and rounds that made it. Kept on this phone
 * only, apart from the settings, so it never goes into a backup; the passphrase itself isn't kept.
 */
@Serializable
data class BackupKey(val salt: String, val rounds: Int, val key: String)

/**
 * A backup sealed with a passphrase: AES-256-GCM, with the key made from the passphrase by
 * PBKDF2-HMAC-SHA256. The file carries its salt and rounds, so it opens with the passphrase alone.
 */
object BackupSeal {
    const val ROUNDS = 600_000
    const val MIN_LENGTH = 8
    private const val KIND = "digital refuge sealed backup"

    @Serializable
    private data class Sealed(val kind: String, val salt: String, val rounds: Int, val iv: String, val data: String)

    private val random = SecureRandom()
    private val b64 = Base64.getEncoder()
    private val unb64 = Base64.getDecoder()

    private fun bytes(n: Int) = ByteArray(n).also(random::nextBytes)

    private fun derive(passphrase: String, salt: ByteArray, rounds: Int): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(passphrase.toCharArray(), salt, rounds, 256))
            .encoded

    /** A new key from [passphrase], with a fresh salt. Slow on purpose: call it off the main thread. */
    fun key(passphrase: String, rounds: Int = ROUNDS): BackupKey {
        val salt = bytes(16)
        return BackupKey(b64.encodeToString(salt), rounds, b64.encodeToString(derive(passphrase, salt, rounds)))
    }

    /** [json] sealed with [key], as the file's text. */
    fun seal(json: String, key: BackupKey): String {
        val iv = bytes(12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(unb64.decode(key.key), "AES"), GCMParameterSpec(128, iv))
        val data = cipher.doFinal(json.toByteArray())
        return AppJson.encodeToString(Sealed.serializer(), Sealed(KIND, key.salt, key.rounds, b64.encodeToString(iv), b64.encodeToString(data)))
    }

    private fun read(raw: String): Sealed? =
        runCatching { AppJson.decodeFromString(Sealed.serializer(), raw) }.getOrNull()?.takeIf { it.kind == KIND }

    fun isSealed(raw: String): Boolean = read(raw) != null

    /** The backup inside [raw], or null if the passphrase is wrong or the file isn't sealed. Slow, as [key]. */
    fun open(raw: String, passphrase: String): String? {
        val sealed = read(raw) ?: return null
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val key = derive(passphrase, unb64.decode(sealed.salt), sealed.rounds)
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, unb64.decode(sealed.iv)))
            cipher.doFinal(unb64.decode(sealed.data)).decodeToString()
        }.getOrNull()
    }
}
