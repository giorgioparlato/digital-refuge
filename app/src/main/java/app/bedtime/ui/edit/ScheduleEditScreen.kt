package app.bedtime.ui.edit

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import app.bedtime.ui.components.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.apps.AppCatalog
import app.bedtime.data.AppJson
import app.bedtime.data.DndMode
import app.bedtime.data.Repository
import app.bedtime.data.Schedule
import app.bedtime.data.Templates
import app.bedtime.data.UnlockConfig
import app.bedtime.data.UnlockMode
import app.bedtime.engine.Engine
import app.bedtime.service.DndController
import app.bedtime.service.GreyscaleController
import app.bedtime.service.SystemApps
import app.bedtime.ui.apps.AppPickerScreen
import app.bedtime.ui.apps.PickerMode
import app.bedtime.ui.components.AppIconStack
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.BottomActionBar
import app.bedtime.ui.components.Callout
import app.bedtime.ui.components.CalloutKind
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.DaySelector
import app.bedtime.ui.components.LowercaseStrings
import app.bedtime.ui.components.NumberStepper
import app.bedtime.ui.components.ObsidianTextField
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.OptionRow
import app.bedtime.ui.components.QuickChip
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.SegmentedChoice
import app.bedtime.ui.components.SubOptionRow
import app.bedtime.ui.components.TimeTile
import app.bedtime.ui.formatDays
import app.bedtime.ui.formatMinuteOfDay
import app.bedtime.ui.formatWaitSeconds
import app.bedtime.ui.formatMinutes
import app.bedtime.ui.is24Hour
import app.bedtime.ui.pluralApps
import app.bedtime.ui.scheduleMinutes
import app.bedtime.ui.theme.Obsidian
import app.bedtime.unlock.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private val ScheduleSaver = Saver<Schedule, String>(
    save = { AppJson.encodeToString(Schedule.serializer(), it) },
    restore = { AppJson.decodeFromString(Schedule.serializer(), it) },
)

private const val PICK_BLOCKED = "blocked"
private const val PICK_ALLOWED = "allowed"
private const val TIME_START = "start"
private const val TIME_END = "end"

/**
 * Edits an existing schedule or block ([scheduleId]), or creates a new one from a [Templates] key
 * (including the "from scratch" keys).
 */
