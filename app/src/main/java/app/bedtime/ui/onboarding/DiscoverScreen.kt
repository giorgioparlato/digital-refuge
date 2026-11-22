package app.bedtime.ui.onboarding

import app.bedtime.ui.components.LineIcons
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.launch
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.Routes
import app.bedtime.data.Repository
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian

/** The groups features are listed in, in this order. */
private enum class Group(val title: String) { CORE("core features"), PRODUCTIVITY("productivity"), OTHER("other") }

/**
 * A feature with a page of its own: its name, its short name (for previous and next), its line on the
 * list, its group and its icon.
 */
private enum class Feature(val title: String, val short: String, val line: String, val group: Group, val icon: ImageVector) {
    SESSION("when a session runs", "session", "see what blocked apps show, and how to leave early", Group.CORE, BedtimeIcons.Block),
    HOME_SCREEN("home screen", "home screen", "use refuge as your launcher", Group.CORE, LineIcons.Home),
    LOCK_SCREEN("the lock screen", "lock screen", "use refuge's lockscreen, with the widgets you choose", Group.CORE, LineIcons.Lock),
    WIDGETS("some widgets", "widgets", "see what each widget does when you tap it", Group.CORE, BedtimeIcons.Grid),
    INSIGHTS("insights", "insights", "see your screen time against your budget", Group.CORE, LineIcons.Insights),
    SEARCH("search", "search", "search anything, and do things, as you type", Group.PRODUCTIVITY, LineIcons.Search),
    NOTES("notes", "notes", "notes in markdown, synced with obsidian if you like", Group.PRODUCTIVITY, LineIcons.Edit),
    QUICK_NOTES("quick notes", "quick notes", "jot a note with a double tap, even locked", Group.PRODUCTIVITY, LineIcons.Edit),
    VOICE("voice notes", "voice notes", "speak your notes instead of typing", Group.PRODUCTIVITY, BedtimeIcons.Mic),
    OBSIDIAN("obsidian", "obsidian", "find, add to and save notes in your vault", Group.PRODUCTIVITY, LineIcons.Share),
    HABITS("habits", "habits", "a simple habit tracker", Group.OTHER, LineIcons.CheckCircle),
    MICROJOURNAL("the microjournal", "microjournal", "a journal, one line at a time", Group.OTHER, LineIcons.DateRange),
    DICTIONARY("the dictionary", "dictionary", "look up a word without unlocking", Group.OTHER, BedtimeIcons.Book),
}

/**
 * What refuge does beyond keeping apps shut: a list by group, and a page for each feature with a short
 * looping demo drawn here, a line or two about it, and a way to set it up. The pages swipe from one to
 * the next, with previous and next at the foot too. [onOpen] pushes a page; [onTab] goes to one of the
 * tabs (focus, insights).
 */
@Composable
fun DiscoverScreen(onBack: () -> Unit, onOpen: (String) -> Unit, onTab: (String) -> Unit, startAt: String? = null) {
    var open by rememberSaveable { mutableStateOf(startAt) }
    val feature = open?.let { name -> Feature.entries.firstOrNull { it.name == name } }
    BackHandler(enabled = feature != null) { open = null }
    if (feature == null) {
        FeatureList(onBack, onPick = { open = it.name })
        return
    }
    FeaturePages(feature, onBack = { open = null }, onPage = { open = it.name }, onOpen = onOpen, onTab = onTab)
}

