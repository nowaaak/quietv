package io.github.nowaaak.quietv.ui

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

class FocusHalo(
    fill: Int,
    halo: Int,
    private val haloWidth: Float,
) : Drawable() {

    private var focused = false
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill }
    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = halo }

    override fun isStateful(): Boolean = true

    override fun onStateChange(state: IntArray): Boolean {
        val now = state.contains(android.R.attr.state_focused)
        if (now == focused) return false
        focused = now
        invalidateSelf()
        return true
    }

    override fun draw(canvas: Canvas) {
        if (!focused) return
        val b = bounds
        val r = minOf(b.width(), b.height()) / 2f
        canvas.drawRoundRect(b.left.toFloat(), b.top.toFloat(), b.right.toFloat(), b.bottom.toFloat(), r, r, haloPaint)
        val inset = haloWidth
        canvas.drawRoundRect(
            b.left + inset, b.top + inset, b.right - inset, b.bottom - inset,
            r - inset, r - inset, fillPaint,
        )
    }

    override fun setAlpha(alpha: Int) {
        fillPaint.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        fillPaint.colorFilter = colorFilter
        invalidateSelf()
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
