package app.bedtime.ui.limits

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.mutableIntStateOf
import kotlinx.coroutines.delay
import java.time.LocalTime
import app.bedtime.ui.components.MessageChoiceRow
import app.bedtime.ui.formatMinuteOfDay
import app.bedtime.ui.components.TimeTile
import app.bedtime.ui.edit.TimePickerDialog
import app.bedtime.engine.EditWindows
import app.bedtime.data.EditWindow
import app.bedtime.ui.components.PageGutter
import app.bedtime.apps.AppCatalog
import app.bedtime.data.AppJson
import app.bedtime.data.AppLimit
import app.bedtime.data.LimitRules
import app.bedtime.data.OutOfTime
import app.bedtime.data.Repository
import app.bedtime.engine.Limits
import app.bedtime.ui.apps.AppPickerScreen
import app.bedtime.ui.apps.PickerMode
import app.bedtime.ui.components.AppIconStack
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.BottomActionBar
import app.bedtime.ui.components.Callout
import app.bedtime.ui.components.CalloutKind
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.NumberStepper
import app.bedtime.ui.components.ObsidianTextField
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.OptionRow
import app.bedtime.ui.components.QuickChip
import app.bedtime.ui.components.SegmentedChoice
import app.bedtime.ui.components.SubOptionRow
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.SectionLabel
import app.bedtime.ui.edit.EditorHeader
import app.bedtime.ui.edit.RenameDialog
import app.bedtime.ui.edit.UnlockStepsEditor
import androidx.compose.ui.unit.sp
import app.bedtime.ui.formatMinutes
import app.bedtime.ui.formatWaitSeconds
import app.bedtime.ui.pluralApps
import app.bedtime.ui.theme.Obsidian
import app.bedtime.unlock.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private val LimitSaver = Saver<AppLimit, String>(
    save = { AppJson.encodeToString(AppLimit.serializer(), it) },
    restore = { AppJson.decodeFromString(AppLimit.serializer(), it) },
)

/** Why a limit can't be saved yet, or null if it can. */
internal fun limitProblem(draft: AppLimit, newPassword: String): String? {
    val rules = draft.rules
    return when {
        rules.apps.isEmpty() -> "Pick at least one app for this limit."
        (rules.pauseSeconds ?: 0) <= 0 && rules.dailyMinutes == null -> "Turn on a pause, a daily budget, or both."
        rules.dailyMinutes != null && rules.outOfTime != OutOfTime.TOMORROW && rules.unlock.passwordEnabled &&
            !rules.unlock.hasPassword && newPassword.isBlank() -> "Choose a password, or switch that step off."
        else -> null
    }
}

