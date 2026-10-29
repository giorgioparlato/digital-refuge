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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.bedtime.data.Problems
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.VerbatimText
import app.bedtime.ui.theme.Obsidian

/** What went wrong lately, newest first, to read here or copy into a message. */
@Composable
fun ProblemsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var lines by remember { mutableStateOf(Problems.read()) }
    ProblemsContent(
        lines = lines,
        onCopy = {
            context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText("digital refuge problems", lines.joinToString("\n")))
            Toast.makeText(context, "copied", Toast.LENGTH_SHORT).show()
        },
        onClear = {
            Problems.clear()
            lines = emptyList()
        },
        onBack = onBack,
    )
}

@Composable
internal fun ProblemsContent(lines: List<String>, onCopy: () -> Unit, onClear: () -> Unit, onBack: () -> Unit) {
    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Recent problems", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "What failed lately and where, newest first: the last ${Problems.MAX}. Only what went wrong is noted, never what was on the screen. Copy it into a message if something isn't working.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            if (lines.isEmpty()) {
                Text("Nothing has gone wrong.", style = MaterialTheme.typography.bodyLarge, color = c.textFaint, modifier = Modifier.padding(vertical = 12.dp))
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onCopy) { Text("Copy all", color = c.accentText, fontWeight = FontWeight.SemiBold) }
                    TextButton(onClick = onClear) { Text("Clear", color = c.textMuted) }
                }
                lines.forEach { VerbatimText(it, style = MaterialTheme.typography.bodySmall, color = c.textNormal) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
