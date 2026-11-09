package app.bedtime.ui.onboarding

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.Routes
import app.bedtime.apps.AppCatalog
import app.bedtime.data.Changelog
import app.bedtime.data.HourlyUse
import app.bedtime.data.LimitChoice
import app.bedtime.data.Onboarding
import app.bedtime.data.Repository
import app.bedtime.data.SetupApp
import app.bedtime.data.SetupStep
import app.bedtime.data.UseWidget
import app.bedtime.data.UseWords
import app.bedtime.service.DndController
import app.bedtime.service.SystemApps
import app.bedtime.service.UsageReader
import app.bedtime.ui.components.AppIcon
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.ui.components.PlainButton
import app.bedtime.ui.components.QuickChip
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.SegmentedChoice
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.TimeTile
import app.bedtime.ui.components.VerbatimText
import app.bedtime.ui.components.rememberUseDays
import app.bedtime.ui.edit.TimePickerDialog
import app.bedtime.ui.formatMinuteOfDay
import app.bedtime.ui.formatMinutes
import app.bedtime.ui.setup.AccessibilityHow
import app.bedtime.ui.setup.tryStart
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Setting refuge up for yourself, the first time or again: a step a screen, each saved as you go on
 * (so leaving half way keeps what was done), the optional ones left with "not now", and a summary. [onDone] gets the
 * pages to open after, in order, as asked for: the tour, then the everyday launcher's.
 */
