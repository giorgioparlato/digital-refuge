package app.bedtime.engine

import app.bedtime.data.AppLimit
import app.bedtime.data.LimitRules
import app.bedtime.data.OutOfTime
import app.bedtime.data.UnlockConfig
import app.bedtime.data.UsageDay

/** Pure rules for limits: what holds today, how much time is left, and what counts as loosening. */
object Limits {
    /** [limits] as they stand on [today]: looser rules that have come due take over, due deletions go. */
    fun settle(limits: List<AppLimit>, today: Long): List<AppLimit> =
        limits.filterNot { it.deleteFromDay != null && today >= it.deleteFromDay }
            .map { if (it.next != null && today >= it.nextFromDay) it.copy(rules = it.next, next = null) else it }

    /** The limits in force on [today] that include [pkg]. */
    fun covering(limits: List<AppLimit>, pkg: String, today: Long): List<AppLimit> =
        settle(limits, today).filter { pkg in it.rules.apps }

    /**
     * Seconds of [limit]'s budget left for [pkg] on [usage]'s day, counting [unsaved] seconds not yet
     * written down. Null when there's no budget to run out of: none set, or lifted for the day.
     */
    fun secondsLeft(limit: AppLimit, pkg: String, usage: UsageDay, unsaved: Map<String, Long> = emptyMap()): Long? {
        val rules = limit.rules
        val minutes = rules.dailyMinutes ?: return null
        if (limit.id in usage.lifted) return null
        val counted = if (rules.shared) rules.apps else setOf(pkg)
        val used = counted.sumOf { (usage.seconds[it] ?: 0L) + (unsaved[it] ?: 0L) }
        val extra = (usage.extras[limit.id]?.size ?: 0) * rules.extraMinutes * 60L
        return minutes * 60L + extra - used
    }

    /**
     * Seconds used on [usage]'s day as the budget sees it: all the apps together when they share it,
     * otherwise the most-used one, since that's the one closest to running out.
     */
    fun usedSeconds(limit: AppLimit, usage: UsageDay): Long {
        val each = limit.rules.apps.map { usage.seconds[it] ?: 0L }
        return if (limit.rules.shared) each.sum() else each.maxOrNull() ?: 0L
    }

    /** Seconds in [limit]'s apps over the 7 days ending [today], and how often more time was taken. */
    fun week(limit: AppLimit, days: List<UsageDay>, today: Long): Pair<Long, Int> {
        val recent = days.filter { it.day in (today - 6)..today }
        return recent.sumOf { day -> limit.rules.apps.sumOf { day.seconds[it] ?: 0L } } to
            recent.sumOf { it.extras[limit.id]?.size ?: 0 }
    }

    /** The first limit whose budget for [pkg] has run out, if any. */
    fun exhausted(limits: List<AppLimit>, pkg: String, usage: UsageDay, unsaved: Map<String, Long> = emptyMap()): AppLimit? =
        covering(limits, pkg, usage.day).firstOrNull { (secondsLeft(it, pkg, usage, unsaved) ?: 1L) <= 0L }

    /** The least time left for [pkg] across its limits, or null if none of them has a budget. */
    fun leastLeft(limits: List<AppLimit>, pkg: String, usage: UsageDay, unsaved: Map<String, Long> = emptyMap()): Long? =
        covering(limits, pkg, usage.day).mapNotNull { secondsLeft(it, pkg, usage, unsaved) }.minOrNull()

    /** The pause before opening [pkg], in seconds, and the grace after it in minutes; null if none applies. */
    fun pauseFor(limits: List<AppLimit>, pkg: String, today: Long): Pair<Int, Int>? {
        val pausing = covering(limits, pkg, today).filter { (it.rules.pauseSeconds ?: 0) > 0 }
        if (pausing.isEmpty()) return null
        return pausing.maxOf { it.rules.pauseSeconds ?: 0 } to pausing.minOf { it.rules.graceMinutes }
    }

    /**
     * Stores [edited] over [existing] on [today]. Anything that makes the limit stricter applies at
     * once. Anything that loosens it — fewer apps, a shorter pause, more minutes, an easier way to
     * get more — is saved but waits for midnight, so it can't be used to get round today's limit.
     * A limit made today can be changed freely until the day ends: nothing has been given up yet.
     */
    fun save(existing: AppLimit?, edited: AppLimit, today: Long): AppLimit {
        if (existing == null) return edited.copy(next = null, deleteFromDay = null, createdDay = today)
        val current = settle(listOf(existing), today).firstOrNull() ?: existing
        val base = edited.copy(createdDay = current.createdDay, deleteFromDay = null)
        return when {
            current.createdDay == today || !isLooser(current.rules, edited.rules) -> base.copy(next = null)
            else -> base.copy(rules = current.rules, next = edited.rules, nextFromDay = today + 1)
        }
    }

    /** Deletes now if the limit was made today; otherwise marks it to go at midnight. Null means gone. */
    fun delete(existing: AppLimit, today: Long): AppLimit? =
        if (existing.createdDay == today) null else existing.copy(deleteFromDay = today + 1)

    /** Whether [new] lets you off more lightly than [old] in any way at all. */
    fun isLooser(old: LimitRules, new: LimitRules): Boolean =
        !new.apps.containsAll(old.apps) ||
            (new.pauseSeconds ?: 0) < (old.pauseSeconds ?: 0) ||
            ((old.pauseSeconds ?: 0) > 0 && new.graceMinutes > old.graceMinutes) ||
            budgetLooser(old, new) ||
            isLooser(old.unlock, new.unlock)

    private fun budgetLooser(old: LimitRules, new: LimitRules): Boolean {
        val oldMinutes = old.dailyMinutes ?: return false
        val newMinutes = new.dailyMinutes ?: return true
        if (newMinutes > oldMinutes) return true
        // "Each app gets the minutes in full" allows more than "shared between them".
        if (old.shared && !new.shared && new.apps.size > 1) return true
        if (new.outOfTime.ordinal > old.outOfTime.ordinal) return true
        return new.outOfTime == OutOfTime.MORE_TIME && new.extraMinutes > old.extraMinutes
    }

    /** Whether the unlock steps got easier: a step dropped, or made shorter. */
    fun isLooser(old: UnlockConfig, new: UnlockConfig): Boolean =
        (old.waitEnabled && (!new.waitEnabled || new.waitDurationSeconds < old.waitDurationSeconds)) ||
            (old.textEnabled && (!new.textEnabled || new.textLength < old.textLength)) ||
            (old.passwordEnabled && old.hasPassword && !new.passwordEnabled) ||
            (old.escalate && (!new.escalate || new.escalateFactor < old.escalateFactor))
}