@Composable
private fun FeatureList(onBack: () -> Unit, onPick: (Feature) -> Unit) {
    val c = Obsidian.colors
    Scaffold(containerColor = c.bgPrimary, topBar = { ObsidianTopBar("Discover features", onBack = onBack) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("see what refuge does beyond keeping apps shut. each opens to a short look at how it works.", fontSize = 14.sp, color = c.textMuted)
            Group.entries.forEach { group ->
                Text(group.title, fontSize = 13.sp, color = c.accentText, modifier = Modifier.padding(top = 10.dp, start = 2.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(c.bgSecondary).padding(horizontal = 14.dp, vertical = 2.dp)) {
                    Feature.entries.filter { it.group == group }.forEach { f ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 54.dp).clickable(onClickLabel = "how it works") { onPick(f) }.padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(30.dp).clip(RoundedCornerShape(9.dp)).background(c.interactive), contentAlignment = Alignment.Center) {
                                Icon(f.icon, contentDescription = null, tint = c.accentText, modifier = Modifier.size(16.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(f.title, fontSize = 15.sp, color = c.textNormal)
                                Text(f.line, fontSize = 12.sp, color = c.textFaint)
                            }
                            Text("›", fontSize = 18.sp, color = c.textFaint)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** The feature pages, side by side: swipe between them, or use previous and next at the foot. */
@Composable
private fun FeaturePages(start: Feature, onBack: () -> Unit, onPage: (Feature) -> Unit, onOpen: (String) -> Unit, onTab: (String) -> Unit) {
    val c = Obsidian.colors
    val pager = rememberPagerState(initialPage = start.ordinal) { Feature.entries.size }
    val scope = rememberCoroutineScope()
    // The page in view is the one open, so going back to the list and in again keeps your place.
    LaunchedEffect(pager) { snapshotFlow { pager.currentPage }.collect { onPage(Feature.entries[it]) } }
    val current = Feature.entries[pager.currentPage]
    val previous = Feature.entries.getOrNull(current.ordinal - 1)
    val next = Feature.entries.getOrNull(current.ordinal + 1)
    fun go(f: Feature) = scope.launch { pager.animateScrollToPage(f.ordinal) }
    Scaffold(
        containerColor = c.bgPrimary,
        topBar = { ObsidianTopBar(current.title, onBack = onBack, subtitle = "discover features") },
        bottomBar = {
            Row(
                Modifier.fillMaxWidth().background(c.bgPrimary).navigationBarsPadding().padding(horizontal = PageGutter, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PageButton(previous?.let { "‹ ${it.short}" } ?: "‹ the list", filled = false, modifier = Modifier.weight(1f)) { if (previous != null) go(previous) else onBack() }
                Text("${current.ordinal + 1} of ${Feature.entries.size}", fontSize = 11.sp, color = c.textFaint)
                PageButton(next?.let { "${it.short} ›" } ?: "the list ›", filled = false, modifier = Modifier.weight(1f)) { if (next != null) go(next) else onBack() }
            }
        },
    ) { padding ->
        HorizontalPager(pager, Modifier.fillMaxSize().padding(padding), verticalAlignment = Alignment.Top) { page ->
            FeaturePage(Feature.entries[page], onOpen, onTab)
        }
    }
}

/** One feature's page: its demo and lines, then the way to set it up. */
@Composable
private fun FeaturePage(f: Feature, onOpen: (String) -> Unit, onTab: (String) -> Unit) {
    val context = LocalContext.current
    val settings by remember { Repository.get(context) }.settings.collectAsStateWithLifecycle(initialValue = null)
    // Where it's set up: what the card says, and where it goes.
    val (action, place, go) = when (f) {
        Feature.SESSION -> Triple("set it up", "in the focus tab", { onTab(Routes.HOME) })
        Feature.HOME_SCREEN -> Triple("set it up", "in home screen settings", { onOpen(Routes.SETTINGS_HOME) })
        Feature.LOCK_SCREEN -> Triple("set it up", "in lock screen settings", { onOpen(Routes.SETTINGS_LOCK) })
        Feature.WIDGETS -> Triple("set it up", "in widget settings", { onOpen(Routes.STYLE_SHOWN) })
        Feature.INSIGHTS -> Triple("open it", "in insights", { onOpen(Routes.STATS) })
        Feature.SEARCH, Feature.DICTIONARY -> Triple("set it up", "in search settings", { onOpen(Routes.SEARCH) })
        Feature.NOTES, Feature.QUICK_NOTES, Feature.VOICE, Feature.OBSIDIAN -> Triple("set it up", "in notes settings", { onOpen(Routes.QUICK_NOTES) })
        Feature.HABITS -> Triple("set it up", "in habits settings", { onOpen(Routes.HABITS) })
        Feature.MICROJOURNAL -> Triple("set it up", "in microjournal settings", { onOpen(Routes.SETTINGS_JOURNAL) })
    }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = PageGutter, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
            when (f) {
                Feature.SESSION -> SessionPage()
                Feature.HOME_SCREEN -> HomeScreenPage(showLauncher = settings?.homeScreen == false) { onOpen(Routes.LAUNCHER) }
                Feature.LOCK_SCREEN -> LockScreenPage()
                Feature.WIDGETS -> WidgetsPage()
                Feature.INSIGHTS -> InsightsPage()
                Feature.SEARCH -> SearchPage()
                Feature.NOTES -> NotesPage()
                Feature.QUICK_NOTES -> QuickNotesPage()
                Feature.VOICE -> VoicePage()
                Feature.OBSIDIAN -> ObsidianPage()
                Feature.HABITS -> HabitsPage()
                Feature.MICROJOURNAL -> MicrojournalPage()
                Feature.DICTIONARY -> DictionaryPage()
            }
            SetUpCard(action, place, go)
            Spacer(Modifier.height(12.dp))
    }
}

/** The way from a page to the feature itself: what to do, and where. */
@Composable
private fun SetUpCard(action: String, place: String, onClick: () -> Unit) {
    val c = Obsidian.colors
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier.fillMaxWidth().clip(shape).background(c.accent.copy(alpha = 0.08f)).border(1.dp, c.accentFill, shape)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(LineIcons.Settings, contentDescription = null, tint = c.accentText, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(action, fontSize = 14.sp, color = c.textNormal)
            Text(place, fontSize = 12.sp, color = c.textMuted)
        }
        Text("›", fontSize = 18.sp, color = c.accentText)
    }
}

// ---------------------------------------------------------------- the pages

@Composable
internal fun SessionPage() {
    val c = Obsidian.colors
    Intro("while a block, a schedule or a limit runs, the apps it shuts open to a cover instead.")
    val t = loop(14_000)
    Demo {
        Phone {
            when {
                t < 0.12f -> Screen {
                    Small("bedtime · until 07:00")
                    Text("22:41", fontSize = 34.sp, color = c.textNormal)
                    Spacer(Modifier.height(22.dp))
                    AppNames(listOf("phone", "messages", "instagram", "maps"), tapped = "instagram", t = t, at = 0.07f)
                }
                t < 0.50f -> Screen(center = true) {
                    Small("bedtime · until 07:00")
                    Spacer(Modifier.weight(1f))
                    val waited = ramp(t, 0.17f, 0.44f)
                    Box(Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                        Ring(waited, Modifier.fillMaxSize())
                        val left = kotlin.math.ceil(60 * (1f - waited)).toInt()
                        Text("0:%02d".format(left).let { if (left == 60) "1:00" else it }, fontSize = 20.sp, color = c.textNormal)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("wait a minute to open instagram", fontSize = 13.sp, color = c.textMuted, textAlign = TextAlign.Center)
                    Spacer(Modifier.weight(1f))
                    MiniButton("continue", filled = true, alpha = if (t >= 0.44f) 1f else 0.35f, t = t, tapAt = 0.47f)
                }
                t < 0.86f -> Screen {
                    Small("bedtime · until 07:00")
                    Spacer(Modifier.height(18.dp))
                    Text("why are you opening instagram?", fontSize = 15.sp, color = c.textNormal)
                    Spacer(Modifier.height(4.dp))
                    Typed("checking anna's message", ramp(t, 0.54f, 0.68f), caret = t < 0.70f, size = 13)
                    Spacer(Modifier.height(14.dp))
                    Text("how do you feel?", fontSize = 12.sp, color = c.textMuted)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("tired", "bored", "calm").forEach { Chip(it, on = it == "tired" && t >= 0.74f, small = true) }
                    }
                    Spacer(Modifier.weight(1f))
                    MiniButton("open instagram", filled = true, t = t, tapAt = 0.82f)
                }
                else -> FakeApp("instagram")
            }
        }
        Caption(
            t,
            0f to "open a blocked app, and refuge asks you to pause first.",
            0.12f to "here, you wait a minute.",
            0.50f to "then you write why you're opening it, and how you feel.",
            0.86f to "then the app opens, for as long as you chose.",
        )
    }
    InfoCard("these are two options of several", "you set what opening a blocked app asks for: a wait, a passage to copy, a few words of why, a password, or nothing.")
}

@Composable
private fun HomeScreenPage(showLauncher: Boolean, onLauncher: () -> Unit) {
    val c = Obsidian.colors
    Intro("use refuge as your launcher: a calm home screen with only what you put on it.")
    val t = loop(15_000)
    Demo {
        Phone {
            if (t < 0.6f) {
                Screen {
                    Text("14:20", fontSize = 40.sp, color = c.textNormal)
                    Text("thursday 19 · 9° light rain", fontSize = 11.sp, color = c.textMuted)
                    Small("16:00 · team call")
                    Spacer(Modifier.height(28.dp))
                    AppNames(listOf("phone", "messages", "maps", "camera", "notes"))
                    Spacer(Modifier.weight(1f))
                    Small("swipe up for every app", Modifier.align(Alignment.CenterHorizontally))
                }
                Finger(Modifier.align(Alignment.BottomCenter).offset(y = (-40).dp - (110 * ramp(t, 0.26f, 0.33f)).dp), visible = t in 0.24f..0.35f)
                Sheet(slide(t, 0.31f, 0.36f, 0.6f, 0.6f), full = true) {
                    Field("search apps, or anything", typed = false)
                    listOf("b" to listOf("bank", "books"), "c" to listOf("calendar", "camera", "clock"), "m" to listOf("maps", "messages", "music"), "n" to listOf("news", "notes")).forEach { (letter, apps) ->
                        Text(letter, fontSize = 10.sp, color = c.accentText)
                        apps.forEach { Text(it, fontSize = 14.sp, color = c.textNormal) }
                    }
                }
            } else {
                Screen {
                    Small("bedtime · until 07:00")
                    Text("22:41", fontSize = 40.sp, color = c.textMuted)
                    Text("thursday 19 november", fontSize = 11.sp, color = c.textFaint)
                    Spacer(Modifier.height(28.dp))
                    listOf("phone", "maps").forEach { Text(it, fontSize = 16.sp, color = c.textMuted) }
                }
            }
        }
        Caption(t, 0f to "see the time, a few widgets you chose, and your apps as words.", 0.3f to "swipe up for every app, or type to find anything.", 0.6f to "in a session, it turns quiet: only the apps you keep.")
    }
    if (showLauncher) Text("use refuge as my home screen ›", fontSize = 14.sp, color = c.accentText, modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onLauncher).padding(vertical = 6.dp, horizontal = 2.dp))
}

@Composable
internal fun LockScreenPage() {
    val c = Obsidian.colors
    Intro("you can use refuge's lockscreen, only with what you choose, and the option of cool widgets")
    val t = loop(8_000)
    Demo {
        Phone {
            Screen(center = true) {
                Small("bedtime · until 07:00")
                Text("22:41", fontSize = 48.sp, color = c.textNormal)
                Text("thursday 19 november · 6° clear", fontSize = 11.sp, color = c.textMuted)
                Small("tomorrow 09:30 · dentist")
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(true, true, false).forEach { Box(Modifier.size(6.dp).clip(CircleShape).background(if (it) c.accent else c.interactiveHover)) }
                }
                Spacer(Modifier.height(14.dp))
                Box(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Small("1 held until morning")
                        val away = ramp(t, 0.35f, 0.48f)
                        if (away < 1f) Notice("messages · anna", "are we still on for saturday?", Modifier.graphicsLayer { translationX = away * 160.dp.toPx(); alpha = 1f - away })
                    }
                    if (t in 0.5f..0.92f) Text("later: back when bedtime ends", fontSize = 10.sp, color = c.accentText, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 22.dp))
                }
                Spacer(Modifier.weight(1f))
                val lift = if (t in 0.6f..0.9f) kotlin.math.sin((t - 0.6f) / 0.3f * Math.PI).toFloat() else 0f
                Small("⌃ swipe up, or tap, to unlock", Modifier.graphicsLayer { translationY = -lift * 10.dp.toPx() })
            }
        }
    }
    Tiles(
        "widgets of its own" to "show the weather, the next event, habits or a note",
        "curated notifications" to "choose which notifications you see, from whom and from which apps",
        "gestures" to "double tap for a quick note; swipe for the camera or the torch",
        "default lockscreen, or only in sessions" to "choose which in settings",
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun WidgetsPage() {
    val c = Obsidian.colors
    Intro("on the home screen or lock screen, each does a little more than it shows.")
    val t = loop(20_000)
    // Which widget is in focus: habits, the budget, the calendar, then the weather; the rest fade back.
    val step = (t * 4).toInt().coerceAtMost(3)
    fun focus(i: Int) = Modifier.graphicsLayer { alpha = if (step == i) 1f else 0.28f; val s = if (step == i) 1.04f else 1f; scaleX = s; scaleY = s }
    Demo {
        Phone(height = 400.dp) {
            Screen {
                // The clock kept small, top left, so the widgets have the room.
                Row(verticalAlignment = Alignment.Bottom) {
                    Text("22:41", fontSize = 13.sp, color = c.textNormal)
                    Small("  thu 19 nov · bedtime until 07:00", Modifier.padding(bottom = 1.dp))
                }
                Spacer(Modifier.height(6.dp))
                Box(focus(3)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf(
                            Triple("thu", app.bedtime.data.WeatherIcon.CLOUDY, "9° 4°"),
                            Triple("fri", app.bedtime.data.WeatherIcon.RAIN, "8° 3°"),
                            Triple("sat", app.bedtime.data.WeatherIcon.CLEAR, "11° 2°"),
                            Triple("sun", app.bedtime.data.WeatherIcon.PARTLY, "12° 4°"),
                            Triple("mon", app.bedtime.data.WeatherIcon.RAIN, "7° 3°"),
                        ).forEach { (day, sky, temps) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Small(day)
                                Icon(app.bedtime.ui.components.WeatherIcons.of(sky), contentDescription = null, tint = c.textMuted, modifier = Modifier.size(13.dp))
                                Text(temps, fontSize = 9.sp, color = c.textNormal)
                            }
                        }
                    }
                    TapRing(t, 0.78f, Modifier.offset(x = 4.dp))
                }
                Box(focus(2)) {
                    Text("16:00 · team call", fontSize = 11.sp, color = c.textNormal, modifier = Modifier.padding(vertical = 2.dp))
                    TapRing(t, 0.53f, Modifier.offset(x = 50.dp, y = (-6).dp))
                }
                Column(focus(0), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HabitDots("read", listOf(true, true, false, true, true, false), done = t >= 0.08f && t < 0.99f, t = t, at = 0.06f, compact = true)
                    HabitDots("stretch", listOf(false, true, true, true, false, true), done = t >= 0.16f && t < 0.99f, t = t, at = 0.14f, compact = true)
                }
                Box(focus(1)) {
                    val over = t >= 0.34f
                    Column(Modifier.padding(top = 2.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Row(Modifier.fillMaxWidth()) {
                            Text(if (over) "50 of 45 min on the phone" else "40 of 45 min on the phone", fontSize = 10.sp, color = c.textMuted, modifier = Modifier.weight(1f))
                            Text(if (over) "5m over" else "5 min left", fontSize = 9.sp, color = if (over) c.orange else c.textFaint)
                        }
                        BudgetBar(over)
                    }
                    TapRing(t, 0.42f, Modifier.offset(x = 100.dp))
                }
                // The foot of a lock screen: a padlock, as if it's still locked.
                Spacer(Modifier.weight(1f))
                Icon(LineIcons.Lock, contentDescription = null, tint = c.textFaint, modifier = Modifier.size(16.dp).align(Alignment.CenterHorizontally))
            }
            Sheet(slide(t, 0.42f, 0.44f, 0.48f, 0.50f)) {
                Row(Modifier.fillMaxWidth()) { Small("today · 50 min", Modifier.weight(1f)); Small("most first") }
                listOf(Triple("instagram", 22, true), Triple("youtube", 14, true), Triple("messages", 8, false), Triple("maps", 6, false)).forEach { (name, m, distracting) ->
                    Row(Modifier.fillMaxWidth()) { Text(name, fontSize = 12.sp, color = c.textNormal, modifier = Modifier.weight(1f)); Small("$m min") }
                    Bar(m / 22f, if (distracting) c.distracting else c.textMuted)
                }
            }
            Sheet(slide(t, 0.54f, 0.57f, 0.71f, 0.74f)) {
                Small("today")
                EventLine("09:30", "dentist", past = true)
                EventLine("16:00", "team call", now = true)
                EventLine("19:30", "dinner with anna")
                Small("tomorrow")
                EventLine("10:00", "yoga")
            }
            Sheet(slide(t, 0.79f, 0.82f, 0.96f, 0.99f)) {
                Text("rain today", fontSize = 12.sp, color = c.textNormal)
                listOf("23:00" to 70, "00:00" to 80, "01:00" to 55, "02:00" to 30, "03:00" to 15).forEach { (hour, chance) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Small(hour, Modifier.width(36.dp))
                        Box(Modifier.weight(1f)) { Bar(chance / 100f, c.accent.copy(alpha = 0.35f + 0.65f * chance / 100f), height = 8.dp) }
                        Small("$chance%", Modifier.width(32.dp).padding(start = 6.dp))
                    }
                }
                Text("open your weather app ›", fontSize = 11.sp, color = c.accentText)
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("habits", "time on phone", "calendar", "weather").forEachIndexed { i, label -> Chip(label, on = i == step) }
        }
        Caption(
            t,
            0f to "see habits as dots. tap today's to tick it, without unlocking.",
            0.25f to "track time against your budget, here 45 min. past it, the bar shows by how much; tap for the apps that took most.",
            0.5f to "see the next event. tap for the whole list; hold to hide one until it's over.",
            0.75f to "see the next five days. tap for today's rain, hour by hour, and your weather app.",
        )
    }
}

@Composable
private fun InsightsPage() {
    val c = Obsidian.colors
    Intro("see your phone usage, overall and in distracting apps in relation to the budget you set for yourself")
    Demo {
        val scale = listOf(0xFF5FA87E, 0xFF8DBB8E, 0xFFBFD3A6, 0xFFF2E6C9, 0xFFEDB49A, 0xFFE2756F, 0xFFD7263D).map { Color(it) }
        val levels = listOf(1, 2, 3, 2, 4, 5, 3, 2, 1, 3, 3, 4, 6, 2, 1, 2, 2, 3, 5, 4, 3, 2, 1, 1, 3, 6, 5, 2)
        val cells: List<Int?> = listOf(null, null) + levels
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            cells.chunked(7).forEachIndexed { row, week ->
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    (0 until 7).forEach { i ->
                        val level = week.getOrNull(i)
                        val day = row * 7 + i - 1
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(7.dp))
                                .then(if (level != null) Modifier.background(scale[level]) else Modifier)
                                .then(if (day == levels.size) Modifier.border(2.dp, c.accent, RoundedCornerShape(7.dp)) else Modifier),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (level != null) Text("$day", fontSize = 11.sp, color = if (level in 2..4) Color(0xFF2A2A2A) else Color.White)
                        }
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth()) { Small("0", Modifier.weight(1f)); Small("1h budget", Modifier.weight(1f)); Small("3h 10m") }
        Spacer(Modifier.height(8.dp))
        Small("screen time over months", Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.Bottom) {
            listOf(Triple("jun", 62, 0.35f), Triple("jul", 70, 0.42f), Triple("aug", 58, 0.38f), Triple("sep", 54, 0.3f), Triple("oct", 46, 0.26f), Triple("nov", 40, 0.22f)).forEach { (name, h, part) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.fillMaxWidth().height(h.dp).clip(RoundedCornerShape(3.dp)).background(c.interactiveHover), contentAlignment = Alignment.BottomCenter) {
                        Box(Modifier.fillMaxWidth().height((h * part).dp).background(c.distracting))
                    }
                    Small(name, Modifier.padding(top = 4.dp))
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Swatch(c.interactiveHover, "on the phone")
            Swatch(c.distracting, "in distracting apps")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchPage() {
    val c = Obsidian.colors
    Intro("swipe up to search anything, add stuff to your calendar, or do a bunch more things")
    val t = loop(16_000)
    val scene = (t * 4).toInt().coerceAtMost(3)
    Demo {
        Phone(height = 330.dp) {
            Screen {
                val start = scene * 0.25f
                when (scene) {
                    0 -> {
                        Field("dinner fri 8pm", progress = ramp(t, 0.01f, 0.09f))
                        if (t >= 0.11f) {
                            Result("add to calendar", "dinner · friday 20:00", picked = t >= 0.18f)
                            Small("dinner ideas · notes", Modifier.padding(start = 10.dp))
                        }
                    }
                    1 -> {
                        Field("m coffee", progress = ramp(t, start + 0.01f, start + 0.06f))
                        if (t >= start + 0.09f) {
                            Result("coffee, on maps", "it opens your maps app, searching", picked = t >= start + 0.17f)
                            Small("m is maps; you choose the words", Modifier.padding(start = 10.dp))
                        }
                    }
                    2 -> {
                        Field("@anna", progress = ramp(t, start + 0.01f, start + 0.04f))
                        if (t >= start + 0.07f) {
                            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(c.interactive).padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("anna", fontSize = 12.sp, color = c.textNormal)
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("call", "message", "signal").forEachIndexed { i, label ->
                                        val on = i == 0 && t >= start + 0.16f
                                        Text(label, fontSize = 10.sp, color = if (on) c.textOnAccent else c.textMuted, textAlign = TextAlign.Center, modifier = Modifier.weight(1f).clip(RoundedCornerShape(50)).background(if (on) c.accentFill else Color.Transparent).border(1.dp, if (on) c.accentFill else c.border, RoundedCornerShape(50)).padding(vertical = 6.dp))
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        Field("why do cats purr", progress = ramp(t, start + 0.01f, start + 0.11f))
                        if (t >= start + 0.13f) {
                            Result("search the web", "it opens your browser, searching", picked = t >= start + 0.18f)
                            Small("wikipedia · why do cats purr", Modifier.padding(start = 10.dp))
                        }
                    }
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            listOf("add to calendar", "find on maps", "reach a contact", "search online").forEachIndexed { i, label -> Chip(label, on = i == scene) }
        }
        Small("four examples of many")
    }
    Card {
        Text("and much more", fontSize = 13.sp, color = c.textNormal)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("start a timer", "set an alarm", "find files", "do a sum", "open a setting", "search in spotify", "or other apps").forEach { Chip(it, small = true) }
        }
        Text("you can set up keywords for specific actions (e.g. m for maps search). If you write a date or time the option of adding a calendar event shows up automatically", fontSize = 11.sp, color = c.textFaint, lineHeight = 16.sp)
    }
}

@Composable
private fun NotesPage() {
    val c = Obsidian.colors
    Intro("notes written in markdown. can be synced with obsidian")
    val t = loop(10_000)
    Demo(padding = 12.dp) {
        if (t < 0.33f || t >= 0.67f) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoteCard(tag = "pinned", title = "to-do", body = "☐ return the book\n☐ book the dentist\n☐ buy bread", tagColor = c.accentText)
                    NoteCard(tag = "#films", title = "films to see", body = "perfect days, past lives, aftersun", tint = Color(0xFFE0B243))
                    NoteCard(tag = "⏰ saturday 10:00", title = "plant the bulbs", tint = Color(0xFF4DB2D1))
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NoteCard(tag = "#books", title = "books to read", body = "braiding sweetgrass", tint = Color(0xFFEF8839))
                    NoteCard(title = "words", body = "sonder — the sense that every passer-by has a life as full as yours")
                    NoteCard(title = "trip to lisbon", body = "tram 28 early, the castle, pastéis in belém")
                }
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    Triple("to-do", "pinned", c.interactiveHover), Triple("books to read", "22:41", Color(0xFFEF8839)), Triple("films to see", "yesterday", Color(0xFFE0B243)),
                    Triple("words", "monday", c.interactiveHover), Triple("plant the bulbs", "12 nov", Color(0xFF4DB2D1)), Triple("trip to lisbon", "3 nov", c.interactiveHover),
                ).forEach { (name, whenText, dot) ->
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.interactive).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(dot))
                        Text(name, fontSize = 13.sp, color = c.textNormal, modifier = Modifier.weight(1f).padding(start = 8.dp))
                        Small(whenText)
                    }
                }
            }
        }
    }
    Tiles("#tags" to "a #word is a label; tap one to see only those", "a special note for to-dos" to "ticked ones fold away under the rest")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun QuickNotesPage() {
    val c = Obsidian.colors
    Intro("jot a thought down in a second, even from your lockscreen, before the phone pulls you elsewhere.")
    Label("type a quick note, even from the lock screen")
    val t = loop(8_000)
    Demo {
        Phone(width = 220.dp, height = 330.dp) {
            Screen(center = true) {
                Small("bedtime · until 07:00")
                Text("22:41", fontSize = 44.sp, color = c.textNormal)
                Small("thursday 19 november")
            }
            TapRing(t, 0.07f, Modifier.align(Alignment.Center).offset(y = 10.dp))
            TapRing(t, 0.13f, Modifier.align(Alignment.Center).offset(y = 10.dp))
            Sheet(slide(t, 0.18f, 0.25f, 0.92f, 0.99f)) {
                Text("a quick note", fontSize = 11.sp, color = c.textMuted)
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp)).border(1.dp, c.accent, RoundedCornerShape(9.dp)).background(c.bgPrimaryAlt).padding(horizontal = 9.dp, vertical = 7.dp)) {
                    Typed("book to read -", ramp(t, 0.28f, 0.40f), caret = t < 0.40f)
                    Typed("braiding sweetgrass", ramp(t, 0.40f, 0.58f), caret = t >= 0.40f)
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Chip("refuge notes", on = t >= 0.62f, small = true)
                    Chip("to-do", small = true)
                    Chip("microjournal", small = true)
                }
            }
            if (t in 0.70f..0.90f) Toast("kept in your notes", Modifier.align(Alignment.TopCenter).padding(top = 110.dp))
        }
    }
    Text("double tap any empty spot on the lock screen or home screen. if locked, it only ever adds: nothing already written shows.", fontSize = 12.sp, color = c.textFaint, lineHeight = 17.sp)
    Label("some of what quick note can do")
    Card {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("refuge notes", "the to-do", "today's microjournal", "a file in your vault", "the calendar, if it says when").forEach { Chip(it) }
        }
    }
}

