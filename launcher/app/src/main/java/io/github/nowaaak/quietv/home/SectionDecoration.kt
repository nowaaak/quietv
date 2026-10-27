package io.github.nowaaak.quietv.home

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import io.github.nowaaak.quietv.R
import io.github.nowaaak.quietv.ui.themeColor

class SectionDecoration(context: Context, private val dockSize: Int) : RecyclerView.ItemDecoration() {

    private val density = context.resources.displayMetrics.density
    private val locale = context.resources.configuration.locales[0]
    private val favorites = context.getString(R.string.favorites).uppercase(locale)
    private val allApps = context.getString(R.string.all_apps).uppercase(locale)
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.themeColor(R.attr.qvTextMuted)
        textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 11f, context.resources.displayMetrics)
        letterSpacing = 0.14f
        typeface = context.resources.getFont(R.font.geist)
        fontVariationSettings = "'wght' 500"
    }

    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val pos = parent.getChildAdapterPosition(view)
        if (pos in 0 until dockSize) {
            outRect.top = (LABEL_DP * density).toInt()
            outRect.bottom = (DOCK_BOTTOM_DP * density).toInt()
        } else if (pos in dockSize until 2 * dockSize) {
            outRect.top = ((LABEL_DP + GRID_GAP_DP) * density).toInt()
        }
    }

    override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val label = when (parent.getChildAdapterPosition(child)) {
                0 -> favorites
                dockSize -> allApps
                else -> null
            } ?: continue
            val card = child.findViewById<View>(R.id.card) ?: continue
            val x = child.left + card.left + 2 * density
            val y = child.top + child.translationY - 10 * density
            c.drawText(label, x, y, text)
        }
    }

    companion object {
        const val LABEL_DP = 30f
        const val DOCK_BOTTOM_DP = 4f
        private const val GRID_GAP_DP = 14f
    }
}