@Composable
fun SetupFlow(onDone: (List<String>) -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val schedules by repo.schedules.collectAsStateWithLifecycle(initialValue = null)
    val use = rememberUseDays(8, live = false)
    val today = HourlyUse.today()

    // Every app with an icon but the phone's essentials, with this week's time and Android's category.
    val apps by produceState<List<SetupApp>?>(null, use) {
        val week = ((today - 6)..today).mapNotNull { use.days[it] }
        val excluded = SystemApps.essentials(context)
        value = withContext(Dispatchers.IO) {
            AppCatalog.launchableApps(context).filter { it.packageName !in excluded }.map { app ->
                val category = runCatching { context.packageManager.getApplicationInfo(app.packageName, 0).category }.getOrDefault(-1)
                SetupApp(app.packageName, app.label, week.sumOf { it.apps[app.packageName] ?: 0L }, Onboarding.pulling(category))
            }
        }
    }
    val s = settings ?: return
    val list = apps ?: return
    val scheduleList = schedules ?: return

    val steps = SetupStep.entries
    var at by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[at.coerceAtMost(steps.lastIndex)]
    // The steps left for now: nothing of theirs is saved.
    var leftNames by rememberSaveable { mutableStateOf(listOf<String>()) }
    val left = leftNames.map { SetupStep.valueOf(it) }.toSet()

    // What's chosen along the way, starting from what's already set (setup can be gone through again).
    var distracting by rememberSaveable { mutableStateOf(s.distractingApps.ifEmpty { Onboarding.suggestedDistracting(list) }.toList()) }
    val weekAll = list.sumOf { it.weekSeconds } / 7 / 60
    // What the later steps shut and limit: the apps ticked, or what was there before if that step was left.
    val shuts = if (SetupStep.APPS in left) s.distractingApps else distracting.toSet()
    val weekDistracting = list.filter { it.pkg in shuts }.sumOf { it.weekSeconds } / 7 / 60
    // 0: distracting apps, 1: the whole phone.
    var budgetFor by rememberSaveable { mutableIntStateOf(0) }
    var budget by rememberSaveable { mutableIntStateOf(-1) }
    val existing = Onboarding.existingBedtime(scheduleList)
    var start by rememberSaveable { mutableIntStateOf(existing?.startMinute ?: (22 * 60 + 30)) }
    var end by rememberSaveable { mutableIntStateOf(existing?.endMinute ?: (7 * 60)) }
    var greyscale by rememberSaveable { mutableStateOf(existing?.greyscale ?: false) }
    var minimal by rememberSaveable { mutableStateOf(existing?.minimalMode ?: false) }
    var quiet by rememberSaveable { mutableStateOf(existing?.hideNotifications ?: false) }
    val limitChoices = remember { mutableStateMapOf<String, LimitChoice>() }
    var always by rememberSaveable { mutableStateOf(s.alwaysAvailable.toList()) }
    var launcher by rememberSaveable { mutableStateOf(s.homeScreen) }

    val back = { if (at > 0) at-- }
    BackHandler(enabled = at > 0) { back() }

    // Each step saved as you go on from it.
    fun save(done: SetupStep) {
        scope.launch {
            when (done) {
                SetupStep.APPS -> repo.updateSettings { it.copy(distractingApps = distracting.toSet()) }
                SetupStep.BUDGET -> {
                    val whole = budgetFor == 1
                    val minutes = budget.takeIf { it > 0 } ?: Onboarding.suggestedBudget((if (whole) weekAll else weekDistracting).toInt(), UseWidget().allowance(whole))
                    repo.updateSettings { it.copy(homeStyle = it.homeStyle.copy(useWidget = it.homeStyle.useWidget.withAllowance(whole, minutes))) }
                }
                SetupStep.BEDTIME -> repo.upsert(Onboarding.bedtime(Onboarding.existingBedtime(scheduleList), start, end, shuts, greyscale, minimal, quiet))
                SetupStep.LIMITS -> {
                    val limited = s.limits.flatMap { it.rules.apps }.toSet()
                    Onboarding.worst(list, shuts).filter { it.pkg !in limited }.forEach { app ->
                        Onboarding.limit(app, limitChoices[app.pkg] ?: LimitChoice.NONE, today)?.let { repo.saveLimit(it, today) }
                    }
                }
                SetupStep.ALWAYS -> repo.updateSettings { it.copy(alwaysAvailable = always.toSet(), alwaysAvailableSeeded = true) }
                else -> Unit
            }
        }
    }
    fun finish(tour: Boolean = false) {
        scope.launch {
            val installed = Changelog.installedVersion(context)
            repo.updateSettings { it.copy(onboarded = true, lastSeenVersion = if (it.onboarded) it.lastSeenVersion else installed) }
            onDone(listOfNotNull(Routes.TOUR.takeIf { tour }, Routes.LAUNCHER.takeIf { launcher && !s.homeScreen }))
        }
    }
    val next = {
        save(step)
        leftNames = (left - step).map { it.name }
        if (step == SetupStep.DONE) finish() else at++
    }
    val notNow = {
        leftNames = (left + step).map { it.name }
        at++
    }

    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Where you are, and a way out of the whole thing.
            Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 16.dp, top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    steps.indices.forEach { i ->
                        Box(Modifier.weight(1f).height(3.dp).clip(CircleShape).background(if (i <= at) c.accentFill else c.interactive))
                    }
                }
                Text(
                    if (step == SetupStep.DONE) "" else "Skip setup",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.textFaint,
                    modifier = Modifier.padding(start = 12.dp).clip(RoundedCornerShape(8.dp)).clickable(enabled = step != SetupStep.DONE) { finish() }.padding(6.dp),
                )
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                when (step) {
                    SetupStep.APPS -> Apps(list, distracting.toSet(), use.fromAndroid) { pkg -> distracting = if (pkg in distracting) distracting - pkg else distracting + pkg }
                    SetupStep.BUDGET -> Budget(use.fromAndroid, weekAll.toInt(), weekDistracting.toInt(), budgetFor, { budgetFor = it }, budget, { budget = it })
                    SetupStep.BEDTIME -> Bedtime(
                        start, { start = it }, end, { end = it },
                        greyscale, { greyscale = it }, minimal, { minimal = it }, quiet, { quiet = it }, shuts.size,
                    )
                    SetupStep.LIMITS -> Limits(Onboarding.worst(list, shuts), s.limits.flatMap { it.rules.apps }.toSet(), limitChoices)
                    SetupStep.ALWAYS -> Always(list, always.toSet()) { pkg -> always = if (pkg in always) always - pkg else always + pkg }
                    SetupStep.PERMISSION -> Permission(SetupStep.BEDTIME !in left && (quiet || greyscale))
                    SetupStep.HOME -> Home(launcher) { launcher = it }
                    SetupStep.DONE -> Done(
                        onTour = { save(step); finish(tour = true) },
                        lines = listOf(
                            "Distracting apps" to (if (SetupStep.APPS in left) "not now" else if (distracting.isEmpty()) "none" else "${distracting.size}"),
                            "A day's budget" to if (SetupStep.BUDGET in left) {
                                "not now"
                            } else {
                                formatMinutes(
                                    (budget.takeIf { it > 0 } ?: Onboarding.suggestedBudget((if (budgetFor == 1) weekAll else weekDistracting).toInt(), UseWidget().allowance(budgetFor == 1))).toLong(),
                                ) + if (budgetFor == 1) " on the phone" else " in distracting apps"
                            },
                            "Bedtime" to if (SetupStep.BEDTIME in left) "not now" else "${formatMinuteOfDay(context, start)} – ${formatMinuteOfDay(context, end)}",
                            "Limits" to if (SetupStep.LIMITS in left) "not now" else limitChoices.values.count { it != LimitChoice.NONE }.let { if (it == 0) "none" else "$it" },
                            "Always available" to if (SetupStep.ALWAYS in left) "not now" else "${always.size}",
                        ),
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                if (at > 0) {
                    Text(
                        "Back",
                        style = MaterialTheme.typography.labelLarge,
                        color = c.textMuted,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { back() }.padding(horizontal = 8.dp, vertical = 12.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                }
                if (step.optional) {
                    PlainButton("Not now", onClick = { notNow() }, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(10.dp))
                }
                CtaButton(
                    if (step == SetupStep.DONE) "Start" else "Next",
                    onClick = { next() },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun Title(title: String, body: String) {
    val c = Obsidian.colors
    Text(title, fontSize = 26.sp, color = c.textNormal)
    Text(body, style = MaterialTheme.typography.bodyLarge, color = c.textMuted)
}

@Composable
internal fun Apps(apps: List<SetupApp>, chosen: Set<String>, measured: Boolean, onToggle: (String) -> Unit) {
    Title("Your distracting apps", "The ones that pull you in: what bedtime shuts, what limits hold, and what insights count.")
    UsageAccess(measured, "to see how long each app took this week")
    var everything by rememberSaveable { mutableStateOf(false) }
    val shown = if (everything) apps.sortedWith(compareByDescending<SetupApp> { it.weekSeconds }.thenBy { it.label.lowercase() }) else Onboarding.candidates(apps, chosen)
    SectionCard {
        shown.forEach { app -> AppTick(app, app.pkg in chosen, if (measured && app.weekSeconds >= 60) UseWords.short(app.weekSeconds / 7) + " a day" else null) { onToggle(app.pkg) } }
        if (!everything) More("All apps") { everything = true }
    }
}

@Composable
internal fun Budget(measured: Boolean, all: Int, distracting: Int, budgetFor: Int, onFor: (Int) -> Unit, budget: Int, onBudget: (Int) -> Unit) {
    val c = Obsidian.colors
    Title("A day's budget", "Insights and the tracker widget measure each day against it. It doesn't block anything: limits do that.")
    UsageAccess(measured, "to start from what you do now")
    if (measured) {
        Text(
            "Lately, on an average day: ${formatMinutes(all.toLong())} on the phone, ${formatMinutes(distracting.toLong())} in your distracting apps.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.textNormal,
        )
    }
    SegmentedChoice(listOf("Distracting apps", "Whole phone"), budgetFor, onFor)
    val whole = budgetFor == 1
    val suggested = Onboarding.suggestedBudget(if (whole) all else distracting, UseWidget().allowance(whole))
    val now = if (budget > 0) budget else suggested
    val options = ((if (whole) UseWords.PHONE_BUDGETS else UseWords.DISTRACTING_BUDGETS) + suggested).distinct().sorted()
    @OptIn(ExperimentalLayoutApi::class)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { m -> QuickChip(UseWords.compact(m) + if (m == suggested && measured) " · suggested" else "", selected = m == now) { onBudget(m) } }
    }
    if (measured) Text("Suggested: a little under what you do now.", style = MaterialTheme.typography.bodySmall, color = c.textFaint)
}

@Composable
internal fun Bedtime(
    start: Int, onStart: (Int) -> Unit,
    end: Int, onEnd: (Int) -> Unit,
    greyscale: Boolean, onGreyscale: (Boolean) -> Unit,
    minimal: Boolean, onMinimal: (Boolean) -> Unit,
    quiet: Boolean, onQuiet: (Boolean) -> Unit,
    apps: Int,
) {
    val context = LocalContext.current
    val c = Obsidian.colors
    Title("A bedtime", "Hours when your distracting apps stay shut, every night. Getting out early asks for a 10-minute wait and a passage to copy, so the urge can pass.")
    SectionCard {
        var picking by rememberSaveable { mutableStateOf<String?>(null) }
        picking?.let { which ->
            TimePickerDialog(if (which == "start") "Phone down at" else "Until", if (which == "start") start else end, onDismiss = { picking = null }) { minute ->
                if (which == "start") onStart(minute) else onEnd(minute)
                picking = null
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TimeTile("Phone down at", formatMinuteOfDay(context, start), Modifier.weight(1f)) { picking = "start" }
            TimeTile("Until", formatMinuteOfDay(context, end), Modifier.weight(1f)) { picking = "end" }
        }
        Text(
            if (apps == 0) "No distracting apps chosen, so nothing is shut yet." else "Shuts your ${if (apps == 1) "distracting app" else "$apps distracting apps"}.",
            style = MaterialTheme.typography.bodySmall,
            color = c.textFaint,
            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
        )
        Line("Greyscale", "Colours drain away; on Android 15 and newer, with do not disturb access", greyscale, onGreyscale)
        Line("Refuge's quiet home screen", "The time and your few apps as words", minimal, onMinimal)
        Line("Silence notifications", "Held back until morning; alarms and music are never silenced", quiet, onQuiet)
    }
}

@Composable
internal fun Limits(worst: List<SetupApp>, limited: Set<String>, choices: MutableMap<String, LimitChoice>) {
    val c = Obsidian.colors
    Title("Limits for every day", "For your most used distracting apps, outside bedtime too: a pause asks for a few breaths before the app opens; minutes a day give it an allowance.")
    if (worst.isEmpty()) {
        Text("No distracting apps chosen yet, so nothing to limit. Limits can be made any time on focus.", style = MaterialTheme.typography.bodyMedium, color = c.textFaint)
        return
    }
    worst.forEach { app ->
        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app.pkg, app.label, 28.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    VerbatimText(app.label, style = MaterialTheme.typography.bodyLarge, color = c.textNormal)
                    if (app.weekSeconds >= 60) Text(UseWords.short(app.weekSeconds / 7) + " a day lately", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                }
            }
            if (app.pkg in limited) {
                Text("Already has a limit.", style = MaterialTheme.typography.bodySmall, color = c.textFaint, modifier = Modifier.padding(top = 8.dp))
            } else {
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LimitChoice.entries.forEach { choice -> QuickChip(choice.label, selected = (choices[app.pkg] ?: LimitChoice.NONE) == choice) { choices[app.pkg] = choice } }
                }
            }
        }
    }
}

@Composable
internal fun Always(apps: List<SetupApp>, chosen: Set<String>, onToggle: (String) -> Unit) {
    Title("Always available", "Apps bedtime never shuts, a tap away from its emergency button: maps, rides, your authenticator, your bank. Keep it short.")
    var everything by rememberSaveable { mutableStateOf(false) }
    val suggested = SystemApps.ALWAYS_AVAILABLE_SUGGESTIONS.toSet()
    val shown = if (everything) apps.sortedBy { it.label.lowercase() } else apps.filter { it.pkg in suggested || it.pkg in chosen }.sortedBy { it.label.lowercase() }
    SectionCard {
        shown.forEach { app -> AppTick(app, app.pkg in chosen, null) { onToggle(app.pkg) } }
        if (shown.isEmpty()) Text("None of the usual ones are installed.", style = MaterialTheme.typography.bodySmall, color = Obsidian.colors.textFaint)
        if (!everything) More("All apps") { everything = true }
    }
}

@Composable
private fun Permission(dndToo: Boolean) {
    val context = LocalContext.current
    val c = Obsidian.colors
    var serviceOn by remember { mutableStateOf(false) }
    var dndOk by remember { mutableStateOf(false) }
    LifecycleResumeEffect(Unit) {
        serviceOn = app.bedtime.service.SystemApps.isAccessibilityServiceEnabled(context)
        dndOk = DndController.hasAccess(context)
        onPauseOrDispose { }
    }
    Title("One permission", "What lets refuge notice a blocked app the moment it opens. It only learns which app is in front.")
    SectionCard {
        if (serviceOn) Text("On. That's all refuge needs.", style = MaterialTheme.typography.bodyMedium, color = c.accentText, modifier = Modifier.padding(bottom = 8.dp))
        AccessibilityHow(
            serviceOn,
            onOpenAppInfo = { context.tryStart(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))) },
            onOpenAccessibility = { context.tryStart(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
        )
    }
    if (dndToo && !dndOk) {
        SectionCard {
            Text("Do not disturb access", style = MaterialTheme.typography.bodyLarge, color = c.textNormal)
            Text("For silencing and greyscale at bedtime.", style = MaterialTheme.typography.bodySmall, color = c.textMuted, modifier = Modifier.padding(bottom = 10.dp))
            PlainButton("Open do not disturb access", onClick = { context.tryStart(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }, modifier = Modifier.fillMaxWidth())
        }
    }
    Text("The rest (notifications, battery, the full-screen reminder) is in settings › permissions & setup, whenever you like.", style = MaterialTheme.typography.bodySmall, color = c.textFaint)
}

@Composable
internal fun Home(on: Boolean, onOn: (Boolean) -> Unit) {
    Title("A quieter home screen", "Refuge's own: the time, a few widgets you choose, your apps as words. It can be there all the time, not only at bedtime.")
    SectionCard {
        Line("Use it as my home screen", "Android asks once setup is done; switch back any time in settings › home screen", on, onOn)
    }
}

@Composable
internal fun Done(lines: List<Pair<String, String>>, onTour: () -> Unit) {
    val c = Obsidian.colors
    Title("You're set", "All of it can be changed later in settings.")
    SectionCard {
        lines.forEach { (what, value) ->
            Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Text(what, style = MaterialTheme.typography.bodyLarge, color = c.textNormal, modifier = Modifier.weight(1f))
                Text(value, style = MaterialTheme.typography.bodyLarge, color = c.textMuted)
            }
        }
    }
    Text(
        "There's more when you want it: habits, notes and the microjournal, voice notes, check-ins, Obsidian, widgets, search and gestures.",
        style = MaterialTheme.typography.bodySmall,
        color = c.textFaint,
    )
    More("Show me around", onTour)
}

/** Usage access asked for with its reason, where it isn't given; refuge comes back here after. */
@Composable
private fun UsageAccess(measured: Boolean, why: String) {
    if (measured) return
    val context = LocalContext.current
    val c = Obsidian.colors
    SectionCard {
        Text("Allow usage access $why", style = MaterialTheme.typography.bodyLarge, color = c.textNormal)
        Text("Android's own record of which app was in front. It stays on the phone. Without it, refuge counts from now on.", style = MaterialTheme.typography.bodySmall, color = c.textMuted, modifier = Modifier.padding(bottom = 10.dp))
        PlainButton("Allow usage access", onClick = { context.tryStart(UsageReader.accessIntent(context)) }, modifier = Modifier.fillMaxWidth())
    }
}

/** An app with its icon, a line of how much it's used, and its tick. */
@Composable
private fun AppTick(app: SetupApp, on: Boolean, detail: String?, onToggle: () -> Unit) {
    val c = Obsidian.colors
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClickLabel = if (on) "untick ${app.label}" else "tick ${app.label}", onClick = onToggle).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app.pkg, app.label, 28.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            VerbatimText(app.label, style = MaterialTheme.typography.bodyLarge, color = c.textNormal)
            if (detail != null) Text(detail, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
        Box(
            Modifier.size(22.dp).clip(CircleShape).background(if (on) c.accentFill else Color.Transparent).border(1.5.dp, if (on) c.accentFill else c.textFaint, CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (on) Icon(Icons.Default.Check, contentDescription = null, tint = c.textOnAccent, modifier = Modifier.size(14.dp)) }
    }
}

@Composable
private fun More(label: String, onClick: () -> Unit) {
    Text(
        "$label ›",
        style = MaterialTheme.typography.labelLarge,
        color = Obsidian.colors.accentText,
        modifier = Modifier.padding(top = 6.dp).clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 6.dp),
    )
}

@Composable
private fun Line(title: String, description: String?, on: Boolean, onOn: (Boolean) -> Unit) {
    val c = Obsidian.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = c.textNormal)
            if (description != null) Text(description, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
        Spacer(Modifier.width(12.dp))
        ObsidianToggle(on, onOn)
    }
}
