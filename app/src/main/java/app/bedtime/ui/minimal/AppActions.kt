package app.bedtime.ui.minimal

import androidx.compose.material.icons.filled.Search
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.net.Uri
import android.os.Process
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.bedtime.apps.AppEntry
import app.bedtime.data.HomeStyle
import app.bedtime.ui.components.AppIcon
import app.bedtime.ui.components.Text
import app.bedtime.ui.homestyle.HomePalette
import app.bedtime.ui.homestyle.palette

/** One of an app's own shortcuts ("new message", "scan a code"), as its id and label. */
data class AppShortcut(val id: String, val label: String)

/** What can be done with an app from the home screen, wired to the phone. */
class AppActionHandlers(
    val toggleFavourite: () -> Unit = {},
    val move: (up: Boolean) -> Unit = {},
    val rename: (String) -> Unit = {},
    val setKeyword: (String) -> Unit = {},
    val toggleHidden: () -> Unit = {},
    val info: () -> Unit = {},
    val uninstall: () -> Unit = {},
    val shortcut: (AppShortcut) -> Unit = {},
)

/** The sheet a long press on an app opens, over the home screen. */
@Composable
internal fun AppActionsDialog(
    app: AppEntry,
    style: HomeStyle,
    favourite: Boolean,
    hidden: Boolean,
    canUninstall: Boolean,
    shortcuts: List<AppShortcut>,
    handlers: AppActionHandlers,
    onDismiss: () -> Unit,
    keyword: String = "",
) {
    Dialog(onDismissRequest = onDismiss) {
        AppActionsContent(app, style, favourite, hidden, canUninstall, shortcuts, handlers, onDone = onDismiss, keyword = keyword)
    }
}

/**
 * The app's own shortcuts first, then: on the home screen or off it (and its place there), a name of
 * your own, hidden from the lists or not, its info, and uninstalling outside sessions.
 */
@Composable
internal fun AppActionsContent(
    app: AppEntry,
    style: HomeStyle,
    favourite: Boolean,
    hidden: Boolean,
    canUninstall: Boolean,
    shortcuts: List<AppShortcut>,
    handlers: AppActionHandlers,
    onDone: () -> Unit = {},
    initialRenaming: Boolean = false,
    /** Its keyword in the search, typed alone to open it; empty if none. */
    keyword: String = "",
) {
    val p = style.palette()
    var renaming by remember { mutableStateOf(initialRenaming) }
    var keying by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(app.label) }
    var typed by remember { mutableStateOf(keyword) }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(p.background)
            .border(1.dp, p.faint.copy(alpha = 0.25f), RoundedCornerShape(24.dp))
            .padding(vertical = 18.dp),
    ) {
        Row(Modifier.padding(horizontal = 22.dp).padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(app.packageName, app.label, 28.dp)
            Spacer(Modifier.width(12.dp))
            Text(app.label, style = MaterialTheme.typography.titleMedium, color = p.text)
        }
        if (keying) {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 6.dp)) {
                Text("Type it in the search, then Enter, to open ${app.label}.", style = MaterialTheme.typography.bodySmall, color = p.faint)
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = typed,
                    onValueChange = { typed = it.filterNot { c -> c.isWhitespace() }.lowercase().take(4) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = p.text),
                    cursorBrush = SolidColor(p.accent),
                    modifier = Modifier.fillMaxWidth().border(1.dp, p.faint.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).padding(12.dp),
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Save",
                        style = MaterialTheme.typography.labelLarge,
                        color = p.accent,
                        modifier = Modifier.clip(RoundedCornerShape(50)).clickable {
                            handlers.setKeyword(typed)
                            onDone()
                        }.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            return@Column
        }
        if (renaming) {
            Column(Modifier.padding(horizontal = 22.dp, vertical = 6.dp)) {
                BasicTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = p.text),
                    cursorBrush = SolidColor(p.accent),
                    modifier = Modifier.fillMaxWidth().border(1.dp, p.faint.copy(alpha = 0.5f), RoundedCornerShape(12.dp)).padding(12.dp),
                )
                Spacer(Modifier.height(8.dp))
                Row {
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Save",
                        style = MaterialTheme.typography.labelLarge,
                        color = p.accent,
                        modifier = Modifier.clip(RoundedCornerShape(50)).clickable {
                            handlers.rename(name.trim())
                            onDone()
                        }.padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
            return@Column
        }
        shortcuts.forEach { s -> ActionRow(Icons.AutoMirrored.Filled.KeyboardArrowRight, s.label, p) { handlers.shortcut(s); onDone() } }
        if (shortcuts.isNotEmpty()) Spacer(Modifier.height(6.dp))
        ActionRow(if (favourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, if (favourite) "Take off the home screen" else "Add to the home screen", p) {
            handlers.toggleFavourite()
            onDone()
        }
        if (favourite) {
            ActionRow(Icons.Default.KeyboardArrowUp, "Move up", p) { handlers.move(true) }
            ActionRow(Icons.Default.KeyboardArrowDown, "Move down", p) { handlers.move(false) }
        }
        ActionRow(Icons.Default.Edit, "Rename", p) { renaming = true }
        ActionRow(Icons.Default.Search, if (keyword.isEmpty()) "Give it a keyword" else "Keyword: $keyword", p) { keying = true }
        ActionRow(Icons.Default.Clear, if (hidden) "Show in the lists again" else "Hide from the lists", p) {
            handlers.toggleHidden()
            onDone()
        }
        ActionRow(Icons.Default.Info, "App info", p) { handlers.info(); onDone() }
        if (canUninstall) ActionRow(Icons.Default.Delete, "Uninstall", p) { handlers.uninstall(); onDone() }
    }
}

@Composable
private fun ActionRow(icon: ImageVector, label: String, p: HomePalette, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = p.faint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = p.text)
    }
}

/**
 * Apps' own shortcuts, which Android shares only with the phone's default home screen; none
 * otherwise. Kept by id for starting them.
 */
object AppShortcuts {
    private const val MAX = 5

    fun of(context: Context, pkg: String): List<ShortcutInfo> {
        val launcher = context.getSystemService(LauncherApps::class.java) ?: return emptyList()
        if (!runCatching { launcher.hasShortcutHostPermission() }.getOrDefault(false)) return emptyList()
        val query = LauncherApps.ShortcutQuery()
            .setPackage(pkg)
            .setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED)
        return runCatching { launcher.getShortcuts(query, Process.myUserHandle()) }.getOrNull().orEmpty()
            .filter { it.isEnabled }
            .sortedBy { it.rank }
            .take(MAX)
    }

    fun start(context: Context, shortcut: ShortcutInfo) {
        runCatching { context.getSystemService(LauncherApps::class.java)?.startShortcut(shortcut, null, null) }
    }

    fun infoIntent(pkg: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", pkg, null)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun uninstallIntent(pkg: String): Intent =
        Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