@Composable
private fun VoicePage() {
    val c = Obsidian.colors
    Intro("speak your notes or journaling instead of typing. an offline model writes out what you said. to use it you need to download a local voice transcription model.")
    val t = loop(10_000)
    Demo {
        Box(Modifier.fillMaxWidth().height(270.dp)) {
            if (t < 0.5f) {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(9.dp).clip(CircleShape).background(c.red.copy(alpha = if ((t * 10).toInt() % 2 == 0) 1f else 0.3f)))
                        Text("recording", fontSize = 13.sp, color = c.textMuted, modifier = Modifier.padding(start = 8.dp))
                    }
                    Text("0:42", fontSize = 40.sp, color = c.textNormal)
                    Row(Modifier.height(44.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                        val delays = listOf(0f, .3f, .6f, .15f, .45f, .75f, .2f, .5f, .05f, .35f, .65f, .25f, .55f, .1f, .4f, .7f, .3f, .6f)
                        delays.forEach { d ->
                            val wave = 0.3f + 0.7f * ((kotlin.math.sin(((t * 10_000 / 900f) + d) * 2 * Math.PI) + 1) / 2).toFloat()
                            Box(Modifier.width(4.dp).height(40.dp * wave).clip(RoundedCornerShape(2.dp)).background(c.accent))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip("to today's microjournal", on = true)
                        Chip("as a note")
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().padding(4.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Small("today's microjournal")
                    Chip("▶ 18:05 · 0:42", on = true, small = true)
                    Text(
                        if (t < 0.76f) "18:05 · 0:42 · transcribing…" else "18:05 · 0:42 · finally said no to the extra project. felt lighter on the walk home.",
                        fontSize = 14.sp,
                        color = c.textNormal,
                        lineHeight = 20.sp,
                    )
                    Text("the recording stays a tap away above the text; the words can be edited like any line.", fontSize = 12.sp, color = c.textFaint, lineHeight = 17.sp)
                }
            }
        }
    }
    Label("start one from")
    Tiles("a gesture" to "use one, even locked", "a note" to "tap its mic button", "the journal" to "record straight in", columns = 3)
    Card { Text("set it up once, and start audio-journaling\n(instructions in settings)", fontSize = 13.sp, color = c.textNormal) }
}

@Composable
private fun ObsidianPage() {
    val c = Obsidian.colors
    Intro("if you keep notes in obsidian, refuge works with them, all on the phone.")
    Label("three ways they meet")
    Card(gap = 14.dp) {
        listOf(
            "find obsidian notes in quick search" to "type a note's title, and open it in obsidian or read it right there.",
            "add quick notes to your obsidian notes" to "send one straight to the end of a note in your vault, like today's daily note or a reading list.",
            "save refuge's notes to obsidian automatically" to "switch it on, and each note is saved to your vault as you change it; delete one, and it's deleted there too.",
        ).forEachIndexed { i, (title, line) ->
            Row {
                Box(Modifier.size(22.dp).clip(CircleShape).background(c.interactive), contentAlignment = Alignment.Center) { Text("${i + 1}", fontSize = 11.sp, color = c.accentText) }
                Column(Modifier.padding(start = 10.dp)) {
                    Text(title, fontSize = 14.sp, color = c.textNormal)
                    Text(line, fontSize = 12.sp, color = c.textFaint, lineHeight = 17.sp)
                }
            }
        }
    }
    Text("saving of refuge notes to obsidian is one-way only\n\nall other obsidian notes continue working as normal, and remain accessible in your obsidian app and vault", fontSize = 12.sp, color = c.textFaint, lineHeight = 17.sp)
}

@Composable
internal fun HabitsPage() {
    val c = Obsidian.colors
    Intro("simple, but effective and accessible habit tracker")
    Label("today")
    val t = loop(8_000)
    Card(gap = 0.dp) {
        HabitRow("read", done = t >= 0.36f, strength = 0.54f + 0.08f * ramp(t, 0.4f, 0.6f), note = "strong-ish")
        HabitRow("practice", done = true, strength = 0.82f, note = "strong")
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(26.dp).clip(CircleShape).border(2.dp, c.interactiveHover, CircleShape))
            Text("morning routine", fontSize = 15.sp, color = c.textNormal, modifier = Modifier.weight(1f).padding(start = 12.dp))
            Small(if (t >= 0.6f) "2 of 3" else "1 of 3")
        }
        Column(Modifier.padding(start = 38.dp, top = 6.dp, bottom = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text("squats", fontSize = 12.sp, color = c.textFaint, textDecoration = TextDecoration.LineThrough)
            Text("stretch", fontSize = 12.sp, color = if (t >= 0.6f) c.textFaint else c.textNormal, textDecoration = if (t >= 0.6f) TextDecoration.LineThrough else null)
            Text("plank", fontSize = 12.sp, color = c.textNormal)
        }
    }
    Text("a habit can be a tick, a count, or a set of steps for a routine. keep it to some days only, if you like.", fontSize = 12.sp, color = c.textFaint, lineHeight = 17.sp)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MicrojournalPage() {
    val c = Obsidian.colors
    Intro("special folder in notes just for journaling. every day you can easily add a line at any time. inspired from struthless' microjournaling concept: using our phones for something that turns our attention inwards rather than squandering it away")
    val t = loop(9_000)
    Card(gap = 10.dp) {
        Text("thursday 19 november · vent, mindset", fontSize = 15.sp, color = c.textNormal)
        JournalLine("08:12", "slept badly, but the walk helped")
        JournalLine("13:40", "why now? bored on the train · bored")
        JournalLine("18:05", "🎙 0:42 · finally said no to the extra project")
        if (t >= 0.25f) Row {
            Text("22:10 · ", fontSize = 13.sp, color = c.textFaint)
            Typed("long day, proud of the walk", ramp(t, 0.32f, 0.62f), caret = t < 0.62f, size = 13)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("tired", "happy", "anxious", "bored").forEach { Chip(it, on = it == "happy" && t >= 0.68f, small = true) }
        }
    }
    InfoCard("option of journaling prompts, when you want them", "pick from sections: vent, obligations, mindset, gratitude, ideation, trajectory. each has a few questions, all yours to change. they follow struthless's journalling system.")
}

@Composable
internal fun DictionaryPage() {
    val c = Obsidian.colors
    Intro("look up a word right from the lock screen, without unlocking the phone. the dictionaries are kept on the phone, so nothing else opens.")
    val t = loop(10_000)
    Demo(padding = 16.dp, center = false) {
        Field("d sonder", progress = ramp(t, 0.04f, 0.16f), big = true)
        if (t >= 0.2f) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("wiktionary", fontSize = 11.sp, color = c.accentText)
            Text("sonder", fontSize = 18.sp, color = c.textNormal)
            Text("noun. the realisation that each passer-by has a life as vivid and complex as your own.", fontSize = 13.sp, color = c.textNormal, lineHeight = 19.sp)
            Small("any word in it can be tapped to look that up too")
            Text(if (t >= 0.6f) "in your words note" else "+ save to words", fontSize = 13.sp, color = if (t >= 0.6f) c.textFaint else c.accentText)
        }
        Spacer(Modifier.height(if (t >= 0.2f) 40.dp else 200.dp))
    }
    Text("words can be saved to favorites, and they end up in one note.", fontSize = 12.sp, color = c.textFaint, lineHeight = 17.sp)
    InfoCard("set it up first", "the dictionary works offline, so you need to download one before you can use it. you set it up in settings › search.")
}

