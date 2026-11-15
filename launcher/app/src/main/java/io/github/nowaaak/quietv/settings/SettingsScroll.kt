package io.github.nowaaak.quietv.settings

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.widget.ScrollView

/**
 * Scrolls so the focused row keeps a margin to the top and bottom of the screen, instead of
 * stopping at the very edge. Near the start and the end of the list it scrolls no further than
 * the list goes.
 */
class SettingsScroll(context: Context, attrs: AttributeSet?) : ScrollView(context, attrs) {

    private val margin = (MARGIN_DP * resources.displayMetrics.density).toInt()

    override fun computeScrollDeltaToGetChildRectOnScreen(rect: Rect): Int {
        val padded = Rect(rect)
        padded.inset(0, -margin)
        return super.computeScrollDeltaToGetChildRectOnScreen(padded)
    }

    private companion object {
        const val MARGIN_DP = 120f
    }
}
