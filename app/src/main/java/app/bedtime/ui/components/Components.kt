package app.bedtime.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import app.bedtime.ui.components.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bedtime.ui.theme.Obsidian
import java.time.DayOfWeek
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObsidianTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    val c = Obsidian.colors
    Column {
        TopAppBar(
            title = { Text(title, style = MaterialTheme.typography.titleLarge) },
            navigationIcon = {
                if (onBack != null) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                }
            },
            actions = actions,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = c.bgPrimary,
                titleContentColor = c.textNormal,
                navigationIconContentColor = c.textMuted,
                actionIconContentColor = c.textMuted,
            ),
        )
        HorizontalDivider(color = c.border)
    }
}

/** Rounded card grouping related settings. */
@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = Obsidian.colors
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(c.bgSecondary)
            .border(1.dp, c.border, shape)
            .padding(16.dp),
    ) {
        if (title != null) Text(title, style = MaterialTheme.typography.titleMedium, color = c.textNormal)
        if (subtitle != null) {
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = c.textMuted, modifier = Modifier.padding(top = 2.dp))
        }
        if (title != null || subtitle != null) Spacer(Modifier.height(12.dp))
        content()
    }
}

/** Icon on a soft tinted square. */
@Composable
fun IconBadge(icon: ImageVector, tint: Color = Obsidian.colors.accent, size: Dp = 38.dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.55f))
    }
}

/** Icon + title + description on the left, a control on the right. */
@Composable
fun OptionRow(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val c = Obsidian.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .then(if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier)
            .heightIn(min = 60.dp)
            .padding(vertical = 8.dp)
            .alpha(if (enabled) 1f else 0.55f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = c.textNormal)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = c.textMuted)
            }
        }
        trailing?.invoke()
    }
}

/** Indented row under an [OptionRow] holding a secondary control, e.g. a stepper. */
@Composable
fun SubOptionRow(label: String, content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(start = 52.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Obsidian.colors.textMuted, modifier = Modifier.weight(1f))
        content()
    }
}

@Composable
fun Chevron() {
    Icon(
        Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = Obsidian.colors.textFaint,
        modifier = Modifier.padding(start = 4.dp),
    )
}

/** Big tappable time display. */
@Composable
fun TimeTile(
    label: String,
    time: String,
    modifier: Modifier = Modifier,
    caption: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val c = Obsidian.colors
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier
            .clip(shape)
            .background(c.bgPrimaryAlt)
            .border(1.dp, c.border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, color = c.textMuted)
        Text(
            styledTime(time, suffixSize = 16.sp),
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            color = c.textNormal,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
        )
        Text(caption ?: " ", style = MaterialTheme.typography.labelMedium, color = c.accentText)
    }
}

/**
 * Shrinks a trailing AM/PM marker ("10:30 PM") so 12-hour times stay on one line, the way phone
 * clocks show them. 24-hour times ("22:30") pass through unchanged.
 */
fun styledTime(time: String, suffixSize: TextUnit): AnnotatedString {
    val split = time.lastIndexOf(' ')
    if (split <= 0 || time.substring(split).any { it.isDigit() }) return AnnotatedString(time)
    return buildAnnotatedString {
        append(time.substring(0, split))
        withStyle(SpanStyle(fontSize = suffixSize)) { append(time.substring(split)) }
    }
}

@Composable
fun QuickChip(text: String, selected: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val c = Obsidian.colors
    val shape = RoundedCornerShape(50)
    Text(
        text,
        modifier = Modifier
            .clip(shape)
            .background(if (selected) c.accent.copy(alpha = 0.16f) else Color.Transparent)
            .border(1.dp, if (selected) c.accent.copy(alpha = 0.5f) else c.border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) c.accentText else c.textMuted,
    )
}

/** Progress through a multi-step flow: done steps get a check, the current one is filled. */
@Composable
fun StepChips(labels: List<String>, current: Int) {
    val c = Obsidian.colors
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        labels.forEachIndexed { index, label ->
            val done = index < current
            val active = index == current
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(
                        when {
                            active -> c.accentFill
                            done -> c.green.copy(alpha = 0.15f)
                            else -> c.interactive
                        },
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (done) {
                    Icon(Icons.Default.Check, contentDescription = "Done", tint = c.green, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    color = when {
                        active -> c.textOnAccent
                        done -> c.green
                        else -> c.textMuted
                    },
                )
            }
        }
    }
}