// ---------------------------------------------------------------- what the pages are made of

/** A fixed place in every loop, for pictures of a demo; null in the app, where they play. */
internal val LocalDemoTime = androidx.compose.runtime.staticCompositionLocalOf<Float?> { null }

/** A loop's place, 0 to 1, going round every [ms]. */
@Composable
private fun loop(ms: Int): Float {
    LocalDemoTime.current?.let { return it }
    val loop = rememberInfiniteTransition(label = "demo")
    val t by loop.animateFloat(0f, 1f, infiniteRepeatable(tween(ms, easing = LinearEasing), RepeatMode.Restart), label = "t")
    return t
}

/** How far [t] is from [from] to [to], 0 before and 1 after. */
private fun ramp(t: Float, from: Float, to: Float) = ((t - from) / (to - from)).coerceIn(0f, 1f)

/** A sheet's place: sliding in from [inFrom] to [inTo], out from [outFrom] to [outTo]. */
private fun slide(t: Float, inFrom: Float, inTo: Float, outFrom: Float, outTo: Float): Float =
    if (t >= outTo) 0f else minOf(ramp(t, inFrom, inTo), 1f - if (outTo > outFrom) ramp(t, outFrom, outTo) else 0f)

@Composable
private fun Intro(text: String) = Text(text, fontSize = 14.sp, color = Obsidian.colors.textMuted, lineHeight = 20.sp)

