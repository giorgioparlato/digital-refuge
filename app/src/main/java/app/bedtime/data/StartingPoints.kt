package app.bedtime.data

/**
 * Starting points for how hard leaving early is. One fills the steps in; they stay yours to change,
 * and once changed none of these is the one chosen any more.
 */
enum class Strictness(val label: String) {
    GENTLE("Gentle"),
    FIRM("Firm"),
    STRICT("Strict"),
    ;

    /** [unlock] with this one's steps, the password kept as it is but switched off. */
    fun applied(unlock: UnlockConfig): UnlockConfig = when (this) {
        GENTLE -> unlock.copy(waitEnabled = true, waitSeconds = 30, textEnabled = false, writeEnabled = false, passwordEnabled = false, escalate = false)
        FIRM -> unlock.copy(
            waitEnabled = true, waitSeconds = 10 * 60,
            textEnabled = true, textLength = 150, textSource = TextSource.PASSAGES,
            writeEnabled = false, passwordEnabled = false, escalate = false,
        )
        STRICT -> FIRM.applied(unlock).copy(escalate = true, escalateFactor = 1.5f)
    }

    companion object {
        /** The one [unlock] is, step for step; null when it's your own. */
        fun of(unlock: UnlockConfig): Strictness? = entries.firstOrNull { steps(it.applied(unlock)) == steps(unlock) }

        /** What a challenge asks, leaving out what a switched-off step would ask. */
        private fun steps(u: UnlockConfig): List<Any?> = listOf(
            u.waitEnabled, u.waitDurationSeconds.takeIf { u.waitEnabled },
            u.textEnabled, u.textLength.takeIf { u.textEnabled }, u.textSource.takeIf { u.textEnabled },
            u.writeEnabled, u.passwordEnabled, u.escalate, u.escalateFactor.takeIf { u.escalate },
        )
    }
}

/**
 * Starting points for notifications while a session runs: who can ring, whether what's silenced
 * stays out of sight, and what refuge's lock screen lists. One fills the three in; they stay yours.
 */
enum class Hush(val label: String, val dnd: DndMode, val outOfSight: Boolean, val lockNotes: LockNotes) {
    USUAL("As usual", DndMode.OFF, false, LockNotes.USUAL),
    QUIET("Quiet", DndMode.PRIORITY, true, LockNotes.OWN),
    SILENT("Silent", DndMode.SILENCE, true, LockNotes.NONE),
    ;

    fun applied(schedule: Schedule): Schedule = schedule.copy(dnd = dnd, hideNotifications = outOfSight, lockNotes = lockNotes)

    companion object {
        /** The one [schedule] is; null when it's your own. */
        fun of(schedule: Schedule): Hush? = entries.firstOrNull { it.applied(schedule) == schedule }
    }
}
