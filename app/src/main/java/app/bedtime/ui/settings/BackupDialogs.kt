package app.bedtime.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.bedtime.data.BackupSeal
import app.bedtime.ui.components.ObsidianTextField
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.launch

/** Before the weekly backup's folder is chosen: what the file holds, who can read it, and where syncing takes it. */
@Composable
internal fun BackupFolderWarning(sealed: Boolean, journal: Boolean, onChoose: () -> Unit, onDismiss: () -> Unit) {
    val c = Obsidian.colors
    val holds = "the backup holds your schedules, history, notes, habits and phone use" +
        (if (journal) ", and your microjournal with its moods and why answers." else ".")
    val who = if (sealed) {
        "it's sealed with your passphrase, so only someone who knows it can read it."
    } else {
        "it's plain text: anyone who can open the folder can read it. a backup passphrase, in these settings, seals it."
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("before you choose a folder", color = c.textNormal) },
        text = {
            Text(
                "$holds $who\n\na folder that syncs (google drive, dropbox, syncthing, a computer you share) puts a copy there too. choose one only you can reach.",
                color = c.textMuted,
            )
        },
        confirmButton = { TextButton(onClick = onChoose) { Text("choose folder", color = c.accentText, fontWeight = FontWeight.SemiBold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("cancel", color = c.textMuted) } },
        containerColor = c.bgSecondary,
    )
}

/**
 * Setting, changing or removing the backup passphrase. [onSet] makes the key, which takes a moment, so
 * the buttons wait for it.
 */
@Composable
internal fun BackupPassphraseDialog(sealed: Boolean, onSet: suspend (String) -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit) {
    val c = Obsidian.colors
    val scope = rememberCoroutineScope()
    var first by remember { mutableStateOf("") }
    var again by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    val tooShort = first.length < BackupSeal.MIN_LENGTH
    val differ = again.isNotEmpty() && again != first
    val ready = !tooShort && again == first && !working
    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { Text(if (sealed) "backup passphrase" else "seal backups with a passphrase", color = c.textNormal) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    if (sealed) {
                        "backups and exports are sealed. a new passphrase seals the next ones; files already written still open with the old one."
                    } else {
                        "backups and exports then can't be read without it, wherever they end up. forget it and there's no way back: " +
                            "not even refuge can open them. files already written stay as they were."
                    },
                    color = c.textMuted,
                )
                ObsidianTextField(
                    first, { first = it }, password = true, label = if (sealed) "new passphrase" else "passphrase", enabled = !working,
                    supportingText = if (first.isNotEmpty() && tooShort) "at least ${BackupSeal.MIN_LENGTH} characters" else null,
                    isError = first.isNotEmpty() && tooShort,
                )
                ObsidianTextField(
                    again, { again = it }, password = true, label = "the same again", enabled = !working,
                    supportingText = if (differ) "they don't match" else null, isError = differ,
                )
            }
        },
        confirmButton = {
            Row {
                if (sealed) TextButton(onClick = onRemove, enabled = !working) { Text("remove", color = c.textMuted) }
                TextButton(
                    onClick = {
                        working = true
                        scope.launch { onSet(first) }
                    },
                    enabled = ready,
                ) {
                    Text(
                        if (working) "sealing…" else if (sealed) "change" else "seal",
                        color = if (ready) c.accentText else c.textFaint,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !working) { Text("cancel", color = c.textMuted) } },
        containerColor = c.bgSecondary,
    )
}

/** Importing a sealed backup: its passphrase first. [onOpen] is false when the passphrase doesn't open it. */
@Composable
internal fun OpenSealedBackupDialog(onOpen: suspend (String) -> Boolean, onDismiss: () -> Unit) {
    val c = Obsidian.colors
    val scope = rememberCoroutineScope()
    var passphrase by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { Text("this backup is sealed", color = c.textNormal) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("its passphrase opens it, and then it replaces your schedules, blocks and settings.", color = c.textMuted)
                ObsidianTextField(
                    passphrase, {
                        passphrase = it
                        wrong = false
                    },
                    password = true, label = "passphrase", enabled = !working,
                    supportingText = if (wrong) "that passphrase doesn't open it" else null, isError = wrong,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    working = true
                    scope.launch {
                        wrong = !onOpen(passphrase)
                        working = false
                    }
                },
                enabled = passphrase.isNotEmpty() && !working,
            ) { Text(if (working) "opening…" else "open", color = c.accentText, fontWeight = FontWeight.SemiBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !working) { Text("cancel", color = c.textMuted) } },
        containerColor = c.bgSecondary,
    )
}