@Composable
private fun Label(text: String) = Text(text, fontSize = 13.sp, color = Obsidian.colors.accentText, modifier = Modifier.padding(top = 4.dp))

@Composable
private fun Small(text: String, modifier: Modifier = Modifier) = Text(text, fontSize = 10.sp, color = Obsidian.colors.textFaint, modifier = modifier)

/** The grey stage a demo plays on. */
@Composable
private fun Demo(padding: Dp = 14.dp, center: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Obsidian.colors.bgSecondary).padding(padding),
        horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun Card(gap: Dp = 8.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Obsidian.colors.bgSecondary).padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(gap), content = content)
}

@Composable
private fun InfoCard(title: String, body: String) = Card(gap = 6.dp) {
    Text(title, fontSize = 13.sp, color = Obsidian.colors.textNormal)
    Text(body, fontSize = 12.sp, color = Obsidian.colors.textFaint, lineHeight = 17.sp)
}

/** Small cards side by side, a title and a line each. */
@Composable
private fun Tiles(vararg tiles: Pair<String, String>, columns: Int = 2) {
    val c = Obsidian.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        tiles.toList().chunked(columns).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (title, line) ->
                    Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(c.bgSecondary).padding(horizontal = 12.dp, vertical = 10.dp)) {
                        Text(title, fontSize = 12.sp, color = c.textNormal)
                        Text(line, fontSize = 10.sp, color = c.textFaint, lineHeight = 14.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PageButton(text: String, filled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val c = Obsidian.colors
    Box(
        modifier.heightIn(min = 46.dp).clip(RoundedCornerShape(50)).background(if (filled) c.accentFill else Color.Transparent)
            .border(1.dp, if (filled) c.accentFill else c.border, RoundedCornerShape(50)).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 14.sp, color = if (filled) c.textOnAccent else c.textNormal, textAlign = TextAlign.Center)
    }
}

/** A phone drawn small, for a demo to play in. */
@Composable
private fun Phone(width: Dp = 240.dp, height: Dp = 440.dp, content: @Composable BoxScope.() -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(Modifier.size(width, height).clip(shape).background(Obsidian.colors.bgPrimaryAlt).border(1.dp, Obsidian.colors.border, shape), content = content)
}

/** One of the phone's screens, filling it. */
@Composable
private fun Screen(center: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().background(Obsidian.colors.bgPrimaryAlt).padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = if (center) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
private fun AppNames(names: List<String>, tapped: String? = null, t: Float = 0f, at: Float = 0f) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        names.forEach { name ->
            Box {
                Text(name, fontSize = 15.sp, color = Obsidian.colors.textNormal)
                if (name == tapped) TapRing(t, at, Modifier.offset(x = 22.dp, y = (-6).dp))
            }
        }
    }
}

