package app.bedtime.ui.quotes

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.bedtime.data.Quote
import app.bedtime.data.QuotePrefs
import app.bedtime.data.QuoteSource
import app.bedtime.data.Quotes
import app.bedtime.data.Repository
import app.bedtime.data.credit
import app.bedtime.ui.components.ObsidianTextField
import app.bedtime.ui.components.ObsidianTopBar
import app.bedtime.ui.components.PageGutter
import app.bedtime.ui.components.QuickChip
import app.bedtime.ui.components.SectionCard
import app.bedtime.ui.components.SectionLabel
import app.bedtime.ui.components.SegmentedChoice
import app.bedtime.ui.components.SoftPillButton
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.launch
import java.time.LocalDateTime

/** Your own quotes, favourites and hidden ones, and which of them rotate. */
@Composable
fun QuotesScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    val settings by repo.settings.collectAsStateWithLifecycle(initialValue = null)
    val current = settings ?: return
    QuotesContent(
        prefs = current.quotes,
        recent = Quotes.recent(LocalDateTime.now(), current.homeStyle.quoteRefresh, current.quotes),
        onChange = { transform -> scope.launch { repo.updateQuotes(transform) } },
        onBack = onBack,
    )
}

private enum class AppFilter { ALL, FAVOURITES, HIDDEN }

