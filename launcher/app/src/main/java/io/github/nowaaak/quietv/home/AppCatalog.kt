package io.github.nowaaak.quietv.home

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import io.github.nowaaak.quietv.config.LauncherConfig
import io.github.nowaaak.quietv.firetv.Platform

data class AppEntry(
    val label: String,
    val component: ComponentName,
    val banner: Drawable?,
    val icon: Drawable?,
    val isStockHome: Boolean = false,
) {
    val pkg: String get() = component.packageName
}

class AppCatalog(private val context: Context) {

    fun load(config: LauncherConfig, savedOrder: List<String>): List<AppEntry> {
        val pm = context.packageManager
        val leanback = pm.queryIntentActivities(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER),
            0,
        )
        val wanted = config.favorites.toSet() + config.extraApps
        val seen = leanback.mapNotNull { it.activityInfo?.packageName }.toMutableSet()
        val plain = pm.queryIntentActivities(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0)
            .filter { ri -> ri.activityInfo?.packageName?.let { it in wanted && seen.add(it) } == true }

        val hidden = config.hidden.toSet()
        val entries = (leanback + plain)
            .filter { ri -> ri.activityInfo != null }
            .filter { ri -> ri.activityInfo.packageName != context.packageName && ri.activityInfo.packageName !in hidden }
            .map { ri -> entry(pm, ri) }
            .toMutableList()
        stockHomeEntry(pm, config)?.let { entries += it }

        return TileOrder.sort(entries, savedOrder, config.favorites, key = { it.pkg }, label = { it.label })
    }

    fun label(pkg: String): String {
        val pm = context.packageManager
        return try {
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            pkg
        }
    }

    private fun entry(pm: PackageManager, ri: ResolveInfo): AppEntry {
        val info = ri.activityInfo
        val banner = info.loadBanner(pm) ?: info.applicationInfo.loadBanner(pm)
        return AppEntry(
            label = ri.loadLabel(pm).toString(),
            component = ComponentName(info.packageName, info.name),
            banner = banner,
            icon = ri.loadIcon(pm),
        )
    }

    private fun stockHomeEntry(pm: PackageManager, config: LauncherConfig): AppEntry? {
        if (!config.fireTv.showStockHomeTile) return null
        val stock = Platform.stockHomePackage(context, config)
        if (stock.isEmpty()) return null
        return try {
            val info = pm.getApplicationInfo(stock, 0)
            AppEntry(
                label = pm.getApplicationLabel(info).toString(),
                component = ComponentName(stock, STOCK_HOME_MARKER),
                banner = null,
                icon = pm.getApplicationIcon(info),
                isStockHome = true,
            )
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private companion object {
        const val STOCK_HOME_MARKER = "home"
    }
}
