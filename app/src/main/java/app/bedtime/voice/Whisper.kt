package app.bedtime.voice

/**
 * Whisper on the phone (whisper.cpp, built in cpp/): a model loaded from a file you chose, and audio,
 * 16 kHz mono, turned into words. Nothing leaves the phone. Not there on a 32-bit phone: [available].
 */
object Whisper {
    val available: Boolean = runCatching { System.loadLibrary("refuge_whisper") }.isSuccess

    private external fun load(path: String): Long
    private external fun transcribe(handle: Long, audio: FloatArray, threads: Int, language: String): ByteArray?
    private external fun free(handle: Long)

    /**
     * The words in [audio] (16 kHz mono, -1 to 1) with the model at [modelPath], in [language] ("auto" to
     * find it); null if the model won't load or it fails. Slow: call it off the main thread.
     */
    @Synchronized
    fun words(modelPath: String, audio: FloatArray, language: String = "auto"): String? {
        if (!available) return null
        val handle = load(modelPath).takeIf { it != 0L } ?: return null
        return try {
            val threads = Runtime.getRuntime().availableProcessors().coerceIn(2, 6)
            transcribe(handle, audio, threads, language)?.toString(Charsets.UTF_8)?.trim()
        } finally {
            free(handle)
        }
    }
}
