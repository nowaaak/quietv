package io.github.nowaaak.quietv.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/** A colour dot for the accent picker. Focused it gets a ring, the colour in use gets a check dot. */
class SwatchDrawable(
    color: Int,
    ring: Int,
    private val selected: Boolean,
    private val ringWidth: Float,
) : Drawable() {

    private var focused = false
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    private val mark = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = if (Contrast.luminance(color) > MARK_THRESHOLD) Color.BLACK else Color.WHITE
    }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = ringWidth
        this.color = ring
    }

    override fun isStateful(): Boolean = true

    override fun onStateChange(state: IntArray): Boolean {
        val now = state.contains(android.R.attr.state_focused)
        if (now == focused) return false
        focused = now
        invalidateSelf()
        return true
    }

    override fun draw(canvas: Canvas) {
        val b = bounds
        val cx = b.exactCenterX()
        val cy = b.exactCenterY()
        val outer = minOf(b.width(), b.height()) / 2f
        val gap = ringWidth * 2f
        canvas.drawCircle(cx, cy, outer - gap, fill)
        if (selected) canvas.drawCircle(cx, cy, outer * MARK_SIZE, mark)
        if (focused) canvas.drawCircle(cx, cy, outer - ringWidth / 2f, ringPaint)
    }

    override fun setAlpha(alpha: Int) = Unit

    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private companion object {
        const val MARK_THRESHOLD = 0.45
        const val MARK_SIZE = 0.16f
    }
}
