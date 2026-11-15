package io.github.nowaaak.quietv.settings

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import io.github.nowaaak.quietv.R
import io.github.nowaaak.quietv.config.LauncherConfig
import io.github.nowaaak.quietv.config.ThemePreset
import io.github.nowaaak.quietv.ui.Palette
import io.github.nowaaak.quietv.ui.ThemeColors
import io.github.nowaaak.quietv.ui.ThemePreview

/** Every theme as a small home screen, so you see what you get before you pick it. */
class ThemePicker(
    private val activity: Activity,
    private val palette: Palette,
    private val config: LauncherConfig,
    private val onPick: (ThemePreset) -> Unit,
) {

    private val density = activity.resources.displayMetrics.density

    fun show(anchor: View) {
        val sheet = PickerSheet(
            activity, palette,
            activity.getString(R.string.picker_theme_title),
            activity.getString(R.string.picker_hint),
        )
        val grid = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        var focus: View? = null
        ThemePreset.entries.chunked(COLUMNS).forEach { presets ->
            val row = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                clipChildren = false
            }
            for (preset in presets) {
                val card = card(preset, row) {
                    sheet.dismiss()
                    if (preset != config.theme || config.colors.isNotEmpty()) onPick(preset)
                }
                if (preset == config.theme) focus = card.findViewById(R.id.preview)
                row.addView(card)
            }
            grid.addView(row)
        }
        sheet.content.addView(grid)
        sheet.show(anchor, focus)
    }

    private fun card(preset: ThemePreset, parent: LinearLayout, pick: () -> Unit): View {
        val current = preset == config.theme
        // The current theme keeps your own colours, a new one starts from its own.
        val look = if (current) palette else Palette.of(preset, emptyMap())
        val accent = ThemeColors.accentForTheme(config.accent, palette.light, look.light)
        val view = activity.layoutInflater.inflate(R.layout.item_theme_card, parent, false)
        val preview = view.findViewById<ImageView>(R.id.preview)
        preview.setImageDrawable(ThemePreview(look, accent, config.tileRadius))
        preview.foreground = GradientDrawable().apply {
            cornerRadius = RING_RADIUS_DP * density
            setStroke((RING_DP * density).toInt(), ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_focused), intArrayOf()),
                intArrayOf(palette.text, Color.TRANSPARENT),
            ))
        }
        preview.contentDescription = name(preset)
        preview.setOnClickListener { pick() }
        preview.setOnFocusChangeListener { v, focused ->
            val scale = if (focused) FOCUS_SCALE else 1f
            v.animate().scaleX(scale).scaleY(scale).setDuration(FOCUS_MS)
                .setInterpolator(DecelerateInterpolator()).start()
        }
        view.findViewById<TextView>(R.id.name).apply {
            text = if (current && config.colors.isNotEmpty()) {
                activity.getString(R.string.theme_custom, name(preset))
            } else {
                name(preset)
            }
            setTextColor(if (current) palette.text else palette.textMuted)
        }
        view.findViewById<View>(R.id.current).apply {
            visibility = if (current) View.VISIBLE else View.GONE
            backgroundTintList = ColorStateList.valueOf(config.accent)
        }
        return view
    }

    private fun name(preset: ThemePreset): String = activity.getString(nameRes(preset))

    companion object {
        private const val COLUMNS = 4
        private const val RING_DP = 2.5f
        private const val RING_RADIUS_DP = 10f
        private const val FOCUS_SCALE = 1.06f
        private const val FOCUS_MS = 150L

        fun nameRes(preset: ThemePreset): Int = when (preset) {
            ThemePreset.GRAPHITE -> R.string.theme_graphite
            ThemePreset.SAND -> R.string.theme_sand
            ThemePreset.GLASS -> R.string.theme_glass
        }
    }
}
