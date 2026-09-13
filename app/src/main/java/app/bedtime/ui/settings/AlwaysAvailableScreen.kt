package app.bedtime.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.AppSettings
import app.bedtime.data.Repository
import app.bedtime.ui.apps.AppPickerScreen
import app.bedtime.ui.apps.PickerMode
import kotlinx.coroutines.launch

/** Settings → Emergency → Always-available apps. */
@Composable
fun AlwaysAvailableScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings: AppSettings? by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val current = settings ?: return
    AppPickerScreen(
        title = "Always available",
        initial = current.alwaysAvailable,
        mode = PickerMode.ALWAYS,
        onDone = { selected ->
            scope.launch {
                repo.updateSettings { it.copy(alwaysAvailable = selected) }
                onBack()
            }
        },
        onBack = onBack,
    )
}