@Composable
fun LimitEditScreen(limitId: String?, onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now().toEpochDay() }
    var original by rememberSaveable(stateSaver = LimitSaver) { mutableStateOf(AppLimit()) }
    /** What holds today, which may be stricter than [original] if a looser version is waiting. */
    var held by rememberSaveable(stateSaver = LimitSaver) { mutableStateOf(AppLimit()) }
    var draft by rememberSaveable(stateSaver = LimitSaver) { mutableStateOf(AppLimit()) }
    var loaded by rememberSaveable { mutableStateOf(limitId == null) }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var picking by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var editingWindow by rememberSaveable { mutableStateOf<String?>(null) }
    // Ticks so a window that closes (or opens) while the page is open is noticed.
    var nowMinuteOfDay by remember { mutableIntStateOf(LocalTime.now().let { it.hour * 60 + it.minute }) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000)
            nowMinuteOfDay = LocalTime.now().let { it.hour * 60 + it.minute }
        }
    }

    LaunchedEffect(limitId) {
        if (!loaded && limitId != null) {
            Limits.settle(repo.settings.first().limits, today).firstOrNull { it.id == limitId }?.let {
                // Edit what's coming, if a looser version is already waiting for midnight.
                val shown = it.copy(rules = it.next ?: it.rules)
                held = it
                original = shown
                draft = shown
            }
            loaded = true
        }
    }
    if (!loaded) return

    if (picking) {
        AppPickerScreen("Apps to limit", draft.rules.apps, PickerMode.BLOCK, onDone = {
            draft = draft.copy(rules = draft.rules.copy(apps = it))
            error = null
            picking = false
        }, onBack = { picking = false })
        return
    }

    val isNew = limitId == null
    // A limit made today can't shut itself yet: the hours are being chosen right now.
    val shutWindow = held.editWindow?.takeIf { !isNew && held.createdDay != today && !EditWindows.isOpen(it, nowMinuteOfDay) }
    val dirty = shutWindow == null && (draft != original || newPassword.isNotEmpty())
    // Compared with what holds today, not with a looser version already waiting.
    val loosens = !isNew && held.createdDay != today && Limits.isLooser(held.rules, draft.rules)
    fun requestBack() {
        if (dirty) confirmDiscard = true else onBack()
    }
    BackHandler { requestBack() }

    fun save() {
        limitProblem(draft, newPassword)?.let {
            error = it
            return
        }
        scope.launch {
            var toSave = draft.copy(name = draft.name.trim().ifEmpty { "Limit" })
            val unlock = toSave.rules.unlock
            if (unlock.passwordEnabled && newPassword.isNotBlank()) {
                val (hash, salt) = withContext(Dispatchers.Default) { PasswordHasher.hash(newPassword) }
                toSave = toSave.copy(rules = toSave.rules.copy(unlock = unlock.copy(passwordHash = hash, passwordSalt = salt)))
            }
            repo.saveLimit(toSave, today)
            onBack()
        }
    }

    editingWindow?.let { which ->
        val window = draft.editWindow ?: EditWindow()
        TimePickerDialog(
            title = if (which == WINDOW_START) "Can be changed from" else "Can be changed until",
            initialMinute = if (which == WINDOW_START) window.startMinute else window.endMinute,
            onDismiss = { editingWindow = null },
            onConfirm = { minute ->
                draft = draft.copy(editWindow = if (which == WINDOW_START) window.copy(startMinute = minute) else window.copy(endMinute = minute))
                editingWindow = null
            },
        )
    }

    if (confirmDelete) {
        val now = held.createdDay == today
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = Obsidian.colors.bgSecondary,
            title = { Text("Delete “${draft.name}”?") },
            text = {
                Text(
                    if (now) "It goes straight away." else "It keeps holding until midnight, so deleting it can't get you round today.",
                    color = Obsidian.colors.textMuted,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        repo.deleteLimit(draft.id, today)
                        onBack()
                    }
                }) { Text("Delete", color = Obsidian.colors.red, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep it", color = Obsidian.colors.textMuted) } },
        )
    }
    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            containerColor = Obsidian.colors.bgSecondary,
            title = { Text("Discard changes?") },
            text = { Text("Your edits won't be saved.", color = Obsidian.colors.textMuted) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onBack()
                }) { Text("Discard", color = Obsidian.colors.red, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing", color = Obsidian.colors.textMuted) } },
        )
    }

    LimitEditContent(
        draft = draft,
        onDraftChange = {
            draft = it
            error = null
        },
        isNew = isNew,
        loosens = loosens,
        newPassword = newPassword,
        onNewPasswordChange = { newPassword = it },
        error = error,
        onBack = ::requestBack,
        onSave = ::save,
        onDelete = { confirmDelete = true },
        onPickApps = { picking = true },
        appLabel = { AppCatalog.label(context, it) },
        shutWindow = shutWindow,
        onEditWindowStart = { editingWindow = WINDOW_START },
        onEditWindowEnd = { editingWindow = WINDOW_END },
    )
}

