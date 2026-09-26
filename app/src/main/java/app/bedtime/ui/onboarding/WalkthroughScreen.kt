package app.bedtime.ui.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.bedtime.ui.components.BedtimeIcons
import app.bedtime.ui.components.CtaButton
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.launch

private data class Page(val icon: ImageVector, val title: String, val body: String, val note: String)

private val pages = listOf(
    Page(
        BedtimeIcons.Refuge,
        "hours that ask less",
        "some hours are better spent not reaching for anything. you choose which ones, and the phone keeps to them.",
        "none of this leaves your phone. no account, no sync \u2014 the app has no internet permission at all.",
    ),
    Page(
        BedtimeIcons.Moon,
        "what a session is like",
        "the apps you named stay shut. the home screen thins out to what you chose. the colour can drain away.",
        "notifications wait. music and alarms are never silenced.",
    ),
    Page(
        BedtimeIcons.Hourglass,
        "the way out is slow on purpose",
        "a wait, a passage to copy, a password. long enough for the reaching to pass.",
        "each unlock in a day can make the next one longer. the count starts again each morning.",
    ),
    Page(
        BedtimeIcons.Target,
        "as firm as you want",
        "every way around a session has a switch of its own. turn them all on and there is no way out but the " +
            "steps you set yourself.",
        "the switches sit in each block, and in settings \u2192 staying blocked.",
    ),
)

/** The first-run walk-through: one idea a page. The fuller version lives in [OnboardingScreen]. */
@Composable
fun WalkthroughScreen(onDone: () -> Unit) {
    val c = Obsidian.colors
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val last = pager.currentPage == pages.lastIndex

    Scaffold(containerColor = c.bgPrimary) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Brush.verticalGradient(listOf(Color(0xFF1F2C25), c.bgPrimary))),
        ) {
            Row(Modifier.fillMaxWidth().padding(top = 16.dp, end = 26.dp), horizontalArrangement = Arrangement.End) {
                Text(
                    if (last) "" else "skip",
                    style = MaterialTheme.typography.labelLarge,
                    color = c.textFaint,
                    modifier = Modifier.clickable(enabled = !last, onClick = onDone).padding(6.dp),
                )
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index ->
                PageBody(pages[index])
            }
            Dots(pager.currentPage)
            Spacer(Modifier.height(18.dp))
            CtaButton(
                if (last) "get started" else "next",
                onClick = {
                    if (last) onDone() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 28.dp),
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun PageBody(page: Page) {
    val c = Obsidian.colors
    Column(Modifier.fillMaxSize().padding(horizontal = 28.dp)) {
        Spacer(Modifier.weight(0.7f))
        Box(
            Modifier.size(102.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier.size(70.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) { Icon(page.icon, contentDescription = null, tint = c.accent, modifier = Modifier.size(32.dp)) }
        }
        Spacer(Modifier.height(32.dp))
        Text(page.title, fontSize = 27.sp, fontWeight = FontWeight.Light, color = c.textNormal)
        Spacer(Modifier.height(14.dp))
        Text(page.body, style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 26.sp), color = c.textMuted)
        Spacer(Modifier.height(18.dp))
        Row {
            Box(Modifier.width(3.dp).height(46.dp).clip(CircleShape).background(c.accent.copy(alpha = 0.5f)))
            Spacer(Modifier.width(14.dp))
            Text(page.note, style = MaterialTheme.typography.bodyMedium, color = c.textFaint)
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun Dots(current: Int) {
    val c = Obsidian.colors
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        repeat(pages.size) { i ->
            Box(
                Modifier
                    .padding(horizontal = 4.dp)
                    .height(6.dp)
                    .width(if (i == current) 20.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (i == current) c.accent else c.textFaint.copy(alpha = 0.35f)),
            )
        }
    }
}