/** A finger's touch, pressed briefly at [at]. */
@Composable
private fun TapRing(t: Float, at: Float, modifier: Modifier = Modifier) {
    val p = (t - at + 0.015f) / 0.04f
    if (p !in 0f..1f) return
    Box(
        modifier.size(32.dp).graphicsLayer {
            val s = 0.5f + p
            scaleX = s
            scaleY = s
            alpha = if (p < 0.5f) 1f else 2f - 2f * p
        }.border(2.dp, Obsidian.colors.textNormal, CircleShape),
    )
}

@Composable
private fun Finger(modifier: Modifier, visible: Boolean) {
    if (!visible) return
    Box(modifier.size(22.dp).clip(CircleShape).background(Obsidian.colors.textNormal.copy(alpha = 0.45f)).border(2.dp, Obsidian.colors.textNormal.copy(alpha = 0.8f), CircleShape))
}

@Composable
private fun MiniButton(text: String, filled: Boolean, alpha: Float = 1f, t: Float = 0f, tapAt: Float? = null) {
    val c = Obsidian.colors
    Box(
        Modifier.fillMaxWidth().height(40.dp).graphicsLayer { this.alpha = alpha }.clip(RoundedCornerShape(50)).background(if (filled) c.accentFill else Color.Transparent)
            .border(1.dp, if (filled) c.accentFill else c.border, RoundedCornerShape(50)),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontSize = 13.sp, color = if (filled) c.textOnAccent else c.textMuted)
        if (tapAt != null) TapRing(t, tapAt)
    }
}

