package app.bedtime.data

/**
 * Short words typed before what you're looking for, sending it straight to one place: "m cafe" to
 * maps, "n buy milk" to your notes, "@anna" to contacts. Apps can have one too, and theirs works alone:
 * "sp" opens Spotify. Keyed like [SearchOrder] choices, apps as "app:" + package.
 */
object Keywords {
    const val APP = "app:"

    fun appKey(pkg: String) = "$APP$pkg"

    /** Where something typed with a keyword goes ([key]), and the words left after the keyword. */
    data class Hit(val key: String, val words: String)

    /** A few to start with, each only where it doesn't clash with one already taken. */
    fun defaults(choices: List<SearchChoice>, notes: List<NoteTarget>): Map<String, String> {
        val out = linkedMapOf<String, String>()
        fun put(key: String?, keyword: String) {
            if (key != null && keyword.isNotBlank() && keyword !in out.values) out[key] = keyword
        }
        put(choices.firstOrNull { it.key.startsWith("web:") }?.key, "w")
        put(choices.firstOrNull { it.key.startsWith("maps:") }?.key, "m")
        put(choices.firstOrNull { it.key.startsWith("wikipedia:") }?.key, "wp")
        put(choices.firstOrNull { it.key.startsWith("lookup:") }?.key, "d")
        put(choices.firstOrNull { it.key.startsWith("spotify:") }?.key, "s")
        put(choices.firstOrNull { it.key == SearchOrder.OBSIDIAN }?.key, "o")
        put(choices.firstOrNull { it.key == SearchOrder.FILES }?.key, "f")
        put(SearchOrder.REFUGE_NOTES, "n")
        put(SearchOrder.CALENDAR, "c")
        put(SearchOrder.CONTACTS, "@")
        notes.forEach { put(SearchOrder.noteKey(it), it.name.filter { c -> c.isLetterOrDigit() }.take(2).lowercase()) }
        return out
    }

    /** Your keywords once you've changed any, otherwise the suggested ones. */
    fun effective(saved: Map<String, String>, custom: Boolean, defaults: Map<String, String>): Map<String, String> =
        if (custom) saved else defaults

    /** A keyword as kept: lowercase, no spaces, at most a few characters. */
    fun clean(keyword: String): String = keyword.filterNot { it.isWhitespace() }.lowercase().take(4)

    /**
     * The keyword [query] starts with, if any: a keyword, a space, then words ("m cafe"); a sign needs
     * no space ("@anna"); an app's keyword can be the whole query ("sp"). Longer keywords win over
     * shorter ones they begin with ("wp" over "w"). Nothing until there are words after it.
     */
    fun match(query: String, keywords: Map<String, String>): Hit? {
        val q = query.trimStart()
        for ((key, keyword) in keywords.entries.sortedByDescending { it.value.length }) {
            if (keyword.isEmpty()) continue
            val words = when {
                q.startsWith("$keyword ", ignoreCase = true) -> q.substring(keyword.length + 1)
                keyword.none { it.isLetterOrDigit() } && q.startsWith(keyword) -> q.substring(keyword.length)
                key.startsWith(APP) && q.trim().equals(keyword, ignoreCase = true) -> ""
                else -> continue
            }.trim()
            if (words.isNotEmpty() || key.startsWith(APP)) return Hit(key, words)
        }
        return null
    }
}
