package io.github.nowaaak.quietv.settings

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import io.github.nowaaak.quietv.R
import io.github.nowaaak.quietv.config.LauncherConfig
import io.github.nowaaak.quietv.ui.ColorSlider
import io.github.nowaaak.quietv.ui.Contrast
import io.github.nowaaak.quietv.ui.Hsv
import io.github.nowaaak.quietv.ui.Palette
import io.github.nowaaak.quietv.ui.SwatchDrawable
import io.github.nowaaak.quietv.ui.ThemeColors
import io.github.nowaaak.quietv.ui.ThemePreview

/**
 * Accent colours as dots with a live preview of the home screen above them. "Custom colour" opens
 * three sliders for any colour.
 */
class AccentPicker(
    private val activity: Activity,
    private val palette: Palette,
    private val config: LauncherConfig,
    private val onPick: (Int) -> Unit,
) {

    private val density = activity.resources.displayMetrics.density
    private lateinit var sheet: PickerSheet
    private lateinit var preview: ImageView
    private lateinit var hex: TextView
    private lateinit var warning: TextView
    private lateinit var choices: FrameLayout

    fun show(anchor: View) {
        sheet = PickerSheet(
            activity, palette,
            activity.getString(R.string.picker_accent_title),
            activity.getString(R.string.picker_hint),
        )
        val body = activity.layoutInflater.inflate(R.layout.sheet_accent, sheet.content, false)
        preview = body.findViewById(R.id.preview)
        hex = body.findViewById<TextView>(R.id.hex).apply { setTextColor(palette.text) }
        warning = body.findViewById<TextView>(R.id.warning).apply { setTextColor(palette.textMuted) }
        choices = body.findViewById(R.id.choices)
        sheet.content.addView(body)
        showColor(config.accent)
        sheet.show(anchor, presets())
    }

    private fun showColor(color: Int) {
        preview.setImageDrawable(ThemePreview(palette, color, config.tileRadius))
        hex.text = LauncherConfig.formatColor(color)
        warning.visibility = if (Contrast.ratio(color, palette.background) < MIN_CONTRAST) View.VISIBLE else View.GONE
    }

    private fun pick(color: Int) {
        sheet.dismiss()
        if (color != config.accent) onPick(color)
    }

    /** Builds the grid of preset colours and returns the dot to focus first. */
    private fun presets(): View? {
        choices.removeAllViews()
        val column = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        // Rainbow order, with white or black at the end.
        val colors = ThemeColors.accents(palette.light).sortedBy { color ->
            Hsv.of(color).let { if (it.saturation < GREY) GREY_LAST else it.hue }
        }
        val size = (SWATCH_DP * density).toInt()
        val margin = (SWATCH_GAP_DP * density / 2).toInt()
        var focus: View? = null
        var first: View? = null
        colors.chunked(COLUMNS).forEach { line ->
            val row = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
            for (color in line) {
                val dot = View(activity).apply {
                    background = SwatchDrawable(color, palette.text, color == config.accent, RING_DP * density)
                    isFocusable = true
                    isClickable = true
                    contentDescription = LauncherConfig.formatColor(color)
                    setOnFocusChangeListener { _, focused -> if (focused) showColor(color) }
                    setOnClickListener { pick(color) }
                }
                if (color == config.accent) focus = dot
                if (first == null) first = dot
                row.addView(dot, LinearLayout.LayoutParams(size, size).apply { setMargins(margin, margin, margin, margin) })
            }
            column.addView(row)
        }
        val custom = config.accent !in colors
        val customRow = settingRow(
            column,
            activity.getString(R.string.accent_custom),
            if (custom) LauncherConfig.formatColor(config.accent) else null,
        ) { showEditor(Hsv.of(config.accent)) }
        customRow.setOnFocusChangeListener { _, focused -> if (focused) showColor(config.accent) }
        column.addView(customRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = (ROW_GAP_DP * density).toInt()
        })
        choices.addView(column)
        return focus ?: if (custom) customRow else first
    }

    private fun showEditor(start: Hsv) {
        choices.removeAllViews()
        var color = start
        val column = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        val sliders = mutableListOf<ColorSlider>()
        fun refresh() {
            sliders.forEach { it.show(color) }
            showColor(color.toColor())
        }
        val channels = listOf(
            ColorSlider.Channel.HUE to R.string.accent_hue,
            ColorSlider.Channel.SATURATION to R.string.accent_saturation,
            ColorSlider.Channel.VALUE to R.string.accent_brightness,
        )
        for ((channel, label) in channels) {
            val row = activity.layoutInflater.inflate(R.layout.item_slider, column, false)
            val labelView = row.findViewById<TextView>(R.id.label).apply {
                setText(label)
                setTextColor(palette.textMuted)
            }
            val slider = ColorSlider(activity).apply {
                this.channel = channel
                focusColor = palette.text
                contentDescription = activity.getString(label)
                onChange = { value ->
                    color = when (channel) {
                        ColorSlider.Channel.HUE -> color.copy(hue = value)
                        ColorSlider.Channel.SATURATION -> color.copy(saturation = value)
                        ColorSlider.Channel.VALUE -> color.copy(value = value)
                    }
                    refresh()
                }
                setOnFocusChangeListener { _, focused ->
                    labelView.setTextColor(if (focused) palette.text else palette.textMuted)
                }
            }
            row.findViewById<FrameLayout>(R.id.slot).addView(slider)
            sliders += slider
            column.addView(row)
        }
        val apply = settingRow(column, activity.getString(R.string.accent_apply), null) { pick(color.toColor()) }
        column.addView(apply, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = (ROW_GAP_DP * density).toInt()
        })
        choices.addView(column)
        refresh()
        sliders.first().requestFocus()
    }

    private fun settingRow(parent: ViewGroup, title: String, value: String?, action: () -> Unit): View {
        val row = activity.layoutInflater.inflate(R.layout.item_setting, parent, false)
        row.backgroundTintList = ThemeColors.rowBackground(palette)
        row.findViewById<TextView>(R.id.title).apply {
            text = title
            setTextColor(ThemeColors.pillContent(palette))
        }
        row.findViewById<TextView>(R.id.value).apply {
            setTextColor(ThemeColors.pillContentDim(palette))
            if (value == null) visibility = View.GONE else text = value
        }
        row.findViewById<View>(R.id.swatch).visibility = View.GONE
        row.setOnClickListener { action() }
        return row
    }

    private companion object {
        const val COLUMNS = 6
        const val SWATCH_DP = 52f
        const val SWATCH_GAP_DP = 18f
        const val RING_DP = 3f
        const val ROW_GAP_DP = 16f
        const val MIN_CONTRAST = 3.0
        const val GREY = 0.15f
        const val GREY_LAST = 1000f
    }
}