/** The wait's ring, filled to [progress]. */
@Composable
private fun Ring(progress: Float, modifier: Modifier) {
    val c = Obsidian.colors
    Canvas(modifier) {
        val stroke = 5.dp.toPx()
        val inset = stroke / 2
        val arc = Size(size.width - stroke, size.height - stroke)
        drawArc(c.border, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
        drawArc(c.accent, -90f, 360f * progress, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
    }
}

@Composable
private fun FakeApp(name: String) {
    val c = Obsidian.colors
    Column(Modifier.fillMaxSize().background(c.bgPrimaryAlt)) {
        Text(name, fontSize = 14.sp, color = c.textNormal, modifier = Modifier.padding(16.dp))
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(10.dp)).background(c.interactive))
            Box(Modifier.fillMaxWidth(0.7f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(c.interactive))
            Box(Modifier.fillMaxWidth(0.45f).height(10.dp).clip(RoundedCornerShape(5.dp)).background(c.interactive))
        }
    }
}

/** The line under a demo, the one for the part playing now: each starts at its number. */
@Composable
private fun Caption(t: Float, vararg parts: Pair<Float, String>) {
    val text = parts.lastOrNull { t >= it.first }?.second ?: parts.first().second
    Text(text, fontSize = 13.sp, color = Obsidian.colors.textMuted, textAlign = TextAlign.Center, lineHeight = 19.sp, minLines = 2, modifier = Modifier.fillMaxWidth())
}

/** A panel rising from the phone's foot, [shown] of the way up. */
@Composable
private fun BoxScope.Sheet(shown: Float, full: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    if (shown <= 0f) return
    val c = Obsidian.colors
    val shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    Column(
        Modifier.align(Alignment.BottomCenter).fillMaxWidth().then(if (full) Modifier.fillMaxSize() else Modifier)
            .graphicsLayer { translationY = (1f - shown) * size.height }
            .clip(shape).background(c.bgSecondary).padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        content = content,
    )
}

