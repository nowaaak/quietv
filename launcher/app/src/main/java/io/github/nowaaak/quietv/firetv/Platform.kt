package io.github.nowaaak.quietv.firetv

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import io.github.nowaaak.quietv.config.LauncherConfig

object Platform {

    private const val AMAZON_HOME = "com.amazon.tv.launcher"
    private const val FIRE_TV_FEATURE = "amazon.hardware.fire_tv"
    private const val HOME_ENTRY = "io.github.nowaaak.quietv.HomeEntry"

    fun isFireOs(context: Context): Boolean =
        Build.MANUFACTURER.equals("Amazon", ignoreCase = true) ||
            context.packageManager.hasSystemFeature(FIRE_TV_FEATURE)

    fun stockHomePackage(context: Context, config: LauncherConfig): String =
        config.homeRedirect.stockHomePackage.ifBlank { if (isFireOs(context)) AMAZON_HOME else "" }

    fun syncHomeEntry(context: Context) {
        val pm = context.packageManager
        val entry = ComponentName(context, HOME_ENTRY)
        val wanted = if (isFireOs(context)) {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DEFAULT
        }
        if (pm.getComponentEnabledSetting(entry) != wanted) {
            pm.setComponentEnabledSetting(entry, wanted, PackageManager.DONT_KILL_APP)
        }
    }

    fun redirectServiceEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        val service = ComponentName(context, HomeRedirectService::class.java)
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == service }
    }

    fun redirectServiceRunning(context: Context): Boolean {
        val manager = context.getSystemService(AccessibilityManager::class.java) ?: return false
        val service = ComponentName(context, HomeRedirectService::class.java)
        return manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any {
            val info = it.resolveInfo.serviceInfo
            ComponentName(info.packageName, info.name) == service
        }
    }
}
