package app.bedtime.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import app.bedtime.service.WindowEventLog
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.SoftPillButton
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.VerbatimText
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())

/**
 * A temporary test page: records what the phone reports while you open the recent-apps view, so we
 * can tell whether that view can be recognised on this phone. Nothing is saved or sent.
 */
@Composable
fun WindowEventsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val c = Obsidian.colors
    var entries by remember { mutableStateOf(WindowEventLog.snapshot()) }
    var recording by remember { mutableStateOf(WindowEventLog.recording) }
    LifecycleResumeEffect(Unit) {
        entries = WindowEventLog.snapshot()
        recording = WindowEventLog.recording
        onPauseOrDispose { }
    }
    LaunchedEffect(recording) {
        while (recording) {
            delay(1_000)
            entries = WindowEventLog.snapshot()
            recording = WindowEventLog.recording
        }
    }
    // Exactly as the phone sent it: case matters here, so this is the one page that isn't lowercased.
    val report = entries.joinToString("\n") { e ->
        "${CLOCK.format(Instant.ofEpochMilli(e.at))}  ${e.pkg}  ${e.cls ?: "-"}  text=[${e.text}]  desc=[${e.description ?: ""}]"
    }
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Recent apps test", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                "This finds out whether your phone lets the recent-apps view be recognised. Do it while no session " +
                    "with minimal mode is running.\n\n1. Tap start.\n2. Go to your home screen, then swipe up and hold to " +
                    "open recent apps. Then press home once more.\n3. Come back here and tap copy, then paste it in the chat.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SoftPillButton(if (recording) "Recording…" else "Start", onClick = {
                    WindowEventLog.start()
                    recording = true
                    entries = emptyList()
                }, enabled = !recording)
                if (recording) {
                    TextButton(onClick = {
                        WindowEventLog.stop()
                        recording = false
                    }) { Text("Stop", color = c.textMuted) }
                }
            }
            SectionCard(title = "What the phone reported", subtitle = if (entries.isEmpty()) "Nothing yet" else "${entries.size} screens") {
                VerbatimText(
                    report.ifEmpty { "—" },
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    color = c.textNormal,
                )
            }
            TextButton(
                enabled = entries.isNotEmpty(),
                onClick = {
                    context.getSystemService(ClipboardManager::class.java)
                        .setPrimaryClip(ClipData.newPlainText("recent apps test", report))
                    Toast.makeText(context, "copied", Toast.LENGTH_SHORT).show()
                },
            ) { Text("Copy", color = if (entries.isNotEmpty()) c.accentText else c.textFaint) }
            Spacer(Modifier.height(24.dp))
        }
    }
}
