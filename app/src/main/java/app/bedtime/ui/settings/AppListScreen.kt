package app.bedtime.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.apps.AppCatalog
import app.bedtime.data.AppNameSize
import app.bedtime.data.AppSettings
import app.bedtime.data.Keywords
import app.bedtime.data.Repository
import app.bedtime.ui.apps.AppPickerScreen
import app.bedtime.ui.apps.PickerMode
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.DragList
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.OptionRow
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.SegmentedChoice
import app.bedtime.ui.components.Text
import app.bedtime.ui.components.VerbatimText
import app.bedtime.ui.pluralApps
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Your home screen's apps: favourites in your order, the ones left out, your own names and keywords,
 * and how big the names are. Holding an app on the home screen changes the same things.
 */
@Composable
fun AppListScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings: AppSettings? by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    var labels by remember { mutableStateOf(emptyMap<String, String>()) }
    LaunchedEffect(Unit) {
        labels = withContext(Dispatchers.IO) { AppCatalog.launchableApps(context).associate { it.packageName to it.label } }
    }
    var picking by rememberSaveable { mutableStateOf<String?>(null) }
    val current = settings ?: return
    fun save(change: (AppSettings) -> AppSettings) {
        scope.launch { repo.updateSettings(change) }
    }
    when (picking) {
        PICK_FAVOURITES -> {
            AppPickerScreen(
                "Favourites",
                current.homeFavourites.toSet(),
                PickerMode.FAVOURITE,
                onDone = { apps ->
                    // Those kept stay where they were; new ones join at the end, by name.
                    save { s -> s.copy(homeFavourites = s.homeFavourites.filter { it in apps } + (apps - s.homeFavourites.toSet()).sortedBy { labels[it]?.lowercase() ?: it }) }
                    picking = null
                },
                onBack = { picking = null },
            )
            return
        }
        PICK_HIDDEN -> {
            AppPickerScreen(
                "Hidden from the lists",
                current.hiddenApps,
                PickerMode.HIDE,
                onDone = { apps ->
                    save { it.copy(hiddenApps = apps) }
                    picking = null
                },
                onBack = { picking = null },
            )
            return
        }
    }
    AppListContent(
        settings = current,
        labels = labels,
        onBack = onBack,
        onPickFavourites = { picking = PICK_FAVOURITES },
        onPickHidden = { picking = PICK_HIDDEN },
        onChange = ::save,
    )
}

private const val PICK_FAVOURITES = "favourites"
private const val PICK_HIDDEN = "hidden"

@Composable
internal fun AppListContent(
    settings: AppSettings,
    labels: Map<String, String>,
    onBack: () -> Unit,
    onPickFavourites: () -> Unit = {},
    onPickHidden: () -> Unit = {},
    onChange: ((AppSettings) -> AppSettings) -> Unit = {},
) {
    val c = Obsidian.colors
    val style = settings.homeStyle
    fun name(pkg: String) = settings.appNames[pkg] ?: labels[pkg] ?: pkg
    val appKeywords = settings.keywords.filterKeys { it.startsWith(Keywords.APP) }
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("The app list", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "On your home screen and in its search. Holding an app there changes these too: it's where you rename one or give it a keyword.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            SectionCard(title = "Favourites", subtitle = "On the home screen outside sessions; drag by the handle to change the order") {
                DragList(settings.homeFavourites, key = { it }, onMoved = { moved -> onChange { it.copy(homeFavourites = moved) } }) { pkg ->
                    VerbatimText(name(pkg), style = MaterialTheme.typography.bodyLarge, color = c.textNormal, modifier = Modifier.weight(1f).padding(start = 8.dp))
                    RemoveButton("take ${name(pkg)} off") { onChange { it.copy(homeFavourites = it.homeFavourites - pkg) } }
                }
                if (settings.homeFavourites.isEmpty()) Text("None yet.", style = MaterialTheme.typography.bodyMedium, color = c.textFaint, modifier = Modifier.padding(vertical = 8.dp))
                Row(
                    Modifier.fillMaxWidth().clip(CircleShape).clickable(onClick = onPickFavourites).padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = c.accent, modifier = Modifier.size(20.dp))
                    Text("Choose favourites", style = MaterialTheme.typography.bodyLarge, color = c.accentText, modifier = Modifier.padding(start = 10.dp))
                }
            }
            SectionCard {
                OptionRow(
                    Icons.Default.Clear,
                    "Hidden apps",
                    description = if (settings.hiddenApps.isEmpty()) "None · they still open, and still follow sessions" else "${pluralApps(settings.hiddenApps.size)} left out of the lists and search",
                    onClick = onPickHidden,
                ) { Chevron() }
            }
            SectionCard(title = "Your own names", subtitle = "Used on the home screen and in its lists; × goes back to the app's own") {
                if (settings.appNames.isEmpty()) Text("None yet. Hold an app on the home screen to rename it.", style = MaterialTheme.typography.bodyMedium, color = c.textFaint)
                settings.appNames.entries.sortedBy { it.value.lowercase() }.forEach { (pkg, own) ->
                    NamedRow(own, labels[pkg] ?: pkg) { onChange { it.copy(appNames = it.appNames - pkg) } }
                }
            }
            SectionCard(title = "Keywords for apps", subtitle = "Typed alone in the search, they open the app") {
                if (appKeywords.isEmpty()) Text("None yet. Hold an app on the home screen to give it one.", style = MaterialTheme.typography.bodyMedium, color = c.textFaint)
                appKeywords.entries.sortedBy { it.value }.forEach { (key, keyword) ->
                    NamedRow("“$keyword”", name(key.removePrefix(Keywords.APP))) { onChange { it.copy(keywords = it.keywords - key) } }
                }
            }
            SectionCard(title = "Sizes") {
                Text("App names", style = MaterialTheme.typography.labelMedium, color = c.textMuted, modifier = Modifier.padding(bottom = 8.dp))
                SegmentedChoice(
                    options = listOf("Very small", "Small", "Medium", "Large"),
                    selected = style.appNameSize.ordinal,
                    onSelect = { i -> onChange { it.copy(homeStyle = it.homeStyle.copy(appNameSize = AppNameSize.entries[i])) } },
                )
                Text("Full app list and search", style = MaterialTheme.typography.labelMedium, color = c.textMuted, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                SegmentedChoice(
                    options = listOf("Very small", "Small", "Medium", "Large"),
                    selected = style.listSize.ordinal,
                    onSelect = { i -> onChange { it.copy(homeStyle = it.homeStyle.copy(listSize = AppNameSize.entries[i])) } },
                )
                Spacer(Modifier.height(8.dp))
                OptionRow(BedtimeIcons.Grid, "App icons", description = "Small icons next to app names") {
                    ObsidianToggle(style.showIcons, { on -> onChange { it.copy(homeStyle = it.homeStyle.copy(showIcons = on)) } })
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** Your word for something, what it stands for under it, and a × to take it back. */
@Composable
private fun NamedRow(title: String, under: String, onRemove: () -> Unit) {
    val c = Obsidian.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            VerbatimText(title, style = MaterialTheme.typography.bodyLarge, color = c.textNormal)
            VerbatimText(under, style = MaterialTheme.typography.bodySmall, color = c.textFaint)
        }
        RemoveButton("remove $title", onRemove)
    }
}

@Composable
private fun RemoveButton(label: String, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clip(CircleShape).clickable(onClickLabel = label, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Clear, contentDescription = label, tint = Obsidian.colors.textFaint, modifier = Modifier.size(18.dp))
    }
}