private const val WINDOW_START = "start"
private const val WINDOW_END = "end"

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LimitEditContent(
    draft: AppLimit,
    onDraftChange: (AppLimit) -> Unit,
    isNew: Boolean,
    loosens: Boolean,
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    error: String?,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onPickApps: () -> Unit,
    appLabel: (String) -> String = { it },
    /** Set while outside the hours this limit may be changed in: the page is then read-only. */
    shutWindow: EditWindow? = null,
    onEditWindowStart: () -> Unit = {},
    onEditWindowEnd: () -> Unit = {},
) {
    val context = LocalContext.current
    val c = Obsidian.colors
    val rules = draft.rules
    val editable = shutWindow == null
    fun setRules(transform: (LimitRules) -> LimitRules) = onDraftChange(draft.copy(rules = transform(rules)))

    var renaming by rememberSaveable { mutableStateOf(false) }
    if (renaming) {
        RenameDialog(
            name = draft.name,
            title = "Name this limit",
            placeholder = "e.g. Social",
            onDismiss = { renaming = false },
            onDone = {
                onDraftChange(draft.copy(name = it))
                renaming = false
            },
        )
    }
    Scaffold(
        containerColor = c.bgPrimary,
        bottomBar = {
            BottomActionBar {
                if (shutWindow != null) {
                    CtaButton(
                        "Can be changed from ${formatMinuteOfDay(context, shutWindow.startMinute)}",
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    CtaButton(
                        when {
                            isNew -> "Create limit"
                            loosens -> "Save, starting tomorrow"
                            else -> "Save changes"
                        },
                        onClick = onSave,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(bottom = padding.calculateBottomPadding()).verticalScroll(rememberScrollState())) {
            // Like a schedule's page: the name on top, and large under it what the limit mostly is.
            EditorHeader(
                name = draft.name,
                placeholder = "name this limit",
                editable = editable,
                onBack = onBack,
                onRename = { renaming = true },
            ) {
                Text(
                    when {
                        rules.dailyMinutes != null -> "${formatMinutes(rules.dailyMinutes.toLong())} a day"
                        (rules.pauseSeconds ?: 0) > 0 -> "${formatWaitSeconds(rules.pauseSeconds ?: 0)} pause"
                        else -> "not set yet"
                    },
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Light,
                    color = c.textNormal,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (rules.apps.isEmpty()) "no apps yet · choose them below" else limitSummary(draft),
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textMuted,
                )
            }
            Column(Modifier.padding(horizontal = PageGutter)) {
                if (shutWindow != null) {
                    Callout(
                        title = "Outside the hours you set",
                        body = "You chose to let this one be changed only between " +
                            "${formatMinuteOfDay(context, shutWindow.startMinute)} and " +
                            "${formatMinuteOfDay(context, shutWindow.endMinute)}. Until then it can't be edited or deleted.",
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }
                if (error != null) {
                    Callout(title = "Almost there", kind = CalloutKind.WARNING, body = error, modifier = Modifier.padding(top = 14.dp))
                }
                if (loosens) {
                    Callout(
                        title = "This loosens it, so it starts tomorrow",
                        kind = CalloutKind.INFO,
                        body = "Today keeps the limit as it was. Making a limit stricter always takes effect at once.",
                        modifier = Modifier.padding(top = 14.dp),
                    )
                } else if (shutWindow == null) {
                    // Said before any change is made, not only once one loosens it.
                    Text(
                        if (isNew) {
                            "Once it's saved, making it stricter takes effect at once, while loosening it or deleting it " +
                                "waits until the next day. Until tonight, while it's new, you can still change it freely."
                        } else {
                            "Making it stricter takes effect at once. Loosening it — fewer apps, a shorter pause, more " +
                                "minutes, an easier way to get more — or deleting it waits until the next day."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textFaint,
                        modifier = Modifier.padding(top = 14.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                OptionRow(
                    BedtimeIcons.Grid,
                    "Apps",
                    description = if (rules.apps.isEmpty()) "None yet · tap to choose" else pluralApps(rules.apps.size),
                    enabled = editable,
                    onClick = onPickApps,
                ) {
                    AppIconStack(rules.apps.sorted(), appLabel)
                    Chevron()
                }

                SectionLabel("Pause before opening")
                OptionRow(
                    BedtimeIcons.Hourglass,
                    "A few breaths first",
                    description = "Before one of these apps opens, wait a moment and decide",
                    enabled = editable,
                ) {
                    ObsidianToggle(
                        (rules.pauseSeconds ?: 0) > 0,
                        { on -> setRules { it.copy(pauseSeconds = if (on) 10 else null) } },
                        enabled = editable,
                    )
                }
                AnimatedVisibility((rules.pauseSeconds ?: 0) > 0) {
                    Column {
                        SubOptionRow("How long") {
                            NumberStepper(
                                rules.pauseSeconds ?: 10,
                                { v -> setRules { it.copy(pauseSeconds = v) } },
                                3..120,
                                presets = listOf(5, 10, 15, 20, 30, 60),
                                title = "pause",
                                enabled = editable,
                                format = { formatWaitSeconds(it) },
                            )
                        }
                        SubOptionRow("Back within") {
                            NumberStepper(
                                rules.graceMinutes,
                                { v -> setRules { it.copy(graceMinutes = v) } },
                                0..60,
                                presets = listOf(0, 1, 2, 5, 10, 15),
                                title = "grace",
                                enabled = editable,
                                format = { if (it == 0) "every time" else "$it min" },
                            )
                        }
                        if (rules.apps.size > 1) {
                            Text(
                                "Opening one anyway lets you into",
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.textMuted,
                                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                            )
                            SegmentedChoice(
                                options = listOf("Just that app", "All of them"),
                                selected = if (rules.pauseForGroup) 1 else 0,
                                onSelect = { i -> setRules { it.copy(pauseForGroup = i == 1) } },
                                enabled = editable,
                            )
                        }
                        Text(
                            if (rules.apps.size > 1 && rules.pauseForGroup) {
                                if (rules.graceMinutes == 0) {
                                    "Getting through the pause once opens all of these apps, until you leave them for something else."
                                } else {
                                    "Getting through the pause once opens the way to all of these apps. Leaving them and coming back " +
                                        "within ${rules.graceMinutes} min doesn't ask again."
                                }
                            } else if (rules.graceMinutes == 0) {
                                "The pause comes every time you open one of them."
                            } else {
                                "After you open one anyway, leaving and coming back within ${rules.graceMinutes} min doesn't ask again."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textMuted,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }

                SectionLabel("Daily budget")
                OptionRow(
                    Icons.Default.Refresh,
                    "Minutes a day",
                    description = "Once they're used, the apps step aside until tomorrow",
                    enabled = editable,
                ) {
                    ObsidianToggle(
                        rules.dailyMinutes != null,
                        { on -> setRules { it.copy(dailyMinutes = if (on) 30 else null) } },
                        enabled = editable,
                    )
                }
                AnimatedVisibility(rules.dailyMinutes != null) {
                    Column {
                        SubOptionRow("Each day") {
                            NumberStepper(
                                rules.dailyMinutes ?: 30,
                                { v -> setRules { it.copy(dailyMinutes = v) } },
                                5..480,
                                step = 5,
                                suffix = " min",
                                presets = listOf(10, 15, 20, 30, 45, 60, 90, 120),
                                title = "minutes a day",
                                enabled = editable,
                            )
                        }
                        if (rules.apps.size > 1) {
                            Text(
                                "Shared or each",
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.textMuted,
                                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                            )
                            SegmentedChoice(
                                options = listOf("Shared", "Each app"),
                                selected = if (rules.shared) 0 else 1,
                                onSelect = { i -> setRules { it.copy(shared = i == 0) } },
                                enabled = editable,
                            )
                            Text(
                                if (rules.shared) {
                                    "${formatMinutes((rules.dailyMinutes ?: 0).toLong())} across all of them, so time doesn't just move from one to another."
                                } else {
                                    "${formatMinutes((rules.dailyMinutes ?: 0).toLong())} in each of them."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textMuted,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                        Text(
                            "When it runs out",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textMuted,
                            modifier = Modifier.padding(top = 14.dp, bottom = 8.dp),
                        )
                        // Four answers don't fit one row of segments, so they wrap as chips.
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                OutOfTime.TOMORROW to "Wait for tomorrow",
                                OutOfTime.MORE_TIME to "Set extra time",
                                OutOfTime.CHOOSE to "You choose",
                                OutOfTime.REST_OF_DAY to "Rest of the day",
                            ).forEach { (option, label) ->
                                QuickChip(label, rules.outOfTime == option, editable) { setRules { it.copy(outOfTime = option) } }
                            }
                        }
                        Text(
                            when (rules.outOfTime) {
                                OutOfTime.TOMORROW -> "No way to get more. The apps come back at midnight."
                                OutOfTime.MORE_TIME -> "The steps below give you a set amount more, and get longer each time if you choose."
                                OutOfTime.CHOOSE -> "When you need more, you first say how much — some minutes, or the rest of today — then do the steps below."
                                OutOfTime.REST_OF_DAY -> "The steps below lift the limit until midnight."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textMuted,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                        AnimatedVisibility(rules.outOfTime == OutOfTime.MORE_TIME) {
                            SubOptionRow("Each time") {
                                NumberStepper(
                                    rules.extraMinutes,
                                    { v -> setRules { it.copy(extraMinutes = v) } },
                                    1..120,
                                    suffix = " min",
                                    presets = listOf(5, 10, 15, 20, 30),
                                    title = "more time",
                                    enabled = editable,
                                )
                            }
                        }
                        AnimatedVisibility(rules.outOfTime != OutOfTime.TOMORROW) {
                            Column {
                                SectionLabel("The steps for more, in this order")
                                UnlockStepsEditor(
                                    rules.unlock,
                                    { u -> setRules { it.copy(unlock = u) } },
                                    editable = editable,
                                    newPassword = newPassword,
                                    onNewPasswordChange = onNewPasswordChange,
                                )
                            }
                        }
                    }
                }

                SectionLabel("What it says")
                MessageChoiceRow(
                    "What the cover says",
                    draft.message,
                    { onDraftChange(draft.copy(message = it)) },
                    enabled = editable,
                )

                SectionLabel("When it can be changed")
                OptionRow(
                    Icons.Default.Lock,
                    "Changeable only at certain hours",
                    description = "Outside them it can't be edited or deleted",
                    enabled = editable,
                ) {
                    ObsidianToggle(
                        draft.editWindow != null,
                        { on -> onDraftChange(draft.copy(editWindow = if (on) EditWindow() else null)) },
                        enabled = editable,
                    )
                }
                AnimatedVisibility(draft.editWindow != null) {
                    val window = draft.editWindow ?: EditWindow()
                    Column(Modifier.padding(top = 8.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TimeTile("From", formatMinuteOfDay(context, window.startMinute), Modifier.weight(1f), enabled = editable, onClick = onEditWindowStart)
                            TimeTile(
                                "Until",
                                formatMinuteOfDay(context, window.endMinute),
                                Modifier.weight(1f),
                                caption = if (window.endMinute <= window.startMinute) "next day" else null,
                                enabled = editable,
                                onClick = onEditWindowEnd,
                            )
                        }
                        Text(
                            "Outside these hours this page is read-only, and the limit can't be deleted. " +
                                "Loosening it still waits for midnight either way.",
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textMuted,
                            modifier = Modifier.padding(top = 10.dp),
                        )
                    }
                }

                if (!isNew && editable) {
                    Spacer(Modifier.height(18.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = onDelete) { Text("Delete limit", color = c.red, fontWeight = FontWeight.Medium) }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
