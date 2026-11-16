package io.github.nowaaak.quietv.settings

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Outline
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import io.github.nowaaak.quietv.R
import io.github.nowaaak.quietv.config.LauncherConfig
import io.github.nowaaak.quietv.config.WallpaperConfig
import io.github.nowaaak.quietv.ui.Palette
import io.github.nowaaak.quietv.ui.ThemeColors
import io.github.nowaaak.quietv.ui.ThemePreview
import io.github.nowaaak.quietv.wallpaper.Scenes
import io.github.nowaaak.quietv.wallpaper.WallpaperLook
import io.github.nowaaak.quietv.wallpaper.WallpaperStore

/**
 * Backgrounds as a list next to a preview of the home screen, with the dimming slider below. The
 * preview changes as you move through the list and the slider, darkened exactly like the home screen.
 */
class BackgroundPicker(
    private val activity: Activity,
    private val palette: Palette,
    private val config: LauncherConfig,
    private val choices: List<String>,
    private val current: String,
    private val name: (String) -> String,
    private val onPick: (choice: String, dim: Int) -> Unit,
) {

    private val density = activity.resources.displayMetrics.density
    private val pictures = HashMap<String, Bitmap?>()
    private val loading = HashSet<String>()
    private lateinit var sheet: PickerSheet
    private lateinit var preview: ImageView
    private lateinit var dimRow: SliderRow
    private var selected = current
    private var dim = config.wallpaper.dim

    fun show(anchor: View, focusDimming: Boolean) {
        sheet = PickerSheet(
            activity, palette,
            activity.getString(R.string.settings_background),
            activity.getString(R.string.picker_slider_hint),
        )
        val column = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        val top = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
        preview = ImageView(activity).apply {
            scaleType = ImageView.ScaleType.FIT_XY
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, view.width * PREVIEW_CORNER)
                }
            }
            clipToOutline = true
        }
        top.addView(preview, LinearLayout.LayoutParams(px(PREVIEW_WIDTH_DP), px(PREVIEW_HEIGHT_DP)))

        val list = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
        var focus: View? = null
        for (choice in choices) {
            val row = choiceRow(list, choice)
            if (choice == current) focus = row
            // Keep the row height from its layout, only the width changes.
            row.layoutParams = row.layoutParams.apply { width = px(LIST_WIDTH_DP) }
            list.addView(row)
        }
        top.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            marginStart = px(GAP_DP)
        })
        column.addView(top, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT))

        dimRow = SliderRow(
            activity, column, palette, activity.getString(R.string.settings_wallpaper_dim),
            min = 0, max = WallpaperConfig.MAX_DIM, step = DIM_STEP, start = dim,
            format = { activity.getString(R.string.percent, it) },
        ) { value ->
            dim = value
            updatePreview()
        }
        dimRow.slider.setOnClickListener { pick() }
        column.addView(dimRow.view, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = px(GAP_DP)
        })

        sheet.content.addView(column)
        updatePreview()
        sheet.show(anchor, if (focusDimming && selected != SettingsActivity.BACKGROUND_COLOR) dimRow.slider else focus)
    }

    private fun choiceRow(parent: ViewGroup, choice: String): View {
        val row = activity.layoutInflater.inflate(R.layout.item_setting, parent, false)
        row.backgroundTintList = ThemeColors.rowBackground(palette)
        row.findViewById<TextView>(R.id.title).apply {
            text = name(choice)
            setTextColor(ThemeColors.pillContent(palette))
        }
        row.findViewById<TextView>(R.id.value).visibility = View.GONE
        row.findViewById<View>(R.id.swatch).apply {
            // A dot marks the background in use.
            visibility = if (choice == current) View.VISIBLE else View.INVISIBLE
            (background.mutate() as GradientDrawable).setColor(config.accent)
        }
        row.setOnFocusChangeListener { _, focused ->
            if (focused && selected != choice) {
                selected = choice
                updatePreview()
            }
        }
        row.setOnClickListener { pick() }
        return row
    }

    private fun pick() {
        sheet.dismiss()
        if (selected != current || dim != config.wallpaper.dim) onPick(selected, dim)
    }

    private fun updatePreview() {
        val choice = selected
        val plain = choice == SettingsActivity.BACKGROUND_COLOR
        dimRow.view.visibility = if (plain) View.INVISIBLE else View.VISIBLE
        dimRow.slider.isFocusable = !plain
        if (plain) {
            preview.setImageDrawable(ThemePreview(palette, config.accent, config.tileRadius))
            return
        }
        if (pictures.containsKey(choice)) {
            compose(pictures[choice])
            return
        }
        if (!loading.add(choice)) return
        val width = px(PREVIEW_WIDTH_DP)
        val height = px(PREVIEW_HEIGHT_DP)
        Thread {
            val picture = if (choice == SettingsActivity.BACKGROUND_IMAGE) {
                val store = WallpaperStore(activity)
                if (store.exists()) WallpaperStore.decode(store.file, width, height) else null
            } else {
                Scenes.render(activity, choice, width, height)
            }
            activity.runOnUiThread {
                loading.remove(choice)
                pictures[choice] = picture
                if (!activity.isDestroyed && selected == choice) compose(picture)
            }
        }.start()
    }

    private fun compose(picture: Bitmap?) {
        if (picture == null) {
            preview.setImageDrawable(ThemePreview(palette, config.accent, config.tileRadius))
            return
        }
        val dimmed = WallpaperLook.dim(picture.copy(Bitmap.Config.ARGB_8888, true), palette, dim)
        preview.setImageDrawable(
            LayerDrawable(
                arrayOf(
                    BitmapDrawable(activity.resources, dimmed),
                    ThemePreview(palette, config.accent, config.tileRadius, drawBackground = false),
                ),
            ),
        )
    }

    private fun px(dp: Float): Int = (dp * density).toInt()

    private companion object {
        const val PREVIEW_WIDTH_DP = 400f
        const val PREVIEW_HEIGHT_DP = 225f
        const val PREVIEW_CORNER = 0.035f
        const val LIST_WIDTH_DP = 280f
        const val GAP_DP = 24f
        const val DIM_STEP = 5
    }
}