/** Numbered circle that turns into a green check when done. */
@Composable
fun StatusDot(done: Boolean, number: Int? = null) {
    val c = Obsidian.colors
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (done) c.accentFill else c.interactive)
            .border(1.5.dp, if (done) c.accentFill else c.textFaint.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Icon(Icons.Default.Check, contentDescription = "Done", tint = c.textOnAccent, modifier = Modifier.size(16.dp))
        } else if (number != null) {
            Text("$number", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = c.textMuted)
        }
    }
}

@Composable
fun StatusPill(text: String, color: Color) {
    Text(
        text,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium,
        color = color,
    )
}

/** Bottom bar for the screen's main action; follows the keyboard and gesture bar. */
@Composable
fun BottomActionBar(content: @Composable () -> Unit) {
    val c = Obsidian.colors
    Column(Modifier.fillMaxWidth().background(c.bgPrimary)) {
        HorizontalDivider(color = c.border)
        Box(
            Modifier
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) { content() }
    }
}

@Composable
fun ObsidianToggle(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
    val c = Obsidian.colors
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = c.textOnAccent,
            checkedTrackColor = c.accent,
            checkedBorderColor = c.accent,
            uncheckedThumbColor = c.textOnAccent,
            uncheckedTrackColor = c.interactiveHover,
            uncheckedBorderColor = Color.Transparent,
            disabledCheckedTrackColor = c.accent.copy(alpha = 0.4f),
            disabledCheckedThumbColor = c.textOnAccent,
            disabledUncheckedTrackColor = c.interactive,
            disabledUncheckedThumbColor = c.textFaint,
            disabledUncheckedBorderColor = Color.Transparent,
        ),
    )
}

/** Obsidian's `mod-cta` button. */
@Composable
fun CtaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    compact: Boolean = false,
) {
    val c = Obsidian.colors
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = if (compact) 40.dp else 50.dp)
            .topHighlight(RoundedCornerShape(12.dp), if (c.isDark) 0.12f else 0.25f),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = c.accentFill,
            contentColor = c.textOnAccent,
            disabledContainerColor = c.interactive,
            disabledContentColor = c.textFaint,
        ),
        contentPadding = if (compact) PaddingValues(horizontal = 16.dp, vertical = 6.dp) else PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            text,
            fontWeight = FontWeight.SemiBold,
            style = if (compact) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
fun PlainButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    destructive: Boolean = false,
) {
    val c = Obsidian.colors
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp).topHighlight(RoundedCornerShape(12.dp), if (c.isDark) 0.06f else 0.7f),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = c.interactive,
            contentColor = if (destructive) c.red else c.textNormal,
            disabledContainerColor = c.interactive,
            disabledContentColor = c.textFaint,
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    ) { Text(text, fontWeight = FontWeight.Medium) }
}

/** Primary's tactile buttons: a faint light line along the top inner edge. */
private fun Modifier.topHighlight(shape: Shape, alpha: Float): Modifier = clip(shape).drawWithContent {
    drawContent()
    drawRect(Color.White.copy(alpha = alpha), size = Size(size.width, 1.dp.toPx()))
}

enum class CalloutKind { NOTE, INFO, WARNING, SUCCESS }

/** Obsidian-style callout: tinted block with an icon and coloured title. */
@Composable
fun Callout(
    title: String,
    modifier: Modifier = Modifier,
    kind: CalloutKind = CalloutKind.NOTE,
    body: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val c = Obsidian.colors
    val color = when (kind) {
        CalloutKind.NOTE -> c.accent
        CalloutKind.INFO -> c.blue
        CalloutKind.WARNING -> c.orange
        CalloutKind.SUCCESS -> c.green
    }
    val icon = when (kind) {
        CalloutKind.WARNING -> Icons.Default.Warning
        CalloutKind.SUCCESS -> Icons.Default.CheckCircle
        else -> Icons.Default.Info
    }
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.1f))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, color = color, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
        }
        if (body != null) {
            Text(body, color = c.textNormal, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 6.dp))
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction, contentPadding = PaddingValues(0.dp)) {
                Text(actionLabel, color = color, fontWeight = FontWeight.Medium)
            }
        }
    }
}

