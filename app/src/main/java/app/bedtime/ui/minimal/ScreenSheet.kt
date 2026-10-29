package app.bedtime.ui.minimal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.bedtime.Routes
import app.bedtime.data.HomeStyle
import app.bedtime.data.Module
import app.bedtime.data.ModuleChoice
import app.bedtime.data.Screen
import app.bedtime.ui.components.Text
import app.bedtime.ui.homestyle.palette

/**
 * A long press on an empty spot: this screen's widgets to switch on or off right here, and the way to
 * their order, the look, the gestures and every setting.
 */
@Composable
internal fun ScreenSheet(
    screen: Screen,
    style: HomeStyle,
    widgets: List<ModuleChoice>,
    sessionOwned: Boolean,
    onSwitch: (Module, Boolean) -> Unit,
    onOpen: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) { ScreenSheetContent(screen, style, widgets, sessionOwned, onSwitch, { onOpen(it); onDismiss() }) }
}

@Composable
internal fun ScreenSheetContent(
    screen: Screen,
    style: HomeStyle,
    widgets: List<ModuleChoice>,
    sessionOwned: Boolean,
    onSwitch: (Module, Boolean) -> Unit,
    onOpen: (String) -> Unit,
) {
    val p = style.palette()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(p.background)
            .border(1.dp, p.faint.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
            .padding(vertical = 18.dp),
    ) {
        Text(if (screen == Screen.HOME) "This screen" else "The lock screen", style = MaterialTheme.typography.titleMedium, color = p.text, modifier = Modifier.padding(horizontal = 22.dp))
        Text(
            if (sessionOwned) "Its widgets while this session runs. Hold one on the screen for its own options." else "Its widgets. Hold one on the screen for its own options.",
            style = MaterialTheme.typography.bodySmall,
            color = p.faint,
            modifier = Modifier.padding(horizontal = 22.dp).padding(top = 2.dp, bottom = 6.dp),
        )
        Column(Modifier.heightIn(max = 340.dp).verticalScroll(rememberScrollState())) {
            widgets.forEach { choice ->
                Row(
                    Modifier.fillMaxWidth().clickable { onSwitch(choice.module, !choice.on) }.padding(horizontal = 22.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(choice.module.label, style = MaterialTheme.typography.bodyLarge, color = if (choice.on) p.text else p.faint, modifier = Modifier.weight(1f))
                    if (choice.on) Icon(Icons.Default.Check, contentDescription = "shown", tint = p.accent, modifier = Modifier.size(18.dp))
                }
            }
        }
        Spacer(Modifier.padding(top = 6.dp))
        Link("Widgets and their order", p) { onOpen(if (screen == Screen.HOME) Routes.STYLE_SHOWN else Routes.STYLE_SHOWN_LOCK) }
        Link("Look", p) { onOpen(if (screen == Screen.HOME) Routes.HOME_STYLE else Routes.HOME_STYLE_LOCK) }
        Link("Gestures", p) { onOpen(if (screen == Screen.HOME) Routes.STYLE_GESTURES else Routes.STYLE_SWIPES) }
        Link("All settings", p) { onOpen(Routes.SETTINGS) }
    }
}

@Composable
private fun Link(label: String, p: app.bedtime.ui.homestyle.HomePalette, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = p.text, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = p.faint, modifier = Modifier.size(18.dp))
    }
}
