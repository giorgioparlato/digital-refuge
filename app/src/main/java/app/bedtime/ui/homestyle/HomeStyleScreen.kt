package app.bedtime.ui.homestyle

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import app.bedtime.ui.components.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.apps.AppCatalog
import app.bedtime.apps.AppEntry
import app.bedtime.data.AppSettings
import app.bedtime.data.HomeStyle
import app.bedtime.data.Repository
import app.bedtime.data.TextSize
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.ObsidianTextField
import app.bedtime.ui.components.ObsidianToggle
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.OptionRow
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.SegmentedChoice
import app.bedtime.ui.formatMinuteOfDay
import app.bedtime.ui.minimal.MinimalHomeContent
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

@Composable
fun HomeStyleScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings: AppSettings? by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    var apps by remember { mutableStateOf(emptyList<AppEntry>()) }
    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) { AppCatalog.launchableApps(context).take(4) }
    }
    val current = settings ?: return
    HomeStyleContent(
        style = current.homeStyle,
        onChange = { style -> scope.launch { repo.updateSettings { it.copy(homeStyle = style) } } },
        previewApps = apps,
        now = LocalDateTime.now(),
        onBack = onBack,
    )
}

@Composable
internal fun HomeStyleContent(
    style: HomeStyle,
    onChange: (HomeStyle) -> Unit,
    previewApps: List<AppEntry>,
    now: LocalDateTime,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Minimal home screen", onBack = onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "What you see when a schedule or block uses minimal mode. Changes save as you go.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textMuted,
            )
            val previewShape = RoundedCornerShape(24.dp)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(460.dp)
                    .clip(previewShape)
                    .border(1.dp, c.border, previewShape),
            ) {
                MinimalHomeContent(
                    now = now,
                    scheduleName = "Bedtime",
                    until = formatMinuteOfDay(context, 7 * 60),
                    apps = previewApps,
                    style = style,
                    onLaunch = {},
                    onUnlock = {},
                    preview = true,
                )
            }

            SectionCard(title = "Background") {
                SwatchRow(BackgroundPresets, style.background) { onChange(style.copy(background = it)) }
                HexField(style.background) { onChange(style.copy(background = it)) }
            }
            SectionCard(title = "Accent") {
                SwatchRow(AccentPresets, style.accent) { onChange(style.copy(accent = it)) }
                HexField(style.accent) { onChange(style.copy(accent = it)) }
            }
            SectionCard(title = "Text size") {
                SegmentedChoice(
                    options = listOf("Small", "Medium", "Large"),
                    selected = style.textSize.ordinal,
                    onSelect = { onChange(style.copy(textSize = TextSize.entries[it])) },
                )
            }
            SectionCard(title = "Show") {
                OptionRow(Icons.Default.Face, "Greeting", description = "“Good evening”, “Good morning”…") {
                    ObsidianToggle(style.showGreeting, { onChange(style.copy(showGreeting = it)) })
                }
                OptionRow(Icons.Default.DateRange, "Date") {
                    ObsidianToggle(style.showDate, { onChange(style.copy(showDate = it)) })
                }
                OptionRow(BedtimeIcons.Grid, "App icons", description = "Small icons next to app names") {
                    ObsidianToggle(style.showIcons, { onChange(style.copy(showIcons = it)) })
                }
            }
            SectionCard {
                OptionRow(
                    BedtimeIcons.Contrast,
                    "Keep home screen in colour",
                    description = "When greyscale is on, only apps turn grey",
                ) { ObsidianToggle(style.keepInColour, { onChange(style.copy(keepInColour = it)) }) }
                OptionRow(
                    BedtimeIcons.Refuge,
                    "Show on the lock screen",
                    description = "During any session, the lock screen shows this clock and the session, without the apps",
                ) { ObsidianToggle(style.lockScreen, { onChange(style.copy(lockScreen = it)) }) }
            }
            TextButton(onClick = { onChange(HomeStyle()) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Reset to default", color = c.textMuted)
            }
        }
    }
}

@Composable
private fun SwatchRow(presets: List<Pair<String, Long>>, selected: Long, onPick: (Long) -> Unit) {
    val c = Obsidian.colors
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        presets.forEach { (name, value) ->
            val isSelected = value == selected
            val colour = Color(value)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colour)
                        .border(if (isSelected) 3.dp else 1.dp, if (isSelected) c.accent else c.border, CircleShape)
                        .clickable(onClickLabel = name) { onPick(value) },
                    contentAlignment = Alignment.Center,
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "$name selected",
                            tint = if (colour.luminance() > 0.5f) Color.Black else Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Text(name, style = MaterialTheme.typography.labelSmall, color = c.textMuted, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
private fun HexField(value: Long, onPick: (Long) -> Unit) {
    var text by remember(value) { mutableStateOf(value.toHexColor()) }
    ObsidianTextField(
        value = text,
        onValueChange = {
            text = it
            parseHexColor(it)?.let(onPick)
        },
        label = "Custom colour (#RRGGBB)",
        modifier = Modifier.padding(top = 12.dp),
    )
}
