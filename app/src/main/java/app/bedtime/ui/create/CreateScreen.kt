package app.bedtime.ui.create

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import app.bedtime.ui.components.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.bedtime.apps.AppCatalog
import app.bedtime.data.ScheduleKind
import app.bedtime.data.Template
import app.bedtime.data.TemplateIcon
import app.bedtime.data.Templates
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.Chevron
import app.bedtime.ui.components.IconBadge
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.formatDays
import app.bedtime.ui.formatMinuteOfDay
import app.bedtime.ui.formatMinutes
import app.bedtime.ui.theme.Obsidian

/** "New": start from scratch or from a template. Always reachable, even with schedules already set up. */
@Composable
fun CreateScreen(onBack: () -> Unit, onPick: (String) -> Unit) {
    val context = LocalContext.current
    CreateContent(onBack = onBack, onPick = onPick, isInstalled = { AppCatalog.isLaunchable(context, it) })
}

@Composable
internal fun CreateContent(onBack: () -> Unit, onPick: (String) -> Unit, isInstalled: (String) -> Boolean = { false }) {
    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("New", onBack = onBack) }) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            GroupTitle("Start from scratch")
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ScratchCard(Icons.Default.DateRange, "Schedule", "Repeats on days and times", Modifier.weight(1f)) {
                    onPick(Templates.SCRATCH_SCHEDULE)
                }
                ScratchCard(BedtimeIcons.Hourglass, "Block", "Start any time, for a set length", Modifier.weight(1f)) {
                    onPick(Templates.SCRATCH_BLOCK)
                }
            }
            TemplateGallery(onPick = onPick, isInstalled = isInstalled)
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** Both template groups; emits into the caller's Column. */
@Composable
fun TemplateGallery(onPick: (String) -> Unit, isInstalled: (String) -> Boolean) {
    GroupTitle("Schedules", "Switch on by themselves at set times")
    Templates.all.filter { it.kind == ScheduleKind.RECURRING }.forEach { template ->
        TemplateCard(template, isInstalled) { onPick(template.key) }
    }
    GroupTitle("Focus blocks", "Start one whenever you need it")
    Templates.all.filter { it.kind == ScheduleKind.BLOCK }.forEach { template ->
        TemplateCard(template, isInstalled) { onPick(template.key) }
    }
}

fun TemplateIcon.vector(): ImageVector = when (this) {
    TemplateIcon.MOON -> BedtimeIcons.Moon
    TemplateIcon.TARGET -> BedtimeIcons.Target
    TemplateIcon.SUN -> BedtimeIcons.Sun
    TemplateIcon.LEAF -> BedtimeIcons.Leaf
    TemplateIcon.HEADPHONES -> BedtimeIcons.Headphones
    TemplateIcon.HOURGLASS -> BedtimeIcons.Hourglass
    TemplateIcon.BOOK -> BedtimeIcons.Book
}

@Composable
private fun GroupTitle(title: String, subtitle: String? = null) {
    val c = Obsidian.colors
    Column(Modifier.fillMaxWidth().padding(top = 12.dp, start = 4.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = c.textNormal)
        if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
    }
}

@Composable
private fun ScratchCard(icon: ImageVector, title: String, description: String, modifier: Modifier, onClick: () -> Unit) {
    val c = Obsidian.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .fillMaxHeight()
            .clip(shape)
            .background(c.bgSecondary)
            .border(1.dp, c.border, shape)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        IconBadge(icon)
        Spacer(Modifier.height(10.dp))
        Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = c.textNormal)
        Text(description, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
    }
}

@Composable
private fun TemplateCard(template: Template, isInstalled: (String) -> Boolean, onClick: () -> Unit) {
    val context = LocalContext.current
    val c = Obsidian.colors
    val shape = RoundedCornerShape(14.dp)
    val sample = remember(template.key) { template.build(isInstalled) }
    val timing = if (sample.isBlock) {
        formatMinutes(sample.durationMinutes.toLong())
    } else {
        "${formatDays(sample.days)} · ${formatMinuteOfDay(context, sample.startMinute)}–${formatMinuteOfDay(context, sample.endMinute)}"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.bgSecondary)
            .border(1.dp, c.border, shape)
            .clickable(onClickLabel = "Use ${template.name}", onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(template.icon.vector())
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(template.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = c.textNormal)
            Text(timing, style = MaterialTheme.typography.bodySmall, color = c.textNormal)
            Text(template.blurb, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
        }
        Chevron()
    }
}
