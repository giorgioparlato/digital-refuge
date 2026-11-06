package app.bedtime.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.Repository
import app.bedtime.service.HomeRole
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.PlainButton
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.launch

/**
 * Refuge's home screen as your everyday launcher, or only in minimal sessions: a choice of its own in the
 * home screen's settings, not a step of setup, as nothing needs it.
 */
@Composable
fun LauncherScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    var isDefaultHome by remember { mutableStateOf(HomeRole.isDefault(context)) }
    // Android only shows its "use as your home app?" when asked for an answer back. If it comes back at
    // once without asking (declined before, or held elsewhere for good), its own home-app page is where
    // to choose instead.
    var askedHomeAt by remember { mutableLongStateOf(0L) }
    val chooseHome = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isDefaultHome = HomeRole.isDefault(context)
        if (!isDefaultHome && System.currentTimeMillis() - askedHomeAt < 800) {
            runCatching { context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }
    val askHome = {
        askedHomeAt = System.currentTimeMillis()
        runCatching { chooseHome.launch(HomeRole.askIntent(context)) }
    }
    LifecycleResumeEffect(Unit) {
        isDefaultHome = HomeRole.isDefault(context)
        onPauseOrDispose { }
    }
    val on = settings?.homeScreen == true
    val set = { use: Boolean ->
        HomeRole.setOffered(context, use)
        scope.launch { repo.updateSettings { it.copy(homeScreen = use) } }
        // Switched on, Android asks; switched off, it falls back to your other home app by itself.
        if (use) askHome()
    }
    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Everyday launcher", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Optional. Without it, refuge's home screen is there only during the sessions you choose, and your usual launcher the rest of the time.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            SectionCard {
                Text(
                    "The minimal screen all the time, with your favourite apps. Switch it off any time and your previous launcher is back.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textNormal,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                when {
                    !on -> PlainButton("Use as everyday launcher", onClick = { set(true) }, modifier = Modifier.fillMaxWidth())
                    !isDefaultHome -> {
                        Text("Not your home app yet.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.padding(bottom = 8.dp))
                        PlainButton("Choose it as your home app", onClick = { askHome() }, modifier = Modifier.fillMaxWidth())
                        PlainButton("Stop using it", onClick = { set(false) }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
                    }
                    else -> {
                        Text("Pressing Home brings you to the minimal screen.", style = MaterialTheme.typography.bodyMedium, color = c.textMuted, modifier = Modifier.padding(bottom = 8.dp))
                        PlainButton("Stop using it as launcher", onClick = { set(false) }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}
