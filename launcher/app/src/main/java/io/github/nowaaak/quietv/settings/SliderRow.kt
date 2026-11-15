package io.github.nowaaak.quietv.settings

import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import io.github.nowaaak.quietv.R
import io.github.nowaaak.quietv.ui.Contrast
import io.github.nowaaak.quietv.ui.Palette
import io.github.nowaaak.quietv.ui.ValueSlider

/** A labelled slider with its current value on the right, as used by the pickers. */
class SliderRow(
    activity: Activity,
    parent: ViewGroup,
    palette: Palette,
    label: String,
    min: Int,
    max: Int,
    step: Int,
    start: Int,
    private val format: (Int) -> String,
    onChange: (Int) -> Unit,
) {

    val view = activity.layoutInflater.inflate(R.layout.item_slider, parent, false)
    private val valueView: TextView = view.findViewById(R.id.value)

    val slider = ValueSlider(activity).apply {
        this.min = min
        this.max = max
        this.step = step
        focusColor = palette.text
        thumbColor = palette.text
        fillColor = palette.text
        lineColor = Contrast.withAlpha(palette.text, LINE_ALPHA)
        contentDescription = label
        show(start)
    }

    init {
        val labelView = view.findViewById<TextView>(R.id.label).apply {
            text = label
            setTextColor(palette.textMuted)
        }
        valueView.setTextColor(palette.textMuted)
        valueView.text = format(start)
        slider.onChange = { value ->
            valueView.text = format(value)
            onChange(value)
        }
        slider.setOnFocusChangeListener { _, focused ->
            val color = if (focused) palette.text else palette.textMuted
            labelView.setTextColor(color)
            valueView.setTextColor(color)
        }
        view.findViewById<FrameLayout>(R.id.slot).addView(slider)
    }

    /** Moves the slider and its value label without calling onChange. */
    fun show(value: Int) {
        slider.show(value)
        valueView.text = format(slider.value)
    }

    private companion object {
        const val LINE_ALPHA = 0x40
    }
}
