package app.bedtime.ui.blocked

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bedtime.ui.components.LineIcons
import app.bedtime.ui.components.ProgressRing
import app.bedtime.ui.components.SoftPillButton
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian

/** A way off a cover: its words, what it does, and whether it can be taken yet. */
internal data class CoverAction(val label: String, val onClick: () -> Unit, val enabled: Boolean = true)

/**
 * The line at the top of a cover: where you are ([label]: "bedtime is on", "social · 45 min a day"),
 * a [detail] at the other end ("until 07:00", "30 of 45 min today"), and under them how far along,
 * when there's a [progress] to show. A [session]'s line is sage; a limit's, [distracting] time, clay rose.
 */
internal data class CoverWhere(
    val label: String,
    val detail: String? = null,
    val progress: Float? = null,
    val session: Boolean = false,
    val distracting: Boolean = false,
)

/**
 * The page behind a cover: a session's has a faint sage light at the top, a limit's or anything
 * else's is plain, so which one is in front is clear at a glance.
 */
@Composable
internal fun Modifier.coverBackground(session: Boolean): Modifier {
    val c = Obsidian.colors
    return if (session) {
        background(Brush.verticalGradient(0f to c.accent.copy(alpha = 0.11f).compositeOver(c.bgPrimary), 0.55f to c.bgPrimary))
    } else {
        background(c.bgPrimary)
    }
}

/**
 * Every full screen that stands between you and something — a blocked app, a locked Settings page,
 * the pause before opening, a day's minutes gone, a nudge, blocking switched off — has one frame:
 * where you are at the top ([where]), an [emblem] (a ring, mostly), a large light title and a line of
 * explanation, and at the foot one main way on, the full width, with the quieter ones under it. When
 * there are words chosen for this cover ([message]), they're what you read first: the title becomes
 * a small line above them. [count] says how often today, beside a small rose dot; [extra] sits under
 * the words (moods, something else to do).
 */
@Composable
internal fun CoverLayout(
    emblem: @Composable () -> Unit,
    title: String,
    body: String?,
    primary: CoverAction,
    modifier: Modifier = Modifier,
    where: CoverWhere? = null,
    count: String? = null,
    secondary: List<CoverAction> = emptyList(),
    /** Words chosen for this cover (yours, or one of the app's), the largest words on it. */
    message: String? = null,
    /** A last line in the text's own colour: the question the cover leaves you with. */
    question: String? = null,
    /** The emblem in the middle of the page (the breathing disc) rather than at the words' edge. */
    emblemCentered: Boolean = false,
    titleSize: TextUnit = 28.sp,
    extra: (@Composable () -> Unit)? = null,
) {
    val c = Obsidian.colors
    Column(
        modifier
            .fillMaxSize()
            .coverBackground(where?.session == true)
            .safeDrawingPadding()
            .padding(horizontal = 28.dp, vertical = 24.dp),
    ) {
        where?.let { CoverWhereLine(it) }
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth(), contentAlignment = if (emblemCentered) Alignment.Center else Alignment.CenterStart) { emblem() }
        Spacer(Modifier.height(if (emblemCentered) 30.dp else 24.dp))
        if (count != null) {
            CoverCount(count)
            Spacer(Modifier.height(12.dp))
        }
        if (message != null) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = c.textMuted)
            Spacer(Modifier.height(14.dp))
            Text("“$message”", fontSize = 28.sp, fontWeight = FontWeight.Light, lineHeight = 36.sp, color = c.textNormal)
            if (body != null) {
                Spacer(Modifier.height(14.dp))
                Text(body, style = MaterialTheme.typography.bodyLarge, color = c.textMuted)
            }
        } else {
            Text(title, fontSize = titleSize, fontWeight = FontWeight.Light, lineHeight = titleSize * 1.25f, color = c.textNormal)
            if (body != null) {
                Spacer(Modifier.height(12.dp))
                Text(body, style = MaterialTheme.typography.bodyLarge, color = c.textMuted)
            }
        }
        if (question != null) {
            Spacer(Modifier.height(10.dp))
            Text(question, style = MaterialTheme.typography.bodyLarge, color = c.textNormal)
        }
        if (extra != null) {
            Spacer(Modifier.height(26.dp))
            extra()
        }
        Spacer(Modifier.weight(1.3f))
        SoftPillButton(primary.label, onClick = primary.onClick, enabled = primary.enabled, modifier = Modifier.fillMaxWidth())
        secondary.forEachIndexed { i, action ->
            QuietButton(action.label, Modifier.padding(top = if (i == 0) 4.dp else 0.dp), enabled = action.enabled, onClick = action.onClick)
        }
    }
}

/** Where you are and how far along, on one thin line at the top of a cover. */
@Composable
internal fun CoverWhereLine(where: CoverWhere) {
    val c = Obsidian.colors
    val fill = when {
        where.session -> c.accentText
        where.distracting -> c.distracting
        else -> c.textMuted
    }
    Column(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(where.label, style = MaterialTheme.typography.bodyMedium, color = if (where.session) c.accentText else c.textMuted, modifier = Modifier.weight(1f))
            where.detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.textMuted) }
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(2.dp).clip(RoundedCornerShape(50)).background(c.interactive)) {
            val progress = where.progress?.coerceIn(0f, 1f) ?: 0f
            if (progress > 0f) Box(Modifier.fillMaxWidth(progress).height(2.dp).clip(RoundedCornerShape(50)).background(fill))
        }
    }
}

/** How often today ("the 4th time today"), in plain words after a small clay-rose dot. */
@Composable
internal fun CoverCount(text: String) {
    val c = Obsidian.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(c.distracting))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
    }
}

/**
 * The covers' emblem: a thin ring around an icon, filled by [progress] — how far a session has come,
 * how much of a limit is gone — in [color].
 */
@Composable
internal fun CoverRing(icon: ImageVector, progress: Float, color: Color) {
    val c = Obsidian.colors
    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
        ProgressRing(progress = progress, modifier = Modifier.size(64.dp), stroke = 2.dp, color = color, track = c.interactive)
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(26.dp))
    }
}

/** A cover's quieter way on: muted words the full width, a comfortable target, faint until it can be taken. */
@Composable
internal fun QuietButton(label: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val c = Obsidian.colors
    Box(
        modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(50)).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (enabled) c.textMuted else c.textFaint)
    }
}

/** A full-width pill that isn't the main way on: the raised grey, the words in cream. */
@Composable
internal fun PlainPillButton(label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Obsidian.colors
    Box(
        modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(50)).background(c.bgSecondary).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, fontSize = 16.sp, color = c.textNormal)
    }
}

/**
 * Something to do instead of the app: one thing, a line in the microjournal. Shown on the covers
 * that send you away, whether or not the microjournal is on elsewhere.
 */
@Composable
internal fun InsteadRow(onWrite: () -> Unit) {
    val c = Obsidian.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Instead", style = MaterialTheme.typography.labelMedium, color = c.textMuted)
        Row(
            Modifier
                .clip(RoundedCornerShape(50))
                .border(1.dp, c.border, RoundedCornerShape(50))
                .clickable(role = Role.Button, onClick = onWrite)
                .heightIn(min = 44.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(LineIcons.Edit, contentDescription = null, tint = c.textNormal, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text("Write a line", style = MaterialTheme.typography.bodyMedium, color = c.textNormal)
        }
    }
}