/** A search field, with [text] typed as far as [progress]. */
@Composable
private fun Field(text: String, progress: Float = 1f, typed: Boolean = true, big: Boolean = false) {
    val c = Obsidian.colors
    Row(
        Modifier.fillMaxWidth().height(if (big) 40.dp else 34.dp).clip(RoundedCornerShape(50)).background(c.bgPrimaryAlt)
            .border(1.dp, if (typed) c.accent else c.border, RoundedCornerShape(50)).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (typed) Typed(text, progress, caret = true, size = if (big) 14 else 13) else Text(text, fontSize = 12.sp, color = c.textFaint)
    }
}

/** [text] typed as far as [progress], with the cursor after it while [caret]. */
@Composable
private fun Typed(text: String, progress: Float, caret: Boolean, size: Int = 12) {
    val c = Obsidian.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text.take(kotlin.math.ceil(text.length * progress).toInt()), fontSize = size.sp, color = c.textNormal, maxLines = 1)
        if (caret) Box(Modifier.width(1.dp).height((size + 2).dp).background(c.accentText))
    }
}

@Composable
private fun Result(title: String, line: String, picked: Boolean) {
    val c = Obsidian.colors
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(if (picked) c.accentFill else c.interactive).padding(10.dp)) {
        Text(title, fontSize = 12.sp, color = if (picked) c.textOnAccent else c.textNormal)
        Text(line, fontSize = 10.sp, color = if (picked) c.textOnAccent else c.textMuted)
    }
}

@Composable
private fun Chip(text: String, on: Boolean = false, small: Boolean = false) {
    val c = Obsidian.colors
    Text(
        text,
        fontSize = if (small) 11.sp else 12.sp,
        color = if (on) c.accentText else c.textMuted,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) c.accent.copy(alpha = 0.22f) else Color.Transparent)
            .border(1.dp, if (on) c.accent else c.border, RoundedCornerShape(50)).padding(horizontal = if (small) 9.dp else 12.dp, vertical = if (small) 4.dp else 6.dp),
    )
}

@Composable
private fun Toast(text: String, modifier: Modifier) {
    val c = Obsidian.colors
    Text(text, fontSize = 11.sp, color = c.textOnAccent, textAlign = TextAlign.Center, modifier = modifier.clip(RoundedCornerShape(10.dp)).background(c.accentFill).padding(horizontal = 12.dp, vertical = 8.dp))
}

@Composable
private fun Notice(title: String, line: String, modifier: Modifier = Modifier) {
    val c = Obsidian.colors
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(c.bgSecondary).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(title, fontSize = 10.sp, color = c.textNormal)
        Small(line)
    }
}

@Composable
private fun HabitDots(name: String, week: List<Boolean>, done: Boolean, t: Float, at: Float, compact: Boolean = false) {
    val c = Obsidian.colors
    val dot = if (compact) 11.dp else 13.dp
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(name, fontSize = if (compact) 11.sp else 12.sp, color = c.textNormal, modifier = Modifier.width(52.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(if (compact) 5.dp else 6.dp), verticalAlignment = Alignment.CenterVertically) {
            week.forEach { Box(Modifier.size(dot).clip(CircleShape).background(if (it) c.accent else c.interactive)) }
            Box(contentAlignment = Alignment.Center) {
                Box(Modifier.size(dot).clip(CircleShape).background(if (done) c.accent else Color.Transparent).border(2.dp, if (done) c.accent else c.accentText, CircleShape))
                TapRing(t, at)
            }
        }
    }
}

@Composable
private fun BudgetBar(over: Boolean) {
    val c = Obsidian.colors
    if (over) {
        Row(Modifier.fillMaxWidth().height(5.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Box(Modifier.weight(45f).fillMaxSize().clip(RoundedCornerShape(50)).background(c.textMuted))
            Box(Modifier.weight(5f).fillMaxSize().clip(RoundedCornerShape(50)).background(c.orange))
        }
    } else {
        Bar(40f / 45f, c.textMuted, height = 5.dp)
    }
}

@Composable
private fun Bar(fraction: Float, color: Color, height: Dp = 4.dp) {
    Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50)).background(Obsidian.colors.interactive)) {
        Box(Modifier.fillMaxWidth(fraction.coerceIn(0f, 1f)).fillMaxSize().clip(RoundedCornerShape(50)).background(color))
    }
}

@Composable
private fun EventLine(time: String, title: String, past: Boolean = false, now: Boolean = false) {
    val c = Obsidian.colors
    Row {
        Text(time, fontSize = 12.sp, color = if (now) c.accentText else c.textFaint, modifier = Modifier.width(44.dp))
        Text(title, fontSize = 12.sp, color = if (past) c.textFaint else c.textNormal, textDecoration = if (past) TextDecoration.LineThrough else null)
    }
}

@Composable
private fun Swatch(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(color))
        Text(label, fontSize = 11.sp, color = Obsidian.colors.textMuted, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun NoteCard(title: String, body: String? = null, tag: String? = null, tint: Color? = null, tagColor: Color? = null) {
    val c = Obsidian.colors
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(tint?.copy(alpha = 0.18f) ?: c.interactive).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tag?.let { Text(it, fontSize = 9.sp, color = tagColor ?: c.textMuted) }
        Text(title, fontSize = 12.sp, color = c.textNormal)
        body?.let { Text(it, fontSize = 11.sp, color = c.textMuted, lineHeight = 15.sp) }
    }
}

@Composable
private fun HabitRow(name: String, done: Boolean, strength: Float, note: String) {
    val c = Obsidian.colors
    Row(Modifier.fillMaxWidth().heightIn(min = 58.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(26.dp).clip(CircleShape).background(if (done) c.accent else Color.Transparent).border(2.dp, if (done) c.accent else c.interactiveHover, CircleShape), contentAlignment = Alignment.Center) {
            if (done) Text("✓", fontSize = 13.sp, color = c.bgPrimaryAlt)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(name, fontSize = 15.sp, color = c.textNormal)
            Bar(strength, c.accent)
        }
        Small(note)
    }
}

@Composable
private fun JournalLine(time: String, text: String) {
    Row {
        Text("$time · ", fontSize = 13.sp, color = Obsidian.colors.textFaint)
        Text(text, fontSize = 13.sp, color = Obsidian.colors.textNormal, lineHeight = 19.sp)
    }
}
