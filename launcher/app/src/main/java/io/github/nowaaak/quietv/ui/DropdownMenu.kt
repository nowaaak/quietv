package io.github.nowaaak.quietv.ui

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.annotation.DrawableRes
import io.github.nowaaak.quietv.R

class DropdownMenu(
    private val activity: Activity,
    root: ViewGroup,
    private val palette: Palette,
    private val accent: Int,
) {

    private val density = activity.resources.displayMetrics.density
    private val panel: View = activity.layoutInflater.inflate(R.layout.popup_menu, root, false)
    private val rows: LinearLayout = panel.findViewById(R.id.rows)
    private val content = ThemeColors.pillContent(palette)

    init {
        panel.background = ThemeColors.rounded(
            palette.surface,
            PANEL_RADIUS_DP * density,
            palette.stroke,
            density.toInt().coerceAtLeast(1),
        )
    }
    private val popup = PopupWindow(panel, (WIDTH_DP * density).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT, true)
        .apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            isOutsideTouchable = true
            animationStyle = 0
            elevation = 0f
        }

    val isEmpty: Boolean get() = rows.childCount == 0

    var onDismiss: (() -> Unit)? = null

    fun addSeparator() {
        val line = View(activity)
        line.setBackgroundColor(palette.line)
        val margin = (6 * density).toInt()
        val inset = (12 * density).toInt()
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, density.toInt().coerceAtLeast(1))
        lp.setMargins(inset, margin, inset, margin)
        rows.addView(line, lp)
    }

    fun addRow(
        @DrawableRes icon: Int,
        title: String,
        status: String? = null,
        active: Boolean = false,
        action: () -> Unit,
    ) {
        val row = activity.layoutInflater.inflate(R.layout.item_menu, rows, false)
        row.backgroundTintList = ThemeColors.rowBackground(palette)
        row.findViewById<ImageView>(R.id.icon).apply {
            setImageResource(icon)
            imageTintList = content
        }
        row.findViewById<TextView>(R.id.title).apply {
            text = title
            setTextColor(content)
        }
        val statusView = row.findViewById<TextView>(R.id.status)
        statusView.setTextColor(ThemeColors.pillContentDim(palette))
        val dot = row.findViewById<View>(R.id.dot)
        dot.backgroundTintList = ColorStateList.valueOf(accent)
        dot.visibility = if (active) View.VISIBLE else View.GONE
        if (status == null) {
            statusView.visibility = View.GONE
        } else {
            statusView.text = status
        }
        row.setOnClickListener {
            popup.dismiss()
            action()
        }
        rows.addView(row)
    }

    fun show(anchor: View, gravity: Int, gapDp: Float = DEFAULT_GAP_DP, focusIndex: Int = 0) {
        popup.setOnDismissListener { onDismiss?.invoke() }
        val width = popup.width
        panel.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        val height = panel.measuredHeight
        val gap = (gapDp * density).toInt()
        val location = IntArray(2)
        anchor.getLocationOnScreen(location)
        val x = if (gravity == Gravity.END) location[0] + anchor.width - width else location[0]
        val fitsBelow = location[1] + anchor.height + gap + height <= anchor.rootView.height
        val y = if (fitsBelow) location[1] + anchor.height + gap else location[1] - gap - height

        panel.alpha = 0f
        panel.pivotY = if (fitsBelow) 0f else height.toFloat()
        panel.scaleY = 0.92f
        panel.translationY = (if (fitsBelow) -8 else 8) * density
        popup.showAtLocation(anchor, Gravity.NO_GRAVITY, x, y)
        panel.animate().alpha(1f).scaleY(1f).translationY(0f).setDuration(220)
            .setInterpolator(DecelerateInterpolator(1.6f)).start()
        val focusable = (0 until rows.childCount).map { rows.getChildAt(it) }.filter { it.isFocusable }
        (focusable.getOrNull(focusIndex) ?: focusable.firstOrNull())?.requestFocus()
    }

    val isShowing: Boolean get() = popup.isShowing

    private companion object {
        const val WIDTH_DP = 240f
        const val DEFAULT_GAP_DP = 10f
        const val PANEL_RADIUS_DP = 18f
    }
}
