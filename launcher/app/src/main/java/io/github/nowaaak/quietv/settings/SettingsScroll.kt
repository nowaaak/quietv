package io.github.nowaaak.quietv.settings

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.ScrollView

/**
 * When the focused row is not fully on screen, scrolls so it keeps a margin to the top and bottom
 * of the screen, instead of stopping at the very edge. Near the start and the end of the list it
 * scrolls no further than the list goes.
 */
class SettingsScroll(context: Context, attrs: AttributeSet?) : ScrollView(context, attrs) {

    private val margin = (MARGIN_DP * resources.displayMetrics.density).toInt()

    override fun computeScrollDeltaToGetChildRectOnScreen(rect: Rect): Int {
        // A row that is fully visible never moves the list: otherwise a list that nearly fits
        // jumps a little as soon as its last row is focused.
        if (super.computeScrollDeltaToGetChildRectOnScreen(rect) == 0) return 0
        val padded = Rect(rect)
        padded.inset(0, -margin)
        return super.computeScrollDeltaToGetChildRectOnScreen(padded)
    }

    private companion object {
        const val MARGIN_DP = 120f
    }
}
