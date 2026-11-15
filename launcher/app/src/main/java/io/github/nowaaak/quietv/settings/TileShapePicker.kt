package io.github.nowaaak.quietv.settings

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import io.github.nowaaak.quietv.R
import io.github.nowaaak.quietv.config.LauncherConfig
import io.github.nowaaak.quietv.ui.Palette
import io.github.nowaaak.quietv.ui.ThemePreview

/** Tile corners on a slider, with the home screen above it changing as you move it. */
class TileShapePicker(
    private val activity: Activity,
    private val palette: Palette,
    private val config: LauncherConfig,
    private val onPick: (Int) -> Unit,
) {

    private val density = activity.resources.displayMetrics.density

    fun show(anchor: View) {
        val sheet = PickerSheet(
            activity, palette,
            activity.getString(R.string.settings_tile_shape),
            activity.getString(R.string.picker_slider_hint),
        )
        val column = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        val preview = ImageView(activity).apply {
            scaleType = ImageView.ScaleType.FIT_XY
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            setImageDrawable(ThemePreview(palette, config.accent, config.tileRadius))
        }
        column.addView(
            preview,
            LinearLayout.LayoutParams((PREVIEW_WIDTH_DP * density).toInt(), (PREVIEW_HEIGHT_DP * density).toInt()).apply {
                bottomMargin = (GAP_DP * density).toInt()
            },
        )
        val row = SliderRow(
            activity, column, palette, activity.getString(R.string.tile_radius),
            min = 0, max = LauncherConfig.MAX_TILE_RADIUS, step = 1, start = config.tileRadius,
            format = { activity.getString(R.string.dp_value, it) },
        ) { radius ->
            preview.setImageDrawable(ThemePreview(palette, config.accent, radius))
        }
        row.slider.setOnClickListener {
            sheet.dismiss()
            if (row.slider.value != config.tileRadius) onPick(row.slider.value)
        }
        column.addView(row.view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        sheet.content.addView(column)
        sheet.show(anchor, row.slider)
    }

    private companion object {
        const val PREVIEW_WIDTH_DP = 480f
        const val PREVIEW_HEIGHT_DP = 270f
        const val GAP_DP = 20f
    }
}
