package io.github.nowaaak.quietv.firetv

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import io.github.nowaaak.quietv.config.LauncherConfig

object Platform {

    private const val AMAZON_HOME = "com.amazon.tv.launcher"
    private const val FIRE_TV_FEATURE = "amazon.hardware.fire_tv"
    private const val HOME_ENTRY = "io.github.nowaaak.quietv.HomeEntry"
    private const val INPUTS_ENTRY = "io.github.nowaaak.quietv.InputsEntry"
    const val ACTION_VIEW_INPUTS = "com.android.tv.action.VIEW_INPUTS"

    fun isFireOs(context: Context): Boolean =
        Build.MANUFACTURER.equals("Amazon", ignoreCase = true) ||
            context.packageManager.hasSystemFeature(FIRE_TV_FEATURE)

    fun stockHomePackage(context: Context, config: LauncherConfig): String =
        config.homeRedirect.stockHomePackage.ifBlank { if (isFireOs(context)) AMAZON_HOME else "" }

    fun syncHomeEntry(context: Context) {
        setEntry(context, HOME_ENTRY, !isFireOs(context), PackageManager.COMPONENT_ENABLED_STATE_DEFAULT)
    }

    fun syncInputsEntry(context: Context, enabled: Boolean) {
        setEntry(context, INPUTS_ENTRY, enabled, PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
    }

    private fun setEntry(context: Context, name: String, enabled: Boolean, onState: Int) {
        val pm = context.packageManager
        val entry = ComponentName(context, name)
        val wanted = if (enabled) onState else PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        if (pm.getComponentEnabledSetting(entry) != wanted) {
            pm.setComponentEnabledSetting(entry, wanted, PackageManager.DONT_KILL_APP)
        }
    }

    fun redirectServiceListed(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val service = ComponentName(context, HomeRedirectService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == service }
    }

    fun accessibilityOn(context: Context): Boolean =
        Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1

    fun redirectServiceEnabled(context: Context): Boolean =
        redirectServiceListed(context) && accessibilityOn(context)
}
