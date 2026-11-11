package app.bedtime.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.VerbatimText
import app.bedtime.ui.theme.Obsidian

/** Someone thanked: who, what of theirs refuge took after, and where to find them. */
private data class Credit(val name: String, val what: String, val link: String)

private val THANKS = listOf(
    Credit("Struthless", "The microjournal and its prompts, and the question before unlocking: why did I pick up the phone?", "https://www.youtube.com/@Struthless"),
    Credit("Minimal Phone", "A phone that does less on purpose: the quiet home screen.", "https://minimalcompany.com"),
    Credit("Clicks Communicator", "A phone for talking to people rather than scrolling: the people who matter first.", "https://clicksphone.com/communicator"),
    Credit("Alfred", "Typing to find anything, with a word before it for where: the look-up.", "https://www.alfredapp.com"),
    Credit("Loop Habit Tracker", "Habits and their strength, kept on the phone.", "https://loophabits.org"),
    Credit("Primary, by Cecilia May", "The theme for Obsidian whose colours refuge wears: warm greys with a green accent.", "https://github.com/primary-theme/obsidian"),
)

/** Code and fonts refuge is built with, and their licences: in the app's own files, or on the web. */
private data class Used(val name: String, val what: String, val licence: String, val file: String?, val link: String)

private val OPEN_SOURCE = listOf(
    Used("whisper.cpp", "Voice notes put into words on the phone", "MIT", "licenses/whisper.cpp-MIT.txt", "https://github.com/ggml-org/whisper.cpp"),
    Used("Android Jetpack", "Compose, DataStore and AndroidX", "Apache 2.0", null, "https://www.apache.org/licenses/LICENSE-2.0"),
    Used("Kotlin", "The language, and kotlinx.serialization", "Apache 2.0", null, "https://www.apache.org/licenses/LICENSE-2.0"),
    Used("Inter", "The app's font", "SIL Open Font License", "licenses/inter-OFL.txt", "https://github.com/rsms/inter"),
    Used("Fraunces", "A font to choose", "SIL Open Font License", "licenses/fraunces-OFL.txt", "https://github.com/undercasetype/Fraunces"),
    Used("Literata", "A font to choose", "SIL Open Font License", "licenses/literata-OFL.txt", "https://github.com/googlefonts/literata"),
    Used("Nunito", "A font to choose", "SIL Open Font License", "licenses/nunito-OFL.txt", "https://github.com/googlefonts/nunito"),
    Used("Spectral", "A font to choose", "SIL Open Font License", "licenses/spectral-OFL.txt", "https://github.com/productiontype/Spectral"),
)

private fun open(context: Context, link: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

/** Thanks to those refuge took after, with a tap to each; then the open source it's built with. */
@Composable
fun CreditsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val c = Obsidian.colors
    // A licence's own text, read from the app's files, shown as written.
    var reading by remember { mutableStateOf<Pair<String, String>?>(null) }
    reading?.let { (name, text) ->
        AlertDialog(
            onDismissRequest = { reading = null },
            containerColor = c.bgSecondary,
            title = { Text(name) },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    VerbatimText(text, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                }
            },
            confirmButton = { TextButton(onClick = { reading = null }) { Text("Close", color = c.accentText) } },
        )
    }
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Credits", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Ideas refuge took after. A tap opens each in your browser.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted)
            SectionCard(title = "With thanks") {
                THANKS.forEach { credit ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClickLabel = "open ${credit.name}") { open(context, credit.link) }.padding(vertical = 8.dp),
                    ) {
                        Text("${credit.name} ›", style = MaterialTheme.typography.bodyLarge, color = c.textNormal)
                        Text(credit.what, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                    }
                }
            }
            SectionCard(title = "Open source", subtitle = "A tap shows its licence") {
                OPEN_SOURCE.forEach { used ->
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClickLabel = "its licence") {
                            val text = used.file?.let { file -> runCatching { context.assets.open(file).bufferedReader().use { it.readText() } }.getOrNull() }
                            if (text != null) reading = used.name to text else open(context, used.link)
                        }.padding(vertical = 8.dp),
                    ) {
                        Text(used.name, style = MaterialTheme.typography.bodyLarge, color = c.textNormal)
                        Text("${used.what} · ${used.licence}", style = MaterialTheme.typography.bodySmall, color = c.textMuted)
                    }
                }
            }
        }
    }
}
