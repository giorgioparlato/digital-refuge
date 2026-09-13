package app.bedtime.ui.components

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Lowercases the string resources that library composables read for themselves, like the time picker's
 * "AM"/"PM", so they follow the app's all-lowercase rule.
 */
@Composable
fun LowercaseStrings(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val lowercase = remember(context) { LowercaseContext(context) }
    CompositionLocalProvider(LocalContext provides lowercase, content = content)
}

private class LowercaseContext(base: Context) : ContextWrapper(base) {
    private val lowercase = LowercaseResources(base.resources)

    override fun getResources(): Resources = lowercase
}

/** Everything but strings goes to the real resources. */
@Suppress("DEPRECATION")
private class LowercaseResources(private val base: Resources) :
    Resources(base.assets, base.displayMetrics, base.configuration) {
    override fun getString(id: Int): String = base.getString(id).lowercase()

    override fun getString(id: Int, vararg formatArgs: Any?): String = base.getString(id, *formatArgs).lowercase()

    override fun getText(id: Int): CharSequence = base.getText(id).toString().lowercase()
}
