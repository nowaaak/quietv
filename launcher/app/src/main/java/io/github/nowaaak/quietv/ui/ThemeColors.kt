package io.github.nowaaak.quietv.ui

import android.app.Activity
import android.content.Context
import android.util.TypedValue
import androidx.annotation.AttrRes
import io.github.nowaaak.quietv.R
import io.github.nowaaak.quietv.config.LauncherConfig
import io.github.nowaaak.quietv.config.ThemeMode

object ThemeColors {

    val DARK_ACCENTS = intArrayOf(
        LauncherConfig.DEFAULT_ACCENT,
        0xFF7DD3FC.toInt(),
        0xFFFCD34D.toInt(),
        0xFFFCA5A5.toInt(),
        0xFFC4B5FD.toInt(),
        0xFFF3F4F6.toInt(),
    )

    val LIGHT_ACCENTS = intArrayOf(
        0xFF059669.toInt(),
        0xFF2563EB.toInt(),
        0xFFD97706.toInt(),
        0xFFDC2626.toInt(),
        0xFF7C3AED.toInt(),
        0xFF15171C.toInt(),
    )

    fun accents(theme: ThemeMode): IntArray = if (theme == ThemeMode.LIGHT) LIGHT_ACCENTS else DARK_ACCENTS

    fun nextAccent(theme: ThemeMode, current: Int): Int {
        val list = accents(theme)
        val index = list.indexOf(current)
        return list[(index + 1) % list.size]
    }

    fun accentForTheme(current: Int, from: ThemeMode, to: ThemeMode): Int {
        val index = accents(from).indexOf(current)
        return if (index >= 0) accents(to)[index] else current
    }

    fun apply(activity: Activity, theme: ThemeMode) {
        activity.setTheme(if (theme == ThemeMode.LIGHT) R.style.Theme_Quietv_Light else R.style.Theme_Quietv)
    }
}

fun Context.themeColor(@AttrRes attr: Int): Int {
    val value = TypedValue()
    theme.resolveAttribute(attr, value, true)
    return if (value.resourceId != 0) getColor(value.resourceId) else value.data
}