@Composable
internal fun QuotesContent(
    prefs: QuotePrefs,
    recent: List<Quote>,
    onChange: ((QuotePrefs) -> QuotePrefs) -> Unit,
    onBack: () -> Unit,
) {
    val c = Obsidian.colors
    val toggleFavourite = { q: Quote -> onChange { it.copy(favourites = it.favourites.toggle(q.text)) } }
    val hide = { q: Quote -> onChange { it.copy(hidden = it.hidden + q.text, favourites = it.favourites - q.text) } }
    val unhide = { q: Quote -> onChange { it.copy(hidden = it.hidden - q.text) } }
    var filter by rememberSaveable { mutableStateOf(AppFilter.ALL) }
    // The one of yours being edited, by its text; null while adding a new one.
    var editing by rememberSaveable { mutableStateOf<String?>(null) }
    var text by rememberSaveable { mutableStateOf("") }
    var author by rememberSaveable { mutableStateOf("") }
    var work by rememberSaveable { mutableStateOf("") }
    var pasting by rememberSaveable { mutableStateOf(false) }
    var pasted by rememberSaveable { mutableStateOf("") }
    val clearForm = {
        editing = null
        text = ""
        author = ""
        work = ""
    }

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = { ObsidianTopBar("Quotes", onBack = onBack, subtitle = "Changes save as you go") },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = PageGutter),
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            item(key = "source") {
                SectionLabel("Which quotes")
                SegmentedChoice(
                    options = listOf("App's and yours", "Only yours", "Only favourites"),
                    selected = prefs.source.ordinal,
                    onSelect = { i -> onChange { it.copy(source = QuoteSource.entries[i]) } },
                )
                fallbackNote(prefs)?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = c.textMuted, modifier = Modifier.padding(top = 10.dp))
                }
            }

            if (recent.isNotEmpty()) {
                item(key = "recent") {
                    SectionLabel("Recently shown")
                    SectionCard {
                        recent.forEach { q ->
                            QuoteRow(q) {
                                FavouriteButton(q.text in prefs.favourites) { toggleFavourite(q) }
                                SmallIcon(Icons.Default.Clear, "don't show again") { hide(q) }
                            }
                        }
                    }
                }
            }

            item(key = "mine") {
                SectionLabel("Yours")
                SectionCard {
                    if (prefs.mine.isEmpty()) {
                        Text(
                            "None yet. Add one below, or paste many at once.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textMuted,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
                    prefs.mine.forEach { q ->
                        val hidden = q.text in prefs.hidden
                        QuoteRow(q, dimmed = hidden) {
                            if (hidden) {
                                ShowAgain { unhide(q) }
                            } else {
                                FavouriteButton(q.text in prefs.favourites) { toggleFavourite(q) }
                            }
                            SmallIcon(Icons.Default.Edit, "edit") {
                                editing = q.text
                                text = q.text
                                author = q.author
                                work = q.work.orEmpty()
                                pasting = false
                            }
                            SmallIcon(Icons.Default.Delete, "delete") {
                                if (editing == q.text) clearForm()
                                onChange { it.removeMine(q.text) }
                            }
                        }
                    }
                }
            }

            item(key = "add") {
                Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickChip(if (editing == null) "Add one" else "Edit", selected = !pasting) { pasting = false }
                        QuickChip("Paste many", selected = pasting, enabled = editing == null) { pasting = true }
                    }
                    if (pasting) {
                        ObsidianTextField(
                            pasted,
                            { pasted = it },
                            singleLine = false,
                            placeholder = "One a line, each can end in — author, where it's from",
                            modifier = Modifier.fillMaxWidth().height(160.dp),
                        )
                        val parsed = Quotes.parseMany(pasted)
                        SoftPillButton(
                            when (parsed.size) {
                                0 -> "Add"
                                1 -> "Add 1 quote"
                                else -> "Add ${parsed.size} quotes"
                            },
                            onClick = {
                                onChange { it.addMine(parsed) }
                                pasted = ""
                            },
                            enabled = parsed.isNotEmpty(),
                        )
                    } else {
                        ObsidianTextField(text, { text = it }, singleLine = false, label = "Quote", modifier = Modifier.fillMaxWidth())
                        ObsidianTextField(author, { author = it }, label = "Author (optional)", modifier = Modifier.fillMaxWidth())
                        ObsidianTextField(work, { work = it }, label = "Where it's from (optional)", modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            SoftPillButton(
                                if (editing == null) "Add" else "Save",
                                onClick = {
                                    val quote = Quote(text.trim(), author.trim(), work.trim().ifEmpty { null })
                                    val old = editing
                                    onChange { if (old == null) it.addMine(listOf(quote)) else it.editMine(old, quote) }
                                    clearForm()
                                },
                                enabled = text.isNotBlank(),
                            )
                            if (editing != null) {
                                Text(
                                    "Cancel",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = c.textMuted,
                                    modifier = Modifier.clip(CircleShape).clickable(onClick = clearForm).padding(12.dp),
                                )
                            }
                        }
                    }
                }
            }

            item(key = "app-label") {
                SectionLabel("The app's quotes", Modifier.padding(top = 12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                    QuickChip("All", filter == AppFilter.ALL) { filter = AppFilter.ALL }
                    QuickChip("Favourites", filter == AppFilter.FAVOURITES) { filter = AppFilter.FAVOURITES }
                    QuickChip("Hidden", filter == AppFilter.HIDDEN) { filter = AppFilter.HIDDEN }
                }
            }
            val shown = Quotes.all.filter {
                when (filter) {
                    AppFilter.ALL -> true
                    AppFilter.FAVOURITES -> it.text in prefs.favourites
                    AppFilter.HIDDEN -> it.text in prefs.hidden
                }
            }
            item(key = "app") {
                SectionCard {
                    if (shown.isEmpty()) {
                        Text(
                            if (filter == AppFilter.FAVOURITES) "No favourites among these yet." else "Nothing hidden.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textMuted,
                            modifier = Modifier.padding(vertical = 6.dp),
                        )
                    }
                    shown.forEach { q ->
                        val hidden = q.text in prefs.hidden
                        QuoteRow(q, dimmed = hidden) {
                            if (hidden) {
                                ShowAgain { unhide(q) }
                            } else {
                                FavouriteButton(q.text in prefs.favourites) { toggleFavourite(q) }
                                SmallIcon(Icons.Default.Clear, "don't show again") { hide(q) }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

/** Why the rotation isn't what was picked, when it has fallen back; null when it hasn't. */
private fun fallbackNote(prefs: QuotePrefs): String? = when (prefs.source) {
    QuoteSource.ALL -> null
    QuoteSource.MINE -> if (prefs.mine.none { it.text !in prefs.hidden }) {
        "You have no quotes of your own showing yet, so the app's and yours are rotating."
    } else {
        null
    }
    QuoteSource.FAVOURITES -> if ((Quotes.all + prefs.mine).none { it.text in prefs.favourites && it.text !in prefs.hidden }) {
        "You have no favourites yet, so the app's and yours are rotating. Hold a quote or tap ♡ to add one."
    } else {
        null
    }
}

private fun Set<String>.toggle(key: String) = if (key in this) this - key else this + key

/** Adds your quotes, skipping any already there. */
internal fun QuotePrefs.addMine(quotes: List<Quote>): QuotePrefs {
    val known = (Quotes.all + mine).map { it.text }.toMutableSet()
    return copy(mine = mine + quotes.filter { known.add(it.text) })
}

/** Replaces one of yours; its favourite and hidden marks move with it. */
internal fun QuotePrefs.editMine(oldText: String, quote: Quote): QuotePrefs {
    fun Set<String>.moved() = if (oldText in this) this - oldText + quote.text else this
    return copy(
        mine = mine.map { if (it.text == oldText) quote else it }.distinctBy { it.text },
        favourites = favourites.moved(),
        hidden = hidden.moved(),
    )
}

internal fun QuotePrefs.removeMine(text: String): QuotePrefs =
    copy(mine = mine.filterNot { it.text == text }, favourites = favourites - text, hidden = hidden - text)

@Composable
private fun QuoteRow(quote: Quote, dimmed: Boolean = false, actions: @Composable () -> Unit) {
    val c = Obsidian.colors
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f).alpha(if (dimmed) 0.45f else 1f).padding(top = 6.dp)) {
            Text("“${quote.text}”", style = MaterialTheme.typography.bodyMedium, color = c.textNormal)
            quote.credit()?.let {
                Text("— $it", style = MaterialTheme.typography.labelMedium, color = c.textFaint, modifier = Modifier.padding(top = 4.dp))
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) { actions() }
    }
}

@Composable
private fun FavouriteButton(favourite: Boolean, onClick: () -> Unit) =
    SmallIcon(
        if (favourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
        if (favourite) "no longer a favourite" else "favourite",
        tint = if (favourite) Obsidian.colors.accent else Obsidian.colors.textFaint,
        onClick = onClick,
    )

@Composable
private fun SmallIcon(icon: ImageVector, label: String, tint: Color = Obsidian.colors.textFaint, onClick: () -> Unit) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).clickable(onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp)) }
}

@Composable
private fun ShowAgain(onClick: () -> Unit) {
    Text(
        "Show again",
        style = MaterialTheme.typography.labelMedium,
        color = Obsidian.colors.accentText,
        modifier = Modifier.clip(CircleShape).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 10.dp),
    )
}
