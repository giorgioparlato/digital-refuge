package app.bedtime.unlock

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ITERATIONS = 60_000
    private const val KEY_BITS = 256

    /** Returns (hash, salt), both Base64. */
    fun hash(password: String): Pair<String, String> {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        return encode(derive(password, salt)) to encode(salt)
    }

    fun verify(password: String, hash: String, salt: String): Boolean =
        MessageDigest.isEqual(derive(password, decode(salt)), decode(hash))

    private fun derive(password: String, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS))
            .encoded

    private fun encode(bytes: ByteArray) = Base64.getEncoder().encodeToString(bytes)
    private fun decode(text: String) = Base64.getDecoder().decode(text)
}
