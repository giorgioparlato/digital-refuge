package app.bedtime.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.ConcurrentHashMap

data class AppEntry(val packageName: String, val label: String)

object AppCatalog {
    private val iconCache = ConcurrentHashMap<String, ImageBitmap>()

    /** Every app with a launcher icon, except this one, sorted by name. */
    fun launchableApps(context: Context): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return context.packageManager.queryIntentActivities(intent, 0)
            .map { it.activityInfo.packageName }
            .distinct()
            .filter { it != context.packageName }
            .map { AppEntry(it, label(context, it)) }
            .sortedBy { it.label.lowercase() }
    }

    fun label(context: Context, pkg: String): String = try {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        pkg
    }

    fun icon(context: Context, pkg: String): ImageBitmap? = iconCache[pkg] ?: try {
        context.packageManager.getApplicationIcon(pkg).toBitmap(96, 96).asImageBitmap().also { iconCache[pkg] = it }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }

    fun isLaunchable(context: Context, pkg: String): Boolean =
        context.packageManager.getLaunchIntentForPackage(pkg) != null

    fun launch(context: Context, pkg: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return true
    }
}
