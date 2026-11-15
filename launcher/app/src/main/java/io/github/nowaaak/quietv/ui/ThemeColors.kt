package io.github.nowaaak.quietv.ui

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import io.github.nowaaak.quietv.R
import io.github.nowaaak.quietv.config.LauncherConfig

object ThemeColors {

    // Pairs: the same colour for dark and for light themes. The first one is the default. Every shade
    // keeps 4.5:1 against the backgrounds of its themes (PaletteTest). Keep in sync with
    // cli/src/quietv/themes.py. The accent picker sorts them by hue.
    val DARK_ACCENTS = intArrayOf(
        LauncherConfig.DEFAULT_ACCENT,
        0xFF7DD3FC.toInt(),
        0xFFFCD34D.toInt(),
        0xFFFCA5A5.toInt(),
        0xFFC4B5FD.toInt(),
        0xFFF3F4F6.toInt(),
        0xFFFDBA74.toInt(),
        0xFFBEF264.toInt(),
        0xFF5EEAD4.toInt(),
        0xFFA5B4FC.toInt(),
        0xFFF0ABFC.toInt(),
        0xFFF9A8D4.toInt(),
    )

    val LIGHT_ACCENTS = intArrayOf(
        0xFF047857.toInt(),
        0xFF1D4ED8.toInt(),
        0xFF92400E.toInt(),
        0xFFB91C1C.toInt(),
        0xFF6D28D9.toInt(),
        0xFF15171C.toInt(),
        0xFF9A3412.toInt(),
        0xFF3F6212.toInt(),
        0xFF0F766E.toInt(),
        0xFF4338CA.toInt(),
        0xFFA21CAF.toInt(),
        0xFFBE185D.toInt(),
    )

    private val FOCUSED = intArrayOf(android.R.attr.state_focused)
    private val DEFAULT = intArrayOf()

    fun accents(light: Boolean): IntArray = if (light) LIGHT_ACCENTS else DARK_ACCENTS

    fun accentForTheme(current: Int, fromLight: Boolean, toLight: Boolean): Int {
        val index = accents(fromLight).indexOf(current)
        return if (index >= 0) accents(toLight)[index] else current
    }

    fun apply(activity: Activity, palette: Palette) {
        activity.setTheme(if (palette.light) R.style.Theme_Quietv_Light else R.style.Theme_Quietv)
        activity.window.setBackgroundDrawable(windowBackground(activity, palette))
    }

    fun windowBackground(activity: Activity, palette: Palette): Drawable {
        if (!palette.glass) return ColorDrawable(palette.background)
        val base = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(palette.background, palette.backgroundEnd),
        )
        if (palette.glow == 0) return base
        val metrics = activity.resources.displayMetrics
        val glow = GradientDrawable().apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            colors = intArrayOf(palette.glow, Color.TRANSPARENT)
            gradientRadius = maxOf(metrics.widthPixels, metrics.heightPixels) * GLOW_RADIUS
            setGradientCenter(GLOW_X, GLOW_Y)
        }
        return LayerDrawable(arrayOf(base, glow))
    }

    fun surfaceFill(palette: Palette): Int = if (palette.glass) {
        Contrast.withAlpha(Contrast.blend(palette.surface, Color.WHITE, GLASS_FROST), GLASS_FILL_ALPHA)
    } else {
        palette.surface
    }

    fun edge(palette: Palette): Int =
        if (palette.glass) Contrast.withAlpha(Color.WHITE, GLASS_EDGE_ALPHA) else palette.stroke

    fun card(palette: Palette, radius: Float): Drawable {
        if (!palette.glass) return rounded(palette.surface, radius)
        return LayerDrawable(arrayOf(rounded(surfaceFill(palette), radius), sheen(radius)))
    }

    private fun sheen(radius: Float): Drawable = GradientDrawable(
        GradientDrawable.Orientation.TOP_BOTTOM,
        intArrayOf(
            Contrast.withAlpha(Color.WHITE, GLASS_SHEEN_TOP_ALPHA),
            Contrast.withAlpha(Color.WHITE, GLASS_SHEEN_BOTTOM_ALPHA),
        ),
    ).apply { cornerRadius = radius }

    fun panel(palette: Palette, radius: Float, strokeWidth: Int): Drawable {
        if (!palette.glass) return rounded(palette.surface, radius, palette.stroke, strokeWidth)
        return LightEdge(radius, Contrast.withAlpha(palette.surface, GLASS_PANEL_ALPHA), rim(strokeWidth))
    }

    fun edged(palette: Palette, radius: Float, strokeWidth: Int): Drawable =
        LightEdge(radius, surfaceFill(palette), rim(strokeWidth))

    fun edgeOnly(radius: Float, strokeWidth: Int): Drawable = LightEdge(radius, Color.TRANSPARENT, rim(strokeWidth))

    fun halo(palette: Palette, accent: Int, density: Float): Drawable =
        FocusHalo(palette.text, Contrast.withAlpha(accent, HALO_ALPHA), HALO_DP * density)

    fun topContent(palette: Palette): ColorStateList =
        if (palette.glass) focusable(palette.onText, Contrast.withAlpha(palette.text, ICON_REST_ALPHA)) else pillContent(palette)

    private fun rim(strokeWidth: Int): Float = strokeWidth * GLASS_RIM

    fun pillContent(palette: Palette): ColorStateList = focusable(palette.onText, palette.text)

    fun pillContentDim(palette: Palette): ColorStateList = focusable(palette.onTextDim, palette.textMuted)

    fun pillBackground(palette: Palette): ColorStateList = focusable(palette.text, surfaceFill(palette))

    fun rowBackground(palette: Palette): ColorStateList = focusable(palette.text, Color.TRANSPARENT)

    fun rounded(fill: Int, radius: Float, stroke: Int = Color.TRANSPARENT, strokeWidth: Int = 0): GradientDrawable =
        GradientDrawable().apply {
            setColor(fill)
            cornerRadius = radius
            if (strokeWidth > 0) setStroke(strokeWidth, stroke)
        }

    private fun focusable(focused: Int, normal: Int): ColorStateList =
        ColorStateList(arrayOf(FOCUSED, DEFAULT), intArrayOf(focused, normal))

    private const val GLASS_FROST = 0.5f
    private const val GLASS_FILL_ALPHA = 0x1F
    private const val GLASS_PANEL_ALPHA = 0xF2
    private const val HALO_ALPHA = 0x59
    private const val HALO_DP = 3f
    private const val ICON_REST_ALPHA = 0xD9
    private const val GLASS_RIM = 1.25f
    private const val GLASS_EDGE_ALPHA = 0x38
    private const val GLASS_SHEEN_TOP_ALPHA = 0x1C
    private const val GLASS_SHEEN_BOTTOM_ALPHA = 0x03
    private const val GLOW_RADIUS = 0.55f
    private const val GLOW_X = 0.12f
    private const val GLOW_Y = 0.05f
}
