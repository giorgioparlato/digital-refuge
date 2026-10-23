package app.bedtime.ui.quotes

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import app.bedtime.data.Quote
import app.bedtime.data.Repository
import app.bedtime.ui.components.Text
import app.bedtime.ui.theme.Obsidian
import kotlinx.coroutines.launch

/** What the quote menu does, wired to your saved choices. */
class QuoteActions(val favourite: (Quote) -> Unit, val hide: (Quote) -> Unit)

/** Favouriting and hiding, saved straight away. A hide says where to undo it. */
@Composable
fun rememberQuoteActions(): QuoteActions {
    val context = LocalContext.current
    val repo = remember { Repository.get(context) }
    val scope = rememberCoroutineScope()
    return remember {
        QuoteActions(
            favourite = { quote ->
                scope.launch {
                    repo.updateQuotes {
                        it.copy(favourites = if (quote.text in it.favourites) it.favourites - quote.text else it.favourites + quote.text)
                    }
                }
            },
            hide = { quote ->
                scope.launch { repo.updateQuotes { it.copy(hidden = it.hidden + quote.text, favourites = it.favourites - quote.text) } }
                Toast.makeText(context, "hidden · undo it on the quotes page", Toast.LENGTH_SHORT).show()
            },
        )
    }
}

/**
 * The long-press menu on a quote: keep it as a favourite (or stop), never see it again, or move on to
 * another. Anchored to the quote it belongs to.
 */
@Composable
fun QuoteMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    favourite: Boolean,
    onFavourite: () -> Unit,
    onHide: () -> Unit,
    onAnother: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = Obsidian.colors
    Box(modifier) {
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, containerColor = c.bgSecondary) {
            DropdownMenuItem(
                text = { Text(if (favourite) "No longer a favourite" else "Favourite", color = c.textNormal) },
                leadingIcon = { Icon(if (favourite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, contentDescription = null, tint = c.accent) },
                onClick = {
                    onFavourite()
                    onDismiss()
                },
            )
            DropdownMenuItem(
                text = { Text("Don't show again", color = c.textNormal) },
                leadingIcon = { Icon(Icons.Default.Clear, contentDescription = null, tint = c.textMuted) },
                onClick = {
                    onHide()
                    onDismiss()
                },
            )
            DropdownMenuItem(
                text = { Text("Another one", color = c.textNormal) },
                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null, tint = c.textMuted) },
                onClick = {
                    onAnother()
                    onDismiss()
                },
            )
        }
    }
}
