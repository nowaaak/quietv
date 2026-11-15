package io.github.nowaaak.quietv.settings

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.PopupWindow
import android.widget.TextView
import io.github.nowaaak.quietv.R
import io.github.nowaaak.quietv.ui.Contrast
import io.github.nowaaak.quietv.ui.Palette
import io.github.nowaaak.quietv.ui.ThemeColors

/** A centred panel over a dimmed screen. Back closes it without choosing anything. */
class PickerSheet(activity: Activity, private val palette: Palette, title: String, hint: String) {

    private val density = activity.resources.displayMetrics.density
    private val root: View = activity.layoutInflater.inflate(R.layout.sheet_picker, FrameLayout(activity), false)
    private val panel: View = root.findViewById(R.id.panel)
    val content: FrameLayout = root.findViewById(R.id.content)

    private val popup = PopupWindow(root, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, true)
        .apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            animationStyle = 0
            elevation = 0f
        }

    var onDismiss: (() -> Unit)? = null

    init {
        root.setBackgroundColor(Contrast.withAlpha(palette.background, SCRIM_ALPHA))
        panel.background = ThemeColors.panel(palette, PANEL_RADIUS_DP * density, density.toInt().coerceAtLeast(1))
        root.findViewById<TextView>(R.id.title).apply {
            text = title
            setTextColor(palette.text)
        }
        root.findViewById<TextView>(R.id.hint).apply {
            text = hint
            setTextColor(palette.textMuted)
        }
    }

    fun show(anchor: View, focus: View?) {
        popup.setOnDismissListener { onDismiss?.invoke() }
        panel.alpha = 0f
        panel.scaleX = START_SCALE
        panel.scaleY = START_SCALE
        popup.showAtLocation(anchor, Gravity.NO_GRAVITY, 0, 0)
        panel.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(DURATION_MS)
            .setInterpolator(DecelerateInterpolator(1.6f)).start()
        focus?.requestFocus()
    }

    fun dismiss() = popup.dismiss()

    private companion object {
        const val SCRIM_ALPHA = 0xD9
        const val PANEL_RADIUS_DP = 22f
        const val START_SCALE = 0.96f
        const val DURATION_MS = 200L
    }
}
