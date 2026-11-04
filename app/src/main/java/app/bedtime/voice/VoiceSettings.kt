package app.bedtime.voice

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.FolderIndex
import app.bedtime.data.Repository
import app.bedtime.data.Voice
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.ui.components.OptionRow
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Voice notes, off until switched on: the folder their recordings go in, the Whisper model that hears
 * them (a file you download yourself, so refuge never goes online), and the language. Their buttons
 * show elsewhere only once all of it is there.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VoiceSettingsCard() {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val s = settings ?: return
    val c = Obsidian.colors
    val say = { text: String -> Toast.makeText(context, text, Toast.LENGTH_SHORT).show() }
    var copying by remember { mutableStateOf(false) }
    val pickFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val kept = runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }.isSuccess
        if (kept) scope.launch { repo.updateSettings { it.copy(voiceFolder = uri.toString()) } } else say("that folder can't be written to from here")
    }
    // The model is copied in, so it's there whatever happens to the download.
    val pickModel = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        copying = true
        scope.launch {
            val name = withContext(Dispatchers.IO) { copyModel(context, uri) }
            copying = false
            if (name == null) say("couldn't read that file") else repo.updateSettings { it.copy(voiceModel = name) }
        }
    }
    val folderName by produceState<String?>(null, s.voiceFolder) {
        value = s.voiceFolder?.let { withContext(Dispatchers.IO) { runCatching { FolderIndex.folderName(context, Uri.parse(it)) }.getOrNull() } }
    }
    SectionCard(title = "Voice notes", subtitle = "Recorded and heard on the phone: the recording kept in a folder of yours, its words in the microjournal") {
        OptionRow(BedtimeIcons.Mic, "Voice notes") { ObsidianToggle(s.voiceOn, { on -> scope.launch { repo.updateSettings { it.copy(voiceOn = on) } } }) }
        if (!s.voiceOn) return@SectionCard
        OptionRow(
            Icons.Default.DateRange,
            "Folder for recordings",
            description = folderName ?: "None yet. Your Obsidian vault's attachments folder, for one: Obsidian plays them",
            onClick = { pickFolder.launch(null) },
        ) { Chevron() }
        OptionRow(
            BedtimeIcons.Mic,
            "Whisper model",
            description = when {
                !Whisper.available -> "This phone can't run Whisper: it needs a 64-bit phone"
                copying -> "Copying it in…"
                s.voiceModel != null -> s.voiceModel
                else -> "None yet. Download ggml-base-q5_1.bin (57 MB) from huggingface.co/ggerganov/whisper.cpp, then choose it here"
            },
            onClick = { if (!copying) pickModel.launch(arrayOf("*/*")) },
        ) { Chevron() }
        Text("Language", style = MaterialTheme.typography.labelLarge, color = c.textMuted, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Voice.LANGUAGES.forEach { (code, name) ->
                val on = code == s.voiceLanguage
                Text(
                    name,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (on) c.accentText else c.textMuted,
                    modifier = Modifier.clip(RoundedCornerShape(50)).border(1.dp, if (on) c.accent else c.border, RoundedCornerShape(50))
                        .clickable { scope.launch { repo.updateSettings { it.copy(voiceLanguage = code) } } }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        Text(
            if (Voice.ready(s)) "Record from the microjournal, the quick note, or a long press on the notes tab."
            else "Once a folder and a model are chosen, the record buttons show in the microjournal, the quick note and the notes tab.",
            style = MaterialTheme.typography.bodySmall,
            color = c.textMuted,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}

/** The model at [uri] copied into refuge's own storage; its name, or null if it couldn't be read. */
private fun copyModel(context: android.content.Context, uri: Uri): String? = runCatching {
    val target = VoiceRecorder.modelFile(context).apply { parentFile?.mkdirs() }
    val partial = java.io.File(target.parentFile, "model.part")
    checkNotNull(context.contentResolver.openInputStream(uri)).use { input -> partial.outputStream().use { input.copyTo(it) } }
    check(partial.renameTo(target) || partial.copyTo(target, overwrite = true).exists())
    partial.delete()
    context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
        if (c.moveToFirst()) c.getString(0) else null
    } ?: "model.bin"
}.getOrNull()