/** Obsidian tag pill. */
@Composable
fun Tag(text: String, modifier: Modifier = Modifier) {
    val c = Obsidian.colors
    Text(
        text,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(c.accent.copy(alpha = 0.15f))
            .drawBehind {
                // Primary's tags sit on a slight inner shadow along the bottom edge.
                val h = 1.5.dp.toPx()
                drawRect(Color.Black.copy(alpha = if (c.isDark) 0.2f else 0.08f), Offset(0f, size.height - h), Size(size.width, h))
            }
            .padding(horizontal = 9.dp, vertical = 3.dp),
        style = MaterialTheme.typography.labelMedium,
        color = c.accentText,
    )
}

@Composable
fun ObsidianTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    password: Boolean = false,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    isError: Boolean = false,
    supportingText: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    textStyle: TextStyle = LocalTextStyle.current,
) {
    val c = Obsidian.colors
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        textStyle = textStyle,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it, color = c.textFaint) } },
        leadingIcon = leadingIcon,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        visualTransformation = if (password) PasswordVisualTransformation() else LowercaseTransformation,
        keyboardOptions = if (password) KeyboardOptions(keyboardType = KeyboardType.Password) else keyboardOptions,
        singleLine = singleLine,
        maxLines = maxLines,
        shape = RoundedCornerShape(10.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.accent,
            unfocusedBorderColor = c.border,
            disabledBorderColor = c.border,
            focusedContainerColor = c.bgPrimaryAlt,
            unfocusedContainerColor = c.bgPrimaryAlt,
            disabledContainerColor = c.bgPrimaryAlt,
            cursorColor = c.accent,
            focusedLabelColor = c.accentText,
            unfocusedLabelColor = c.textMuted,
            focusedTextColor = c.textNormal,
            unfocusedTextColor = c.textNormal,
            disabledTextColor = c.textMuted,
            disabledLabelColor = c.textFaint,
        ),
    )
}

@Composable
fun SegmentedChoice(options: List<String>, selected: Int, onSelect: (Int) -> Unit, enabled: Boolean = true) {
    val c = Obsidian.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(c.bgPrimaryAlt)
            .border(1.dp, c.border, RoundedCornerShape(12.dp))
            .padding(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val on = index == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (on) (if (c.isDark) c.interactiveHover else c.bgSecondary) else Color.Transparent)
                    .clickable(enabled = enabled) { onSelect(index) }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (on) c.textNormal else c.textMuted,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
fun DaySelector(days: Set<Int>, onChange: (Set<Int>) -> Unit, enabled: Boolean = true) {
    val c = Obsidian.colors
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        for (day in 1..7) {
            val on = day in days
            val name = DayOfWeek.of(day).getDisplayName(JavaTextStyle.SHORT, Locale.getDefault())
            Box(
                Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                    .clip(CircleShape)
                    .background(if (on) c.accentFill else c.interactive)
                    .clickable(enabled = enabled, onClickLabel = name) { onChange(if (on) days - day else days + day) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    name.take(1),
                    color = if (on) c.textOnAccent else c.textMuted,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun CodeBlock(code: String, onCopy: () -> Unit) {
    val c = Obsidian.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(c.bgPrimaryAlt)
            .border(1.dp, c.border, RoundedCornerShape(10.dp))
            .padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Commands are case-sensitive, so they're shown exactly as written.
        VerbatimText(
            code,
            modifier = Modifier.weight(1f).padding(vertical = 8.dp),
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            lineHeight = 18.sp,
            color = c.textNormal,
        )
        TextButton(onClick = onCopy) { Text("Copy", color = c.accentText, fontWeight = FontWeight.Medium) }
    }
}
