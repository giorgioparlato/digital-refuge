package app.bedtime.service

import android.app.role.RoleManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

/** digital refuge as the phone's home screen: offered only when you ask for it, and given back when you don't. */
object HomeRole {
    private fun alias(context: Context) = ComponentName(context.packageName, "${context.packageName}.Home")

    /** Lets Android offer digital refuge as a home screen, or stops it. */
    fun setOffered(context: Context, on: Boolean) {
        context.packageManager.setComponentEnabledSetting(
            alias(context),
            if (on) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
    }

    /** Whether pressing Home lands here now. */
    fun isDefault(context: Context): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolved = runCatching { context.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY) }.getOrNull()
        return resolved?.activityInfo?.packageName == context.packageName
    }

    /** Android's own question, "use digital refuge as your home app?", or its home-app page on older phones. */
    fun askIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roles = context.getSystemService(RoleManager::class.java)
            if (roles != null && roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
                roles.createRequestRoleIntent(RoleManager.ROLE_HOME)
            } else {
                Intent(Settings.ACTION_HOME_SETTINGS)
            }
        } else {
            Intent(Settings.ACTION_HOME_SETTINGS)
        }
}