@Composable
fun ScheduleEditScreen(
    scheduleId: String?,
    template: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onUnlock: (String) -> Unit,
    onSetup: () -> Unit,
) {
    val context = LocalContext.current
    val c = Obsidian.colors
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val state by Engine.state(context).collectAsStateWithLifecycle()
    val essentials = remember { SystemApps.essentials(context) }

    var original by rememberSaveable(stateSaver = ScheduleSaver) {
        mutableStateOf(
            template?.let { key -> Templates.instantiate(key, { AppCatalog.isLaunchable(context, it) }, essentials) }
                ?: Schedule(name = "", allowedApps = essentials),
        )
    }
    var draft by rememberSaveable(stateSaver = ScheduleSaver) { mutableStateOf(original) }
    var loaded by rememberSaveable { mutableStateOf(scheduleId == null) }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var picking by rememberSaveable { mutableStateOf<String?>(null) }
    var editingTime by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var greyscaleAvailable by remember { mutableStateOf(GreyscaleController.isAvailable(context)) }
    var dndAvailable by remember { mutableStateOf(DndController.hasAccess(context)) }
    LifecycleResumeEffect(Unit) {
        greyscaleAvailable = GreyscaleController.isAvailable(context)
        dndAvailable = DndController.hasAccess(context)
        onPauseOrDispose { }
    }

    LaunchedEffect(scheduleId) {
        if (!loaded && scheduleId != null) {
            repo.schedules.first().firstOrNull { it.id == scheduleId }?.let {
                original = it
                draft = it
            }
            loaded = true
        }
    }
    if (!loaded) return

    // While a schedule is enforcing, editing it would be an escape hatch around the unlock challenge.
    val readOnly = scheduleId != null && state?.occurrenceOf(scheduleId) != null
    val dirty = !readOnly && (draft != original || newPassword.isNotEmpty())

    when (picking) {
        PICK_BLOCKED -> {
            AppPickerScreen("Apps to block", draft.blockedApps, PickerMode.BLOCK, onDone = {
                draft = draft.copy(blockedApps = it)
                picking = null
            }, onBack = { picking = null })
            return
        }
        PICK_ALLOWED -> {
            AppPickerScreen("Apps to allow", draft.allowedApps, PickerMode.ALLOW, onDone = {
                draft = draft.copy(allowedApps = it)
                picking = null
            }, onBack = { picking = null })
            return
        }
    }

    fun requestBack() {
        if (dirty) confirmDiscard = true else onBack()
    }
    BackHandler { requestBack() }

    editingTime?.let { which ->
        TimePickerDialog(
            title = if (which == TIME_START) "Starts at" else "Ends at",
            initialMinute = if (which == TIME_START) draft.startMinute else draft.endMinute,
            onDismiss = { editingTime = null },
            onConfirm = { minute ->
                draft = if (which == TIME_START) draft.copy(startMinute = minute) else draft.copy(endMinute = minute)
                editingTime = null
            },
        )
    }

    fun save() {
        val problem = when {
            !draft.isBlock && draft.days.isEmpty() -> "Pick at least one day for this schedule."
            draft.unlock.passwordEnabled && !draft.unlock.hasPassword && newPassword.isBlank() ->
                "Choose a password, or switch off the password step."
            else -> null
        }
        if (problem != null) {
            error = problem
            return
        }
        saving = true
        scope.launch {
            var toSave = draft.copy(name = draft.name.trim().ifEmpty { if (draft.isBlock) "Focus block" else "Schedule" })
            if (toSave.unlock.passwordEnabled && newPassword.isNotBlank()) {
                val (hash, salt) = withContext(Dispatchers.Default) { PasswordHasher.hash(newPassword) }
                toSave = toSave.copy(unlock = toSave.unlock.copy(passwordHash = hash, passwordSalt = salt))
            }
            repo.upsert(toSave)
            onSaved()
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = c.bgSecondary,
            title = { Text("Delete “${draft.name}”?") },
            text = { Text("This can't be undone.", color = c.textMuted) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    scope.launch {
                        repo.delete(draft.id)
                        onSaved()
                    }
                }) { Text("Delete", color = c.red, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Keep it", color = c.textMuted) } },
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            containerColor = c.bgSecondary,
            title = { Text("Discard changes?") },
            text = { Text("Your edits won't be saved.", color = c.textMuted) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onBack()
                }) { Text("Discard", color = c.red, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing", color = c.textMuted) } },
        )
    }

    ScheduleEditContent(
        draft = draft,
        onDraftChange = {
            draft = it
            error = null
        },
        isNew = scheduleId == null,
        readOnly = readOnly,
        newPassword = newPassword,
        onNewPasswordChange = { newPassword = it },
        greyscaleAvailable = greyscaleAvailable,
        dndAvailable = dndAvailable,
        error = error,
        saving = saving,
        onBack = ::requestBack,
        onSave = ::save,
        onDelete = { confirmDelete = true },
        onUnlock = { scheduleId?.let(onUnlock) },
        onPickBlocked = { picking = PICK_BLOCKED },
        onPickAllowed = { picking = PICK_ALLOWED },
        onEditStart = { editingTime = TIME_START },
        onEditEnd = { editingTime = TIME_END },
        onOpenSetup = onSetup,
        appLabel = { AppCatalog.label(context, it) },
        essentials = essentials,
    )
}

@Composable
internal fun ScheduleEditContent(
    draft: Schedule,
    onDraftChange: (Schedule) -> Unit,
    isNew: Boolean,
    readOnly: Boolean,
    newPassword: String,
    onNewPasswordChange: (String) -> Unit,
    greyscaleAvailable: Boolean,
    dndAvailable: Boolean,
    error: String?,
    saving: Boolean,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onUnlock: () -> Unit,
    onPickBlocked: () -> Unit,
    onPickAllowed: () -> Unit,
    onEditStart: () -> Unit,
    onEditEnd: () -> Unit,
    onOpenSetup: () -> Unit,
    appLabel: (String) -> String = { it },
    /** Pre-filled as allowed apps when minimal mode is switched on with none chosen yet. */
    essentials: Set<String> = emptySet(),
) {
    val context = LocalContext.current
    val c = Obsidian.colors
    val editable = !readOnly
    val unlock = draft.unlock
    val isBlock = draft.isBlock

    fun setUnlock(transform: (UnlockConfig) -> UnlockConfig) = onDraftChange(draft.copy(unlock = transform(unlock)))

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            ObsidianTopBar(
                when {
                    isNew && isBlock -> "New block"
                    isNew -> "New schedule"
                    isBlock -> "Edit block"
                    else -> "Edit schedule"
                },
                onBack = onBack,
            )
        },
        bottomBar = {
            BottomActionBar {
                if (readOnly) {
                    CtaButton("Unlock to make changes", onClick = onUnlock, modifier = Modifier.fillMaxWidth())
                } else {
                    CtaButton(
                        when {
                            saving -> "Saving…"
                            isNew && isBlock -> "Create block"
                            isNew -> "Create schedule"
                            else -> "Save changes"
                        },
                        onClick = onSave,
                        enabled = !saving,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (readOnly) {
                Callout(
                    title = if (isBlock) "This block is running" else "This schedule is on right now",
                    body = "To keep you honest, it can't be changed or deleted until you unlock it or it ends.",
                )
            }
            if (error != null) Callout(title = "Almost there", kind = CalloutKind.WARNING, body = error)

            SectionCard {
                ObsidianTextField(
                    value = draft.name,
                    onValueChange = { onDraftChange(draft.copy(name = it)) },
                    label = "Name",
                    placeholder = if (isBlock) "e.g. Deep work" else "e.g. Bedtime",
                    enabled = editable,
                )
                if (!isBlock) {
                    Spacer(Modifier.height(18.dp))
                    Text("Repeats on", style = MaterialTheme.typography.labelLarge, color = c.textMuted)
                    DaySelector(draft.days, onChange = { onDraftChange(draft.copy(days = it)) }, enabled = editable)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickChip("Every day", draft.days.size == 7, editable) { onDraftChange(draft.copy(days = (1..7).toSet())) }
                        QuickChip("Weekdays", draft.days == (1..5).toSet(), editable) { onDraftChange(draft.copy(days = (1..5).toSet())) }
                        QuickChip("Weekends", draft.days == setOf(6, 7), editable) { onDraftChange(draft.copy(days = setOf(6, 7))) }
                    }
                }
            }

            if (isBlock) {
                SectionCard(title = "Length") {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("Default length", style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.weight(1f))
                        NumberStepper(
                            draft.durationMinutes,
                            { onDraftChange(draft.copy(durationMinutes = it)) },
                            5..480,
                            step = 5,
                            suffix = " min",
                            enabled = editable,
                            presets = listOf(15, 25, 30, 45, 60, 90, 120, 180),
                            title = "default length",
                        )
                    }
                    Text(
                        "You can adjust it each time you start the block.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textMuted,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            } else {
                SectionCard(title = "Time") {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TimeTile(
                            "Starts",
                            formatMinuteOfDay(context, draft.startMinute),
                            Modifier.weight(1f),
                            enabled = editable,
                            onClick = onEditStart,
                        )
                        TimeTile(
                            "Ends",
                            formatMinuteOfDay(context, draft.endMinute),
                            Modifier.weight(1f),
                            caption = if (draft.endMinute <= draft.startMinute) "next day" else null,
                            enabled = editable,
                            onClick = onEditEnd,
                        )
                    }
                    Text(
                        "${formatDays(draft.days)} · ${formatMinutes(scheduleMinutes(draft).toLong())} each time",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textMuted,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }

            SectionCard(title = "While it's on") {
                OptionRow(
                    BedtimeIcons.Block,
                    "Block apps",
                    description = if (draft.blockedApps.isEmpty()) "None yet · tap to choose" else pluralApps(draft.blockedApps.size),
                    enabled = editable,
                    onClick = onPickBlocked,
                ) {
                    AppIconStack(draft.blockedApps.sorted(), appLabel)
                    Chevron()
                }
                OptionRow(
                    BedtimeIcons.Contrast,
                    "Greyscale",
                    description = if (greyscaleAvailable) "Fade apps to black and white" else "Needs a one-time step (see Setup)",
                    enabled = editable,
                ) { ObsidianToggle(draft.greyscale, { onDraftChange(draft.copy(greyscale = it)) }, enabled = editable) }
                OptionRow(
                    Icons.Default.Home,
                    "Minimal mode",
                    description = "Swap your home screen for a calm list of essential apps",
                    enabled = editable,
                ) {
                    ObsidianToggle(
                        draft.minimalMode,
                        { on ->
                            val allowed = if (on && draft.allowedApps.isEmpty()) essentials else draft.allowedApps
                            onDraftChange(draft.copy(minimalMode = on, allowedApps = allowed))
                        },
                        enabled = editable,
                    )
                }
                AnimatedVisibility(draft.minimalMode) {
                    OptionRow(
                        BedtimeIcons.Grid,
                        "Allowed apps",
                        description = if (draft.allowedApps.isEmpty()) {
                            "Just the phone app for now · tap to add"
                        } else {
                            "${pluralApps(draft.allowedApps.size)}, plus the phone app"
                        },
                        enabled = editable,
                        onClick = onPickAllowed,
                    ) {
                        AppIconStack(draft.allowedApps.sorted(), appLabel)
                        Chevron()
                    }
                }
            }

            SectionCard(title = "Notifications") {
                Text("Do Not Disturb", style = MaterialTheme.typography.labelLarge, color = c.textMuted)
                Spacer(Modifier.height(8.dp))
                SegmentedChoice(
                    options = listOf("Off", "Priority", "Silence"),
                    selected = draft.dnd.ordinal,
                    onSelect = { onDraftChange(draft.copy(dnd = DndMode.entries[it])) },
                    enabled = editable,
                )
                Text(
                    when (draft.dnd) {
                        DndMode.OFF -> "Calls and notifications work as usual."
                        DndMode.PRIORITY -> "Only your priority contacts can ring. Music, videos and alarms still play."
                        DndMode.SILENCE -> "No calls or notification sounds. Music, videos and alarms still play."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textMuted,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                )
                OptionRow(
                    Icons.Default.Notifications,
                    "Hide notifications",
                    description = "Held back while it's on, then they all come back",
                    enabled = editable,
                ) { ObsidianToggle(draft.hideNotifications, { onDraftChange(draft.copy(hideNotifications = it)) }, enabled = editable) }
                AnimatedVisibility((draft.dnd != DndMode.OFF || draft.hideNotifications) && !dndAvailable) {
                    Callout(
                        title = "Needs Do Not Disturb access",
                        kind = CalloutKind.INFO,
                        body = "Grant it once in Setup so digital refuge can quiet your phone.",
                        actionLabel = "Open setup",
                        onAction = onOpenSetup,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            SectionCard(title = "Leaving early", subtitle = "Make unlocking take a little effort. The steps you pick run in this order.") {
                OptionRow(BedtimeIcons.Hourglass, "Wait it out", description = "Start a timer, come back when it's done", enabled = editable) {
                    ObsidianToggle(unlock.waitEnabled, { on -> setUnlock { it.copy(waitEnabled = on) } }, enabled = editable)
                }
                AnimatedVisibility(unlock.waitEnabled) {
                    SubOptionRow("Timer") {
                        NumberStepper(
                            unlock.waitDurationSeconds,
                            { v -> setUnlock { it.copy(waitSeconds = v) } },
                            5..3600,
                            step = 5,
                            enabled = editable,
                            presets = listOf(10, 30, 60, 120, 300, 600),
                            title = "timer",
                            format = { formatWaitSeconds(it) },
                        )
                    }
                }
                OptionRow(Icons.Default.Edit, "Type random text", description = "No pasting, typos don't count", enabled = editable) {
                    ObsidianToggle(unlock.textEnabled, { on -> setUnlock { it.copy(textEnabled = on) } }, enabled = editable)
                }
                AnimatedVisibility(unlock.textEnabled) {
                    Column {
                        SubOptionRow("Characters") {
                            NumberStepper(
                                unlock.textLength,
                                { v -> setUnlock { it.copy(textLength = v) } },
                                25..2000,
                                step = 25,
                                enabled = editable,
                                presets = listOf(50, 100, 150, 200, 300, 500, 1000),
                                title = "characters",
                            )
                        }
                    }
                }
                OptionRow(Icons.Default.Lock, "Password", description = "Tip: let someone else choose it", enabled = editable) {
                    ObsidianToggle(unlock.passwordEnabled, { on -> setUnlock { it.copy(passwordEnabled = on) } }, enabled = editable)
                }
                AnimatedVisibility(unlock.passwordEnabled && editable) {
                    ObsidianTextField(
                        value = newPassword,
                        onValueChange = onNewPasswordChange,
                        label = if (unlock.hasPassword) "New password (leave empty to keep)" else "Choose a password",
                        password = true,
                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                    )
                }
                AnimatedVisibility(unlock.waitEnabled || unlock.textEnabled) {
                    Column {
                        OptionRow(
                            Icons.Default.Refresh,
                            "Harder each time",
                            description = "Each early unlock in a day makes the wait and text longer",
                            enabled = editable,
                        ) { ObsidianToggle(unlock.escalate, { on -> setUnlock { it.copy(escalate = on) } }, enabled = editable) }
                        AnimatedVisibility(unlock.escalate) {
                            SubOptionRow("How much") {
                                NumberStepper(
                                    (unlock.escalateFactor * 10).roundToInt(),
                                    { v -> setUnlock { it.copy(escalateFactor = v / 10f) } },
                                    11..40,
                                    enabled = editable,
                                    presets = listOf(15, 20, 25, 30, 40),
                                    title = "each time",
                                    format = { "%.1f×".format(it / 10f) },
                                )
                            }
                        }
                    }
                }
                val anyStep = unlock.waitEnabled || unlock.textEnabled ||
                    (unlock.passwordEnabled && (unlock.hasPassword || newPassword.isNotBlank()))
                AnimatedVisibility(!anyStep) {
                    Callout(
                        title = "No steps picked",
                        kind = CalloutKind.WARNING,
                        body = "Unlocking will take a single tap.",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            SectionCard(title = "When you unlock") {
                SegmentedChoice(
                    options = listOf(if (isBlock) "End the block" else "End for today", "Take a break"),
                    selected = draft.unlockAction.mode.ordinal,
                    onSelect = { onDraftChange(draft.copy(unlockAction = draft.unlockAction.copy(mode = UnlockMode.entries[it]))) },
                    enabled = editable,
                )
                Text(
                    when (draft.unlockAction.mode) {
                        UnlockMode.END_SESSION -> if (isBlock) "The block stops there." else "The schedule stays off until its next start."
                        UnlockMode.PAUSE -> "It switches itself back on after your break."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textMuted,
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                )
                AnimatedVisibility(draft.unlockAction.mode == UnlockMode.PAUSE) {
                    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Break length", style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.weight(1f))
                        NumberStepper(
                            draft.unlockAction.pauseMinutes,
                            { v -> onDraftChange(draft.copy(unlockAction = draft.unlockAction.copy(pauseMinutes = v))) },
                            1..240,
                            suffix = " min",
                            enabled = editable,
                            presets = listOf(5, 10, 15, 20, 30, 45, 60),
                            title = "break length",
                        )
                    }
                }
            }

            SectionCard(
                title = "Banking or ID apps",
                subtitle = "If one won't run while blocking is on, you can pause for a short break (turn the pause on in settings). Each break lasts:",
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Break length", style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.weight(1f))
                    NumberStepper(
                        draft.breakMinutes,
                        { v -> onDraftChange(draft.copy(breakMinutes = v)) },
                        1..60,
                        suffix = " min",
                        enabled = editable,
                        presets = listOf(1, 2, 3, 5, 10),
                        title = "break length",
                    )
                }
            }

            if (!isNew && editable) {
                TextButton(onClick = onDelete, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(if (isBlock) "Delete block" else "Delete schedule", color = c.red, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Start/end time: Android's clock dial (hour first, then minutes), following the phone's 12/24-hour setting. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(title: String, initialMinute: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    val context = LocalContext.current
    val state = rememberTimePickerState(initialMinute / 60, initialMinute % 60, is24Hour(context))
    Dialog(onDismissRequest = onDismiss) {
        TimeDialContent(title, state, onDismiss, onConfirm = { onConfirm(state.hour * 60 + state.minute) })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimeDialContent(title: String, state: TimePickerState, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    val c = Obsidian.colors
    Surface(shape = RoundedCornerShape(20.dp), color = c.bgSecondary) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = c.textNormal, modifier = Modifier.fillMaxWidth())
            LowercaseStrings {
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = c.bgPrimaryAlt,
                        clockDialSelectedContentColor = c.textOnAccent,
                        clockDialUnselectedContentColor = c.textNormal,
                        selectorColor = c.accentFill,
                        containerColor = c.bgSecondary,
                        periodSelectorBorderColor = c.border,
                        periodSelectorSelectedContainerColor = c.accentFill,
                        periodSelectorUnselectedContainerColor = Color.Transparent,
                        periodSelectorSelectedContentColor = c.textOnAccent,
                        periodSelectorUnselectedContentColor = c.textMuted,
                        timeSelectorSelectedContainerColor = c.accentFill,
                        timeSelectorUnselectedContainerColor = c.bgPrimaryAlt,
                        timeSelectorSelectedContentColor = c.textOnAccent,
                        timeSelectorUnselectedContentColor = c.textNormal,
                    ),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) { Text("cancel", color = c.textMuted) }
                TextButton(onClick = onConfirm) {
                    Text("done", color = c.accentText, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
