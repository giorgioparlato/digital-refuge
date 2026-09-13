package app.bedtime.data

/**
 * Tapping a group in the app picker: ticks all of its apps, or unticks them if they're all ticked
 * already. A partly ticked group gets completed.
 */
fun toggleGroup(selected: Set<String>, group: Set<String>): Set<String> =
    if (group.isNotEmpty() && selected.containsAll(group)) selected - group else selected + group
