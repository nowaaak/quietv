package io.github.nowaaak.quietv.ui

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import io.github.nowaaak.quietv.R
import io.github.nowaaak.quietv.config.LauncherConfig

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
        0xFF047857.toInt(),
        0xFF1D4ED8.toInt(),
        0xFF92400E.toInt(),
        0xFFB91C1C.toInt(),
        0xFF6D28D9.toInt(),
        0xFF15171C.toInt(),
    )

    private val FOCUSED = intArrayOf(android.R.attr.state_focused)
    private val DEFAULT = intArrayOf()

    fun accents(light: Boolean): IntArray = if (light) LIGHT_ACCENTS else DARK_ACCENTS

    fun nextAccent(light: Boolean, current: Int): Int {
        val list = accents(light)
        val index = list.indexOf(current)
        return list[(index + 1) % list.size]
    }

    fun accentForTheme(current: Int, fromLight: Boolean, toLight: Boolean): Int {
        val index = accents(fromLight).indexOf(current)
        return if (index >= 0) accents(toLight)[index] else current
    }

    fun apply(activity: Activity, palette: Palette) {
        activity.setTheme(if (palette.light) R.style.Theme_Quietv_Light else R.style.Theme_Quietv)
        activity.window.setBackgroundDrawable(ColorDrawable(palette.background))
    }

    fun pillContent(palette: Palette): ColorStateList = focusable(palette.onText, palette.text)

    fun pillContentDim(palette: Palette): ColorStateList = focusable(palette.onTextDim, palette.textMuted)

    fun pillBackground(palette: Palette): ColorStateList = focusable(palette.text, palette.surface)

    fun rowBackground(palette: Palette): ColorStateList = focusable(palette.text, Color.TRANSPARENT)

    fun rounded(fill: Int, radius: Float, stroke: Int = Color.TRANSPARENT, strokeWidth: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = radius
            if (strokeWidth > 0) setStroke(strokeWidth, stroke)
        }

    private fun focusable(focused: Int, normal: Int): ColorStateList =
        ColorStateList(arrayOf(FOCUSED, DEFAULT), intArrayOf(focused, normal))
}
