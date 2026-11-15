package io.github.nowaaak.quietv.settings

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.InputFilter
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import io.github.nowaaak.quietv.R
import io.github.nowaaak.quietv.config.LauncherConfig
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
        ) { showEditor(config.accent) }
        customRow.setOnFocusChangeListener { _, focused -> if (focused) showColor(config.accent) }
        column.addView(customRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = (ROW_GAP_DP * density).toInt()
        })
        choices.addView(column)
        return focus ?: if (custom) customRow else first
    }

    /** A hex field and red, green and blue sliders. Each one updates the others and the preview. */
    private fun showEditor(start: Int) {
        choices.removeAllViews()
        var color = start or OPAQUE
        var updating = false
        val column = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        val rows = mutableListOf<SliderRow>()
        val hexField = hexField()

        fun refresh(fromHex: Boolean) {
            updating = true
            val channels = intArrayOf(Color.red(color), Color.green(color), Color.blue(color))
            rows.forEachIndexed { i, row ->
                row.show(channels[i])
                row.slider.thumbColor = color
                row.slider.trackColors = intArrayOf(withChannel(color, i, 0), withChannel(color, i, MAX_CHANNEL))
            }
            if (!fromHex) {
                hexField.setText(LauncherConfig.formatColor(color))
                hexField.setSelection(hexField.text.length)
            }
            updating = false
            showColor(color)
        }

        hexField.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (updating) return
                val digits = s.toString().trim().removePrefix("#")
                val parsed = LauncherConfig.parseColor("#$digits") ?: return
                color = parsed
                refresh(fromHex = true)
            }
        })
        val hexRow = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        hexRow.addView(TextView(activity).apply {
            setText(R.string.accent_hex)
            setTextColor(palette.textMuted)
            textSize = LABEL_SP
            typeface = activity.resources.getFont(R.font.geist)
        }, LinearLayout.LayoutParams((LABEL_DP * density).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT))
        hexRow.addView(hexField, LinearLayout.LayoutParams((HEX_DP * density).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT))
        column.addView(hexRow, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = (ROW_GAP_DP * density / 2).toInt()
        })

        val labels = intArrayOf(R.string.accent_red, R.string.accent_green, R.string.accent_blue)
        labels.forEachIndexed { channel, label ->
            val row = SliderRow(
                activity, column, palette, activity.getString(label),
                min = 0, max = MAX_CHANNEL, step = CHANNEL_STEP, start = 0,
                format = { it.toString() },
            ) { value ->
                color = withChannel(color, channel, value)
                refresh(fromHex = false)
            }
            row.slider.setOnClickListener { pick(color) }
            rows += row
            column.addView(row.view)
        }
        val apply = settingRow(column, activity.getString(R.string.accent_apply), null) { pick(color) }
        column.addView(apply, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = (ROW_GAP_DP * density).toInt()
        })
        choices.addView(column)
        refresh(fromHex = false)
        rows.first().slider.requestFocus()
    }

    private fun hexField(): EditText = EditText(activity).apply {
        setTextColor(palette.text)
        setHintTextColor(palette.textMuted)
        hint = LauncherConfig.formatColor(LauncherConfig.DEFAULT_ACCENT)
        textSize = HEX_SP
        typeface = activity.resources.getFont(R.font.geist)
        isSingleLine = true
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
            InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        imeOptions = EditorInfo.IME_ACTION_DONE
        filters = arrayOf<InputFilter>(InputFilter.LengthFilter(HEX_LENGTH))
        contentDescription = activity.getString(R.string.accent_hex)
        val padding = (HEX_PADDING_DP * density).toInt()
        setPadding(padding, padding / 2, padding, padding / 2)
        background = GradientDrawable().apply {
            cornerRadius = HEX_RADIUS_DP * density
            setColor(Contrast.withAlpha(palette.text, HEX_FILL_ALPHA))
            setStroke(
                (HEX_RING_DP * density).toInt(),
                ColorStateList(
                    arrayOf(intArrayOf(android.R.attr.state_focused), intArrayOf()),
                    intArrayOf(palette.text, Color.TRANSPARENT),
                ),
            )
        }
    }

    private fun withChannel(color: Int, channel: Int, value: Int): Int = when (channel) {
        0 -> Color.rgb(value, Color.green(color), Color.blue(color))
        1 -> Color.rgb(Color.red(color), value, Color.blue(color))
        else -> Color.rgb(Color.red(color), Color.green(color), value)
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
        const val OPAQUE = 0xFF000000.toInt()
        const val MAX_CHANNEL = 255
        const val CHANNEL_STEP = 5
        const val HEX_LENGTH = 7
        const val LABEL_DP = 140f
        const val LABEL_SP = 15f
        const val HEX_DP = 200f
        const val HEX_SP = 20f
        const val HEX_PADDING_DP = 14f
        const val HEX_RADIUS_DP = 10f
        const val HEX_RING_DP = 2f
        const val HEX_FILL_ALPHA = 0x14
    }
}
